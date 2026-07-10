package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AppUser
import com.example.data.Configuration
import com.example.data.ForwardLog
import com.example.data.WithdrawJob
import com.example.network.AuthApi
import com.example.network.WebhookSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(private val database: AppDatabase) : ViewModel() {

    private val configDao = database.configDao()
    private val logDao = database.forwardLogDao()
    private val withdrawDao = database.withdrawJobDao()
    private val appUserDao = database.appUserDao()

    // Configuration flow
    val configState: StateFlow<Configuration> = configDao.getConfig()
        .map { config ->
            val defaultConfig = Configuration()
            val existingConfig = config ?: defaultConfig
            
            // Merge existing selected packages with default packages (add any new default packages)
            val existingPackages = existingConfig.selectedBankPackages.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val defaultPackages = defaultConfig.selectedBankPackages.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val mergedPackages = (existingPackages + defaultPackages).distinct()
            
            val mergedConfig = existingConfig.copy(
                selectedBankPackages = mergedPackages.joinToString(",")
            )
            
            // If merged config is different from existing, save it back to database
            if (config == null || mergedConfig.selectedBankPackages != existingConfig.selectedBankPackages) {
                viewModelScope.launch {
                    configDao.insertConfig(mergedConfig)
                }
            }
            
            mergedConfig
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = Configuration() // Fallback/Initial default config
        )

    // Logs flow
    val logsState: StateFlow<List<ForwardLog>> = logDao.getAllLogs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Withdraw Jobs flow
    val withdrawJobsState: StateFlow<List<WithdrawJob>> = withdrawDao.getAllJobs()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // App User flow
    val appUserState: StateFlow<AppUser?> = appUserDao.getAppUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    private val _isTestingWebhook = MutableStateFlow(false)
    val isTestingWebhook: StateFlow<Boolean> = _isTestingWebhook.asStateFlow()

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult: StateFlow<String?> = _testResult.asStateFlow()

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    fun login(username: String) {
        viewModelScope.launch {
            _isLoggingIn.value = true
            _loginError.value = null

            val result = AuthApi.login(username)
            if (result.success) {
                appUserDao.insertAppUser(
                    AppUser(
                        id = 1,
                        username = result.username ?: username,
                        expiresAt = result.expiresAt
                    )
                )
            } else {
                _loginError.value = result.message
            }

            _isLoggingIn.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            appUserDao.clearAppUser()
        }
    }

    fun updateSmsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = configState.value
            configDao.insertConfig(current.copy(isSmsForwardEnabled = enabled))
        }
    }

    fun updateNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = configState.value
            configDao.insertConfig(current.copy(isNotificationForwardEnabled = enabled))
        }
    }

    fun saveConfig(webhookUrl: String, token: String) {
        viewModelScope.launch {
            val current = configState.value
            configDao.insertConfig(current.copy(webhookUrl = webhookUrl, token = token))
        }
    }

    fun updateSelectedBanks(selectedPackages: List<String>) {
        viewModelScope.launch {
            val current = configState.value
            // Deduplicate and filter out empty strings
            val uniquePackages = selectedPackages.map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            val packagesString = uniquePackages.joinToString(",")
            configDao.insertConfig(current.copy(selectedBankPackages = packagesString))
        }
    }

    fun clearAllLogs() {
        viewModelScope.launch {
            logDao.clearLogs()
        }
    }

    fun sendTestWebhook(context: Context) {
        viewModelScope.launch {
            _isTestingWebhook.value = true
            _testResult.value = null
            
            try {
                // Get current time formatted in Asia/Bangkok timezone
                val tz = java.util.TimeZone.getTimeZone("Asia/Bangkok")
                val dfDate = java.text.SimpleDateFormat("dd/MM/yy", java.util.Locale.US).apply { timeZone = tz }
                val dfTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.US).apply { timeZone = tz }
                
                val currentTimestamp = System.currentTimeMillis()
                val thaiDate = dfDate.format(java.util.Date(currentTimestamp))
                val thaiTime = dfTime.format(java.util.Date(currentTimestamp))
                
                // Format the test message to look exactly like a real KBank / K PLUS transaction notification with current time.
                // This ensures correct regex parsing on the backend so that it successfully matches the transaction!
                val testMessage = "คุณได้รับโอนเงินจำนวน 3,500.00 บาท จาก นายสมชาย เมื่อ $thaiDate $thaiTime น."

                // Simulate a bank transfer notification for testing
                val success = WebhookSender.sendForward(
                    context = context,
                    type = "NOTIFICATION",
                    sender = "KBank",
                    message = testMessage,
                    ignoreEnabledCheck = true
                )
                if (success) {
                    _testResult.value = "ส่งข้อความทดสอบสำเร็จ! กรุณาตรวจสอบที่ Webhook ของคุณ"
                } else {
                    // Fetch the latest log to see why it failed
                    val latestLog = logDao.getLatestLog()
                    val errorDetail = latestLog?.responseMessage ?: "เซิร์ฟเวอร์ปฏิเสธการเชื่อมต่อหรือ URL ผิดพลาด"
                    _testResult.value = "การส่งข้อความทดสอบล้มเหลว: $errorDetail"
                }
            } catch (e: Exception) {
                _testResult.value = "การทดสอบล้มเหลว: ${e.localizedMessage}"
            } finally {
                _isTestingWebhook.value = false
            }
        }
    }

    fun fetchJobsManual(context: Context) {
        viewModelScope.launch {
            try {
                val current = configState.value
                if (current.token.isBlank()) return@launch
                val list = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    com.example.network.WithdrawApi.fetchWithdrawJobs(current.token)
                }
                if (list.isNotEmpty()) {
                    withdrawDao.insertJobs(list)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateJobStatus(ref: String, status: String, bankRef: String? = null, note: String? = null) {
        viewModelScope.launch {
            val existing = withdrawDao.getJobByRef(ref)
            if (existing != null) {
                withdrawDao.insertJob(existing.copy(status = status, bankRef = bankRef, note = note))
            }
        }
    }

    fun reportJobManual(ref: String, status: String, bankRef: String, note: String) {
        viewModelScope.launch {
            val existing = withdrawDao.getJobByRef(ref) ?: return@launch
            withdrawDao.insertJob(existing.copy(status = "PROCESSING"))
            
            val token = configState.value.token
            val result = kotlinx.coroutines.withContext(Dispatchers.IO) {
                com.example.network.WithdrawApi.reportWithdrawComplete(token, ref, status, bankRef, note)
            }
            
            if (result.first) {
                withdrawDao.insertJob(existing.copy(status = if (status == "success") "SUCCESS" else "FAILED", bankRef = bankRef, note = note))
            } else {
                withdrawDao.insertJob(existing.copy(status = "FAILED", note = "รายงานไม่สำเร็จ: ${result.second}"))
            }
        }
    }

    fun clearAllWithdrawJobs() {
        viewModelScope.launch {
            withdrawDao.clearAllJobs()
        }
    }

    fun addMockWithdrawJob() {
        viewModelScope.launch {
            val mockRef = "WD" + (100000 + (Math.random() * 900000).toInt()).toString()
            val banks = listOf("kbank", "scb", "bbl", "ktb", "tmb")
            val selectedBank = banks.random()
            val mockJob = com.example.data.WithdrawJob(
                ref = mockRef,
                amount = (100..5000).random().toDouble(),
                bankCode = selectedBank,
                accountNumber = (1000000000..9999999999).random().toString(),
                accountName = listOf("นายสมชาย ใจดี", "นางสาวสมศรี มั่งมี", "นายประหยัด ขยัน", "นางกริชดา เลิศล้ำ").random(),
                status = "PENDING",
                isSimulated = true,
                timestamp = System.currentTimeMillis()
            )
            withdrawDao.insertJob(mockJob)
            com.example.service.BankAutomationService.logToRobot("สร้างรายการถอนจำลอง: $mockRef สำเร็จ")
        }
    }

    fun executeKBankOpenApiWithdrawal(job: WithdrawJob, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                com.example.service.BankAutomationService.logToRobot("--- เริ่มการโอนเงินด้วย KBank OpenAPI ---")
                updateJobStatus(job.ref, "PROCESSING", null, "กำลังดำเนินรายการด้วย KBank OpenAPI")

                // Step 1: OAuth 2.0 Token
                com.example.service.BankAutomationService.logToRobot("[API ขั้นที่ 1/3] กำลังดึงสิทธิ์ OAuth Token...")
                val oauthResult = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    com.example.network.KBankOpenApi.getAccessToken()
                }

                val token = oauthResult.first
                if (token == null) {
                    com.example.service.BankAutomationService.logToRobot("เกิดข้อผิดพลาด OAuth: ${oauthResult.second}")
                    updateJobStatus(job.ref, "FAILED", null, oauthResult.second)
                    onResult(false, oauthResult.second)
                    return@launch
                }
                com.example.service.BankAutomationService.logToRobot("เชื่อมต่อ OAuth สำเร็จ! Token: Bearer ${token.take(15)}...")

                // Step 2: Verify Data (Inquiry Account)
                com.example.service.BankAutomationService.logToRobot("[API ขั้นที่ 2/3] ตรวจสอบข้อมูลบัญชีผู้รับโอน...")
                val isKBank = job.bankCode.lowercase().contains("kbank") || job.bankCode.lowercase() == "kb"
                val envIdVerify = if (isKBank) "CFT001" else "CFT003"
                val transType = if (isKBank) "I" else "O"
                val uniqueIdSuffix = String.format(java.util.Locale.US, "00000000000000000000%06d", (100000..999999).random())

                val verifyResult = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    com.example.network.KBankOpenApi.verifyData(
                        token = token,
                        envId = envIdVerify,
                        transType = transType,
                        proxyType = "A",
                        proxyValue = job.accountNumber,
                        bankCode = if (isKBank) "004" else "025", // "004" is KBANK as per document
                        amount = job.amount,
                        uniqueIdSuffix = uniqueIdSuffix
                    )
                }

                val rsTransId = verifyResult.first
                if (rsTransId == null) {
                    com.example.service.BankAutomationService.logToRobot("ตรวจสอบบัญชีล้มเหลว: ${verifyResult.second}")
                    updateJobStatus(job.ref, "FAILED", null, verifyResult.second)
                    onResult(false, verifyResult.second)
                    return@launch
                }
                com.example.service.BankAutomationService.logToRobot("บัญชีผู้รับได้รับการตรวจสอบแล้ว! rsTransID: $rsTransId")

                // Step 3: Fund Transfer
                com.example.service.BankAutomationService.logToRobot("[API ขั้นที่ 3/3] สั่งโอนเงิน (Fund Transfer)...")
                val envIdTransfer = if (isKBank) "CFT002" else "CFT004"
                
                val transferResult = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    com.example.network.KBankOpenApi.fundTransfer(
                        token = token,
                        envId = envIdTransfer,
                        rsTransID = rsTransId,
                        uniqueIdSuffix = uniqueIdSuffix
                    )
                }

                val slipNo = transferResult.first
                if (slipNo == null) {
                    com.example.service.BankAutomationService.logToRobot("การสั่งโอนเงิน API ล้มเหลว: ${transferResult.second}")
                    updateJobStatus(job.ref, "FAILED", null, transferResult.second)
                    onResult(false, transferResult.second)
                    return@launch
                }

                com.example.service.BankAutomationService.logToRobot("โอนเงิน API สำเร็จร้อยละ 100! เลขอ้างอิง: $slipNo")
                com.example.service.BankAutomationService.logToRobot("กำลังส่งรายงานผลกลับไปยัง PayGate Webhook...")

                // Report back to central server
                val centralToken = configState.value.token
                val reportResult = kotlinx.coroutines.withContext(Dispatchers.IO) {
                    com.example.network.WithdrawApi.reportWithdrawComplete(
                        token = centralToken,
                        ref = job.ref,
                        status = "success",
                        bankRef = slipNo,
                        note = "โอนผ่านระบบ KBank OpenAPI Sandbox"
                    )
                }

                if (reportResult.first) {
                    com.example.service.BankAutomationService.logToRobot("รายงานผลสำเร็จ! งานหมายเลข ${job.ref} เสร็จสิ้นบริบูรณ์")
                    withdrawDao.insertJob(job.copy(status = "SUCCESS", bankRef = slipNo, note = "โอนด้วย KBank OpenAPI สำเร็จ"))
                    onResult(true, "โอนสำเร็จ เลขอ้างอิง: $slipNo")
                } else {
                    com.example.service.BankAutomationService.logToRobot("รายงานผลไม่สำเร็จ: ${reportResult.second}")
                    withdrawDao.insertJob(job.copy(status = "FAILED", note = "โอนผ่าน API สำเร็จแต่รายงานเข้าระบบส่วนกลางล้มเหลว: ${reportResult.second}"))
                    onResult(false, "โอนสำเร็จ แต่รายงานล้มเหลว: ${reportResult.second}")
                }

            } catch (e: Exception) {
                com.example.service.BankAutomationService.logToRobot("ระบบ OpenAPI เกิดข้อผิดพลาดร้ายแรง: ${e.message}")
                updateJobStatus(job.ref, "FAILED", null, e.localizedMessage)
                onResult(false, e.localizedMessage ?: e.toString())
            }
        }
    }

    fun clearTestResult() {
        _testResult.value = null
    }

    fun clearLoginError() {
        _loginError.value = null
    }
}

// ViewModel Factory
class MainViewModelFactory(private val database: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
