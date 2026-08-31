package com.safecircle.app.vpn.dns

import java.nio.ByteBuffer

/**
 * IPv4 + UDP + DNS 질의 패킷을 파싱한다. IPv6/TCP DNS는 스코프 밖 (docs/ARCHITECTURE.md 참고:
 * 이 VPN은 addRoute로 가짜 DNS 서버 주소만 터널링하므로 여기 도달하는 패킷은 사실상 DNS 질의뿐).
 */
data class DnsQueryPacket(
    val srcAddress: ByteArray,
    val dstAddress: ByteArray,
    val srcPort: Int,
    val dstPort: Int,
    val dnsPayload: ByteArray,
    val queryId: Int,
    val questionDomain: String,
    val questionSectionRaw: ByteArray
) {
    companion object {
        private const val IPV4_VERSION = 4

        fun parse(raw: ByteArray, length: Int): DnsQueryPacket? {
            if (length < 20) return null
            val versionAndIhl = raw[0].toInt() and 0xFF
            val version = versionAndIhl shr 4
            if (version != IPV4_VERSION) return null
            val ihl = (versionAndIhl and 0x0F) * 4
            val protocol = raw[9].toInt() and 0xFF
            if (protocol != 17) return null // UDP only

            val srcAddress = raw.copyOfRange(12, 16)
            val dstAddress = raw.copyOfRange(16, 20)

            if (length < ihl + 8) return null
            val udpStart = ihl
            val srcPort = ((raw[udpStart].toInt() and 0xFF) shl 8) or (raw[udpStart + 1].toInt() and 0xFF)
            val dstPort = ((raw[udpStart + 2].toInt() and 0xFF) shl 8) or (raw[udpStart + 3].toInt() and 0xFF)
            if (dstPort != 53) return null

            val udpLength = ((raw[udpStart + 4].toInt() and 0xFF) shl 8) or (raw[udpStart + 5].toInt() and 0xFF)
            val dnsStart = udpStart + 8
            val dnsLength = udpLength - 8
            if (dnsLength <= 12 || dnsStart + dnsLength > length) return null

            val dnsPayload = raw.copyOfRange(dnsStart, dnsStart + dnsLength)
            val queryId = ((dnsPayload[0].toInt() and 0xFF) shl 8) or (dnsPayload[1].toInt() and 0xFF)

            val (domain, questionEnd) = parseQuestionName(dnsPayload, 12) ?: return null
            // QNAME 다음 QTYPE(2)+QCLASS(2)까지 포함해 질문 섹션 원본을 보존 (응답 조립 시 그대로 재사용)
            val questionSectionEnd = questionEnd + 4
            if (questionSectionEnd > dnsPayload.size) return null
            val questionSectionRaw = dnsPayload.copyOfRange(12, questionSectionEnd)

            return DnsQueryPacket(srcAddress, dstAddress, srcPort, dstPort, dnsPayload, queryId, domain, questionSectionRaw)
        }

        /** DNS 라벨 시퀀스를 사람이 읽을 수 있는 도메인 문자열로 변환. 압축 포인터(0xC0)는 질의 섹션에 나타나지 않으므로 미지원. */
        private fun parseQuestionName(payload: ByteArray, start: Int): Pair<String, Int>? {
            val labels = mutableListOf<String>()
            var pos = start
            while (pos < payload.size) {
                val len = payload[pos].toInt() and 0xFF
                if (len == 0) {
                    return labels.joinToString(".") to (pos + 1)
                }
                pos += 1
                if (pos + len > payload.size) return null
                labels.add(String(payload, pos, len, Charsets.US_ASCII))
                pos += len
            }
            return null
        }
    }
}

object DnsResponseBuilder {

    /** NXDOMAIN 응답: 차단된 도메인에 대해 "존재하지 않음"으로 답한다 (내용 열람이 아닌 목적지 차단). */
    fun buildBlockedResponse(query: DnsQueryPacket): ByteArray {
        val dns = ByteBuffer.allocate(12 + query.questionSectionRaw.size)
        dns.putShort(query.queryId.toShort())
        dns.putShort(0x8183.toShort()) // QR=1, RD=1, RA=1, RCODE=3 (NXDOMAIN)
        dns.putShort(1) // QDCOUNT
        dns.putShort(0) // ANCOUNT
        dns.putShort(0) // NSCOUNT
        dns.putShort(0) // ARCOUNT
        dns.put(query.questionSectionRaw)
        return wrapUdpIpv4(
            payload = dns.array(),
            srcAddress = query.dstAddress, // 응답이므로 src/dst를 뒤집는다
            dstAddress = query.srcAddress,
            srcPort = query.dstPort,
            dstPort = query.srcPort
        )
    }

    /** 업스트림 DNS(예: 1.1.1.1)에서 받은 원본 응답 바이트를 그대로 클라이언트에게 릴레이한다. */
    fun wrapUpstreamResponse(query: DnsQueryPacket, upstreamDnsPayload: ByteArray): ByteArray =
        wrapUdpIpv4(
            payload = upstreamDnsPayload,
            srcAddress = query.dstAddress,
            dstAddress = query.srcAddress,
            srcPort = query.dstPort,
            dstPort = query.srcPort
        )

    private fun wrapUdpIpv4(payload: ByteArray, srcAddress: ByteArray, dstAddress: ByteArray, srcPort: Int, dstPort: Int): ByteArray {
        val udpLength = 8 + payload.size
        val totalLength = 20 + udpLength
        val packet = ByteBuffer.allocate(totalLength)

        // IPv4 header
        packet.put(0x45.toByte()) // version=4, IHL=5 (20 bytes, no options)
        packet.put(0x00) // DSCP/ECN
        packet.putShort(totalLength.toShort())
        packet.putShort(0) // identification
        packet.putShort(0x4000.toShort()) // flags=DF, fragment offset=0
        packet.put(64) // TTL
        packet.put(17) // protocol = UDP
        val checksumPos = packet.position()
        packet.putShort(0) // header checksum placeholder
        packet.put(srcAddress)
        packet.put(dstAddress)

        // UDP header (checksum=0 is valid for IPv4 per RFC 768 — receiver-side apps don't require it)
        packet.putShort(srcPort.toShort())
        packet.putShort(dstPort.toShort())
        packet.putShort(udpLength.toShort())
        packet.putShort(0)

        packet.put(payload)

        val array = packet.array()
        val checksum = ipv4HeaderChecksum(array, 0, 20)
        array[checksumPos] = (checksum shr 8).toByte()
        array[checksumPos + 1] = (checksum and 0xFF).toByte()
        return array
    }

    private fun ipv4HeaderChecksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = offset
        while (i < offset + length) {
            val word = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        while (sum shr 16 != 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv() and 0xFFFF
    }
}
