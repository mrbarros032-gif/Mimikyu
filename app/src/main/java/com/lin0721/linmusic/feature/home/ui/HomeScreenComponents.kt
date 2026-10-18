package com.lin0721.linmusic.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lin0721.linmusic.core.ui.components.MimikyuButton
import com.lin0721.linmusic.core.ui.theme.MimikyuSpacing

// of
@Composable
fun SectionHeader(title: String, showAction: Boolean = true) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 36.dp, bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(text = title, color = MaterialTheme.colorScheme.onSurface, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (showAction) { Text(text = "Show All", color = Color.Gray, fontSize = 12.sp) }
    }
}

@Composable
fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
}

@Composable
fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(MimikyuSpacing.xl), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Failed to load data", color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
        Text(message, color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = MimikyuSpacing.sm))
        MimikyuButton(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)) { Text("Retry") }
    }
}
