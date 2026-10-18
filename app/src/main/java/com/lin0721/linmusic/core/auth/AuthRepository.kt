package com.lin0721.linmusic.core.auth

import com.lin0721.linmusic.core.api.AccountInfoResponse
import com.lin0721.linmusic.core.api.QrCheckResponse
import com.lin0721.linmusic.core.api.QrKeyResponse
import kotlinx.coroutines.flow.Flow

// and （）
interface AuthRepository {

    // Current
    fun getAccountInfo(): Flow<Result<AccountInfoResponse>>


    fun logout(): Flow<Result<Unit>>

    // key
    fun getQrKey(): Flow<Result<QrKeyResponse>>

    // ，803  QrCheckResponse.cookies  of  Cookie
    fun checkQrStatus(key: String): Flow<Result<QrCheckResponse>>
}
