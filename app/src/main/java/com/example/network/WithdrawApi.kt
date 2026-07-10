package com.example.network

import android.util.Log
import com.example.data.WithdrawJob
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object WithdrawApi {
    private const val TAG = "WithdrawApi"
    
    // Default base URL provided by the user
    var baseHostUrl = "https://ad852e84-996f-4143-85c0-b3697a493f91-00-32c6epyutf3ti.sisko.replit.dev"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

    /**
     * Fetch the list of pending withdrawal jobs.
     */
    fun fetchWithdrawJobs(token: String): List<WithdrawJob> {
        val url = "$baseHostUrl/api/v1/withdraw/jobs"
        Log.d(TAG, "Fetching withdrawal jobs from: $url")
        
        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("X-SMS-Token", token)
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Fetch failed with HTTP code: ${response.code}")
                    return emptyList()
                }

                val bodyStr = response.body?.string() ?: return emptyList()
                Log.d(TAG, "Fetch raw response: $bodyStr")

                val jsonResponse = JSONObject(bodyStr)
                if (!jsonResponse.has("data")) {
                    return emptyList()
                }

                val dataArray = jsonResponse.getJSONArray("data")
                val jobsList = mutableListOf<WithdrawJob>()

                for (i in 0 until dataArray.length()) {
                    val item = dataArray.getJSONObject(i)
                    val ref = item.optString("ref", "")
                    val amount = item.optDouble("amount", 0.0)
                    val bankCode = item.optString("bank_code", "")
                    val accountNumber = item.optString("account_number", "")
                    val accountName = item.optString("account_name", "")

                    if (ref.isNotEmpty()) {
                        jobsList.add(
                            WithdrawJob(
                                ref = ref,
                                amount = amount,
                                bankCode = bankCode,
                                accountNumber = accountNumber,
                                accountName = accountName,
                                status = "PENDING", // Initial local state
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
                return jobsList
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching withdrawal jobs", e)
            return emptyList()
        }
    }

    /**
     * Report completion of a withdrawal job.
     */
    fun reportWithdrawComplete(
        token: String,
        ref: String,
        status: String, // "success" or "failed"
        bankRef: String,
        note: String
    ): Pair<Boolean, String> {
        val url = "$baseHostUrl/api/v1/withdraw/complete"
        Log.d(TAG, "Reporting withdrawal job complete to: $url")

        val payload = JSONObject().apply {
            put("ref", ref)
            put("status", status)
            put("bank_ref", bankRef)
            put("note", note)
        }

        val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("X-SMS-Token", token)
            .addHeader("Content-Type", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                val code = response.code
                Log.d(TAG, "Report response (HTTP $code): $responseBody")

                return if (response.isSuccessful) {
                    Pair(true, "สำเร็จ (HTTP $code)")
                } else {
                    Pair(false, "ล้มเหลว (HTTP $code): $responseBody")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reporting withdrawal job complete", e)
            return Pair(false, "ข้อผิดพลาดเครือข่าย: ${e.localizedMessage}")
        }
    }
}
