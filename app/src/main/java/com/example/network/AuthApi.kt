package com.example.network

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AuthApi {
    private const val TAG = "AuthApi"
    
    // Backend URL - change to your actual domain (easyo.online) when hosted
    var baseUrl = "https://easyo.online" // Use your actual Hostinger domain

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    data class AuthResult(
        val success: Boolean,
        val message: String,
        val username: String? = null,
        val expiresAt: String? = null
    )

    suspend fun login(username: String): AuthResult {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val url = "$baseUrl/api.php?action=app_login"
            Log.d(TAG, "Logging in with username: $username to $url")

            val payload = JSONObject().apply {
                put("username", username)
            }

            val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .build()

            try {
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string() ?: ""
                    Log.d(TAG, "Login response: $responseBody")

                    val json = JSONObject(responseBody)
                    val success = json.optBoolean("success", false)
                    val message = json.optString("message", "")

                    if (success) {
                        val userObj = json.optJSONObject("user")
                        val expiresAt = userObj?.optString("expires_at")
                        AuthResult(true, message, username, expiresAt)
                    } else {
                        AuthResult(false, message)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Login error", e)
                AuthResult(false, "ข้อผิดพลาดเครือข่าย: ${e.localizedMessage}")
            }
        }
    }
}
