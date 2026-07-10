package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.data.AppDatabase
import com.example.data.WithdrawJob
import com.example.network.WebhookSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BankNotificationListenerService : NotificationListenerService() {
    private val TAG = "BankNotificationListener"
    private val serviceScope = CoroutineScope(Dispatchers.Default)
    private val NOTIFICATION_ID = 8888
    private val CHANNEL_ID = "easyo_forwarder_channel"
    private var screenReceiver: android.content.BroadcastReceiver? = null
    private var isPollingJobs = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service Created")
        startForegroundCompat()

        // Start periodic withdrawal polling
        startWithdrawalPollingLoop()

        // Dynamic registration of a screen-on/user-unlock receiver to trigger automatic service recovery
        try {
            val filter = android.content.IntentFilter().apply {
                addAction(android.content.Intent.ACTION_SCREEN_ON)
                addAction(android.content.Intent.ACTION_USER_PRESENT)
            }
            screenReceiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
                    Log.d(TAG, "Screen Event captured: ${intent.action}. Invoking self-healing rebind...")
                    forceRebind(applicationContext)
                }
            }
            registerReceiver(screenReceiver, filter)
            Log.d(TAG, "Screen event dynamic receiver registered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register dynamic screen receiver", e)
        }
    }

    private fun startWithdrawalPollingLoop() {
        if (isPollingJobs) return
        isPollingJobs = true
        serviceScope.launch {
            Log.d(TAG, "Starting 24/7 Withdrawal Polling loop (every 20s)")
            while (isPollingJobs) {
                try {
                    val db = AppDatabase.getDatabase(applicationContext)
                    val config = AppDatabase.getMergedConfig(db.configDao().getConfigDirect())
                    
                    if (config.token.isNotBlank()) {
                        val jobs = com.example.network.WithdrawApi.fetchWithdrawJobs(config.token)
                        if (jobs.isNotEmpty()) {
                            Log.d(TAG, "Polled ${jobs.size} pending withdrawal jobs")
                            
                            // Check if there are newly discovered jobs to notify the user
                            for (job in jobs) {
                                val existing = db.withdrawJobDao().getJobByRef(job.ref)
                                if (existing == null) {
                                    db.withdrawJobDao().insertJob(job)
                                    showNewJobNotification(job)
                                    com.example.service.BankAutomationService.logToRobot("ตรวจพบรายการถอนใหม่: ${job.ref} ยอด ${job.amount} บาท")
                                }
                            }
                        }
                    }
                } catch (e: java.lang.Exception) {
                    Log.e(TAG, "Error in withdrawal polling loop: ${e.message}")
                }
                kotlinx.coroutines.delay(20000) // Poll every 20 seconds
            }
        }
    }

    private fun showNewJobNotification(job: WithdrawJob) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "easyo_withdraw_channel"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "EasyO Withdrawal Alert",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "แจ้งเตือนเมื่อมีรายการถอนเงินรออนุมัติ"
                    enableVibration(true)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notificationIntent = Intent(this, com.example.MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this,
                job.ref.hashCode(),
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val formattedAmount = String.format(java.util.Locale.US, "%,.2f", job.amount)
            val notification = NotificationCompat.Builder(this, channelId)
                .setContentTitle("มีรายการถอนใหม่เข้ามา!")
                .setContentText("ยอด $formattedAmount บาท บัญชี ${job.accountNumber} (${job.bankCode.uppercase()})")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            notificationManager.notify(job.ref.hashCode(), notification)
        } catch (e: java.lang.Exception) {
            Log.e(TAG, "Failed to display job notification", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service Destroyed")
        try {
            if (screenReceiver != null) {
                unregisterReceiver(screenReceiver)
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat()
        return START_STICKY
    }

    private fun startForegroundCompat() {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "EasyO Forwarder Service",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "รันบริการดักจับแจ้งเตือนของธนาคารและ SMS ตลอด 24 ชม."
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notificationIntent = Intent(this, com.example.MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("EasyO Forwarder กำลังทำงาน")
                .setContentText("ระบบแสตนด์บายดักจับการแจ้งเตือนธนาคารและ SMS ในเบื้องหลัง")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            Log.d(TAG, "Foreground service started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service compat", e)
        }
    }

    private val BANK_PACKAGES = setOf(
        "com.kasikorn.kplus",
        "com.kasikornbank.kbiz",
        "com.kasikornbank.kmerchant",
        "com.kasikorn.retail.mbanking.wap",
        "com.scb.phone",
        "th.co.krungthaibank.next",
        "com.bualuang.mbanking",
        "com.krungsri.kma",
        "com.ttbbank.oneapp",
        "gsb.or.th.mymo",
        "com.tdg.truemoneywallet",
        "com.garena.android.koalapay",
        "th.co.lhbank.mobilebanking",
        "com.uob.mightyth",
        "th.co.cimbthai.clicks"
    )

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification Listener Connected!")
        startForegroundCompat()
        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                db.forwardLogDao().insertLog(
                    com.example.data.ForwardLog(
                        type = "NOTIFICATION",
                        sender = "SYSTEM",
                        message = "ระบบดักจับแจ้งเตือน (Notification Listener) เชื่อมต่อสำเร็จแล้ว!",
                        status = "SUCCESS",
                        responseMessage = "แอปตรวจจับข้อความและแจ้งเตือนธนาคารทำงานอยู่ในเบื้องหลังแล้ว"
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error logging connection success", e)
            }
        }
    }

    private fun isBankPackage(packageName: String): Boolean {
        if (BANK_PACKAGES.contains(packageName)) return true
        val lowerPkg = packageName.lowercase()
        val bankKeywords = listOf(
            "kasikorn", "kplus", "kbiz", "kmerchant", "scb.phone", "krungthai", 
            "bualuang", "mbanking", "krungsri", "ttbbank", "mymo", "gsb.or.th", 
            "truemoney", "koalapay", "shopeepay", "lhbank", "uob", "cimb"
        )
        return bankKeywords.any { lowerPkg.contains(it) }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        
        // Skip our own app notifications to avoid loops
        if (packageName == applicationContext.packageName) return

        // Only process known bank apps or packages with bank keywords (supporting dual/cloned apps)
        val isKnownBank = isBankPackage(packageName)
        if (!isKnownBank) return

        serviceScope.launch {
                    try {
                        val db = AppDatabase.getDatabase(applicationContext)
                        val config = AppDatabase.getMergedConfig(db.configDao().getConfigDirect())
                        val extras = sbn.notification.extras ?: android.os.Bundle()

                // Resolve a human-friendly sender name (e.g., K PLUS, SCB EASY, etc. or fallback to Title/Package)
                val appLabel = try {
                    val pm = packageManager
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    pm.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    null
                }

                val resolvedSender = when {
                            packageName.contains("kasikorn.kplus", ignoreCase = true) || packageName.contains("kasikorn.retail.mbanking.wap", ignoreCase = true) -> "KBank"
                            packageName.contains("kasikornbank.kbiz", ignoreCase = true) -> "KBiz"
                            packageName.contains("kasikornbank.kmerchant", ignoreCase = true) -> "KMerchant"
                            packageName.contains("scb.phone", ignoreCase = true) -> "SCB"
                            packageName.contains("krungthaibank.next", ignoreCase = true) -> "Krungthai"
                            packageName.contains("bualuang.mbanking", ignoreCase = true) -> "Bualuang"
                            packageName.contains("krungsri.kma", ignoreCase = true) -> "Krungsri"
                            packageName.contains("ttbbank.oneapp", ignoreCase = true) -> "ttb"
                            packageName.contains("gsb.or.th.mymo", ignoreCase = true) -> "MyMo"
                            packageName.contains("tdg.truemoneywallet", ignoreCase = true) -> "TrueMoney"
                            packageName.contains("garena.android.koalapay", ignoreCase = true) -> "ShopeePay"
                            packageName.contains("lhbank.mobilebanking", ignoreCase = true) -> "LHB"
                            packageName.contains("uob.mightyth", ignoreCase = true) -> "UOB"
                            packageName.contains("cimbthai.clicks", ignoreCase = true) -> "CIMB"
                            else -> when {
                                !appLabel.isNullOrBlank() -> appLabel
                                else -> packageName
                            }
                        }

                // Dump all extras keys to standard format for debugging/troubleshooting
                val extrasDebug = try {
                    val sb = StringBuilder()
                    extras.keySet().forEach { key ->
                        val value = extras.get(key)
                        val valStr = value?.toString() ?: "null"
                        val safeVal = if (valStr.length > 80) valStr.take(80) + "..." else valStr
                        sb.append("$key=$safeVal; ")
                    }
                    sb.toString()
                } catch (e: Exception) {
                    "Error dumping extras: ${e.message}"
                }

                // 1. Extract Title safely
                val title = (extras.getCharSequence(Notification.EXTRA_TITLE)
                    ?: extras.getCharSequence(Notification.EXTRA_TITLE_BIG)
                    ?: extras.getString("android.title")
                    ?: sbn.notification.tickerText)?.toString() ?: ""

                // 2. Extract Text Candidates with layered robust fallbacks
                var text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

                if (text.isBlank()) {
                    val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
                    if (!bigText.isNullOrBlank()) {
                        text = bigText
                    }
                }

                if (text.isBlank()) {
                    val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
                    if (!subText.isNullOrBlank()) {
                        text = subText
                    }
                }

                if (text.isBlank()) {
                    val summaryText = extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)?.toString()
                    if (!summaryText.isNullOrBlank()) {
                        text = summaryText
                    }
                }

                if (text.isBlank()) {
                    val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                    if (!lines.isNullOrEmpty()) {
                        text = lines.filterNotNull().joinToString("\n")
                    }
                }

                if (text.isBlank()) {
                    val tickerText = sbn.notification.tickerText?.toString()
                    if (!tickerText.isNullOrBlank()) {
                        text = tickerText
                    }
                }

                // 3. Fallback scan: Deep recursive scan of all possible fields in the extras Bundle if text is still completely blank
                if (text.isBlank()) {
                    val deepCandidates = extractDeepTextFromBundle(extras)
                    val filteredCandidates = deepCandidates.filter {
                        it != title && !it.equals(title, ignoreCase = true)
                    }
                    if (filteredCandidates.isNotEmpty()) {
                        text = filteredCandidates.joinToString("\n")
                    }
                }

                // Combine Title and Text representing what is actually visible on screen
                val combinedMessage = when {
                    title.isNotBlank() && text.isNotBlank() -> {
                        if (text.contains(title, ignoreCase = true)) {
                            text
                        } else {
                            "$title\n$text"
                        }
                    }
                    text.isNotBlank() -> text
                    else -> title
                }

                Log.d(TAG, "Captured bank notification from $packageName. Title: $title, Text: $text, Combined: $combinedMessage")

                // If message is entirely empty, log to db as FAILED so the user can see we captured it but could not extract text
                if (combinedMessage.isBlank()) {
                    db.forwardLogDao().insertLog(
                        com.example.data.ForwardLog(
                            type = "NOTIFICATION",
                            sender = resolvedSender,
                            message = "[ไม่มีเนื้อหาแจ้งเตือน] Extras: $extrasDebug",
                            status = "FAILED",
                            responseMessage = "ข้ามการส่ง: ระบบตรวจพบแจ้งเตือนแต่ไม่พบข้อความข้างใน"
                        )
                    )
                    return@launch
                }

                // Check if notification forwarding is enabled globally. If not, log as FAILED with reason so user sees it in RECENT ACTIVITY
                if (!config.isNotificationForwardEnabled) {
                    db.forwardLogDao().insertLog(
                        com.example.data.ForwardLog(
                            type = "NOTIFICATION",
                            sender = resolvedSender,
                            message = combinedMessage,
                            status = "FAILED",
                            responseMessage = "ข้ามการส่ง: ระบบปิดการดักการแจ้งเตือนไว้ในหน้าหลัก"
                        )
                    )
                    return@launch
                }

                // Check if the source package is one of the selected bank packages in setting tab (supporting loose matching for cloned/dual apps and comma-separated lists)
                        val selectedList = config.selectedBankPackages.split(",").flatMap { it.split(",").map { pkg -> pkg.trim() } }.filter { it.isNotEmpty() }
                        val isSelectedBank = selectedList.any { selectedPkg ->
                            packageName.equals(selectedPkg, ignoreCase = true) ||
                            packageName.lowercase().contains(selectedPkg.lowercase()) ||
                            selectedPkg.lowercase().contains(packageName.lowercase())
                        }

                if (!isSelectedBank) {
                    db.forwardLogDao().insertLog(
                        com.example.data.ForwardLog(
                            type = "NOTIFICATION",
                            sender = resolvedSender,
                            message = combinedMessage,
                            status = "FAILED",
                            responseMessage = "ข้ามการส่ง: ธนาคารนี้ ($packageName) ไม่ถูกเลือกในหน้า 'เลือกธนาคาร'"
                        )
                    )
                    return@launch
                }

                // Forward notification via WebhookSender
                WebhookSender.sendForward(
                    context = applicationContext,
                    type = "NOTIFICATION",
                    sender = resolvedSender,
                    message = combinedMessage
                )

            } catch (e: Exception) {
                Log.e(TAG, "Error handling notification post", e)
                try {
                    val db = AppDatabase.getDatabase(applicationContext)
                    db.forwardLogDao().insertLog(
                        com.example.data.ForwardLog(
                            type = "NOTIFICATION",
                            sender = "SYSTEM_ERROR",
                            message = "เกิดข้อผิดพลาดในการรับแจ้งเตือน: ${e.localizedMessage}",
                            status = "FAILED",
                            responseMessage = e.toString()
                        )
                    )
                } catch (ex: Exception) {
                    // Ignore
                }
            }
        }
    }

    private fun extractDeepTextFromBundle(bundle: Bundle, depth: Int = 0): List<String> {
        if (depth > 10) return emptyList()
        val texts = mutableListOf<String>()
        try {
            for (key in bundle.keySet()) {
                val value = bundle.get(key) ?: continue
                when (value) {
                    is Bundle -> {
                        texts.addAll(extractDeepTextFromBundle(value, depth + 1))
                    }
                    is CharSequence -> {
                        val str = value.toString().trim()
                        if (str.isNotBlank() && !isIgnoredString(str, key)) {
                            texts.add(str)
                        }
                    }
                    is Array<*> -> {
                        for (item in value) {
                            if (item is Bundle) {
                                texts.addAll(extractDeepTextFromBundle(item, depth + 1))
                            } else if (item is CharSequence) {
                                val str = item.toString().trim()
                                if (str.isNotBlank() && !isIgnoredString(str, key)) {
                                    texts.add(str)
                                }
                            }
                        }
                    }
                    is List<*> -> {
                        for (item in value) {
                            if (item is Bundle) {
                                texts.addAll(extractDeepTextFromBundle(item, depth + 1))
                            } else if (item is CharSequence) {
                                val str = item.toString().trim()
                                if (str.isNotBlank() && !isIgnoredString(str, key)) {
                                    texts.add(str)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return texts.distinct()
    }

    private fun isIgnoredString(value: String, key: String): Boolean {
        val ignoredKeys = setOf(
            "android.template",
            "android.appInfo",
            "android.audioContents",
            "android.intent",
            "android.pendingIntent"
        )
        if (ignoredKeys.contains(key)) return true
        if (value == "true" || value == "false") return true
        if (value.startsWith("com.") || value.contains(".android.")) return true
        return false
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification Listener Disconnected! Requesting rebind...")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                requestRebind(android.content.ComponentName(this, BankNotificationListenerService::class.java))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to rebind on disconnect", e)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }

    companion object {
        fun isServiceEnabled(context: android.content.Context): Boolean {
            val pkgName = context.packageName
            val flat = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            if (!flat.isNullOrEmpty()) {
                val names = flat.split(":")
                for (name in names) {
                    val cn = android.content.ComponentName.unflattenFromString(name)
                    if (cn != null && cn.packageName == pkgName) {
                        return true
                    }
                }
            }
            return false
        }

        fun forceRebind(context: android.content.Context) {
            try {
                if (isServiceEnabled(context)) {
                    val componentName = android.content.ComponentName(
                        context,
                        BankNotificationListenerService::class.java
                    )
                    
                    // Programmatic toggle of the service component to force Android OS to rebind the NotificationListener
                    val pm = context.packageManager
                    pm.setComponentEnabledSetting(
                        componentName,
                        android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        android.content.pm.PackageManager.DONT_KILL_APP
                    )
                    
                    pm.setComponentEnabledSetting(
                        componentName,
                        android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        android.content.pm.PackageManager.DONT_KILL_APP
                    )

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        requestRebind(componentName)
                    }
                    Log.d("BankNotificationListener", "Force rebind & component toggle completed successfully")
                }
            } catch (e: Exception) {
                Log.e("BankNotificationListener", "Failed to force rebind", e)
            }
        }
    }
}
