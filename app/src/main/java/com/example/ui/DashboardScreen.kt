package com.example.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Link
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ForwardLog
import com.example.ui.theme.AccentBlue
import com.example.ui.theme.DarkYellowText
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.LightYellowBg
import com.example.ui.theme.LimeGreen
import com.example.ui.theme.LimeGreenDim
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningYellow
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassBorderLight
import com.example.ui.theme.GlassBgDarker
import com.example.ui.theme.GlassAccentGreen
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Popular Thai banks mapping
data class BankAppInfo(
    val name: String,
    val packageName: String,
    val color: Color,
    val initial: String,
    val fullName: String
)

val THAI_BANKS = listOf(
    BankAppInfo("K PLUS", "com.kasikorn.kplus,com.kasikorn.retail.mbanking.wap", Color(0xFF138F47), "K", "ธนาคารกสิกรไทย (K PLUS)"),
    BankAppInfo("K-Biz", "com.kasikornbank.kbiz", Color(0xFF138F47), "KB", "กสิกรไทย ธุรกิจ (K-Biz)"),
    BankAppInfo("K-Merchant", "com.kasikornbank.kmerchant", Color(0xFF138F47), "KM", "กสิกรไทย ร้านค้า (K-Merchant)"),
    BankAppInfo("SCB EASY", "com.scb.phone", Color(0xFF4E2A84), "S", "ธนาคารไทยพาณิชย์ (SCB EASY)"),
    BankAppInfo("Krungthai NEXT", "th.co.krungthaibank.next", Color(0xFF00A4E4), "KT", "ธนาคารกรุงไทย (Krungthai NEXT)"),
    BankAppInfo("Bualuang mB", "com.bualuang.mbanking", Color(0xFF003399), "B", "ธนาคารกรุงเทพ (Bualuang mBanking)"),
    BankAppInfo("KMA krungsri", "com.krungsri.kma", Color(0xFFFCBA12), "Kr", "ธนาคารกรุงศรีอยุธยา (KMA)"),
    BankAppInfo("ttb touch", "com.ttbbank.oneapp", Color(0xFF0056FF), "ttb", "ธนาคารทหารไทยธนชาต (ttb touch)"),
    BankAppInfo("MyMo", "gsb.or.th.mymo", Color(0xFFED008C), "M", "ธนาคารออมสิน (MyMo)"),
    BankAppInfo("TrueMoney", "com.tdg.truemoneywallet", Color(0xFFFF5F00), "TM", "ทรูมันนี่ วอลเล็ท (TrueMoney Wallet)"),
    BankAppInfo("ShopeePay", "com.garena.android.koalapay", Color(0xFFFF5722), "SP", "ช้อปปี้เพย์ (ShopeePay)"),
    BankAppInfo("LHB mChoice", "th.co.lhbank.mobilebanking", Color(0xFF53565A), "LH", "ธนาคารแลนด์ แอนด์ เฮ้าส์"),
    BankAppInfo("UOB TMRW", "com.uob.mightyth", Color(0xFF012D74), "U", "ธนาคารยูโอบี (UOB TMRW)"),
    BankAppInfo("CIMB Clicks", "th.co.cimbthai.clicks", Color(0xFFC7082C), "C", "ธนาคารซีไอเอ็มบี ไทย")
)

fun isNotificationServiceEnabled(context: Context): Boolean {
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

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val pkgName = context.packageName
    val flat = android.provider.Settings.Secure.getString(
        context.contentResolver,
        android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
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

fun openAccessibilitySettings(context: Context) {
    try {
        val intent = android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "ไม่สามารถเปิดหน้าตั้งค่า Accessibility ได้", android.widget.Toast.LENGTH_LONG).show()
    }
}

fun isBatteryOptimizationIgnored(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
    return pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
}

fun requestIgnoreBatteryOptimization(context: Context) {
    try {
        val intent = android.content.Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            val intent = android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            context.startActivity(intent)
        } catch (ex: Exception) {
            android.widget.Toast.makeText(context, "ไม่สามารถเปิดหน้าตั้งค่าการประหยัดแบตเตอรี่ได้", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}

fun openAutostartSettings(context: Context) {
    val intentCandidates = listOf(
        android.content.Intent().setComponent(android.content.ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
        android.content.Intent().setComponent(android.content.ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")),
        android.content.Intent().setComponent(android.content.ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")),
        android.content.Intent().setComponent(android.content.ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager")),
        android.content.Intent().setComponent(android.content.ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")),
        android.content.Intent().setComponent(android.content.ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")),
        android.content.Intent().setComponent(android.content.ComponentName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")),
        android.content.Intent().setComponent(android.content.ComponentName("com.samsung.android.sm_cn", "com.samsung.android.sm.ui.ram.AutoRunActivity")),
        android.content.Intent().setComponent(android.content.ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")),
        android.content.Intent().setComponent(android.content.ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"))
    )
    
    for (intent in intentCandidates) {
        try {
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            return
        } catch (e: Exception) {
            // Try next candidate
        }
    }
    try {
        val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
        context.startActivity(intent)
        android.widget.Toast.makeText(context, "กรุณาหาหัวข้อ 'เริ่มต้นอัตโนมัติ' (Autostart) หรือ 'การจัดการแอปเบื้องหลัง'", android.widget.Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "ไม่สามารถเปิดหน้าตั้งค่าได้", android.widget.Toast.LENGTH_LONG).show()
    }
}

@Composable
fun LoginScreen(viewModel: MainViewModel) {
    var username by remember { mutableStateOf("") }
    val isLoggingIn by viewModel.isLoggingIn.collectAsState()
    val loginError by viewModel.loginError.collectAsState()
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDark)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x262563EB), Color.Transparent),
                        center = Offset(size.width * -0.1f, size.height * -0.05f),
                        radius = size.minDimension * 0.8f
                    ),
                    radius = size.minDimension * 0.8f,
                    center = Offset(size.width * -0.1f, size.height * -0.05f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x266366F1), Color.Transparent),
                        center = Offset(size.width * 1.1f, size.height * 1.05f),
                        radius = size.minDimension * 0.9f
                    ),
                    radius = size.minDimension * 0.9f,
                    center = Offset(size.width * 1.1f, size.height * 1.05f)
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x1AFFFFFF))
                        .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "O",
                        color = LimeGreen,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.SansSerif
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "SYSTEM SECURE",
                        color = AccentBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "EasyO Forwarder",
                        color = TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Username Input
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "ชื่อผู้ใช้",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            viewModel.clearLoginError()
                        },
                        placeholder = { Text("กรุณาใส่ชื่อผู้ใช้", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LimeGreen,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = LimeGreen,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )

                    if (loginError != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = loginError!!,
                            color = ErrorRed,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Login Button
            Button(
                onClick = { viewModel.login(username) },
                modifier = Modifier.fillMaxWidth(),
                enabled = username.isNotBlank() && !isLoggingIn,
                colors = ButtonDefaults.buttonColors(containerColor = LimeGreen),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                if (isLoggingIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = SlateDark
                    )
                } else {
                    Text(
                        text = "เข้าสู่ระบบ",
                        color = SlateDark,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MainDashboardApp(viewModel: MainViewModel) {
    val appUser by viewModel.appUserState.collectAsState()

    if (appUser == null) {
        LoginScreen(viewModel = viewModel)
        return
    }
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(0) }
    var hasNotificationPermission by remember { mutableStateOf(isNotificationServiceEnabled(context)) }
    var hasAccessibilityPermission by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }

    // Periodically refresh permission checks and force rebind if enabled
    LaunchedEffect(Unit) {
        while (true) {
            hasNotificationPermission = isNotificationServiceEnabled(context)
            hasAccessibilityPermission = isAccessibilityServiceEnabled(context)
            if (hasNotificationPermission) {
                com.example.service.BankNotificationListenerService.forceRebind(context)
            }
            kotlinx.coroutines.delay(2000)
        }
    }

    val config by viewModel.configState.collectAsState()
    val logs by viewModel.logsState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDark)
            .drawBehind {
                // Top-left soft blue gradient blur (mesh decorative element)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x262563EB), Color.Transparent),
                        center = Offset(size.width * -0.1f, size.height * -0.05f),
                        radius = size.minDimension * 0.8f
                    ),
                    radius = size.minDimension * 0.8f,
                    center = Offset(size.width * -0.1f, size.height * -0.05f)
                )
                // Bottom-right soft indigo gradient blur (mesh decorative element)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0x266366F1), Color.Transparent),
                        center = Offset(size.width * 1.1f, size.height * 1.05f),
                        radius = size.minDimension * 0.9f
                    ),
                    radius = size.minDimension * 0.9f,
                    center = Offset(size.width * 1.1f, size.height * 1.05f)
                )
            }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0x0F1E293B), // Translucent dark
                    modifier = Modifier
                        .testTag("bottom_nav_bar")
                        .drawBehind {
                            // Top border representing glass refraction
                            drawLine(
                                color = GlassBorder,
                                start = Offset(0f, 0f),
                                end = Offset(size.width, 0f),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(imageVector = if (currentTab == 0) Icons.Filled.CheckCircle else Icons.Outlined.Home, contentDescription = "หน้าหลัก") },
                        label = { Text("หน้าหลัก", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = LimeGreen,
                            selectedTextColor = LimeGreen,
                            indicatorColor = Color(0x19FFFFFF),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(imageVector = Icons.Outlined.Link, contentDescription = "ตั้งค่าที่อยู่") },
                        label = { Text("เปลี่ยนที่อยู่", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = LimeGreen,
                            selectedTextColor = LimeGreen,
                            indicatorColor = Color(0x19FFFFFF),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = { Icon(imageVector = Icons.Outlined.Devices, contentDescription = "เลือกธนาคาร") },
                        label = { Text("เลือกธนาคาร", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = LimeGreen,
                            selectedTextColor = LimeGreen,
                            indicatorColor = Color(0x19FFFFFF),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = { Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = "โอนเงินออโต้") },
                        label = { Text("โอนเงินออโต้", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = LimeGreen,
                            selectedTextColor = LimeGreen,
                            indicatorColor = Color(0x19FFFFFF),
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Brand Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x1AFFFFFF))
                                .border(BorderStroke(1.dp, GlassBorder), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "O",
                                color = LimeGreen,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "SYSTEM SECURE",
                                color = AccentBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "EasyO Forwarder",
                                color = TextPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ผู้ใช้: ${appUser?.username ?: ""}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Monitoring status badge
                        val liveMonitoring = config.isSmsForwardEnabled || config.isNotificationForwardEnabled
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (liveMonitoring) Color(0x1A10B981) else Color(0x1AEF4444))
                                .border(BorderStroke(1.dp, if (liveMonitoring) Color(0x3310B981) else Color(0x33EF4444)), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (liveMonitoring) GlassAccentGreen else ErrorRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (liveMonitoring) "LIVE MONITORING" else "PAUSED",
                                color = if (liveMonitoring) GlassAccentGreen else ErrorRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        // Logout Button
                        Button(
                            onClick = { viewModel.logout() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AEF4444)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "ออก",
                                color = ErrorRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                HorizontalDivider(color = GlassBorder, thickness = 1.dp)

            // Dynamic Content Tabs
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentTab) {
                    0 -> DashboardTab(
                        viewModel = viewModel,
                        hasNotificationPermission = hasNotificationPermission,
                        hasAccessibilityPermission = hasAccessibilityPermission,
                        logs = logs,
                        smsEnabled = config.isSmsForwardEnabled,
                        notificationEnabled = config.isNotificationForwardEnabled,
                        webhookUrl = config.webhookUrl,
                        token = config.token,
                        onSmsToggle = { viewModel.updateSmsEnabled(it) },
                        onNotificationToggle = { enabled ->
                            viewModel.updateNotificationEnabled(enabled)
                            if (enabled) {
                                com.example.service.BankNotificationListenerService.forceRebind(context)
                            }
                        },
                        onGrantPermissionClick = {
                            try {
                                context.startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "ไม่สามารถเปิดหน้าตั้งค่าได้", Toast.LENGTH_LONG).show()
                            }
                        },
                        onGrantAccessibilityClick = { openAccessibilitySettings(context) }
                    )
                    1 -> SettingsTab(
                        viewModel = viewModel,
                        currentUrl = config.webhookUrl,
                        currentToken = config.token,
                        onSave = { url, token ->
                            viewModel.saveConfig(url, token)
                            Toast.makeText(context, "บันทึกที่อยู่เรียบร้อยแล้ว", Toast.LENGTH_SHORT).show()
                        }
                    )
                    2 -> AppSelectionTab(
                        selectedBanksString = config.selectedBankPackages,
                        onBanksChanged = { selectedList ->
                            viewModel.updateSelectedBanks(selectedList)
                            com.example.service.BankNotificationListenerService.forceRebind(context)
                        }
                    )
                    3 -> WithdrawAutomationTab(
                        viewModel = viewModel
                    )
                }
            }
            } // Close Column from line 510
        }
    }
}

@Composable
fun DashboardTab(
    viewModel: MainViewModel,
    hasNotificationPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    logs: List<ForwardLog>,
    smsEnabled: Boolean,
    notificationEnabled: Boolean,
    webhookUrl: String,
    token: String,
    onSmsToggle: (Boolean) -> Unit,
    onNotificationToggle: (Boolean) -> Unit,
    onGrantPermissionClick: () -> Unit,
    onGrantAccessibilityClick: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    var isBatteryIgnored by remember { mutableStateOf(isBatteryOptimizationIgnored(context)) }

    LaunchedEffect(Unit) {
        while (true) {
            isBatteryIgnored = isBatteryOptimizationIgnored(context)
            kotlinx.coroutines.delay(2000)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // 1. Background Run Assistant Panel (Highly Requested Custom Feature)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("background_assistant_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0x1610B981), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Background Settings Helper",
                                tint = LimeGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ตัวช่วยการทำงานเบื้องหลัง (Background Run Assistant)",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "สิทธิ์แอดมิน (Admin) ไม่สามารถดักการแจ้งเตือนได้ ต้องเปิดสิทธิ์ด้านล่างนี้แทน",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = GlassBorder.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // 1. Notification Listener Access
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (hasNotificationPermission) LimeGreen else WarningYellow, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "1. สิทธิ์ดักจับการแจ้งเตือน (Notification Access)",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (hasNotificationPermission) "เปิดการเชื่อมต่อระบบดักจับสำเร็จแล้ว" else "จำเป็นต้องเปิดเพื่อให้เห็นแจ้งเตือนธนาคาร",
                                color = if (hasNotificationPermission) LimeGreen else TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                            )
                        }
                        Button(
                            onClick = {
                                if (hasNotificationPermission) {
                                    // Force rebind as a connection test
                                    com.example.service.BankNotificationListenerService.forceRebind(context)
                                    Toast.makeText(context, "ทดสอบกระตุ้นระบบดักจับสำเร็จ!", Toast.LENGTH_SHORT).show()
                                } else {
                                    onGrantPermissionClick()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasNotificationPermission) Color(0x1610B981) else WarningYellow
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = if (hasNotificationPermission) "เชื่อมต่อใหม่" else "กดเปิดสิทธิ์",
                                color = if (hasNotificationPermission) LimeGreen else SlateDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Ignore Battery Optimizations
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (isBatteryIgnored) LimeGreen else WarningYellow, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "2. ยกเว้นการประหยัดแบตเตอรี่ (Ignore Battery)",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (isBatteryIgnored) "ทำงานแบบไม่จำกัด ไม่ถูกปิดโดยระบบ" else "ระบบอาจปิดการทำงานแอปเมื่อจอดับ",
                                color = if (isBatteryIgnored) LimeGreen else TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                            )
                        }
                        Button(
                            onClick = { requestIgnoreBatteryOptimization(context) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBatteryIgnored) Color(0x1610B981) else WarningYellow
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = if (isBatteryIgnored) "เปิดแล้ว" else "กดอนุญาต",
                                color = if (isBatteryIgnored) LimeGreen else SlateDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Accessibility Service (for Auto Transfer)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(if (hasAccessibilityPermission) LimeGreen else WarningYellow, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "3. สิทธิ์ Accessibility Service (สำหรับโอนเงินอัตโนมัติ)",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = if (hasAccessibilityPermission) "ระบบโอนเงินอัตโนมัติเปิดใช้งานแล้ว" else "จำเป็นต้องเปิดเพื่อใช้งานระบบโอนเงินอัตโนมัติ",
                                color = if (hasAccessibilityPermission) LimeGreen else TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                            )
                        }
                        Button(
                            onClick = onGrantAccessibilityClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (hasAccessibilityPermission) Color(0x1610B981) else WarningYellow
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = if (hasAccessibilityPermission) "เปิดแล้ว" else "กดเปิดสิทธิ์",
                                color = if (hasAccessibilityPermission) LimeGreen else SlateDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. Autostart Settings Helper
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(AccentBlue, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "4. เริ่มต้นอัตโนมัติ (Autostart Settings)",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "สำหรับ Xiaomi/Oppo/Vivo/Samsung ช่วยไม่ให้แอปหลุด",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(start = 16.dp, top = 2.dp)
                            )
                        }
                        Button(
                            onClick = { openAutostartSettings(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = "กดเปิดตั้งค่า",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 2. Overview Counters Card
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // SMS stats Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ระบบดัก SMS", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = smsEnabled,
                                onCheckedChange = onSmsToggle,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = LimeGreen,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = Color(0x26000000)
                                ),
                                modifier = Modifier.testTag("sms_forward_switch")
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = logs.filter { it.type == "SMS" && it.status == "SUCCESS" }.size.toString(),
                            color = TextPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ส่งออกสำเร็จวันนี้",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                // Notification stats Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, GlassBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("ดักการแจ้งเตือน", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Switch(
                                checked = notificationEnabled,
                                onCheckedChange = onNotificationToggle,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = LimeGreen,
                                    uncheckedThumbColor = TextSecondary,
                                    uncheckedTrackColor = Color(0x26000000)
                                ),
                                modifier = Modifier.testTag("notification_forward_switch")
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = logs.filter { it.type == "NOTIFICATION" && it.status == "SUCCESS" }.size.toString(),
                            color = TextPrimary,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ส่งออกสำเร็จวันนี้",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // 3. Webhook Summary Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "เป้าหมายการส่งออก Webhook",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Webhook URL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Outlined.Link, contentDescription = "Webhook Url", tint = AccentBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (webhookUrl.isBlank()) "ไม่ได้ตั้งค่าที่อยู่" else webhookUrl,
                            color = if (webhookUrl.isBlank()) ErrorRed else TextPrimary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (webhookUrl.isNotBlank()) {
                                    clipboardManager.setText(AnnotatedString(webhookUrl))
                                    Toast.makeText(context, "คัดลอก URL แล้ว", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Url", tint = TextSecondary, modifier = Modifier.size(14.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Access Token
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Outlined.Key, contentDescription = "Access Token", tint = LimeGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        val displayToken = if (token.length > 8) {
                            token.take(4) + "..." + token.takeLast(4)
                        } else if (token.isBlank()) {
                            "ไม่มีโทเคน"
                        } else {
                            token
                        }
                        Text(
                            text = displayToken,
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                if (token.isNotBlank()) {
                                    clipboardManager.setText(AnnotatedString(token))
                                    Toast.makeText(context, "คัดลอก Token แล้ว", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Token", tint = TextSecondary, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // 4. Log History Section Title
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(LimeGreen)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RECENT ACTIVITY",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                if (logs.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearAllLogs() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "ลบประวัติ", tint = ErrorRed.copy(alpha = 0.8f))
                    }
                }
            }
        }

        // 5. Activity Log list or empty state
        if (logs.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0x0AFFFFFF)),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, GlassBorder.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = "No logs yet",
                            tint = TextSecondary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "ไม่มีประวัติการส่งต่อ SMS / การแจ้งเตือน",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "SMS ที่ได้รับและผ่านตัวกรองจะถูกแสดงขึ้นเมื่อส่งออกสำเร็จ",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        } else {
            items(logs.take(50)) { log ->
                LogItemCard(log)
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
fun LogItemCard(log: ForwardLog) {
    val dateString = remember(log.timestamp) {
        val formatter = SimpleDateFormat("HH:mm:ss · dd/MM/yy", Locale.getDefault()).apply {
            timeZone = java.util.TimeZone.getTimeZone("Asia/Bangkok")
        }
        formatter.format(Date(log.timestamp))
    }

    val isSuccess = log.status == "SUCCESS"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sender and type
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (log.type == "SMS") AccentBlue.copy(alpha = 0.15f) else LimeGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (log.type == "SMS") Icons.Default.Sms else Icons.Default.Notifications,
                            contentDescription = log.type,
                            tint = if (log.type == "SMS") AccentBlue else LimeGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = log.sender,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${log.type} · $dateString",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }

                // Status Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSuccess) Color(0x2610B981) else Color(0x26EF4444))
                        .border(BorderStroke(1.dp, if (isSuccess) Color(0x3310B981) else Color(0x33EF4444)), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = log.status,
                        tint = if (isSuccess) GlassAccentGreen else ErrorRed,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isSuccess) "ส่งสำเร็จ" else "ล้มเหลว",
                        color = if (isSuccess) GlassAccentGreen else ErrorRed,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            
            // Log message body
            Text(
                text = log.message,
                color = TextPrimary.copy(alpha = 0.9f),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            // If failed, show response or reason
            if (log.responseMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(GlassBgDarker)
                        .border(BorderStroke(0.5.dp, GlassBorder), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = "Status detail",
                        tint = if (isSuccess) GlassAccentGreen else ErrorRed,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = log.responseMessage,
                        color = if (isSuccess) TextSecondary else Color(0xFFFCA5A5),
                        fontSize = 10.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsTab(
    viewModel: MainViewModel,
    currentUrl: String,
    currentToken: String,
    onSave: (String, String) -> Unit
) {
    val context = LocalContext.current
    var url by remember(currentUrl) { mutableStateOf(currentUrl) }
    var token by remember(currentToken) { mutableStateOf(currentToken) }

    val isTesting by viewModel.isTestingWebhook.collectAsState()
    val testResult by viewModel.testResult.collectAsState()

    // Clear test results on screen load or dismiss
    LaunchedEffect(Unit) {
        viewModel.clearTestResult()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Setup guide
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "แก้ไขเป้าหมาย Webhook",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ระบบจะยิง HTTP POST ไปยัง URL นี้ทันทีที่ได้รับ SMS หรือการแจ้งเตือนจากธนาคาร พร้อมแนบ Token ใน Headers และ Body เพื่อความปลอดภัย 100%",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        // Form Fields
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, GlassBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Webhook URL field
                    Text(
                        text = "Webhook URL (ที่อยู่เซิร์ฟเวอร์ callback)",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = url,
                        onValueChange = { url = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("webhook_url_input"),
                        placeholder = { Text("https://example.com/callback", color = TextSecondary.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LimeGreen,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = GlassBgDarker,
                            unfocusedContainerColor = GlassBgDarker
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Access Token field
                    Text(
                        text = "Access Token / Bearer Token",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = token,
                        onValueChange = { token = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("webhook_token_input"),
                        placeholder = { Text("กรอก Token สำหรับการยืนยันตัวตน", color = TextSecondary.copy(alpha = 0.5f)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LimeGreen,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = GlassBgDarker,
                            unfocusedContainerColor = GlassBgDarker
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Test Webhook Button
                        Button(
                            onClick = {
                                if (url.isBlank()) {
                                    Toast.makeText(context, "กรุณากรอก Webhook URL ก่อนทดสอบ", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.sendTestWebhook(context)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("test_webhook_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, GlassBorder)
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(color = LimeGreen, modifier = Modifier.size(18.dp))
                            } else {
                                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Test", tint = LimeGreen)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("ทดสอบส่ง", color = LimeGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        // Save Button
                        Button(
                            onClick = { onSave(url, token) },
                            modifier = Modifier
                                .weight(1.2f)
                                .height(46.dp)
                                .testTag("save_webhook_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = LimeGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Save, contentDescription = "Save", tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("บันทึกที่อยู่", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Test outcome display
        if (testResult != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (testResult!!.contains("สำเร็จ")) Color(0x2610B981) else Color(0x26EF4444)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (testResult!!.contains("สำเร็จ")) Color(0x4010B981) else Color(0x40EF4444))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (testResult!!.contains("สำเร็จ")) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = "Test result icon",
                            tint = if (testResult!!.contains("สำเร็จ")) GlassAccentGreen else ErrorRed,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = testResult!!,
                            color = if (testResult!!.contains("สำเร็จ")) TextPrimary else Color(0xFFFECACA),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
fun AppSelectionTab(
    selectedBanksString: String,
    onBanksChanged: (List<String>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val selectedPackages = remember(selectedBanksString) {
        selectedBanksString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    val filteredBanks = THAI_BANKS.filter {
        it.fullName.contains(searchQuery, ignoreCase = true) ||
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.packageName.contains(searchQuery, ignoreCase = true)
    }

    // Check if all filtered banks are selected
    val allFilteredSelected = remember(filteredBanks, selectedPackages) {
        filteredBanks.all { bank ->
            val bankPackages = bank.packageName.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            bankPackages.any { selectedPackages.contains(it) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(10.dp))

        // Card header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "คัดกรองแอปธนาคารที่ต้องการดักจับ",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "เลือกเฉพาะแอปธนาคารที่คุณติดตั้งและต้องการใช้งานเท่านั้น เพื่อป้องกันความสับสนและรับประกันความถูกต้องแม่นยำในการนำไปประมวลผล 100%",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("bank_search_input"),
            placeholder = { Text("ค้นหาชื่อธนาคาร...", color = TextSecondary.copy(alpha = 0.5f)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = LimeGreen,
                unfocusedBorderColor = GlassBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = GlassBgDarker,
                unfocusedContainerColor = GlassBgDarker
            ),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Select All / Deselect All buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    val allPackages = filteredBanks.flatMap { bank ->
                        bank.packageName.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    }
                    val newSelectedPackages = (selectedPackages + allPackages).distinct()
                    onBanksChanged(newSelectedPackages)
                },
                colors = ButtonDefaults.buttonColors(containerColor = LimeGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("เลือกทั้งหมด", color = SlateDark, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    val packagesToRemove = filteredBanks.flatMap { bank ->
                        bank.packageName.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    }
                    val newSelectedPackages = selectedPackages.filterNot { packagesToRemove.contains(it) }
                    onBanksChanged(newSelectedPackages)
                },
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("ยกเลิกทั้งหมด", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // List of banks
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filteredBanks) { bank ->
                val bankPackages = bank.packageName.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val isSelected = bankPackages.any { selectedPackages.contains(it) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val newList = if (isSelected) {
                                selectedPackages.filterNot { bankPackages.contains(it) }
                            } else {
                                selectedPackages + bankPackages
                            }
                            onBanksChanged(newList)
                        }
                        .testTag("bank_card_${bank.packageName}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0x24FFFFFF) else Color(0x12FFFFFF)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isSelected) GlassBorderLight else GlassBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Circular Initial Badge for Bank
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(bank.color.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = bank.initial,
                                    color = bank.color,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = bank.name,
                                    color = if (isSelected) TextPrimary else TextPrimary.copy(alpha = 0.8f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = bank.packageName,
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        Switch(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                val newList = if (checked) {
                                    selectedPackages + bankPackages
                                } else {
                                    selectedPackages.filterNot { bankPackages.contains(it) }
                                }
                                onBanksChanged(newList)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = LimeGreen,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = Color(0x1F000000)
                            ),
                            modifier = Modifier.testTag("bank_switch_${bank.packageName}")
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
}

private fun RowPaddingCompact() = androidx.compose.foundation.layout.PaddingValues(
    horizontal = 12.dp,
    vertical = 6.dp
)
