package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.service.pojo.ProjectInvestmentRequest
import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionDataAndInfo

interface ProjectInvestmentService {
    fun generateInvestInProjectTransaction(request: ProjectInvestmentRequest): TransactionDataAndInfo
    fun investInProject(signedTransaction: String): String
}
