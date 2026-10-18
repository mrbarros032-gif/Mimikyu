package com.lin0721.linmusic.feature.settings.data

import com.lin0721.linmusic.core.model.EmptyBody
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import okhttp3.MultipartBody

// 、、 and  of  Retrofit 。
interface SettingsApi {


    @POST("/eapi/user/level")
    suspend fun getUserLevel(
        @Body body: EmptyBody = EmptyBody()
    ): UserLevelResponse


    @POST("/eapi/user/binding")
    suspend fun getUserBindings(
        @Body body: UserBindingRequest
    ): UserBindingResponse


    @POST("/eapi/user/update")
    suspend fun updateUserProfile(
        @Body body: UserProfileUpdateRequest
    ): UserProfileUpdateResponse


    @POST("/eapi/nickname/check")
    suspend fun checkNickname(
        @Body body: NicknameCheckRequest
    ): NicknameCheckResponse


    @POST("/eapi/point/dailyTask")
    suspend fun dailySignin(
        @Body body: DailySigninRequest
    ): DailySigninResponse

    // ( MultipartBody )
    @Multipart
    @POST("/weapi/user/avatar/upload/v1")
    suspend fun uploadAvatar(
        @Part imgFile: MultipartBody.Part
    ): AvatarUploadResponse
}

// ======================= Settings & Privacy DTO =======================

@Serializable
data class UserLevelResponse(
    val code: Int = 0,
    val data: UserLevelData? = null
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class UserLevelData(
    val level: Int = 0,
    val nextPlayCount: Long = 0,
    val nextLoginCount: Int = 0,
    val nowPlayCount: Long = 0,
    val nowLoginCount: Int = 0,
    val progress: Double = 0.0
)

@Serializable
data class UserBindingRequest(
    val uid: Long
)

@Serializable
data class UserBindingResponse(
    val code: Int = 0,
    val bindings: List<UserBindingItem> = emptyList()
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class UserBindingItem(
    val type: Int = 0, // 1-, 2-, 5-, 10-, 20-QQ, 1000-
    val typeName: String = "",
    val userId: Long = 0,
    val expired: Boolean = false
)

@Serializable
data class UserProfileUpdateRequest(
    val nickname: String,
    val gender: Int,
    val birthday: Long,
    val province: Int,
    val city: Int,
    val signature: String
)

@Serializable
data class UserProfileUpdateResponse(
    val code: Int = 0
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class NicknameCheckRequest(
    val nickname: String
)

@Serializable
data class NicknameCheckResponse(
    val code: Int = 0,
    val duplicated: Boolean = false,
    val candidateNicknames: List<String> = emptyList()
) {
    val isSuccess: Boolean get() = code == 200
}

@Serializable
data class DailySigninRequest(
    val type: Int = 0
)

@Serializable
data class DailySigninResponse(
    val code: Int = 0,
    val point: Int = 0,
    val msg: String? = null
) {
    val isSuccess: Boolean get() = code == 200 || code == -2
}

@Serializable
data class AvatarUploadResponse(
    val code: Int = 0,
    val url: String? = null
) {
    val isSuccess: Boolean get() = code == 200
}
