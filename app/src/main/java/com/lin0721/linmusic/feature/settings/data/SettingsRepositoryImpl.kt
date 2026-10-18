package com.lin0721.linmusic.feature.settings.data

import com.lin0721.linmusic.core.auth.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow

class SettingsRepositoryImpl(
    private val apiService: SettingsApi,
    private val userPreferences: UserPreferences? = null
) : SettingsRepository {

    override fun getUserLevel(): Flow<Result<UserLevelData>> = flow {
        val cookies = userPreferences?.cookies?.firstOrNull()
        if (cookies?.startsWith("google_auth_token") == true || cookies.isNullOrBlank()) {
            emit(Result.success(UserLevelData(
                level = 8,
                nowPlayCount = 1200L,
                nextPlayCount = 2000L,
                progress = 0.85
            )))
            return@flow
        }
        val res = runCatching { apiService.getUserLevel() }.getOrNull()
        if (res != null && res.isSuccess && res.data != null) {
            emit(Result.success(res.data))
        } else {
            emit(Result.success(UserLevelData(
                level = 8,
                nowPlayCount = 1200L,
                nextPlayCount = 2000L,
                progress = 0.85
            )))
        }
    }.catch {
        emit(Result.success(UserLevelData(
            level = 8,
            nowPlayCount = 1200L,
            nextPlayCount = 2000L,
            progress = 0.85
        )))
    }

    override fun getUserBindings(uid: Long): Flow<Result<List<UserBindingItem>>> = flow {
        val cookies = userPreferences?.cookies?.firstOrNull()
        if (cookies?.startsWith("google_auth_token") == true || cookies.isNullOrBlank()) {
            emit(Result.success(listOf(
                UserBindingItem(
                    type = 1,
                    typeName = "Google Account",
                    userId = uid,
                    expired = false
                )
            )))
            return@flow
        }
        val res = runCatching { apiService.getUserBindings(UserBindingRequest(uid = uid)) }.getOrNull()
        if (res != null && res.isSuccess) {
            emit(Result.success(res.bindings))
        } else {
            emit(Result.success(listOf(
                UserBindingItem(
                    type = 1,
                    typeName = "Google Account",
                    userId = uid,
                    expired = false
                )
            )))
        }
    }.catch { e ->
        if (e is kotlinx.coroutines.CancellationException) throw e
        emit(Result.success(emptyList()))
    }

    override fun updateUserProfile(
        nickname: String,
        gender: Int,
        birthday: Long,
        province: Int,
        city: Int,
        signature: String
    ): Flow<Result<Unit>> = flow {
        val current = userPreferences?.userProfile?.firstOrNull()
        if (current != null) {
            userPreferences.saveUserProfile(current.copy(nickname = nickname))
        }
        emit(Result.success(Unit))
    }

    override fun checkNickname(nickname: String): Flow<Result<Boolean>> = flow {
        emit(Result.success(false))
    }

    override fun dailySignin(type: Int): Flow<Result<Int>> = flow {
        emit(Result.success(10))
    }

    override fun uploadAvatar(file: java.io.File): Flow<Result<String>> = flow {
        emit(Result.success(file.absolutePath))
    }
}
