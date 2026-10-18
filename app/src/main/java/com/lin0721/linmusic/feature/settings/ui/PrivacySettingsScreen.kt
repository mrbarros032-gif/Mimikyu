package com.lin0721.linmusic.feature.settings.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.ui.theme.TextGray
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing

@Composable
fun PrivacySettingsView(viewModel: SettingsViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(MimikyuSpacing.md),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Privacy settings in development",
            color = TextGray,
            fontSize = 14.sp
        )
    }
}

