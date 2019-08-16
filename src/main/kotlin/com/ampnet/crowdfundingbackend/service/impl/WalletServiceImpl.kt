package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.blockchain.BlockchainService
import com.ampnet.crowdfundingbackend.controller.pojo.request.WalletCreateRequest
import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.enums.WalletType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InternalException
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.UserWallet
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationRepository
import com.ampnet.crowdfundingbackend.persistence.repository.ProjectRepository
import com.ampnet.crowdfundingbackend.persistence.repository.UserWalletRepository
import com.ampnet.crowdfundingbackend.persistence.repository.WalletRepository
import com.ampnet.crowdfundingbackend.service.TransactionInfoService
import com.ampnet.crowdfundingbackend.service.WalletService
import com.ampnet.crowdfundingbackend.blockchain.pojo.GenerateProjectWalletRequest
import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.crowdfundingbackend.persistence.model.PairWalletCode
import com.ampnet.crowdfundingbackend.persistence.repository.PairWalletCodeRepository
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import java.time.ZonedDateTime

@Service
class WalletServiceImpl(
    private val walletRepository: WalletRepository,
    private val projectRepository: ProjectRepository,
    private val organizationRepository: OrganizationRepository,
    private val userWalletRepository: UserWalletRepository,
    private val blockchainService: BlockchainService,
    private val transactionInfoService: TransactionInfoService,
    private val pairWalletCodeRepository: PairWalletCodeRepository
) : WalletService {

    companion object : KLogging()

    private val charPool: List<Char> = ('A'..'Z') + ('0'..'9')

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
    @Throws(InternalException::class)
    override fun getWalletBalance(wallet: Wallet): Long {
        val walletHash = wallet.hash
            ?: throw ResourceNotFoundException(ErrorCode.WALLET_NOT_ACTIVATED, "Wallet not activated")
        return blockchainService.getBalance(walletHash)
    }

    @Transactional(readOnly = true)
    override fun getUserWallet(userUuid: UUID): Wallet? {
        return ServiceUtils.wrapOptional(userWalletRepository.findByUserUuid(userUuid))?.wallet
    }

    @Transactional
    @Throws(ResourceAlreadyExistsException::class, InternalException::class)
    override fun createUserWallet(userUuid: UUID, request: WalletCreateRequest): Wallet {
        userWalletRepository.findByUserUuid(userUuid).ifPresent {
            throw ResourceAlreadyExistsException(ErrorCode.WALLET_EXISTS, "User: $userUuid already has a wallet.")
        }
        pairWalletCodeRepository.findByAddress(request.address).ifPresent {
            pairWalletCodeRepository.delete(it)
        }

        val wallet = createWallet(request.publicKey, WalletType.USER)
        val userWallet = UserWallet(0, userUuid, wallet)
        userWalletRepository.save(userWallet)
        return wallet
    }

    @Transactional
    override fun generateTransactionToCreateProjectWallet(project: Project, userUuid: UUID): TransactionDataAndInfo {
        throwExceptionIfProjectHasWallet(project)
        val userWalletHash = getUserWallet(userUuid)?.hash
                ?: throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "User wallet is missing")
        val organizationWalletHash = project.organization.wallet?.hash
            ?: throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "Organization wallet is missing")

        val request = GenerateProjectWalletRequest(project, organizationWalletHash, userWalletHash)
        val data = blockchainService.generateProjectWalletTransaction(request)
        val info = transactionInfoService.createProjectTransaction(project, userUuid)
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    @Throws(ResourceAlreadyExistsException::class)
    override fun createProjectWallet(project: Project, signedTransaction: String): Wallet {
        throwExceptionIfProjectHasWallet(project)
        val txHash = blockchainService.postTransaction(signedTransaction)
        val wallet = createWallet(txHash, WalletType.PROJECT)
        project.wallet = wallet
        projectRepository.save(project)
        return wallet
    }

    @Transactional
    override fun generateTransactionToCreateOrganizationWallet(
        organization: Organization,
        userUuid: UUID
    ): TransactionDataAndInfo {
        throwExceptionIfOrganizationAlreadyHasWallet(organization)
        val walletHash = getUserWallet(userUuid)?.hash
                ?: throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "User wallet is missing")

        val data = blockchainService.generateCreateOrganizationTransaction(walletHash)
        val info = transactionInfoService.createOrgTransaction(organization, userUuid)
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    override fun createOrganizationWallet(organization: Organization, signedTransaction: String): Wallet {
        throwExceptionIfOrganizationAlreadyHasWallet(organization)
        val txHash = blockchainService.postTransaction(signedTransaction)
        val wallet = createWallet(txHash, WalletType.ORG)
        organization.wallet = wallet
        organizationRepository.save(organization)
        return wallet
    }

    @Transactional
    override fun generatePairWalletCode(request: WalletCreateRequest): PairWalletCode {
        pairWalletCodeRepository.findByAddress(request.address).ifPresent {
            pairWalletCodeRepository.delete(it)
        }
        val code = generatePairWalletCode()
        val pairWalletCode = PairWalletCode(0, request.address, request.publicKey, code, ZonedDateTime.now())
        return pairWalletCodeRepository.save(pairWalletCode)
    }

    @Transactional(readOnly = true)
    override fun getPairWalletCode(code: String): PairWalletCode? {
        return ServiceUtils.wrapOptional(pairWalletCodeRepository.findByCode(code))
    }

    private fun createWallet(activationData: String, type: WalletType): Wallet {
        if (walletRepository.findByHash(activationData).isPresent) {
            throw ResourceAlreadyExistsException(ErrorCode.WALLET_HASH_EXISTS,
                    "Trying to create wallet: $type with existing activationData: $activationData")
        }
        val wallet = Wallet(0, activationData, type, Currency.EUR, ZonedDateTime.now(), null, null)
        return walletRepository.save(wallet)
    }

    private fun throwExceptionIfProjectHasWallet(project: Project) {
        project.wallet?.let {
            throw ResourceAlreadyExistsException(ErrorCode.WALLET_EXISTS,
                    "Project: ${project.name} already has a wallet.")
        }
    }

    private fun throwExceptionIfOrganizationAlreadyHasWallet(organization: Organization) {
        organization.wallet?.let {
            throw ResourceAlreadyExistsException(ErrorCode.WALLET_EXISTS,
                    "Organization: ${organization.name} already has a wallet.")
        }
    }

    private fun getWalletById(walletId: Int): Wallet = walletRepository.findById(walletId).orElseThrow {
        throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "Missing wallet id: $walletId")
    }

    private fun generatePairWalletCode(): String = (1..6)
        .map { kotlin.random.Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")
}
