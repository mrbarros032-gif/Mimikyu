package com.lin0721.linmusic.core.network

import com.lin0721.linmusic.R

// AppError  of ， strings.xml
fun AppError.toMessage(resourceProvider: ResourceProvider): String = when (this) {
    AppError.NetworkError -> resourceProvider.getString(R.string.app_error_network)
    AppError.RiskControl -> resourceProvider.getString(R.string.app_error_risk_control)
    AppError.Unauthorized -> resourceProvider.getString(R.string.app_error_unauthorized)
    AppError.ParseError -> resourceProvider.getString(R.string.app_error_parse)
    is AppError.BizError -> resourceProvider.getString(R.string.app_error_biz_default)
}

// ：Repository  AppError， AppError  message
fun Throwable.toUserMessage(resourceProvider: ResourceProvider): String =
    (this as? AppError)?.toMessage(resourceProvider) ?: (message ?: resourceProvider.getString(R.string.app_error_biz_default))
