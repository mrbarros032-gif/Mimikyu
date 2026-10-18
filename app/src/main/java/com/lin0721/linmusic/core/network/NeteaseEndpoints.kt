package com.lin0721.linmusic.core.network

// of  and 。 NetworkModule、HeaderInterceptor  and
// WebViewLoginScreen  6 ，。
//
// ： buildConfigField —— ，
// staging/sandbox ， of 。
object NeteaseEndpoints {

    // ，weapi  and
    const val WEB_HOST = "music.163.com"

    const val WEB_BASE_URL = "https://$WEB_HOST"

    // eapi  of ， PC
    const val EAPI_HOST = "interface.music.163.com"

    // xeapi  of （//Delete）
    const val XEAPI_HOST = "interface3.music.163.com"

    // ， X-Real-IP
    const val DOMAIN_SUFFIX = "163.com"


    const val LOGIN_URL = "$WEB_BASE_URL/m/login"
}
