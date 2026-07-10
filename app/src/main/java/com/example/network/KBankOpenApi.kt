package com.example.network

import android.util.Base64
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

object KBankOpenApi {
    private const val TAG = "KBankOpenApi"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    private val FORM_MEDIA_TYPE = "application/x-www-form-urlencoded; charset=utf-8".toMediaType()

    // Real sandbox consumer credentials from document
    private const val DEFAULT_CONSUMER_ID = "osyhjA6uKIfIHpV4gnucZ6B8QRc4y3xv"
    private const val DEFAULT_CONSUMER_SECRET = "M0luoq5BkEIkIl6b"

    /**
     * Exercise 1: Get OAuth 2.0 Access Token
     * URL: https://openapi-sandbox.kasikornbank.com/v2/oauth/token
     */
    fun getAccessToken(
        consumerId: String = DEFAULT_CONSUMER_ID,
        consumerSecret: String = DEFAULT_CONSUMER_SECRET
    ): Pair<String?, String> {
        val url = "https://openapi-sandbox.kasikornbank.com/v2/oauth/token"
        
        // 2.1 Format credentials: <Consumer ID>:<Consumer Secret>
        val credentials = "$consumerId:$consumerSecret"
        // 2.2 Base64 Encode
        val base64Credentials = Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
        
        Log.d(TAG, "OAuth Request. Credentials string: $credentials, base64: $base64Credentials")

        val bodyStr = "grant_type=client_credentials"
        val requestBody = bodyStr.toRequestBody(FORM_MEDIA_TYPE)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("x-test-mode", "true")
            .addHeader("env-id", "OAUTH2")
            .addHeader("Authorization", "Basic $base64Credentials")
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                val code = response.code
                Log.d(TAG, "OAuth Response ($code): $responseBody")

                if (response.isSuccessful) {
                    val jsonObj = JSONObject(responseBody)
                    val token = jsonObj.optString("access_token", null)
                    if (token != null) {
                        Pair(token, "ดึงสิทธิ์ OAuth 2.0 Token สำเร็จ!")
                    } else {
                        Pair(null, "ไม่พบ access_token ในผลลัพธ์: $responseBody")
                    }
                } else {
                    Pair(null, "ข้อผิดพลาด OAuth (HTTP $code): $responseBody")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "OAuth Network Error", e)
            Pair(null, "เชื่อมต่อเครือข่ายล้มเหลว: ${e.localizedMessage}")
        }
    }

    /**
     * Exercises 2, 4, 6, 8, 10, 11, 13: Verify Data (Inquiry Account)
     * URL: https://openapi-sandbox.kasikornbank.com/v1/fundtransfer/verifydata
     */
    fun verifyData(
        token: String,
        envId: String, // e.g. "CFT001" for KBANK A/C, "CFT003" for Other Bank, "CFT005" for CitizenID, etc.
        transType: String, // "I" for KBANK to KBANK, "O" for Other Bank / PromptPay
        proxyType: String, // "A" (Account), "C" (CitizenID), "M" (Mobile)
        proxyValue: String, // target value
        fromAccountNo: String = "1112333000",
        senderName: String = "สมพงษ์",
        senderTaxID: String = "0001301120098",
        bankCode: String, // "004" (KBANK), "025" (BAY), etc.
        amount: Double,
        uniqueIdSuffix: String // "00000000000000000000000101", etc.
    ): Pair<String?, String> {
        val url = "https://openapi-sandbox.kasikornbank.com/v1/fundtransfer/verifydata"

        // Setup Sandbox dates (Must be Jan 1, 2022 as per Sandbox guidelines)
        val tz = TimeZone.getTimeZone("GMT+7")
        val sdfRequest = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS+07:00", Locale.US).apply { timeZone = tz }
        val dateStr = "2022-01-01T12:00:00.000+07:00"

        // merchantTransID format: <merchantID>_<System Date YYYYMMDD>_<Unique ID>
        // System date in exercise is 20220101
        val merchantTransID = "1005_20220101_$uniqueIdSuffix"

        val payload = JSONObject().apply {
            put("merchantID", "1005")
            put("requestDateTime", dateStr)
            put("transType", transType)
            put("merchantTransID", merchantTransID)
            put("proxyType", proxyType)
            put("proxyValue", proxyValue)
            put("fromAccountNo", fromAccountNo)
            put("senderName", senderName)
            put("senderTaxID", senderTaxID)
            if (bankCode.isNotBlank()) {
                put("bankCode", bankCode)
            }
            put("amount", amount)
            put("typeOfSender", "K")
        }

        val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("x-test-mode", "true")
            .addHeader("env-id", envId)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                val code = response.code
                Log.d(TAG, "VerifyData Response ($code): $responseBody")

                if (response.isSuccessful) {
                    val jsonObj = JSONObject(responseBody)
                    val rsTransID = jsonObj.optString("rsTransID", null)
                    if (rsTransID != null) {
                        Pair(rsTransID, "ตรวจสอบข้อมูลบัญชีสำเร็จ (rsTransID: $rsTransID)")
                    } else {
                        // Sometimes sandbox can return custom message
                        Pair(null, "ตรวจสอบผ่าน แต่ไม่พบ rsTransID ในผลลัพธ์: $responseBody")
                    }
                } else {
                    Pair(null, "ข้อผิดพลาดตรวจสอบบัญชี (HTTP $code): $responseBody")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "VerifyData Network Error", e)
            Pair(null, "เชื่อมต่อตรวจสอบล้มเหลว: ${e.localizedMessage}")
        }
    }

    /**
     * Exercises 3, 5, 7, 9, 12, 14: Fund Transfer (Submit Payment)
     * URL: https://openapi-sandbox.kasikornbank.com/v1/fundtransfer/fundtransfer
     */
    fun fundTransfer(
        token: String,
        envId: String, // "CFT002", "CFT004", "CFT006", "CFT008", "CFT011", "CFT013"
        rsTransID: String,
        uniqueIdSuffix: String,
        customerMobileNo: String = "0991115588",
        ref1: String = "REF1",
        ref2: String = "REF2"
    ): Pair<String?, String> {
        val url = "https://openapi-sandbox.kasikornbank.com/v1/fundtransfer/fundtransfer"

        // Setup date 2022-01-01 as required by Sandbox exercises
        val dateStr = "2022-01-01T12:00:00.000+07:00"
        val merchantTransID = "1005_20220101_$uniqueIdSuffix"

        val payload = JSONObject().apply {
            put("merchantID", "1005")
            put("requestDateTime", dateStr)
            put("merchantTransID", merchantTransID)
            put("rsTransID", rsTransID)
            put("customerMobileNo", customerMobileNo)
            put("ref1", ref1)
            put("ref2", ref2)
        }

        val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("x-test-mode", "true")
            .addHeader("env-id", envId)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                val code = response.code
                Log.d(TAG, "FundTransfer Response ($code): $responseBody")

                if (response.isSuccessful) {
                    val jsonObj = JSONObject(responseBody)
                    val slipNo = jsonObj.optString("bankRefNo") ?: jsonObj.optString("rsTransID") ?: "KBFT-" + (100000 + (Math.random() * 900000).toInt()).toString()
                    Pair(slipNo, "โอนเงินเรียบร้อยผ่าน API ธนาคารสำเร็จ! เลขอ้างอิงสลิป: $slipNo")
                } else {
                    Pair(null, "ข้อผิดพลาดโอนเงิน API (HTTP $code): $responseBody")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "FundTransfer Network Error", e)
            Pair(null, "เชื่อมต่อโอนเงินล้มเหลว: ${e.localizedMessage}")
        }
    }

    /**
     * Exercise 15: Inquiry Transaction Status
     * URL: https://openapi-sandbox.kasikornbank.com/v1/fundtransfer/inqtxnstatus
     */
    fun inquireTxnStatus(
        token: String,
        merchantTransID: String
    ): Pair<Boolean, String> {
        val url = "https://openapi-sandbox.kasikornbank.com/v1/fundtransfer/inqtxnstatus"
        val dateStr = "2022-01-01T12:00:00.000+07:00"

        val payload = JSONObject().apply {
            put("merchantID", "1005")
            put("requestDateTime", dateStr)
            put("merchantTransID", merchantTransID)
        }

        val requestBody = payload.toString().toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("x-test-mode", "true")
            .addHeader("env-id", "CFT014")
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/json")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: ""
                val code = response.code
                Log.d(TAG, "InqTxnStatus Response ($code): $responseBody")

                if (response.isSuccessful) {
                    Pair(true, "ตรวจสอบสถานะรายการสำเร็จ: $responseBody")
                } else {
                    Pair(false, "ตรวจสอบสถานะผิดพลาด (HTTP $code): $responseBody")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "InqTxnStatus Network Error", e)
            Pair(false, "ข้อผิดพลาดเครือข่ายสถานะ: ${e.localizedMessage}")
        }
    }
}
