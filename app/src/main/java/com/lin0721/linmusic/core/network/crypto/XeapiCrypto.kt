package com.lin0721.linmusic.core.network.crypto

import java.net.URLEncoder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Provider
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import java.util.zip.GZIPInputStream
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

// xeapi ：version ，sk  of
data class XeapiPublicKeyState(
    val version: String,
    val publicKey: String,
    val sk: String?
)

// xeapi ：X25519  + AES-GCM/ECB + HMAC 。
object XeapiCrypto {

    // AES-256-ECB ， +  R  +
    internal val XEAPI_STATIC_KEY: ByteArray = hexToBytes(
        "ab1d5a430f6bb04a3f01e81ddd72bd916d5ce591248ac128714806d7f8fb1b84"
    )

    // HMAC ；HMAC  of  key  of  UTF-8 ， base64
    private const val XEAPI_SIGN_KEY =
        "mUHCwVNWJbunMqAHf5MImuirT6plvs6VSFW62MGHstFQxhBGdEoIhLItH3djc4+FB/OKty3+lL2rGeoFBpVe5g=="

    // AES-128-ECB， and  NeteaseCrypto  of  EAPI_KEY ， xeapi
    private const val EAPI_KEY = "e82ckenh8dichen8"

    // RFC8410 X25519  DER SPKI （12 ）：Android  32 ，
    // BouncyCastle  of  JCA KeyFactory
    private val X25519_SPKI_PREFIX = hexToBytes("302a300506032b656e032100")

    // RFC8410 X25519  DER PKCS8 （16 ），
    private val X25519_PKCS8_PREFIX = hexToBytes("302e020100300506032b656e04220420")

    private val bcProvider: Provider = BouncyCastleProvider()

    fun generateEphemeralKeyPair(): KeyPair =
        KeyPairGenerator.getInstance("X25519", bcProvider).generateKeyPair()

    fun rawPublicKeyBytes(publicKey: PublicKey): ByteArray {
        val encoded = publicKey.encoded
        return encoded.copyOfRange(encoded.size - 32, encoded.size)
    }

    fun importPeerPublicKey(rawBytes: ByteArray): PublicKey {
        val spki = X25519_SPKI_PREFIX + rawBytes
        return KeyFactory.getInstance("X25519", bcProvider).generatePublic(X509EncodedKeySpec(spki))
    }

    // ； of  generateEphemeralKeyPair()
    internal fun importPrivateKeyForTest(rawBytes: ByteArray): PrivateKey {
        val pkcs8 = X25519_PKCS8_PREFIX + rawBytes
        return KeyFactory.getInstance("X25519", bcProvider).generatePrivate(PKCS8EncodedKeySpec(pkcs8))
    }

    fun computeSharedSecret(privateKey: PrivateKey, peerPublicKey: PublicKey): ByteArray {
        val agreement = KeyAgreement.getInstance("X25519", bcProvider)
        agreement.init(privateKey)
        agreement.doPhase(peerPublicKey, true)
        return agreement.generateSecret()
    }

    // HKDF ： HMAC-Extract  prk， (ephemeralPub||0x01) HMAC-Expand  16  AES-128
    fun deriveAesKey(sharedSecret: ByteArray, ephemeralPublicRaw: ByteArray): ByteArray {
        val prk = hmacSha256(ByteArray(32), if (sharedSecret.isEmpty()) ByteArray(32) else sharedSecret)
        val okm = hmacSha256(prk, ephemeralPublicRaw + byteArrayOf(1))
        return okm.copyOfRange(0, 16)
    }

    // AES/GCM/NoPadding：Java  of  doFinal  16  tag ， tag
    fun gcmEncrypt(key: ByteArray, iv: ByteArray, plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return cipher.doFinal(plaintext)
    }

    private fun hmacSha256(key: ByteArray, message: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(message)
    }

    // of ：HMAC-SHA256(XEAPI_SIGN_KEY, timestamp+nonce)
    fun sign(timestamp: String, nonce: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(XEAPI_SIGN_KEY.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val digest = mac.doFinal((timestamp + nonce).toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(digest)
    }

    fun aesEcbEncrypt(key: ByteArray, plaintext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"))
        return cipher.doFinal(plaintext)
    }

    fun aesEcbDecrypt(key: ByteArray, ciphertext: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"))
        return cipher.doFinal(ciphertext)
    }

    // of  encryptedData，
    fun decryptPublicKeyResponse(encryptedDataBase64: String): XeapiPublicKeyState {
        val plaintext = aesEcbDecrypt(XEAPI_STATIC_KEY, Base64.getDecoder().decode(encryptedDataBase64))
        val json = JSONObject(String(plaintext, Charsets.UTF_8))
        return XeapiPublicKeyState(
            version = json.optString("version", ""),
            publicKey = json.optString("publicKey", ""),
            sk = if (json.has("sk") && !json.isNull("sk")) json.getString("sk") else null
        )
    }

    // ：AES-ECB(eapiKey)  gzip
    fun decryptResponseBody(body: ByteArray): String {
        val decrypted = aesEcbDecrypt(EAPI_KEY.toByteArray(Charsets.UTF_8), body)
        val isGzip = decrypted.size >= 2 &&
            decrypted[0] == 0x1f.toByte() &&
            decrypted[1] == 0x8b.toByte()
        val plaintext = if (isGzip) {
            GZIPInputStream(ByteArrayInputStream(decrypted)).use { it.readBytes() }
        } else {
            decrypted
        }
        return String(plaintext, Charsets.UTF_8)
    }

    data class XeapiEncryptedForm(val b: String, val s: String, val r: String)

    private val secureRandom = SecureRandom()

    // application/x-www-form-urlencoded ， Map  of
    private fun encodeForm(data: Map<String, String>): String =
        data.entries.joinToString("&") { (key, value) ->
            "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
        }

    // of  JSON： xeapi 、 POST、 content-type，
    // body+queryString ，（body  queryString）
    internal fun buildPlaintext(uri: String, data: Map<String, String>): String {
        val bodyBase64 = Base64.getEncoder().encodeToString(encodeForm(data).toByteArray(Charsets.UTF_8))
        return "{\"body\":\"$bodyBase64\",\"queryString\":\"e_r=true\"}"
    }

    // ： XOR 、base64 ，
    internal fun midTransform(ciphertext: ByteArray, randomMask: ByteArray): ByteArray {
        val xored = ByteArray(ciphertext.size) { i -> (ciphertext[i].toInt() xor randomMask[i and 0x0f].toInt()).toByte() }
        val b64 = Base64.getEncoder().encodeToString(xored).toByteArray(Charsets.UTF_8)
        val rotate = if (b64.isNotEmpty()) (randomMask[0].toInt() and 0x0f) % b64.size else 0
        return randomMask + b64.copyOfRange(rotate, b64.size) + b64.copyOfRange(0, rotate)
    }

    private fun randomBytes(size: Int): ByteArray = ByteArray(size).also { secureRandom.nextBytes(it) }

    // xeapi  of  B/S/R 。 uri/data/publicKeyState/os  of ，
    // （CryptoInterceptor）， of
    fun assembleRequest(
        uri: String,
        data: Map<String, String>,
        publicKeyState: XeapiPublicKeyState,
        os: String = "android",
        dynamicKey: ByteArray = randomBytes(16),
        midTransformMask: ByteArray = randomBytes(16),
        ephemeralKeyPair: KeyPair = generateEphemeralKeyPair(),
        gcmIv: ByteArray = randomBytes(12)
    ): XeapiEncryptedForm {
        val plaintext = buildPlaintext(uri, data).toByteArray(Charsets.UTF_8)
        val innerCipher = aesEcbEncrypt(XEAPI_STATIC_KEY, plaintext)
        val transformed = midTransform(innerCipher, midTransformMask)
        val b = aesEcbEncrypt(dynamicKey, transformed)

        val ephemeralPubRaw = rawPublicKeyBytes(ephemeralKeyPair.public)
        val peerPublicKey = importPeerPublicKey(Base64.getDecoder().decode(publicKeyState.publicKey))
        val sharedSecret = computeSharedSecret(ephemeralKeyPair.private, peerPublicKey)
        val aesKey = deriveAesKey(sharedSecret, ephemeralPubRaw)
        val dynamicKeyBase64 = Base64.getEncoder().encodeToString(dynamicKey)
        val gcmPlaintext = "$dynamicKeyBase64|$os|${publicKeyState.sk ?: ""}".toByteArray(Charsets.UTF_8)
        val gcmOutput = gcmEncrypt(aesKey, gcmIv, gcmPlaintext)
        val s = ephemeralPubRaw + gcmIv + gcmOutput

        val r = aesEcbEncrypt(XEAPI_STATIC_KEY, "${publicKeyState.version}|".toByteArray(Charsets.UTF_8))

        return XeapiEncryptedForm(
            b = Base64.getEncoder().encodeToString(b),
            s = Base64.getEncoder().encodeToString(s),
            r = Base64.getEncoder().encodeToString(r)
        )
    }

    private fun hexToBytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) { i -> ((Character.digit(hex[i * 2], 16) shl 4) + Character.digit(hex[i * 2 + 1], 16)).toByte() }
}
