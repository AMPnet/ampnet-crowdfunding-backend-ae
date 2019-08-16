package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.enums.WalletType
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.TransactionInfo
import com.ampnet.crowdfundingbackend.service.pojo.MintServiceRequest
import java.util.UUID

interface TransactionInfoService {
    fun activateWalletTransaction(walletId: Int, type: WalletType, userUuid: UUID): TransactionInfo
    fun createOrgTransaction(organization: Organization, userUuid: UUID): TransactionInfo
    fun createProjectTransaction(project: Project, userUuid: UUID): TransactionInfo
    fun createInvestTransaction(projectName: String, amount: Long, userUuid: UUID): TransactionInfo
    fun createMintTransaction(request: MintServiceRequest, receivingWallet: String): TransactionInfo
    fun createApprovalTransaction(amount: Long, userUuid: UUID, withdrawId: Int): TransactionInfo
    fun createBurnTransaction(amount: Long, userUuid: UUID, withdrawId: Int): TransactionInfo
    fun deleteTransaction(id: Int)
    fun findTransactionInfo(id: Int): TransactionInfo?
}
