package com.lin0721.linmusic.core.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.lin0721.linmusic.core.ui.theme.BackgroundBlack
import com.lin0721.linmusic.core.ui.theme.NeteaseRed
import com.lin0721.linmusic.core.ui.theme.SurfaceDark
import com.lin0721.linmusic.core.ui.theme.WebLoginBackground
import com.lin0721.linmusic.core.network.NeteaseEndpoints
import com.lin0721.linmusic.core.network.RealIpProvider
import com.lin0721.linmusic.core.preferences.SettingsPreferences
import com.lin0721.linmusic.core.ui.theme.ScreenSlideDurationMs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import org.koin.compose.koinInject

// "Phone Login" style="width: Nvh; height: Mvh"
private const val LOGIN_BUTTON_FIX_JS = """
(function() {
  try {
    var btn = document.querySelector('[data-log*="btn_h5_login_method"]');
    if (btn) {
      btn.style.setProperty('width', '86%', 'important');
      btn.style.setProperty('height', '48px', 'important');
    }
  } catch (e) {}
})();
"""

/**
 * 
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewLoginScreen(
    onClose: () -> Unit,
    onLoginSuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }

    // UA（ wv/WebView ）
    val baseUA = "Mozilla/5.0 (Linux; Android 13; Pixel 7 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Mobile Safari/537.36"

    val loginUrl = NeteaseEndpoints.LOGIN_URL

    // IP  of "Use Real IP"， and  RealIpProvider
    val settingsPreferences: SettingsPreferences = koinInject()
    val realIpProvider: RealIpProvider = koinInject()
    var extraHeaders by remember { mutableStateOf<Map<String, String>?>(null) }
    LaunchedEffect(Unit) {
        val useRealIp = settingsPreferences.useRealIp.first()
        val realIpValue = settingsPreferences.realIpValue.first()
        val ip = realIpProvider.resolveIp(useRealIp, realIpValue)
        extraHeaders = if (ip != null) {
            mapOf("X-Real-IP" to ip, "X-Forwarded-For" to ip)
        } else {
            emptyMap()
        }
    }
    val resolvedHeaders = extraHeaders

    // WebView Create，Create，
    var slideInFinished by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(ScreenSlideDurationMs.toLong())
        slideInFinished = true
    }
    val readyToLoadWebView = resolvedHeaders != null && slideInFinished

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Secure Login",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    MimikyuIconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundBlack
                )
            )
        },
        containerColor = WebLoginBackground // ，/
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(WebLoginBackground)
        ) {
            if (readyToLoadWebView) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            // ：Clear
                            CookieManager.getInstance().let { manager ->
                                manager.setAcceptCookie(true)
                                manager.setAcceptThirdPartyCookies(this, true)
                                manager.removeAllCookies(null)
                                manager.flush()
                            }

                            // WebView ， resize
                            setBackgroundColor(android.graphics.Color.parseColor("#F5F5F7"))

                            webChromeClient = WebChromeClient()

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                // UA
                                userAgentString = baseUA
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    // Token (MUSIC_U)
                                    checkCookies(onLoginSuccess)
                                    isLoading = false // ，Close
                                    view?.evaluateJavascript(LOGIN_BUTTON_FIX_JS, null)
                                }

                                override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                                    checkCookies(onLoginSuccess)
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    return false
                                }
                            }

                            loadUrl(loginUrl, resolvedHeaders)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }


            AnimatedVisibility(
                visible = isLoading,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                // ，
                Box(modifier = Modifier.fillMaxSize().background(WebLoginBackground), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeteaseRed)
                }
            }
        }
    }
}

private fun checkCookies(onLoginSuccess: (String) -> Unit) {
    val cookieManager = CookieManager.getInstance()
    val cookies = cookieManager.getCookie(NeteaseEndpoints.WEB_BASE_URL)

    // ： MUSIC_U=
    if (cookies != null && cookies.contains("MUSIC_U=")) {
        onLoginSuccess(cookies)
    }
}
