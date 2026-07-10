package com.example.ui

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.WithdrawJob
import com.example.service.BankAutomationService
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WithdrawAutomationTab(viewModel: MainViewModel) {
    val context = LocalContext.current
    val jobs by viewModel.withdrawJobsState.collectAsState()
    val config by viewModel.configState.collectAsState()
    val robotLogs by BankAutomationService.robotLogsFlow.collectAsState()
    
    var isAccessibilityEnabled by remember { mutableStateOf(false) }
    var showManualReportDialog by remember { mutableStateOf<WithdrawJob?>(null) }
    var showSimulatorDialog by remember { mutableStateOf<WithdrawJob?>(null) }

    // Periodically check if Accessibility Service is enabled
    LaunchedEffect(Unit) {
        while (true) {
            isAccessibilityEnabled = isAccessibilityServiceEnabled(context, BankAutomationService::class.java)
            kotlinx.coroutines.delay(1500)
        }
    }

    // Auto-trigger simulator dialog if active job is set and is simulated
    LaunchedEffect(Unit) {
        while (true) {
            val active = BankAutomationService.activeJob
            if (active != null && active.isSimulated && showSimulatorDialog == null) {
                showSimulatorDialog = active
            }
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // 1. Accessibility Service & Poller Status Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (isAccessibilityEnabled) Color(0x1610B981) else Color(0x1AEF4444),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Android,
                                contentDescription = "Accessibility",
                                tint = if (isAccessibilityEnabled) LimeGreen else ErrorRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ระบบโอนเงินอัตโนมัติ (Accessibility API)",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isAccessibilityEnabled) "สถานะ: ทำงานร่วมกับแอปธนาคารสำเร็จ" else "สถานะ: ยังไม่ได้เปิดบริการคลิกแทนคุณ",
                                color = if (isAccessibilityEnabled) LimeGreen else TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            } catch (e: Exception) {
                                Toast.makeText(context, "ไม่สามารถเปิดหน้าตั้งค่าการช่วยเหลือได้", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAccessibilityEnabled) Color(0x1610B981) else WarningYellow
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (isAccessibilityEnabled) "เชื่อมต่ออยู่" else "กดเปิดสิทธิ์",
                            color = if (isAccessibilityEnabled) LimeGreen else SlateDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 2. Control Quick Actions (Fetch, Create Mock, Clear)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { 
                    viewModel.fetchJobsManual(context)
                    Toast.makeText(context, "ดึงงานจากระบบ...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1.2f).height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = "ดึงงานทันที", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("ดึงงานถอน", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { viewModel.addMockWithdrawJob() },
                modifier = Modifier.weight(1.5f).height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1610B981)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0x3310B981))
            ) {
                Icon(Icons.Filled.Add, contentDescription = "สร้างรายการจำลอง", tint = LimeGreen, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("สร้างงานถอนจำลอง", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = LimeGreen)
            }

            Button(
                onClick = { viewModel.clearAllWithdrawJobs() },
                modifier = Modifier.weight(0.9f).height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AEF4444)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0x33EF4444))
            ) {
                Icon(Icons.Filled.DeleteSweep, contentDescription = "ล้างข้อมูล", tint = ErrorRed, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("ล้าง", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ErrorRed)
            }
        }

        // 3. Robot Console Live Logs Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B0F19)),
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ROBOT AUTOMATION CONSOLE (LOGS)",
                        color = Color(0x80FFFFFF),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(if (isAccessibilityEnabled) LimeGreen else ErrorRed, CircleShape)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                if (robotLogs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "ไม่มีข้อมูลการทำงาน, สแตนด์บายดักจับการทำงานอัตโนมัติ...",
                            color = Color(0x4DFFFFFF),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(robotLogs) { logText ->
                            Text(
                                text = logText,
                                color = if (logText.contains("สำเร็จ") || logText.contains("success")) LimeGreen else if (logText.contains("ล้มเหลว") || logText.contains("error")) ErrorRed else Color(0xFFE2E8F0),
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // 4. Withdrawal Jobs List Section
        Text(
            text = "รายการคำขอถอนเงินรออนุมัติ (${jobs.size})",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        if (jobs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(SlateCard, RoundedCornerShape(24.dp))
                    .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(24.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AccountBalance,
                        contentDescription = "No jobs",
                        tint = TextSecondary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "ไม่มีประวัติรายการถอนเงินค้างคา",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ระบบจะทำการดาวน์โหลดงานอัตโนมัติทุก 20 วินาที หรือคุณสามารถกดสร้างรายการจำลองเพื่อทดสอบได้ทันที",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(jobs) { job ->
                    WithdrawJobRow(
                        job = job,
                        onAutoTransfer = {
                            if (!isAccessibilityEnabled) {
                                Toast.makeText(context, "กรุณาเปิด Accessibility Service ก่อนใช้ระบบอัตโนมัติ", Toast.LENGTH_LONG).show()
                                try {
                                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                                } catch (e: Exception) {}
                            } else {
                                BankAutomationService.activeJob = job
                                BankAutomationService.logToRobot("เริ่มการโอนเงินอัตโนมัติรายการ ${job.ref}")
                                viewModel.updateJobStatus(job.ref, "PROCESSING", null, "เริ่มดำเนินการระบบอัตโนมัติ")
                                
                                if (job.isSimulated) {
                                    showSimulatorDialog = job
                                } else {
                                    // Launch the real bank app
                                    val bankPkg = when (job.bankCode.lowercase()) {
                                        "kbank" -> "com.kasikorn.kplus"
                                        "scb" -> "com.scb.phone"
                                        "ktb" -> "th.co.krungthaibank.next"
                                        else -> "com.kasikorn.kplus" // Default fallback
                                    }
                                    try {
                                        val intent = context.packageManager.getLaunchIntentForPackage(bankPkg)
                                        if (intent != null) {
                                            context.startActivity(intent)
                                        } else {
                                            Toast.makeText(context, "ไม่พบแอปธนาคาร ${job.bankCode.uppercase()} บนเครื่องนี้", Toast.LENGTH_LONG).show()
                                            viewModel.updateJobStatus(job.ref, "FAILED", null, "ไม่พบแอปธนาคารบนเครื่อง")
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "ไม่สามารถเปิดแอปธนาคารได้: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        onManualReport = {
                            showManualReportDialog = job
                        }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    // Manual reporting Dialog
    if (showManualReportDialog != null) {
        val job = showManualReportDialog!!
        var slipRefInput by remember { mutableStateOf("") }
        var isSuccessStatus by remember { mutableStateOf(true) }
        var manualNote by remember { mutableStateOf("") }

        Dialog(
            onDismissRequest = { showManualReportDialog = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "รายงานผลการโอนเงินแมนนวล (Manual Report)",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )

                    HorizontalDivider(color = GlassBorder)

                    // Job Details
                    Column(
                        modifier = Modifier
                            .background(Color(0x0CFFFFFF), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("อ้างอิง: ${job.ref}", color = AccentBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("ยอดเงิน: ${String.format(Locale.US, "%,.2f", job.amount)} บาท", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("บัญชี: ${job.accountNumber} (${job.bankCode.uppercase()})", color = TextSecondary, fontSize = 13.sp)
                        Text("ชื่อบัญชี: ${job.accountName}", color = TextSecondary, fontSize = 13.sp)
                    }

                    // Success or Failed Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { isSuccessStatus = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSuccessStatus) Color(0x1610B981) else Color(0x0CFFFFFF)
                            ),
                            border = BorderStroke(1.dp, if (isSuccessStatus) LimeGreen else Color.Transparent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("สำเร็จ (Success)", color = if (isSuccessStatus) LimeGreen else TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { isSuccessStatus = false },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isSuccessStatus) Color(0x1AEF4444) else Color(0x0CFFFFFF)
                            ),
                            border = BorderStroke(1.dp, if (!isSuccessStatus) ErrorRed else Color.Transparent),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("ไม่ผ่าน (Failed)", color = if (!isSuccessStatus) ErrorRed else TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Slip Ref input
                    if (isSuccessStatus) {
                        OutlinedTextField(
                            value = slipRefInput,
                            onValueChange = { slipRefInput = it },
                            label = { Text("เลขอ้างอิงจากสลิปธนาคาร (Slip Ref/No.)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LimeGreen,
                                focusedLabelColor = LimeGreen,
                                unfocusedBorderColor = GlassBorder,
                                unfocusedLabelColor = TextSecondary,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Note input
                    OutlinedTextField(
                        value = manualNote,
                        onValueChange = { manualNote = it },
                        label = { Text("บันทึกเพิ่มเติม (Note)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentBlue,
                            focusedLabelColor = AccentBlue,
                            unfocusedBorderColor = GlassBorder,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showManualReportDialog = null },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("ยกเลิก", color = TextSecondary)
                        }

                        Button(
                            onClick = {
                                if (isSuccessStatus && slipRefInput.isBlank()) {
                                    Toast.makeText(context, "กรุณากรอกเลขอ้างอิงสลิปธนาคาร", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                val statusStr = if (isSuccessStatus) "success" else "failed"
                                val finalNote = if (manualNote.isBlank()) {
                                    if (isSuccessStatus) "โอนเงินเรียบร้อยแมนนวล" else "โอนเงินแมนนวลไม่ผ่าน"
                                } else {
                                    manualNote
                                }
                                viewModel.reportJobManual(job.ref, statusStr, slipRefInput, finalNote)
                                showManualReportDialog = null
                                Toast.makeText(context, "กำลังส่งรายงานผลแมนนวลไปยังระบบ...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("ส่งรายงานผล", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // EasyBank Simulator Interactive Dialog
    if (showSimulatorDialog != null) {
        val job = showSimulatorDialog!!
        var simStep by remember { mutableStateOf(1) } // 1: Input Form, 2: Review/Confirm, 3: Success Slip
        var simAccountInput by remember { mutableStateOf("") }
        var simAmountInput by remember { mutableStateOf("") }
        var simBankInput by remember { mutableStateOf("") }
        var simSlipNo by remember { mutableStateOf("") }

        // Setup handlers to auto-progress simulating actual accessibility gestures if automation is listening!
        LaunchedEffect(simStep) {
            if (isAccessibilityEnabled) {
                if (simStep == 1) {
                    // Simulating reading/typing delays
                    Handler(Looper.getMainLooper()).postDelayed({
                        simAccountInput = job.accountNumber
                        simBankInput = job.bankCode.uppercase()
                        simAmountInput = String.format(Locale.US, "%.2f", job.amount)
                        BankAutomationService.logToRobot("[Simulator] ตรวจพบการกรอกและโอนเงินจากหุ่นยนต์...")
                    }, 1200)
                } else if (simStep == 2) {
                    // Robot clicking Confirm
                    Handler(Looper.getMainLooper()).postDelayed({
                        BankAutomationService.logToRobot("[Simulator] หุ่นยนต์กดยืนยันการโอนเงิน...")
                    }, 1500)
                }
            }
        }

        Dialog(
            onDismissRequest = { 
                showSimulatorDialog = null
                BankAutomationService.activeJob = null
            },
            properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .border(BorderStroke(2.dp, LimeGreen), RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)) // Slate dark phone screen
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header mimicking a bank app
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(LimeGreen, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.AccountBalance, contentDescription = "Bank", tint = Color(0xFF0F172A), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("EasyBank Mobile", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0x3310B981))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("SANDBOX MOD", color = LimeGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = Color(0x1AFFFFFF))

                    when (simStep) {
                        1 -> {
                            Text(
                                text = "EasyBank Simulator - กรอกข้อมูล",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // Account Number input
                            OutlinedTextField(
                                value = simAccountInput,
                                onValueChange = { simAccountInput = it },
                                label = { Text("ระบุเลขบัญชี 10 หลัก") },
                                modifier = Modifier.fillMaxWidth().testTag("sim_account_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LimeGreen,
                                    focusedLabelColor = LimeGreen,
                                    unfocusedBorderColor = Color(0x33FFFFFF),
                                    unfocusedLabelColor = Color(0x80FFFFFF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Bank code input
                            OutlinedTextField(
                                value = simBankInput,
                                onValueChange = { simBankInput = it },
                                label = { Text("ระบุธนาคารปลายทาง") },
                                modifier = Modifier.fillMaxWidth().testTag("sim_bank_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LimeGreen,
                                    focusedLabelColor = LimeGreen,
                                    unfocusedBorderColor = Color(0x33FFFFFF),
                                    unfocusedLabelColor = Color(0x80FFFFFF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            // Amount Input
                            OutlinedTextField(
                                value = simAmountInput,
                                onValueChange = { simAmountInput = it },
                                label = { Text("ระบุจำนวนเงิน") },
                                modifier = Modifier.fillMaxWidth().testTag("sim_amount_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = LimeGreen,
                                    focusedLabelColor = LimeGreen,
                                    unfocusedBorderColor = Color(0x33FFFFFF),
                                    unfocusedLabelColor = Color(0x80FFFFFF),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    simStep = 2
                                },
                                modifier = Modifier.fillMaxWidth().testTag("sim_submit_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = LimeGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("คลิกเพื่อโอนเงิน", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                            }
                        }
                        2 -> {
                            Text(
                                text = "EasyBank Simulator - ยืนยัน",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x0AFFFFFF), RoundedCornerShape(12.dp))
                                    .border(BorderStroke(1.dp, Color(0x1AFFFFFF)), RoundedCornerShape(12.dp))
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("โปรดตรวจสอบข้อมูลการโอนเงินปลายทาง", color = Color(0x80FFFFFF), fontSize = 11.sp)
                                HorizontalDivider(color = Color(0x0AFFFFFF))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("ธนาคารผู้รับ:", color = Color(0x80FFFFFF), fontSize = 12.sp)
                                    Text(simBankInput, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("เลขที่บัญชี:", color = Color(0x80FFFFFF), fontSize = 12.sp)
                                    Text(simAccountInput, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("ชื่อบัญชี:", color = Color(0x80FFFFFF), fontSize = 12.sp)
                                    Text(job.accountName, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("จำนวนเงินโอน:", color = Color(0x80FFFFFF), fontSize = 12.sp)
                                    Text("$simAmountInput THB", color = LimeGreen, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    simSlipNo = "EB" + SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date()) + "-" + (100000 + (Math.random() * 900000).toInt()).toString()
                                    simStep = 3
                                },
                                modifier = Modifier.fillMaxWidth().testTag("sim_confirm_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = LimeGreen),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("กดยืนยันการโอนเงินจริง", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                            }
                        }
                        3 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0x1610B981), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = "Success", tint = LimeGreen, modifier = Modifier.size(32.dp))
                                }

                                Text(
                                    text = "โอนเงินเสร็จสมบูรณ์",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x05FFFFFF), RoundedCornerShape(12.dp))
                                        .padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text("ธนาคาร EasyBank ได้โอนเงินสำเร็จแล้ว", color = Color(0x80FFFFFF), fontSize = 11.sp)
                                    Text("ผู้รับโอน: ${job.accountName}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("จำนวนเงิน: $simAmountInput THB", color = LimeGreen, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "เลขที่อ้างอิง: $simSlipNo",
                                        color = Color(0xCCFFFFFF),
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.testTag("sim_slip_text")
                                    )
                                }

                                Button(
                                    onClick = { 
                                        showSimulatorDialog = null
                                        BankAutomationService.activeJob = null
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x33FFFFFF)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("ปิดหน้าต่าง", color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WithdrawJobRow(
    job: WithdrawJob,
    onAutoTransfer: () -> Unit,
    onManualReport: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("withdraw_job_row_${job.ref}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Ref, Bank, status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = job.ref,
                        color = AccentBlue,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (job.isSimulated) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x1610B981))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("SIMULATED", color = LimeGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Status Badge
                val (badgeColor, badgeText, badgeBg) = when (job.status) {
                    "PENDING" -> Triple(WarningYellow, "รอดำเนินการ", Color(0x1AFFCC00))
                    "PROCESSING" -> Triple(AccentBlue, "กำลังโอนเงิน...", Color(0x1A2563EB))
                    "SUCCESS" -> Triple(LimeGreen, "โอนเงินสำเร็จ", Color(0x1A10B981))
                    "FAILED" -> Triple(ErrorRed, "โอนไม่ผ่าน", Color(0x1AEF4444))
                    else -> Triple(TextSecondary, job.status, Color(0x0CFFFFFF))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(badgeBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = badgeText, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Body info (Target account name, accountNumber, Bank Code, amount)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = job.accountName,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${job.accountNumber} • ${job.bankCode.uppercase()}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                // Amount display
                Text(
                    text = "${String.format(Locale.US, "%,.2f", job.amount)} ฿",
                    color = if (job.status == "SUCCESS") LimeGreen else TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Displays notes or bank slip ref if present
            if (!job.bankRef.isNullOrBlank() || !job.note.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = GlassBorder.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(8.dp))
                if (!job.bankRef.isNullOrBlank()) {
                    Text(
                        text = "เลขอ้างอิงสลิป: ${job.bankRef}",
                        color = Color(0xCCFFFFFF),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                if (!job.note.isNullOrBlank()) {
                    Text(
                        text = "บันทึก: ${job.note}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Action Buttons
            if (job.status == "PENDING" || job.status == "FAILED") {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Auto transfer trigger
                    Button(
                        onClick = onAutoTransfer,
                        modifier = Modifier.weight(1.3f).height(38.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LimeGreen),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Auto", tint = Color(0xFF0F172A), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("โอนเงินอัตโนมัติ", color = Color(0xFF0F172A), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // 2. Manual report fallback
                    Button(
                        onClick = onManualReport,
                        modifier = Modifier.weight(1f).height(38.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x0CFFFFFF)),
                        border = BorderStroke(1.dp, GlassBorder),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = "Manual", tint = TextPrimary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("รายงานผลแมนนวล", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Checks if a specific Accessibility Service is currently enabled in Android Settings.
 */
fun isAccessibilityServiceEnabled(context: Context, serviceClass: Class<*>): Boolean {
    val expectedComponentName = android.content.ComponentName(context, serviceClass)
    val enabledServicesSetting = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServicesSetting)
    while (colonSplitter.hasNext()) {
        val componentNameString = colonSplitter.next()
        val enabledComponent = android.content.ComponentName.unflattenFromString(componentNameString)
        if (enabledComponent != null && enabledComponent == expectedComponentName) {
            return true
        }
    }
    return false
}
