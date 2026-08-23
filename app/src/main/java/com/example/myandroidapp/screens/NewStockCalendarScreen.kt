package com.example.myandroidapp.screens

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.myandroidapp.data.AppDatabase
import com.example.myandroidapp.data.StockCalendarEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private data class NewIssueItem(
    val type: String,
    val securityCode: String,
    val name: String,
    val issueDate: String,
    val market: String
)

@Composable
fun NewStockCalendarScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { AppDatabase.getDatabase(context) }
    val dao = remember { database.stockCalendarDao() }

    var savedItems by remember { mutableStateOf(listOf<StockCalendarEntity>()) }
    var fetchedItems by remember { mutableStateOf(listOf<NewIssueItem>()) }
    var isFetching by remember { mutableStateOf(false) }

    var calendarPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    var pendingCalendarEntity by remember { mutableStateOf<StockCalendarEntity?>(null) }
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        calendarPermissionGranted = granted
        if (granted) {
            pendingCalendarEntity?.let { addCalendar(it) }
            pendingCalendarEntity = null
        } else {
            Toast.makeText(context, "未授予日历权限，无法添加日历", Toast.LENGTH_SHORT).show()
        }
    }

    fun addCalendar(entity: StockCalendarEntity) {
        if (!calendarPermissionGranted) {
            pendingCalendarEntity = entity
            calendarPermissionLauncher.launch(Manifest.permission.WRITE_CALENDAR)
            return
        }
        scope.launch {
            val eventId = addIssueToCalendar(context, entity)
            if (eventId != null) {
                withContext(Dispatchers.IO) {
                    dao.updateCalendarEventId(entity.type, entity.securityCode, eventId)
                }
                refreshSaved()
                Toast.makeText(context, "已添加日历: ${entity.name}", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "添加日历失败，请检查系统日历是否可用", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun refreshSaved() {
        scope.launch {
            savedItems = withContext(Dispatchers.IO) { dao.getAll() }
        }
    }

    LaunchedEffect(Unit) {
        refreshSaved()
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

        Text(text = "新股日历", fontSize = 28.sp, modifier = Modifier.padding(bottom = 8.dp))
        Text(
            text = "获取已知未来 A 股新股 / 新债（可转债）发行信息",
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                isFetching = true
                scope.launch {
                    val items = fetchNewIssues()
                    fetchedItems = items
                    isFetching = false
                    if (items.isEmpty()) {
                        Toast.makeText(context, "未获取到数据，请稍后重试", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "获取到 ${items.size} 条数据", Toast.LENGTH_SHORT).show()
                    }
                }
            }) {
                Text(if (isFetching) "获取中..." else "获取最新数据")
            }

            Button(onClick = {
                if (fetchedItems.isEmpty()) {
                    Toast.makeText(context, "请先获取最新数据", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                scope.launch {
                    withContext(Dispatchers.IO) {
                        dao.upsertAll(
                            fetchedItems.map { item ->
                                StockCalendarEntity(
                                    type = item.type,
                                    securityCode = item.securityCode,
                                    name = item.name,
                                    issueDate = item.issueDate,
                                    market = item.market
                                )
                            }
                        )
                    }
                    refreshSaved()
                    Toast.makeText(context, "已保存/更新 ${fetchedItems.size} 条到本地数据库", Toast.LENGTH_SHORT).show()
                }
            }) {
                Text("一键保存/更新到本地")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isFetching) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.size(8.dp))
                Text("正在获取新股日历数据...", fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (fetchedItems.isNotEmpty()) {
            Text(
                text = "获取到的数据（${fetchedItems.size} 条）",
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            fetchedItems.forEach { item ->
                IssueCard(item.type, item.name, item.securityCode, item.issueDate, item.market)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Text(
            text = "本地数据库已保存（${savedItems.size} 条）",
            fontSize = 18.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (savedItems.isEmpty()) {
            Text(text = "暂无已保存的数据，请先获取并一键保存", fontSize = 14.sp)
        } else {
            savedItems.forEach { entity ->
                IssueCard(
                    type = entity.type,
                    name = entity.name,
                    code = entity.securityCode,
                    date = entity.issueDate,
                    market = entity.market,
                    calendarEventId = entity.calendarEventId,
                    onAddCalendar = { addCalendar(entity) }
                )
            }
        }
    }
}

@Composable
private fun IssueCard(
    type: String,
    name: String,
    code: String,
    date: String,
    market: String,
    calendarEventId: Long? = null,
    onAddCalendar: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = type,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = name, fontSize = 16.sp)
                Text(text = "代码: $code", fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = date, fontSize = 14.sp)
                Text(text = market, fontSize = 12.sp)
                if (onAddCalendar != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    if (calendarEventId != null) {
                        Text(
                            text = "已添加日历",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Button(onClick = onAddCalendar) {
                            Text("添加日历", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun fetchJson(urlStr: String): String? {
    return try {
        val url = URL(urlStr)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty(
            "User-Agent",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"
        )
        connection.setRequestProperty("Referer", "https://data.eastmoney.com/")
        val reader = BufferedReader(connection.inputStream.bufferedReader())
        val response = reader.readText()
        reader.close()
        connection.disconnect()
        response
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private suspend fun fetchNewIssues(): List<NewIssueItem> = withContext(Dispatchers.IO) {
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())
    val items = mutableListOf<NewIssueItem>()

    try {
        val ipoUrl = "https://datacenter-web.eastmoney.com/api/data/v1/get?reportName=RPTA_APP_IPOAPPLY&columns=ALL&pageNumber=1&pageSize=100&sortColumns=APPLY_DATE&sortTypes=-1&source=WEB&client=WEB"
        fetchJson(ipoUrl)?.let { response ->
            val json = JSONObject(response)
            val data = json.optJSONObject("result")?.optJSONArray("data") ?: return@let
            for (i in 0 until data.length()) {
                val obj = data.getJSONObject(i)
                val date = obj.optString("APPLY_DATE").take(10)
                if (date.isNotEmpty() && date >= today) {
                    items.add(
                        NewIssueItem(
                            type = "新股",
                            securityCode = obj.optString("SECURITY_CODE"),
                            name = obj.optString("SECURITY_NAME_ABBR").ifEmpty { obj.optString("SECURITY_NAME") },
                            issueDate = date,
                            market = obj.optString("MARKET").ifEmpty { obj.optString("TRADE_MARKET") }
                        )
                    )
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    try {
        val bondUrl = "https://datacenter-web.eastmoney.com/api/data/v1/get?reportName=RPT_BOND_CB_LIST&columns=ALL&pageNumber=1&pageSize=500&sortColumns=PUBLIC_START_DATE&sortTypes=-1&source=WEB&client=WEB"
        fetchJson(bondUrl)?.let { response ->
            val json = JSONObject(response)
            val data = json.optJSONObject("result")?.optJSONArray("data") ?: return@let
            for (i in 0 until data.length()) {
                val obj = data.getJSONObject(i)
                val date = obj.optString("PUBLIC_START_DATE").take(10)
                if (date.isNotEmpty() && date >= today) {
                    items.add(
                        NewIssueItem(
                            type = "可转债",
                            securityCode = obj.optString("SECURITY_CODE"),
                            name = obj.optString("SECURITY_NAME_ABBR").ifEmpty { obj.optString("SECURITY_NAME") },
                            issueDate = date,
                            market = when (obj.optString("TRADE_MARKET")) {
                                "CNSESH" -> "上交所"
                                "CNSESZ" -> "深交所"
                                else -> obj.optString("TRADE_MARKET")
                            }
                        )
                    )
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    items.distinctBy { it.type + it.securityCode }.sortedBy { it.issueDate }
}

/**
 * 调用系统日历 API，为指定新股/可转债创建全天事件，返回事件 ID；失败返回 null。
 */
private suspend fun addIssueToCalendar(context: Context, entity: StockCalendarEntity): Long? {
    return withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            // 查找可见且可写的日历，优先使用主日历
            val projection = arrayOf(
                CalendarContract.Calendars._ID,
                CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                CalendarContract.Calendars.IS_PRIMARY
            )
            val selection = "${CalendarContract.Calendars.VISIBLE} = 1 AND " +
                    "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ${CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR}"
            val calendars = mutableListOf<Triple<Long, Int, Int>>()
            resolver.query(CalendarContract.Calendars.CONTENT_URI, projection, selection, null, null)
                ?.use { cursor ->
                    while (cursor.moveToNext()) {
                        calendars.add(
                            Triple(
                                cursor.getLong(0),
                                cursor.getInt(1),
                                cursor.getInt(2)
                            )
                        )
                    }
                }
            val calendarId = calendars
                .sortedWith(compareByDescending<Triple<Long, Int, Int>> { it.third }.thenByDescending { it.second })
                .firstOrNull()
                ?.first
                ?: return@withContext null

            val start = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).parse(entity.issueDate)?.time
                ?: return@withContext null

            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, "${entity.type} ${entity.name}")
                put(
                    CalendarContract.Events.DESCRIPTION,
                    "代码: ${entity.securityCode}\n市场: ${entity.market}\n发行日期: ${entity.issueDate}"
                )
                put(CalendarContract.Events.DTSTART, start)
                put(CalendarContract.Events.DTEND, start + 24 * 60 * 60 * 1000L)
                put(CalendarContract.Events.ALL_DAY, 1)
                put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            }
            val uri = resolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
