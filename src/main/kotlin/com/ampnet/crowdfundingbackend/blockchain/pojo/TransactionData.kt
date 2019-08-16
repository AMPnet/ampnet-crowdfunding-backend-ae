package com.ampnet.crowdfundingbackend.blockchain.pojo

import com.ampnet.crowdfunding.proto.RawTxResponse
import com.ampnet.crowdfundingbackend.persistence.model.TransactionInfo

data class TransactionData(val tx: String) {
    constructor(rawTxResponse: RawTxResponse) : this(rawTxResponse.tx)
}
data class TransactionDataAndInfo(val transactionData: TransactionData, val transactionInfo: TransactionInfo)
