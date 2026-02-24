package com.example.universal

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class ImageGenerator(private val context: Context) {
    
    companion object {
        // 本地默认地址
        private const val DEFAULT_COMFYUI_HOST = "127.0.0.1"
        private const val DEFAULT_COMFYUI_PORT = 8188
        
        // Fooocus 默认地址
        private const val DEFAULT_FOOOCUS_HOST = "127.0.0.1"
        private const val DEFAULT_FOOOCUS_PORT = 7865
        
        // 预定义的模型选择
        enum class ImageModel(val displayName: String, val modelFile: String) {
            Z_IMAGE_TURBO("Z-Image-Turbo (快速写实)", "z_image_turbo.safetensors"),
            PONY_NSFW("Pony Diffusion V6 XL NSFW", "pony diffusion v6 xl.safetensors"),
            JUGGERNAUT_XL("Juggernaut XL (写实)", "juggernaut_xl.safetensors"),
            FLUX_SCHNELL("Flux.1 Schnell (高质量)", "flux1-dev.safetensors"),
            SDXL_LIGHTNING("SDXL Lightning (极速)", "sdxl_lightning.safetensors")
        }
        
        enum class VideoModel(val displayName: String) {
            LTX_V2("LTX-2 (推荐)"),
            SVD_XT("Stable Video Diffusion"),
            WAN_2_1("Wan 2.1")
        }

        // Z-Image Turbo API 配置
        const val Z_IMAGE_API_BASE = "https://zimageturbo.ai"
        const val KEY_ZIMAGE_API_KEY = "zimage_api_key"

        // Grok Image API 配置
        const val GROK_API_BASE = "https://api.x.ai/v1"
        const val KEY_GROK_API_KEY = "grok_api_key"

        // 保存到 SharedPreferences 的 Key
        const val KEY_COMFYUI_URL = "comfyui_url"
        const val KEY_FOOOCUS_URL = "fooocus_url"
        const val KEY_IMAGE_MODEL = "image_model"
    }
    
    // 支持远程 URL（如 Zeabur）
    private var comfyuiUrl: String = ""  // 完整 URL，如 https://xxx.zeabur.app
    private var fooocusUrl: String = ""
    private var useHttps: Boolean = true  // 默认使用 HTTPS
    
    // Z-Image Turbo API
    private var zImageApiKey: String = ""
    
    // Grok Image API
    private var grokApiKey: String = ""
    
    private var comfyuiHost: String = DEFAULT_COMFYUI_HOST
    private var comfyuiPort: Int = DEFAULT_COMFYUI_PORT
    private var fooocusHost: String = DEFAULT_FOOOCUS_HOST
    private var fooocusPort: Int = DEFAULT_FOOOCUS_PORT
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // 从 SharedPreferences 加载配置
    fun loadConfig(prefs: android.content.SharedPreferences) {
        comfyuiUrl = prefs.getString(KEY_COMFYUI_URL, "") ?: ""
        fooocusUrl = prefs.getString(KEY_FOOOCUS_URL, "") ?: ""
        zImageApiKey = prefs.getString(KEY_ZIMAGE_API_KEY, "") ?: ""
        grokApiKey = prefs.getString(KEY_GROK_API_KEY, "") ?: ""
        
        if (comfyuiUrl.isNotEmpty()) {
            parseUrl(comfyuiUrl) { host, port, https ->
                comfyuiHost = host
                comfyuiPort = port
                useHttps = https
            }
        }
        
        if (fooocusUrl.isNotEmpty()) {
            parseUrl(fooocusUrl) { host, port, https ->
                fooocusHost = host
                fooocusPort = port
            }
        }
    }

    // 保存配置到 SharedPreferences  
    fun saveConfig(prefs: android.content.SharedPreferences, comfyUrl: String, fooUrl: String) {
        prefs.edit()
            .putString(KEY_COMFYUI_URL, comfyUrl)
            .putString(KEY_FOOOCUS_URL, fooUrl)
            .apply()
        
        comfyuiUrl = comfyUrl
        fooocusUrl = fooUrl
        
        if (comfyUrl.isNotEmpty()) {
            parseUrl(comfyUrl) { host, port, https ->
                comfyuiHost = host
                comfyuiPort = port
                useHttps = https
            }
        }
        
        if (fooUrl.isNotEmpty()) {
            parseUrl(fooUrl) { host, port, _ ->
                fooocusHost = host
                fooocusPort = port
            }
        }
    }

    // 解析 URL（支持 http/https）
    private fun parseUrl(url: String, callback: (String, Int, Boolean) -> Unit) {
        try {
            val fullUrl = if (!url.startsWith("http")) "https://$url" else url
            val javaUrl = java.net.URL(fullUrl)
            val port = if (javaUrl.port == -1) {
                if (javaUrl.protocol == "https") 443 else 80
            } else javaUrl.port
            callback(javaUrl.host, port, javaUrl.protocol == "https")
        } catch (e: Exception) {
            android.util.Log.e("ImageGen", "Invalid URL: $url")
        }
    }

    // 获取基础 URL
    private fun getComfyBaseUrl(): String {
        return if (comfyuiUrl.isNotEmpty()) {
            if (!comfyuiUrl.startsWith("http")) "https://$comfyuiUrl" else comfyuiUrl
        } else {
            val protocol = if (useHttps) "https" else "http"
            "$protocol://$comfyuiHost:$comfyuiPort"
        }
    }
    
    // ========== ComfyUI 图片生成 ==========
    
    suspend fun generateImageWithComfyUI(
        prompt: String,
        negativePrompt: String = "blurry, deformed, extra limbs, low quality, watermark, text, logo",
        model: ImageModel = ImageModel.Z_IMAGE_TURBO,
        steps: Int = 8,
        cfgScale: Float = 2.0f,
        width: Int = 512,
        height: Int = 768,
        seed: Long = -1
    ): GeneratedImage? = withContext(Dispatchers.IO) {
        try {
            val baseUrl = getComfyBaseUrl()
            
            // 构建 prompt
            val positive = buildPrompt(prompt, model)
            val finalNegative = if (negativePrompt.isNotEmpty()) {
                "$negativePrompt, blurry, deformed, low quality"
            } else {
                "blurry, deformed, extra limbs, low quality, watermark"
            }
            
            // 构建工作流（简化的通用工作流）
            val workflow = buildComfyUIWorkflow(
                positive = positive,
                negative = finalNegative,
                steps = steps,
                cfgScale = cfgScale,
                width = width,
                height = height,
                seed = seed,
                model = model
            )
            
            // 调用 ComfyUI API
            val jsonMediaType = "application/json".toMediaType()
            val requestBody = JSONObject(workflow).toString().toRequestBody(jsonMediaType)
            
            val request = Request.Builder()
                .url("$baseUrl/prompt")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val jsonResponse = JSONObject(responseBody ?: "")
                val promptId = jsonResponse.getString("prompt_id")
                
                // 等待生成完成
                waitForCompletion(host, port, promptId)
                
                // 获取生成的图片
                getComfyUIImages(host, port, promptId)
            } else {
                android.util.Log.e("ImageGen", "ComfyUI error: ${response.code} - ${response.message}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageGen", "Error generating image: ${e.message}")
            null
        }
    }
    
    private fun buildPrompt(prompt: String, model: ImageModel): String {
        val stylePrefix = when (model) {
            ImageModel.Z_IMAGE_TURBO -> "masterpiece, best quality, photorealistic, highly detailed, "
            ImageModel.PONY_NSFW -> "masterpiece, best quality, pony style, anime, "
            ImageModel.JUGGERNAUT_XL -> "masterpiece, best quality, photorealistic, 8k uhd, dslr, "
            ImageModel.FLUX_SCHNELL -> "masterpiece, best quality, detailed, "
            ImageModel.SDXL_LIGHTNING -> "masterpiece, best quality, fast generation, "
        }
        return "$stylePrefix$prompt"
    }
    
    private fun buildComfyUIWorkflow(
        positive: String,
        negative: String,
        steps: Int,
        cfgScale: Float,
        width: Int,
        height: Int,
        seed: Long,
        model: ImageModel
    ): String {
        val actualSeed = if (seed == -1L) (Math.random() * 1000000).toLong() else seed
        
        // 简化的 ComfyUI 工作流 JSON
        return """
        {
            "1": {
                "inputs": {
                    "text": "$positive",
                    "clip": ["3", 0]
                },
                "class_type": "CLIPTextEncode",
                "_meta": {"title": "Positive Prompt"}
            },
            "2": {
                "inputs": {
                    "text": "$negative",
                    "clip": ["3", 0]
                },
                "class_type": "CLIPTextEncode",
                "_meta": {"title": "Negative Prompt"}
            },
            "3": {
                "inputs": {
                    "model_name": "${model.name.lowercase()}.safetensors"
                },
                "class_type": "CheckpointLoaderSimple",
                "_meta": {"title": "Load Checkpoint"}
            },
            "4": {
                "inputs": {
                    "samples": ["5", 0],
                    "vae": ["3", 2]
                },
                "class_type": "VAEDecode",
                "_meta": {"title": "VAE Decode"}
            },
            "5": {
                "inputs": {
                    "cfg": $cfgScale,
                    "positive": ["1", 0],
                    "negative": ["2", 0],
                    "model": ["3", 0],
                    "sampler": "euler",
                    "steps": $steps,
                    "seed": $actualSeed,
                    "scheduler": "normal"
                },
                "class_type": "KSampler",
                "_meta": {"title": "K采样器"}
            },
            "6": {
                "inputs": {
                    "width": $width,
                    "height": $height,
                    "batch_size": 1
                },
                "class_type": "EmptyLatentImage",
                "_meta": {"title": "Empty Latent Image"}
            },
            "7": {
                "inputs": {
                    "images": ["4", 0]
                },
                "class_type": "SaveImage",
                "_meta": {"title": "Save Image"}
            }
        }
        """.trimIndent()
    }
    
    private suspend fun waitForCompletion(host: String, port: Int, promptId: String) {
        var completed = false
        var attempts = 0
        val maxAttempts = 120 // 最多等待 2 分钟
        
        while (!completed && attempts < maxAttempts) {
            kotlinx.coroutines.delay(1000)
            attempts++
            
            try {
                val request = Request.Builder()
                    .url("$baseUrl/history/$promptId")
                    .get()
                    .build()
                
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    if (body.contains("\"status\":")) {
                        val historyJson = JSONObject(body)
                        if (historyJson.has(promptId)) {
                            val promptData = historyJson.getJSONObject(promptId)
                            if (promptData.has("status")) {
                                val status = promptData.getJSONObject("status")
                                if (status.getString("status_str") == "success") {
                                    completed = true
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("ImageGen", "Poll error: ${e.message}")
            }
        }
    }
    
    private fun getComfyUIImages(host: String, port: Int, promptId: String): GeneratedImage? {
        return try {
            val request = Request.Builder()
                .url("http://$host:$port/history/$promptId")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string() ?: ""
                val json = JSONObject(body)
                val promptData = json.getJSONObject(promptId)
                val outputs = promptData.getJSONObject("outputs")
                
                // 找到 SaveImage 节点
                val saveImageNode = outputs.keys().asSequence().firstOrNull { key ->
                    outputs.getJSONObject(key).has("images")
                }
                
                if (saveImageNode != null) {
                    val images = outputs.getJSONObject(saveImageNode).getJSONArray("images")
                    if (images.length() > 0) {
                        val imageInfo = images.getJSONObject(0)
                        val filename = imageInfo.getString("filename")
                        val subfolder = imageInfo.optString("subfolder", "")
                        
                        // 下载图片
                        val imageUrl = if (subfolder.isNotEmpty()) {
                            "$baseUrl/view?filename=$subfolder/$filename"
                        } else {
                            "$baseUrl/view?filename=$filename"
                        }
                        
                        downloadImage(imageUrl, filename)
                    }
                } else null
            } else null
        } catch (e: Exception) {
            android.util.Log.e("ImageGen", "Error getting images: ${e.message}")
            null
        }
    }
    
    // ========== Fooocus 图片生成（更简单） ==========
    
    suspend fun generateImageWithFooocus(
        prompt: String,
        negativePrompt: String = "",
        style: String = "photorealistic",
        aspectRatio: String = "vertical", // vertical, square, horizontal
        speed: String = "fast" // fast, normal, quality
    ): GeneratedImage? = withContext(Dispatchers.IO) {
        try {
            val host = fooocusHost
            val port = fooocusPort
            
            val reqBody = JSONObject().apply {
                put("prompt", prompt)
                put("negative_prompt", negativePrompt)
                put("style", style)
                put("aspect_ratio", aspectRatio)
                put("speed", speed)
                put("publish_api", true)
            }
            
            val fooocusUrl = if (fooocusUrl.isNotEmpty()) {
                if (!fooocusUrl.startsWith("http")) "https://$fooocusUrl" else fooocusUrl
            } else {
                "http://$fooocusHost:$fooocusPort"
            }
            
            val jsonMediaType = "application/json".toMediaType()
            val requestBody = reqBody.toString().toRequestBody(jsonMediaType)
            
            val request = Request.Builder()
                .url("$fooocusUrl/v1/generation")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val jsonResponse = JSONObject(responseBody ?: "")
                
                if (jsonResponse.has("base64")) {
                    val base64Image = jsonResponse.getString("base64")
                    val filename = "fooocus_${System.currentTimeMillis()}.png"
                    saveBase64Image(base64Image, filename)
                } else null
            } else {
                android.util.Log.e("ImageGen", "Fooocus error: ${response.code}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("ImageGen", "Fooocus error: ${e.message}")
            null
        }
    }
    
    // ========== 视频生成 ==========
    
    suspend fun generateVideoWithLTX(
        imagePath: String,
        prompt: String = "",
        duration: Int = 5,
        seed: Long = -1
    ): GeneratedVideo? = withContext(Dispatchers.IO) {
        try {
            // LTX-Video API 调用
            val actualSeed = if (seed == -1L) (Math.random() * 1000000).toLong() else seed
            
            val reqBody = JSONObject().apply {
                put("image_path", imagePath)
                put("prompt", prompt)
                put("duration", duration)
                put("seed", actualSeed)
                put("cfg_scale", 1.0)
            }
            
            val request = Request.Builder()
                .url("http://$comfyuiHost:$comfyuiPort/ltx_video")
                .post(reqBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val jsonResponse = JSONObject(responseBody ?: "")
                
                if (jsonResponse.has("video_path")) {
                    val videoPath = jsonResponse.getString("video_path")
                    GeneratedVideo(
                        path = videoPath,
                        duration = duration,
                        model = "LTX-Video"
                    )
                } else null
            } else {
                android.util.Log.e("VideoGen", "LTX error: ${response.code}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("VideoGen", "Error generating video: ${e.message}")
            null
        }
    }
    
    suspend fun generateVideoWithSVD(
        imagePath: String,
        frames: Int = 14,
        seed: Long = -1
    ): GeneratedVideo? = withContext(Dispatchers.IO) {
        try {
            // SVD API 调用
            val actualSeed = if (seed == -1L) (Math.random() * 1000000).toLong() else seed
            
            // 读取图片并转为 base64
            val imageFile = File(imagePath)
            if (!imageFile.exists()) {
                android.util.Log.e("VideoGen", "Image file not found: $imagePath")
                return@withContext null
            }
            
            val imageBytes = imageFile.readBytes()
            val imageBase64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            
            val reqBody = JSONObject().apply {
                put("image", "data:image/png;base64,$imageBase64")
                put("video_length", frames)
                put("seed", actualSeed)
            }
            
            val request = Request.Builder()
                .url("http://$comfyuiHost:$comfyuiPort/svd")
                .post(reqBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val jsonResponse = JSONObject(responseBody ?: "")
                
                if (jsonResponse.has("images")) {
                    // SVD 返回一系列图片帧，需要合成视频
                    val frames = jsonResponse.getJSONArray("images")
                    val outputPath = saveVideoFrames(frames)
                    
                    if (outputPath != null) {
                        GeneratedVideo(
                            path = outputPath,
                            duration = frames.length() / 8, // 约 8fps
                            model = "SVD"
                        )
                    } else null
                } else null
            } else {
                android.util.Log.e("VideoGen", "SVD error: ${response.code}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("VideoGen", "Error generating video: ${e.message}")
            null
        }
    }
    
    // ========== 辅助函数 ==========
    
    private fun downloadImage(url: String, filename: String): GeneratedImage? {
        return try {
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                if (bytes != null) {
                    val savedPath = saveImageToStorage(bytes, filename)
                    if (savedPath != null) {
                        GeneratedImage(
                            path = savedPath,
                            filename = filename,
                            model = "ComfyUI"
                        )
                    } else null
                } else null
            } else null
        } catch (e: Exception) {
            android.util.Log.e("ImageGen", "Download error: ${e.message}")
            null
        }
    }
    
    private fun saveBase64Image(base64: String, filename: String): GeneratedImage? {
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            val savedPath = saveImageToStorage(bytes, filename)
            if (savedPath != null) {
                GeneratedImage(path = savedPath, filename = filename, model = "Fooocus")
            } else null
        } catch (e: Exception) {
            android.util.Log.e("ImageGen", "Save base64 error: ${e.message}")
            null
        }
    }
    
    private fun saveImageToStorage(bytes: ByteArray, filename: String): String? {
        return try {
            val imagesDir = File(context.getExternalFilesDir(null), "generated_images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }
            
            val outputFile = File(imagesDir, filename)
            FileOutputStream(outputFile).use { fos ->
                fos.write(bytes)
            }
            
            outputFile.absolutePath
        } catch (e: Exception) {
            android.util.Log.e("ImageGen", "Save error: ${e.message}")
            null
        }
    }
    
    private fun saveVideoFrames(frames: JSONArray): String? {
        return try {
            val videosDir = File(context.getExternalFilesDir(null), "generated_videos")
            if (!videosDir.exists()) {
                videosDir.mkdirs()
            }
            
            // 简化处理：保存第一帧作为预览
            if (frames.length() > 0) {
                val firstFrame = frames.getString(0)
                val bytes = Base64.decode(firstFrame, Base64.NO_WRAP)
                val outputPath = File(videosDir, "svd_preview_${System.currentTimeMillis()}.png")
                FileOutputStream(outputPath).use { fos ->
                    fos.write(bytes)
                }
                outputPath.absolutePath
            } else null
        } catch (e: Exception) {
            android.util.Log.e("VideoGen", "Save frames error: ${e.message}")
            null
        }
    }
    
    // ========== 配置 ==========
    
    fun configureComfyUI(host: String, port: Int) {
        this.comfyuiHost = host
        this.comfyuiPort = port
    }
    
    fun configureFooocus(host: String, port: Int) {
        this.fooocusHost = host
        this.fooocusPort = port
    }

    // ========== Z-Image Turbo API ==========
    
    suspend fun generateImageWithZImage(
        prompt: String,
        aspectRatio: String = "9:16",  // 9:16 竖版, 16:9 横版, 1:1 方版, 4:3, 3:4
        seed: Long = -1
    ): GeneratedImage? = withContext(Dispatchers.IO) {
        if (zImageApiKey.isEmpty()) {
            android.util.Log.e("ZImage", "Z-Image API key not configured")
            return@withContext null
        }

        try {
            // 1. 提交生成任务
            val taskId = submitZImageTask(prompt, aspectRatio, seed)
            if (taskId == null) {
                android.util.Log.e("ZImage", "Failed to submit task")
                return@withContext null
            }

            android.util.Log.d("ZImage", "Task submitted: $taskId, waiting for completion...")

            // 2. 轮询等待完成
            val imageUrl = waitForZImageCompletion(taskId)
            if (imageUrl == null) {
                android.util.Log.e("ZImage", "Failed to get result")
                return@withContext null
            }

            // 3. 下载图片
            val filename = "zimage_${System.currentTimeMillis()}.jpg"
            val savedPath = downloadAndSaveImage(imageUrl, filename)
            
            if (savedPath != null) {
                GeneratedImage(
                    path = savedPath,
                    filename = filename,
                    model = "Z-Image-Turbo"
                )
            } else null
        } catch (e: Exception) {
            android.util.Log.e("ZImage", "Error: ${e.message}")
            null
        }
    }

    private suspend fun submitZImageTask(
        prompt: String,
        aspectRatio: String,
        seed: Long
    ): String? = withContext(Dispatchers.IO) {
        try {
            val reqBody = JSONObject().apply {
                put("prompt", prompt)
                put("aspect_ratio", aspectRatio)
                if (seed > 0) put("seed", seed)
            }

            val request = Request.Builder()
                .url("$Z_IMAGE_API_BASE/api/generate")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $zImageApiKey")
                .post(reqBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()
            
            android.util.Log.d("ZImage", "Submit response: $body")

            if (response.isSuccessful) {
                val json = JSONObject(body ?: "")
                if (json.getInt("code") == 200) {
                    json.getJSONObject("data").getString("task_id")
                } else null
            } else null
        } catch (e: Exception) {
            android.util.Log.e("ZImage", "Submit error: ${e.message}")
            null
        }
    }

    private suspend fun waitForZImageCompletion(taskId: String, maxWaitSeconds: Int = 120): String? = withContext(Dispatchers.IO) {
        var waitCount = 0
        val maxAttempts = maxWaitSeconds / 2  // 每 2 秒查询一次

        while (waitCount < maxAttempts) {
            kotlinx.coroutines.delay(2000)
            waitCount++

            try {
                val request = Request.Builder()
                    .url("$Z_IMAGE_API_BASE/api/status?task_id=$taskId")
                    .header("Authorization", "Bearer $zImageApiKey")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val body = response.body?.string()

                if (response.isSuccessful) {
                    val json = JSONObject(body ?: "")
                    val data = json.getJSONObject("data")
                    val status = data.getString("status")

                    android.util.Log.d("ZImage", "Status: $status")

                    when (status) {
                        "SUCCESS" -> {
                            val images = data.getJSONArray("response")
                            if (images.length() > 0) {
                                return@withContext images.getString(0)
                            }
                        }
                        "FAILED" -> {
                            val error = data.optString("error_message", "Unknown error")
                            android.util.Log.e("ZImage", "Task failed: $error")
                            return@withContext null
                        }
                        "IN_PROGRESS", "PENDING" -> {
                            // 继续等待
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("ZImage", "Poll error: ${e.message}")
            }
        }

        android.util.Log.w("ZImage", "Timeout waiting for result")
        null
    }

    private fun downloadAndSaveImage(imageUrl: String, filename: String): String? {
        return try {
            val request = Request.Builder().url(imageUrl).get().build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val bytes = response.body?.bytes()
                if (bytes != null) {
                    val imagesDir = File(context.getExternalFilesDir(null), "generated_images")
                    if (!imagesDir.exists()) imagesDir.mkdirs()
                    
                    val outputFile = File(imagesDir, filename)
                    FileOutputStream(outputFile).use { fos ->
                        fos.write(bytes)
                    }
                    android.util.Log.d("ZImage", "Image saved: ${outputFile.absolutePath}")
                    outputFile.absolutePath
                } else null
            } else {
                android.util.Log.e("ZImage", "Download failed: ${response.code}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("ZImage", "Download error: ${e.message}")
            null
        }
    }

    fun setZImageApiKey(apiKey: String) {
        this.zImageApiKey = apiKey
    }

    fun setGrokApiKey(apiKey: String) {
        this.grokApiKey = apiKey
    }

    // ========== Grok Image API ==========
    
    suspend fun generateImageWithGrok(
        prompt: String,
        model: String = "grok-imagine-image"
    ): GeneratedImage? = withContext(Dispatchers.IO) {
        if (grokApiKey.isEmpty()) {
            android.util.Log.e("Grok", "Grok API key not configured")
            return@withContext null
        }

        try {
            val reqBody = JSONObject().apply {
                put("model", model)
                put("prompt", prompt)
            }

            val request = Request.Builder()
                .url("$GROK_API_BASE/images/generations")
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer $grokApiKey")
                .post(reqBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            android.util.Log.d("Grok", "Sending request: $prompt")

            val response = client.newCall(request).execute()
            val body = response.body?.string()

            android.util.Log.d("Grok", "Response: $body")

            if (response.isSuccessful) {
                val json = JSONObject(body ?: "")
                val data = json.getJSONArray("data")
                
                if (data.length() > 0) {
                    val imageData = data.getJSONObject(0)
                    val imageUrl = imageData.getString("url")
                    
                    val filename = "grok_${System.currentTimeMillis()}.png"
                    val savedPath = downloadAndSaveImage(imageUrl, filename)
                    
                    if (savedPath != null) {
                        return@withContext GeneratedImage(
                            path = savedPath,
                            filename = filename,
                            model = "Grok Image"
                        )
                    }
                }
                null
            } else {
                android.util.Log.e("Grok", "Error: ${response.code} - $body")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("Grok", "Error: ${e.message}")
            null
        }
    }
}

data class GeneratedImage(
    val path: String,
    val filename: String,
    val model: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class GeneratedVideo(
    val path: String,
    val duration: Int,
    val model: String,
    val timestamp: Long = System.currentTimeMillis()
)
