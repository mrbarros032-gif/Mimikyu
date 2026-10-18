package com.lin0721.linmusic.core.network

import java.security.MessageDigest

// ：eapi  and  xeapi / deviceId，
// CryptoInterceptor ，
object NeteaseDeviceId {

    // ， Android ；MD5  JVM/Android ，
    fun compute(brand: String, model: String, board: String): String {
        return try {
            val rawId = "${brand}_${model}_${board}"
            val digest = MessageDigest.getInstance("MD5").digest(rawId.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            model
        }
    }

    fun current(): String = compute(
        brand = android.os.Build.BRAND,
        model = android.os.Build.MODEL,
        board = android.os.Build.BOARD
    )
}
