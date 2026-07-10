package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.data.AppDatabase
import com.example.data.WithdrawJob
import com.example.network.WithdrawApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class BankAutomationService : AccessibilityService() {

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BankAutomationService Created")
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "BankAutomationService Destroyed")
        instance = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "BankAutomationService Connected!")
        logToRobot("ระบบ Automation เชื่อมต่อเรียบร้อยแล้ว แสตนด์บายรองาน...")
    }

    override fun onInterrupt() {
        Log.w(TAG, "Automation service interrupted")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val sJob = activeJob ?: return
        val rootNode = rootInActiveWindow ?: return

        // Check foreground package
        val packageName = event.packageName?.toString() ?: ""
        
        // Match either our own app (for Sandbox Simulation) or actual banking apps
        if (packageName == this.packageName || packageName == "com.example") {
            processSimulatorAutomation(rootNode, sJob)
        } else if (packageName.contains("kasikorn.kplus") || packageName.contains("kplus")) {
            processRealKBankAutomation(rootNode, sJob)
        } else if (packageName.contains("scb.phone")) {
            processRealSCBAutomation(rootNode, sJob)
        }
    }

    /**
     * Automatic node traversal and execution for our Sandbox EasyBank Simulator screen.
     */
    private fun processSimulatorAutomation(root: AccessibilityNodeInfo, job: WithdrawJob) {
        // Step 1: Detect if we are on the simulator form input screen
        val isSimulatorScreen = findNodeByText(root, "EasyBank Simulator - กรอกข้อมูล") != null
        if (isSimulatorScreen) {
            var actionTaken = false

            // Set account number
            val accField = findNodeByViewIdOrHint(root, "sim_account_input", "ระบุเลขบัญชี 10 หลัก")
            if (accField != null && accField.text?.toString() != job.accountNumber) {
                logToRobot("กำลังกรอกเลขบัญชี: ${job.accountNumber}")
                val arguments = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, job.accountNumber)
                }
                accField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                actionTaken = true
            }

            // Set amount
            val amountField = findNodeByViewIdOrHint(root, "sim_amount_input", "ระบุจำนวนเงิน")
            val amountStr = String.format(Locale.US, "%.2f", job.amount)
            if (amountField != null && amountField.text?.toString() != amountStr) {
                logToRobot("กำลังกรอกจำนวนเงิน: $amountStr บาท")
                val arguments = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, amountStr)
                }
                amountField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                actionTaken = true
            }

            // Set bank selection if it is a spinner/text
            val bankField = findNodeByViewIdOrHint(root, "sim_bank_input", "ระบุธนาคารปลายทาง")
            if (bankField != null && bankField.text?.toString() != job.bankCode) {
                val arguments = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, job.bankCode.uppercase())
                }
                bankField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                actionTaken = true
            }

            // Click the Transfer button if fields are filled
            if (accField?.text?.toString() == job.accountNumber && amountField?.text?.toString() == amountStr) {
                val transferBtn = findNodeByText(root, "คลิกเพื่อโอนเงิน") ?: findNodeByText(root, "โอนเงิน")
                if (transferBtn != null && transferBtn.isEnabled) {
                    logToRobot("กรอกข้อมูลครบถ้วน กำลังคลิกปุ่ม 'โอนเงิน' เพื่อยืนยัน...")
                    transferBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
            }
        }

        // Step 2: Detect if we are on the confirmation screen of the simulator
        val isConfirmScreen = findNodeByText(root, "EasyBank Simulator - ยืนยัน") != null
        if (isConfirmScreen) {
            val confirmBtn = findNodeByText(root, "กดยืนยันการโอนเงินจริง") ?: findNodeByText(root, "ยืนยัน")
            if (confirmBtn != null) {
                logToRobot("ตรวจพบหน้ารีวิวข้อมูล กำลังคลิก 'ยืนยันการโอนเงิน' เพื่อเสร็จสิ้น...")
                confirmBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
        }

        // Step 3: Detect completion/slip page of the simulator, extract slip ref and report
        val isSuccessScreen = findNodeByText(root, "โอนเงินเสร็จสมบูรณ์") != null
        if (isSuccessScreen) {
            val slipRefNode = findNodeByContainsText(root, "เลขที่อ้างอิง:")
            if (slipRefNode != null) {
                val fullText = slipRefNode.text?.toString() ?: ""
                val slipNo = fullText.substringAfter("เลขที่อ้างอิง:").trim()
                
                if (slipNo.isNotEmpty() && slipNo != lastProcessedSlip) {
                    lastProcessedSlip = slipNo
                    logToRobot("โอนสำเร็จจริง! ดึงเลขอ้างอิงสลิปสำเร็จ: $slipNo")
                    completeActiveJob(job, "success", slipNo, "โอนสำเร็จผ่านระบบอัตโนมัติ")
                }
            }
        }
    }

    /**
     * Node-traversal algorithm for Kasikorn Bank (K PLUS) transfer automation.
     */
    private fun processRealKBankAutomation(root: AccessibilityNodeInfo, job: WithdrawJob) {
        // K PLUS automation search queries
        // Find fields with hints like "เลขที่บัญชี" or "บัญชีผู้รับโอน"
        val accField = findNodeByContainsText(root, "เลขที่บัญชี") ?: findNodeByContainsText(root, "ระบุเลขบัญชี")
        if (accField != null && accField.isEditable && accField.text?.toString() != job.accountNumber) {
            logToRobot("[K PLUS] กำลังกรอกเลขบัญชีปลายทาง: ${job.accountNumber}")
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, job.accountNumber)
            }
            accField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }

        val amountField = findNodeByContainsText(root, "จำนวนเงิน") ?: findNodeByContainsText(root, "ระบุจำนวนเงิน")
        val amountStr = String.format(Locale.US, "%.2f", job.amount)
        if (amountField != null && amountField.isEditable && amountField.text?.toString() != amountStr) {
            logToRobot("[K PLUS] กำลังกรอกจำนวนเงิน: $amountStr บาท")
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, amountStr)
            }
            amountField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }

        // Click next/confirm button
        val nextBtn = findNodeByText(root, "ต่อไป") ?: findNodeByText(root, "ถัดไป") ?: findNodeByText(root, "ตกลง")
        if (nextBtn != null && nextBtn.isClickable) {
            logToRobot("[K PLUS] กำลังคลิกดำเนินการต่อ...")
            nextBtn.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }

    /**
     * Node-traversal algorithm for Siam Commercial Bank (SCB EASY) transfer automation.
     */
    private fun processRealSCBAutomation(root: AccessibilityNodeInfo, job: WithdrawJob) {
        val accField = findNodeByContainsText(root, "เลขที่บัญชีผู้รับ") ?: findNodeByContainsText(root, "บัญชีผู้รับโอน")
        if (accField != null && accField.isEditable && accField.text?.toString() != job.accountNumber) {
            logToRobot("[SCB EASY] กรอกเลขบัญชีปลายทาง...")
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, job.accountNumber)
            }
            accField.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
        }
    }

    /**
     * Deep search utilities for accessibility trees
     */
    private fun findNodeByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val list = node.findAccessibilityNodeInfosByText(text)
        if (!list.isNullOrEmpty()) {
            for (item in list) {
                if (item.text?.toString() == text) return item
            }
        }
        
        // Fallback recursive
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByText(child, text)
            if (result != null) return result
        }
        return null
    }

    private fun findNodeByContainsText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val list = node.findAccessibilityNodeInfosByText(text)
        if (!list.isNullOrEmpty()) {
            return list[0]
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByContainsText(child, text)
            if (result != null) return result
        }
        return null
    }

    private fun findNodeByViewIdOrHint(node: AccessibilityNodeInfo, viewId: String, hintText: String): AccessibilityNodeInfo? {
        // Search by ID (including package namespace)
        val fullId = "$packageName:id/$viewId"
        val listById = node.findAccessibilityNodeInfosByViewId(fullId)
        if (!listById.isNullOrEmpty()) {
            return listById[0]
        }
        val listByShortId = node.findAccessibilityNodeInfosByViewId(viewId)
        if (!listByShortId.isNullOrEmpty()) {
            return listByShortId[0]
        }

        // Search by hint text
        if (node.hintText?.toString()?.contains(hintText, ignoreCase = true) == true) {
            return node
        }
        if (node.text?.toString()?.contains(hintText, ignoreCase = true) == true) {
            return node
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val result = findNodeByViewIdOrHint(child, viewId, hintText)
            if (result != null) return result
        }
        return null
    }

    private fun completeActiveJob(job: WithdrawJob, status: String, bankRef: String, note: String) {
        activeJob = null // Clear active job

        serviceScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val updatedJob = job.copy(
                status = if (status == "success") "SUCCESS" else "FAILED",
                bankRef = bankRef,
                note = note,
                timestamp = System.currentTimeMillis()
            )
            db.withdrawJobDao().insertJob(updatedJob)

            logToRobot("กำลังส่งรายงานผลกลับไปยัง PayGate API (Ref: ${job.ref})...")
            
            // Get current token
            val token = db.configDao().getConfigDirect()?.token ?: "fd49e732c5f5ed78fe5fe38b5f8ac8c2"
            
            val result = WithdrawApi.reportWithdrawComplete(
                token = token,
                ref = job.ref,
                status = status,
                bankRef = bankRef,
                note = note
            )

            if (result.first) {
                logToRobot("ส่งรายงานผลเรียบร้อย! งานสำเร็จอย่างสมบูรณ์.")
            } else {
                logToRobot("ส่งรายงานผลล้มเหลว: ${result.second}. ระบบจะ Reclaim เพื่อให้ลองใหม่ได้")
            }
        }
    }

    companion object {
        private const val TAG = "BankAutomationService"
        private val serviceScope = CoroutineScope(Dispatchers.Default)

        var instance: BankAutomationService? = null
        var activeJob: WithdrawJob? = null
        var lastProcessedSlip: String = ""

        // For logging debug actions to UI
        val robotLogsFlow = kotlinx.coroutines.flow.MutableStateFlow<List<String>>(emptyList())

        fun logToRobot(message: String) {
            Log.d(TAG, "Robot: $message")
            val currentList = robotLogsFlow.value.toMutableList()
            val time = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(java.util.Date())
            currentList.add(0, "[$time] $message")
            if (currentList.size > 100) {
                currentList.removeAt(currentList.lastIndex)
            }
            robotLogsFlow.value = currentList
        }

        fun isServiceRunning(): Boolean {
            return instance != null
        }
    }
}
