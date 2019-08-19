package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.service.pojo.UserWithWallet
import java.util.UUID

interface CooperativeWalletService {
    fun generateWalletActivationTransaction(walletId: Int, userUuid: UUID): TransactionDataAndInfo
    fun activateWallet(walletId: Int, signedTransaction: String): Wallet
    fun getAllUserWithUnactivatedWallet(): List<UserWithWallet>
    fun getOrganizationsWithUnactivatedWallet(): List<Organization>
    fun getProjectsWithUnactivatedWallet(): List<Project>
}
