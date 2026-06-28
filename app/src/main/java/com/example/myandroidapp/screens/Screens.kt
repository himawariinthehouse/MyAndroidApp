package com.example.myandroidapp.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.core.content.ContextCompat
import com.example.myandroidapp.data.AppDatabase
import com.example.myandroidapp.data.GaodeKeyEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class PlaceLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

private data class PlaceGroup(
    val name: String,
    val locations: List<PlaceLocation>
)

@Composable
fun TransportScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val dao = remember { database.gaodeKeyDao() }

    var mapKey by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val entity = withContext(Dispatchers.IO) { dao.getGaodeKey() }
        if (entity != null) {
            mapKey = entity.key
        }
    }

    val permissionGranted = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted.value = granted
        if (!granted) {
            Toast.makeText(context, "位置权限未授予", Toast.LENGTH_SHORT).show()
        }
    }

    var groupName by remember { mutableStateOf("") }
    var placeName by remember { mutableStateOf("") }
    var selectedGroupName by remember { mutableStateOf<String?>(null) }
    var groups by remember { mutableStateOf(listOf<PlaceGroup>()) }
    var expandedGroups by remember { mutableStateOf(setOf<String>()) }
    var copyMessage by remember { mutableStateOf("") }

    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "返回")
        }

        Text(text = "交通页面", fontSize = 28.sp, modifier = Modifier.padding(bottom = 16.dp))

        Text(text = "地图 API Key", fontSize = 16.sp)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = mapKey,
            onValueChange = { mapKey = it },
            label = { Text("请输入高德地图 API Key") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            val trimmedKey = mapKey.trim()
            if (trimmedKey.isEmpty()) {
                Toast.makeText(context, "请输入有效的 API Key", Toast.LENGTH_SHORT).show()
                return@Button
            }
            scope.launch {
                withContext(Dispatchers.IO) {
                    dao.insertOrUpdate(GaodeKeyEntity(id = 1, key = trimmedKey))
                }
                Toast.makeText(context, "地图 API Key 已保存", Toast.LENGTH_SHORT).show()
            }
        }) {
            Text("保存 API Key")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = if (permissionGranted.value) "位置权限已授予" else "请授予定位权限以保存当前位置", fontSize = 14.sp)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (!permissionGranted.value) {
                    locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                } else {
                    Toast.makeText(context, "定位权限已授予", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text(if (permissionGranted.value) "定位权限已授予" else "请求定位权限")
            }
            Button(onClick = {
                if (!permissionGranted.value) {
                    locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                } else {
                    Toast.makeText(context, "请使用“新增地点”按钮保存当前位置", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("获取当前定位")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = groupName,
            onValueChange = { groupName = it },
            label = { Text("分组名称") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            val trimmedName = groupName.trim()
            when {
                trimmedName.isEmpty() -> Toast.makeText(context, "请输入分组名", Toast.LENGTH_SHORT).show()
                groups.any { it.name == trimmedName } -> Toast.makeText(context, "该分组已存在", Toast.LENGTH_SHORT).show()
                else -> {
                    groups = groups + PlaceGroup(name = trimmedName, locations = emptyList())
                    selectedGroupName = trimmedName
                    groupName = ""
                    Toast.makeText(context, "已新增分组", Toast.LENGTH_SHORT).show()
                }
            }
        }) {
            Text("新增分组")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "新增地点", fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(
            value = placeName,
            onValueChange = { placeName = it },
            label = { Text("地点名称") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(text = "选择分组：", modifier = Modifier.weight(1f))
            TextButton(onClick = {
                if (groups.isNotEmpty()) {
                    val currentIndex = groups.indexOfFirst { it.name == selectedGroupName }
                    selectedGroupName = if (currentIndex == -1 || currentIndex == groups.lastIndex) groups.first().name else groups[currentIndex + 1].name
                }
            }) {
                Text(selectedGroupName ?: "请先添加分组")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            val trimmedPlaceName = placeName.trim()
            val groupNameValue = selectedGroupName

            if (trimmedPlaceName.isEmpty()) {
                Toast.makeText(context, "请输入地点名称", Toast.LENGTH_SHORT).show()
                return@Button
            }
            if (groupNameValue.isNullOrEmpty()) {
                Toast.makeText(context, "请选择一个分组", Toast.LENGTH_SHORT).show()
                return@Button
            }
            if (!permissionGranted.value) {
                Toast.makeText(context, "请先授予定位权限", Toast.LENGTH_SHORT).show()
                locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                return@Button
            }

            requestCurrentLocation(context) { location ->
                if (location == null) {
                    Toast.makeText(context, "无法获取当前位置，请检查定位设置", Toast.LENGTH_SHORT).show()
                    return@requestCurrentLocation
                }

                groups = groups.map { group ->
                    if (group.name == groupNameValue) {
                        group.copy(
                            locations = group.locations + PlaceLocation(
                                name = trimmedPlaceName,
                                latitude = location.latitude,
                                longitude = location.longitude
                            )
                        )
                    } else {
                        group
                    }
                }
                placeName = ""
                copyMessage = "已保存当前位置到 $groupNameValue"
            }
        }) {
            Text("保存当前地点")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(text = "地点分组列表", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))

        if (groups.isEmpty()) {
            Text(text = "目前暂无分组，请先创建分组", fontSize = 14.sp)
        } else {
            LazyColumn(modifier = Modifier.fillMaxHeight()) {
                items(groups, key = { it.name }) { group ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                expandedGroups = if (expandedGroups.contains(group.name)) {
                                    expandedGroups - group.name
                                } else {
                                    expandedGroups + group.name
                                }
                            },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = group.name, fontSize = 16.sp, modifier = Modifier.weight(1f))
                                Icon(
                                    imageVector = if (expandedGroups.contains(group.name)) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = null
                                )
                            }
                            if (expandedGroups.contains(group.name)) {
                                Spacer(modifier = Modifier.height(8.dp))
                                if (group.locations.isEmpty()) {
                                    Text(text = "该分组暂无地点", fontSize = 14.sp)
                                } else {
                                    group.locations.forEach { location ->
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                                .padding(10.dp)
                                                .pointerInput(location) {
                                                    detectTapGestures(
                                                        onLongPress = {
                                                            val copiedText = "${location.latitude},${location.longitude}"
                                                            clipboardManager.setText(AnnotatedString(copiedText))
                                                            copyMessage = "已复制 ${location.latitude}, ${location.longitude}"
                                                            Toast.makeText(context, "已复制经纬度", Toast.LENGTH_SHORT).show()
                                                        }
                                                    )
                                                }
                                        ) {
                                            Text(text = location.name, fontSize = 15.sp)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = "经度: ${location.longitude}", fontSize = 13.sp)
                                            Text(text = "纬度: ${location.latitude}", fontSize = 13.sp)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (copyMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = copyMessage, fontSize = 14.sp)
        }
    }
}

private fun requestCurrentLocation(context: Context, callback: (Location?) -> Unit) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (locationManager == null) {
        callback(null)
        return
    }

    getLastKnownLocation(context)?.let {
        callback(it)
        return
    }

    val mainHandler = Handler(Looper.getMainLooper())
    val listener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            locationManager.removeUpdates(this)
            mainHandler.removeCallbacksAndMessages(null)
            callback(location)
        }

        override fun onProviderEnabled(provider: String) {}
        override fun onProviderDisabled(provider: String) {}
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    }

    val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
    )
    var requested = false
    providers.forEach { provider ->
        try {
            if (locationManager.isProviderEnabled(provider)) {
                requested = true
                locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            }
        } catch (_: SecurityException) {
        }
    }

    if (!requested) {
        callback(null)
        return
    }

    mainHandler.postDelayed({
        locationManager.removeUpdates(listener)
        callback(null)
    }, 8000)
}

private fun getLastKnownLocation(context: Context): Location? {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
    val providers = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER,
        LocationManager.PASSIVE_PROVIDER
    )
    providers.forEach { provider ->
        try {
            val location = locationManager.getLastKnownLocation(provider)
            if (location != null) return location
        } catch (ignored: SecurityException) {
        }
    }
    return null
}

@Composable
fun TimeScreen(navController: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "返回"
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "时间页面",
                fontSize = 28.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            Text(
                text = "这是时间相关的内容",
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun HealthScreen(navController: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.Start
    ) {
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "返回"
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "健康页面",
                fontSize = 28.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            Text(
                text = "这是健康相关的内容",
                fontSize = 16.sp
            )
        }
    }
}
