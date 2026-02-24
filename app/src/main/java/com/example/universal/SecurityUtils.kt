package com.example.universal

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object SecurityUtils {
    private const val PREFS_NAME = "SecurePrefs"
    private const val KEY_ENCRYPTED_DATA = "encrypted_data"
    private const val KEY_HASH = "app_hash"
    private const val ALGORITHM = "AES"
    
    private var encryptedPrefs: SharedPreferences? = null

    fun init(context: Context) {
        encryptedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun encryptAndStore(context: Context, key: String, value: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encrypted = encrypt(value, getDeviceId(context))
        prefs.edit().putString(key, encrypted).apply()
    }

    fun retrieveAndDecrypt(context: Context, key: String): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encrypted = prefs.getString(key, null) ?: return null
        return try {
            decrypt(encrypted, getDeviceId(context))
        } catch (e: Exception) {
            null
        }
    }

    private fun encrypt(data: String, key: String): String {
        val secretKey = generateKey(key)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
    }

    private fun decrypt(encryptedData: String, key: String): String {
        val secretKey = generateKey(key)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        val decryptedBytes = cipher.doFinal(Base64.decode(encryptedData, Base64.NO_WRAP))
        return String(decryptedBytes, Charsets.UTF_8)
    }

    private fun generateKey(key: String): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(key.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(hash, ALGORITHM)
    }

    private fun getDeviceId(context: Context): String {
        return android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "default_device_key"
    }

    fun isDebugMode(): Boolean {
        return BuildConfig.DEBUG
    }

    fun getAppSignatureHash(context: Context): String {
        try {
            val packageInfo = context.packageManager.getPackageInfo(
                context.packageName,
                android.content.pm.PackageManager.GET_SIGNATURES
            )
            val signature = packageInfo.signatures[0]
            val digest = MessageDigest.getInstance("SHA")
            val hash = digest.digest(signature.toByteArray())
            return hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            return "unknown"
        }
    }

    fun validateAppSignature(context: Context, validHashes: List<String>): Boolean {
        val currentHash = getAppSignatureHash(context)
        return validHashes.any { it.equals(currentHash, ignoreCase = true) }
    }

    fun clearAllSecureData(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}

object LogSecurity {
    private const val TAG = "App"
    
    fun d(tag: String, message: String) {
        if (!SecurityUtils.isDebugMode()) return
        android.util.Log.d("$TAG-$tag", message)
    }

    fun e(tag: String, message: String) {
        if (!SecurityUtils.isDebugMode()) return
        android.util.Log.e("$TAG-$tag", message)
    }

    fun w(tag: String, message: String) {
        if (!SecurityUtils.isDebugMode()) return
        android.util.Log.w("$TAG-$tag", message)
    }

    fun i(tag: String, message: String) {
        if (!SecurityUtils.isDebugMode()) return
        android.util.Log.i("$TAG-$tag", message)
    }

    fun sensitive(tag: String, message: String) {
        if (!SecurityUtils.isDebugMode()) return
        android.util.Log.w("$TAG-$tag", "[SENSITIVE] ${message.take(20)}...")
    }
}
