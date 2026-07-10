package com.example.network

import android.content.Context
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.ForwardLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object WebhookSender {
    private const val TAG = "WebhookSender"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    private fun escapeJson(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    suspend fun sendForward(
        context: Context,
        type: String, // "SMS" or "NOTIFICATION"
        sender: String,
        message: String,
        ignoreEnabledCheck: Boolean = false
    ): Boolean {
        return withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val configDao = db.configDao()
            val logDao = db.forwardLogDao()

            // Fetch configuration directly and merge with defaults
            val config = AppDatabase.getMergedConfig(configDao.getConfigDirect())

            // Check if forwarding is enabled for this type
            if (!ignoreEnabledCheck) {
                if (type == "SMS" && !config.isSmsForwardEnabled) {
                    Log.d(TAG, "SMS forwarding is disabled in settings.")
                    return@withContext false
                }
                if (type == "NOTIFICATION" && !config.isNotificationForwardEnabled) {
                    Log.d(TAG, "Notification forwarding is disabled in settings.")
                    return@withContext false
                }
            }

            val url = config.webhookUrl
            val token = config.token

            if (url.isBlank()) {
                Log.w(TAG, "Webhook URL is blank. Cannot forward.")
                logDao.insertLog(
                    ForwardLog(
                        type = type,
                        sender = sender,
                        message = message,
                        status = "FAILED",
                        responseMessage = "Webhook URL is empty or blank."
                    )
                )
                return@withContext false
            }

            Log.d(TAG, "Forwarding $type from $sender to $url")

            val tz = java.util.TimeZone.getTimeZone("Asia/Bangkok")
            val currentTimestamp = System.currentTimeMillis()
            val currentTimeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).apply { timeZone = tz }.format(Date(currentTimestamp))
            val currentDateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).apply { timeZone = tz }.format(Date(currentTimestamp))

            // Build request JSON with only the essential keys expected by the callback backend.
            // This avoids strict schema validation errors (e.g. unknown fields) on the receiving server.
            val requestBodyJson = """
                {
                  "sender": "${escapeJson(sender)}",
                  "message": "${escapeJson(message)}",
                  "token": "${escapeJson(token)}",
                  "timestamp": $currentTimestamp,
                  "time": "$currentTimeStr",
                  "date": "$currentDateStr",
                  "created_at": "$currentDateStr",
                  "type": "${escapeJson(type)}"
                }
            """.trimIndent()

            try {
                val requestBody = requestBodyJson.toRequestBody(JSON_MEDIA_TYPE)
                
                val request = Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer $token")
                    .addHeader("x-token", token)
                    .addHeader("token", token)
                    .addHeader("key", token)
                    .addHeader("secret", token)
                    .addHeader("x-sms-token", token)
                    .addHeader("X-SMS-Token", token)
                    .addHeader("X-API-KEY", token)
                    .addHeader("api_key", token)
                    .addHeader("Content-Type", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    var isSuccessful = response.isSuccessful
                    var responseBody = response.body?.string() ?: ""
                    var responseCode = response.code

                    // Fallback to application/x-www-form-urlencoded if JSON failed (for compatibility with legacy backends)
                    if (!isSuccessful) {
                        Log.d(TAG, "JSON request returned $responseCode. Retrying with FormUrlEncoded...")
                        val formBody = FormBody.Builder()
                            .add("sender", sender)
                            .add("message", message)
                            .add("token", token)
                            .add("timestamp", currentTimestamp.toString())
                            .add("time", currentTimeStr)
                            .add("date", currentDateStr)
                            .add("created_at", currentDateStr)
                            .add("type", type)
                            .build()

                        val fallbackRequest = Request.Builder()
                            .url(url)
                            .post(formBody)
                            .addHeader("Authorization", "Bearer $token")
                            .addHeader("x-token", token)
                            .addHeader("token", token)
                            .build()

                        client.newCall(fallbackRequest).execute().use { fbResponse ->
                            isSuccessful = fbResponse.isSuccessful
                            responseBody = fbResponse.body?.string() ?: ""
                            responseCode = fbResponse.code
                        }
                    }
                    
                    val logStatus = if (isSuccessful) "SUCCESS" else "FAILED"
                    val responseMsg = if (isSuccessful) {
                        "HTTP $responseCode: OK"
                    } else {
                        "HTTP $responseCode: ${responseBody.take(150)}"
                    }

                    Log.d(TAG, "Forward result: Status=$logStatus, Response=$responseMsg")

                    // Insert to local database logs
                    logDao.insertLog(
                        ForwardLog(
                            type = type,
                            sender = sender,
                            message = message,
                            status = logStatus,
                            responseMessage = responseMsg
                        )
                    )
                    return@withContext isSuccessful
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error forwarding to webhook", e)
                val responseMsg = e.localizedMessage ?: e.toString()
                // Insert failure to local logs
                logDao.insertLog(
                    ForwardLog(
                        type = type,
                        sender = sender,
                        message = message,
                        status = "FAILED",
                        responseMessage = responseMsg
                    )
                )
                return@withContext false
            }
        }
    }
}
