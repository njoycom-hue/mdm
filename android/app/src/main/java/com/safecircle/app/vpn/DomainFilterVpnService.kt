package com.safecircle.app.vpn

import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import com.safecircle.app.settings.PolicyRepository
import com.safecircle.app.vpn.dns.DnsQueryPacket
import com.safecircle.app.vpn.dns.DnsResponseBuilder
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import kotlin.concurrent.thread

/**
 * 도메인 블랙리스트 기반 사이트 차단. 실제 트래픽을 통째로 터널링하지 않고, VPN 라우팅을
 * "가짜 DNS 서버 주소"로만 좁혀서(addRoute) DNS 질의만 가로챈다. 그 외 앱 트래픽은 평소처럼
 * 일반 네트워크로 직접 나간다 — 콘텐츠 감청이 아니라 목적지 차단이 목표이므로 이 정도로 충분하다.
 */
class DomainFilterVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var workerThread: Thread? = null
    @Volatile private var running = false

    private lateinit var policyRepository: PolicyRepository

    override fun onCreate() {
        super.onCreate()
        policyRepository = PolicyRepository(applicationContext)
    }

    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        startVpn()
        return START_STICKY
    }

    private fun startVpn() {
        if (vpnInterface != null) return

        val builder = Builder()
            .setSession("SafeCircle")
            .addAddress(LOCAL_ADDRESS, 32)
            .addDnsServer(FAKE_DNS_ADDRESS)
            .addRoute(FAKE_DNS_ADDRESS, 32)

        vpnInterface = builder.establish() ?: return
        running = true
        workerThread = thread(name = "safecircle-dns-filter") { runFilterLoop() }
    }

    private fun runFilterLoop() {
        val fd = vpnInterface ?: return
        val input = FileInputStream(fd.fileDescriptor)
        val output = FileOutputStream(fd.fileDescriptor)
        val buffer = ByteArray(32 * 1024)

        while (running) {
            val length = try {
                input.read(buffer)
            } catch (e: Exception) {
                if (running) Log.w(TAG, "tun read failed", e)
                break
            }
            if (length <= 0) continue

            val query = DnsQueryPacket.parse(buffer, length) ?: continue
            handleDnsQuery(query, output)
        }
    }

    private fun handleDnsQuery(query: DnsQueryPacket, output: FileOutputStream) {
        val blocked = isBlocked(query.questionDomain)
        if (blocked) {
            output.write(DnsResponseBuilder.buildBlockedResponse(query))
            return
        }

        try {
            val socket = DatagramSocket()
            protect(socket) // 이 소켓의 트래픽은 VPN 터널을 다시 타지 않도록 제외
            socket.soTimeout = 5000

            val upstream = InetSocketAddress(UPSTREAM_DNS, 53)
            socket.send(DatagramPacket(query.dnsPayload, query.dnsPayload.size, upstream))

            val responseBuffer = ByteArray(1500)
            val responsePacket = DatagramPacket(responseBuffer, responseBuffer.size)
            socket.receive(responsePacket)
            socket.close()

            val upstreamPayload = responseBuffer.copyOfRange(0, responsePacket.length)
            output.write(DnsResponseBuilder.wrapUpstreamResponse(query, upstreamPayload))
        } catch (e: Exception) {
            Log.w(TAG, "upstream DNS forward failed for ${query.questionDomain}", e)
        }
    }

    private fun isBlocked(domain: String): Boolean {
        val blockedDomains = policyRepository.blockedDomains()
        val lower = domain.lowercase().trimEnd('.')
        return blockedDomains.any { lower == it || lower.endsWith(".$it") }
    }

    override fun onDestroy() {
        running = false
        workerThread?.interrupt()
        vpnInterface?.close()
        vpnInterface = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "DomainFilterVpn"
        private const val LOCAL_ADDRESS = "10.111.222.2"
        private const val FAKE_DNS_ADDRESS = "10.111.222.1"
        private const val UPSTREAM_DNS = "1.1.1.1"
    }
}
