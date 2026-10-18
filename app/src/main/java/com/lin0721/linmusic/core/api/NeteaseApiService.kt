package com.lin0721.linmusic.core.api

import com.lin0721.linmusic.core.model.EmptyBody
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

// of  Retrofit （， core/auth ）。
interface NeteaseApiService {

    // Current
    @POST("/eapi/nuser/account/get")
    suspend fun getAccountInfo(
        @Body body: EmptyBody = EmptyBody()
    ): AccountInfoResponse


    @POST("/eapi/logout")
    suspend fun logoutApi(
        @Body body: EmptyBody = EmptyBody()
    ): LogoutApiResponse

    // key
    @POST("/weapi/login/qrcode/unikey")
    suspend fun getQrKey(
        @Body body: QrKeyRequest = QrKeyRequest()
    ): QrKeyResponse

    // ； Cookie  Set-Cookie ， Response
    @POST("/weapi/login/qrcode/client/login")
    suspend fun checkQrStatus(
        @Body body: QrCheckRequest
    ): Response<QrCheckResponse>
}

// =======================  =======================

@Serializable
data class AccountInfoResponse(
    val code: Int = 0,
    val account: Account? = null,
    val profile: UserProfile? = null
)

@Serializable
data class Account(
    val id: Long = 0,
    val userName: String = "",
    val type: Int = 0,
    val status: Int = 0,
)

@Serializable
data class UserProfile(
    val userId: Long = 0,
    val nickname: String = "",
    val avatarUrl: String = "",
    @SerialName("backgroundUrl")
    val backgroundUrl: String = "",
    val signature: String = "",
)

@Serializable
data class LogoutApiResponse(
    val code: Int = 0
) {
    val isSuccess: Boolean get() = code == 200
}

// =======================  =======================

// type  1， Web/PC
@Serializable
data class QrKeyRequest(val type: Int = 1)

@Serializable
data class QrKeyResponse(
    val code: Int = 0,
    val unikey: String = ""
)

@Serializable
data class QrCheckRequest(
    val key: String,
    val type: Int = 1
)

// code: 800  / 801  / 802  / 803
// cookies  JSON ， AuthRepositoryImpl  Set-Cookie
@Serializable
data class QrCheckResponse(
    val code: Int = 0,
    val message: String? = null,
    @Transient
    val cookies: String? = null
)
