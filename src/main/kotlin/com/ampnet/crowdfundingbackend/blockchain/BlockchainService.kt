package com.ampnet.crowdfundingbackend.blockchain

import com.ampnet.crowdfundingbackend.blockchain.pojo.BlockchainTransaction
import com.ampnet.crowdfundingbackend.blockchain.pojo.ProjectInvestmentTxRequest
import com.ampnet.crowdfundingbackend.blockchain.pojo.GenerateProjectWalletRequest
import com.ampnet.crowdfundingbackend.blockchain.pojo.Portfolio
import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionData

interface BlockchainService {
    fun getBalance(hash: String): Long
    fun addWallet(activationData: String): TransactionData
    fun generateCreateOrganizationTransaction(userWalletHash: String): TransactionData
    fun generateProjectWalletTransaction(request: GenerateProjectWalletRequest): TransactionData
    fun postTransaction(transaction: String): String
    fun generateProjectInvestmentTransaction(request: ProjectInvestmentTxRequest): TransactionData
    fun generateMintTransaction(toHash: String, amount: Long): TransactionData
    fun generateBurnTransaction(burnFromTxHash: String): TransactionData
    fun generateApproveBurnTransaction(burnFromTxHash: String, amount: Long): TransactionData
    fun getPortfolio(hash: String): Portfolio
    fun getTransactions(hash: String): List<BlockchainTransaction>
}
