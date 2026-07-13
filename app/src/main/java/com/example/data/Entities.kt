package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "configuration")
data class Configuration(
    @PrimaryKey val id: Int = 1,
    val webhookUrl: String = "https://easyo.online/api/v1/sms/callback",
    val token: String = "0d208ccb24bbe4a9d9dfce89a1d9e831bd52d5eb",
    val isSmsForwardEnabled: Boolean = true,
    val isNotificationForwardEnabled: Boolean = true,
    val selectedBankPackages: String = "com.kasikorn.kplus,com.kasikorn.retail.mbanking.wap,com.kasikornbank.kbiz,com.kasikornbank.kmerchant,com.scb.phone,th.co.krungthaibank.next,com.bualuang.mbanking,com.krungsri.kma,com.ttbbank.oneapp,gsb.or.th.mymo,com.tdg.truemoneywallet,com.garena.android.koalapay,th.co.lhbank.mobilebanking,com.uob.mightyth,th.co.cimbthai.clicks"
)

@Entity(tableName = "forward_logs")
data class ForwardLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "SMS" or "NOTIFICATION"
    val sender: String,
    val message: String,
    val status: String, // "SUCCESS" or "FAILED"
    val responseMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "withdraw_jobs")
data class WithdrawJob(
    @PrimaryKey val ref: String,
    val amount: Double,
    val bankCode: String,
    val accountNumber: String,
    val accountName: String,
    val status: String, // "PENDING", "RESERVED", "PROCESSING", "SUCCESS", "FAILED"
    val bankRef: String? = null,
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isSimulated: Boolean = false
)

@Entity(tableName = "app_user")
data class AppUser(
    @PrimaryKey val id: Int = 1,
    val username: String,
    val expiresAt: String? = null
)
