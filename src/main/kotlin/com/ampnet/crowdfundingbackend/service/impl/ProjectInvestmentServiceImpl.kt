package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.blockchain.BlockchainService
import com.ampnet.crowdfundingbackend.blockchain.pojo.ProjectInvestmentTxRequest
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InvalidRequestException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.service.ProjectInvestmentService
import com.ampnet.crowdfundingbackend.service.TransactionInfoService
import com.ampnet.crowdfundingbackend.service.WalletService
import com.ampnet.crowdfundingbackend.service.pojo.ProjectInvestmentRequest
import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionDataAndInfo
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import java.time.ZonedDateTime

@Service
class ProjectInvestmentServiceImpl(
    private val walletService: WalletService,
    private val blockchainService: BlockchainService,
    private val transactionInfoService: TransactionInfoService
) : ProjectInvestmentService {

    @Transactional
    @Throws(InvalidRequestException::class, ResourceNotFoundException::class)
    override fun generateInvestInProjectTransaction(request: ProjectInvestmentRequest): TransactionDataAndInfo {
        verifyProjectIsStillActive(request.project)
        verifyInvestmentAmountIsValid(request.project, request.amount)

        val userWallet = getUserWallet(request.investorUuid)
        verifyUserHasEnoughFunds(userWallet, request.amount)

        val projectWallet = getProjectWallet(request.project)
        verifyProjectDidNotReachExpectedInvestment(projectWallet, request.project.expectedFunding)

        val userWalletHash = getWalletHash(userWallet)
        val projectWalletHash = getWalletHash(projectWallet)
        val investRequest = ProjectInvestmentTxRequest(userWalletHash, projectWalletHash, request.amount)
        val data = blockchainService.generateProjectInvestmentTransaction(investRequest)
        val info = transactionInfoService.createInvestTransaction(
                request.project.name, request.amount, request.investorUuid)
        return TransactionDataAndInfo(data, info)
    }

    override fun investInProject(signedTransaction: String): String =
        blockchainService.postTransaction(signedTransaction)

    private fun verifyProjectIsStillActive(project: Project) {
        if (project.active.not()) {
            throw InvalidRequestException(ErrorCode.PRJ_NOT_ACTIVE, "Project is not active")
        }
        if (project.endDate.isBefore(ZonedDateTime.now())) {
            throw InvalidRequestException(ErrorCode.PRJ_DATE_EXPIRED, "Project has expired at: ${project.endDate}")
        }
    }

    private fun verifyInvestmentAmountIsValid(project: Project, amount: Long) {
        if (amount > project.maxPerUser) {
            throw InvalidRequestException(ErrorCode.PRJ_MAX_PER_USER, "User can invest max ${project.maxPerUser}")
        }
        if (amount < project.minPerUser) {
            throw InvalidRequestException(
                    ErrorCode.PRJ_MIN_PER_USER, "User has to invest at least ${project.minPerUser}")
        }
    }

    private fun verifyUserHasEnoughFunds(wallet: Wallet, amount: Long) {
        val funds = walletService.getWalletBalance(wallet)
        if (funds < amount) {
            throw InvalidRequestException(ErrorCode.WALLET_FUNDS, "User does not have enough funds on wallet")
        }
    }

    private fun verifyProjectDidNotReachExpectedInvestment(wallet: Wallet, expectedFunding: Long) {
        val currentFunds = walletService.getWalletBalance(wallet)
        if (currentFunds == expectedFunding) {
            throw InvalidRequestException(
                    ErrorCode.PRJ_MAX_FUNDS, "Project has reached expected funding: $currentFunds")
        }
    }

    private fun getUserWallet(userUuid: UUID) = walletService.getUserWallet(userUuid)
        ?: throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "User does not have the wallet")

    private fun getProjectWallet(project: Project) = project.wallet
        ?: throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "Project does not have the wallet")

    private fun getWalletHash(wallet: Wallet) = wallet.hash
        ?: throw ResourceNotFoundException(ErrorCode.WALLET_NOT_ACTIVATED, "Not activated")
}
