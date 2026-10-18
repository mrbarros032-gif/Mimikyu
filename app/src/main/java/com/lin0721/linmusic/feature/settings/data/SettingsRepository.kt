package com.lin0721.linmusic.feature.settings.data

import kotlinx.coroutines.flow.Flow

// 、 and （settings ）
interface SettingsRepository {


    fun getUserLevel(): Flow<Result<UserLevelData>>


    fun getUserBindings(uid: Long): Flow<Result<List<UserBindingItem>>>


    fun updateUserProfile(nickname: String, gender: Int, birthday: Long, province: Int, city: Int, signature: String): Flow<Result<Unit>>


    fun checkNickname(nickname: String): Flow<Result<Boolean>>


    fun dailySignin(type: Int): Flow<Result<Int>> // Back of


    fun uploadAvatar(file: java.io.File): Flow<Result<String>>
}
