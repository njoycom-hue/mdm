package com.safecircle.app.vpn

import android.net.VpnService
import android.os.ParcelFileDescriptor

/**
 * 도메인 블랙리스트 기반 사이트 차단. 트래픽 내용을 열람하지 않고 DNS/SNI 수준에서
 * 목적지 도메인만 확인해 매치되면 드롭한다 (콘텐츠 필터링 목적, 감청 아님).
 *
 * NOTE: 실제 패킷 파싱/포워딩 구현은 별도 스토리로 분리. 여기서는 서비스 골격과
 * 차단 리스트 갱신 인터페이스만 정의한다.
 */
class DomainFilterVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null

    override fun onCreate() {
        super.onCreate()
        // TODO: BlockedDomainRepository에서 서버 동기화된 도메인 리스트 로드
    }

    fun startVpn() {
        val builder = Builder()
            .setSession("SafeCircle")
            .addAddress("10.0.0.2", 32)
            .addDnsServer("1.1.1.1")
            .addRoute("0.0.0.0", 0)

        vpnInterface = builder.establish()
        // TODO: 백그라운드 스레드에서 tun 인터페이스 읽어 DNS 쿼리의 도메인만 검사,
        //       차단 대상이면 응답 없이 드롭, 그 외는 그대로 포워딩
    }

    override fun onDestroy() {
        vpnInterface?.close()
        super.onDestroy()
    }
}
