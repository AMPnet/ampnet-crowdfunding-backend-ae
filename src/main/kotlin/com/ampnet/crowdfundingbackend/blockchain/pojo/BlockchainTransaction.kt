package com.ampnet.crowdfundingbackend.blockchain.pojo

import com.ampnet.crowdfunding.proto.TransactionsResponse

data class BlockchainTransaction(
    val fromTxHash: String,
    val toTxHash: String,
    val amount: Long,
    val type: TransactionsResponse.Transaction.Type
) {
    constructor(transaction: TransactionsResponse.Transaction) : this(
        transaction.fromTxHash,
        transaction.toTxHash,
        transaction.amount.toLong(),
        transaction.type
    )
}
