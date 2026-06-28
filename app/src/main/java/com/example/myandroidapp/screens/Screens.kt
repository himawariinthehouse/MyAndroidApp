package com.example.myandroidapp.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.core.content.ContextCompat
import com.example.myandroidapp.data.AppDatabase
import com.example.myandroidapp.data.GaodeKeyEntity
import com.example.myandroidapp.data.GroupEntity
import com.example.myandroidapp.data.PlaceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.URL
import java.net.URLEncoder

private data class PlaceLocation(
    val id: Long,
    val name: String,
    val latitude: Double,
    val longitude: Double
)

private data class PlaceGroup(
    val name: String,
    val locations: List<PlaceLocation>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val dao = remember { database.gaodeKeyDao() }
    val placeDao = remember { database.placeDao() }
    val groupDao = remember { database.groupDao() }

    var mapKey by remember { mutableStateOf("") }
    var placeName by remember { mutableStateOf("") }
    var groupName by remember { mutableStateOf("") }
    var groupNames by remember { mutableStateOf(listOf("默认分组")) }
    var selectedGroupName by remember { mutableStateOf("默认分组") }
    var groups by remember { mutableStateOf(listOf<PlaceGroup>()) }
    var expandedGroups by remember { mutableStateOf(setOf<String>()) }
    var copyMessage by remember { mutableStateOf("") }
    var groupDropdownExpanded by remember { mutableStateOf(false) }
    var isGettingLocation by remember { mutableStateOf(false) }
    var showAddressDialog by remember { mutableStateOf(false) }
    var addressList by remember { mutableStateOf(listOf<String>()) }
    var longPressPlace by remember { mutableStateOf<PlaceLocation?>(null) }

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

    // 初始化：加载 API Key、分组和地点
    LaunchedEffect(Unit) {
        val keyEntity = withContext(Dispatchers.IO) { dao.getGaodeKey() }
        if (keyEntity != null) {
            mapKey = keyEntity.key
        }
        // 确保“默认分组”存在
        val existingGroups = withContext(Dispatchers.IO) { groupDao.getAllGroupNames() }
        if (existingGroups.isEmpty()) {
            withContext(Dispatchers.IO) { groupDao.insert(GroupEntity(name = "默认分组")) }
        }
        val savedGroupNames = withContext(Dispatchers.IO) { groupDao.getAllGroupNames() }
        val savedPlaces = withContext(Dispatchers.IO) { placeDao.getAllPlaces() }
        val grouped = savedPlaces.groupBy { it.groupName }.map { (name, places) ->
            PlaceGroup(
                name = name,
                locations = places.map { PlaceLocation(it.id, it.name, it.latitude, it.longitude) }
            )
        }
        groupNames = if (savedGroupNames.isEmpty()) listOf("默认分组") else savedGroupNames
        groups = grouped
        selectedGroupName = groupNames.first()
    }

    suspend fun refreshGroups() {
        val names = withContext(Dispatchers.IO) { groupDao.getAllGroupNames() }
        val places = withContext(Dispatchers.IO) { placeDao.getAllPlaces() }
        groupNames = if (names.isEmpty()) listOf("默认分组") else names
        groups = places.groupBy { it.groupName }.map { (name, placeList) ->
            PlaceGroup(
                name = name,
                locations = placeList.map { PlaceLocation(it.id, it.name, it.latitude, it.longitude) }
            )
        }
    }

    /**
     * WGS84 坐标转 GCJ02 坐标（高德坐标系）
     * 使用高德坐标转换 API
     * @return Pair(gcjLat, gcjLng) 或失败时返回原始坐标
     */
    suspend fun convertToGcj02(apiKey: String, lat: Double, lng: Double): Pair<Double, Double> {
        return withContext(Dispatchers.IO) {
            try {
                val coords = "$lng,$lat"
                val urlStr = "https://restapi.amap.com/v3/assistant/coordinate/convert?output=json&coordsys=gps&locations=${URLEncoder.encode(coords, "UTF-8")}&key=${URLEncoder.encode(apiKey, "UTF-8")}"
                val url = URL(urlStr)
                val reader = BufferedReader(url.openStream().bufferedReader())
                val response = reader.readText()
                reader.close()
                val json = JSONObject(response)
                if (json.optString("status") == "1") {
                    val locations = json.optString("locations")
                    if (locations.isNotEmpty()) {
                        val parts = locations.split(",")
                        if (parts.size == 2) {
                            val gcjLng = parts[0].toDouble()
                            val gcjLat = parts[1].toDouble()
                            return@withContext Pair(gcjLat, gcjLng)
                        }
                    }
                }
                Pair(lat, lng)
            } catch (e: Exception) {
                e.printStackTrace()
                Pair(lat, lng)
            }
        }
    }

    suspend fun reverseGeocodeWithPois(apiKey: String, lat: Double, lng: Double): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val location = "$lng,$lat"
                val urlStr = "https://restapi.amap.com/v3/geocode/regeo?output=json&location=${URLEncoder.encode(location, "UTF-8")}&key=${URLEncoder.encode(apiKey, "UTF-8")}&radius=50&extensions=all"
                val url = URL(urlStr)
                val reader = BufferedReader(url.openStream().bufferedReader())
                val response = reader.readText()
                reader.close()
                val json = JSONObject(response)
                val result = mutableListOf<String>()
                if (json.optString("status") == "1") {
                    val regeocode = json.getJSONObject("regeocode")
                    val mainAddress = regeocode.optString("formatted_address")
                    if (mainAddress.isNotEmpty()) {
                        result.add(mainAddress)
                    }
                    val pois = regeocode.optJSONArray("pois")
                    if (pois != null) {
                        for (i in 0 until pois.length()) {
                            val poi = pois.getJSONObject(i)
                            val poiName = poi.optString("name")
                            if (poiName.isNotEmpty() && !result.contains(poiName)) {
                                result.add(poiName)
                            }
                        }
                    }
                }
                result
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
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
                    return@Button
                }
                if (mapKey.trim().isEmpty()) {
                    Toast.makeText(context, "请先设置地图 API Key", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                isGettingLocation = true
                requestCurrentLocation(context) { location ->
                    isGettingLocation = false
                    if (location == null) {
                        Toast.makeText(context, "无法获取当前位置", Toast.LENGTH_SHORT).show()
                        return@requestCurrentLocation
                    }
                    scope.launch {
                        val (gcjLat, gcjLng) = convertToGcj02(mapKey.trim(), location.latitude, location.longitude)
                        val addresses = reverseGeocodeWithPois(mapKey.trim(), gcjLat, gcjLng)
                        if (addresses.size > 1) {
                            addressList = addresses
                            showAddressDialog = true
                        } else if (addresses.isNotEmpty()) {
                            placeName = addresses.first()
                        } else {
                            placeName = "$gcjLat, $gcjLng"
                        }
                    }
                }
            }) {
                Text(if (isGettingLocation) "定位中..." else "获取当前定位")
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
                groupNames.contains(trimmedName) -> Toast.makeText(context, "该分组已存在", Toast.LENGTH_SHORT).show()
                else -> {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            groupDao.insert(GroupEntity(name = trimmedName))
                        }
                        refreshGroups()
                        selectedGroupName = trimmedName
                        groupName = ""
                        Toast.makeText(context, "已新增分组", Toast.LENGTH_SHORT).show()
                    }
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

        Text(text = "选择分组", fontSize = 14.sp)
        Spacer(modifier = Modifier.height(4.dp))
        ExposedDropdownMenuBox(
            expanded = groupDropdownExpanded,
            onExpandedChange = { groupDropdownExpanded = !groupDropdownExpanded }
        ) {
            OutlinedTextField(
                value = selectedGroupName,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = groupDropdownExpanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = groupDropdownExpanded,
                onDismissRequest = { groupDropdownExpanded = false }
            ) {
                groupNames.forEach { name ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            selectedGroupName = name
                            groupDropdownExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = {
            val trimmedPlaceName = placeName.trim()
            val groupNameValue = selectedGroupName

            if (trimmedPlaceName.isEmpty()) {
                Toast.makeText(context, "请先获取当前位置", Toast.LENGTH_SHORT).show()
                return@Button
            }
            if (!permissionGranted.value) {
                Toast.makeText(context, "请先授予定位权限", Toast.LENGTH_SHORT).show()
                locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                return@Button
            }

            requestCurrentLocation(context) { location ->
                if (location == null) {
                    Toast.makeText(context, "无法获取当前位置", Toast.LENGTH_SHORT).show()
                    return@requestCurrentLocation
                }

                scope.launch {
                    val (gcjLat, gcjLng) = convertToGcj02(mapKey.trim(), location.latitude, location.longitude)
                    withContext(Dispatchers.IO) {
                        placeDao.insert(
                            PlaceEntity(
                                name = trimmedPlaceName,
                                latitude = gcjLat,
                                longitude = gcjLng,
                                groupName = groupNameValue
                            )
                        )
                    }
                    refreshGroups()
                    placeName = ""
                    copyMessage = "已保存位置到 $groupNameValue"
                }
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
            Column {
                groups.forEach { group ->
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
                                                            longPressPlace = location
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

    // 长按地点弹框
    longPressPlace?.let { place ->
        AlertDialog(
            onDismissRequest = { longPressPlace = null },
            title = { Text(place.name) },
            text = {
                Text("经度: ${place.longitude}\n纬度: ${place.latitude}", fontSize = 14.sp)
            },
            confirmButton = {
                TextButton(onClick = {
                    // 跳转到高德地图
                    try {
                        val uri = Uri.parse(
                            "androidamap://viewMap?sourceApplication=MyAndroidApp" +
                                    "&poiname=${URLEncoder.encode(place.name, "UTF-8")}" +
                                    "&lat=${place.latitude}&lon=${place.longitude}&dev=0"
                        )
                        val intent = Intent(Intent.ACTION_VIEW, uri)
                        intent.setPackage("com.autonavi.minimap")
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "未安装高德地图App", Toast.LENGTH_SHORT).show()
                    }
                    longPressPlace = null
                }) {
                    Text("高德地图")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) {
                            placeDao.deleteById(place.id)
                        }
                        refreshGroups()
                        Toast.makeText(context, "已删除 ${place.name}", Toast.LENGTH_SHORT).show()
                    }
                    longPressPlace = null
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // 地址选择弹框
    if (showAddressDialog) {
        AlertDialog(
            onDismissRequest = { showAddressDialog = false },
            title = { Text("选择地址") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    addressList.forEach { address ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    placeName = address
                                    showAddressDialog = false
                                },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = address,
                                modifier = Modifier.padding(12.dp),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddressDialog = false }) {
                    Text("取消")
                }
            }
        )
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
