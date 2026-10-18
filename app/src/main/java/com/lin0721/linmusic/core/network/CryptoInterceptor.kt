package com.lin0721.linmusic.core.network

import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.network.crypto.NeteaseCrypto
import com.lin0721.linmusic.core.network.crypto.XeapiCrypto
import com.lin0721.linmusic.core.network.crypto.XeapiKeyStore
import kotlinx.coroutines.runBlocking
import okhttp3.FormBody
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer

private const val TAG = "CryptoInterceptor"

// OkHttp  ——  API  (WeApi/EApi/LinuxApi/XeApi)
class CryptoInterceptor(private val xeapiKeyStore: XeapiKeyStore) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val url = originalRequest.url.toString()

        // of
        val originalBody = originalRequest.body ?: return chain.proceed(originalRequest)

        val cryptoType = resolveCryptoType(url) ?: return chain.proceed(originalRequest)
        val rawJson = originalBody.readString()
        val cookies = originalRequest.header("Cookie") ?: ""

        val encryptedForm = when (cryptoType) {
            CryptoType.WEAPI -> buildWeApiForm(rawJson, cookies)
            CryptoType.EAPI -> buildEApiForm(url, rawJson)
            CryptoType.LINUXAPI -> buildLinuxApiForm(rawJson)
            CryptoType.XEAPI -> buildXeApiForm(url, rawJson)
        }

        val newRequest = originalRequest.newBuilder()
            .post(encryptedForm)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()

        val response = chain.proceed(newRequest)
        return if (cryptoType == CryptoType.XEAPI) decryptXeApiResponse(response) else response
    }

    // xeapi  queryString  e_r=true，，
    // Retrofit  JSON
    private fun decryptXeApiResponse(response: Response): Response {
        val body = response.body ?: return response
        val encryptedBytes = body.bytes()
        val decryptedJson = try {
            XeapiCrypto.decryptResponseBody(encryptedBytes)
        } catch (e: Exception) {
            AppLogger.e(TAG, "xeapi response body decryption failed", e)
            return response.newBuilder()
                .body(encryptedBytes.toResponseBody(body.contentType()))
                .build()
        }
        return response.newBuilder()
            .body(decryptedJson.toResponseBody("application/json; charset=utf-8".toMediaType()))
            .build()
    }

    // ----------  ----------

    internal enum class CryptoType { WEAPI, EAPI, LINUXAPI, XEAPI }

    // URL （internal ）
    internal fun resolveCryptoType(url: String): CryptoType? = when {
        url.contains("/eapi/") -> CryptoType.EAPI
        url.contains("/xeapi/") -> CryptoType.XEAPI
        url.contains("/linux/api/") -> CryptoType.LINUXAPI
        url.contains("/weapi/") || url.contains("/api/") -> CryptoType.WEAPI
        else -> null
    }

    // ----------  FormBody ----------

    // WeApi： params + encSecKey
    private fun buildWeApiForm(rawJson: String, cookies: String): FormBody {
        val csrfToken = Regex("__csrf=([^;]+)").find(cookies)?.groupValues?.get(1) ?: ""
        
        val jsonPayload = try {
            val orgJson = org.json.JSONObject(rawJson)
            orgJson.put("csrf_token", csrfToken)
            // Back JSON (e_r=false)， binary
            orgJson.put("e_r", false)
            orgJson.toString()
        } catch (e: Exception) {
            AppLogger.w(TAG, "WeApi csrf_token injection failed, fallback to raw body", e)
            rawJson
        }

        val encrypted = NeteaseCrypto.weapi(jsonPayload)
        return FormBody.Builder()
            .add("params", encrypted.getValue("params"))
            .add("encSecKey", encrypted.getValue("encSecKey"))
            .build()
    }

    // EApi： params
    private fun buildEApiForm(url: String, rawJson: String): FormBody {
        // URL  /eapi/...  EApi  of  url
        val eapiPath = extractEApiPath(url)
        val apiPath = eapiPath.replace("/eapi/", "/api/")
        
        // JSON payload  Header
        val rootObj = try {
            org.json.JSONObject(rawJson)
        } catch (e: Exception) {
            AppLogger.e(TAG, "EApi request body parsing failed, payload cleared: $url", e)
            org.json.JSONObject()
        }
        
        val model = android.os.Build.MODEL
        val deviceId = NeteaseDeviceId.current()

        val headerObj = org.json.JSONObject().apply {
            put("osver", android.os.Build.VERSION.RELEASE)
            put("deviceId", deviceId)
            put("os", "android")
            put("appver", "9.0.90")
            put("versioncode", "140")
            put("mobilename", model)
            put("buildver", System.currentTimeMillis().toString().substring(0, 10))
            put("resolution", "1920x1080")
            put("requestId", "${System.currentTimeMillis()}_${(1000..9999).random()}")
        }
        rootObj.put("header", headerObj)
        rootObj.put("e_r", false)

        val encrypted = NeteaseCrypto.eapi(apiPath, rootObj.toString())
        return FormBody.Builder()
            .add("params", encrypted.getValue("params"))
            .build()
    }

    // LinuxApi： eparams
    private fun buildLinuxApiForm(rawJson: String): FormBody {
        val encrypted = NeteaseCrypto.linuxapi(rawJson)
        return FormBody.Builder()
            .add("eparams", encrypted.getValue("eparams"))
            .build()
    }

    // XEapi： B/S/R
    private fun buildXeApiForm(url: String, rawJson: String): FormBody {
        val xeapiPath = extractXeApiPath(url)
        val apiPath = xeapiPath.replace("/xeapi/", "/api/")

        val dataMap = try {
            val obj = org.json.JSONObject(rawJson)
            val map = LinkedHashMap<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = obj.get(key).toString()
            }
            map
        } catch (e: Exception) {
            AppLogger.e(TAG, "XEapi request body parsing failed, payload cleared: $url", e)
            emptyMap()
        }

        val publicKeyState = runBlocking { xeapiKeyStore.getOrFetchPublicKey() }
            ?: throw java.io.IOException("xeapi public key failed, comments temporarily unavailable")

        val encrypted = XeapiCrypto.assembleRequest(apiPath, dataMap, publicKeyState)
        return FormBody.Builder()
            .add("B", encrypted.b)
            .add("S", encrypted.s)
            .add("R", encrypted.r)
            .build()
    }

    // URL  /xeapi/  of
    private fun extractXeApiPath(url: String): String {
        val idx = url.indexOf("/xeapi/")
        return if (idx != -1) url.substring(idx) else url
    }

    // ----------  ----------

    // [RequestBody]
    private fun RequestBody.readString(): String {
        val buffer = Buffer()
        writeTo(buffer)
        return buffer.readUtf8()
    }
    // URL  /eapi/  of
    private fun extractEApiPath(url: String): String {
        val idx = url.indexOf("/eapi/")
        return if (idx != -1) url.substring(idx) else url
    }
}
