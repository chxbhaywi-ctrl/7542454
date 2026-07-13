package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.example.network.WebhookSender
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    private val TAG = "SmsReceiver"

    // รายการชื่อผู้ส่ง SMS ธนาคารที่รู้จักทั้งหมด
    private val SMS_SENDER_KEYWORDS = listOf(
        "KBANK", "KBANK-", "K-PLUS", "K PLUS", 
        "SCB", "SCB-", "SCBEASY", "SCB EASY",
        "KRUNGTHAI", "KT BANK", "KTB",
        "KRUNGSRI", "KRUNGSRI-", "Bualuang",
        "TTB", "TTBANK", "TTB BANK",
        "GSB", "MyMo", "GSB-",
        "TRUEMONEY", "TrueMoney",
        "SHOPEEPAY", "ShopeePay",
        "LHB", "LH BANK",
        "UOB", "UOB-",
        "CIMB", "CIMB-",
        "BBL", "BangkokBank", "Bangkok Bank",
        "GH BANK", "GHBANK",
        "TISCO",
        "THANACHART",
        "PROMPTPAY",
        "PAYPAL",
        "GRABPAY",
        "BOT"
    )
    
    // แผนที่ชื่อผู้ส่งเป็นชื่อธนาคาร
    private fun getBankNameFromSmsSender(sender: String): String {
        val normalizedSender = sender.uppercase().trim()
        return when {
            normalizedSender.contains("KBANK") || normalizedSender.contains("K-PLUS") || normalizedSender.contains("K PLUS") -> "KBank"
            normalizedSender.contains("SCB") || normalizedSender.contains("SCBEASY") -> "SCB"
            normalizedSender.contains("KRUNGTHAI") || normalizedSender.contains("KT BANK") || normalizedSender.contains("KTB") -> "Krungthai"
            normalizedSender.contains("KRUNGSRI") || normalizedSender.contains("BUALUANG") -> "Krungsri"
            normalizedSender.contains("TTB") || normalizedSender.contains("TTBANK") -> "TTB"
            normalizedSender.contains("GSB") || normalizedSender.contains("MYMO") -> "GSB"
            normalizedSender.contains("TRUEMONEY") -> "TrueMoney"
            normalizedSender.contains("SHOPEEPAY") -> "ShopeePay"
            normalizedSender.contains("LHB") -> "LHB"
            normalizedSender.contains("UOB") -> "UOB"
            normalizedSender.contains("CIMB") -> "CIMB"
            normalizedSender.contains("BBL") || normalizedSender.contains("BANGKOKBANK") || normalizedSender.contains("BANGKOK BANK") -> "Bangkok Bank"
            normalizedSender.contains("GHBANK") || normalizedSender.contains("GH BANK") -> "GHBank"
            normalizedSender.contains("TISCO") -> "TISCO"
            normalizedSender.contains("THANACHART") -> "Thanachart"
            normalizedSender.contains("PROMPTPAY") -> "PromptPay"
            normalizedSender.contains("PAYPAL") -> "PayPal"
            normalizedSender.contains("GRABPAY") || normalizedSender.contains("GRAB") -> "GrabPay"
            normalizedSender.contains("BOT") -> "BOT"
            else -> sender
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.d(TAG, "SMS Received!")
            
            // ระบบรับ SMS ที่สมบูรณ์สำหรับทุก Android เวอร์ชัน
            val messages = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    Telephony.Sms.Intents.getMessagesFromIntent(intent)
                } else {
                    getSmsMessagesLegacy(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting SMS messages", e)
                return
            }
            
            if (messages.isNullOrEmpty()) return

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    // Self-healing: wake up or rebind notification listener when SMS event occurs
                    com.example.service.BankNotificationListenerService.forceRebind(context.applicationContext)

                    // Combine multi-part SMS messages if they are from the same sender
                    val sender = messages[0].displayOriginatingAddress ?: messages[0].originatingAddress ?: "Unknown"
                    val fullBody = messages.mapNotNull { it.messageBody }.joinToString("")
                    
                    Log.d(TAG, "Processing SMS from $sender: $fullBody")
                    
                    // แปลงชื่อผู้ส่งเป็นชื่อธนาคารที่เข้าใจง่าย
                    val resolvedSender = getBankNameFromSmsSender(sender)
                    
                    // Forward SMS
                    WebhookSender.sendForward(
                        context = context.applicationContext,
                        type = "SMS",
                        sender = resolvedSender,
                        message = fullBody
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing incoming SMS", e)
                    try {
                        val db = AppDatabase.getDatabase(context.applicationContext)
                        db.forwardLogDao().insertLog(
                            com.example.data.ForwardLog(
                                type = "SMS",
                                sender = "ERROR",
                                message = "เกิดข้อผิดพลาดในการรับ SMS: ${e.localizedMessage}",
                                status = "FAILED",
                                responseMessage = e.toString()
                            )
                        )
                    } catch (ex: Exception) {
                        // Ignore
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
    
    // รองรับ Android เวอร์ชันเก่า
    @Suppress("DEPRECATION")
    private fun getSmsMessagesLegacy(intent: Intent): Array<SmsMessage?> {
        val pdus = intent.extras?.get("pdus") as? Array<*> ?: return emptyArray()
        val format = intent.extras?.getString("format")
        return pdus.map { pdu ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                SmsMessage.createFromPdu(pdu as ByteArray, format)
            } else {
                SmsMessage.createFromPdu(pdu as ByteArray)
            }
        }.toTypedArray()
    }
}
