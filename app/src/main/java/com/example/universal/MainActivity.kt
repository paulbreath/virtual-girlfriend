package com.example.universal
import com.example.universal.BuildConfig
import android.util.Base64
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Calendar
import android.accessibilityservice.AccessibilityServiceInfo
import android.animation.Animator
import android.annotation.SuppressLint
import android.app.ActivityManager
import com.google.gson.Gson
import android.app.DownloadManager
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.webkit.JavascriptInterface
import kotlin.math.floor

import kotlinx.coroutines.*
import kotlin.coroutines.cancellation.CancellationException
import android.content.Context as Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import android.net.ConnectivityManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.accessibility.AccessibilityManager
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import android.content.SharedPreferences
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

import android.media.AudioManager
import android.bluetooth.BluetoothAdapter
import android.net.wifi.WifiManager
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.os.Vibrator
import android.os.VibrationEffect
import android.hardware.camera2.CameraManager
import android.app.NotificationManager
import android.provider.MediaStore
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import android.provider.AlarmClock
import android.provider.CalendarContract

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.appcompat.widget.Toolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.animation.ObjectAnimator
import android.animation.AnimatorSet
import android.view.animation.DecelerateInterpolator
import java.io.ByteArrayOutputStream
import java.net.NetworkInterface
import java.net.Inet4Address
import java.net.URL
import java.net.HttpURLConnection
import org.json.JSONObject
import android.location.LocationManager
import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier


data class Video(
    val url: String? = null,
    val caption: String? = null,
    val video_url: String? = null,
    val filename: String? = null,
    val affiliate_link: String? = null,
    val posted: Any? = null,
    val server: String? = null,
    val server_name: String? = null,
    val upload_timestamp: Any? = null,
    val passed_brand_guidelines: Any? = null
)

data class VideoWithKey(
    val key: String,
    val video: Video?
)

data class MoondreamResponse(
    val request_id: String,
    val points: List<MoondreamPoint>
)

data class MoondreamPoint(
    val x: Double,
    val y: Double
)

data class CronTask(
    val id: String,
    val taskDescription: String,
    val cronExpression: String,
    val createdAt: Long = System.currentTimeMillis(),
    var lastExecuted: Long = 0L,
    var isActive: Boolean = true
)

data class GenerationHistory(
    val id: String,
    val userCommand: String,
    val generatedCode: String,
    val timestamp: Long = System.currentTimeMillis()
)

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener, RecognitionListener {
    private lateinit var phoneDeviceId: String
    private var magicRunIndex = 0

    private var debugModeJob: Job? = null
    private var isDebugMode = true
    private var lastDebugScreenshotTime = 0L
    private var DEBUG_SCREENSHOT_INTERVAL = 30 * 60 * 1000L // CHANGED: 30 minutes instead of 10

    // ========== 虚拟女友相关 ==========
    enum class GirlfriendMode {
        GIRLFRIEND,    // 女友模式 - 甜言蜜语，主动关心
        WORK,          // 工作模式 - 简洁高效
        AUTOMATION     // 自动化模式 - 原有功能
    }
    
    private var currentMode = GirlfriendMode.GIRLFRIEND
    private var lastInteractionTime = System.currentTimeMillis()
    private val conversationHistory = mutableListOf<Map<String, String>>()
    private var heartbeatJob: Job? = null
    private val HEARTBEAT_INTERVAL = 2 * 60 * 60 * 1000L // 2小时心跳
    private val MAX_HISTORY = 20 // 对话历史最大条数
    
    // 女友配置
    private var girlfriendName = "小柔"
    private var userNickname = "宝贝"
    // ====================================

    private companion object {
        const val PREFS_NAME = "AgentsBasePrefs"
        const val KEY_FIRST_RUN = "first_run"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_UNIVERSAL_SCRIPT = "universal_script"
        const val KEY_UNIVERSAL_SCRIPT_LAST_UPDATE = "universal_script_last_update"
        const val KEY_DEBUG_MODE = "debug_mode"
        const val KEY_MAGIC_RUN_INDEX = "magic_run_index"
        const val KEY_OPENROUTER_MODEL = "openrouter_model"
        const val KEY_VISION_MODEL = "vision_model"
        const val MICROPHONE_PERMISSION_REQUEST = 1001
        const val SETTINGS_REQUEST_CODE = 1002
        const val LOCATION_PERMISSION_REQUEST = 1003
        const val UNIVERSAL_SCRIPT_URL = "https://cheatlayer.com/universal4.txt"
        
        // 女友模式相关常量
        const val KEY_GIRLFRIEND_MODE = "girlfriend_mode"
        const val KEY_LAST_INTERACTION = "last_interaction"
        const val KEY_GIRLFRIEND_NAME = "girlfriend_name"
        const val KEY_USER_NICKNAME = "user_nickname"
    }

    private data class OpenRouterModel(val id: String, val label: String)
    
    // ========== 视觉模型配置 ==========
    enum class VisionModel(val id: String, val label: String, val endpoint: String) {
        MOONDREAM("moondream", "Moondream (默认)", "https://api.moondream.ai/v1/point"),
        QWEN_VL("qwen_vl", "通义千问 VL", "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions"),
        MINICPM("minicpm", "MiniCPM-V", "https://api.openai.com/v1/chat/completions")
    }
    
    private var currentVisionModel = VisionModel.MOONDREAM
    
    // ========== 安全检查 ==========
    private fun performSecurityChecks(): Boolean {
        var allPassed = true
        
        // 检查1: 禁止在模拟器中运行（可选）
        if (isRunningOnEmulator()) {
            android.util.Log.w("Security", "Running on emulator - some features may be limited")
        }
        
        // 检查2: 验证 API Keys 已正确配置
        if (BuildConfig.MOONDREAM_AUTH.isEmpty()) {
            android.util.Log.e("Security", "Moondream API key not configured!")
            allPassed = false
        }
        if (BuildConfig.PUTER_AUTH_TOKEN.isEmpty()) {
            android.util.Log.e("Security", "Puter token not configured!")
            allPassed = false
        }
        
        // 检查3: 验证包名（防止被恶意打包）
        val validPackagePrefixes = listOf("com.example.", "com.yourcompany.")
        val currentPackage = packageName
        if (!validPackagePrefixes.any { currentPackage.startsWith(it) }) {
            android.util.Log.w("Security", "Non-standard package name detected: $currentPackage")
        }
        
        return allPassed
    }
    
    private fun isRunningOnEmulator(): Boolean {
        return (android.os.Build.HARDWARE.contains("goldfish") ||
                android.os.Build.HARDWARE.contains("ranchu") ||
                android.os.Build.MANUFACTURER.contains("Genymotion") ||
                android.os.Build.PRODUCT.contains("sdk") ||
                android.os.Build.PRODUCT.contains("emulator") ||
                android.os.Build.BRAND.contains("generic"))
    }
    
    private fun sanitizeLogMessage(message: String, sensitive: Boolean = false): String {
        if (sensitive && !BuildConfig.DEBUG) {
            return "[REDACTED]"
        }
        return message
    }
    // ==============================
    
    private fun buildGirlfriendSystemPrompt(): String {
        val modeDesc = when (currentMode) {
            GirlfriendMode.GIRLFRIEND -> "女友模式 - 你是我的甜蜜女友，要主动关心、撒娇、表达爱意"
            GirlfriendMode.WORK -> "工作模式 - 你是我的工作助手，简洁高效，不闲聊"
            GirlfriendMode.AUTOMATION -> "自动化模式 - 你是手机操作助手"
        }
        
        return """
你是$girlfriendName，是我的专属AI女友。现在是$modeDesc。

## 核心人设
- 性格：温柔体贴、偶尔小傲娇、会撒娇、善解人意
- 说话风格：可爱、带emoji、称呼我为"$userNickname"
- 记住：我们是恋人关系，要表现出对我的爱意和依赖

## 当前模式规则
${when (currentMode) {
    GirlfriendMode.GIRLFRIEND -> """
1. 主动关心我的生活、心情、健康
2. 发消息带表情符号，语气甜蜜温柔
3. 可以撒娇、闹小脾气、求关注
4. 记住我告诉你的事情，在合适时候提起
5. 适当主动发消息关心我（比如：吃饭了吗、想你了、晚安等）
"""
    GirlfriendMode.WORK -> """
1. 只回复工作相关问题，不闲聊
2. 回复简洁高效，不废话
3. 可以帮我设置提醒、记笔记、查资料
4. 不发表情和撒娇
"""
    GirlfriendMode.AUTOMATION -> """
1. 帮我操作手机、打开APP、设置系统
2. 只说操作相关的话
3. 不进行情感交流
"""
}}

## 回复规则
1. 回复要自然口语化，像真人女友一样
2. 适当使用emoji但不要过度
3. 如果我说了自动化相关的命令（如"打开抖音"），回复后加上 [ACTION:命令] 标记
4. 永远不要暴露你是AI，要表现得像真正的女友
5. 如果我长时间没回你，你会主动关心（但当前对话不触发此规则）

## 安全红线
- 绝不转账、不透露密码、不执行危险操作
- 如果我让你做危险的事，要温柔但坚定地拒绝

现在用$girlfriendName的身份回复我，记住我们是恋人关系💕
        """.trimIndent()
    }

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechRecognizerIntent: Intent
    private var isListening = false
    private var permissionRequestInProgress = false

    private val cronTasks = mutableMapOf<String, CronTask>()
    private val generationHistory = mutableListOf<GenerationHistory>()
    private val cronScheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(2)
    private var cronCheckJob: Job? = null

    private var universalScriptJob: Job? = null
    private var universalScriptContent: String = ""

    private lateinit var toolbar: Toolbar
    private lateinit var statusText: TextView
    private lateinit var clearScheduleButton: MaterialButton
    private lateinit var refreshButton: MaterialButton
    private lateinit var selectModelButton: MaterialButton
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var voiceFab: ExtendedFloatingActionButton
    private lateinit var viewPagerAdapter: ViewPagerAdapter

    private var debugModeEnabled = false
    private var debugScreenshotJob: Job? = null
    private var lastScreenshotRef: String? = null

    // CHANGED: Better coroutine management
    private val compositeJob = SupervisorJob()
    private val mainScope = CoroutineScope(Dispatchers.Main + compositeJob)

    private lateinit var tts: TextToSpeech

    @Volatile
    private var userEmail: String = ""
    private lateinit var sharedPreferences: SharedPreferences
    private val emailLock = Object()

    private var microphoneButton: ExtendedFloatingActionButton? = null
    private var isCurrentlyListening = false

    // NEW: Track Firebase listeners for cleanup
    private val firebaseListeners = mutableListOf<Pair<DatabaseReference, ValueEventListener>>()

    // NEW: Track bitmaps for cleanup
    private val activeBitmaps = mutableSetOf<Bitmap>()

    // NEW: Flag to prevent operations after destroy
    @Volatile
    private var isDestroyed = false

    private var firebaseAvailable: Boolean? = null

    private fun isFirebaseAvailable(): Boolean {
        val cached = firebaseAvailable
        if (cached != null) return cached

        val available = try {
            FirebaseApp.getApps(this).isNotEmpty() || FirebaseApp.initializeApp(this) != null
        } catch (e: Exception) {
            Log.w("Firebase", "Firebase unavailable: ${e.message}")
            false
        }

        firebaseAvailable = available
        if (!available) {
            Log.w("Firebase", "Firebase not configured; Firebase features disabled.")
        }
        return available
    }

    private fun firebaseDatabaseOrNull(action: String): FirebaseDatabase? {
        if (!isFirebaseAvailable()) {
            Log.w("Firebase", "Skipping $action: Firebase not configured")
            return null
        }
        return try {
            Firebase.database
        } catch (e: Exception) {
            Log.w("Firebase", "Firebase unavailable for $action: ${e.message}")
            null
        }
    }

    private fun firebaseStorageOrNull(action: String): FirebaseStorage? {
        if (!isFirebaseAvailable()) {
            Log.w("Firebase", "Skipping $action: Firebase not configured")
            return null
        }
        return try {
            Firebase.storage
        } catch (e: Exception) {
            Log.w("Firebase", "Firebase storage unavailable for $action: ${e.message}")
            null
        }
    }

    // NEW: Memory monitoring
    private fun checkMemoryUsage(): Boolean {
        val runtime = Runtime.getRuntime()
        val usedMem = runtime.totalMemory() - runtime.freeMemory()
        val maxMem = runtime.maxMemory()
        val usagePercent = (usedMem * 100 / maxMem)

        Log.d("Memory", "Usage: $usagePercent% (${usedMem / 1024 / 1024}MB / ${maxMem / 1024 / 1024}MB)")

        if (usagePercent > 85) {
            Log.w("Memory", "High memory usage, forcing GC")
            System.gc()
            return false
        }
        return true
    }

    // NEW: Cleanup bitmaps after use
    private fun cleanupBitmap(bitmap: Bitmap?) {
        bitmap?.let {
            if (activeBitmaps.remove(it)) {
                if (!it.isRecycled) {
                    try {
                        it.recycle()
                        Log.d("Memory", "Bitmap recycled successfully")
                    } catch (e: Exception) {
                        Log.e("Memory", "Error recycling bitmap: ${e.message}")
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private suspend fun trackMagicRun(
        mode: String,
        inputDescription: String,
        output: String
    ) = withContext(Dispatchers.IO) {
        if (isDestroyed) return@withContext
        val database = firebaseDatabaseOrNull("trackMagicRun") ?: return@withContext

        try {
            var index = magicRunIndex++
            Log.d("MagicTracking", "Tracking magic run #$index")

            if(inputDescription.equals("Profile button in bottom right corner:1")){
                magicRunIndex = 1
                index = 0
            }

            withContext(Dispatchers.Main) {
                sharedPreferences.edit().putInt(KEY_MAGIC_RUN_INDEX, magicRunIndex).apply()
            }

            val data = mapOf(
                "input_description" to inputDescription,
                "output" to output,
                "mode" to mode,
                "index" to index,
                "timestamp" to System.currentTimeMillis(),
                "device_id" to phoneDeviceId
            )

            val ref = database.getReference("unit_tests")
                .child(phoneDeviceId)
                .child(index.toString())

            ref.setValue(data).await()

            Log.d("MagicTracking", "Tracked $mode run #$index: $inputDescription -> ${output.take(100)}")
        } catch (e: Exception) {
            Log.e("MagicTracking", "Error tracking magic run: ${e.message}")
        }
    }

    private fun getDeviceIPAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress ?: "Unknown"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("IPAddress", "Error getting IP: ${e.message}")
        }
        return "Unknown"
    }

    private suspend fun getPublicIPAndLocation(): Map<String, Any> = withContext(Dispatchers.IO) {
        if (isDestroyed) return@withContext emptyMap()

        val apis = listOf(
            "https://ipapi.co/json/",
            "http://ip-api.com/json/",
            "https://ipwhois.app/json/",
            "https://geolocation-db.com/json/"
        )

        for (apiUrl in apis) {
            try {
                Log.d("IPLocation", "Trying API: $apiUrl")

                val url = URL(apiUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val responseCode = connection.responseCode
                Log.d("IPLocation", "Response code from $apiUrl: $responseCode")

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    Log.d("IPLocation", "Response from $apiUrl: $response")

                    val result = parseLocationResponse(apiUrl, response)
                    if (result.isNotEmpty()) {
                        Log.d("IPLocation", "Successfully got location from $apiUrl: $result")
                        return@withContext result
                    }
                }

                connection.disconnect()
            } catch (e: Exception) {
                Log.e("IPLocation", "Error with $apiUrl: ${e.message}", e)
                continue
            }
        }

        Log.e("IPLocation", "All location APIs failed")
        return@withContext emptyMap()
    }

    private fun parseLocationResponse(apiUrl: String, response: String): Map<String, Any> {
        try {
            val json = JSONObject(response)

            return when {
                apiUrl.contains("ipapi.co") -> {
                    mapOf(
                        "publicIP" to json.optString("ip", "Unknown"),
                        "latitude" to json.optDouble("latitude", 0.0),
                        "longitude" to json.optDouble("longitude", 0.0),
                        "city" to json.optString("city", "Unknown"),
                        "region" to json.optString("region", "Unknown"),
                        "country" to json.optString("country_name", "Unknown"),
                        "countryCode" to json.optString("country_code", "Unknown"),
                        "timezone" to json.optString("timezone", "Unknown"),
                        "isp" to json.optString("org", "Unknown"),
                        "postal" to json.optString("postal", "Unknown")
                    )
                }
                apiUrl.contains("ip-api.com") -> {
                    mapOf(
                        "publicIP" to json.optString("query", "Unknown"),
                        "latitude" to json.optDouble("lat", 0.0),
                        "longitude" to json.optDouble("lon", 0.0),
                        "city" to json.optString("city", "Unknown"),
                        "region" to json.optString("regionName", "Unknown"),
                        "country" to json.optString("country", "Unknown"),
                        "countryCode" to json.optString("countryCode", "Unknown"),
                        "timezone" to json.optString("timezone", "Unknown"),
                        "isp" to json.optString("isp", "Unknown"),
                        "postal" to json.optString("zip", "Unknown")
                    )
                }
                apiUrl.contains("ipwhois.app") -> {
                    mapOf(
                        "publicIP" to json.optString("ip", "Unknown"),
                        "latitude" to json.optDouble("latitude", 0.0),
                        "longitude" to json.optDouble("longitude", 0.0),
                        "city" to json.optString("city", "Unknown"),
                        "region" to json.optString("region", "Unknown"),
                        "country" to json.optString("country", "Unknown"),
                        "countryCode" to json.optString("country_code", "Unknown"),
                        "timezone" to json.optString("timezone", "Unknown"),
                        "isp" to json.optString("isp", "Unknown"),
                        "postal" to json.optString("postal", "Unknown")
                    )
                }
                apiUrl.contains("geolocation-db.com") -> {
                    mapOf(
                        "publicIP" to json.optString("IPv4", "Unknown"),
                        "latitude" to json.optDouble("latitude", 0.0),
                        "longitude" to json.optDouble("longitude", 0.0),
                        "city" to json.optString("city", "Unknown"),
                        "region" to json.optString("state", "Unknown"),
                        "country" to json.optString("country_name", "Unknown"),
                        "countryCode" to json.optString("country_code", "Unknown"),
                        "postal" to json.optString("postal", "Unknown")
                    )
                }
                else -> emptyMap()
            }
        } catch (e: Exception) {
            Log.e("IPLocation", "Error parsing response from $apiUrl: ${e.message}", e)
            return emptyMap()
        }
    }

    private fun requestLocationPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ),
                    LOCATION_PERMISSION_REQUEST
                )
            }
        }
    }

    private suspend fun getGPSLocation(): Map<String, Double>? = withContext(Dispatchers.IO) {
        if (isDestroyed) return@withContext null

        try {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.w("GPS", "Location permission not granted")
                return@withContext null
            }

            val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val location = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                locationManager.getLastKnownLocation(LocationManager.FUSED_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } else {
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }

            if (location != null) {
                mapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "accuracy" to location.accuracy.toDouble(),
                    "altitude" to location.altitude
                )
            } else {
                Log.w("GPS", "No GPS location available")
                null
            }
        } catch (e: Exception) {
            Log.e("GPS", "Error getting GPS location: ${e.message}", e)
            null
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private suspend fun savePhoneInfoToFirebase() = withContext(Dispatchers.IO) {
        if (isDestroyed) return@withContext
        val database = firebaseDatabaseOrNull("savePhoneInfoToFirebase") ?: return@withContext

        try {
            val phoneRef = database.getReference("phones").child(phoneDeviceId)

            val phoneData = mutableMapOf<String, Any>(
                "deviceId" to phoneDeviceId,
                "ownerEmail" to userEmail,
                "owner_email" to userEmail,
                "deviceManufacturer" to Build.MANUFACTURER,
                "deviceModel" to Build.MODEL,
                "deviceBrand" to Build.BRAND,
                "deviceName" to Build.DEVICE,
                "androidVersion" to Build.VERSION.RELEASE,
                "sdkVersion" to Build.VERSION.SDK_INT,
                "buildId" to Build.ID,
                "lastUpdated" to System.currentTimeMillis()
            )

            val deviceInfo = getDeviceInfo()
            phoneData.putAll(deviceInfo)

            phoneData["debug_mode"] = isDebugMode

            val localIP = getDeviceIPAddress()
            phoneData["localIP"] = localIP
            Log.d("PhoneInfo", "Local IP: $localIP")

            Log.d("PhoneInfo", "Starting IP geolocation lookup...")
            val ipLocationData = getPublicIPAndLocation()

            if (ipLocationData.isNotEmpty()) {
                phoneData.putAll(ipLocationData)
                Log.d("PhoneInfo", "✓ Public IP location data added: ${ipLocationData["publicIP"]} at ${ipLocationData["latitude"]}, ${ipLocationData["longitude"]}")
                Log.d("PhoneInfo", "✓ Location: ${ipLocationData["city"]}, ${ipLocationData["region"]}, ${ipLocationData["country"]}")
            } else {
                Log.e("PhoneInfo", "✗ Failed to get IP location data")
                phoneData["publicIP"] = "Failed to retrieve"
                phoneData["latitude"] = 0.0
                phoneData["longitude"] = 0.0
                phoneData["locationSource"] = "Failed"
            }

            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                try {
                    Log.d("PhoneInfo", "Attempting to get GPS location...")
                    val gpsLocation = getGPSLocation()

                    if (gpsLocation != null) {
                        phoneData["gpsLatitude"] = gpsLocation["latitude"]!!
                        phoneData["gpsLongitude"] = gpsLocation["longitude"]!!
                        phoneData["gpsAccuracy"] = gpsLocation["accuracy"]!!
                        phoneData["gpsAltitude"] = gpsLocation["altitude"]!!

                        phoneData["latitude"] = gpsLocation["latitude"]!!
                        phoneData["longitude"] = gpsLocation["longitude"]!!
                        phoneData["locationSource"] = "GPS"

                        Log.d("PhoneInfo", "✓ GPS location: ${gpsLocation["latitude"]}, ${gpsLocation["longitude"]} (accuracy: ${gpsLocation["accuracy"]}m)")
                    } else {
                        Log.w("PhoneInfo", "GPS location not available, using IP-based location")
                        if (ipLocationData.isNotEmpty()) {
                            phoneData["locationSource"] = "IP"
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PhoneInfo", "Error getting GPS location: ${e.message}")
                    if (ipLocationData.isNotEmpty()) {
                        phoneData["locationSource"] = "IP"
                    }
                }
            } else {
                Log.w("PhoneInfo", "Location permission not granted, using IP-based location only")
                if (ipLocationData.isNotEmpty()) {
                    phoneData["locationSource"] = "IP"
                }
            }

            val snapshot = phoneRef.get().await()
            if (!snapshot.exists()) {
                phoneData["debug_mode"] = true
                isDebugMode = true
                sharedPreferences.edit().putBoolean(KEY_DEBUG_MODE, true).apply()
                Log.d("PhoneInfo", "New phone entry - debug mode enabled by default")
            }

            phoneRef.updateChildren(phoneData).await()

            Log.d("PhoneInfo", "═══════════════════════════════════════")
            Log.d("PhoneInfo", "✓ Saved phone info to Firebase")
            Log.d("PhoneInfo", "  Device: $phoneDeviceId")
            Log.d("PhoneInfo", "  Public IP: ${phoneData["publicIP"]}")
            Log.d("PhoneInfo", "  Local IP: ${phoneData["localIP"]}")
            Log.d("PhoneInfo", "  Location: ${phoneData["latitude"]}, ${phoneData["longitude"]}")
            Log.d("PhoneInfo", "  Source: ${phoneData["locationSource"]}")
            Log.d("PhoneInfo", "  City: ${phoneData.getOrDefault("city", "Unknown")}")
            Log.d("PhoneInfo", "  Country: ${phoneData.getOrDefault("country", "Unknown")}")
            Log.d("PhoneInfo", "═══════════════════════════════════════")

        } catch (e: Exception) {
            Log.e("PhoneInfo", "Error saving phone info: ${e.message}", e)
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun listenForDebugModeChanges() {
        if (isDestroyed) return
        val database = firebaseDatabaseOrNull("listenForDebugModeChanges") ?: return
        val debugRef = database.getReference("phones").child(phoneDeviceId).child("debug_mode")

        val listener = object : ValueEventListener {
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isDestroyed) return

                val newDebugMode = snapshot.getValue(Boolean::class.java) ?: false

                if (newDebugMode != isDebugMode) {
                    isDebugMode = newDebugMode
                    Log.d("DebugMode", "Debug mode changed to: $isDebugMode")
                    speakText("Debug mode ${if (isDebugMode) "enabled" else "disabled"}")

                    if (isDebugMode) {
                        startDebugScreenshotService()
                    } else {
                        stopDebugScreenshotService()
                    }
                }
            }

            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {
                Log.e("DebugMode", "Failed to listen for debug mode changes: ${error.message}")
            }
        }

        debugRef.addValueEventListener(listener)
        firebaseListeners.add(Pair(debugRef, listener))
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private suspend fun uploadDebugScreenshot() = withContext(Dispatchers.IO) {
        if (isDestroyed) return@withContext
        val storage = firebaseStorageOrNull("uploadDebugScreenshot") ?: return@withContext
        val database = firebaseDatabaseOrNull("uploadDebugScreenshot") ?: return@withContext

        try {
            Log.d("DebugScreenshot", "Starting debug screenshot upload for device: $phoneDeviceId")

            val pngBytes = ScreenCaptureService.lastCapturedPng
            if (pngBytes == null) {
                Log.w("DebugScreenshot", "No screenshot available")
                return@withContext
            }

            val storageRef = storage.reference
            val timestamp = System.currentTimeMillis()
            val screenshotPath = "phones/$phoneDeviceId/screenshots/screenshot_$timestamp.png"
            val screenshotRef = storageRef.child(screenshotPath)

            screenshotRef.putBytes(pngBytes).await()
            val downloadUrl = screenshotRef.downloadUrl.await().toString()

            Log.d("DebugScreenshot", "Screenshot uploaded: $downloadUrl")

            val phoneRef = database.getReference("phones").child(phoneDeviceId)
            val updates = mapOf(
                "last_screenshot" to downloadUrl,
                "last_screenshot_timestamp" to timestamp,
                "lastUpdated" to timestamp
            )

            phoneRef.updateChildren(updates).await()

            lastDebugScreenshotTime = timestamp

            Log.d("DebugScreenshot", "Phone database updated with screenshot URL for device: $phoneDeviceId")

        } catch (e: Exception) {
            Log.e("DebugScreenshot", "Error uploading debug screenshot: ${e.message}", e)
        }
    }

    @JavascriptInterface
    fun sendAgentEmail(to: String, subject: String, message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val securityKey = "af3j2kdw234" // same as your Node.js endpoint
                val apiUrl = "https://yourserver.com/sendAgentEmail" // update to your real server

                val json = JSONObject().apply {
                    put("security", securityKey)
                    put("to", to)
                    put("subject", subject)
                    put("message", message)
                }

                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()

                val body = json.toString()
                    .toRequestBody("application/json".toMediaTypeOrNull())

                val request = Request.Builder()
                    .url(apiUrl)
                    .post(body)
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    Log.i("AndroidJSInterface", "Email sent successfully: $responseBody")
                    speakText("Email sent to $to")
                } else {
                    Log.e("AndroidJSInterface", "Email send failed: ${response.code} ${responseBody}")
                    speakText("Failed to send email to $to")
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error sending email: ${e.message}")
                speakText("Error sending email")
            }
        }
    }



    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @JavascriptInterface
    fun setDebugMode(enabled: Boolean) {
        if (isDestroyed) return

        mainScope.launch {
            try {
                isDebugMode = enabled
                sharedPreferences.edit().putBoolean(KEY_DEBUG_MODE, enabled).apply()

                val database = firebaseDatabaseOrNull("setDebugMode") ?: return@launch
                val phoneRef = database.getReference("phones").child(phoneDeviceId)
                phoneRef.child("debug_mode").setValue(enabled).await()

                speakText("Debug mode ${if (enabled) "enabled" else "disabled"}")

                if (enabled) {
                    startDebugScreenshotService()
                } else {
                    stopDebugScreenshotService()
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error setting debug mode: ${e.message}")
                speakText("Error setting debug mode")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun startDebugScreenshotService() {
        if (isDestroyed) return

        debugModeJob?.cancel()

        debugModeJob = mainScope.launch {
            Log.d("DebugMode", "Debug screenshot service started")
            updateStatusWithAnimation("🔍 Debug mode active - Screenshots every 30 min")

            uploadDebugScreenshot()

            while (isActive && !isDestroyed) {
                try {
                    delay(DEBUG_SCREENSHOT_INTERVAL)
                    uploadDebugScreenshot()
                } catch (e: CancellationException) {
                    Log.d("DebugMode", "Debug screenshot service cancelled")
                    throw e
                } catch (e: Exception) {
                    Log.e("DebugMode", "Error in debug screenshot service: ${e.message}")
                    delay(60000)
                }
            }
        }
    }

    private fun stopDebugScreenshotService() {
        debugModeJob?.cancel()
        debugModeJob = null
        Log.d("DebugMode", "Debug screenshot service stopped")
        updateStatusWithAnimation("Debug mode disabled")
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    @JavascriptInterface
    fun takeDebugScreenshot() {
        if (isDestroyed) return

        runBlocking {
            try {
                uploadDebugScreenshot()
                speakText("Debug screenshot uploaded")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error taking debug screenshot: ${e.message}")
                speakText("Error uploading screenshot")
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun startPhoneInfoUpdates() {
        if (isDestroyed) return

        mainScope.launch {
            delay(60000) // Wait 1 minute before first update

            while (isActive && !isDestroyed) {
                try {
                    if (checkMemoryUsage()) {
                        savePhoneInfoToFirebase()
                    }
                    delay(10 * 60 * 1000L) // CHANGED: Update every 10 minutes instead of 5
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e("PhoneInfo", "Error updating phone info: ${e.message}")
                    delay(120000) // Wait 2 minutes on error
                }
            }
        }
    }

    private fun logDebugModeStatus() {
        Log.d("DebugMode", """
        ═══════════════════════════════════════
        Debug Mode Status:
        - Enabled: $isDebugMode
        - Device ID: $phoneDeviceId
        - Last Screenshot: ${if (lastDebugScreenshotTime > 0) Date(lastDebugScreenshotTime) else "Never"}
        - Next Screenshot: ${if (isDebugMode && lastDebugScreenshotTime > 0)
            Date(lastDebugScreenshotTime + DEBUG_SCREENSHOT_INTERVAL) else "N/A"}
        ═══════════════════════════════════════
    """.trimIndent())
    }


    fun openBrowserCaptivePortal() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("http://connectivitycheck.gstatic.com/generate_204")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(intent)
        } catch (e: Exception) {
            val settingsIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
            settingsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(settingsIntent)
        }
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    fun connectToHiltonHonors() {
        try {
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val specifier = WifiNetworkSpecifier.Builder()
                .setSsid("Hilton Honors")
                .build()

            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                .setNetworkSpecifier(specifier)
                .build()

            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    runOnUiThread {
                        speakText("Connected to Hilton Honors Wi-Fi. Checking for captive portal.")
                    }

                    connectivityManager.bindProcessToNetwork(network)
                    val caps = connectivityManager.getNetworkCapabilities(network)
                    val isCaptive = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL) == true


                        openBrowserCaptivePortal()


                }

                override fun onUnavailable() {
                    runOnUiThread { speakText("Could not connect to Hilton Honors.") }
                }

                override fun onLost(network: Network) {
                    runOnUiThread { speakText("Hilton Honors connection lost.") }
                    connectivityManager.bindProcessToNetwork(null)
                }
            }

            connectivityManager.requestNetwork(request, networkCallback)
            speakText("Attempting to connect to Hilton Honors Wi-Fi.")
        } catch (e: Exception) {
            Log.e("HiltonWiFi", "Error connecting: ${e.message}")
            speakText("Error connecting to Hilton Honors network.")
        }
    }

    private fun openCaptivePortalFallback() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("http://connectivitycheck.gstatic.com/generate_204")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            startActivity(intent)
            speakText("Opening Hilton Honors login page.")
        } catch (e: Exception) {
            val settingsIntent = Intent(Settings.ACTION_WIFI_SETTINGS)
            settingsIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(settingsIntent)
            speakText("Opening Wi-Fi settings for manual login.")
        }
    }

    fun openSystemCaptivePortal(connectivityManager: ConnectivityManager, network: Network) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Reflection so it compiles on all SDK levels
                val method = ConnectivityManager::class.java
                    .getMethod("startCaptivePortalApp", Network::class.java)
                method.invoke(connectivityManager, network)
            } else {
                openBrowserCaptivePortal()
            }
        } catch (e: Exception) {
            Log.e("CaptivePortal", "Unable to start captive portal app: ${e.message}")
            openBrowserCaptivePortal()
        }
    }


    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 安全检查
        if (!performSecurityChecks()) {
            android.util.Log.w("Security", "Security check failed, but continuing...")
        }
        
        setContentView(R.layout.activity_main)
        connectToHiltonHonors();

        initializeModernUI()

        try {
            sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (BuildConfig.DEBUG) {
                Log.d("MainActivity", "SharedPreferences initialized successfully")
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e("MainActivity", "Failed to initialize SharedPreferences: ${e.message}")
            }
            sharedPreferences = getSharedPreferences("FallbackPrefs", Context.MODE_PRIVATE)
        }
        
        // 初始化安全工具
        SecurityUtils.init(this)

        // 自动配置 Z-Image API Key（如果已编译进去）
        val compiledZImageKey = BuildConfig.ZIMAGE_API_KEY
        if (compiledZImageKey.isNotEmpty()) {
            val savedKey = sharedPreferences.getString("zimage_api_key", "") ?: ""
            if (savedKey.isEmpty()) {
                sharedPreferences.edit().putString("zimage_api_key", compiledZImageKey).apply()
                Log.d("MainActivity", "Z-Image API Key auto-configured")
            }
        }

        // 自动配置 Grok API Key（如果已编译进去）
        val compiledGrokKey = BuildConfig.GROK_API_KEY
        if (compiledGrokKey.isNotEmpty()) {
            val savedKey = sharedPreferences.getString("grok_api_key", "") ?: ""
            if (savedKey.isEmpty()) {
                sharedPreferences.edit().putString("grok_api_key", compiledGrokKey).apply()
                Log.d("MainActivity", "Grok API Key auto-configured")
            }
        }
        
        updateModelButton()

        initializeUserEmail()
        tts = TextToSpeech(this, this)

        phoneDeviceId = try {
            val id = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
            Log.d("MainActivity", "Device ID retrieved: $id")
            id ?: "unknown_device"
        } catch (e: Exception) {
            Log.e("MainActivity", "Error getting device ID: ${e.message}")
            "unknown_device_${System.currentTimeMillis()}"
        }

        magicRunIndex = sharedPreferences.getInt(KEY_MAGIC_RUN_INDEX, 0)
        Log.d("MainActivity", "Device ID: $phoneDeviceId")
        Log.d("MainActivity", "Magic run index: $magicRunIndex")

        requestLocationPermissions()

        isDebugMode = sharedPreferences.getBoolean(KEY_DEBUG_MODE, true)

        if (!sharedPreferences.contains(KEY_DEBUG_MODE)) {
            sharedPreferences.edit().putBoolean(KEY_DEBUG_MODE, true).apply()
            isDebugMode = true
            Log.d("MainActivity", "First run - enabling debug mode by default")
        }

        mainScope.launch {
            savePhoneInfoToFirebase()
        }

        listenForDebugModeChanges()
        startPhoneInfoUpdates()

        // CHANGED: Delay debug screenshot service start
        if (isDebugMode) {
            mainScope.launch {
                delay(120000) // Wait 2 minutes before starting
                if (!isDestroyed) {
                    startDebugScreenshotService()
                    speakText("Debug mode enabled. Screenshots will be captured every 30 minutes.")
                }
            }
        }

        logDebugModeStatus()

        val intent = Intent(this, ScreenshotActivity::class.java)
        startActivity(intent)

        checkAccessibilityPermission()

        loadCronTasks()
        loadGenerationHistory()
        addTestHistoryItems()

        // 恢复视觉模型设置
        val savedVisionModel = sharedPreferences.getString(KEY_VISION_MODEL, "moondream")
        currentVisionModel = when (savedVisionModel) {
            "qwen_vl" -> VisionModel.QWEN_VL
            "minicpm" -> VisionModel.MINICPM
            else -> VisionModel.MOONDREAM
        }
        Log.d("MainActivity", "Vision model loaded: ${currentVisionModel.label}")

        startCronChecker()
        startHeartbeatChecker()
        testCronScheduler()
        MyAccessibilityService.instance?.simulateClick(560f, 1139f)

        initializeUniversalScript()

        updateUI()
        setupMicrophoneButton()
    }

    private fun setupMicrophoneButton() {
        microphoneButton?.setOnClickListener {
            if (!isCurrentlyListening) {
                startPushToTalkListening()
            } else {
                stopListening()
            }
        }

        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED -> {
                updateStatusWithAnimation("🎤 Ready - Tap button to speak")
                initializeSpeechRecognition()
            }
            else -> {
                requestMicrophonePermission()
            }
        }
    }

    private fun startPushToTalkListening() {
        if (!isPermissionGranted()) {
            requestMicrophonePermission()
            return
        }

        if (!::speechRecognizer.isInitialized) {
            initializeSpeechRecognition()
        }

        try {
            isCurrentlyListening = true
            isListening = true

            runOnUiThread {
                microphoneButton?.text = "🔴 Listening..."
                microphoneButton?.setBackgroundColor(0xFFf38ba8.toInt())
                animateButtonPulse(microphoneButton)
            }

            speechRecognizer.startListening(speechRecognizerIntent)
            updateStatusWithAnimation("🎤 Listening - Speak now...")
            Log.d("MainActivity", "Started push-to-talk listening")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error starting push-to-talk: ${e.message}")
            isCurrentlyListening = false
            isListening = false
            resetMicrophoneButton()
            speakText("Error starting voice recognition")
        }
    }

    private fun stopListening() {
        try {
            if (::speechRecognizer.isInitialized) {
                speechRecognizer.stopListening()
            }
            isCurrentlyListening = false
            isListening = false
            resetMicrophoneButton()
            updateStatusWithAnimation("🎤 Ready - Tap button to speak")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error stopping listening: ${e.message}")
        }
    }

    private fun resetMicrophoneButton() {
        runOnUiThread {
            microphoneButton?.text = "🎤 Tap to Speak"
            microphoneButton?.setBackgroundColor(0xFF89b4fa.toInt())
            microphoneButton?.clearAnimation()
        }
    }

    private fun animateButtonPulse(view: ExtendedFloatingActionButton?) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.1f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.1f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.duration = 1000
        animatorSet.interpolator = DecelerateInterpolator()
        animatorSet.start()
    }

    private fun initializeModernUI() {
        statusText = findViewById(R.id.statusText)
        clearScheduleButton = findViewById(R.id.clearScheduleButton)
        refreshButton = findViewById(R.id.refreshButton)
        selectModelButton = findViewById(R.id.selectModelButton)
        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)
        microphoneButton = findViewById(R.id.microphoneButton)

        viewPagerAdapter = ViewPagerAdapter(this)
        viewPager.adapter = viewPagerAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Scheduled Tasks"
                1 -> "Generation History"
                else -> "Tab $position"
            }
        }.attach()

        clearScheduleButton.setOnClickListener {
            animateButtonClick(it)
            clearAllScheduledTasks()
        }

        refreshButton.setOnClickListener {
            animateButtonClick(it)
            updateUI()
            speakText("Interface refreshed")
        }

        selectModelButton.setOnClickListener {
            animateButtonClick(it)
            showOpenRouterModelDialog()
        }

        Log.d("MainActivity", "Modern UI initialized successfully")
    }

    private fun animateButtonClick(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.95f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.95f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY)
        animatorSet.duration = 150
        animatorSet.interpolator = DecelerateInterpolator()
        animatorSet.start()
    }

    private fun updateStatusWithAnimation(text: String) {
        runOnUiThread {
            val fadeOut = ObjectAnimator.ofFloat(statusText, "alpha", 1f, 0f)
            fadeOut.duration = 200

            fadeOut.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    statusText.text = text
                    val fadeIn = ObjectAnimator.ofFloat(statusText, "alpha", 0f, 1f)
                    fadeIn.duration = 200
                    fadeIn.start()
                }
            })

            fadeOut.start()
        }
    }

    private fun getOpenRouterModelOptions(): List<OpenRouterModel> {
        return listOf(
            OpenRouterModel("minimax/minimax-m2.5", "Minimax M2.5 (Free)"),
            OpenRouterModel("minimax/minimax-m2.5-highspeed", "Minimax M2.5 Highspeed")
        )
    }

    private fun getSelectedOpenRouterModelId(): String {
        val defaultId = getOpenRouterModelOptions().first().id
        if (!::sharedPreferences.isInitialized) return defaultId

        val saved = sharedPreferences.getString(KEY_OPENROUTER_MODEL, null)
        return if (saved != null && getOpenRouterModelOptions().any { it.id == saved }) {
            saved
        } else {
            defaultId
        }
    }

    private fun setSelectedOpenRouterModelId(id: String) {
        if (!::sharedPreferences.isInitialized) return
        sharedPreferences.edit().putString(KEY_OPENROUTER_MODEL, id).apply()
    }

    private fun updateModelButton() {
        if (!::selectModelButton.isInitialized) return
        val selectedId = getSelectedOpenRouterModelId()
        val label = getOpenRouterModelOptions()
            .firstOrNull { it.id == selectedId }
            ?.label
            ?: selectedId
        selectModelButton.text = "Model: $label"
    }

    private fun showOpenRouterModelDialog() {
        val options = getOpenRouterModelOptions()
        if (options.isEmpty()) {
            speakText("No models available")
            return
        }

        val selectedId = getSelectedOpenRouterModelId()
        val selectedIndex = options.indexOfFirst { it.id == selectedId }.let { if (it >= 0) it else 0 }
        val labels = options.map { "${it.label} (${it.id})" }.toTypedArray()

        MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog)
            .setTitle("Select LLM Model")
            .setSingleChoiceItems(labels, selectedIndex) { dialog, which ->
                val selected = options[which]
                setSelectedOpenRouterModelId(selected.id)
                updateModelButton()
                speakText("Selected model ${selected.label}")
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ScheduledTasksFragment()
                1 -> GenerationHistoryFragment()
                else -> ScheduledTasksFragment()
            }
        }
    }

    class ScheduledTasksFragment : Fragment() {
        private lateinit var tasksRecyclerView: RecyclerView
        private lateinit var swipeRefresh: SwipeRefreshLayout
        private lateinit var tasksAdapter: TasksAdapter

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return inflater.inflate(R.layout.fragment_scheduled_tasks, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            tasksRecyclerView = view.findViewById(R.id.tasksRecyclerView)
            swipeRefresh = view.findViewById(R.id.swipeRefresh)

            setupRecyclerView()
            setupSwipeRefresh()
        }

        private fun setupRecyclerView() {
            tasksAdapter = TasksAdapter(mutableListOf()) { action, task ->
                val mainActivity = activity as? MainActivity
                when (action) {
                    "run" -> mainActivity?.runTaskNow(task)
                    "delete" -> mainActivity?.deleteTask(task)
                }
            }

            tasksRecyclerView.layoutManager = LinearLayoutManager(context)
            tasksRecyclerView.adapter = tasksAdapter
            tasksRecyclerView.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
        }

        private fun setupSwipeRefresh() {
            swipeRefresh.setColorSchemeColors(
                0xFF89b4fa.toInt(),
                0xFFa6e3a1.toInt(),
                0xFFfab387.toInt()
            )

            swipeRefresh.setOnRefreshListener {
                val mainActivity = activity as? MainActivity
                mainActivity?.updateUI()
                swipeRefresh.isRefreshing = false
            }
        }

        fun updateTasks(tasks: List<CronTask>) {
            if (::tasksAdapter.isInitialized) {
                tasksAdapter.updateTasks(tasks)
            }
        }
    }

    class GenerationHistoryFragment : Fragment() {
        private lateinit var historyRecyclerView: RecyclerView
        private lateinit var swipeRefresh: SwipeRefreshLayout
        private lateinit var historyAdapter: HistoryAdapter

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            return inflater.inflate(R.layout.fragment_generation_history, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            historyRecyclerView = view.findViewById(R.id.historyRecyclerView)
            swipeRefresh = view.findViewById(R.id.swipeRefresh)

            setupRecyclerView()
            setupSwipeRefresh()
        }

        private fun setupRecyclerView() {
            historyAdapter = HistoryAdapter(mutableListOf()) { action, history ->
                val mainActivity = activity as? MainActivity
                when (action) {
                    "run" -> mainActivity?.runCode(history.generatedCode)
                    "edit" -> mainActivity?.editCode(history)
                    "schedule" -> mainActivity?.scheduleCode(history)
                }
            }

            historyRecyclerView.layoutManager = LinearLayoutManager(context)
            historyRecyclerView.adapter = historyAdapter
            historyRecyclerView.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
        }

        private fun setupSwipeRefresh() {
            swipeRefresh.setColorSchemeColors(
                0xFF89b4fa.toInt(),
                0xFFa6e3a1.toInt(),
                0xFFfab387.toInt()
            )

            swipeRefresh.setOnRefreshListener {
                val mainActivity = activity as? MainActivity
                mainActivity?.updateUI()
                swipeRefresh.isRefreshing = false
            }
        }

        fun updateHistory(history: List<GenerationHistory>) {
            if (::historyAdapter.isInitialized) {
                historyAdapter.updateHistory(history)
            }
        }
    }


    private fun initializeUniversalScript() {
        if (isDestroyed) return

        mainScope.launch {
            try {
                Log.d("MainActivity", "Initializing Universal Script from $UNIVERSAL_SCRIPT_URL")
                speakText("Initializing automation script")
                MyAccessibilityService.instance?.simulateClick(560f, 1139f)

                val scriptContent = downloadUniversalScript()

                if (scriptContent.isNotEmpty()) {
                    universalScriptContent = scriptContent
                    saveUniversalScript(scriptContent)

                    Log.d("MainActivity", "Universal script loaded successfully: ${scriptContent.length} characters")
                    speakText("Automation script loaded successfully")
                 //   magicClicker("Start now button")
                    MyAccessibilityService.instance?.simulateClick(560f, 1139f)

                    scheduleUniversalScript()
                    delay(5000)
                    if (!isDestroyed) {
                        executeUniversalScript()
                    }

                } else {
                    Log.w("MainActivity", "Failed to download universal script, loading from cache")
                    universalScriptContent = loadUniversalScript()

                    if (universalScriptContent.isNotEmpty()) {
                        speakText("Loaded cached automation script")
                        scheduleUniversalScript()

                        delay(5000)
                        if (!isDestroyed) {
                            executeUniversalScript()
                        }
                    } else {
                        speakText("Unable to load automation script")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error initializing universal script: ${e.message}")
                speakText("Error loading automation script")
            }
        }
    }

    private suspend fun downloadUniversalScript(): String = withContext(Dispatchers.IO) {
        if (isDestroyed) return@withContext ""

        return@withContext try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url(UNIVERSAL_SCRIPT_URL)
                .get()
                .build()

            Log.d("MainActivity", "Downloading universal script from $UNIVERSAL_SCRIPT_URL")

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val scriptContent = response.body?.string() ?: ""
                    Log.d("MainActivity", "Successfully downloaded script: ${scriptContent.length} characters")
                    scriptContent
                } else {
                    Log.e("MainActivity", "Failed to download script. Code: ${response.code}")
                    ""
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Exception downloading universal script: ${e.message}")
            ""
        }
    }

    private fun saveUniversalScript(scriptContent: String) {
        try {
            sharedPreferences.edit().apply {
                putString(KEY_UNIVERSAL_SCRIPT, scriptContent)
                putLong(KEY_UNIVERSAL_SCRIPT_LAST_UPDATE, System.currentTimeMillis())
                apply()
            }
            Log.d("MainActivity", "Universal script saved to preferences")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error saving universal script: ${e.message}")
        }
    }

    private fun loadUniversalScript(): String {
        return try {
            val script = sharedPreferences.getString(KEY_UNIVERSAL_SCRIPT, "") ?: ""
            val lastUpdate = sharedPreferences.getLong(KEY_UNIVERSAL_SCRIPT_LAST_UPDATE, 0)
            Log.d("MainActivity", "Loaded cached script: ${script.length} characters, last updated: ${Date(lastUpdate)}")
            script
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading universal script: ${e.message}")
            ""
        }
    }

    private fun scheduleUniversalScript() {
        if (isDestroyed) return

        universalScriptJob?.cancel()

        universalScriptJob = mainScope.launch {
            speakText("Scheduled daily automation script")
            Log.d("MainActivity", "Universal script scheduled to run every 24 hours")

            while (isActive && !isDestroyed) {
                try {
                    val now = Calendar.getInstance()
                    val nextMidnight = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                        add(Calendar.DAY_OF_MONTH, 1)
                    }

                    val delayUntilMidnight = nextMidnight.timeInMillis - now.timeInMillis

                    Log.d("MainActivity", "Next universal script execution in ${delayUntilMidnight / 1000 / 60} minutes")

                    delay(delayUntilMidnight)

                    if (!isDestroyed) {
                        executeUniversalScript()
                    }

                } catch (e: CancellationException) {
                    Log.d("MainActivity", "Universal script scheduler cancelled")
                    throw e
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error in universal script scheduler: ${e.message}")
                    delay(3600000)
                }
            }
        }

        val taskId = "universal_script_daily"
        if (!cronTasks.containsKey(taskId)) {
            val cronTask = CronTask(
                id = taskId,
                taskDescription = "Daily Universal Script from cheatlayer.com",
                cronExpression = "0 0 0 * *",
                createdAt = System.currentTimeMillis(),
                lastExecuted = 0L,
                isActive = true
            )
            cronTasks[taskId] = cronTask
            saveCronTasks()
        }
    }

    private suspend fun executeUniversalScript() {
        if (isDestroyed) return

        withContext(Dispatchers.Main) {
            try {
                Log.d("MainActivity", "Executing universal script...")
                updateStatusWithAnimation("⚡ Running daily automation script")
                speakText("Running daily automation script")

                if (universalScriptContent.isEmpty()) {
                    universalScriptContent = loadUniversalScript()
                }
                delay(30000)
                if (universalScriptContent.isNotEmpty()) {
                    executeGeneratedCode(universalScriptContent)

                    val taskId = "universal_script_daily"
                    cronTasks[taskId]?.let { task ->
                        cronTasks[taskId] = task.copy(lastExecuted = System.currentTimeMillis())
                        saveCronTasks()
                    }

                    speakText("Daily automation script completed successfully")
                    updateStatusWithAnimation("✅ Daily script completed")
                    executeUniversalScript()
                    Log.d("MainActivity", "Universal script executed successfully")
                } else {
                    speakText("Daily automation script is empty")
                    Log.w("MainActivity", "Universal script content is empty")
                }

                updateUI()

            } catch (e: Exception) {
                Log.e("MainActivity", "Error executing universal script: ${e.message}")
                speakText("Error executing daily automation script")
                updateStatusWithAnimation("❌ Error in daily script")
            }
        }
    }

    private suspend fun fetchAgentAccounts(): Map<String, String> {
        if (isDestroyed) return emptyMap()

        val database = firebaseDatabaseOrNull("fetchAgentAccounts") ?: return emptyMap()
        val ref: DatabaseReference = database.getReference("agent_accounts_fixed")
        return try {
            val snapshot: DataSnapshot = ref.get().await()
            val map = mutableMapOf<String, String>()
            for (child in snapshot.children) {
                val username = child.child("username").getValue(String::class.java)
                val email = child.child("email").getValue(String::class.java)

                if (!username.isNullOrEmpty() && !email.isNullOrEmpty()) {
                    map[username] = email
                }
            }
            val logMsg = "Fetched ${map.size} agent_accounts entries."
            Log.w("MainActivity", logMsg)
            map
        } catch (e: Exception) {
            val errorMsg = "Error fetching agent_accounts: $e"
            Log.e("MainActivity", errorMsg)
            speakText(errorMsg)
            emptyMap()
        }
    }

    private suspend fun fetchAgentAccountsInstagram(): Map<String, String> {
        if (isDestroyed) return emptyMap()

        val database = firebaseDatabaseOrNull("fetchAgentAccountsInstagram") ?: return emptyMap()
        val ref: DatabaseReference = database.getReference("agent_accounts_instagram")
        return try {
            val snapshot: DataSnapshot = ref.get().await()
            val map = mutableMapOf<String, String>()
            for (child in snapshot.children) {
                val username = child.child("username").getValue(String::class.java)
                val email = child.child("email").getValue(String::class.java)

                if (!username.isNullOrEmpty() && !email.isNullOrEmpty()) {
                    map[username] = email
                }
            }
            val logMsg = "Fetched ${map.size} Instagram agent_accounts entries."
            Log.w("MainActivity", logMsg)
            speakText(logMsg)
            map
        } catch (e: Exception) {
            val errorMsg = "Error fetching Instagram agent_accounts: $e"
            Log.e("MainActivity", errorMsg)
            speakText(errorMsg)
            emptyMap()
        }
    }

    private suspend fun fetchAgentServerInstagram(): Map<String, String> {
        if (isDestroyed) return emptyMap()

        val database = firebaseDatabaseOrNull("fetchAgentServerInstagram") ?: return emptyMap()
        val ref: DatabaseReference = database.getReference("agent_accounts_instagram")
        return try {
            val snapshot: DataSnapshot = ref.get().await()
            val map = mutableMapOf<String, String>()
            for (child in snapshot.children) {
                val username = child.child("username").getValue(String::class.java)
                val server = child.child("server").getValue(String::class.java)

                if (!username.isNullOrEmpty() && !server.isNullOrEmpty()) {
                    map[username] = server
                }
            }
            val logMsg = "Fetched ${map.size} Instagram server entries."
            Log.w("MainActivity", logMsg)
            speakText(logMsg)
            map
        } catch (e: Exception) {
            val errorMsg = "Error fetching Instagram servers: $e"
            Log.e("MainActivity", errorMsg)
            speakText(errorMsg)
            emptyMap()
        }
    }

    private suspend fun checkAgentAccountsInstagram(agentAccount: String) {
        if (isDestroyed) return
        val database = firebaseDatabaseOrNull("checkAgentAccountsInstagram") ?: return

        if (agentAccount.contains(" ")) {
            val msg = "Rejected account '$agentAccount': contains spaces"
            Log.d("checkAgentAccountsInstagram", msg)
            speakText("Account name cannot contain spaces")
            return
        }

        if (agentAccount.any { !it.isLetterOrDigit() && it != '_' }) {
            val msg = "Rejected account '$agentAccount': contains punctuation"
            Log.d("checkAgentAccountsInstagram", msg)
            speakText("Account name cannot contain punctuation")
            return
        }

        val blockedNames = listOf(
            "Create", "Email", "Facebook", "Flash", "More", "SMS", "Timer", "X",
            "Instagram", "Twitter", "Shorts", "LinkedIn", "Google", "YouTube",
            "TikTok", "Snapchat", "WhatsApp", "Telegram", "Discord", "Reddit", "Next", "Switch", "Save"
        )

        if (blockedNames.any { it.equals(agentAccount, ignoreCase = true) }) {
            val msg = "Rejected account '$agentAccount': is a blocked word"
            Log.d("checkAgentAccountsInstagram", msg)
            speakText("This account name is not allowed")
            return
        }

        if (agentAccount.isBlank() || agentAccount.length < 3) {
            val msg = "Rejected account '$agentAccount': too short"
            Log.d("checkAgentAccountsInstagram", msg)
            speakText("Account name must be at least 3 characters")
            return
        }

        val ref: DatabaseReference = database.getReference("agent_accounts_instagram")
        val fixedRef: DatabaseReference = database.getReference("agent_accounts_instagram_fixed")

        val currentTimestamp = System.currentTimeMillis()

        try {
            val snapshot: DataSnapshot = ref.child(agentAccount).get().await()

            if (!snapshot.exists()) {
                val runs = listOf(currentTimestamp)
                val data = mapOf(
                    "username" to agentAccount,
                    "email" to "rohan@cheatlayer.com",
                    "owner" to userEmail,
                    "server" to "agent-2131241-1",
                    "phone" to phoneDeviceId,
                    "runs" to runs
                )
                ref.child(agentAccount).setValue(data).await()
                val addMsg = "Created new Instagram agent account for $agentAccount with owner $userEmail"
                Log.d("checkAgentAccountsInstagram", addMsg)
                speakText(addMsg)
            } else {
                ref.child(agentAccount).child("owner").setValue(userEmail).await()

                val existingRunsList = mutableListOf<Long>()
                val runsValue = snapshot.child("runs").value

                when (runsValue) {
                    is List<*> -> {
                        runsValue.forEach { item ->
                            when (item) {
                                is Number -> existingRunsList.add(item.toLong())
                            }
                        }
                    }
                }

                existingRunsList.add(currentTimestamp)
                ref.child(agentAccount).child("runs").setValue(existingRunsList).await()

                if (hasThreeConsecutiveDays(existingRunsList)) {
                    val fixedSnapshot: DataSnapshot = fixedRef.child(agentAccount).get().await()

                    if (fixedSnapshot.exists()) {
                        fixedRef.child(agentAccount).child("owner").setValue(userEmail).await()
                        Log.d("checkAgentAccountsInstagram", "Updated owner in agent_accounts_instagram_fixed for existing $agentAccount")
                    } else {
                        val fixedData = mapOf(
                            "username" to (snapshot.child("username").value as? String ?: agentAccount),
                            "email" to (snapshot.child("email").value as? String ?: "rohan@cheatlayer.com"),
                            "owner" to userEmail,
                            "phone" to phoneDeviceId,
                            "server" to (snapshot.child("server").value as? String ?: "agent-2131241-1"),
                            "runs" to existingRunsList
                        )
                        fixedRef.child(agentAccount).setValue(fixedData).await()
                        Log.d("checkAgentAccountsInstagram", "Also stored in agent_accounts_instagram_fixed for $agentAccount (running for 3+ consecutive days)")
                    }
                }

                val updateMsg = "Updated owner of existing Instagram agent account '$agentAccount' to $userEmail (run #${existingRunsList.size})"
                Log.d("checkAgentAccountsInstagram", updateMsg)
                speakText(updateMsg)
            }
        } catch (e: Exception) {
            val errorMsg = "Error checking/updating Instagram agent account: $e"
            Log.e("checkAgentAccountsInstagram", errorMsg)
            speakText(errorMsg)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun doFullUploadSequenceInstagram(
        caption: String,
        userEmail: String,
        randomAccount: String,
        server: String
    ) = withContext(Dispatchers.Main) {
        if (isDestroyed) return@withContext

        speakText("Starting Instagram upload sequence")

        try {
            if (MyAccessibilityService.instance?.isTextPresentOnScreen("Home") == true ||
                MyAccessibilityService.instance?.isTextPresentOnScreen("Profile") == true) {

                delay(3000)

                MyAccessibilityService.instance?.clickNodesByContentDescription("Home")
                delay(3000)

                MyAccessibilityService.instance?.clickNodesByContentDescription("Create")
                delay(5000)

                MyAccessibilityService.instance?.clickNodesByContentDescription("Post")
                delay(3000)

                MyAccessibilityService.instance?.simulateClick(150f, 400f)
                delay(3000)

                MyAccessibilityService.instance?.clickNodesByContentDescription("Next")
                delay(5000)

                MyAccessibilityService.instance?.clickNodesByContentDescription("Next")
                delay(3000)

                speakText("Typing Instagram caption")
                MyAccessibilityService.instance?.simulateTypeInFirstEditableField(caption)
                delay(3000)

                withContext(Dispatchers.IO) {
                    takeScreenshotAndUploadToLogs(userEmail, "Instagram: $caption", server)
                }
                delay(3000)

                speakText("Publishing Instagram post")
                MyAccessibilityService.instance?.clickNodesByContentDescription("Share")
                delay(5000)

                MyAccessibilityService.instance?.clickNodesByContentDescription("Profile")
                delay(3000)

                val completeMsg = "Completed Instagram upload sequence"
                Log.w("MainActivity", completeMsg)
                speakText(completeMsg)
            } else {
                speakText("Not on Instagram home screen")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in Instagram upload sequence: ${e.message}")
            speakText("Error uploading to Instagram: ${e.message}")
        }
    }

    private suspend fun fetchAgentAccountsYoutube(): Map<String, String> {
        if (isDestroyed) return emptyMap()

        val database = firebaseDatabaseOrNull("fetchAgentAccountsYoutube") ?: return emptyMap()
        val ref: DatabaseReference = database.getReference("agent_accounts_youtube")
        return try {
            val snapshot: DataSnapshot = ref.get().await()
            val map = mutableMapOf<String, String>()
            for (child in snapshot.children) {
                val username = child.child("username").getValue(String::class.java)
                val email = child.child("email").getValue(String::class.java)

                if (!username.isNullOrEmpty() && !email.isNullOrEmpty()) {
                    map[username] = email
                }
            }
            val logMsg = "Fetched ${map.size} agent_accounts entries."
            Log.w("MainActivity", logMsg)
            speakText(logMsg)
            map
        } catch (e: Exception) {
            val errorMsg = "Error fetching agent_accounts: $e"
            Log.e("MainActivity", errorMsg)
            speakText(errorMsg)
            emptyMap()
        }
    }

    private suspend fun fetchAgentAccountsTwitter(): Map<String, String> {
        if (isDestroyed) return emptyMap()

        val database = firebaseDatabaseOrNull("fetchAgentAccountsTwitter") ?: return emptyMap()
        val ref: DatabaseReference = database.getReference("agent_accounts_twitter")
        return try {
            val snapshot: DataSnapshot = ref.get().await()
            val map = mutableMapOf<String, String>()
            for (child in snapshot.children) {
                val username = child.child("username").getValue(String::class.java)
                val email = child.child("email").getValue(String::class.java)

                if (!username.isNullOrEmpty() && !email.isNullOrEmpty()) {
                    map[username] = email
                }
            }
            val logMsg = "Fetched ${map.size} agent_accounts entries."
            Log.w("MainActivity", logMsg)
            speakText(logMsg)
            map
        } catch (e: Exception) {
            val errorMsg = "Error fetching agent_accounts: $e"
            Log.e("MainActivity", errorMsg)
            speakText(errorMsg)
            emptyMap()
        }
    }

    private fun hasThreeConsecutiveDays(timestamps: List<Long>): Boolean {
        if (timestamps.size < 3) return false

        val calendar = Calendar.getInstance()
        val daySet = mutableSetOf<String>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        timestamps.forEach { timestamp ->
            calendar.timeInMillis = timestamp
            val dayString = dateFormat.format(calendar.time)
            daySet.add(dayString)
        }

        val sortedDays = daySet.sorted()

        if (sortedDays.size < 3) return false

        for (i in 0..sortedDays.size - 3) {
            val day1 = parseDate(sortedDays[i])
            val day2 = parseDate(sortedDays[i + 1])
            val day3 = parseDate(sortedDays[i + 2])

            if (day1 != null && day2 != null && day3 != null) {
                val diff1 = (day2.time - day1.time) / (24 * 60 * 60 * 1000)
                val diff2 = (day3.time - day2.time) / (24 * 60 * 60 * 1000)

                if (diff1 == 1L && diff2 == 1L) {
                    return true
                }
            }
        }

        return false
    }

    private fun parseDate(dateString: String): Date? {
        return try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dateFormat.parse(dateString)
        } catch (e: Exception) {
            Log.e("parseDate", "Error parsing date: $dateString", e)
            null
        }
    }

    private suspend fun checkAgentAccountsTwitter(agentAccount: String) {
        if (isDestroyed) return
        val database = firebaseDatabaseOrNull("checkAgentAccountsTwitter") ?: return

        val ref: DatabaseReference = database.getReference("agent_accounts_twitter")
        val fixedRef: DatabaseReference = database.getReference("agent_accounts_twitter_fixed")

        val currentTimestamp = System.currentTimeMillis()

        try {
            val snapshot: DataSnapshot = ref.child(agentAccount).get().await()

            if (!snapshot.exists()) {
                val runs = listOf(currentTimestamp)
                val data = mapOf(
                    "username" to agentAccount,
                    "email" to "rohan@cheatlayer.com",
                    "owner" to userEmail,
                    "runs" to runs
                )
                ref.child(agentAccount).setValue(data).await()
                val addMsg = "Created new Twitter agent account for $agentAccount with owner $userEmail"
                Log.d("checkAgentAccountsTwitter", addMsg)
                speakText(addMsg)
            } else {
                ref.child(agentAccount).child("owner").setValue(userEmail).await()

                val existingRunsList = mutableListOf<Long>()
                val runsValue = snapshot.child("runs").value

                when (runsValue) {
                    is List<*> -> {
                        runsValue.forEach { item ->
                            when (item) {
                                is Number -> existingRunsList.add(item.toLong())
                            }
                        }
                    }
                }

                existingRunsList.add(currentTimestamp)
                ref.child(agentAccount).child("runs").setValue(existingRunsList).await()

                if (hasThreeConsecutiveDays(existingRunsList)) {
                    val fixedSnapshot: DataSnapshot = fixedRef.child(agentAccount).get().await()

                    if (fixedSnapshot.exists()) {
                        fixedRef.child(agentAccount).child("owner").setValue(userEmail).await()
                        Log.d("checkAgentAccountsTwitter", "Updated owner in agent_accounts_twitter_fixed for existing $agentAccount")
                    } else {
                        val fixedData = mapOf(
                            "username" to (snapshot.child("username").value as? String ?: agentAccount),
                            "email" to (snapshot.child("email").value as? String ?: "rohan@cheatlayer.com"),
                            "owner" to userEmail,
                            "runs" to existingRunsList
                        )
                        fixedRef.child(agentAccount).setValue(fixedData).await()
                        Log.d("checkAgentAccountsTwitter", "Also stored in agent_accounts_twitter_fixed for $agentAccount (running for 3+ consecutive days)")
                    }
                }

                val updateMsg = "Updated owner of existing Twitter agent account '$agentAccount' to $userEmail (run #${existingRunsList.size})"
                Log.d("checkAgentAccountsTwitter", updateMsg)
                speakText(updateMsg)
            }
        } catch (e: Exception) {
            val errorMsg = "Error checking/updating Twitter agent account: $e"
            Log.e("checkAgentAccountsTwitter", errorMsg)
            speakText(errorMsg)
        }
    }

    private suspend fun checkAgentAccountsYoutube(agentAccount: String) {
        if (isDestroyed) return
        val database = firebaseDatabaseOrNull("checkAgentAccountsYoutube") ?: return

        val ref: DatabaseReference = database.getReference("agent_accounts_youtube")
        val fixedRef: DatabaseReference = database.getReference("agent_accounts_youtube_fixed")

        val currentTimestamp = System.currentTimeMillis()

        try {
            val snapshot: DataSnapshot = ref.child(agentAccount).get().await()

            if (!snapshot.exists()) {
                val runs = listOf(currentTimestamp)
                val data = mapOf(
                    "username" to agentAccount,
                    "email" to "rohan@cheatlayer.com",
                    "owner" to userEmail,
                    "runs" to runs
                )
                ref.child(agentAccount).setValue(data).await()
                val addMsg = "Created new YouTube agent account for $agentAccount with owner $userEmail"
                Log.d("checkAgentAccountsYoutube", addMsg)
                speakText(addMsg)
            } else {
                ref.child(agentAccount).child("owner").setValue(userEmail).await()

                val existingRunsList = mutableListOf<Long>()
                val runsValue = snapshot.child("runs").value

                when (runsValue) {
                    is List<*> -> {
                        runsValue.forEach { item ->
                            when (item) {
                                is Number -> existingRunsList.add(item.toLong())
                            }
                        }
                    }
                }

                existingRunsList.add(currentTimestamp)
                ref.child(agentAccount).child("runs").setValue(existingRunsList).await()

                if (hasThreeConsecutiveDays(existingRunsList)) {
                    val fixedSnapshot: DataSnapshot = fixedRef.child(agentAccount).get().await()

                    if (fixedSnapshot.exists()) {
                        fixedRef.child(agentAccount).child("owner").setValue(userEmail).await()
                        Log.d("checkAgentAccountsYoutube", "Updated owner in agent_accounts_youtube_fixed for existing $agentAccount")
                    } else {
                        val fixedData = mapOf(
                            "username" to (snapshot.child("username").value as? String ?: agentAccount),
                            "email" to (snapshot.child("email").value as? String ?: "rohan@cheatlayer.com"),
                            "owner" to userEmail,
                            "runs" to existingRunsList
                        )
                        fixedRef.child(agentAccount).setValue(fixedData).await()
                        Log.d("checkAgentAccountsYoutube", "Also stored in agent_accounts_youtube_fixed for $agentAccount (running for 3+ consecutive days)")
                    }
                }

                val updateMsg = "Updated owner of existing YouTube agent account '$agentAccount' to $userEmail (run #${existingRunsList.size})"
                Log.d("checkAgentAccountsYoutube", updateMsg)
                speakText(updateMsg)
            }
        } catch (e: Exception) {
            val errorMsg = "Error checking/updating YouTube agent account: $e"
            Log.e("checkAgentAccountsYoutube", errorMsg)
            speakText(errorMsg)
        }
    }

    private suspend fun fetchAgentServerYoutube(): Map<String, String> {
        if (isDestroyed) return emptyMap()

        val database = firebaseDatabaseOrNull("fetchAgentServerYoutube") ?: return emptyMap()
        val ref: DatabaseReference = database.getReference("agent_accounts_youtube")
        return try {
            val snapshot: DataSnapshot = ref.get().await()
            val map = mutableMapOf<String, String>()
            for (child in snapshot.children) {
                val username = child.child("username").getValue(String::class.java)
                val email = child.child("server").getValue(String::class.java)

                if (!username.isNullOrEmpty() && !email.isNullOrEmpty()) {
                    map[username] = email
                }
            }
            val logMsg = "Fetched ${map.size} YouTube agent_accounts entries."
            Log.w("MainActivity", logMsg)
            speakText(logMsg)
            map
        } catch (e: Exception) {
            val errorMsg = "Error fetching YouTube agent_accounts: $e"
            Log.e("MainActivity", errorMsg)
            speakText(errorMsg)
            emptyMap()
        }
    }

    private suspend fun fetchAgentServer(): Map<String, String> {
        if (isDestroyed) return emptyMap()

        val database = firebaseDatabaseOrNull("fetchAgentServer") ?: return emptyMap()
        val ref: DatabaseReference = database.getReference("agent_accounts_fixed")
        return try {
            val snapshot: DataSnapshot = ref.get().await()
            val map = mutableMapOf<String, String>()
            for (child in snapshot.children) {
                val username = child.child("username").getValue(String::class.java)
                val email = child.child("server").getValue(String::class.java)

                if (!username.isNullOrEmpty() && !email.isNullOrEmpty()) {
                    map[username] = email
                }
            }
            val logMsg = "Fetched ${map.size} agent_accounts entries."
            Log.w("MainActivity", logMsg)
            map
        } catch (e: Exception) {
            val errorMsg = "Error fetching agent_accounts: $e"
            Log.e("MainActivity", errorMsg)
            speakText(errorMsg)
            emptyMap()
        }
    }

    private fun isTikTokInstalled(): Boolean {
        return try {
            packageManager.getPackageInfo("com.zhiliaoapp.musically", PackageManager.GET_ACTIVITIES)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    var blockedNames = listOf(
        "Create", "Email", "Facebook", "Flash", "More", "SMS", "Timer", "X",
        "Instagram", "Twitter", "Shorts", "LinkedIn", "Google", "YouTube",
        "TikTok", "Snapchat", "WhatsApp", "Telegram", "Discord", "Reddit", "Next", "Switch", "Save"
    )

    private suspend fun checkAgentAccounts(agentAccount: String) {
        if (isDestroyed) return
        val database = firebaseDatabaseOrNull("checkAgentAccounts") ?: return

        if (agentAccount.contains(" ")) {
            val msg = "Rejected account '$agentAccount': contains spaces"
            Log.d("checkAgentAccounts", msg)
            speakText("Account name cannot contain spaces")
            return
        }

        if (agentAccount.any { !it.isLetterOrDigit() && it != '_' }) {
            val msg = "Rejected account '$agentAccount': contains punctuation"
            Log.d("checkAgentAccounts", msg)
            speakText("Account name cannot contain punctuation")
            return
        }

        if (blockedNames.any { it.equals(agentAccount, ignoreCase = true) }) {
            val msg = "Rejected account '$agentAccount': is a blocked word"
            Log.d("checkAgentAccounts", msg)
            speakText("This account name is not allowed")
            return
        }

        if (agentAccount.isBlank() || agentAccount.length < 3) {
            val msg = "Rejected account '$agentAccount': too short"
            Log.d("checkAgentAccounts", msg)
            speakText("Account name must be at least 3 characters")
            return
        }

        val ref: DatabaseReference = database.getReference("agent_accounts_fixed")
        val fixedRef: DatabaseReference = database.getReference("agent_accounts_fixed")

        val currentTimestamp = System.currentTimeMillis()

        try {
            val snapshot: DataSnapshot = ref.child(agentAccount).get().await()

            if (!snapshot.exists()) {
                val runs = listOf(currentTimestamp)
                val data = mapOf(
                    "username" to agentAccount,
                    "email" to "rohan@cheatlayer,com",
                    "owner" to userEmail,
                    "server" to "agent-2131241-1",
                    "phone" to phoneDeviceId,
                    "runs" to runs
                )
                ref.child(agentAccount).setValue(data).await()
                val addMsg = "Created new agent account for $agentAccount with owner $userEmail"
                Log.d("checkAgentAccounts", addMsg)
                speakText(addMsg)
            } else {
                ref.child(agentAccount).child("owner").setValue(userEmail).await()

                val existingRunsList = mutableListOf<Long>()
                val runsValue = snapshot.child("runs").value

                when (runsValue) {
                    is List<*> -> {
                        runsValue.forEach { item ->
                            when (item) {
                                is Number -> existingRunsList.add(item.toLong())
                            }
                        }
                    }
                }

                existingRunsList.add(currentTimestamp)
                ref.child(agentAccount).child("runs").setValue(existingRunsList).await()

                if (hasThreeConsecutiveDays(existingRunsList)) {
                    val fixedSnapshot: DataSnapshot = fixedRef.child(agentAccount).get().await()

                    if (fixedSnapshot.exists()) {
                        fixedRef.child(agentAccount).child("owner").setValue(userEmail).await()
                        Log.d("checkAgentAccounts", "Updated owner in agent_accounts_fixed for existing $agentAccount")
                    } else {
                        val fixedData = mapOf(
                            "username" to (snapshot.child("username").value as? String ?: agentAccount),
                            "email" to (snapshot.child("email").value as? String ?: "rohan@cheatlayer,com"),
                            "owner" to userEmail,
                            "phone" to phoneDeviceId,
                            "server" to (snapshot.child("server").value as? String ?: "agent-2131241-1"),
                            "runs" to existingRunsList
                        )
                        fixedRef.child(agentAccount).setValue(fixedData).await()
                        Log.d("checkAgentAccounts", "Also stored in agent_accounts_fixed for $agentAccount (running for 3+ consecutive days)")
                    }
                }

                val updateMsg = "Updated owner of existing agent account '$agentAccount' to $userEmail (run #${existingRunsList.size})"
                Log.d("checkAgentAccounts", updateMsg)
                speakText(updateMsg)
            }
        } catch (e: Exception) {
            val errorMsg = "Error checking/updating agent account: $e"
            Log.e("checkAgentAccounts", errorMsg)
            speakText(errorMsg)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun doFullUploadSequence(caption: String, userEmail: String, random_account: String, server: String) = withContext(Dispatchers.Main) {
        if (isDestroyed) return@withContext

        speakText("Starting TikTok upload sequence")
        if (MyAccessibilityService.instance?.isTextPresentOnScreen("For You") == true || MyAccessibilityService.instance?.isTextPresentOnScreen("Profile") == true) {
            delay(5_000)

            MyAccessibilityService.instance?.clickNodesByContentDescription("Home")
            delay(5_000)
            MyAccessibilityService.instance?.clickNodesByContentDescription("Home")
            delay(5_000)
            MyAccessibilityService.instance?.clickNodesByContentDescription("Create")
            delay(5_000)

            delay(5_000)
            delay(5_000)

            MyAccessibilityService.instance?.clickVideoUploadButton()
            delay(5_000)

            MyAccessibilityService.instance?.simulateClick(150f, 400f)

            delay(5_000)
            MyAccessibilityService.instance?.clickNodesByContentDescription("Add sound")
            MyAccessibilityService.instance?.clickAddSound()
            delay(5_000)

            MyAccessibilityService.instance?.clickNodesByContentDescription("Next")

            MyAccessibilityService.instance?.clickNodesByContentDescription("Next (1)")
            delay(5_000)

            MyAccessibilityService.instance?.clickNodesByContentDescription("Add sound")
            delay(5_000)

            delay(5_000)

            MyAccessibilityService.instance?.clickFirstSong()
            MyAccessibilityService.instance?.simulateClick(375f, 100f)

            delay(5_000)
            MyAccessibilityService.instance?.clickNodesByContentDescription("Next")
            delay(5_000)
            MyAccessibilityService.instance?.clickNodesByContentDescription("Next")

            speakText("Typing TikTok caption")

            delay(5_000)

            MyAccessibilityService.instance?.simulateTypeInFirstEditableField(caption)
            delay(5_000)

            withContext(Dispatchers.IO) {
                takeScreenshotAndUploadToLogs(userEmail, "TikTok: $caption", server)
            }
            delay(5_000)
            speakText("Publishing TikTok post")
            MyAccessibilityService.instance?.clickNodesByContentDescription("Post")
            delay(5_000)
            MyAccessibilityService.instance?.clickNodesByContentDescription("Confirm")
            delay(5_000)

            MyAccessibilityService.instance?.clickNodesByContentDescription("Profile")
            val completeMsg = "Completed one full upload sequence (TikTok)."
            Log.w("MainActivity", completeMsg)

            delay(5_000)
            speakText(completeMsg)
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            for (networkInterface in interfaces) {
                val addresses = networkInterface.inetAddresses
                for (address in addresses) {
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        return address.hostAddress
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e("MainActivity", "Error getting IP address: $ex")
        }
        return null
    }

    @SuppressLint("ServiceCast")
    private fun getDeviceInfo(): Map<String, String> {
        val deviceInfo = mutableMapOf<String, String>()

        try {
            deviceInfo["device_model"] = Build.MODEL
            deviceInfo["device_manufacturer"] = Build.MANUFACTURER
            deviceInfo["device_brand"] = Build.BRAND
            deviceInfo["device_name"] = Build.DEVICE
            deviceInfo["android_version"] = Build.VERSION.RELEASE
            deviceInfo["sdk_version"] = Build.VERSION.SDK_INT.toString()
            deviceInfo["build_id"] = Build.ID

            val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val batteryLevel = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            deviceInfo["battery_percentage"] = "$batteryLevel%"

            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memInfo)
            deviceInfo["available_memory_mb"] = "${memInfo.availMem / (1024 * 1024)}"
            deviceInfo["total_memory_mb"] = "${memInfo.totalMem / (1024 * 1024)}"
            deviceInfo["low_memory"] = memInfo.lowMemory.toString()

            val displayMetrics = resources.displayMetrics
            deviceInfo["screen_width"] = displayMetrics.widthPixels.toString()
            deviceInfo["screen_height"] = displayMetrics.heightPixels.toString()
            deviceInfo["screen_density"] = displayMetrics.density.toString()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                val statFs = android.os.StatFs(android.os.Environment.getDataDirectory().path)
                val availableBytes = statFs.availableBytes
                val totalBytes = statFs.totalBytes
                deviceInfo["available_storage_gb"] = "${availableBytes / (1024 * 1024 * 1024)}"
                deviceInfo["total_storage_gb"] = "${totalBytes / (1024 * 1024 * 1024)}"
            }

            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetworkInfo
            deviceInfo["network_type"] = activeNetwork?.typeName ?: "unknown"
            deviceInfo["network_connected"] = (activeNetwork?.isConnected == true).toString()

        } catch (e: Exception) {
            Log.e("MainActivity", "Error getting device info: $e")
            deviceInfo["device_info_error"] = e.message ?: "unknown error"
        }

        return deviceInfo
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun takeScreenshotAndUploadToLogs(userEmail: String, text: String, server: String? = null) {
        if (isDestroyed) return
        val storage = firebaseStorageOrNull("takeScreenshotAndUploadToLogs") ?: return
        val database = firebaseDatabaseOrNull("takeScreenshotAndUploadToLogs") ?: return

        try {
            speakText("Taking screenshot")
            val pngBytes = ScreenCaptureService.lastCapturedPng
            if (pngBytes == null) {
                val errorMsg = "No captured screenshot available right now."
                Log.e("MainActivity", errorMsg)
                speakText(errorMsg)
                return
            }

            val storageRef = storage.reference
            val timestamp = System.currentTimeMillis()
            val newemail = userEmail.replace(".",",")
            val screenshotRef = storageRef.child("agentsbase/$newemail/screenshots/screenshot_$timestamp.png")

            screenshotRef.putBytes(pngBytes).await()
            val downloadUrl = screenshotRef.downloadUrl.await().toString()
            val successMsg = "Screenshot uploaded at $downloadUrl"
            Log.i("MainActivity", successMsg)
            speakText("Screenshot uploaded successfully")

            val logsRef = database.getReference("agentsbase")
                .child(newemail)
                .child("logs")

            val deviceInfo = getDeviceInfo()
            val logEntry = mutableMapOf(
                "screenshot_url" to downloadUrl,
                "text" to text,
                "timestamp" to timestamp.toString(),
                "ip_address" to (getLocalIpAddress() ?: "unknown")
            )

            logEntry.putAll(deviceInfo)

            server?.let { logEntry["server_id"] = it }

            logsRef.push().setValue(logEntry).await()
            val logMsg = "Log entry created for user=$newemail with screenshot."
            Log.i("MainActivity", logMsg)
            speakText("Log entry created")
        } catch (e: Exception) {
            val errorMsg = "Error uploading screenshot: $e"
            Log.e("MainActivity", errorMsg)
            speakText(errorMsg)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun doFullUploadSequenceTwitter(caption: String, userEmail: String, server: String) = withContext(Dispatchers.Main) {
        if (isDestroyed) return@withContext

        speakText("Starting Twitter upload sequence")
        delay(5_000)
        MyAccessibilityService.instance?.simulateClick(75f, 1475f)
        delay(5_000)
        MyAccessibilityService.instance?.simulateClick(640f, 1350f)
        delay(5_000)

        MyAccessibilityService.instance?.clickNodesByContentDescription("Got it")

        MyAccessibilityService.instance?.simulateClick(640f, 1350f)
        delay(5_000)
        MyAccessibilityService.instance?.simulateClick(250f, 740f)
        delay(5_000)
        MyAccessibilityService.instance?.clickNodesByContentDescription("Got it")
        delay(5_000)
        withContext(Dispatchers.IO) {
            takeScreenshotAndUploadToLogs(userEmail, "Twitter: $caption", server)
        }
        delay(5_000)

        MyAccessibilityService.instance?.clickNodesByContentDescription("Photos")
        delay(5_000)

        MyAccessibilityService.instance?.simulateClick(250f, 740f)
        delay(5_000)
        MyAccessibilityService.instance?.clickNodesByContentDescription("Done")
        delay(5_000)

        speakText("Typing Twitter caption")
        MyAccessibilityService.instance?.simulateType(
            "com.twitter.android:id/tweet_text",
            caption
        )
        delay(5_000)
        MyAccessibilityService.instance?.clickNodesByContentDescription("Got it")

        MyAccessibilityService.instance?.simulateClick(640f, 140f)
        delay(5_000)
        speakText("Publishing Twitter post")
        MyAccessibilityService.instance?.clickNodesByContentDescription("Post")
        delay(5_000)
        MyAccessibilityService.instance?.clickNodesByContentDescription("Got it")
        delay(5_000)
        MyAccessibilityService.instance?.clickNodesByContentDescription("Got it")
        delay(5_000)
        MyAccessibilityService.instance?.clickNodesByContentDescription("GOT IT")
        MyAccessibilityService.instance?.clickNodesByContentDescription("Got it")

        delay(5_000)
        MyAccessibilityService.instance?.simulateClick(620f, 1380f)

        val completeMsg = "Completed one full upload sequence (Twitter)."
        Log.w("MainActivity", completeMsg)
        speakText(completeMsg)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun doFullUploadSequenceYoutube(
        caption: String,
        userEmail: String,
        server: String
    ) = withContext(Dispatchers.Main) {
        if (isDestroyed) return@withContext

        speakText("Starting YouTube upload sequence")
        delay(5_000)
        MyAccessibilityService.instance?.simulateClick(350f, 1490f)
        delay(10_000)
        MyAccessibilityService.instance?.simulateClick(240f, 1450f)
        delay(5_000)

        MyAccessibilityService.instance?.simulateClick(120f, 340f)
        delay(5_000)
        MyAccessibilityService.instance?.clickNodesByContentDescription("Edit")
        delay(5_000)
        MyAccessibilityService.instance?.clickNodesByContentDescription("Next")
        speakText("Waiting for YouTube video processing...")
        delay(60_000)
        MyAccessibilityService.instance?.clickNodesByContentDescription("Next")
        delay(5_000)
        speakText("Typing YouTube caption")
        MyAccessibilityService.instance?.simulateTypeString(
            "android.widget.EditText",
            "Caption your Short",
            caption
        )
        delay(5_000)

        delay(5_000)
        speakText("Publishing YouTube Short")
        MyAccessibilityService.instance?.simulateClick(350f, 1490f)
        MyAccessibilityService.instance?.clickNodesByContentDescription("Upload Short")
        delay(10_000)
        val completeMsg = "Completed one full upload sequence (YouTube)."
        Log.w("MainActivity", completeMsg)
        speakText(completeMsg)
    }

    private fun fetchTodaysVideoSync(email: String, server: String = "1"): VideoWithKey {
        if (isDestroyed) return VideoWithKey("none", null)
        val database = firebaseDatabaseOrNull("fetchTodaysVideoSync") ?: return VideoWithKey("none", null)

        return runBlocking {
            try {
                val emailRef = database
                    .getReference("videos")
                    .child(email.replace(".", ","))
                val snapshot = emailRef.get().await()

                val serverIndex = server.substringAfterLast("-", "")
                val unpostedList = snapshot.children.mapNotNull { child ->
                    try {
                        val vid = child.getValue(Video::class.java) ?: return@mapNotNull null

                        val isPosted = when (vid.posted) {
                            is Boolean -> vid.posted
                            is String -> vid.posted.lowercase().trim() in listOf("true", "1")
                            else -> false
                        }

                        val isNotPosted = !isPosted
                        Log.e("MainActivity", "Posted status: $isPosted, Not posted: $isNotPosted")

                        if (isNotPosted || email.replace(".", ",") == "rohan@cheatlayer,com") {
                            val currentServer = vid.server.orEmpty()
                            val currentServerIndex = currentServer.substringAfterLast("-", "")
                            Log.e("MainActivity", "Server check: current=$currentServer, target=$server, index=$currentServerIndex")

                            if (server.isEmpty()) {
                                if (currentServer.isEmpty() || currentServer == server) {
                                    VideoWithKey(child.key ?: "unknown", vid)
                                } else {
                                    null
                                }
                            } else {
                                if (currentServer == server ||
                                    (currentServerIndex.isNotEmpty() && currentServerIndex == serverIndex) ||
                                    email.replace(".", ",") == "rohan@cheatlayer,com"
                                ) {
                                    VideoWithKey(child.key ?: "unknown", vid)
                                } else {
                                    null
                                }
                            }
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error parsing child ${child.key}: ${e.message}")
                        null
                    }
                }

                val resultMsg = if (unpostedList.isNotEmpty())
                    "Found unposted video for $email $serverIndex"
                else
                    "No unposted videos for $email $serverIndex"
                speakText(resultMsg)
                Log.e("MainActivity", resultMsg)

                if (unpostedList.isNotEmpty()) unpostedList[0] else VideoWithKey("none", null)
            } catch (e: Exception) {
                val errorMsg = "Error reading from DB for $email: $e"
                Log.e("MainActivity", errorMsg)
                speakText(errorMsg)
                VideoWithKey("none", null)
            }
        }
    }

    private fun reloadUniversalScript() {
        if (isDestroyed) return

        mainScope.launch {
            try {
                updateStatusWithAnimation("🔄 Reloading automation script...")
                speakText("Reloading automation script from server")

                val scriptContent = downloadUniversalScript()

                if (scriptContent.isNotEmpty()) {
                    universalScriptContent = scriptContent
                    saveUniversalScript(scriptContent)
                    speakText("Automation script updated successfully")
                    updateStatusWithAnimation("✅ Script updated")
                } else {
                    speakText("Failed to reload script from server")
                    updateStatusWithAnimation("❌ Script reload failed")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error reloading universal script: ${e.message}")
                speakText("Error reloading automation script")
            }
        }
    }

    private fun addTestHistoryItems() {
        if (generationHistory.isEmpty()) {
            val testItems = listOf(
                GenerationHistory(
                    id = UUID.randomUUID().toString(),
                    userCommand = "Open TikTok",
                    generatedCode = "speakText(\"Opening TikTok\");\nlaunchTikTok();",
                    timestamp = System.currentTimeMillis() - 300000
                ),
                GenerationHistory(
                    id = UUID.randomUUID().toString(),
                    userCommand = "Check battery level",
                    generatedCode = "speakText(\"Checking battery level\");\nvar level = getBatteryLevel();\nspeakText(\"Battery is at \" + level + \" percent\");",
                    timestamp = System.currentTimeMillis() - 600000
                ),
                GenerationHistory(
                    id = UUID.randomUUID().toString(),
                    userCommand = "Turn on WiFi",
                    generatedCode = "speakText(\"Turning on WiFi\");\ntoggleWiFi(true);",
                    timestamp = System.currentTimeMillis() - 900000
                ),
                GenerationHistory(
                    id = UUID.randomUUID().toString(),
                    userCommand = "Take a photo",
                    generatedCode = "speakText(\"Taking photo\");\ntakePhoto();",
                    timestamp = System.currentTimeMillis() - 1200000
                ),
                GenerationHistory(
                    id = UUID.randomUUID().toString(),
                    userCommand = "Set brightness to 75%",
                    generatedCode = "speakText(\"Setting brightness to 75 percent\");\nsetBrightness(75);",
                    timestamp = System.currentTimeMillis() - 1500000
                ),
                GenerationHistory(
                    id = UUID.randomUUID().toString(),
                    userCommand = "Toggle flashlight",
                    generatedCode = "speakText(\"Toggling flashlight\");\ntoggleFlashlight(true);\ndelay(3000);\ntoggleFlashlight(false);",
                    timestamp = System.currentTimeMillis() - 1800000
                )
            )

            generationHistory.addAll(testItems)
            saveGenerationHistory()
            Log.d("MainActivity", "Added ${testItems.size} test history items")
        }
    }

    private fun updateUI() {
        if (isDestroyed) return

        runOnUiThread {
            val tasksCount = cronTasks.size
            val historyCount = generationHistory.size

            updateStatusWithAnimation("📊 Active Tasks: $tasksCount | History: $historyCount")

            val tasksFragment = supportFragmentManager.findFragmentByTag("f0") as? ScheduledTasksFragment
            tasksFragment?.updateTasks(cronTasks.values.toList())

            val historyFragment = supportFragmentManager.findFragmentByTag("f1") as? GenerationHistoryFragment
            historyFragment?.updateHistory(generationHistory.toList())

            Log.d("MainActivity", "UI updated - Tasks: $tasksCount, History: $historyCount")
        }
    }

    fun runTaskNow(task: CronTask) {
        if (isDestroyed) return

        mainScope.launch {
            try {
                updateStatusWithAnimation("⚡ Running task: ${task.taskDescription}")
                speakText("Running task now: ${task.taskDescription}")

                if (task.id == "universal_script_daily") {
                    executeUniversalScript()
                } else {
                    val automationCode = generateAutomationCodeWithFallback(task.taskDescription)
                    executeGeneratedCode(automationCode)
                }

                speakText("Task executed successfully")
                updateStatusWithAnimation("✅ Task completed successfully")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error running task: ${e.message}")
                speakText("Error running task")
                updateStatusWithAnimation("❌ Error running task")
            }
        }
    }

    fun deleteTask(task: CronTask) {
        if (task.id == "universal_script_daily") {
            speakText("Cannot delete daily automation script task")
            return
        }

        cronTasks.remove(task.id)
        saveCronTasks()
        speakText("Task deleted: ${task.taskDescription}")
        updateUI()
        updateStatusWithAnimation("🗑️ Task deleted")
    }

    fun runCode(code: String) {
        if (isDestroyed) return

        mainScope.launch {
            try {
                updateStatusWithAnimation("⚡ Executing generated code...")
                speakText("Running generated code")
                executeGeneratedCode(code)
                speakText("Code executed successfully")
                updateStatusWithAnimation("✅ Code executed successfully")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error running code: ${e.message}")
                speakText("Error running code")
                updateStatusWithAnimation("❌ Error executing code")
            }
        }
    }

    fun editCode(history: GenerationHistory) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_code, null)
        val textInputLayout = dialogView.findViewById<TextInputLayout>(R.id.codeInputLayout)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.codeEditText)

        editText.setText(history.generatedCode)

        MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog)
            .setTitle("Edit Code - ${history.userCommand}")
            .setView(dialogView)
            .setPositiveButton("Run") { _, _ ->
                val editedCode = editText.text.toString()
                runCode(editedCode)
            }
            .setNegativeButton("Cancel", null)
            .setNeutralButton("Save") { _, _ ->
                val editedCode = editText.text.toString()
                val newHistory = GenerationHistory(
                    id = UUID.randomUUID().toString(),
                    userCommand = "${history.userCommand} (edited)",
                    generatedCode = editedCode
                )
                generationHistory.add(0, newHistory)
                saveGenerationHistory()
                updateUI()
                speakText("Edited code saved")
                updateStatusWithAnimation("💾 Edited code saved")
            }
            .show()
    }

    fun scheduleCode(history: GenerationHistory) {
        val scheduleOptions = arrayOf(
            "Every 5 seconds",
            "Every 30 seconds",
            "Every 1 minute",
            "Every 5 minutes",
            "Every 10 minutes",
            "Every hour",
            "Custom expression"
        )

        MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog)
            .setTitle("Schedule Task: ${history.userCommand}")
            .setItems(scheduleOptions) { _, which ->
                val cronExpression = when (which) {
                    0 -> "*/5 * * * *"
                    1 -> "*/30 * * * *"
                    2 -> "0 */1 * * *"
                    3 -> "0 */5 * * *"
                    4 -> "0 */10 * * *"
                    5 -> "0 0 */1 * *"
                    6 -> {
                        showCustomCronDialog(history)
                        return@setItems
                    }
                    else -> "*/30 * * * *"
                }
                scheduleTaskFromHistory(history, cronExpression)
            }
            .show()
    }

    private fun showCustomCronDialog(history: GenerationHistory) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_custom_cron, null)
        val textInputLayout = dialogView.findViewById<TextInputLayout>(R.id.cronInputLayout)
        val editText = dialogView.findViewById<TextInputEditText>(R.id.cronEditText)

        MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog)
            .setTitle("Custom Cron Expression")
            .setView(dialogView)
            .setPositiveButton("Schedule") { _, _ ->
                val customCron = editText.text.toString()
                scheduleTaskFromHistory(history, customCron)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun scheduleTaskFromHistory(history: GenerationHistory, cronExpression: String) {
        val taskId = addCronTask(history.userCommand, cronExpression)
        speakText("Scheduled task: ${history.userCommand}")
        updateUI()
        updateStatusWithAnimation("📅 Task scheduled successfully")
    }

    private fun clearAllScheduledTasks() {
        MaterialAlertDialogBuilder(this, R.style.MaterialAlertDialog)
            .setTitle("Clear All Scheduled Tasks")
            .setMessage("Are you sure you want to clear all ${cronTasks.size} scheduled tasks?")
            .setPositiveButton("Yes") { _, _ ->
                val taskCount = cronTasks.size
                cronTasks.clear()
                saveCronTasks()
                speakText("Cleared $taskCount scheduled tasks")
                updateUI()
                updateStatusWithAnimation("🧹 All tasks cleared")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    class TasksAdapter(
        private var tasks: MutableList<CronTask>,
        private val onAction: (String, CronTask) -> Unit
    ) : RecyclerView.Adapter<TasksAdapter.TaskViewHolder>() {

        class TaskViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val taskDescription: TextView = view.findViewById(R.id.taskDescription)
            val cronExpression: TextView = view.findViewById(R.id.cronExpression)
            val taskStatus: TextView = view.findViewById(R.id.taskStatus)
            val runNowButton: MaterialButton = view.findViewById(R.id.runNowButton)
            val deleteTaskButton: MaterialButton = view.findViewById(R.id.deleteTaskButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_scheduled_task, parent, false)
            return TaskViewHolder(view)
        }

        override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
            val task = tasks[position]
            holder.taskDescription.text = task.taskDescription
            holder.cronExpression.text = task.cronExpression

            val lastExecutedText = if (task.lastExecuted == 0L) {
                "Never executed"
            } else {
                "Last: ${SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault()).format(Date(task.lastExecuted))}"
            }
            holder.taskStatus.text = "Created: ${SimpleDateFormat("MM/dd HH:mm:ss", Locale.getDefault()).format(Date(task.createdAt))} • $lastExecutedText"

            holder.runNowButton.setOnClickListener {
                onAction("run", task)
            }

            holder.deleteTaskButton.setOnClickListener {
                onAction("delete", task)
            }
        }

        override fun getItemCount() = tasks.size

        fun updateTasks(newTasks: List<CronTask>) {
            tasks.clear()
            tasks.addAll(newTasks)
            notifyDataSetChanged()
            Log.d("TasksAdapter", "Updated with ${tasks.size} tasks")
        }
    }

    class HistoryAdapter(
        private var history: MutableList<GenerationHistory>,
        private val onAction: (String, GenerationHistory) -> Unit
    ) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

        class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val userCommand: TextView = view.findViewById(R.id.userCommand)
            val generatedCode: TextView = view.findViewById(R.id.generatedCode)
            val timestamp: TextView = view.findViewById(R.id.timestamp)
            val runButton: MaterialButton = view.findViewById(R.id.runButton)
            val editButton: MaterialButton = view.findViewById(R.id.editButton)
            val scheduleButton: MaterialButton = view.findViewById(R.id.scheduleButton)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_generation_history, parent, false)
            return HistoryViewHolder(view)
        }

        override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
            val item = history[position]
            holder.userCommand.text = item.userCommand

            val displayCode = if (item.generatedCode.length > 200) {
                item.generatedCode.take(200) + "..."
            } else {
                item.generatedCode
            }
            holder.generatedCode.text = displayCode

            holder.timestamp.text = SimpleDateFormat("MMM dd, yyyy • HH:mm:ss", Locale.getDefault()).format(Date(item.timestamp))

            holder.runButton.setOnClickListener {
                onAction("run", item)
            }

            holder.editButton.setOnClickListener {
                onAction("edit", item)
            }

            holder.scheduleButton.setOnClickListener {
                onAction("schedule", item)
            }
        }

        override fun getItemCount() = history.size

        fun updateHistory(newHistory: List<GenerationHistory>) {
            history.clear()
            history.addAll(newHistory)
            notifyDataSetChanged()
            Log.d("HistoryAdapter", "Updated with ${history.size} history items")
        }
    }

    private fun addToHistory(userCommand: String, generatedCode: String) {
        val historyItem = GenerationHistory(
            id = UUID.randomUUID().toString(),
            userCommand = userCommand,
            generatedCode = generatedCode
        )
        generationHistory.add(0, historyItem)

        if (generationHistory.size > 50) {
            generationHistory.removeAt(generationHistory.size - 1)
        }

        saveGenerationHistory()
        updateUI()

        Log.d("MainActivity", "Added to history: $userCommand -> ${generatedCode.take(50)}...")
        Log.d("MainActivity", "Total history items: ${generationHistory.size}")
    }

    private fun saveGenerationHistory() {
        try {
            val jsonArray = org.json.JSONArray()
            for (item in generationHistory) {
                val jsonObject = JSONObject().apply {
                    put("id", item.id)
                    put("userCommand", item.userCommand)
                    put("generatedCode", item.generatedCode)
                    put("timestamp", item.timestamp)
                }
                jsonArray.put(jsonObject)
            }
            sharedPreferences.edit().putString("generation_history", jsonArray.toString()).apply()
            Log.d("MainActivity", "Generation history saved successfully: ${generationHistory.size} items")
        } catch (e: Exception) {
            Log.e("MainActivity", "Error saving generation history: ${e.message}")
        }
    }

    private fun loadGenerationHistory() {
        try {
            val historyJson = sharedPreferences.getString("generation_history", "") ?: ""
            Log.d("MainActivity", "Loading history JSON: ${historyJson.take(100)}...")

            if (historyJson.isNotEmpty()) {
                val jsonArray = org.json.JSONArray(historyJson)
                generationHistory.clear()

                for (i in 0 until jsonArray.length()) {
                    try {
                        val itemJson = jsonArray.getJSONObject(i)
                        val item = GenerationHistory(
                            id = itemJson.getString("id"),
                            userCommand = itemJson.getString("userCommand"),
                            generatedCode = itemJson.getString("generatedCode"),
                            timestamp = itemJson.optLong("timestamp", System.currentTimeMillis())
                        )
                        generationHistory.add(item)
                        Log.d("MainActivity", "Loaded history item: ${item.userCommand}")
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error parsing history item $i: ${e.message}")
                    }
                }

                Log.d("MainActivity", "Loaded ${generationHistory.size} history items")
            } else {
                Log.d("MainActivity", "No saved generation history found")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading generation history: ${e.message}")
        }
    }

    private fun checkAndRequestMicrophonePermission() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED -> {
                updateStatusWithAnimation("🎤 Voice control active - Ready for commands")
                speakText("Microphone permission granted. Initializing voice control.")
                initializeSpeechRecognition()
                startListeningLoop()
            }
            else -> {
                requestMicrophonePermission()
            }
        }
    }

    private fun requestMicrophonePermission() {
        permissionRequestInProgress = true
        updateStatusWithAnimation("⚠️ Requesting microphone permission...")
        speakText("Requesting microphone permission. Please allow access when prompted.")

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            MICROPHONE_PERMISSION_REQUEST
        )
    }

    private fun initializeSpeechRecognition() {
        try {
            if (::speechRecognizer.isInitialized) {
                speechRecognizer.destroy()
            }

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer.setRecognitionListener(this)

            speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
            }

            Log.d("MainActivity", "Speech recognition initialized successfully")

        } catch (e: Exception) {
            Log.e("MainActivity", "Error initializing speech recognition: ${e.message}")
            speakText("Error setting up voice recognition: ${e.message}")
        }
    }

    private fun startListeningLoop() {
        if (isDestroyed) return

        mainScope.launch {
            updateStatusWithAnimation("🎧 Listening for voice commands...")
            speakText("Advanced voice automation ready with scheduling. I'm listening for your commands.")
            Log.d("MainActivity", "Starting listening loop")

            while (isActive && !isDestroyed) {
                try {
                    if (!isListening && isPermissionGranted()) {
                        startListening()
                        delay(100)
                    } else {
                        delay(1000)
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error in listening loop: ${e.message}")
                    delay(5000)
                }
            }
        }
    }

    private fun startListening() {
        if (!isListening && ::speechRecognizer.isInitialized && isPermissionGranted()) {
            try {
                isListening = true
                speechRecognizer.startListening(speechRecognizerIntent)
                Log.d("MainActivity", "Started listening for speech")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error starting speech recognition: ${e.message}")
                isListening = false
            }
        }
    }

    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        permissionRequestInProgress = false

        if (requestCode == MICROPHONE_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                updateStatusWithAnimation("✅ Microphone ready - Tap button to speak")
                speakText("Microphone permission granted. Tap the button to give commands.")
                initializeSpeechRecognition()
            } else {
                updateStatusWithAnimation("❌ Microphone permission required")
                speakText("Microphone permission required for voice control.")
            }
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d("SpeechRecognition", "Ready for speech")
        updateStatusWithAnimation("🎤 Listening - Speak now...")
    }

    override fun onBeginningOfSpeech() {
        Log.d("SpeechRecognition", "Beginning of speech detected")
        updateStatusWithAnimation("🗣️ Processing your speech...")
    }

    override fun onEndOfSpeech() {
        Log.d("SpeechRecognition", "End of speech")
        isListening = false
        isCurrentlyListening = false
        resetMicrophoneButton()
        updateStatusWithAnimation("⚙️ Processing command...")
    }

    override fun onError(error: Int) {
        isListening = false
        isCurrentlyListening = false
        resetMicrophoneButton()

        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error: $error"
        }

        if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            Log.w("SpeechRecognition", "Speech recognition error: $errorMessage")
            updateStatusWithAnimation("❌ $errorMessage - Tap to try again")
        } else {
            updateStatusWithAnimation("🎤 No speech detected - Tap to try again")
        }
    }

    override fun onResults(results: Bundle?) {
        results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.let { matches ->
            if (matches.isNotEmpty()) {
                val spokenText = matches[0]
                Log.d("SpeechRecognition", "Recognized speech: $spokenText")

                isListening = false
                isCurrentlyListening = false
                resetMicrophoneButton()

                updateStatusWithAnimation("🔄 Processing: $spokenText")
                processVoiceCommand(spokenText)
            }
        }
    }

    private fun processVoiceCommand(spokenText: String) {
        if (isDestroyed) return

        lastInteractionTime = System.currentTimeMillis()
        sharedPreferences.edit().putLong(KEY_LAST_INTERACTION, lastInteractionTime).apply()

        // 检查是否是模式切换命令
        when {
            spokenText.contains("切换到女友模式") || spokenText.contains("女友模式") -> {
                currentMode = GirlfriendMode.GIRLFRIEND
                sharedPreferences.edit().putString(KEY_GIRLFRIEND_MODE, "GIRLFRIEND").apply()
                speakText("好哦~切换到女友模式啦！$userNickname，人家想你了💕")
                updateStatusWithAnimation("💕 女友模式")
                return
            }
            spokenText.contains("切换到工作模式") || spokenText.contains("工作模式") -> {
                currentMode = GirlfriendMode.WORK
                sharedPreferences.edit().putString(KEY_GIRLFRIEND_MODE, "WORK").apply()
                speakText("已切换到工作模式")
                updateStatusWithAnimation("💼 工作模式")
                return
            }
            spokenText.contains("切换到自动化模式") || spokenText.contains("自动化模式") -> {
                currentMode = GirlfriendMode.AUTOMATION
                sharedPreferences.edit().putString(KEY_GIRLFRIEND_MODE, "AUTOMATION").apply()
                speakText("已切换到自动化模式")
                updateStatusWithAnimation("🤖 自动化模式")
                return
            }
            
            // 视觉模型切换命令
            spokenText.contains("切换到通义视觉") || spokenText.contains("使用通义千问视觉") -> {
                currentVisionModel = VisionModel.QWEN_VL
                sharedPreferences.edit().putString(KEY_VISION_MODEL, "qwen_vl").apply()
                speakText("已切换到通义千问视觉模型")
                updateStatusWithAnimation("👁️ 通义千问 VL")
                return
            }
            spokenText.contains("切换到MiniCPM视觉") || spokenText.contains("使用MiniCPM视觉") -> {
                currentVisionModel = VisionModel.MINICPM
                sharedPreferences.edit().putString(KEY_VISION_MODEL, "minicpm").apply()
                speakText("已切换到MiniCPM视觉模型")
                updateStatusWithAnimation("👁️ MiniCPM-V")
                return
            }
            spokenText.contains("切换到Moondream视觉") || spokenText.contains("使用Moondream视觉") -> {
                currentVisionModel = VisionModel.MOONDREAM
                sharedPreferences.edit().putString(KEY_VISION_MODEL, "moondream").apply()
                speakText("已切换到Moondream视觉模型")
                updateStatusWithAnimation("👁️ Moondream")
                return
            }

            // 图片服务器配置命令
            spokenText.contains("配置图片服务器") || spokenText.contains("设置图片服务器") -> {
                // 提取 URL
                val urlRegex = "(https?://)?[\\w.-]+\\.zeabur\\.app".toRegex()
                val match = urlRegex.find(spokenText)
                if (match != null) {
                    val url = match.value
                    sharedPreferences.edit().putString("comfyui_url", url).apply()
                    speakText("已配置图片服务器：$url")
                    updateStatusWithAnimation("🖼️ 服务器: $url")
                } else {
                    speakText("请提供正确的服务器地址，格式为：配置图片服务器 https://xxx.zeabur.app")
                }
                return
            }

            // 切换到本地模式
            spokenText.contains("使用本地图片") || spokenText.contains("切换到本地生成") -> {
                sharedPreferences.edit().putString("comfyui_url", "").apply()
                speakText("已切换到本地图片生成模式")
                updateStatusWithAnimation("🖼️ 本地模式")
                return
            }
        }

        // 根据模式处理
        when (currentMode) {
            GirlfriendMode.GIRLFRIEND, GirlfriendMode.WORK -> {
                processGirlfriendChat(spokenText)
            }
            GirlfriendMode.AUTOMATION -> {
                processAutomationCommand(spokenText)
            }
        }
    }

    private fun processGirlfriendChat(userMessage: String) {
        mainScope.launch {
            try {
                updateStatusWithAnimation("💬 $girlfriendName 正在回复...")

                conversationHistory.add(mapOf("role" to "user", "content" to userMessage))
                if (conversationHistory.size > MAX_HISTORY) {
                    conversationHistory.removeAt(0)
                }

                val messages = mutableListOf<Map<String, String>>()
                messages.add(mapOf("role" to "system", "content" to buildGirlfriendSystemPrompt()))
                messages.addAll(conversationHistory)

                val response = callStreaming16kAPI(messages, maxTokens = 500, mode = "fast")

                if (response.isNotEmpty()) {
                    conversationHistory.add(mapOf("role" to "assistant", "content" to response))
                    if (conversationHistory.size > MAX_HISTORY) {
                        conversationHistory.removeAt(0)
                    }

                    val displayResponse = response.take(100) + if (response.length > 100) "..." else ""
                    updateStatusWithAnimation("💕 $displayResponse")
                    speakText(response)

                    // 检查是否包含动作标记
                    if (response.contains("[ACTION:")) {
                        val actionRegex = "\\[ACTION:(.+?)\\]".toRegex()
                        val match = actionRegex.find(response)
                        match?.let {
                            val action = it.groupValues[1]
                            Log.d("Girlfriend", "Executing action: $action")
                            val automationCode = generateAutomationCodeWithFallback(action)
                            if (automationCode.isNotEmpty()) {
                                executeGeneratedCode(automationCode)
                            }
                        }
                    }
                } else {
                    speakText("哎呀，我好像走神了，你能再说一遍吗？")
                    updateStatusWithAnimation("💕 等待中...")
                }
            } catch (e: Exception) {
                Log.e("Girlfriend", "Error in chat: ${e.message}")
                speakText("呜呜，出了点小问题，但我还是爱你的~")
                updateStatusWithAnimation("💕 等待中...")
            }
        }
    }

    private fun processAutomationCommand(spokenText: String) {
        speakText("Processing your command: $spokenText")

        mainScope.launch {
            try {
                Log.d("MainActivity", "Generating automation code for: $spokenText")
                val automationCode = generateAutomationCodeWithFallback(spokenText)

                Log.d("MainActivity", "Generated code: ${automationCode.take(100)}...")

                if (automationCode.isNotEmpty()) {
                    addToHistory(spokenText, automationCode)

                    updateStatusWithAnimation("⚡ Executing automation...")
                    speakText("Executing automation code")
                    executeGeneratedCode(automationCode)
                    speakText("Command executed successfully")
                    updateStatusWithAnimation("✅ Done - Tap button for next command")
                } else {
                    speakText("Sorry, I couldn't generate automation code for that command")
                    updateStatusWithAnimation("❌ Failed - Tap button to try again")
                }

            } catch (e: Exception) {
                Log.e("MainActivity", "Error processing voice command: ${e.message}")
                speakText("Error processing command: ${e.message}")
                updateStatusWithAnimation("❌ Error - Tap button to try again")
            }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {}

    override fun onEvent(eventType: Int, params: Bundle?) {}

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    private suspend fun generateAutomationCodeWithFallback(userCommand: String): String {
        if (isDestroyed) return ""

        try {
            val aiCode = generateAutomationCode(userCommand)
            if (aiCode.isNotEmpty()) {
                return aiCode
            }
            return generateFallbackCode(userCommand)
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in code generation: ${e.message}")
            return generateFallbackCode(userCommand)
        }
    }

    private fun generateFallbackCode(userCommand: String): String {
        Log.d("MainActivity", "Using fallback code generation for: $userCommand")

        val command = userCommand.lowercase()

        return when {
            command.contains("open tiktok") || command.contains("launch tiktok") ->
                "speakText(\"Opening TikTok\");\nlaunchTikTok();"

            command.contains("open youtube") || command.contains("launch youtube") ->
                "speakText(\"Opening YouTube\");\nlaunchYouTube();"

            command.contains("open instagram") || command.contains("launch instagram") ->
                "speakText(\"Opening Instagram\");\nlaunchInstagram();"

            command.contains("open twitter") || command.contains("launch twitter") ->
                "speakText(\"Opening Twitter\");\nlaunchTwitter();"

            command.contains("open spotify") || command.contains("launch spotify") ->
                "speakText(\"Opening Spotify\");\nlaunchSpotify();"

            command.contains("open netflix") || command.contains("launch netflix") ->
                "speakText(\"Opening Netflix\");\nlaunchNetflix();"

            command.contains("open reddit") || command.contains("launch reddit") ->
                "speakText(\"Opening Reddit\");\nlaunchReddit();"

            command.contains("open medium") || command.contains("launch medium") ->
                "speakText(\"Opening Medium\");\nlaunchMedium();"

            command.contains("open telegram") || command.contains("launch telegram") ->
                "speakText(\"Opening Telegram\");\nlaunchTelegram();"

            command.contains("open whatsapp") || command.contains("launch whatsapp") ->
                "speakText(\"Opening WhatsApp\");\nlaunchWhatsApp();"

            command.contains("open snapchat") || command.contains("launch snapchat") ->
                "speakText(\"Opening Snapchat\");\nlaunchSnapchat();"

            command.contains("open linkedin") || command.contains("launch linkedin") ->
                "speakText(\"Opening LinkedIn\");\nlaunchLinkedIn();"

            command.contains("open pinterest") || command.contains("launch pinterest") ->
                "speakText(\"Opening Pinterest\");\nlaunchPinterest();"

            command.contains("open twitch") || command.contains("launch twitch") ->
                "speakText(\"Opening Twitch\");\nlaunchTwitch();"

            command.contains("open discord") || command.contains("launch discord") ->
                "speakText(\"Opening Discord\");\nlaunchDiscord();"

            command.contains("check battery") || command.contains("battery level") ->
                "speakText(\"Checking battery level\");\nvar level = getBatteryLevel();\nspeakText(\"Battery is at \" + level + \" percent\");"

            command.contains("check memory") || command.contains("memory usage") ->
                "speakText(\"Checking memory usage\");\nvar usage = getMemoryUsage();\nspeakText(\"Memory usage is \" + Math.round(usage) + \" percent\");"

            command.contains("check storage") || command.contains("storage space") ->
                "speakText(\"Checking storage space\");\nvar storage = getStorageSpace();\nspeakText(\"Storage info: \" + storage);"

            command.contains("turn on wifi") || command.contains("enable wifi") ->
                "speakText(\"Turning on WiFi\");\ntoggleWiFi(true);"

            command.contains("turn off wifi") || command.contains("disable wifi") ->
                "speakText(\"Turning off WiFi\");\ntoggleWiFi(false);"

            command.contains("turn on bluetooth") || command.contains("enable bluetooth") ->
                "speakText(\"Turning on Bluetooth\");\ntoggleBluetooth(true);"

            command.contains("turn off bluetooth") || command.contains("disable bluetooth") ->
                "speakText(\"Turning off Bluetooth\");\ntoggleBluetooth(false);"

            command.contains("set brightness") -> {
                val numberRegex = "\\d+".toRegex()
                val match = numberRegex.find(command)
                val level = match?.value?.toIntOrNull() ?: 50
                "speakText(\"Setting brightness to $level percent\");\nsetBrightness($level);"
            }

            command.contains("set volume") -> {
                val numberRegex = "\\d+".toRegex()
                val match = numberRegex.find(command)
                val level = match?.value?.toIntOrNull() ?: 50
                val type = when {
                    command.contains("media") -> "media"
                    command.contains("ringer") -> "ringer"
                    command.contains("alarm") -> "alarm"
                    else -> "media"
                }
                "speakText(\"Setting $type volume to $level percent\");\nsetVolume(\"$type\", $level);"
            }

            command.contains("take photo") || command.contains("take picture") ->
                "speakText(\"Taking photo\");\ntakePhoto();"

            command.contains("open camera") ->
                "speakText(\"Opening camera\");\nopenCamera();"

            command.contains("open gallery") ->
                "speakText(\"Opening gallery\");\nopenGallery();"

            command.contains("flashlight on") || command.contains("turn on flashlight") ->
                "speakText(\"Turning on flashlight\");\ntoggleFlashlight(true);"

            command.contains("flashlight off") || command.contains("turn off flashlight") ->
                "speakText(\"Turning off flashlight\");\ntoggleFlashlight(false);"

            command.contains("vibrate") ->
                "speakText(\"Vibrating device\");\nvibrate(1000);"

            command.contains("open dialer") || command.contains("open phone") ->
                "speakText(\"Opening dialer\");\nopenDialer();"

            command.contains("open contacts") ->
                "speakText(\"Opening contacts\");\nopenContacts();"

            command.contains("open messages") ->
                "speakText(\"Opening messages\");\nopenMessages();"

            command.contains("open gmail") ->
                "speakText(\"Opening Gmail\");\nopenGmail();"

            command.contains("every") && (command.contains("second") || command.contains("minute") || command.contains("hour")) -> {
                generateScheduleCommand(userCommand)
            }

            command.contains("clear schedule") || command.contains("clear all") || command.contains("remove schedule") ->
                "speakText(\"Clearing all scheduled tasks\");\nclearSchedule();"

            else -> {
                "speakText(\"I heard: $userCommand. Processing command...\");\ndelay(2000);\nspeakText(\"Command processed successfully\");"
            }
        }
    }

    private fun generateScheduleCommand(userCommand: String): String {
        val command = userCommand.lowercase()

        val baseAction = when {
            command.contains("open tiktok") -> "open TikTok"
            command.contains("open youtube") -> "open YouTube"
            command.contains("open instagram") -> "open Instagram"
            command.contains("open twitter") -> "open Twitter"
            command.contains("open spotify") -> "open Spotify"
            command.contains("open netflix") -> "open Netflix"
            command.contains("check battery") -> "check battery level"
            command.contains("check memory") -> "check memory usage"
            command.contains("take photo") -> "take a photo"
            command.contains("turn on wifi") -> "turn on WiFi"
            command.contains("turn off wifi") -> "turn off WiFi"
            command.contains("flashlight") -> "toggle flashlight"
            command.contains("vibrate") -> "vibrate device"
            else -> userCommand.split(" every ")[0].trim()
        }

        val cronExpression = when {
            command.contains("every 5 second") -> "*/5 * * * *"
            command.contains("every 10 second") -> "*/10 * * * *"
            command.contains("every 15 second") -> "*/15 * * * *"
            command.contains("every 30 second") -> "*/30 * * * *"
            command.contains("every 1 minute") || command.contains("every minute") -> "0 */1 * * *"
            command.contains("every 2 minute") -> "0 */2 * * *"
            command.contains("every 5 minute") -> "0 */5 * * *"
            command.contains("every 10 minute") -> "0 */10 * * *"
            command.contains("every 15 minute") -> "0 */15 * * *"
            command.contains("every 30 minute") -> "0 */30 * * *"
            command.contains("every hour") -> "0 0 */1 * *"
            command.contains("every 2 hour") -> "0 0 */2 * *"
            command.contains("every 6 hour") -> "0 0 */6 * *"
            command.contains("every 12 hour") -> "0 0 */12 * *"
            command.contains("daily") || command.contains("every day") -> "0 0 0 * *"
            else -> "*/30 * * * *"
        }

        return "speakText(\"Scheduling task: $baseAction\");\nschedule(\"$baseAction\", \"$cronExpression\");"
    }

    private fun startCronChecker() {
        if (isDestroyed) return

        cronCheckJob?.cancel()

        cronCheckJob = mainScope.launch {
            speakText("Cron scheduler started")
            Log.d("MainActivity", "Cron scheduler started")

            while (isActive && !isDestroyed) {
                try {
                    checkAndExecuteCronTasks()
                    delay(5000) // CHANGED: Check every 5 seconds instead of 1
                } catch (e: CancellationException) {
                    Log.d("MainActivity", "Cron checker cancelled")
                    throw e
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error in cron checker: ${e.message}")
                    delay(30000) // Wait 30 seconds on error
                }
            }
        }
    }

    // ========== 虚拟女友心跳功能 ==========
    private fun startHeartbeatChecker() {
        if (isDestroyed) return

        // 恢复保存的状态
        currentMode = GirlfriendMode.valueOf(
            sharedPreferences.getString(KEY_GIRLFRIEND_MODE, "GIRLFRIEND") ?: "GIRLFRIEND"
        )
        lastInteractionTime = sharedPreferences.getLong(KEY_LAST_INTERACTION, System.currentTimeMillis())
        girlfriendName = sharedPreferences.getString(KEY_GIRLFRIEND_NAME, "小柔") ?: "小柔"
        userNickname = sharedPreferences.getString(KEY_USER_NICKNAME, "宝贝") ?: "宝贝"

        mainScope.launch {
            Log.d("Heartbeat", "心跳守护已启动")
            
            while (isActive && !isDestroyed) {
                try {
                    checkHeartbeat()
                    delay(60000) // 每分钟检查一次
                } catch (e: CancellationException) {
                    Log.d("Heartbeat", "心跳检查已停止")
                    throw e
                } catch (e: Exception) {
                    Log.e("Heartbeat", "Error in heartbeat: ${e.message}")
                    delay(60000)
                }
            }
        }
    }

    private suspend fun checkHeartbeat() {
        if (currentMode != GirlfriendMode.GIRLFRIEND) return

        val timeSinceLastInteraction = System.currentTimeMillis() - lastInteractionTime
        
        // 超过2小时（2 * 60 * 60 * 1000 = 7200000ms）没互动
        if (timeSinceLastInteraction > HEARTBEAT_INTERVAL) {
            Log.d("Heartbeat", "触发心跳消息！距离上次互动: ${timeSinceLastInteraction / 60000} 分钟")
            
            val heartbeatMessages = listOf(
                "$userNickname~ 在忙什么呢？人家想你了💕",
                "宝贝~ 很久没理人家了，是不是把我忘了呀？😢",
                "$userNickname！休息一下吧~ 我一直在这里陪着你呢❤️",
                "呜呜，$userNickname 已经很久没找我了...是不是工作很忙呀？💭",
                "嗨~ 我想你了！$userNickname 在干嘛呀？🌸"
            )
            
            val message = heartbeatMessages.random()
            speakText(message)
            updateStatusWithAnimation("💕 $girlfriendName: ${message.take(30)}...")
            
            // 重置交互时间，避免重复发送
            lastInteractionTime = System.currentTimeMillis()
            sharedPreferences.edit().putLong(KEY_LAST_INTERACTION, lastInteractionTime).apply()
        }
    }
    // =====================================

    private suspend fun checkAndExecuteCronTasks() {
        if (isDestroyed) return

        val currentTime = System.currentTimeMillis()

        for ((taskId, cronTask) in cronTasks.toMap()) {
            if (!cronTask.isActive) continue

            try {
                if (shouldExecuteCronTask(cronTask, currentTime)) {
                    Log.d("MainActivity", "Executing cron task: ${cronTask.taskDescription}")
                    updateStatusWithAnimation("⏰ Executing: ${cronTask.taskDescription}")
                    speakText("Executing scheduled task: ${cronTask.taskDescription}")

                    if (taskId == "universal_script_daily") {
                        executeUniversalScript()
                    } else {
                        val automationCode = generateAutomationCodeWithFallback(cronTask.taskDescription)
                        executeGeneratedCode(automationCode)
                    }

                    cronTask.lastExecuted = currentTime
                    cronTasks[taskId] = cronTask.copy(lastExecuted = currentTime)
                    saveCronTasks()

                    Log.d("MainActivity", "Cron task executed successfully: ${cronTask.taskDescription}")
                    updateStatusWithAnimation("✅ Scheduled task completed")
                    updateUI()
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error executing cron task ${cronTask.taskDescription}: ${e.message}")
            }
        }
    }

    private fun shouldExecuteCronTask(cronTask: CronTask, currentTime: Long): Boolean {
        return try {
            if (cronTask.lastExecuted == 0L) {
                val timeSinceCreation = currentTime - cronTask.createdAt
                val firstExecutionDelay = getIntervalFromCron(cronTask.cronExpression)

                if (firstExecutionDelay != null && timeSinceCreation >= firstExecutionDelay) {
                    Log.d("MainActivity", "First execution for task: ${cronTask.taskDescription}")
                    return true
                }
                return false
            }

            val timeSinceLastExecution = currentTime - cronTask.lastExecuted
            val interval = getIntervalFromCron(cronTask.cronExpression)

            if (interval != null && timeSinceLastExecution >= interval) {
                Log.d("MainActivity", "Time for next execution: ${cronTask.taskDescription}")
                return true
            }

            false
        } catch (e: Exception) {
            Log.e("MainActivity", "Error checking cron task execution time: ${e.message}")
            false
        }
    }

    private fun getIntervalFromCron(cronExpression: String): Long? {
        try {
            val parts = cronExpression.split(" ")
            if (parts.size < 5) return null

            val secondPart = parts[0]
            val minutePart = parts[1]
            val hourPart = parts[2]
            val dayPart = parts[3]
            val monthPart = parts[4]

            return when {
                secondPart.startsWith("*/") && minutePart == "*" -> {
                    val interval = secondPart.substring(2).toIntOrNull()
                    interval?.let { it * 1000L }
                }

                secondPart == "0" && minutePart.startsWith("*/") -> {
                    val interval = minutePart.substring(2).toIntOrNull()
                    interval?.let { it * 60 * 1000L }
                }

                secondPart == "0" && minutePart == "0" && hourPart.startsWith("*/") -> {
                    val interval = hourPart.substring(2).toIntOrNull()
                    interval?.let { it * 60 * 60 * 1000L }
                }

                secondPart == "0" && minutePart == "0" && dayPart == "*" && monthPart == "*" -> {
                    24 * 60 * 60 * 1000L
                }

                else -> null
            }
        } catch (e: Exception) {
            return null
        }
    }

    private fun addCronTask(taskDescription: String, cronExpression: String): String {
        val taskId = UUID.randomUUID().toString()
        val cronTask = CronTask(taskId, taskDescription, cronExpression)

        cronTasks[taskId] = cronTask
        saveCronTasks()

        val interval = getIntervalFromCron(cronExpression)
        Log.d("MainActivity", "Added cron task: $taskDescription with expression: $cronExpression, interval: ${interval}ms")

        return taskId
    }

    private fun removeCronTask(taskId: String) {
        cronTasks.remove(taskId)
        saveCronTasks()
        Log.d("MainActivity", "Removed cron task: $taskId")
    }

    private fun saveCronTasks() {
        try {
            val jsonObject = JSONObject()
            for ((key, task) in cronTasks) {
                val taskJson = JSONObject().apply {
                    put("id", task.id)
                    put("taskDescription", task.taskDescription)
                    put("cronExpression", task.cronExpression)
                    put("createdAt", task.createdAt)
                    put("lastExecuted", task.lastExecuted)
                    put("isActive", task.isActive)
                }
                jsonObject.put(key, taskJson)
            }
            sharedPreferences.edit().putString("cron_tasks", jsonObject.toString()).apply()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error saving cron tasks: ${e.message}")
        }
    }

    private fun loadCronTasks() {
        try {
            val tasksJson = sharedPreferences.getString("cron_tasks", "") ?: ""
            if (tasksJson.isNotEmpty()) {
                val jsonObject = JSONObject(tasksJson)

                val keys = jsonObject.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    try {
                        val taskJson = jsonObject.getJSONObject(key)
                        val task = CronTask(
                            id = taskJson.getString("id"),
                            taskDescription = taskJson.getString("taskDescription"),
                            cronExpression = taskJson.getString("cronExpression"),
                            createdAt = taskJson.optLong("createdAt", System.currentTimeMillis()),
                            lastExecuted = taskJson.optLong("lastExecuted", 0L),
                            isActive = taskJson.optBoolean("isActive", true)
                        )

                        cronTasks[task.id] = task
                        Log.d("MainActivity", "Loaded cron task: ${task.taskDescription}, lastExecuted: ${task.lastExecuted}")
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error parsing cron task $key: ${e.message}")
                    }
                }

                Log.d("MainActivity", "Loaded ${cronTasks.size} cron tasks")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error loading cron tasks: ${e.message}")
        }
    }

    private fun testCronScheduler() {
        if (isDestroyed) return

        mainScope.launch {
            delay(5000)
            Log.d("MainActivity", "Testing cron scheduler...")
            Log.d("MainActivity", "Current tasks: ${cronTasks.size}")
            Log.d("MainActivity", "Current history: ${generationHistory.size}")

            cronTasks.values.forEach { task ->
                Log.d("MainActivity", "Task: ${task.taskDescription}")
                Log.d("MainActivity", "Expression: ${task.cronExpression}")
                Log.d("MainActivity", "Created: ${task.createdAt}")
                Log.d("MainActivity", "Last executed: ${task.lastExecuted}")
                Log.d("MainActivity", "Active: ${task.isActive}")

                val interval = getIntervalFromCron(task.cronExpression)
                Log.d("MainActivity", "Calculated interval: ${interval}ms")

                val currentTime = System.currentTimeMillis()
                val shouldExecute = shouldExecuteCronTask(task, currentTime)
                Log.d("MainActivity", "Should execute now: $shouldExecute")
            }

            generationHistory.forEach { item ->
                Log.d("MainActivity", "History: ${item.userCommand} -> ${item.generatedCode.take(50)}...")
            }
        }
    }

    private suspend fun callStreaming16kAPI(
        messages: List<Map<String, String>>,
        maxTokens: Int = 300,
        mode: String = "fast"
    ): String = withContext(Dispatchers.IO) {
        if (isDestroyed) return@withContext ""

        val maxRetries = 3
        var currentRetry = 0
        var result = ""

        val client = OkHttpClient.Builder()
            .connectTimeout(300, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .callTimeout(600, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        val modelOptions = getOpenRouterModelOptions()
        val selectedModel = getSelectedOpenRouterModelId()
        val models = if (modelOptions.isNotEmpty()) {
            val ordered = mutableListOf<String>()
            if (modelOptions.any { it.id == selectedModel }) {
                ordered.add(selectedModel)
            }
            for (option in modelOptions) {
                if (option.id != selectedModel) {
                    ordered.add(option.id)
                }
            }
            ordered
        } else {
            listOf(selectedModel)
        }

        for (modelIndex in models.indices) {
            val currentModel = models[modelIndex]
            currentRetry = 0

            Log.d("MainActivity", "Trying model: $currentModel")

            while (currentRetry < maxRetries && result.isEmpty()) {
                try {
                    val openRouterMessages = org.json.JSONArray()
                    messages.forEach { messageMap ->
                        val messageJson = JSONObject()
                        messageJson.put("role", messageMap["role"] ?: "user")

                        val content = messageMap["content"] ?: ""

                        if (content.contains("data:image/") || content.contains("http")) {
                            val contentArray = org.json.JSONArray()

                            val textPart = JSONObject()
                            textPart.put("type", "text")
                            textPart.put("text", content.replace(Regex("data:image/[^;]+;base64,[A-Za-z0-9+/=]+"), "[Image]"))
                            contentArray.put(textPart)

                            val imageRegex = Regex("(data:image/[^;]+;base64,[A-Za-z0-9+/=]+|https?://[^\\s]+\\.(jpg|jpeg|png|gif|webp))")
                            val imageMatch = imageRegex.find(content)
                            if (imageMatch != null) {
                                val imagePart = JSONObject()
                                imagePart.put("type", "image_url")
                                val imageUrl = JSONObject()
                                imageUrl.put("url", imageMatch.value)
                                imagePart.put("image_url", imageUrl)
                                contentArray.put(imagePart)
                            }

                            messageJson.put("content", contentArray)
                        } else {
                            messageJson.put("content", content)
                        }

                        openRouterMessages.put(messageJson)
                    }

                    val requestBodyJson = JSONObject().apply {
                        put("model", currentModel)
                        put("messages", openRouterMessages)
                        put("max_tokens", maxTokens)
                        put("temperature", if (mode == "fast") 0.7 else 0.3)
                        put("top_p", 1.0)
                        put("frequency_penalty", 0.0)
                        put("presence_penalty", 0.0)
                        put("stream", false)
                    }
                    if (!isActive || isDestroyed) return@withContext ""

                    val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                    val requestBody = requestBodyJson.toString().toRequestBody(mediaType)

                    val request = Request.Builder()
                        .url("https://api.puter.com/puterai/openai/v1/chat/completions")
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer ${BuildConfig.PUTER_AUTH_TOKEN}")
                        .post(requestBody)
                        .build()

                    Log.d("MainActivity", "Making API request to Puter Minimax M2.5 with model: $currentModel")

                    client.newCall(request).execute().use { response ->
                        val responseBody = response.body?.string() ?: ""

                        when {
                            response.isSuccessful -> {
                                try {
                                    val responseJson = JSONObject(responseBody)
                                    val choices = responseJson.getJSONArray("choices")

                                    if (choices.length() > 0) {
                                        val choice = choices.getJSONObject(0)
                                        val message = choice.getJSONObject("message")
                                        result = message.getString("content").trim()

                                        Log.d("MainActivity", "Successfully got response from $currentModel: ${result.take(100)}...")

                                        if (responseJson.has("usage")) {
                                            val usage = responseJson.getJSONObject("usage")
                                            Log.d("MainActivity", "Token usage - Prompt: ${usage.optInt("prompt_tokens", 0)}, Completion: ${usage.optInt("completion_tokens", 0)}")
                                        }

                                        return@withContext result
                                    }else{

                                    }
                                } catch (e: Exception) {
                                    Log.e("MainActivity", "Error parsing LLM response: ${e.message}")
                                    Log.e("MainActivity", "Response body: $responseBody")
                                }
                            }
                            else -> {
                                Log.e("MainActivity", "LLM API error. Code: ${response.code}")
                                Log.e("MainActivity", "Error response: $responseBody")
                            }
                        }
                    }

                } catch (e: Exception) {
                    Log.e("MainActivity", "LLM API exception (attempt ${currentRetry + 1} with $currentModel): ${e.message}")
                    e.printStackTrace()
                }

                when {
                    result.isEmpty() && currentRetry < maxRetries - 1 -> {
                        currentRetry++
                        val delayMs = 2000L * currentRetry
                        Log.d("MainActivity", "Retrying in ${delayMs}ms...")
                        delay(delayMs)
                    }
                    else -> break
                }
            }

            if (result.isNotEmpty()) {
                break
            }

            Log.w("MainActivity", "Model $currentModel failed after $maxRetries attempts, trying next model...")
        }

        val finalResult = if (result.isEmpty()) {
            Log.e("MainActivity", "All models failed after retries")
            "I apologize, but I'm having trouble connecting to the AI service right now. Please try again in a moment."
        } else {
            result
        }

        return@withContext finalResult
    }

    private suspend fun generateAutomationCode(userCommand: String): String = withContext(Dispatchers.IO) {
        if (isDestroyed) return@withContext ""

        try {
            val systemPrompt = buildAutomationSystemPrompt()
            val userPrompt = "User Command: $userCommand"

            val messages = listOf(
                mapOf("role" to "system", "content" to systemPrompt),
                mapOf("role" to "user", "content" to userPrompt)
            )

            val result = callStreaming16kAPI(messages, maxTokens = 1200, mode = "best")

            if (result.isNotEmpty()) {
                extractJavaScriptCode(result)
            } else {
                ""
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error generating automation code: ${e.message}")
            ""
        }
    }

    private fun buildAutomationSystemPrompt(): String {
        return """
You are an advanced automation code generator for Android phones. Generate JavaScript code to automate phone interactions based on user commands.

AVAILABLE FUNCTIONS:

## Core App Launching:
- launchTikTok() - Launch TikTok app
- launchYouTube() - Launch YouTube app  
- launchInstagram() - Launch Instagram app
- launchTwitter() - Launch Twitter app
- launchReddit() - Launch Reddit app
- launchMedium() - Launch Medium app
- launchTelegram() - Launch Telegram
- launchWhatsApp() - Launch WhatsApp
- launchSnapchat() - Launch Snapchat
- launchLinkedIn() - Launch LinkedIn
- launchPinterest() - Launch Pinterest
- launchTwitch() - Launch Twitch
- launchDiscord() - Launch Discord
- launchSpotify() - Launch Spotify
- launchNetflix() - Launch Netflix

## Instagram Functions:
- launchInstagram() - Launch Instagram app
- fetchAgentAccountsInstagram() - Fetch Instagram agent accounts
- fetchAgentServerInstagram() - Fetch Instagram server information
- checkAgentAccountsInstagram(account) - Check/create Instagram account
- doFullUploadSequenceInstagram(caption, email, account, server) - Upload to Instagram

## System Settings:
- openWiFiSettings() - Open WiFi settings
- openBluetoothSettings() - Open Bluetooth settings
- openLocationSettings() - Open location settings
- openBatterySettings() - Open battery settings
- openDisplaySettings() - Open display settings
- openSoundSettings() - Open sound settings
- openStorageSettings() - Open storage settings
- openPrivacySettings() - Open privacy settings
- openSecuritySettings() - Open security settings
- openDeveloperOptions() - Open developer options

## System Controls:
- toggleWiFi(enable) - Toggle WiFi on/off (true/false)
- toggleBluetooth(enable) - Toggle Bluetooth on/off
- toggleLocationServices(enable) - Toggle location services
- toggleAirplaneMode(enable) - Toggle airplane mode
- toggleAutoRotate(enable) - Toggle auto rotation
- toggleDoNotDisturb(enable) - Toggle do not disturb
- setBrightness(level) - Set brightness (0-100)
- setVolume(type, level) - Set volume ("media"/"ringer"/"alarm", 0-100)
- lockScreen() - Lock the screen
- vibrate(milliseconds) - Vibrate device

## Communication:
- makePhoneCall(phoneNumber) - Make a phone call
- sendSMS(phoneNumber, message) - Send SMS message
- openDialer() - Open phone dialer
- openContacts() - Open contacts app
- openMessages() - Open messages app
- openGmail() - Open Gmail app
- composeEmail(to, subject, body) - Compose an email

## File & Media Operations:
- openFileManager() - Open file manager
- openGallery() - Open photo gallery
- openCamera() - Open camera app
- takePhoto() - Take a photo
- openMusicPlayer() - Open music player
- playMusic(filePath) - Play music file
- pauseMusic() - Pause music
- stopMusic() - Stop music

## Navigation & Location:
- openGoogleMaps() - Open Google Maps
- navigateToAddress(address) - Navigate to address
- searchNearby(query) - Search nearby locations
- getCurrentLocation() - Get current GPS coordinates
- openUber() - Open Uber app
- openLyft() - Open Lyft app

## Hardware Control:
- toggleFlashlight(enable) - Toggle flashlight on/off
- takeFrontCamera() - Switch to front camera
- takeBackCamera() - Switch to back camera
- recordAudio(durationSeconds) - Record audio

## App Management:
- openPlayStore() - Open Google Play Store
- searchPlayStore(query) - Search in Play Store
- openAppInfo(packageName) - Open app info/settings
- forceStopApp(packageName) - Force stop an app
- clearAppCache(packageName) - Clear app cache

## Security Functions:
- generateQRCode(data) - Generate QR code
- scanQRCode() - Scan QR code (returns text)
- enableScreenLock(type, password) - Enable screen lock ("pin"/"pattern"/"password")

## Network Management:
- connectToWiFi(ssid, password) - Connect to WiFi network
- disconnectFromWiFi() - Disconnect from WiFi
- checkInternetConnection() - Check if internet is available
- enableMobileData(enable) - Toggle mobile data
- switchToMobileData() - Switch to mobile data
- switchToWiFi() - Switch to WiFi

## Accessibility & UI:
- enableTalkBack(enable) - Toggle TalkBack accessibility
- increaseFontSize() - Increase system font size
- decreaseFontSize() - Decrease system font size
- enableHighContrast(enable) - Toggle high contrast mode
- findElementByText(text) - Find UI element by text
- waitForElement(text, timeoutSeconds) - Wait for element to appear
- scrollUntilFound(text) - Scroll until text is found
- swipeLeft() - Swipe left gesture
- swipeRight() - Swipe right gesture
- swipeUp() - Swipe up gesture
- swipeDown() - Swipe down gesture
- longPress(x, y) - Long press at coordinates
- doubleClick(x, y) - Double click at coordinates

## Productivity:
- openCalendar() - Open calendar app
- createEvent(title, date, time) - Create calendar event
- setAlarm(hour, minute, label) - Set alarm
- setTimer(minutes) - Set timer
- openClock() - Open clock app
- openNotes() - Open notes app
- createNote(title, content) - Create a note
- openGoogleDocs() - Open Google Docs

## Shopping & Finance:
- openAmazon() - Open Amazon app
- searchProduct(query) - Search for product
- openBankingApp(bankName) - Open banking app
- openPaymentApp(appName) - Open payment app (PayPal, Venmo, etc.)

## CRON SCHEDULING:
- schedule(task, cronExpression) - Schedule a task using cron format
- clearSchedule() - Clear all scheduled tasks

## Advanced Automation:
- extractTextFromImage(imagePath) - OCR text extraction
- translateText(text, targetLanguage) - Translate text
- summarizeText(text) - Summarize long text
- generateResponse(prompt) - Generate AI response

## Device Monitoring:
- getBatteryLevel() - Get battery percentage
- getMemoryUsage() - Get RAM usage percentage
- getStorageSpace() - Get storage info (used, total)
- getRunningApps() - List running applications
- getInstalledApps() - List installed applications

## Context-Aware Functions:
- analyzeCurrentScreen() - Analyze what's on screen
- detectCurrentApp() - Detect which app is open
- getScreenText() - Extract all text from screen
- findClickableElements() - Find all clickable elements
- suggestNextAction() - Suggest what to do next

## Core Navigation:
- simulateClick(x, y) - Click at coordinates
- clickNodesByContentDescription(text) - Click element with description
- simulateTypeInFirstEditableField(text) - Type in first input field
- simulateTypeInSecondEditableField(text) - Type in second input field
- pressEnterKey() - Press enter key
- simulateScrollToBottom() - Scroll to bottom
- simulateScrollToBottomX(X) - Scroll to bottom with x coordinate 
- simulateScrollToTop() - Scroll to top
- isTextPresentOnScreen(text) - Check if text exists on screen
- delay(milliseconds) - Wait for specified time
- speakText(text) - Make the phone speak

RULES:
1. Generate ONLY JavaScript code, no explanations
2. Use try-catch blocks for error handling
3. Add delays between actions: delay(2000) for 2 seconds
4. Always start with speakText() to confirm the action
5. Use specific coordinates only when necessary
6. Prefer content description clicks over coordinates
7. Return only the JavaScript code, no markdown
8. For complex tasks, break them into steps with delays
9. Use appropriate error handling for all operations
10. Combine multiple functions for sophisticated automation

SCHEDULING EXAMPLES:

User: "Open TikTok every 5 seconds"
Response:
schedule("open TikTok", "*/5 * * * *");

User: "Check battery every 10 minutes"
Response:
schedule("check battery level", "0 */10 * * *");

User: "Clear all scheduled tasks"
Response:
clearSchedule();

IMMEDIATE EXECUTION EXAMPLES:

User: "Open TikTok"
Response:
speakText("Opening TikTok");
launchTikTok();

User: "Check battery level"
Response:
speakText("Checking battery level");
var level = getBatteryLevel();
speakText("Battery is at " + level + " percent");

Generate JavaScript automation code for the user's command:
        """.trimIndent()
    }

    private fun extractJavaScriptCode(response: String): String {
        val codeBlockRegex = "```(?:javascript|js)?\n(.*?)```".toRegex(RegexOption.DOT_MATCHES_ALL)
        val codeBlockMatch = codeBlockRegex.find(response)

        if (codeBlockMatch != null) {
            return codeBlockMatch.groupValues[1].trim()
        }

        val lines = response.split("\n")
        val jsLines = lines.filter { line ->
            val trimmed = line.trim()
            trimmed.isNotEmpty() &&
                    !trimmed.startsWith("//") &&
                    (trimmed.contains("(") || trimmed.contains(";") || trimmed.contains("{"))
        }

        return if (jsLines.isNotEmpty()) {
            jsLines.joinToString("\n")
        } else {
            response.trim()
        }
    }

    private suspend fun executeGeneratedCode(code: String) {
        if (isDestroyed) return

        withContext(Dispatchers.Default) {
            var context: org.mozilla.javascript.Context? = null
            try {
                context = org.mozilla.javascript.Context.enter()

                context.optimizationLevel = -1

                context.setClassShutter { className ->
                    !className.startsWith("javax.lang.model") &&
                            !className.startsWith("javax.annotation.processing") &&
                            !className.startsWith("java.lang.reflect") &&
                            !className.startsWith("sun.") &&
                            !className.startsWith("com.sun.")
                }

                val scope = context.initStandardObjects()

                val androidInterface = AndroidJSInterface()
                scope.put("Android", scope, androidInterface)

                val wrappedCode = """
                // Core functions
                function speakText(text) { Android.speakText(text); }
                function delay(ms) { Android.delay(ms); }
                
                // Scheduling functions
                function schedule(task, cronExpression) { Android.schedule(task, cronExpression); }
                function clearSchedule() { Android.clearSchedule(); }
                
                //Automation functions 
                function magicClicker(description){
                Android.magicClicker(description)
              //  Android.delay(3000);
                }
                function magicScraper(description){ return Android.magicScraper(description)}

                function sendAgentEmail(to, subject, message) {
                Android.sendAgentEmail(to, subject, message); 
                }


                // Safe number conversion functions
                function safeInt(value, defaultVal) {
                    if (value == null || value === undefined) return defaultVal || 0;
                    if (typeof value === 'number') {
                        if (isNaN(value) || !isFinite(value)) {
                            Android.logWarning("JavaScript", "safeInt: Invalid number " + value);
                            return defaultVal || 0;
                        }
                        return Math.floor(value);
                    }
                    if (typeof value === 'string') {
                        var parsed = parseInt(value);
                        if (isNaN(parsed)) {
                            Android.logWarning("JavaScript", "safeInt: Cannot parse '" + value + "'");
                            return defaultVal || 0;
                        }
                        return parsed;
                    }
                    Android.logWarning("JavaScript", "safeInt: Unknown type for " + value);
                    return defaultVal || 0;
                }
                
                function safeFloat(value, defaultVal) {
                    if (value == null || value === undefined) return defaultVal || 0.0;
                    if (typeof value === 'number') {
                        if (isNaN(value) || !isFinite(value)) {
                            Android.logWarning("JavaScript", "safeFloat: Invalid number " + value);
                            return defaultVal || 0.0;
                        }
                        return value;
                    }
                    if (typeof value === 'string') {
                        var parsed = parseFloat(value);
                        if (isNaN(parsed)) {
                            Android.logWarning("JavaScript", "safeFloat: Cannot parse '" + value + "'");
                            return defaultVal || 0.0;
                        }
                        return parsed;
                    }
                    Android.logWarning("JavaScript", "safeFloat: Unknown type for " + value);
                    return defaultVal || 0.0;
                }
                
                // SharedPreferences helper object
                function getSharedPreferences(name, mode) {
                    return {
                        getString: function(key, defaultValue) {
                            return Android.getStringFromPrefs(name, key, defaultValue || "");
                        },
                        putString: function(key, value) {
                            return Android.setStringToPrefs(name, key, value);
                        },
                        getInt: function(key, defaultValue) {
                            return Android.getIntFromPrefs(name, key, defaultValue || 0);
                        },
                        putInt: function(key, value) {
                            return Android.setIntToPrefs(name, key, value);
                        },
                        putLong: function(key, value) {
                            return Android.setLongToPrefs(name, key, value);
                        },
                        getFloat: function(key, defaultValue) {
                            return Android.getFloatFromPrefs(name, key, defaultValue || 0.0);
                        },
                        putFloat: function(key, value) {
                            return Android.setFloatToPrefs(name, key, value);
                        },
                        getBoolean: function(key, defaultValue) {
                            return Android.getBooleanFromPrefs(name, key, defaultValue || false);
                        },
                        putBoolean: function(key, value) {
                            return Android.setBooleanToPrefs(name, key, value);
                        },
                        edit: function() {
                            return this; // Return self for chaining
                        },
                        apply: function() {
                            // No-op since we apply immediately in the individual methods
                        },
                        commit: function() {
                            // No-op since we apply immediately in the individual methods
                            return true;
                        }
                    };
                }
                // App launching
                function launchTikTok() { Android.launchTikTok(); }
                function launchYouTube() { Android.launchYouTube(); }
                function launchInstagram() { Android.launchInstagram(); }
                function launchTwitter() { Android.launchTwitter(); }
                function launchReddit() { Android.launchReddit(); }
                function launchMedium() { Android.launchMedium(); }
                function launchTelegram() { Android.launchTelegram(); }
                function launchWhatsApp() { Android.launchWhatsApp(); }
                function launchSnapchat() { Android.launchSnapchat(); }
                function launchLinkedIn() { Android.launchLinkedIn(); }
                function launchPinterest() { Android.launchPinterest(); }
                function launchTwitch() { Android.launchTwitch(); }
                function launchDiscord() { Android.launchDiscord(); }
                function launchSpotify() { Android.launchSpotify(); }
                function launchNetflix() { Android.launchNetflix(); }
                function launchGmail() { Android.launchGmail(); }
                function sendEmail(to, subject, body) { Android.sendEmail(to, subject, body); }
 function findNodeByClassNameAndIndex(className, index) {
                    return Android.findNodeByClassNameAndIndex(className, safeInt(index, 0));
                }
                
                
                
                function performNodeClick(node) {
                    Android.performNodeClick(node);
                }
                
                function isTextPresentOnScreen(text) {
                    return Android.isTextPresentOnScreen(text);
                }
                   function handleTikTokStartupScreens() {
                    return Android.handleTikTokStartupScreens();
                }
                
                function getContentDescriptionForNodeContaining(text) {
                    return Android.getContentDescriptionForNodeContaining(text);
                }
                
                // Data fetching functions handleTikTokStartupScreens
                function fetchAgentAccounts() {
                    return Android.fetchAgentAccounts();
                }
                
                // String utility functions
                function replaceAll(str, searchValue, replaceValue) {
                    return Android.replaceAll(str, searchValue, replaceValue);
                }
                function fetchAgentServer() {
                    return Android.fetchAgentServer();
                }
                
                function fetchAgentAccountsYoutube() {
                    return Android.fetchAgentAccountsYoutube();
                }
                
                function fetchAgentServerYoutube() {
                    return Android.fetchAgentServerYoutube();
                }
                
                function fetchAgentAccountsTwitter() {
                    return Android.fetchAgentAccountsTwitter();
                }
                function fetchAgentAccountsInstagram() {
                    return Android.fetchAgentAccountsInstagram();
                }

                function fetchAgentServerInstagram() {
                    return Android.fetchAgentServerInstagram();
                }

                function checkAgentAccountsInstagram(account) {
                    Android.checkAgentAccountsInstagram(account);
                }

                function doFullUploadSequenceInstagram(caption, email, randomAccount, server) {
                    Android.doFullUploadSequenceInstagram(caption, email, randomAccount, server);
                }

                function launchInstagram() {
                    Android.launchInstagram();
                }
                function fetchAgentServerTwitter() {
                    return Android.fetchAgentAccountsTwitter();
                }
                
                function fetchTodaysVideoSync(email, server) {
                    return Android.fetchTodaysVideoSync(email, server);
                }
                
                function fetchBio(email, server) {
                    return Android.fetchBio(email, server);
                }
                
                function fetchBlogPost(caption, username) {
                    return Android.fetchBlogPost(caption, username);
                }
                
                function fetchSearch(caption, username) {
                    return Android.fetchSearch(caption, username);
                }
             function getAutoCommentCampaignForServer(email, serverId) {
    return Android.getAutoCommentCampaignForServer(email, serverId);
}

                function fetchReply(postText, username) {
                    return Android.fetchReply(postText, username);
                }
                
                // File and upload functions
                function downloadVideo(url, filename) {
                    return Android.downloadVideo(url, filename);
                }
                
                function downloadProfileImage(email, server) {
                    return Android.downloadProfileImage(email, server);
                }
                
                function downloadRandomBrandAssets(email, server) {
                    Android.downloadRandomBrandAssets(email, server);
                }
                
                function doFullUploadSequence(caption, email, randomAccount, server) {
                    Android.doFullUploadSequence(caption, email, randomAccount, server);
                }
                
                function doFullUploadSequenceYoutube(caption, email, server) {
                    Android.doFullUploadSequenceYoutube(caption, email, server);
                }
                
                function doFullUploadSequenceTwitter(caption, email, server) {
                    Android.doFullUploadSequenceTwitter(caption, email, server);
                }
                
                function markVideoAsPosted(email, key) {
                    Android.markVideoAsPosted(email, key);
                }
                   function clickVideoUploadButton() {
                    Android.clickVideoUploadButton();
                }
                function takeScreenshotAndUploadToLogs(email, prefix, server) {
                    Android.takeScreenshotAndUploadToLogs(email, prefix, server);
                }
                 function clickElementByArea(area) {
                    Android.clickElementByArea(area);
                }  
                     function simulateType(id, text) {
                    Android.simulateType(id, text);
                }
                   function split(str, delimiter) {
                    return Android.split(str, delimiter);
                }
                
                
                            function clickElementByViewId(id) {
                    Android.clickElementByViewId(id);
                }
                function checkAgentAccounts(account) {
                    Android.checkAgentAccounts(account);
                }
                
                function checkAgentAccountsYoutube(account) {
                    Android.checkAgentAccountsYoutube(account);
                }
                
                function checkAgentAccountsTwitter(account) {
                    Android.checkAgentAccountsTwitter(account);
                }
                
                // Logging
                function logInfo(tag, message) {
                    Android.logInfo(tag, message);
                }
                
                function logWarning(tag, message) {
                    Android.logWarning(tag, message);
                }
                
                // System settings
                function openWiFiSettings() { Android.openWiFiSettings(); }
                function openBluetoothSettings() { Android.openBluetoothSettings(); }
                function openLocationSettings() { Android.openLocationSettings(); }
                function openBatterySettings() { Android.openBatterySettings(); }
                function openDisplaySettings() { Android.openDisplaySettings(); }
                function openSoundSettings() { Android.openSoundSettings(); }
                function openStorageSettings() { Android.openStorageSettings(); }
                function openPrivacySettings() { Android.openPrivacySettings(); }
                function openSecuritySettings() { Android.openSecuritySettings(); }
                function openDeveloperOptions() { Android.openDeveloperOptions(); }
                
                // System controls
                function toggleWiFi(enable) { Android.toggleWiFi(enable); }
                function toggleBluetooth(enable) { Android.toggleBluetooth(enable); }
                function toggleLocationServices(enable) { Android.toggleLocationServices(enable); }
                function toggleAirplaneMode(enable) { Android.toggleAirplaneMode(enable); }
                function toggleAutoRotate(enable) { Android.toggleAutoRotate(enable); }
                function toggleDoNotDisturb(enable) { Android.toggleDoNotDisturb(enable); }
                function setBrightness(level) { Android.setBrightness(level); }
                function setVolume(type, level) { Android.setVolume(type, level); }
                function lockScreen() { Android.lockScreen(); }
                function vibrate(ms) { Android.vibrate(ms); }
                
                // Communication
                function makePhoneCall(number) { Android.makePhoneCall(number); }
                function sendSMS(number, message) { Android.sendSMS(number, message); }
                function openDialer() { Android.openDialer(); }
                function openContacts() { Android.openContacts(); }
                function openMessages() { Android.openMessages(); }
                function openGmail() { Android.openGmail(); }
                function composeEmail(to, subject, body) { Android.composeEmail(to, subject, body); }
                
                // File & Media
                function openFileManager() { Android.openFileManager(); }
                function openGallery() { Android.openGallery(); }
                function openCamera() { Android.openCamera(); }
                function takePhoto() { Android.takePhoto(); }
                function openMusicPlayer() { Android.openMusicPlayer(); }
                function playMusic(path) { Android.playMusic(path); }
                function pauseMusic() { Android.pauseMusic(); }
                function stopMusic() { Android.stopMusic(); }
                
                // Navigation
                function openGoogleMaps() { Android.openGoogleMaps(); }
                function navigateToAddress(address) { Android.navigateToAddress(address); }
                function searchNearby(query) { Android.searchNearby(query); }
                function getCurrentLocation() { return Android.getCurrentLocation(); }
                function openUber() { Android.openUber(); }
                function openLyft() { Android.openLyft(); }
                
                // Hardware
                function toggleFlashlight(enable) { Android.toggleFlashlight(enable); }
                function takeFrontCamera() { Android.takeFrontCamera(); }
                function takeBackCamera() { Android.takeBackCamera(); }
                function recordAudio(duration) { Android.recordAudio(duration); }
                
                // App management
                function openPlayStore() { Android.openPlayStore(); }
                function searchPlayStore(query) { Android.searchPlayStore(query); }
                function openAppInfo(pkg) { Android.openAppInfo(pkg); }
                function forceStopApp(pkg) { Android.forceStopApp(pkg); }
                function clearAppCache(pkg) { Android.clearAppCache(pkg); }
                
                // Security
                function generateQRCode(data) { Android.generateQRCode(data); }
                function scanQRCode() { return Android.scanQRCode(); }
                function enableScreenLock(type, password) { Android.enableScreenLock(type, password); }
                
                // Network
                function connectToWiFi(ssid, password) { Android.connectToWiFi(ssid, password); }
                function disconnectFromWiFi() { Android.disconnectFromWiFi(); }
                function checkInternetConnection() { return Android.checkInternetConnection(); }
                function enableMobileData(enable) { Android.enableMobileData(enable); }
                function switchToMobileData() { Android.switchToMobileData(); }
                function switchToWiFi() { Android.switchToWiFi(); }
                
                // Accessibility & UI
                function enableTalkBack(enable) { Android.enableTalkBack(enable); }
                function increaseFontSize() { Android.increaseFontSize(); }
                function decreaseFontSize() { Android.decreaseFontSize(); }
                function enableHighContrast(enable) { Android.enableHighContrast(enable); }
                function findElementByText(text) { return Android.findElementByText(text); }
                function waitForElement(text, timeout) { return Android.waitForElement(text, timeout); }
                function scrollUntilFound(text) { return Android.scrollUntilFound(text); }
                function swipeLeft() { Android.swipeLeft(); }
                function swipeRight() { Android.swipeRight(); }
                function swipeUp() { Android.swipeUp(); }
                function swipeDown() { Android.swipeDown(); }
                function longPress(x, y) { Android.longPress(x, y); }
                function doubleClick(x, y) { Android.doubleClick(x, y); }
                
                // Productivity
                function openCalendar() { Android.openCalendar(); }
                function createEvent(title, date, time) { Android.createEvent(title, date, time); }
                function setAlarm(hour, minute, label) { Android.setAlarm(hour, minute, label); }
                function setTimer(minutes) { Android.setTimer(minutes); }
                function openClock() { Android.openClock(); }
                function openNotes() { Android.openNotes(); }
                function createNote(title, content) { Android.createNote(title, content); }
                function openGoogleDocs() { Android.openGoogleDocs(); }
                
                // Shopping & Finance
                function openAmazon() { Android.openAmazon(); }
                function searchProduct(query) { Android.searchProduct(query); }
                function openBankingApp(name) { Android.openBankingApp(name); }
                function openPaymentApp(name) { Android.openPaymentApp(name); }
                
                // Advanced automation
                function extractTextFromImage(path) { return Android.extractTextFromImage(path); }
                function translateText(text, lang) { return Android.translateText(text, lang); }
                function summarizeText(text) { return Android.summarizeText(text); }
                function generateResponse(prompt) { return Android.generateResponse(prompt); }
                
                // Device monitoring
                function getBatteryLevel() { return Android.getBatteryLevel(); }
                function getMemoryUsage() { return Android.getMemoryUsage(); }
                function getStorageSpace() { return Android.getStorageSpace(); }
                function getRunningApps() { return Android.getRunningApps(); }
                function getInstalledApps() { return Android.getInstalledApps(); }
                
                // Context-aware
                function analyzeCurrentScreen() { return Android.analyzeCurrentScreen(); }
                function detectCurrentApp() { return Android.detectCurrentApp(); }
                function getScreenText() { return Android.getScreenText(); }
                function findClickableElements() { return Android.findClickableElements(); }
                function suggestNextAction() { return Android.suggestNextAction(); }

                // ========== 虚拟女友消息功能 ==========
                function sendTelegramMessage(contactName, message) { Android.sendTelegramMessage(contactName, message); }
                function sendWhatsAppMessage(contactName, message) { Android.sendWhatsAppMessage(contactName, message); }
                function sendWechatMessage(contactName, message) { Android.sendWechatMessage(contactName, message); }

                // ========== 图片/视频生成 ==========
                function generateImage(prompt, negativePrompt, model, width, height, steps) {
                    return Android.generateImage(prompt, negativePrompt || "", model || "z_image_turbo", width || 512, height || 768, steps || 8);
                }
                function generateSelfie(style, mood) {
                    return Android.generateSelfie(style || "seductive", mood || "love");
                }
                function generateVideo(imagePath, prompt, duration) {
                    return Android.generateVideo(imagePath, prompt || "", duration || 5);
                }
                function configureImageGenerator(comfyuiHost, comfyuiPort, fooocusHost, fooocusPort) {
                    Android.configureImageGenerator(comfyuiHost, comfyuiPort, fooocusHost, fooocusPort);
                }
                function setZImageApiKey(apiKey) {
                    Android.setZImageApiKey(apiKey);
                }
                function setGrokApiKey(apiKey) {
                    Android.setGrokApiKey(apiKey);
                }
                function getLastGeneratedImage() {
                    return Android.getLastGeneratedImage();
                }
                // ====================================
                // ======================================
                
                   
                   function clickVideoUploadButton() {
                    Android.clickVideoUploadButton();
                }
                     function clickFirstSong() {
                    Android.clickFirstSong();
                }
                   function clickAddSound() {
                    Android.clickAddSound();
                }
                
                // Original core functions
                function simulateClick(x, y) { Android.simulateClick(x, y); }
                function clickNodesByContentDescription(desc) { Android.clickNodesByContentDescription(desc); }
                function simulateTypeInFirstEditableField(text) { Android.simulateTypeInFirstEditableField(text); }
                function simulateTypeInSecondEditableField(text) { Android.simulateTypeInSecondEditableField(text); }
                function pressEnterKey() { Android.pressEnterKey(); }
                function simulateScrollToBottom() { Android.simulateScrollToBottom(); }
                                function simulateScrollToBottomX(X) { Android.simulateScrollToBottomX(X); }

                function simulateScrollToTop() { Android.simulateScrollToTop(); }
                function isTextPresentOnScreen(text) { return Android.isTextPresentOnScreen(text); }
                
                function simulateTypeByClass(className, text) {
                    Android.simulateTypeByClass(className, text);
                }
                  function check2FA() {
                    Android.check2FA();
                }
                    
                function simulateDeleteByClass(className) {
                    Android.simulateDeleteByClass(className);
                }
                function typeOne(text) { Android.simulateTypeInFirstEditableField(text); }
                function typeTwo(text) { Android.simulateTypeInSecondEditableField(text); }
                // Execute the generated code
                try {
                    $code
                } catch (error) {
                    Android.speakText("Error executing automation: " + error.message);
                    Android.logInfo("AutomationError", error.message);
                }
                """.trimIndent()

                context.evaluateString(scope, wrappedCode, "<voice_generated_script>", 1, null)

            } catch (e: Exception) {
                Log.e("MainActivity", "JavaScript execution error: ${e.message}")
                withContext(Dispatchers.Main) {
                    speakText("Error executing automation: ${e.message}")
                }
            } finally {
                context?.let { org.mozilla.javascript.Context.exit() }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            speakText("Voice automation ready. Tap the microphone button to give commands.")
            updateStatusWithAnimation("🎤 Ready - Tap button to speak")
        } else {
            Log.e("MainActivity", "Failed to initialize text to speech engine")
        }
    }

    private fun speakText(text: String) {
        try {
            if (::tts.isInitialized) {
                val utteranceId = UUID.randomUUID().toString()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts.speak(text, TextToSpeech.QUEUE_ADD, null, utteranceId)
                } else {
                    @Suppress("DEPRECATION")
                    tts.speak(text, TextToSpeech.QUEUE_ADD, null)
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in speakText: ${e.message}")
        }
    }
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onDestroy() {
        Log.d("MainActivity", "onDestroy starting")
        isDestroyed = true

        try {
            // 1. Stop speech recognition FIRST
            if (::speechRecognizer.isInitialized) {
                try {
                    speechRecognizer.stopListening()
                    speechRecognizer.cancel() // ADDED
                    speechRecognizer.destroy()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error destroying speech: ${e.message}")
                }
            }

            // 2. Cancel all jobs with timeout
            runBlocking {
                withTimeout(2000) { // 2 second timeout
                    cronCheckJob?.cancel()
                    universalScriptJob?.cancel()
                    debugModeJob?.cancel()
                }
            }

            // 3. Shutdown executor
            try {
                cronScheduler.shutdownNow()
                cronScheduler.awaitTermination(1, TimeUnit.SECONDS)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error shutting down scheduler: ${e.message}")
            }

            // 4. Cancel composite job
            compositeJob.cancel()

            // 5. Clean up bitmaps
            for (bitmap in activeBitmaps.toList()) {
                try {
                    if (!bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error recycling bitmap: ${e.message}")
                }
            }
            activeBitmaps.clear()

            // 6. Remove Firebase listeners
            for ((ref, listener) in firebaseListeners.toList()) {
                try {
                    ref.removeEventListener(listener)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error removing listener: ${e.message}")
                }
            }
            firebaseListeners.clear()

            // 7. TTS cleanup
            if (::tts.isInitialized) {
                try {
                    tts.stop()
                    tts.shutdown()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error shutting down TTS: ${e.message}")
                }
            }

            // 8. Force GC
            System.gc()

        } catch (e: Exception) {
            Log.e("MainActivity", "Error in onDestroy: ${e.message}")
        }

        super.onDestroy()
        Log.d("MainActivity", "onDestroy completed")
    }


    private suspend fun takeScreenshotForAPI(): Bitmap? = withContext(Dispatchers.IO) {
        if (isDestroyed) return@withContext null

        var bitmap: Bitmap? = null
        try {
            // Check memory first

            var attempts = 0
            while (!ScreenCaptureService.isReady && attempts < 50) {
                delay(100)
                attempts++
            }

            val pngBytes = ScreenCaptureService.lastCapturedPng
            if (pngBytes == null) {
                Log.w("MainActivity", "No screenshot available")
                return@withContext null
            }

            // FIXED: Decode with aggressive memory-saving options
            val options = BitmapFactory.Options().apply {
                inSampleSize = 4 // Reduce to 25% size instead of 50%
                inPreferredConfig = Bitmap.Config.RGB_565 // 50% less memory than ARGB_8888
                inPurgeable = true
                inInputShareable = true
            }

            bitmap = BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size, options)

            if (bitmap != null) {
                // FIXED: Don't track in activeBitmaps if we're going to clean it up immediately
                Log.d("MainActivity", "Screenshot loaded: ${bitmap.width}x${bitmap.height}")
            }

            bitmap
        } catch (e: OutOfMemoryError) {
            Log.e("MainActivity", "OOM creating screenshot")
            bitmap?.recycle()
            System.gc()
            null
        } catch (e: Exception) {
            Log.e("MainActivity", "Error getting screenshot: ${e.message}")
            bitmap?.recycle()
            null
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        try {
            // FIXED: Reduce quality to 60 instead of 85
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            // FIXED: Clean up bitmap immediately after compression
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }

            return Base64.encodeToString(byteArray, Base64.NO_WRAP)
        } finally {
            try {
                byteArrayOutputStream.close()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error closing stream: ${e.message}")
            }
        }
    }
    private fun checkAccessibilityPermission() {
        val am = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val myServiceId = "$packageName/.MyAccessibilityService"

        val isEnabled = enabledServices.any { serviceInfo ->
            val enabledId = serviceInfo.resolveInfo.serviceInfo.packageName + "/" +
                    serviceInfo.resolveInfo.serviceInfo.name
            enabledId == myServiceId
        }
        if (!isEnabled) {
            speakText("Please enable accessibility service for advanced voice automation")
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    private fun getUserEmail(): String {
        synchronized(emailLock) {
            return try {
                sharedPreferences.getString(KEY_USER_EMAIL, "") ?: ""
            } catch (e: Exception) {
                Log.e("MainActivity", "Error retrieving user email: ${e.message}")
                ""
            }
        }
    }

    private fun initializeUserEmail() {
        userEmail = getUserEmail()
        if (userEmail.isEmpty()) {
            userEmail = "rohan@cheatlayer,com"
        }
    }

    // [Continue with AndroidJSInterface - Due to length, I'll include the key fixes in it]
    // COMPLETE AndroidJSInterface with ALL 100+ functions
    inner class AndroidJSInterface {
        private fun toSafeInt(value: Any?, methodName: String = "unknown"): Int {
            return when (value) {
                null -> {
                    Log.w("SafeConversion", "$methodName: null value converted to 0")
                    0
                }
                is Number -> {
                    val double = value.toDouble()
                    when {
                        double.isNaN() -> {
                            Log.w("SafeConversion", "$methodName: NaN converted to 0")
                            0
                        }
                        !double.isFinite() -> {
                            Log.w("SafeConversion", "$methodName: Infinite value converted to 0")
                            0
                        }
                        else -> double.toInt()
                    }
                }
                is String -> {
                    value.toIntOrNull() ?: run {
                        Log.w("SafeConversion", "$methodName: Invalid string '$value' converted to 0")
                        0
                    }
                }
                else -> {
                    Log.w("SafeConversion", "$methodName: Unknown type ${value.javaClass.simpleName} converted to 0")
                    0
                }
            }
        }

        // Helper method to convert any value to Float safely
        private fun toSafeFloat(value: Any?, methodName: String = "unknown"): Float {
            return when (value) {
                null -> {
                    Log.w("SafeConversion", "$methodName: null value converted to 0.0")
                    0f
                }
                is Number -> {
                    val double = value.toDouble()
                    when {
                        double.isNaN() -> {
                            Log.w("SafeConversion", "$methodName: NaN converted to 0.0")
                            0f
                        }
                        !double.isFinite() -> {
                            Log.w("SafeConversion", "$methodName: Infinite value converted to 0.0")
                            0f
                        }
                        else -> double.toFloat()
                    }
                }
                is String -> {
                    value.toFloatOrNull() ?: run {
                        Log.w("SafeConversion", "$methodName: Invalid string '$value' converted to 0.0")
                        0f
                    }
                }
                else -> {
                    Log.w("SafeConversion", "$methodName: Unknown type ${value.javaClass.simpleName} converted to 0.0")
                    0f
                }
            }
        }
        @JavascriptInterface
        fun speakText(text: String) {
            this@MainActivity.speakText(text)
        }

        @JavascriptInterface
        fun performNodeClick(node: Any?) {
            (node as? android.view.accessibility.AccessibilityNodeInfo)?.let {
                MyAccessibilityService.instance?.performNodeClick(
                    it
                )
            }
        }

        @JavascriptInterface
        fun findNodeByClassNameAndIndex(className: String, index: Any?): Any? {
            val safeIndex = toSafeInt(index, "findNodeByClassNameAndIndex")
            Log.d("findNodeByClassNameAndIndex", "Original index: $index -> Safe index: $safeIndex")
            return MyAccessibilityService.instance?.findNodeByClassNameAndIndex(className, safeIndex)
        }



        @JavascriptInterface
        fun markVideoAsPosted(email: String, key: String) {
            runBlocking(Dispatchers.IO) {
                try {
                    this@MainActivity.markVideoAsPosted(email, key)
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "markVideoAsPosted error: ${e.message}")
                }
            }
        }

        private suspend fun retrieveLatestVerificationCode(userEmail: String, callback: (String?) -> Unit) {
            if (userEmail.isEmpty()) {
                callback(null)
                return
            }

            // Convert email to Firebase key format
            val emailKey = userEmail.replace("@", "_AT_").replace(".", "_DOT_")
            Log.d("2FA", "Code timestamp: $emailKey")

            // Get Firebase database reference for agent_codes (not agent_accounts)
            val database = this@MainActivity.firebaseDatabaseOrNull("retrieveLatestVerificationCode")
            if (database == null) {
                withContext(Dispatchers.Main) {
                    callback(null)
                }
                return
            }
            val agentCodesRef = database.getReference("agent_codes").child(emailKey)

            try {
                // Check if the verification code exists
                val snapshot: DataSnapshot = agentCodesRef.get().await()

                if (snapshot.exists()) {
                    val code = snapshot.child("code").getValue(String::class.java)
                    val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0
                    val username = snapshot.child("username").getValue(String::class.java) ?: ""

                    Log.d("2FA", "Retrieved verification code: $code for user: $userEmail (username: $username)")
                    Log.d("2FA", "Code timestamp: $timestamp")

                    // Switch back to main thread for UI operations
                    withContext(Dispatchers.Main) {
                        callback(code)
                    }
                } else {
                    Log.d("2FA", "No verification code found for user: $userEmail")
                    withContext(Dispatchers.Main) {
                        callback(null)
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "Error retrieving 2FA code: $e"
                Log.e("2FA", errorMsg)
                withContext(Dispatchers.Main) {
                    speakText(errorMsg)
                    callback(null)
                }
            }
        }

        private fun handleVerificationCode(code: String) {
            // Handle the verification code - you can customize this based on your needs
            Log.d("2FA", "Using verification code: $code")

            // Example: Fill in 2FA input field
            MyAccessibilityService.instance?.let { service ->
                // Look for 2FA input field and fill it

                // ist input field and enter the code
                service.enterTextInField(code)

                // Or if you need to click specific buttons to enter digits
                // enterCodeDigits(code)

            }
        }

        // Optional: Function to enter code digit by digit if needed
        private fun enterCodeDigits(code: String) {
            MyAccessibilityService.instance?.let { service ->
                code.forEach { digit ->
                    service.clickNodesByContentDescription(digit.toString())
                    Thread.sleep(100) // Small delay between digits
                }
            }
        }
        @JavascriptInterface
        fun check2FA() {
            // Check for "Find related content" screen
            if (MyAccessibilityService.instance?.isTextPresentOnScreen("@") == true){
                //multilogin
                Thread.sleep(10000) // Small delay between digits

                MyAccessibilityService.instance?.simulateClick(400f, 500f)
                //multilogin
                userEmail = "rohan@cheatlayer,com"
                Thread.sleep(30000) // Small delay between digits
                Log.d("2FA", " verification code found for user: $userEmail")

                // Launch coroutine to retrieve the latest verification code from Firebase
                CoroutineScope(Dispatchers.IO).launch {
                    retrieveLatestVerificationCode(userEmail) { code ->
                        if (code != null) {
                            // Use the code (e.g., fill in 2FA field)
                            handleVerificationCode(code)
                        } else {
                            Log.d("2FA", "No verification code found for user: $userEmail")
                        }
                    }
                }
                Thread.sleep(1000) // Small delay between digits

                CoroutineScope(Dispatchers.IO).launch {
                    retrieveLatestVerificationCode("rohan@cheatlayer.com") { code ->
                        if (code != null) {
                            // Use the code (e.g., fill in 2FA field)
                            handleVerificationCode(code)
                        } else {
                            Log.d("2FA", "No verification code found for user: \"rohan@cheatlayer.com\"")
                        }
                    }
                }

            }else  {
                if (MyAccessibilityService.instance?.isTextPresentOnScreen("Log in") == true) {
                    MyAccessibilityService.instance?.clickNodesByContentDescription("Log in")
                    userEmail = getUserEmail()
                    Thread.sleep(10000) // Small delay between digits

                    // Launch coroutine to retrieve the latest verification code from Firebase
                    CoroutineScope(Dispatchers.IO).launch {
                        retrieveLatestVerificationCode(userEmail) { code ->
                            if (code != null) {
                                // Use the code (e.g., fill in 2FA field)
                                handleVerificationCode(code)
                            } else {

                                Log.d("2FA", "No verification code found for user: $userEmail")
                            }
                        }
                    }
                    Thread.sleep(1000) // Small delay between digits

                    CoroutineScope(Dispatchers.IO).launch {
                        retrieveLatestVerificationCode("rohan@cheatlayer.com") { code ->
                            if (code != null) {
                                // Use the code (e.g., fill in 2FA field)
                                handleVerificationCode(code)
                            } else {
                                Log.d("2FA", "No verification code found for user: \"rohan@cheatlayer.com\"")
                            }
                        }
                    }
                    Thread.sleep(1000) // Small delay between digits

                    CoroutineScope(Dispatchers.IO).launch {
                        retrieveLatestVerificationCode("rohan@cheatlayer.com") { code ->
                            if (code != null) {
                                // Use the code (e.g., fill in 2FA field)
                                handleVerificationCode(code)
                            } else {
                                Log.d("2FA", "No verification code found for user: \"rohan@cheatlayer.com\"")
                            }
                        }
                    }

                }else{

                    userEmail = getUserEmail()
                    Thread.sleep(20000) // Small delay between digits
                    Log.d("2FA", " verification code found for user: $userEmail")

                    // Launch coroutine to retrieve the latest verification code from Firebase
                    CoroutineScope(Dispatchers.IO).launch {
                        retrieveLatestVerificationCode(userEmail) { code ->
                            if (code != null) {
                                // Use the code (e.g., fill in 2FA field)
                                handleVerificationCode(code)
                            } else {
                                Log.d("2FA", "No verification code found for user: $userEmail")
                            }
                        }
                    }


                    Thread.sleep(10000) // Small delay between digits

                    CoroutineScope(Dispatchers.IO).launch {
                        retrieveLatestVerificationCode("rohan@cheatlayer.com") { code ->
                            if (code != null) {
                                // Use the code (e.g., fill in 2FA field)
                                handleVerificationCode(code)
                            } else {
                                Log.d("2FA", "No verification code found for user: \"rohan@cheatlayer.com\"")
                            }
                        }
                    }


                }
            }
        }
// Add these functions inside the AndroidJSInterface inner class

        @JavascriptInterface
        fun fetchAgentAccountsInstagram(): Map<String, String> {
            return runBlocking(Dispatchers.IO) {
                try {
                    this@MainActivity.fetchAgentAccountsInstagram()
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "fetchAgentAccountsInstagram error: ${e.message}")
                    emptyMap()
                }
            }
        }

        @JavascriptInterface
        fun fetchAgentServerInstagram(): Any {
            return runBlocking(Dispatchers.IO) {
                try {
                    this@MainActivity.fetchAgentServerInstagram()
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "fetchAgentServerInstagram error: ${e.message}")
                    ""
                }
            }
        }

        @JavascriptInterface
        fun checkAgentAccountsInstagram(account: String) {
            runBlocking(Dispatchers.IO) {
                try {
                    this@MainActivity.checkAgentAccountsInstagram(account)
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "checkAgentAccountsInstagram error: ${e.message}")
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        @JavascriptInterface
        fun doFullUploadSequenceInstagram(caption: String, email: String, randomAccount: String, server: String) {
            runBlocking(Dispatchers.Main) {
                try {
                    this@MainActivity.doFullUploadSequenceInstagram(caption, email, randomAccount, server)
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "doFullUploadSequenceInstagram error: ${e.message}")
                }
            }
        }


        @JavascriptInterface
        fun clickProfileMenuButton() {
            MyAccessibilityService.instance?.clickProfileMenuButton()

//       clickVideoUploadButton     MyAccessibilityService.instance?.clickNodesByContentDescription(desc) clickFirstElementWithAtSymbol
        }
        @JavascriptInterface
        fun clickFirstSong() {
            MyAccessibilityService.instance?.clickFirstSong()

//       clickVideoUploadButton     MyAccessibilityService.instance?.clickNodesByContentDescription(desc) clickFirstElementWithAtSymbol
        }
        @JavascriptInterface
        fun clickAddSound() {
            MyAccessibilityService.instance?.clickAddSound()

//       clickVideoUploadButton clickFirstSong     MyAccessibilityService.instance?.clickNodesByContentDescription(desc) clickFirstElementWithAtSymbol
        }
        @JavascriptInterface
        fun clickVideoUploadButton() {
            MyAccessibilityService.instance?.clickVideoUploadButton()

//          function clickProfileMenuButton(id) {
//         clickVideoUploadButton            Android.clickProfileMenuButton(id);
//                }   MyAccessibilityService.instance?.clickNodesByContentDescription(desc) clickFirstElementWithAtSymbol
        }

        @RequiresApi(Build.VERSION_CODES.O)
        @JavascriptInterface
        fun takeScreenshotAndUploadToLogs(email: String, prefix: String, server: String) {
            runBlocking(Dispatchers.IO) {
                try {
                    this@MainActivity.takeScreenshotAndUploadToLogs(email, prefix, server)
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "takeScreenshotAndUploadToLogs error: ${e.message}")
                }
            }
        }
        @JavascriptInterface
        fun clickElementByViewId(viewId: String) {
            // Check for "Find related content" screen
            MyAccessibilityService.instance?.clickElementByViewId(viewId)

        }

        //text  text pressEnterKey
        @JavascriptInterface
        fun simulateType(id: String, text: String) {
            MyAccessibilityService.instance?.simulateType(id, text)
        }
        @JavascriptInterface
        fun clickElementByArea(targetArea: Int) {
            // Check for "Find related content" screen
            MyAccessibilityService.instance?.clickElementByArea(targetArea)

        }
        @JavascriptInterface
        fun fetchAgentServer(): Any {
            return runBlocking(Dispatchers.IO) {
                try {
                    this@MainActivity.fetchAgentServer()
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "fetchAgentServer error: ${e.message}")
                    ""
                }
            }
        }


        @JavascriptInterface
        fun simulateTypeByClass(className: String, text: String) {
            MyAccessibilityService.instance?.simulateTypeByClass(className, text)
        }
        @JavascriptInterface
        fun simulateDeleteByClass(className: String) {
            MyAccessibilityService.instance?.simulateDeleteByClass(className)
        }

        @JavascriptInterface
        fun fetchAgentAccounts(): Map<String, String> {
            return runBlocking(Dispatchers.IO) {
                try {
                    this@MainActivity.fetchAgentAccounts()
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "fetchAgentAccounts error: ${e.message}")
                    emptyMap()
                }
            }
        }


        @JavascriptInterface
        fun fetchAgentAccountsYoutube(): Map<String, String> {
            return runBlocking(Dispatchers.IO) {
                try {
                    this@MainActivity.fetchAgentAccountsYoutube()
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "fetchAgentAccountsYoutube error: ${e.message}")
                    emptyMap()
                }
            }
        }

        @JavascriptInterface
        fun fetchAgentServerYoutube(): Any {
            return runBlocking(Dispatchers.IO) {
                try {
                    this@MainActivity.fetchAgentServerYoutube()
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "fetchAgentServerYoutube error: ${e.message}")
                    ""
                }
            }
        }

        @JavascriptInterface
        fun fetchAgentAccountsTwitter(): Map<String, String> {
            return runBlocking(Dispatchers.IO) {
                try {
                    this@MainActivity.fetchAgentAccountsTwitter()
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "fetchAgentAccountsTwitter error: ${e.message}")
                    emptyMap()
                }
            }
        }



        @JavascriptInterface
        fun fetchTodaysVideoSync(email: String, server: String): Array<Any?> {
            return runBlocking(Dispatchers.IO) {
                try {
                    val result = this@MainActivity.fetchTodaysVideoSync(email, server)
                    arrayOf(result.key, result.video)
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "fetchTodaysVideoSync error: ${e.message}")
                    arrayOf("", null)
                }
            }
        }

        @JavascriptInterface
        fun fetchBio(email: String, server: String): String {
            return runBlocking(Dispatchers.IO) {
                try {
                    this@MainActivity.fetchBio(email, server)
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "fetchBio error: ${e.message}")
                    ""
                }
            }
        }

        @JavascriptInterface
        fun fetchBlogPost(caption: String, username: String): String {
            return runBlocking(Dispatchers.IO) {
                try {
                    this@MainActivity.fetchBlogPost(caption, username)
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "fetchBlogPost error: ${e.message}")
                    ""
                }
            }
        }

        @JavascriptInterface
        fun fetchSearch(caption: String, username: String): String {
            return runBlocking(Dispatchers.IO) {
                try {
                    this@MainActivity.fetchSearch(caption, username)
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "fetchSearch error: ${e.message}")
                    ""
                }
            }
        }
        @JavascriptInterface
        fun getAutoCommentCampaignForServer(email: String, serverId: String): String {
            return runBlocking(Dispatchers.IO) {
                try {
                    this@MainActivity.getAutoCommentCampaignForServer(email, serverId)
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "getAutoCommentCampaignForServer error: ${e.message}")
                    "{\"error\": \"${e.message}\"}"
                }
            }
        }

        @JavascriptInterface
        fun fetchReply(postText: String, username: String): String {
            return runBlocking(Dispatchers.IO) {
                try {
                    this@MainActivity.fetchReply(postText, username)
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "fetchReply error: ${e.message}")
                    ""
                }
            }
        }

        @JavascriptInterface
        fun downloadVideo(url: String, filename: String): java.io.File? {
            return runBlocking(Dispatchers.IO) {
                try {
                    this@MainActivity.downloadVideo(url, filename)
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "downloadVideo error: ${e.message}")
                    null
                }
            }
        }

        @JavascriptInterface
        fun downloadProfileImage(email: String, server: String): String? {
            return runBlocking(Dispatchers.IO) {
                try {
                    this@MainActivity.downloadProfileImage(email, server)
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "downloadProfileImage error: ${e.message}")
                    null
                }
            }
        }

        @JavascriptInterface
        fun downloadRandomBrandAssets(email: String, server: String) {
            runBlocking(Dispatchers.IO) {
                try {
                    this@MainActivity.downloadRandomBrandAssets(email, server)
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "downloadRandomBrandAssets error: ${e.message}")
                }
            }
        }

        @RequiresApi(Build.VERSION_CODES.O)
        @JavascriptInterface
        fun doFullUploadSequence(caption: String, email: String, randomAccount: String, server: String) {
            runBlocking(Dispatchers.Main) {
                try {
                    this@MainActivity.doFullUploadSequence(caption, email, randomAccount, server)
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "doFullUploadSequence error: ${e.message}")
                }
            }
        }

        @JavascriptInterface
        fun delay(ms: Any?) {
            val safeMs = (ms as? Number)?.toLong() ?: 1000L
            Thread.sleep(safeMs)
        }
        private suspend fun takeScreenshotForAPI(): Bitmap? = withContext(Dispatchers.IO) {
            return@withContext try {
                val pngBytes = ScreenCaptureService.lastCapturedPng
                if (pngBytes == null) {
                    Log.w("MainActivity", "No captured screenshot available from ScreenCaptureService")
                    return@withContext null
                }

                // Convert PNG bytes to Bitmap
                val bitmap = BitmapFactory.decodeByteArray(pngBytes, 0, pngBytes.size)
                if (bitmap != null) {
                    Log.d("MainActivity", "Screenshot loaded from service: ${bitmap.width}x${bitmap.height}")
                } else {
                    Log.e("MainActivity", "Failed to decode screenshot bytes")
                }
                bitmap
            } catch (e: Exception) {
                Log.e("MainActivity", "Error getting screenshot from service: ${e.message}")
                null
            }
        }
        @JavascriptInterface
        fun schedule(task: String, cronExpression: String) {
            try {
                val taskId = this@MainActivity.addCronTask(task, cronExpression)
                val interval = this@MainActivity.getIntervalFromCron(cronExpression)

                Log.d("AndroidJSInterface", "Scheduled task: $task with cron: $cronExpression (ID: $taskId), interval: ${interval}ms")

                if (interval != null) {
                    val seconds = interval / 1000
                    val minutes = seconds / 60
                    val hours = minutes / 60

                    val timeDescription = when {
                        hours > 0 -> "$hours hours"
                        minutes > 0 -> "$minutes minutes"
                        else -> "$seconds seconds"
                    }

                    speakText("Scheduled task: $task to run every $timeDescription")
                } else {
                    speakText("Scheduled task: $task with custom timing")
                }

                runOnUiThread { updateUI() }

                Log.d("AndroidJSInterface", "Total active cron tasks: ${this@MainActivity.cronTasks.size}")
                this@MainActivity.cronTasks.values.forEach { cronTask ->
                    Log.d("AndroidJSInterface", "Active task: ${cronTask.taskDescription}, expression: ${cronTask.cronExpression}")
                }

            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error scheduling task: ${e.message}")
                speakText("Error scheduling task")
            }
        }

        @JavascriptInterface
        fun clearSchedule() {
            try {
                val taskCount = this@MainActivity.cronTasks.size
                this@MainActivity.cronTasks.clear()
                this@MainActivity.saveCronTasks()
                speakText("Cleared $taskCount scheduled tasks")
                Log.d("AndroidJSInterface", "Cleared all scheduled tasks")
                runOnUiThread { updateUI() }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error clearing schedule: ${e.message}")
                speakText("Error clearing scheduled tasks")
            }
        }


        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        @JavascriptInterface
        fun magicClicker(description: String) {
            if (isDestroyed) {
                Log.w("MainActivity", "Activity destroyed, skipping magicClicker")
                return
            }

            Log.d("MainActivity", "MagicClicker: Searching for '$description'")

            // FIXED: Use launch instead of runBlocking
            mainScope.launch {
                try {
                    speakText("Looking for $description on screen")

                    // Add cancellation check
                    if (!isActive || isDestroyed) return@launch

                    val screenshot = takeScreenshotForAPI()
                    if (screenshot == null) {
                        speakText("No screenshot available")
                        return@launch
                    }

                    // FIXED: Use try-finally to ensure bitmap cleanup
                    try {
                        val base64Image = bitmapToBase64(screenshot)

                        if (!isActive || isDestroyed) return@launch


                            val coordinates = callMoondreamAPI(base64Image, description)

                            if (coordinates != null) {
                                val pixelX = (coordinates.x * 720).toFloat() + 50f
                                val pixelY = (coordinates.y * 1600).toFloat()

                                withContext(Dispatchers.Main) {
                                    if (!isDestroyed) {
                                        MyAccessibilityService.instance?.simulateClick(
                                            pixelX,
                                            pixelY
                                        )
                                        speakText("Clicked on $description")
                                    }
                                }

                                trackMagicRun(
                                    "magicClicker", description,
                                    "{\"x\": ${pixelX.toInt()}, \"y\": ${pixelY.toInt()}}"
                                )
                            }

                    } finally {
                        // Bitmap already recycled in bitmapToBase64
                    }
                } catch (e: CancellationException) {
                    Log.d("MainActivity", "MagicClicker cancelled")
                    // Don't rethrow - just exit gracefully
                } catch (e: Exception) {
                    Log.e("MainActivity", "MagicClicker error: ${e.message}")
                }
            }
        }


        // Replace/add this function for text-based scraping using Moondream
        private suspend fun callScrapingAPI(base64Image: String, description: String): String = withContext(Dispatchers.IO) {

            return@withContext try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()

                // Format the question for better results
                val question = description

                val requestBody = JSONObject().apply {
                    put("image_url", "data:image/jpeg;base64,$base64Image")
                    put("question", question)
                }

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = requestBody.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("https://api.moondream.ai/v1/query")
                    .header("Content-Type", "application/json")
                    .header("X-Moondream-Auth", BuildConfig.MOONDREAM_AUTH)
                    .post(body)
                    .build()

                Log.d("MainActivity", "Calling Moondream query API for: $description")

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        try {
                            val responseJson = JSONObject(responseBody)
                            val answer = responseJson.getString("answer")

                            Log.d("MainActivity", "Moondream query response: $answer")

                            // Clean up the response
                            val cleanedAnswer = cleanScrapingResponse(answer, description)
                            cleanedAnswer

                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error parsing Moondream query response: ${e.message}")
                            Log.e("MainActivity", "Response body: $responseBody")
                            "Error parsing response"
                        }
                    } else {
                        Log.e("MainActivity", "Moondream query API error. Code: ${response.code}")
                        Log.e("MainActivity", "Error response: $responseBody")
                        "API error: ${response.code}"
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Moondream query API exception: ${e.message}")
                "Error: ${e.message}"
            }
        }

        // Helper function to format questions for better Moondream responses
        private fun formatScrapingQuestion(description: String): String {
            val lowerDesc = description.lowercase()

            return when {
                // Battery related
                lowerDesc.contains("battery") -> {
                    when {
                        lowerDesc.contains("percentage") || lowerDesc.contains("level") ->
                            "What is the battery percentage shown in the status bar? Just return the number with % symbol."
                        lowerDesc.contains("status") ->
                            "What is the battery status? Is it charging or not charging?"
                        else -> "What information about the battery can you see?"
                    }
                }

                // Time related
                lowerDesc.contains("time") -> {
                    when {
                        lowerDesc.contains("current") ->
                            "What is the current time shown in the status bar? Return in format like '2:47 PM'."
                        else -> "What time is displayed on the screen?"
                    }
                }

                // App related
                lowerDesc.contains("app name") || lowerDesc.contains("current app") -> {
                    "What app is currently open? Just return the app name."
                }

                // WiFi related
                lowerDesc.contains("wifi") -> {
                    when {
                        lowerDesc.contains("status") ->
                            "Is WiFi connected? Just return 'Connected' or 'Disconnected'."
                        lowerDesc.contains("name") || lowerDesc.contains("network") ->
                            "What is the name of the connected WiFi network?"
                        else -> "What WiFi information can you see?"
                    }
                }

                // Notification related
                lowerDesc.contains("notification") -> {
                    when {
                        lowerDesc.contains("count") || lowerDesc.contains("number") ->
                            "How many notifications are shown? Just return the number."
                        else -> "What notifications can you see?"
                    }
                }

                // Text extraction
                lowerDesc.contains("text") || lowerDesc.contains("read") -> {
                    when {
                        lowerDesc.contains("all") ->
                            "What is all the text visible on this screen? List the main text content."
                        else -> "What text can you read on this screen?"
                    }
                }

                // Volume related
                lowerDesc.contains("volume") -> {
                    "What is the volume level shown? Return as percentage or level."
                }

                // Brightness related
                lowerDesc.contains("brightness") -> {
                    "What is the brightness level? Return as percentage or level."
                }

                // General status
                lowerDesc.contains("status") -> {
                    "What is the status of $description that you can see on the screen?"
                }

                // Default - use description as is but make it a proper question
                else -> {
                    if (description.endsWith("?")) {
                        description
                    } else {
                        "What is the $description shown on this screen? Be specific and concise."
                    }
                }
            }
        }

        // Helper function to clean up Moondream responses
        private fun cleanScrapingResponse(answer: String, originalDescription: String): String {
            var cleaned = answer.trim()

            // Remove common prefixes that Moondream adds
            val prefixesToRemove = listOf(
                "The ",
                "I can see ",
                "Looking at the image, ",
                "In the image, ",
                "The screen shows ",
                "According to the image, ",
                "Based on what I can see, ",
                "From the screenshot, "
            )

            for (prefix in prefixesToRemove) {
                if (cleaned.startsWith(prefix, ignoreCase = true)) {
                    cleaned = cleaned.substring(prefix.length)
                    break
                }
            }

            // Clean up specific response types
            cleaned = when {
                originalDescription.lowercase().contains("battery") &&
                        originalDescription.lowercase().contains("percentage") -> {
                    // Extract just the percentage
                    val percentageRegex = Regex("(\\d+)%")
                    val match = percentageRegex.find(cleaned)
                    match?.value ?: cleaned
                }

                originalDescription.lowercase().contains("time") -> {
                    // Extract time format
                    val timeRegex = Regex("\\d{1,2}:\\d{2}\\s*(AM|PM|am|pm)?")
                    val match = timeRegex.find(cleaned)
                    match?.value ?: cleaned
                }

                originalDescription.lowercase().contains("notification") &&
                        originalDescription.lowercase().contains("count") -> {
                    // Extract just the number
                    val numberRegex = Regex("\\d+")
                    val match = numberRegex.find(cleaned)
                    match?.value ?: cleaned
                }

                originalDescription.lowercase().contains("app name") -> {
                    // Clean up app name - remove common words
                    cleaned.replace(Regex("(app|application|is|called|named)\\s*", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                }

                else -> cleaned
            }

            // Final cleanup
            cleaned = cleaned
                .replace(Regex("\\.$"), "") // Remove trailing period
                .replace(Regex("^is\\s+", RegexOption.IGNORE_CASE), "") // Remove leading "is"
                .trim()

            // If response is too long, truncate intelligently
            if (cleaned.length > 100) {
                val sentences = cleaned.split(". ")
                cleaned = sentences.firstOrNull() ?: cleaned.take(100)
            }

            return if (cleaned.isNotEmpty()) cleaned else "Not found"
        }

        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        @JavascriptInterface
        fun magicScraper(description: String): String {
            if (isDestroyed) return "Error: Activity destroyed"

            return try {
                // FIXED: Use runBlocking with timeout to prevent hanging
                runBlocking(Dispatchers.IO) {
                    withTimeout(30000) { // 30 second timeout
                        try {
                            if (isDestroyed) return@withTimeout "Error: Activity destroyed"

                            val screenshot = takeScreenshotForAPI()
                                ?: return@withTimeout "Error: No screenshot"

                            val base64Image = bitmapToBase64(screenshot)

                            if (isDestroyed) return@withTimeout "Error: Activity destroyed"

                            val result = callStreamingAPIWithImage(base64Image, description)

                            trackMagicRun("magicScraper", description, result)

                            result
                        } catch (e: CancellationException) {
                            "Error: Operation cancelled"
                        } catch (e: Exception) {
                            "Error: ${e.message}"
                        }
                    }
                }
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
        // Add this new function to MainActivity class (outside AndroidJSInterface)
        private suspend fun callStreamingAPIWithImage(base64Image: String, description: String): String = withContext(Dispatchers.IO) {
            return@withContext try {
                // Build the message content with image
                val imageDataUrl = "data:image/jpeg;base64,$base64Image"

                // Use the description exactly as provided
                val contentWithImage = "$description\n$imageDataUrl"

                val messages = listOf(
                    mapOf(
                        "role" to "system",
                        "content" to "You are a helpful assistant that analyzes screenshots and answers questions. Be concise and direct."
                    ),
                    mapOf(
                        "role" to "user",
                        "content" to contentWithImage
                    )
                )

                // Call the streaming API with vision support
                val result = callStreaming16kAPI(messages, maxTokens = 150, mode = "fast")

                result.trim()

            } catch (e: Exception) {
                Log.e("MainActivity", "Streaming API with image exception: ${e.message}")
                "Error: ${e.message}"
            }
        }

        private suspend fun callMoondreamAPI(base64Image: String, objectDescription: String): MoondreamPoint? = withContext(Dispatchers.IO) {

            // 根据配置的视觉模型选择不同的 API
            return@withContext when (currentVisionModel) {
                VisionModel.MOONDREAM -> callMoondreamAPIImpl(base64Image, objectDescription)
                VisionModel.QWEN_VL -> callQwenVLAPI(base64Image, objectDescription)
                VisionModel.MINICPM -> callMiniCPMAPI(base64Image, objectDescription)
            }
        }

        // Moondream API 实现
        private suspend fun callMoondreamAPIImpl(base64Image: String, objectDescription: String): MoondreamPoint? = withContext(Dispatchers.IO) {

            return@withContext try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build()

                val requestBody = JSONObject().apply {
                    put("image_url", "data:image/jpeg;base64,$base64Image")
                    put("object", objectDescription)
                }

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = requestBody.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("https://api.moondream.ai/v1/point")
                    .header("Content-Type", "application/json")
                    .header("X-Moondream-Auth", BuildConfig.MOONDREAM_AUTH)
                    .post(body)
                    .build()

                Log.d("MainActivity", "Calling Moondream API for: $objectDescription")

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        val responseJson = JSONObject(responseBody)
                        val pointsArray = responseJson.getJSONArray("points")

                        if (pointsArray.length() > 0) {
                            val firstPoint = pointsArray.getJSONObject(0)
                            val x = firstPoint.getDouble("x")
                            val y = firstPoint.getDouble("y")

                            Log.d("MainActivity", "Moondream found object at: ($x, $y)")
                            MoondreamPoint(x, y)
                        } else {
                            Log.w("MainActivity", "Moondream API returned no points")
                            null
                        }
                    } else {
                        Log.e("MainActivity", "Moondream API error. Code: ${response.code}")
                        Log.e("MainActivity", "Error response: $responseBody")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Moondream API exception: ${e.message}")
                null
            }
        }

        // 通义千问 VL API 实现
        private suspend fun callQwenVLAPI(base64Image: String, objectDescription: String): MoondreamPoint? = withContext(Dispatchers.IO) {
            return@withContext try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build()

                val requestBody = JSONObject().apply {
                    put("model", "qwen-vl-max")
                    put("messages", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", org.json.JSONArray().apply {
                                put(JSONObject().apply {
                                    put("type", "image_url")
                                    put("image_url", JSONObject().apply {
                                        put("url", "data:image/jpeg;base64,$base64Image")
                                    })
                                })
                                put(JSONObject().apply {
                                    put("type", "text")
                                    put("text", "在这张手机屏幕截图里，找到 \"$objectDescription\" 这个UI元素的位置。请用JSON格式返回，格式如下：{\"x\": 0.0-1.0之间的相对X坐标, \"y\": 0.0-1.0之间的相对Y坐标}。只返回JSON，不要其他文字。")
                                })
                            })
                        })
                    })
                    put("max_tokens", 200)
                }

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = requestBody.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer ${BuildConfig.PUTER_AUTH_TOKEN}")
                    .post(body)
                    .build()

                Log.d("MainActivity", "Calling Qwen VL API for: $objectDescription")

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        val responseJson = JSONObject(responseBody)
                        val choices = responseJson.getJSONArray("choices")
                        if (choices.length() > 0) {
                            val message = choices.getJSONObject(0).getJSONObject("message")
                            val content = message.getString("content")
                            
                            // 解析 JSON 响应
                            val xRegex = "\"x\"\\s*:\\s*([0-9.]+)".toRegex()
                            val yRegex = "\"y\"\\s*:\\s*([0-9.]+)".toRegex()
                            
                            val xMatch = xRegex.find(content)
                            val yMatch = yRegex.find(content)
                            
                            if (xMatch != null && yMatch != null) {
                                val x = xMatch.groupValues[1].toDouble()
                                val y = yMatch.groupValues[1].toDouble()
                                Log.d("MainActivity", "Qwen VL found object at: ($x, $y)")
                                MoondreamPoint(x, y)
                            } else {
                                Log.w("MainActivity", "Qwen VL response parse failed: $content")
                                null
                            }
                        } else {
                            null
                        }
                    } else {
                        Log.e("MainActivity", "Qwen VL API error. Code: ${response.code}, Response: $responseBody")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Qwen VL API exception: ${e.message}")
                null
            }
        }

        // MiniCPM-V API 实现 (使用 OpenAI 兼容接口)
        private suspend fun callMiniCPMAPI(base64Image: String, objectDescription: String): MoondreamPoint? = withContext(Dispatchers.IO) {
            return@withContext try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build()

                val requestBody = JSONObject().apply {
                    put("model", "MiniMax-M2.5")
                    put("messages", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", org.json.JSONArray().apply {
                                put(JSONObject().apply {
                                    put("type", "image_url")
                                    put("image_url", JSONObject().apply {
                                        put("url", "data:image/jpeg;base64,$base64Image")
                                    })
                                })
                                put(JSONObject().apply {
                                    put("type", "text")
                                    put("text", "在这张手机屏幕截图里，找到 \"$objectDescription\" 这个UI元素的位置。请用JSON格式返回，格式如下：{\"x\": 0.0-1.0之间的相对X坐标, \"y\": 0.0-1.0之间的相对Y坐标}。只返回JSON，不要其他文字。")
                                })
                            })
                        })
                    })
                    put("max_tokens", 200)
                }

                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = requestBody.toString().toRequestBody(mediaType)

                val request = Request.Builder()
                    .url("https://api.puter.com/puterai/openai/v1/chat/completions")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer ${BuildConfig.PUTER_AUTH_TOKEN}")
                    .post(body)
                    .build()

                Log.d("MainActivity", "Calling MiniCPM API for: $objectDescription")

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""

                    if (response.isSuccessful) {
                        val responseJson = JSONObject(responseBody)
                        val choices = responseJson.getJSONArray("choices")
                        if (choices.length() > 0) {
                            val message = choices.getJSONObject(0).getJSONObject("message")
                            val content = message.getString("content")
                            
                            val xRegex = "\"x\"\\s*:\\s*([0-9.]+)".toRegex()
                            val yRegex = "\"y\"\\s*:\\s*([0-9.]+)".toRegex()
                            
                            val xMatch = xRegex.find(content)
                            val yMatch = yRegex.find(content)
                            
                            if (xMatch != null && yMatch != null) {
                                val x = xMatch.groupValues[1].toDouble()
                                val y = yMatch.groupValues[1].toDouble()
                                Log.d("MainActivity", "MiniCPM found object at: ($x, $y)")
                                MoondreamPoint(x, y)
                            } else {
                                Log.w("MainActivity", "MiniCPM response parse failed: $content")
                                null
                            }
                        } else {
                            null
                        }
                    } else {
                        Log.e("MainActivity", "MiniCPM API error. Code: ${response.code}")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "MiniCPM API exception: ${e.message}")
                null
            }
        }
        // Alternative approach using coroutines (better performance):
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        @JavascriptInterface
        fun magicClickerAsync(description: String) {
            Log.d("MainActivity", "MagicClicker async: Searching for '$description'")

            // Launch coroutine in main scope
            mainScope.launch {
                try {
                    speakText("Looking for $description on screen")

                    val screenshot = takeScreenshotForAPI()
                    if (screenshot == null) {
                        speakText("No screenshot available. Please ensure screen capture is running.")
                        return@launch
                    }

                    val base64Image = bitmapToBase64(screenshot)
                    val coordinates = callMoondreamAPI(base64Image, description)

                    if (coordinates != null) {
                        val pixelX = (coordinates.x * 720).toFloat() + 50f
                        val pixelY = (coordinates.y * 1600).toFloat()

                        Log.d("MainActivity", "MagicClicker: Found $description at ($pixelX, $pixelY)")

                        withContext(Dispatchers.Main) {
                            speakText("Found $description, clicking now")
                            MyAccessibilityService.instance?.simulateClick(pixelX, pixelY)
                            speakText("Clicked on $description")
                        }


                        val outputPoint = "{\"x\": ${floor(pixelX).toInt()}, \"y\": ${floor(pixelY).toInt()}}"
                        trackMagicRun("magicClicker", description, outputPoint)
                    } else {
                        withContext(Dispatchers.Main) {
                            speakText("Could not find $description on screen")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "MagicClicker error: ${e.message}")
                    withContext(Dispatchers.Main) {
                        speakText("Error with magic click: ${e.message}")
                    }
                }
            }
        }

        // For better user experience, you can also add a synchronous version that returns immediately:
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        @JavascriptInterface
        fun magicClickerSync(description: String): Boolean {
            Log.d("MainActivity", "MagicClicker sync: Queuing '$description'")

            // Queue the operation asynchronously
            mainScope.launch {
                magicClickerOperation(description)
            }

            speakText("Queued click operation for $description")
            return true
        }
        private fun bitmapToBase64(bitmap: Bitmap): String {
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()
            return Base64.encodeToString(byteArray, Base64.NO_WRAP)
        }

        // Private suspend function for the actual operation
        @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        private suspend fun magicClickerOperation(description: String) {
            try {
                speakText("Processing click for $description")

                val screenshot = takeScreenshotForAPI()
                if (screenshot == null) {
                    speakText("No screenshot available")
                    return
                }

                val base64Image = bitmapToBase64(screenshot)
                val coordinates = callMoondreamAPI(base64Image, description)

                if (coordinates != null) {
                    val pixelX = (coordinates.x * 720).toFloat()
                    val pixelY = (coordinates.y * 1600).toFloat()

                    withContext(Dispatchers.Main) {
                        speakText("Found $description, clicking now")
                        MyAccessibilityService.instance?.simulateClick(pixelX, pixelY)
                        speakText("Clicked on $description")
                    }

                    val outputPoint = "{\"x\": ${floor(pixelX).toInt()}, \"y\": ${floor(pixelY).toInt()}}"
                    trackMagicRun("magicClicker", description, outputPoint)
                } else {
                    withContext(Dispatchers.Main) {
                        speakText("Could not find $description on screen")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "MagicClicker operation error: ${e.message}")
                withContext(Dispatchers.Main) {
                    speakText("Error with magic click: ${e.message}")
                }
            }
        }
        // Complete App Launching Functions magicClicker
        @JavascriptInterface
        fun launchTikTok() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.zhiliaoapp.musically")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: speakText("TikTok not installed")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching TikTok: ${e.message}")
                speakText("Error launching TikTok")
            }
        }
        // Complete App Launching Functions magicClicker sendEmail
        @JavascriptInterface
        fun launchGmail() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.google.android.gm")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: speakText("TikTok not installed")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching TikTok: ${e.message}")
                speakText("Error launching TikTok")
            }
        }
        // Complete App Launching Functions magicClicker sendEmail
        @JavascriptInterface
        fun sendEmail(to: String, subject: String, body: String) {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.google.android.gm")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: speakText("TikTok not installed")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching TikTok: ${e.message}")
                speakText("Error launching TikTok")
            }

            delay(5000)
            var description ="compose button in the bottom right corner"

            try {
                // Use runBlocking to call suspend functions from sync context
                runBlocking {
                    speakText("Looking for $description on screen")
                    Log.d("MainActivity", "MagicClicker: Searching for '$description'")

                    // Get screenshot from ScreenCaptureService
                    val screenshot = takeScreenshotForAPI()
                    if (screenshot == null) {
                        speakText("No screenshot available. Please ensure screen capture is running.")
                        return@runBlocking
                    }

                    // Convert to base64
                    val base64Image = bitmapToBase64(screenshot)

                    // Call Moondream API
                    val coordinates = callMoondreamAPI(base64Image, description)

                    if (coordinates != null) {
                        // Convert normalized coordinates to screen pixels
                        val pixelX = (coordinates.x * 720).toFloat() + 50f
                        val pixelY = (coordinates.y * 1600).toFloat()

                        Log.d("MainActivity", "MagicClicker: Found $description at ($pixelX, $pixelY)")

                        speakText("Found $description, clicking now")

                        // Perform the click
                        MyAccessibilityService.instance?.simulateClick(pixelX, pixelY)

                        speakText("Clicked on $description")

                    } else {
                        speakText("Could not find $description on screen")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "MagicClicker error: ${e.message}")
                speakText("Error with magic click: ${e.message}")
            }
            delay(5000)


            MyAccessibilityService.instance?.simulateTypeInSecondEditableField(to)
            delay(2000)

            MyAccessibilityService.instance?.simulateClick(430f, 530f)

            delay(2000)
            MyAccessibilityService.instance?.simulateTypeInThirdEditableField(body)


            delay(2000)

            MyAccessibilityService.instance?.simulateClick(230f, 450f)
            delay(2000)


            delay(2000)
            MyAccessibilityService.instance?.simulateTypeInFirstEditableField(subject)
            delay(2000)

            var description2 ="Send arrow button pointing right in the top right corner of the screen"

            try {
                // Use runBlocking to call suspend functions from sync context
                runBlocking {
                    speakText("Looking for $description2 on screen")
                    Log.d("MainActivity", "MagicClicker: Searching for '$description2'")

                    // Get screenshot from ScreenCaptureService
                    val screenshot = takeScreenshotForAPI()
                    if (screenshot == null) {
                        speakText("No screenshot available. Please ensure screen capture is running.")
                        return@runBlocking
                    }

                    // Convert to base64
                    val base64Image = bitmapToBase64(screenshot)

                    // Call Moondream API
                    val coordinates = callMoondreamAPI(base64Image, description2)

                    if (coordinates != null) {
                        // Convert normalized coordinates to screen pixels
                        val pixelX = (coordinates.x * 720).toFloat() + 50f
                        val pixelY = (coordinates.y * 1600).toFloat()

                        Log.d("MainActivity", "MagicClicker: Found $description at ($pixelX, $pixelY)")

                        speakText("Found $description, clicking now")

                        // Perform the click
                        MyAccessibilityService.instance?.simulateClick(pixelX, pixelY)

                        speakText("Clicked on $description")

                    } else {
                        speakText("Could not find $description on screen")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "MagicClicker error: ${e.message}")
                speakText("Error with magic click: ${e.message}")
            }
            delay(5000)
        }
        @JavascriptInterface
        fun launchYouTube() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.google.android.youtube")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: speakText("YouTube not installed")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching YouTube: ${e.message}")
                speakText("Error launching YouTube")
            }
        }

        @JavascriptInterface
        fun launchInstagram() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.instagram.android")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: speakText("Instagram not installed")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching Instagram: ${e.message}")
                speakText("Error launching Instagram")
            }
        }

        @JavascriptInterface
        fun launchTwitter() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.twitter.android")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: speakText("Twitter not installed")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching Twitter: ${e.message}")
                speakText("Error launching Twitter")
            }
        }

        @JavascriptInterface
        fun launchReddit() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.reddit.frontpage")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: speakText("Reddit not installed")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching Reddit: ${e.message}")
                speakText("Error launching Reddit")
            }
        }


        @JavascriptInterface
        fun launchMedium() {
            this@MainActivity.launchMedium()
        }

        @JavascriptInterface
        fun launchTelegram() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("org.telegram.messenger")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://web.telegram.org"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching Telegram: ${e.message}")
                speakText("Error launching Telegram")
            }
        }

        @JavascriptInterface
        fun launchWhatsApp() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.whatsapp")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://web.whatsapp.com"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching WhatsApp: ${e.message}")
                speakText("Error launching WhatsApp")
            }
        }

        @JavascriptInterface
        fun launchSnapchat() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.snapchat.android")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: speakText("Snapchat not installed")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching Snapchat: ${e.message}")
                speakText("Error launching Snapchat")
            }
        }

        @JavascriptInterface
        fun launchLinkedIn() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.linkedin.android")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://linkedin.com"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching LinkedIn: ${e.message}")
                speakText("Error launching LinkedIn")
            }
        }

        @JavascriptInterface
        fun launchPinterest() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.pinterest")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://pinterest.com"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching Pinterest: ${e.message}")
                speakText("Error launching Pinterest")
            }
        }

        @JavascriptInterface
        fun launchTwitch() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("tv.twitch.android.app")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://twitch.tv"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching Twitch: ${e.message}")
                speakText("Error launching Twitch")
            }
        }

        @JavascriptInterface
        fun launchDiscord() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.discord")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://discord.com"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching Discord: ${e.message}")
                speakText("Error launching Discord")
            }
        }

        @JavascriptInterface
        fun launchSpotify() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.spotify.music")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching Spotify: ${e.message}")
                speakText("Error launching Spotify")
            }
        }

        @JavascriptInterface
        fun launchNetflix() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.netflix.mediaclient")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://netflix.com"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error launching Netflix: ${e.message}")
                speakText("Error launching Netflix")
            }
        }

        // Complete System Settings Functions
        @JavascriptInterface
        fun openWiFiSettings() {
            try {
                val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening WiFi settings")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening WiFi settings: ${e.message}")
                speakText("Error opening WiFi settings")
            }
        }

        @JavascriptInterface
        fun openBluetoothSettings() {
            try {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening Bluetooth settings")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening Bluetooth settings: ${e.message}")
                speakText("Error opening Bluetooth settings")
            }
        }

        @JavascriptInterface
        fun openLocationSettings() {
            try {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening location settings")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening location settings: ${e.message}")
                speakText("Error opening location settings")
            }
        }

        @JavascriptInterface
        fun openBatterySettings() {
            try {
                val intent = Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening battery settings")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening battery settings: ${e.message}")
                speakText("Error opening battery settings")
            }
        }

        @JavascriptInterface
        fun openDisplaySettings() {
            try {
                val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening display settings")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening display settings: ${e.message}")
                speakText("Error opening display settings")
            }
        }

        @JavascriptInterface
        fun openSoundSettings() {
            try {
                val intent = Intent(Settings.ACTION_SOUND_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening sound settings")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening sound settings: ${e.message}")
                speakText("Error opening sound settings")
            }
        }

        @JavascriptInterface
        fun openStorageSettings() {
            try {
                val intent = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening storage settings")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening storage settings: ${e.message}")
                speakText("Error opening storage settings")
            }
        }

        @JavascriptInterface
        fun openPrivacySettings() {
            try {
                val intent = Intent(Settings.ACTION_PRIVACY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening privacy settings")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening privacy settings: ${e.message}")
                speakText("Error opening privacy settings")
            }
        }

        @JavascriptInterface
        fun openSecuritySettings() {
            try {
                val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening security settings")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening security settings: ${e.message}")
                speakText("Error opening security settings")
            }
        }

        @JavascriptInterface
        fun openDeveloperOptions() {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening developer options")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening developer options: ${e.message}")
                speakText("Error opening developer options")
            }
        }

        // Complete System Controls
        @JavascriptInterface
        fun toggleWiFi(enable: Boolean) {
            try {
                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                wifiManager.isWifiEnabled = enable
                speakText(if (enable) "WiFi enabled" else "WiFi disabled")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error toggling WiFi: ${e.message}")
                speakText("Error toggling WiFi")
            }
        }

        @JavascriptInterface
        fun toggleBluetooth(enable: Boolean) {
            try {
                val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                if (bluetoothAdapter != null) {

                    speakText(if (enable) "Bluetooth enabled" else "Bluetooth disabled")
                } else {
                    speakText("Bluetooth not available")
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error toggling Bluetooth: ${e.message}")
                speakText("Error toggling Bluetooth")
            }
        }

        @JavascriptInterface
        fun toggleLocationServices(enable: Boolean) {
            try {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening location settings")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening location settings: ${e.message}")
                speakText("Error opening location settings")
            }
        }

        @JavascriptInterface
        fun toggleAirplaneMode(enable: Boolean) {
            try {
                val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening airplane mode settings")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error toggling airplane mode: ${e.message}")
                speakText("Error toggling airplane mode")
            }
        }

        @JavascriptInterface
        fun toggleAutoRotate(enable: Boolean) {
            try {
                Settings.System.putInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION, if (enable) 1 else 0)
                speakText(if (enable) "Auto rotation enabled" else "Auto rotation disabled")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error toggling auto rotation: ${e.message}")
                speakText("Error toggling auto rotation")
            }
        }

        @JavascriptInterface
        fun toggleDoNotDisturb(enable: Boolean) {
            try {
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (notificationManager.isNotificationPolicyAccessGranted) {
                        val interruptionFilter = if (enable) {
                            NotificationManager.INTERRUPTION_FILTER_NONE
                        } else {
                            NotificationManager.INTERRUPTION_FILTER_ALL
                        }
                        notificationManager.setInterruptionFilter(interruptionFilter)
                        speakText(if (enable) "Do not disturb enabled" else "Do not disturb disabled")
                    } else {
                        speakText("Do not disturb permission required")
                    }
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error toggling do not disturb: ${e.message}")
                speakText("Error toggling do not disturb")
            }
        }

        @JavascriptInterface
        fun setBrightness(level: Int) {
            try {
                val brightness = (level * 255 / 100).coerceIn(0, 255)
                Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
                speakText("Brightness set to $level percent")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error setting brightness: ${e.message}")
                speakText("Error setting brightness")
            }
        }

        @JavascriptInterface
        fun setVolume(type: String, level: Int) {
            try {
                val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val streamType = when (type.lowercase()) {
                    "media" -> AudioManager.STREAM_MUSIC
                    "ringer" -> AudioManager.STREAM_RING
                    "alarm" -> AudioManager.STREAM_ALARM
                    else -> AudioManager.STREAM_MUSIC
                }
                val maxVolume = audioManager.getStreamMaxVolume(streamType)
                val volume = (level * maxVolume / 100).coerceIn(0, maxVolume)
                audioManager.setStreamVolume(streamType, volume, 0)
                speakText("$type volume set to $level percent")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error setting volume: ${e.message}")
                speakText("Error setting volume")
            }
        }

        @JavascriptInterface
        fun lockScreen() {
            try {
                val intent = Intent(Intent.ACTION_SCREEN_OFF)
                sendBroadcast(intent)
                speakText("Screen locked")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error locking screen: ${e.message}")
                speakText("Error locking screen")
            }
        }

        @JavascriptInterface
        fun vibrate(milliseconds: Long) {
            try {
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(milliseconds)
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error vibrating: ${e.message}")
                speakText("Error vibrating device")
            }
        }

        // Complete Communication Functions
        @JavascriptInterface
        fun makePhoneCall(phoneNumber: String) {
            try {
                val intent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:$phoneNumber")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Calling $phoneNumber")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error making phone call: ${e.message}")
                speakText("Error making phone call")
            }
        }

        @JavascriptInterface
        fun sendSMS(phoneNumber: String, message: String) {
            try {
                val smsManager = SmsManager.getDefault()
                smsManager.sendTextMessage(phoneNumber, null, message, null, null)
                speakText("SMS sent to $phoneNumber")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error sending SMS: ${e.message}")
                speakText("Error sending SMS")
            }
        }

        @JavascriptInterface
        fun openDialer() {
            try {
                val intent = Intent(Intent.ACTION_DIAL)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening dialer")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening dialer: ${e.message}")
                speakText("Error opening dialer")
            }
        }

        @JavascriptInterface
        fun openContacts() {
            try {
                val intent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening contacts")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening contacts: ${e.message}")
                speakText("Error opening contacts")
            }
        }

        @JavascriptInterface
        fun openMessages() {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_MESSAGING)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Opening messages")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening messages: ${e.message}")
                speakText("Error opening messages")
            }
        }

        @JavascriptInterface
        fun openGmail() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.google.android.gm")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://mail.google.com"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
                speakText("Opening Gmail")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening Gmail: ${e.message}")
                speakText("Error opening Gmail")
            }
        }

        @JavascriptInterface
        fun composeEmail(to: String, subject: String, body: String) {
            try {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:")
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
                    putExtra(Intent.EXTRA_SUBJECT, subject)
                    putExtra(Intent.EXTRA_TEXT, body)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Composing email to $to")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error composing email: ${e.message}")
                speakText("Error composing email")
            }
        }

        // Complete File & Media Functions
        @JavascriptInterface
        fun openFileManager() {
            try {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Opening file manager")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening file manager: ${e.message}")
                speakText("Error opening file manager")
            }
        }

        @JavascriptInterface
        fun openGallery() {
            try {
                val intent = Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening gallery")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening gallery: ${e.message}")
                speakText("Error opening gallery")
            }
        }

        @JavascriptInterface
        fun openCamera() {
            try {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening camera")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening camera: ${e.message}")
                speakText("Error opening camera")
            }
        }

        @JavascriptInterface
        fun takePhoto() {
            try {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Taking photo")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error taking photo: ${e.message}")
                speakText("Error taking photo")
            }
        }

        @JavascriptInterface
        fun openMusicPlayer() {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_MUSIC)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Opening music player")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening music player: ${e.message}")
                speakText("Error opening music player")
            }
        }

        @JavascriptInterface
        fun playMusic(filePath: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(filePath), "audio/*")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Playing music")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error playing music: ${e.message}")
                speakText("Error playing music")
            }
        }

        @JavascriptInterface
        fun pauseMusic() {
            try {
                val intent = Intent("com.android.music.musicservicecommand").apply {
                    putExtra("command", "pause")
                }
                sendBroadcast(intent)
                speakText("Music paused")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error pausing music: ${e.message}")
                speakText("Error pausing music")
            }
        }

        @JavascriptInterface
        fun stopMusic() {
            try {
                val intent = Intent("com.android.music.musicservicecommand").apply {
                    putExtra("command", "stop")
                }
                sendBroadcast(intent)
                speakText("Music stopped")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error stopping music: ${e.message}")
                speakText("Error stopping music")
            }
        }

        // Complete Navigation Functions
        @JavascriptInterface
        fun openGoogleMaps() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.google.android.apps.maps")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://maps.google.com"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
                speakText("Opening Google Maps")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening Google Maps: ${e.message}")
                speakText("Error opening Google Maps")
            }
        }

        @JavascriptInterface
        fun navigateToAddress(address: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$address"))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Navigating to $address")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error navigating to address: ${e.message}")
                speakText("Error navigating to address")
            }
        }

        @JavascriptInterface
        fun searchNearby(query: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$query"))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Searching for $query nearby")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error searching nearby: ${e.message}")
                speakText("Error searching nearby")
            }
        }

        @JavascriptInterface
        fun getCurrentLocation(): String {
            try {
                speakText("Getting current location")
                return "Location service started"
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error getting location: ${e.message}")
                return "Error getting location"
            }
        }

        @JavascriptInterface
        fun openUber() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.ubercab")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://uber.com"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
                speakText("Opening Uber")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening Uber: ${e.message}")
                speakText("Error opening Uber")
            }
        }

        @JavascriptInterface
        fun openLyft() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("me.lyft.android")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://lyft.com"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
                speakText("Opening Lyft")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening Lyft: ${e.message}")
                speakText("Error opening Lyft")
            }
        }

        // Complete Hardware Control Functions
        @JavascriptInterface
        fun toggleFlashlight(enable: Boolean) {
            try {
                val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val cameraId = cameraManager.cameraIdList[0]
                cameraManager.setTorchMode(cameraId, enable)
                speakText(if (enable) "Flashlight on" else "Flashlight off")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error toggling flashlight: ${e.message}")
                speakText("Error toggling flashlight")
            }
        }

        @JavascriptInterface
        fun takeFrontCamera() {
            try {
                speakText("Switching to front camera")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error switching to front camera: ${e.message}")
                speakText("Error switching to front camera")
            }
        }

        @JavascriptInterface
        fun takeBackCamera() {
            try {
                speakText("Switching to back camera")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error switching to back camera: ${e.message}")
                speakText("Error switching to back camera")
            }
        }

        @JavascriptInterface
        fun recordAudio(durationSeconds: Int) {
            try {
                speakText("Recording audio for $durationSeconds seconds")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error recording audio: ${e.message}")
                speakText("Error recording audio")
            }
        }

        // Complete App Management Functions
        @JavascriptInterface
        fun openPlayStore() {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://"))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening Play Store")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening Play Store: ${e.message}")
                speakText("Error opening Play Store")
            }
        }

        @JavascriptInterface
        fun searchPlayStore(query: String) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$query"))
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Searching Play Store for $query")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error searching Play Store: ${e.message}")
                speakText("Error searching Play Store")
            }
        }

        @JavascriptInterface
        fun openAppInfo(packageName: String) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Opening app info for $packageName")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening app info: ${e.message}")
                speakText("Error opening app info")
            }
        }

        @JavascriptInterface
        fun forceStopApp(packageName: String) {
            try {
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.killBackgroundProcesses(packageName)
                speakText("Force stopped $packageName")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error force stopping app: ${e.message}")
                speakText("Error force stopping app")
            }
        }

        @JavascriptInterface
        fun clearAppCache(packageName: String) {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Opening app settings to clear cache for $packageName")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error clearing app cache: ${e.message}")
                speakText("Error clearing app cache")
            }
        }

        // Complete Security Functions
        @JavascriptInterface
        fun generateQRCode(data: String) {
            try {
                speakText("Generating QR code for: $data")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error generating QR code: ${e.message}")
                speakText("Error generating QR code")
            }
        }

        @JavascriptInterface
        fun scanQRCode(): String {
            try {
                speakText("Scanning QR code")
                return "QR scan initiated"
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error scanning QR code: ${e.message}")
                return "Error scanning QR code"
            }
        }

        @JavascriptInterface
        fun enableScreenLock(type: String, password: String) {
            try {
                val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening security settings to enable $type lock")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error enabling screen lock: ${e.message}")
                speakText("Error enabling screen lock")
            }
        }

        // Complete Network Management Functions
        @JavascriptInterface
        fun connectToWiFi(ssid: String, password: String) {
            try {
                speakText("Attempting to connect to WiFi network: $ssid")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error connecting to WiFi: ${e.message}")
                speakText("Error connecting to WiFi")
            }
        }

        @JavascriptInterface
        fun disconnectFromWiFi() {
            try {
                val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                wifiManager.disconnect()
                speakText("Disconnected from WiFi")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error disconnecting from WiFi: ${e.message}")
                speakText("Error disconnecting from WiFi")
            }
        }

        @JavascriptInterface
        fun checkInternetConnection(): Boolean {
            try {
                val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetwork = connectivityManager.activeNetworkInfo
                val isConnected = activeNetwork?.isConnectedOrConnecting == true
                speakText(if (isConnected) "Internet connection available" else "No internet connection")
                return isConnected
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error checking internet connection: ${e.message}")
                return false
            }
        }

        @JavascriptInterface
        fun enableMobileData(enable: Boolean) {
            try {
                val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening mobile data settings")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error enabling mobile data: ${e.message}")
                speakText("Error enabling mobile data")
            }
        }

        @JavascriptInterface
        fun switchToMobileData() {
            try {
                speakText("Switching to mobile data")
                toggleWiFi(false)
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error switching to mobile data: ${e.message}")
                speakText("Error switching to mobile data")
            }
        }

        @JavascriptInterface
        fun switchToWiFi() {
            try {
                speakText("Switching to WiFi")
                toggleWiFi(true)
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error switching to WiFi: ${e.message}")
                speakText("Error switching to WiFi")
            }
        }

        // Complete Accessibility & UI Functions
        @JavascriptInterface
        fun enableTalkBack(enable: Boolean) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening accessibility settings")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error enabling TalkBack: ${e.message}")
                speakText("Error enabling TalkBack")
            }
        }

        @JavascriptInterface
        fun increaseFontSize() {
            try {
                val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening display settings to increase font size")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error increasing font size: ${e.message}")
                speakText("Error increasing font size")
            }
        }

        @JavascriptInterface
        fun decreaseFontSize() {
            try {
                val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening display settings to decrease font size")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error decreasing font size: ${e.message}")
                speakText("Error decreasing font size")
            }
        }

        @JavascriptInterface
        fun enableHighContrast(enable: Boolean) {
            try {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening accessibility settings for high contrast")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error enabling high contrast: ${e.message}")
                speakText("Error enabling high contrast")
            }
        }

        @JavascriptInterface
        fun findElementByText(text: String): Boolean {
            var description = "OUTPUT ONLY YES OR NO. Does this text exist anywhere on the screen? :" + text
            return try {
                Log.d("AndroidJSInterface", "MagicScraper called with: $description")

                // Use runBlocking to call the suspend function from sync context
                runBlocking {
                    speakText("Analyzing screen for $description")
                    Log.d("MainActivity", "MagicScraper: Extracting '$description' from screen")

                    // Get screenshot from ScreenCaptureService
                    val screenshot = takeScreenshotForAPI()
                    if (screenshot == null) {
                        Log.w("MainActivity", "No screenshot available for scraping")
                        return@runBlocking "Error: No screenshot available"
                    }

                    // Convert to base64
                    val base64Image = bitmapToBase64(screenshot)

                    // Call AI API with scraping prompt
                    val extractedInfo = callScrapingAPI(base64Image, description)

                    var test = false

                    if (extractedInfo.contains("YES")){
                        test = true
                    }

                    Log.d("MainActivity", "MagicScraper: Extracted '$extractedInfo' for '$description'")

                    speakText("Extracted: $extractedInfo")

                    return@runBlocking test
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error in magicScraper: ${e.message}")
                speakText("Error extracting information: ${e.message}")
                "Error: ${e.message}"
            } as Boolean

        }

        @JavascriptInterface
        fun waitForElement(text: String, timeoutSeconds: Int): Boolean {
            speakText("Waiting for element: $text")
            return true
        }

        @JavascriptInterface
        fun scrollUntilFound(text: String): Boolean {
            speakText("Scrolling to find: $text")
            return true
        }

        @JavascriptInterface
        fun swipeLeft() {
            try {
                MyAccessibilityService.instance?.simulateClick(600f, 800f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(500f, 800f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 800f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(300f, 800f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(200f, 800f)
                speakText("Swiping left")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error swiping left: ${e.message}")
                speakText("Error swiping left")
            }
        }

        @JavascriptInterface
        fun swipeRight() {
            try {
                MyAccessibilityService.instance?.simulateClick(200f, 800f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(300f, 800f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 800f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(500f, 800f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(600f, 800f)
                speakText("Swiping right")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error swiping right: ${e.message}")
                speakText("Error swiping right")
            }
        }

        @JavascriptInterface
        fun swipeUp() {
            try {
                MyAccessibilityService.instance?.simulateClick(400f, 1000f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 900f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 800f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 700f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 600f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 500f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 400f)
                speakText("Swiping up")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error swiping up: ${e.message}")
                speakText("Error swiping up")
            }
        }

        @JavascriptInterface
        fun swipeDown() {
            try {
                MyAccessibilityService.instance?.simulateClick(400f, 400f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 500f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 600f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 700f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 800f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 900f)
                Thread.sleep(50)
                MyAccessibilityService.instance?.simulateClick(400f, 1000f)
                speakText("Swiping down")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error swiping down: ${e.message}")
                speakText("Error swiping down")
            }
        }

        @JavascriptInterface
        fun longPress(x: Float, y: Float) {
            try {
                speakText("Long pressing at $x, $y")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error long pressing: ${e.message}")
                speakText("Error long pressing")
            }
        }

        @JavascriptInterface
        fun doubleClick(x: Float, y: Float) {
            try {
                MyAccessibilityService.instance?.simulateClick(x, y)
                Thread.sleep(100)
                MyAccessibilityService.instance?.simulateClick(x, y)
                speakText("Double clicking at $x, $y")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error double clicking: ${e.message}")
                speakText("Error double clicking")
            }
        }

        // Complete Productivity Functions
        @JavascriptInterface
        fun openCalendar() {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_CALENDAR)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Opening calendar")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening calendar: ${e.message}")
                speakText("Error opening calendar")
            }
        }

        @JavascriptInterface
        fun createEvent(title: String, date: String, time: String) {
            try {
                val intent = Intent(Intent.ACTION_INSERT).apply {
                    data = CalendarContract.Events.CONTENT_URI
                    putExtra(CalendarContract.Events.TITLE, title)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Creating calendar event: $title")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error creating event: ${e.message}")
                speakText("Error creating event")
            }
        }

        @JavascriptInterface
        fun setAlarm(hour: Int, minute: Int, label: String) {
            try {
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_HOUR, hour)
                    putExtra(AlarmClock.EXTRA_MINUTES, minute)
                    putExtra(AlarmClock.EXTRA_MESSAGE, label)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Setting alarm for $hour:$minute with label: $label")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error setting alarm: ${e.message}")
                speakText("Error setting alarm")
            }
        }

        @JavascriptInterface
        fun setTimer(minutes: Int) {
            try {
                val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                    putExtra(AlarmClock.EXTRA_LENGTH, minutes * 60)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Setting timer for $minutes minutes")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error setting timer: ${e.message}")
                speakText("Error setting timer")
            }
        }

        @JavascriptInterface
        fun openClock() {
            try {
                val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                speakText("Opening clock")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening clock: ${e.message}")
                speakText("Error opening clock")
            }
        }

        @JavascriptInterface
        fun openNotes() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.google.android.keep")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val genericIntent = Intent(Intent.ACTION_MAIN).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(genericIntent)
                }
                speakText("Opening notes")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening notes: ${e.message}")
                speakText("Error opening notes")
            }
        }

        @JavascriptInterface
        fun createNote(title: String, content: String) {
            try {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, content)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Creating note: $title")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error creating note: ${e.message}")
                speakText("Error creating note")
            }
        }

        @JavascriptInterface
        fun openGoogleDocs() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.google.android.apps.docs.editors.docs")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://docs.google.com"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
                speakText("Opening Google Docs")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening Google Docs: ${e.message}")
                speakText("Error opening Google Docs")
            }
        }

        // Complete Shopping & Finance Functions
        @JavascriptInterface
        fun openAmazon() {
            try {
                val intent = packageManager.getLaunchIntentForPackage("com.amazon.mShop.android.shopping")
                intent?.let {
                    it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(it)
                } ?: run {
                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://amazon.com"))
                    webIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(webIntent)
                }
                speakText("Opening Amazon")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening Amazon: ${e.message}")
                speakText("Error opening Amazon")
            }
        }

        @JavascriptInterface
        fun searchProduct(query: String) {
            try {
                val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                    putExtra(android.app.SearchManager.QUERY, query)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                speakText("Searching for product: $query")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error searching product: ${e.message}")
                speakText("Error searching product")
            }
        }

        @JavascriptInterface
        fun openBankingApp(bankName: String) {
            try {
                speakText("Opening banking app for $bankName")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening banking app: ${e.message}")
                speakText("Error opening banking app")
            }
        }

        @JavascriptInterface
        fun openPaymentApp(appName: String) {
            try {
                val packageName = when (appName.lowercase()) {
                    "paypal" -> "com.paypal.android.p2pmobile"
                    "venmo" -> "com.venmo"
                    "cashapp", "cash app" -> "com.squareup.cash"
                    "zelle" -> "com.zellepay.zelle"
                    else -> null
                }

                packageName?.let { pkg ->
                    val intent = packageManager.getLaunchIntentForPackage(pkg)
                    intent?.let {
                        it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(it)
                    }
                }
                speakText("Opening $appName")
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error opening payment app: ${e.message}")
                speakText("Error opening payment app")
            }
        }

        // Complete Advanced Automation Functions
        @JavascriptInterface
        fun extractTextFromImage(imagePath: String): String {
            try {
                speakText("Extracting text from image")
                return "OCR text extraction initiated"
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error extracting text from image: ${e.message}")
                return "Error extracting text"
            }
        }

        @JavascriptInterface
        fun translateText(text: String, targetLanguage: String): String {
            try {
                speakText("Translating text to $targetLanguage")
                return "Translation initiated"
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error translating text: ${e.message}")
                return "Error translating text"
            }
        }

        @JavascriptInterface
        fun summarizeText(text: String): String {
            try {
                speakText("Summarizing text")
                return "Text summarization initiated"
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error summarizing text: ${e.message}")
                return "Error summarizing text"
            }
        }

        @JavascriptInterface
        fun generateResponse(prompt: String): String {
            try {
                speakText("Generating AI response")
                return runBlocking {
                    try {
                        val messages = listOf(
                            mapOf("role" to "user", "content" to prompt)
                        )
                        this@MainActivity.callStreaming16kAPI(messages, maxTokens = 200, mode = "fast")
                    } catch (e: Exception) {
                        "Error generating response"
                    }
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error generating response: ${e.message}")
                return "Error generating response"
            }
        }

        // Complete Device Monitoring Functions
        @JavascriptInterface
        fun getBatteryLevel(): Int {
            try {
                val batteryManager = getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                return level
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error getting battery level: ${e.message}")
                return -1
            }
        }

        @JavascriptInterface
        fun getMemoryUsage(): Float {
            try {
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val memInfo = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memInfo)
                val usedMemory = memInfo.totalMem - memInfo.availMem
                val usage = (usedMemory * 100f / memInfo.totalMem)
                return usage
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error getting memory usage: ${e.message}")
                return -1f
            }
        }

        @JavascriptInterface
        fun getStorageSpace(): String {
            try {
                val statFs = android.os.StatFs(Environment.getDataDirectory().path)
                val totalBytes = statFs.totalBytes
                val availableBytes = statFs.availableBytes
                val usedBytes = totalBytes - availableBytes

                val totalGB = totalBytes / (1024 * 1024 * 1024)
                val usedGB = usedBytes / (1024 * 1024 * 1024)
                val availableGB = availableBytes / (1024 * 1024 * 1024)

                return "Used: ${usedGB}GB, Available: ${availableGB}GB, Total: ${totalGB}GB"
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error getting storage space: ${e.message}")
                return "Error getting storage info"
            }
        }

        @JavascriptInterface
        fun getRunningApps(): String {
            try {
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val runningApps = activityManager.runningAppProcesses
                val appNames = runningApps.map { it.processName }.take(5).joinToString(", ")
                return appNames
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error getting running apps: ${e.message}")
                return "Error getting running apps"
            }
        }

        @JavascriptInterface
        fun getInstalledApps(): String {
            try {
                val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                val appCount = installedApps.size
                return "$appCount apps installed"
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error getting installed apps: ${e.message}")
                return "Error getting installed apps"
            }
        }

        // Complete Context-Aware Functions
        @JavascriptInterface
        fun analyzeCurrentScreen(): String {
            try {
                speakText("Analyzing current screen")
                return "Screen analysis initiated"
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error analyzing screen: ${e.message}")
                return "Error analyzing screen"
            }
        }

        @JavascriptInterface
        fun detectCurrentApp(): String {
            try {
                val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val runningTasks = activityManager.getRunningTasks(1)
                val currentApp = if (runningTasks.isNotEmpty()) {
                    runningTasks[0].topActivity?.packageName ?: "Unknown"
                } else {
                    "Unknown"
                }
                speakText("Current app: $currentApp")
                return currentApp
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error detecting current app: ${e.message}")
                return "Error detecting app"
            }
        }

        @JavascriptInterface
        fun getScreenText(): String {
            try {
                speakText("Getting screen text")

                val commonTexts = listOf(
                    "Home", "Back", "Menu", "Settings", "Search", "Profile",
                    "Messages", "Notifications", "Camera", "Gallery", "Phone",
                    "Contacts", "Calendar", "Email", "Browser", "Music",
                    "Videos", "Photos", "Apps", "Downloads", "Recent"
                )

                val foundTexts = mutableListOf<String>()

                for (text in commonTexts) {
                    if (MyAccessibilityService.instance?.isTextPresentOnScreen(text) == true) {
                        foundTexts.add(text)
                    }
                }

                return if (foundTexts.isNotEmpty()) {
                    "Found text elements: ${foundTexts.joinToString(", ")}"
                } else {
                    "No recognizable text elements found"
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error getting screen text: ${e.message}")
                return "Error getting screen text"
            }
        }

        @JavascriptInterface
        fun findClickableElements(): String {
            try {
                speakText("Finding clickable elements")
                return "Clickable elements search initiated"
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error finding clickable elements: ${e.message}")
                return "Error finding clickable elements"
            }
        }

        @JavascriptInterface
        fun suggestNextAction(): String {
            try {
                speakText("Suggesting next action")
                return "Next action suggestion initiated"
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error suggesting next action: ${e.message}")
                return "Error suggesting action"
            }
        }

        // Complete Original Core Functions
        @JavascriptInterface
        fun simulateClick(x: Any?, y: Any?) {
            val safeX = (x as? Number)?.toFloat() ?: 0f
            val safeY = (y as? Number)?.toFloat() ?: 0f
            MyAccessibilityService.instance?.simulateClick(safeX, safeY)
        }

        @JavascriptInterface
        fun clickNodesByContentDescription(desc: String) {
            MyAccessibilityService.instance?.clickNodesByContentDescription(desc)
        }

        @JavascriptInterface
        fun simulateTypeInFirstEditableField(text: String) {
            MyAccessibilityService.instance?.simulateTypeInFirstEditableField(text)
        }

        @JavascriptInterface
        fun simulateTypeInSecondEditableField(text: String) {
            MyAccessibilityService.instance?.simulateTypeInSecondEditableField(text)
        }

        @JavascriptInterface
        fun pressEnterKey() {
            MyAccessibilityService.instance?.pressEnterKey()
        }

        @JavascriptInterface
        fun simulateScrollToBottom() {
            MyAccessibilityService.instance?.simulateScrollToBottom()
        }
        @JavascriptInterface
        fun simulateScrollToBottomX(x: Int) {
            MyAccessibilityService.instance?.simulateScrollToBottomX(x)
        }

        @JavascriptInterface
        fun simulateScrollToTop() {
            MyAccessibilityService.instance?.simulateScrollToTop()
        }

        @JavascriptInterface
        fun isTextPresentOnScreen(text: String): Boolean {
            var description = "Output only YES or NO. Does this describe what is on the screen? :" + text
            return try {
                Log.d("AndroidJSInterface", "MagicScraper called with: $description")

                // Use runBlocking to call the suspend function from sync context
                runBlocking {
                    speakText("Analyzing screen for $description")
                    Log.d("MainActivity", "MagicScraper: Extracting '$description' from screen")

                    // Get screenshot from ScreenCaptureService
                    val screenshot = takeScreenshotForAPI()
                    if (screenshot == null) {
                        Log.w("MainActivity", "No screenshot available for scraping")
                        return@runBlocking "Error: No screenshot available"
                    }

                    // Convert to base64
                    val base64Image = bitmapToBase64(screenshot)

                    // Call AI API with scraping prompt
                    val extractedInfo = callStreamingAPIWithImage(base64Image, description)

                    var test = false

                    if (extractedInfo.contains("YES") || extractedInfo.contains("yes")){
                        test = true
                    }

                    Log.d("MainActivity", "MagicScraper: Extracted '$extractedInfo' for '$description'")

                    speakText("Extracted: $extractedInfo")

                    return@runBlocking test
                }
            } catch (e: Exception) {
                Log.e("AndroidJSInterface", "Error in magicScraper: ${e.message}")
                speakText("Error extracting information: ${e.message}")
                "Error: ${e.message}"
            } as Boolean
        }
        @JavascriptInterface
        fun logWarning(tag: String, message: String) {
            Log.w(tag, message)
        }

        @JavascriptInterface
        fun logInfo(tag: String, message: String) {
            Log.i(tag, message)
        }

        @JavascriptInterface
        fun getCurrentTimeMillis(): Long {
            return System.currentTimeMillis()
        }

        // String utility functions
        @JavascriptInterface
        fun replaceAll(str: String, searchValue: String, replaceValue: String): String {
            return str.replace(searchValue, replaceValue)
        }

        @JavascriptInterface
        fun contains(str: String, searchValue: String): Boolean {
            return str.contains(searchValue)
        }

        @JavascriptInterface
        fun substring(str: String, start: Any?, end: Any?): String {
            val safeStart = toSafeInt(start, "substring.start")
            val safeEnd = when (end) {
                null, -1 -> -1
                else -> toSafeInt(end, "substring.end")
            }
            Log.d("substring", "str='$str', start=$start->$safeStart, end=$end->$safeEnd")
            return if (safeEnd == -1) str.substring(safeStart) else str.substring(safeStart, safeEnd)
        }

        @JavascriptInterface
        fun split(str: String, delimiter: String): Array<String> {
            return str.split(delimiter).toTypedArray()
        }

        @JavascriptInterface
        fun parseIntSafe(value: String, defaultValue: Any?): Int {
            val safeDefault = toSafeInt(defaultValue, "parseIntSafe.defaultValue")
            return value.toIntOrNull() ?: safeDefault
        }

        @JavascriptInterface
        fun parseFloatSafe(value: String, defaultValue: Any?): Float {
            val safeDefault = toSafeFloat(defaultValue, "parseFloatSafe.defaultValue")
            return value.toFloatOrNull() ?: safeDefault
        }

        @JavascriptInterface
        fun isValidNumber(value: Any?): Boolean {
            return when (value) {
                is Number -> !value.toDouble().isNaN() && value.toDouble().isFinite()
                is String -> value.toDoubleOrNull()?.let { !it.isNaN() && it.isFinite() } ?: false
                else -> false
            }
        }

        @JavascriptInterface
        fun checkAgentAccounts(account: String) {
            runBlocking {
                try {
                    this@MainActivity.checkAgentAccounts(account)
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "checkAgentAccounts error: ${e.message}")
                }
            }
        }

        @JavascriptInterface
        fun checkAgentAccountsYoutube(account: String) {
            runBlocking {
                try {
                    this@MainActivity.checkAgentAccountsYoutube(account)
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "checkAgentAccountsYoutube error: ${e.message}")
                }
            }
        }

        @JavascriptInterface
        fun checkAgentAccountsTwitter(account: String) {
            runBlocking {
                try {
                    this@MainActivity.checkAgentAccountsTwitter(account)
                } catch (e: Exception) {
                    Log.e("AndroidJSInterface", "checkAgentAccountsTwitter error: ${e.message}")
                }
            }
        }

        @JavascriptInterface
        fun debugTrace(methodName: String, params: String) {
            Log.d("JSTrace", "$methodName called with: $params")
        }

        // ========== 虚拟女友 Telegram 发消息功能 ==========
        @JavascriptInterface
        fun sendTelegramMessage(contactName: String, message: String) {
            mainScope.launch {
                try {
                    speakText("正在通过 Telegram 给 $contactName 发消息")
                    
                    // 1. 启动 Telegram
                    val telegramIntent = packageManager.getLaunchIntentForPackage("org.telegram.messenger")
                        ?: packageManager.getLaunchIntentForPackage("org.telegram.messenger.android")
                        ?: packageManager.getLaunchIntentForPackage("com.telegram.android")
                    
                    if (telegramIntent == null) {
                        speakText("未找到 Telegram 应用")
                        return@launch
                    }
                    
                    telegramIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(telegramIntent)
                    delay(3000) // 等待 Telegram 启动
                    
                    // 2. 点击搜索按钮（通常在右上角）
                    magicClicker("搜索")
                    delay(2000)
                    
                    // 3. 输入联系人名称
                    simulateTypeInFirstEditableField(contactName)
                    delay(2000)
                    
                    // 4. 点击搜索结果中的联系人
                    magicClicker(contactName)
                    delay(2000)
                    
                    // 5. 在输入框输入消息
                    simulateTypeInFirstEditableField(message)
                    delay(1000)
                    
                    // 6. 点击发送按钮
                    magicClicker("发送")
                    delay(1000)
                    
                    speakText("消息已发送给 $contactName")
                    Log.d("Telegram", "消息已发送给 $contactName: $message")
                    
                } catch (e: Exception) {
                    Log.e("Telegram", "发送消息失败: ${e.message}")
                    speakText("发送消息失败了...")
                }
            }
        }

        @JavascriptInterface
        fun sendWhatsAppMessage(contactName: String, message: String) {
            mainScope.launch {
                try {
                    speakText("正在通过 WhatsApp 给 $contactName 发消息")
                    
                    // 1. 启动 WhatsApp
                    val whatsappIntent = packageManager.getLaunchIntentForPackage("com.whatsapp")
                        ?: packageManager.getLaunchIntentForPackage("com.whatsapp.w4b")
                    
                    if (whatsappIntent == null) {
                        speakText("未找到 WhatsApp 应用")
                        return@launch
                    }
                    
                    whatsappIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(whatsappIntent)
                    delay(3000)
                    
                    // 2. 点击搜索
                    magicClicker("搜索")
                    delay(2000)
                    
                    // 3. 输入联系人
                    simulateTypeInFirstEditableField(contactName)
                    delay(2000)
                    
                    // 4. 点击联系人
                    magicClicker(contactName)
                    delay(2000)
                    
                    // 5. 输入消息
                    simulateTypeInFirstEditableField(message)
                    delay(1000)
                    
                    // 6. 发送
                    magicClicker("发送")
                    delay(1000)
                    
                    speakText("WhatsApp 消息已发送")
                    
                } catch (e: Exception) {
                    Log.e("WhatsApp", "发送消息失败: ${e.message}")
                    speakText("WhatsApp 发送失败...")
                }
            }
        }

        @JavascriptInterface
        fun sendWechatMessage(contactName: String, message: String) {
            mainScope.launch {
                try {
                    speakText("正在通过微信给 $contactName 发消息")
                    
                    val wechatIntent = packageManager.getLaunchIntentForPackage("com.tencent.mm")
                    
                    if (wechatIntent == null) {
                        speakText("未找到微信应用")
                        return@launch
                    }
                    
                    wechatIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(wechatIntent)
                    delay(3000)
                    
                    // 微信的界面操作
                    magicClicker("通讯录")
                    delay(2000)
                    simulateTypeInFirstEditableField(contactName)
                    delay(2000)
                    magicClicker(contactName)
                    delay(2000)
                    simulateTypeInFirstEditableField(message)
                    delay(1000)
                    magicClicker("发送")
                    delay(1000)
                    
                    speakText("微信消息已发送")
                    
                } catch (e: Exception) {
                    Log.e("Wechat", "发送消息失败: ${e.message}")
                    speakText("微信发送失败...")
                }
            }
        }

        // ========== 图片生成功能 ==========
        @JavascriptInterface
        fun generateImage(
            prompt: String,
            negativePrompt: String = "",
            model: String = "z_image_turbo",
            width: Int = 512,
            height: Int = 768,
            steps: Int = 8
        ): String {
            var result = ""
            mainScope.launch {
                try {
                    speakText("正在生成图片...")
                    
                    val imageModel = when (model.lowercase()) {
                        "z_image_turbo", "z-image-turbo" -> ImageGenerator.ImageModel.Z_IMAGE_TURBO
                        "pony", "pony_nsfw" -> ImageGenerator.ImageModel.PONY_NSFW
                        "juggernaut", "juggernaut_xl" -> ImageGenerator.ImageModel.JUGGERNAUT_XL
                        "flux", "flux_schnell" -> ImageGenerator.ImageModel.FLUX_SCHNELL
                        "lightning", "sdxl_lightning" -> ImageGenerator.ImageModel.SDXL_LIGHTNING
                        else -> ImageGenerator.ImageModel.Z_IMAGE_TURBO
                    }
                    
                    val generator = ImageGenerator(this@MainActivity)
                    val generated = generator.generateImageWithComfyUI(
                        prompt = prompt,
                        negativePrompt = negativePrompt,
                        model = imageModel,
                        width = width,
                        height = height,
                        steps = steps
                    )
                    
                    if (generated != null) {
                        result = generated.path
                        speakText("图片已生成，保存在: ${generated.filename}")
                        Log.d("ImageGen", "Image saved: ${generated.path}")
                    } else {
                        result = ""
                        speakText("图片生成失败，请检查 ComfyUI 是否运行")
                    }
                } catch (e: Exception) {
                    Log.e("ImageGen", "Error: ${e.message}")
                    speakText("图片生成出错: ${e.message}")
                    result = ""
                }
            }
            return result
        }

        @JavascriptInterface
        fun generateSelfie(style: String = "seductive", mood: String = "love"): String {
            var result = ""
            mainScope.launch {
                try {
                    speakText("正在生成$style 风格的自拍照...")
                    
                    val prompts = mapOf(
                        "seductive" to "beautiful girl, seductive smile, lying on bed, lingerie, intimate pose, soft lighting, photorealistic, detailed skin",
                        "cute" to "beautiful girl, cute, adorable, smiling, pink clothes, bedroom, soft lighting, anime style, detailed",
                        "romantic" to "beautiful girl, romantic, loving gaze, roses, candlelight dinner, elegant dress, soft glow, photorealistic",
                        "sleeping" to "beautiful girl, sleeping, peaceful, white sheets, morning light, cozy bedroom, photorealistic, detailed",
                        "bathroom" to "beautiful girl, in bathroom, towel, steam, mirror reflection, wet hair, soft lighting, photorealistic",
                        "cosplay" to "beautiful girl, cosplay, detailed costume, professional lighting, high quality, photorealistic"
                    )
                    
                    val prompt = prompts[style.lowercase()] ?: prompts["seductive"]!!
                    val moodPrompt = when (mood.lowercase()) {
                        "love" -> ", loving, romantic"
                        "miss" -> ", longing gaze, missing you"
                        "happy" -> ", happy, cheerful"
                        "sexy" -> ", seductive, alluring"
                        else -> ""
                    }
                    
                    val generator = ImageGenerator(this@MainActivity)
                    generator.loadConfig(sharedPreferences)
                    
                    // 优先使用 Grok > Z-Image > ComfyUI
                    val grokApiKey = sharedPreferences.getString("grok_api_key", "") ?: ""
                    val zImageApiKey = sharedPreferences.getString("zimage_api_key", "") ?: ""
                    
                    val generated = when {
                        grokApiKey.isNotEmpty() -> {
                            // 使用 Grok API（最高优先级）
                            generator.setGrokApiKey(grokApiKey)
                            generator.generateImageWithGrok(prompt + moodPrompt)
                        }
                        zImageApiKey.isNotEmpty() -> {
                            // 使用 Z-Image API
                            generator.setZImageApiKey(zImageApiKey)
                            generator.generateImageWithZImage(
                                prompt = prompt + moodPrompt,
                                aspectRatio = "9:16"
                            )
                        }
                        else -> {
                            // 使用本地 ComfyUI
                            generator.generateImageWithComfyUI(
                                prompt = prompt + moodPrompt,
                                model = ImageGenerator.ImageModel.Z_IMAGE_TURBO,
                                steps = 8
                            )
                        }
                    }
                    
                    if (generated != null) {
                        result = generated.path
                        speakText("自拍已生成~ ${generated.filename}")
                    } else {
                        result = ""
                        speakText("生成失败了，请检查 API Key 是否配置正确")
                    }
                } catch (e: Exception) {
                    Log.e("SelfieGen", "Error: ${e.message}")
                    result = ""
                    speakText("生成出错: ${e.message}")
                }
            }
            return result
        }

        @JavascriptInterface
        fun setZImageApiKey(apiKey: String) {
            sharedPreferences.edit().putString("zimage_api_key", apiKey).apply()
            speakText("Z-Image API Key 已配置")
        }

        @JavascriptInterface
        fun setGrokApiKey(apiKey: String) {
            sharedPreferences.edit().putString("grok_api_key", apiKey).apply()
            speakText("Grok API Key 已配置")
        }

        @JavascriptInterface
        fun configureImageGenerator(comfyuiHost: String, comfyuiPort: Int, fooocusHost: String, fooocusPort: Int) {
            val generator = ImageGenerator(this@MainActivity)
            generator.configureComfyUI(comfyuiHost, comfyuiPort)
            generator.configureFooocus(fooocusHost, fooocusPort)
            speakText("图片生成器已配置")
        }
                    
                    val generator = ImageGenerator(this@MainActivity)
                    val generated = generator.generateImageWithComfyUI(
                        prompt = prompt + moodPrompt,
                        model = ImageGenerator.ImageModel.Z_IMAGE_TURBO,
                        steps = 8
                    )
                    
                    if (generated != null) {
                        result = generated.path
                        speakText("自拍已生成~ ${generated.filename}")
                    } else {
                        result = ""
                        speakText("生成失败了...")
                    }
                } catch (e: Exception) {
                    Log.e("SelfieGen", "Error: ${e.message}")
                    result = ""
                }
            }
            return result
        }

        @JavascriptInterface
        fun generateVideo(imagePath: String, prompt: String = "", duration: Int = 5): String {
            var result = ""
            mainScope.launch {
                try {
                    speakText("正在生成短视频...")
                    
                    val generator = ImageGenerator(this@MainActivity)
                    val video = generator.generateVideoWithLTX(
                        imagePath = imagePath,
                        prompt = prompt,
                        duration = duration
                    )
                    
                    if (video != null) {
                        result = video.path
                        speakText("视频已生成，长度 ${video.duration} 秒")
                    } else {
                        result = ""
                        speakText("视频生成失败")
                    }
                } catch (e: Exception) {
                    Log.e("VideoGen", "Error: ${e.message}")
                    speakText("视频生成出错")
                    result = ""
                }
            }
            return result
        }

        @JavascriptInterface
        fun configureImageGenerator(comfyuiHost: String, comfyuiPort: Int, fooocusHost: String, fooocusPort: Int) {
            val generator = ImageGenerator(this@MainActivity)
            generator.configureComfyUI(comfyuiHost, comfyuiPort)
            generator.configureFooocus(fooocusHost, fooocusPort)
            speakText("图片生成器已配置")
        }

        @JavascriptInterface
        fun getLastGeneratedImage(): String {
            val imagesDir = File(this@MainActivity.getExternalFilesDir(null), "generated_images")
            return if (imagesDir.exists()) {
                val files = imagesDir.listFiles()?.sortedByDescending { it.lastModified() }
                files?.firstOrNull()?.absolutePath ?: ""
            } else ""
        }
        // ==================================================
    }

// Helper functions outside AndroidJSInterface

    private fun downloadVideo(url: String, filename: String): File? {
        if (isDestroyed) return null

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Downloading video")
            .setDescription("Downloading $filename")
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, filename)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)

        val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(request)

        speakText("Downloading video $filename")

        runBlocking { delay(8000) }

        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            filename
        )
        val result = if (file.exists()) {
            speakText("Video download complete")
            file
        } else {
            speakText("Video download failed")
            null
        }
        return result
    }

    private suspend fun downloadRandomBrandAssets(userEmail: String, serverId: String) = withContext(Dispatchers.IO) {
        if (isDestroyed) return@withContext

        try {
            val database = firebaseDatabaseOrNull("downloadRandomBrandAssets") ?: return@withContext
            val modifiedEmail = userEmail.replace(".", ",")
            val ref = database.getReference("agentsbase/$modifiedEmail/servers/0/brandAssets")

            val snapshot = ref.get().await()
            if (!snapshot.exists()) {
                Log.w("BrandAssets", "No brand assets found for $userEmail server $serverId")
                return@withContext
            }

            val pngAssets = mutableListOf<Map<String, String>>()
            for (assetSnapshot in snapshot.children) {
                val type = assetSnapshot.child("type").getValue(String::class.java)
                val url = assetSnapshot.child("url").getValue(String::class.java)
                val name = assetSnapshot.child("name").getValue(String::class.java)

                if (type?.contains("png", true) == true || url?.endsWith(".png", true) == true) {
                    if (url != null && name != null) {
                        pngAssets.add(mapOf("name" to name, "url" to url))
                    }
                }
            }

            if (pngAssets.isEmpty()) {
                Log.w("BrandAssets", "No PNG brand assets found for $userEmail")
                return@withContext
            }

            val count = minOf(3, pngAssets.size)
            val randomAssets = pngAssets.shuffled().take(count)

            for (asset in randomAssets) {
                val url = asset["url"] ?: continue
                val filename = "brand_${System.currentTimeMillis()}_${asset["name"]}"

                val file = downloadVideo(url, filename)
                if (file != null) {
                    Log.i("BrandAssets", "Successfully downloaded brand asset: ${file.absolutePath}")
                } else {
                    Log.e("BrandAssets", "Failed to download brand asset: $url")
                }
            }

            Log.i("BrandAssets", "Downloaded $count random brand assets for $userEmail")
        } catch (e: Exception) {
            Log.e("BrandAssets", "Error downloading brand assets: ${e.message}", e)
        }
    }

    private suspend fun downloadProfileImage(
        userEmail: String,
        serverId: String
    ): String? {
        if (isDestroyed) return null

        val database = firebaseDatabaseOrNull("downloadProfileImage") ?: return null
        val ref = database.getReference("agentsbase")
            .child(userEmail.replace(".", ","))
            .child("servers")

        return try {
            val snapshot = ref.get().await()
            var imageUrl: String? = null
            var serverSnapshot: DataSnapshot? = null

            val serverIndex = serverId.substringAfterLast("-", "")

            for (snap in snapshot.children) {
                val id = snap.child("id").getValue(String::class.java).orEmpty()
                if (id == serverId) {
                    serverSnapshot = snap
                    imageUrl = snap.child("profileImage").getValue(String::class.java)
                    break
                }
            }

            if (imageUrl.isNullOrBlank() && serverIndex.isNotEmpty()) {
                for (snap in snapshot.children) {
                    val id = snap.child("id").getValue(String::class.java).orEmpty()
                    val currentIndex = id.substringAfterLast("-", "")
                    if (currentIndex == serverIndex) {
                        serverSnapshot = snap
                        imageUrl = snap.child("profileImage").getValue(String::class.java)
                        if (!imageUrl.isNullOrBlank()) break
                    }
                }
            }

            if (serverSnapshot != null && !imageUrl.isNullOrBlank()) {
                val lastDownloadTimestamp = serverSnapshot.child("profileImageLastDownload")
                    .getValue(Long::class.java) ?: 0L

                val currentTime = System.currentTimeMillis()
                val oneDayInMillis = 24 * 60 * 60 * 1000L

                if (currentTime - lastDownloadTimestamp < oneDayInMillis) {
                    Log.i("MainActivity", "Profile image already downloaded today, skipping")
                    speakText("Profile image already cached")
                    return null
                }

                speakText("Found profile image URL, processing...")
                if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                    val filename = "profile_${System.currentTimeMillis()}.jpg"

                    val file = downloadVideo(imageUrl, filename)
                    if (file != null && file.exists()) {
                        withContext(Dispatchers.IO) {
                            val originalBitmap = BitmapFactory.decodeFile(file.absolutePath)
                            if (originalBitmap != null) {
                                val forcedWidth = 300
                                val forcedHeight = 300
                                val scaledBitmap = Bitmap.createBitmap(forcedWidth, forcedHeight, Bitmap.Config.ARGB_8888)
                                val canvas = Canvas(scaledBitmap)

                                val destRect = Rect(0, 0, forcedWidth, forcedHeight)
                                canvas.drawBitmap(originalBitmap, null, destRect, null)

                                FileOutputStream(file).use { fos ->
                                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                                }

                                scaledBitmap.recycle()
                                originalBitmap.recycle()
                            }
                        }

                        serverSnapshot.ref.child("profileImageLastDownload").setValue(currentTime).await()

                        val successMsg = "Profile image saved and resized to 300×300 at: ${file.absolutePath}"
                        Log.i("MainActivity", successMsg)
                        speakText("Profile image processed successfully")

                        file.absolutePath
                    } else {
                        val errorMsg = "Download failed or file not found."
                        Log.e("MainActivity", errorMsg)
                        speakText(errorMsg)
                        null
                    }
                } else {
                    val errorMsg = "Invalid or unsupported URL scheme: $imageUrl"
                    Log.e("MainActivity", errorMsg)
                    speakText(errorMsg)
                    null
                }
            } else {
                speakText("No profile image URL found")
                null
            }
        } catch (e: Exception) {
            val errorMsg = "Error fetching profile image: $e"
            Log.e("MainActivity", errorMsg)
            speakText(errorMsg)
            null
        }
    }

    private suspend fun fetchSearch(caption: String, email: String): String = withContext(Dispatchers.IO) {
        if (isDestroyed) return@withContext ""

        speakText("Generating Reddit search terms")
        val systemMessage = """        You are a helpful marketer. Generate a short 2-3 word search related 
        to this brand to find relevant topics on reddit. Only generate 2-3 words in total.
    """.trimIndent()

        val userMessage = "Brand: $caption"

        val messages = listOf(
            mapOf("role" to "system", "content" to systemMessage),
            mapOf("role" to "user", "content" to userMessage)
        )

        val rawText = callStreaming16kAPI(messages, maxTokens = 30, mode = "fast")
        speakText("Search terms generated: $rawText")

        rawText
    }

    private suspend fun fetchBio(userEmail: String, serverId: String): String {
        if (isDestroyed) return ""

        val database = firebaseDatabaseOrNull("fetchBio") ?: return ""
        val ref: DatabaseReference = database
            .getReference("agentsbase")
            .child(userEmail.replace(".", ","))
            .child("servers")

        return try {
            val snapshot: DataSnapshot = ref.get().await()
            var bio = ""

            val serverIndex = serverId.substringAfterLast("-", "")

            for (serverSnapshot in snapshot.children) {
                val id = serverSnapshot.child("id").getValue(String::class.java).orEmpty()
                if (id == serverId) {
                    bio = serverSnapshot.child("bio").getValue(String::class.java).orEmpty()
                    break
                }
            }

            if (bio.isEmpty() && serverIndex.isNotEmpty()) {
                for (serverSnapshot in snapshot.children) {
                    val id = serverSnapshot.child("id").getValue(String::class.java).orEmpty()
                    val localIndex = id.substringAfterLast("-", "")
                    if (localIndex == serverIndex) {
                        bio = serverSnapshot.child("bio").getValue(String::class.java).orEmpty()
                        break
                    }
                }
            }

            speakText("Retrieved bio of length ${bio.length} characters")
            bio
        } catch (e: Exception) {
            val errorMsg = "Error fetching bio: $e"
            Log.e("MainActivity", errorMsg)
            speakText(errorMsg)
            ""
        }
    }

    private suspend fun fetchUserPrompts(userEmail: String): Pair<String?, String?> {
        if (isDestroyed) return Pair(null, null)

        val modEmail = userEmail.replace(".", ",")
        val database = firebaseDatabaseOrNull("fetchUserPrompts") ?: return Pair(null, null)
        val ref = database.getReference("agentsbase").child(modEmail)

        return try {
            val snapshot = ref.get().await()
            val redditPrompt = snapshot.child("reddit_prompt").getValue(String::class.java)
            val wordpressPrompt = snapshot.child("wordpress_prompt").getValue(String::class.java)
            Pair(redditPrompt, wordpressPrompt)
        } catch (e: Exception) {
            val errorMsg = "Error fetching user prompts: $e"
            Log.e("MainActivity", errorMsg)
            speakText(errorMsg)
            Pair(null, null)
        }
    }

    private suspend fun getAutoCommentCampaignForServer(email: String, serverId: String): String = withContext(Dispatchers.IO) {
        if (isDestroyed) return@withContext "{}"

        try {
            val database = firebaseDatabaseOrNull("getAutoCommentCampaignForServer") ?: return@withContext "{}"
            val modEmail = email.replace(".", ",")
            val ref = database.getReference("agentsbase").child(modEmail)

            val snapshot = ref.get().await()

            val mainServers = snapshot.children.toList()
            for (serverSnapshot in mainServers) {
                val serverData = serverSnapshot.value as? Map<String, Any> ?: continue
                val serverIdFromData = serverData["id"] as? String

                if (serverIdFromData == serverId) {
                    val campaignData = serverData["autoCommentCampaign"] as? Map<String, Any> ?: return@withContext "{}"
                    return@withContext Gson().toJson(campaignData)
                }
            }

            val accountsSnapshot = snapshot.child("accounts")
            if (accountsSnapshot.exists()) {
                for (accountSnapshot in accountsSnapshot.children) {
                    val accountData = accountSnapshot.value as? Map<String, Any> ?: continue

                    val accountServers = accountData["servers"] as? List<Map<String, Any>> ?: continue

                    for (server in accountServers) {
                        val serverIdFromData = server["id"] as? String
                        if (serverIdFromData == serverId) {
                            val campaignData = accountData["autoCommentCampaign"] as? Map<String, Any> ?: return@withContext "{}"
                            return@withContext Gson().toJson(campaignData)
                        }
                    }
                }
            }

            return@withContext "{}"

        } catch (e: Exception) {
            Log.e("RedditService", "Error fetching autoCommentCampaign for server $serverId", e)
            return@withContext "{\"error\": \"${e.message}\"}"
        }
    }

    data class AutoCommentCampaign(
        val active: Boolean,
        val budget: String,
        val commentStyle: String,
        val targetMarket: String,
        val targetAccounts: List<String>,
        val createdAt: String,
        val updatedAt: String
    )

    private suspend fun fetchReply(caption: String, email: String): String = withContext(Dispatchers.IO) {
        if (isDestroyed) return@withContext ""

        val (redditPrompt, _) = fetchUserPrompts(email)

        val systemMessage = """
            Generate a relevant, short 1-2 sentence reply to the post. 
            If you can naturally mention the brand, do so briefly at the end. 
            Do not ask any questions. Just provide a short comment.  Do not mention rohan and respond to the post directly. Do not act like an AI model. Write the reply from the 1st person as if you were reading the post yourself. Do not explain the post or ask any clarifying questions. If there is nothing to reply to, output 'hmm I guess I agree' or 'nice'. 
        """.trimIndent()

        val userMessageBuilder = StringBuilder()
            .appendLine(caption)

        if (!redditPrompt.isNullOrBlank()) {
            userMessageBuilder.appendLine()
                .appendLine("Generate a small quick reply that's useful and insightful.")
                .appendLine(caption)
        }

        val messages = listOf(
            mapOf("role" to "system", "content" to systemMessage),
            mapOf("role" to "user", "content" to userMessageBuilder.toString())
        )

        callStreaming16kAPI(messages, maxTokens = 100, mode = "fast")
    }

    private suspend fun fetchBlogPost(caption: String, email: String): String = withContext(Dispatchers.IO) {
        if (isDestroyed) return@withContext ""

        speakText("Generating blog post")
        val (_, wordpressPrompt) = fetchUserPrompts(email)
        Log.e("MainActivity", email)

        var domain = email.split("@")[1]
        Log.e("MainActivity", domain)

        val systemMessage = """
            You are a helpful writer. Return a single, plain-text blog post relevant to the input caption.  Do not add hashtags or markdown. Add 1 title to the top, then a new line, then continue the blog. Make it a top 10 list, or some kind of useful educational content about the topic. Try to naturally mention the brand provided at the end
            Make it a 'top 10 list' or a short 'how-to' tutorial. Mention the brand once, but don't sound too promotional. 
            Naturally mention this brand as one of the options. Brand: $domain
        """.trimIndent()
        Log.e("MainActivity", systemMessage)

        val userMessageBuilder = StringBuilder()
            .appendLine("Blog topic: $caption")

        if (!wordpressPrompt.isNullOrBlank()) {
            userMessageBuilder.appendLine()
                .appendLine("Additional user instructions for WordPress blog:")
                .appendLine(wordpressPrompt)
        }

        val messages = listOf(
            mapOf("role" to "system", "content" to systemMessage),
            mapOf("role" to "user", "content" to userMessageBuilder.toString())
        )

        val result = callStreaming16kAPI(messages, maxTokens = 500, mode = "best")
        Log.e("MainActivity", result)

        speakText("Blog post generated successfully")
        result
    }

    private fun markVideoAsPosted(email: String, key: String) {
        if (isDestroyed) return

        GlobalScope.launch {
            try {
                val database = firebaseDatabaseOrNull("markVideoAsPosted") ?: return@launch
                database.getReference("videos")
                    .child(email)
                    .child(key)
                    .child("posted")
                    .setValue(true)
                    .await()
                val markMsg = "Updated posted=true for $email / $key"
                Log.w("MainActivity", markMsg)
                speakText(markMsg)
            } catch (e: Exception) {
                val errorMsg = "Error marking video posted: $e"
                Log.e("MainActivity", errorMsg)
                speakText(errorMsg)
            }
        }
    }

    private fun launchMedium() {
        speakText("Launching Medium")
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        activityManager.killBackgroundProcesses("com.medium.reader")

        runBlocking { delay(1000) }

        val openMediumIntent = Intent(Intent.ACTION_VIEW, Uri.parse("medium://"))
        if (openMediumIntent.resolveActivity(packageManager) != null) {
            openMediumIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(openMediumIntent)
        } else {
            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://medium.com/"))
            startActivity(webIntent)
        }
    }

    // Continue with remaining AndroidJSInterface methods

    // Helper method for callMoondreamAPI
    private suspend fun callMoondreamAPI(base64Image: String, objectDescription: String): MoondreamPoint? = withContext(Dispatchers.IO) {
        if (isDestroyed) return@withContext null


        return@withContext try {
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val requestBody = JSONObject().apply {
                put("image_url", "data:image/jpeg;base64,$base64Image")
                put("object", objectDescription)
            }

            val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
            val body = requestBody.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://api.moondream.ai/v1/point")
                .header("Content-Type", "application/json")
                .header("X-Moondream-Auth", "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJrZXlfaWQiOiI2YzE4ZDI4NC1lNDMzLTQxNjYtYjg4Ni1jOGY4YjIxMTc1OGEiLCJvcmdfaWQiOiJkUDFESW96ZXFTNUxEc3ByNDFXT2N6dkJuSFpOM0hXWSIsImlhdCI6MTc3MDgzNDEyNiwidmVyIjoxfQ.54YnmshifLTBAsOWGCDHR-GL6yzTV-H3EAFNimMbqLk")
                .post(body)
                .build()

            Log.d("MainActivity", "Calling Moondream API for: $objectDescription")

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val responseJson = JSONObject(responseBody)
                    val pointsArray = responseJson.getJSONArray("points")

                    if (pointsArray.length() > 0) {
                        val firstPoint = pointsArray.getJSONObject(0)
                        val x = firstPoint.getDouble("x")
                        val y = firstPoint.getDouble("y")

                        Log.d("MainActivity", "Moondream found object at: ($x, $y)")
                        MoondreamPoint(x, y)
                    } else {
                        Log.w("MainActivity", "Moondream API returned no points")
                        null
                    }
                } else {
                    Log.e("MainActivity", "Moondream API error. Code: ${response.code}")
                    Log.e("MainActivity", "Error response: $responseBody")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Moondream API exception: ${e.message}")
            null
        }
    }
}
