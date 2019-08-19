package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.blockchain.BlockchainService
import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationRepository
import com.ampnet.crowdfundingbackend.persistence.repository.ProjectRepository
import com.ampnet.crowdfundingbackend.persistence.repository.UserWalletRepository
import com.ampnet.crowdfundingbackend.persistence.repository.WalletRepository
import com.ampnet.crowdfundingbackend.service.CooperativeWalletService
import com.ampnet.crowdfundingbackend.service.TransactionInfoService
import com.ampnet.crowdfundingbackend.service.pojo.UserWithWallet
import com.ampnet.crowdfundingbackend.userservice.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class CooperativeWalletServiceImpl(
    private val userWalletRepository: UserWalletRepository,
    private val walletRepository: WalletRepository,
    private val organizationRepository: OrganizationRepository,
    private val projectRepository: ProjectRepository,
    private val userService: UserService,
    private val blockchainService: BlockchainService,
    private val transactionInfoService: TransactionInfoService
) : CooperativeWalletService {

    @Transactional
    override fun generateWalletActivationTransaction(walletId: Int, userUuid: UUID): TransactionDataAndInfo {
        val wallet = getWalletById(walletId)
        val data = blockchainService.addWallet(wallet.activationData)
        val info = transactionInfoService.activateWalletTransaction(wallet.id, wallet.type, userUuid)
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    override fun activateWallet(walletId: Int, signedTransaction: String): Wallet {
        val wallet = getWalletById(walletId)
        wallet.hash = blockchainService.postTransaction(signedTransaction)
        wallet.activatedAt = ZonedDateTime.now()
        return walletRepository.save(wallet)
    }

    @Transactional(readOnly = true)
    override fun getAllUserWithUnactivatedWallet(): List<UserWithWallet> {
        val userWallets = userWalletRepository.findAllWithUnactivatedWallet().associateBy { it.userUuid }
        val users = userService.getUsers(userWallets.keys.toList())
        return users.mapNotNull { user ->
            userWallets[UUID.fromString(user.uuid)]?.let { wallet ->
                UserWithWallet(user, wallet.wallet)
            }
        }
    }

    @Transactional(readOnly = true)
    override fun getOrganizationsWithUnactivatedWallet(): List<Organization> {
        return organizationRepository.findAllWithUnactivatedWallet()
    }

    @Transactional(readOnly = true)
    override fun getProjectsWithUnactivatedWallet(): List<Project> {
        return projectRepository.findAllWithUnactivatedWallet()
    }

    private fun getWalletById(walletId: Int): Wallet = walletRepository.findById(walletId).orElseThrow {
        throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "Missing wallet id: $walletId")
    }
}
