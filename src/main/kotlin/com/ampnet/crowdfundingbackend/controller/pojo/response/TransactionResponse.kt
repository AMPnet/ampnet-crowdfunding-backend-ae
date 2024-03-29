package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.enums.TransactionType
import com.ampnet.crowdfundingbackend.persistence.model.TransactionInfo
import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionDataAndInfo

data class TransactionResponse(
    val tx: String,
    val txId: Int,
    val info: TransactionInfoResponse
) {
    constructor(transaction: TransactionDataAndInfo) : this(
        transaction.transactionData.tx,
        transaction.transactionInfo.id,
        TransactionInfoResponse(transaction.transactionInfo)
    )
}

data class TransactionInfoResponse(
    val txType: TransactionType,
    val title: String,
    val description: String
) {
    constructor(transactionInfo: TransactionInfo) : this(
        transactionInfo.type,
        transactionInfo.title,
        transactionInfo.description
    )
}
