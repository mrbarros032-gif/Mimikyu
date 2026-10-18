package com.lin0721.linmusic.core.network.crypto

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lin0721.linmusic.core.log.AppLogger
import com.lin0721.linmusic.core.network.NeteaseDeviceId
import kotlinx.coroutines.flow.first
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "XeapiKeyStore"
private const val KEY_FETCH_URL = "https://interface.music.163.com/api/gorilla/anti/crawler/security/key/get"
private const val KEY_FETCH_USER_AGENT =
    "NeteaseMusic/9.5.61.260802021928(9005061);Dalvik/2.1.0 (Linux; U; Android 12; HBN-AL00 Build/cd737a2.0)"

// xeapi  of  and 。 CryptoInterceptor  of  Context/
interface XeapiKeyStore {
    // ；。 null
    suspend fun getOrFetchPublicKey(): XeapiPublicKeyState?

    // ：，
    suspend fun refresh(): XeapiPublicKeyState?
}

private val Context.xeapiKeyDataStore by preferencesDataStore(
    name = "xeapi_key_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { ex ->
        AppLogger.e(TAG, "xeapi key cache corrupted, reset to default", ex)
        emptyPreferences()
    }
)

class XeapiKeyStoreImpl(private val context: Context) : XeapiKeyStore {

    private object Keys {
        val VERSION = stringPreferencesKey("version")
        val PUBLIC_KEY = stringPreferencesKey("public_key")
        val SK = stringPreferencesKey("sk")
    }

    // OkHttpClient， CryptoInterceptor/HeaderInterceptor ，
    // and  NosUploadClient  of
    private val rawClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override suspend fun getOrFetchPublicKey(): XeapiPublicKeyState? {
        readCached()?.let { return it }
        return registerAndCache()
    }

    override suspend fun refresh(): XeapiPublicKeyState? {
        context.xeapiKeyDataStore.edit { it.clear() }
        return registerAndCache()
    }

    private suspend fun readCached(): XeapiPublicKeyState? {
        val prefs = context.xeapiKeyDataStore.data.first()
        val publicKey = prefs[Keys.PUBLIC_KEY] ?: return null
        return XeapiPublicKeyState(
            version = prefs[Keys.VERSION] ?: "",
            publicKey = publicKey,
            sk = prefs[Keys.SK]
        )
    }

    private suspend fun saveCached(state: XeapiPublicKeyState) {
        context.xeapiKeyDataStore.edit { prefs ->
            prefs[Keys.VERSION] = state.version
            prefs[Keys.PUBLIC_KEY] = state.publicKey
            state.sk?.let { prefs[Keys.SK] = it }
        }
    }

    private suspend fun registerAndCache(): XeapiPublicKeyState? {
        val deviceId = NeteaseDeviceId.current()
        val cached = readCached()
        val response = try {
            fetchPublicKey(deviceId, currentKeyVersion = cached?.version ?: "")
        } catch (e: Exception) {
            AppLogger.e(TAG, "xeapi device registration request failed", e)
            return null
        }

        // publicKey/sk， of
        val merged = response.copy(
            publicKey = response.publicKey.ifEmpty { cached?.publicKey.orEmpty() },
            sk = response.sk?.takeIf { it.isNotEmpty() } ?: cached?.sk
        )

        if (merged.publicKey.isEmpty() || merged.sk.isNullOrEmpty()) {
            AppLogger.e(TAG, "xeapi public key response missing required fields, no local cache", null)
            return null
        }

        saveCached(merged)
        return merged
    }

    private fun fetchPublicKey(deviceId: String, currentKeyVersion: String): XeapiPublicKeyState {
        val nonce = (1..16).map { (0..9).random() }.joinToString("")
        val timestamp = System.currentTimeMillis().toString()
        val signature = XeapiCrypto.sign(timestamp, nonce)

        val formBody = FormBody.Builder()
            .add("appVersion", "9.5.61")
            .add("currentKeyVersion", currentKeyVersion)
            .add("deviceId", deviceId)
            .add("nonce", nonce)
            .add("os", "android")
            .add("requestType", "active")
            .add("signature", signature)
            .add("t1", "")
            .add("t2", "")
            .add("timestamp", timestamp)
            .add("uid", "")
            .build()

        val requestBuilder = Request.Builder()
            .url(KEY_FETCH_URL)
            .header("User-Agent", KEY_FETCH_USER_AGENT)
            .post(formBody)
        if (deviceId.isNotEmpty()) {
            requestBuilder.header("Cookie", "deviceId=$deviceId")
        }

        rawClient.newCall(requestBuilder.build()).execute().use { httpResponse ->
            val bodyString = httpResponse.body?.string().orEmpty()
            if (!httpResponse.isSuccessful || bodyString.isEmpty()) {
                throw IllegalStateException("xeapi key fetch http ${httpResponse.code}")
            }
            val json = JSONObject(bodyString)
            if (json.optInt("code") != 200) {
                throw IllegalStateException("xeapi key fetch code ${json.optInt("code")}")
            }
            val data = json.getJSONObject("data")
            val encryptedData = data.optString("encryptedData", "")
            if (encryptedData.isEmpty()) {
                throw IllegalStateException("xeapi key fetch missing encryptedData")
            }
            val respSignature = data.optString("signature", "")
            val respTimestamp = data.optString("timestamp", "")
            if (respSignature.isNotEmpty()) {
                val expectedSignature = XeapiCrypto.sign(respTimestamp, nonce)
                if (expectedSignature != respSignature) {
                    throw IllegalStateException("xeapi key fetch signature mismatch")
                }
            }
            return XeapiCrypto.decryptPublicKeyResponse(encryptedData)
        }
    }
}
