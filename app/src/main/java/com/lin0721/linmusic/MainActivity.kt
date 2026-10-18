package com.lin0721.linmusic

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.lin0721.linmusic.core.player.FloatingLyricService
import com.lin0721.linmusic.core.preferences.SettingsPreferences
import com.lin0721.linmusic.core.ui.theme.MimikyuTheme
import com.lin0721.linmusic.feature.account.ui.ProfilePreviewBottomSheet
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val settingsPreferences: SettingsPreferences by inject()

    private var deepLinkTargetUid by mutableStateOf<String?>(null)
    private var deepLinkTargetUsername by mutableStateOf<String?>(null)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun attachBaseContext(newBase: Context) {
        val locale = Locale.ENGLISH
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (BuildConfig.DEBUG) {
            android.os.StrictMode.setThreadPolicy(
                android.os.StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }
        super.onCreate(savedInstanceState)
        // App ，，
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        requestNotificationPermissionIfNeeded()
        handleDeepLink(intent)

        lifecycleScope.launch {
            settingsPreferences.showDesktopLrc.collectLatest { enabled ->
                if (enabled) {
                    startFloatingLyricOrRequestPermission()
                } else {
                    stopService(Intent(this@MainActivity, FloatingLyricService::class.java))
                }
            }
        }

        setContent {
            MimikyuTheme {
                MimikyuApp()

                if (deepLinkTargetUid != null || deepLinkTargetUsername != null) {
                    ProfilePreviewBottomSheet(
                        targetUid = deepLinkTargetUid,
                        targetUsername = deepLinkTargetUsername,
                        onDismiss = {
                            deepLinkTargetUid = null
                            deepLinkTargetUsername = null
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        val uid = uri.getQueryParameter("uid")
        val pathSegments = uri.pathSegments
        val username = when {
            uri.scheme == "mimikyu" && uri.host == "u" -> {
                pathSegments.firstOrNull()
            }
            pathSegments.isNotEmpty() && pathSegments[0] == "u" -> {
                if (pathSegments.size > 1) pathSegments[1] else null
            }
            else -> uri.lastPathSegment
        }

        if (!uid.isNullOrBlank() || !username.isNullOrBlank()) {
            deepLinkTargetUid = uid
            deepLinkTargetUsername = username
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val enabled = settingsPreferences.showDesktopLrc.first()
            if (enabled && android.provider.Settings.canDrawOverlays(this@MainActivity)) {
                startService(Intent(this@MainActivity, FloatingLyricService::class.java))
            }
        }
    }

    // Android 13+
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // ，
    private fun startFloatingLyricOrRequestPermission() {
        if (android.provider.Settings.canDrawOverlays(this)) {
            startService(Intent(this, FloatingLyricService::class.java))
            return
        }
        android.widget.Toast.makeText(
            this,
            "Please grant overlay permission to display desktop lyrics",
            android.widget.Toast.LENGTH_LONG
        ).show()
        startActivity(
            Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")
            )
        )
    }
}
