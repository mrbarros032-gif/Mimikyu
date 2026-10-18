package com.lin0721.linmusic.core.network

import android.content.Context
import androidx.annotation.StringRes

// Context  of  ViewModel ； applicationContext，
open class ResourceProvider(private val context: Context? = null) {
    open fun getString(@StringRes resId: Int): String = context?.getString(resId).orEmpty()
}
