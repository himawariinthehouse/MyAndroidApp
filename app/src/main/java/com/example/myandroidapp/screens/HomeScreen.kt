package com.example.myandroidapp.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.myandroidapp.navigation.Screen
import com.example.myandroidapp.service.SilentAudioService

@Composable
fun HomeScreen(navController: NavHostController) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(SilentAudioService.isRunning) }

    fun startSilentPlayback() {
        ContextCompat.startForegroundService(context, SilentAudioService.startIntent(context))
        isPlaying = true
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startSilentPlayback()
        } else {
            Toast.makeText(context, "通知权限未授予，无法启动后台播放", Toast.LENGTH_SHORT).show()
        }
    }

    fun onPlayButtonClick() {
        if (isPlaying) {
            context.stopService(SilentAudioService.stopIntent(context))
            isPlaying = false
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startSilentPlayback()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "主页导航",
            fontSize = 32.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = { navController.navigate(Screen.Transport.route) },
            modifier = Modifier
                .padding(8.dp)
                .padding(horizontal = 20.dp)
        ) {
            Text("交通", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navController.navigate(Screen.Time.route) },
            modifier = Modifier
                .padding(8.dp)
                .padding(horizontal = 20.dp)
        ) {
            Text("时间", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { navController.navigate(Screen.Health.route) },
            modifier = Modifier
                .padding(8.dp)
                .padding(horizontal = 20.dp)
        ) {
            Text("健康", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onPlayButtonClick() },
            modifier = Modifier
                .padding(8.dp)
                .padding(horizontal = 20.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isPlaying) "停止播放" else "播放音频", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isPlaying) "静音音频播放中，应用将在后台持续运行" else "播放静音音频可保持应用在后台运行",
            fontSize = 12.sp
        )
    }
}
