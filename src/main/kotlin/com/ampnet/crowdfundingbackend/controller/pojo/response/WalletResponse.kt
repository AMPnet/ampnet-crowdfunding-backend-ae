package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.enums.WalletType
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import java.time.ZonedDateTime

data class WalletResponse(
    val id: Int,
    val activationData: String,
    val type: WalletType,
    val currency: Currency,
    val createdAt: ZonedDateTime,
    val hash: String?,
    val activatedAt: ZonedDateTime?,
    val balance: Long?
) {
    constructor(wallet: Wallet, balance: Long? = null) : this(
        wallet.id,
        wallet.activationData,
        wallet.type,
        wallet.currency,
        wallet.createdAt,
        wallet.hash,
        wallet.activatedAt,
        balance
    )
}
