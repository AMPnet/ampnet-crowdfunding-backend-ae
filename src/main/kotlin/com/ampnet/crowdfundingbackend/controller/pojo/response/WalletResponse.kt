package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.enums.WalletType
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import java.time.ZonedDateTime

data class WalletResponse(
    val id: Int,
    val activationData: String,
    val type: WalletType,
    val balance: Long,
    val currency: Currency,
    val createdAt: ZonedDateTime,
    val hash: String?,
    val activatedAt: ZonedDateTime?
) {
    constructor(wallet: Wallet, balance: Long) : this(
        wallet.id,
        wallet.activationData,
        wallet.type,
        balance,
        wallet.currency,
        wallet.createdAt,
        wallet.hash,
        wallet.activatedAt
    )
}
