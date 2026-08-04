package com.arroom.characters.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.arroom.characters.R

/**
 * Камера — единственное обязательное разрешение.
 * Запрашиваем сразу при старте: без неё приложение бессмысленно.
 */
@Composable
fun CameraPermissionGate(content: @Composable () -> Unit) {
    val context = LocalContext.current

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var asked by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { result ->
        granted = result
        asked = true
    }

    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(Manifest.permission.CAMERA)
    }

    if (granted) {
        content()
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0B12)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Rounded.CameraAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(20.dp))
            Text(
                stringResource(R.string.perm_title),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )
            Spacer(Modifier.height(10.dp))
            Text(
                stringResource(R.string.perm_body),
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFB9B9C6),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))
            Button(
                onClick = {
                    if (asked) {
                        // Пользователь отказал — ведём в системные настройки
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                        )
                    } else {
                        launcher.launch(Manifest.permission.CAMERA)
                    }
                }
            ) {
                Text(stringResource(if (asked) R.string.perm_settings else R.string.perm_allow))
            }
        }
    }
}
