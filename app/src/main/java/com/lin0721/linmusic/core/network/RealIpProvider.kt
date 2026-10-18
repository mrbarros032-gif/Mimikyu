package com.lin0721.linmusic.core.network

// IP ：HeaderInterceptor（） and  WebViewLoginScreen（），
// Custom IP ， and  of  IP
class RealIpProvider {

    private val ipv4Pattern = Regex(
        "^((25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$"
    )

    // of  IP
    val sessionIpAddress: String by lazy {
        val ipPrefixes = listOf(
            "116.25", "218.17", "113.88", "121.14", "119.137",
            "58.60", "124.127", "223.73", "116.228", "180.168"
        )
        val prefix = ipPrefixes.random()
        "$prefix.${(1..254).random()}.${(1..254).random()}"
    }

    private fun isValidIpv4(ip: String): Boolean = ipv4Pattern.matches(ip.trim())

    // Back null；Custom IP， IP
    fun resolveIp(useRealIp: Boolean, customIp: String): String? {
        if (!useRealIp) return null
        return if (customIp.isNotBlank() && isValidIpv4(customIp)) customIp.trim() else sessionIpAddress
    }
}
