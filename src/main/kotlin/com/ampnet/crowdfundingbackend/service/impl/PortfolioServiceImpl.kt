package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfunding.proto.TransactionsResponse
import com.ampnet.crowdfundingbackend.blockchain.BlockchainService
import com.ampnet.crowdfundingbackend.blockchain.pojo.BlockchainTransaction
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.repository.ProjectRepository
import com.ampnet.crowdfundingbackend.persistence.repository.UserWalletRepository
import com.ampnet.crowdfundingbackend.service.PortfolioService
import com.ampnet.crowdfundingbackend.service.pojo.PortfolioStats
import com.ampnet.crowdfundingbackend.service.pojo.ProjectWithInvestment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PortfolioServiceImpl(
    private val userWalletRepository: UserWalletRepository,
    private val blockchainService: BlockchainService,
    private val projectRepository: ProjectRepository
) : PortfolioService {

    @Transactional(readOnly = true)
    override fun getPortfolio(user: UUID): List<ProjectWithInvestment> {
        val userWallet = ServiceUtils.getUserWalletHash(user, userWalletRepository)
        val portfolio = blockchainService.getPortfolio(userWallet).data.associateBy { it.projectTxHash }
        return if (portfolio.isNotEmpty()) {
            val projects = projectRepository.findByWalletHashes(portfolio.keys)
            projects.mapNotNull { project ->
                portfolio[project.wallet?.hash]?.let { it ->
                    ProjectWithInvestment(project, it.amount)
                }
            }
        } else {
            emptyList()
        }
    }

    @Transactional(readOnly = true)
    override fun getPortfolioStats(user: UUID): PortfolioStats {
        val userWallet = ServiceUtils.getUserWalletHash(user, userWalletRepository)
        val transactions = blockchainService.getTransactions(userWallet)
        val investments = sumTransactionForType(transactions, TransactionsResponse.Transaction.Type.INVEST)
        val earnings = sumTransactionForType(transactions, TransactionsResponse.Transaction.Type.SHARE_PAYOUT)
        return PortfolioStats(investments, earnings)
    }

    @Transactional(readOnly = true)
    override fun getInvestmentsInProject(user: UUID, project: Project): List<BlockchainTransaction> {
        val userWalletHash = ServiceUtils.getUserWalletHash(user, userWalletRepository)
        val projectWalletHash = ServiceUtils.getProjectWalletHash(project)
        return blockchainService.getInvestmentsInProject(userWalletHash, projectWalletHash)
    }

    private fun sumTransactionForType(
        transactions: List<BlockchainTransaction>,
        type: TransactionsResponse.Transaction.Type
    ): Long {
        return transactions
            .filter { it.type == type }
            .map { it.amount }
            .sum()
    }
}
