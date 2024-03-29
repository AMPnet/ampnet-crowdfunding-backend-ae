package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.blockchain.BlockchainService
import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.enums.WalletType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
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
import com.ampnet.crowdfundingbackend.exception.GrpcException
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

    @Transactional(readOnly = true)
    @Throws(GrpcException::class)
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
    @Throws(ResourceAlreadyExistsException::class)
    override fun createUserWallet(userUuid: UUID, publicKey: String): Wallet {
        userWalletRepository.findByUserUuid(userUuid).ifPresent {
            throw ResourceAlreadyExistsException(ErrorCode.WALLET_EXISTS, "User: $userUuid already has a wallet.")
        }
        pairWalletCodeRepository.findByPublicKey(publicKey).ifPresent {
            pairWalletCodeRepository.delete(it)
        }

        logger.debug { "Creating wallet: $publicKey for user: $userUuid" }
        val wallet = createWallet(publicKey, WalletType.USER)
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

        logger.debug { "Generating create wallet transaction for project: ${project.id}" }
        val request = GenerateProjectWalletRequest(project, organizationWalletHash, userWalletHash)
        val data = blockchainService.generateProjectWalletTransaction(request)
        val info = transactionInfoService.createProjectTransaction(project, userUuid)
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    @Throws(ResourceAlreadyExistsException::class)
    override fun createProjectWallet(project: Project, signedTransaction: String): Wallet {
        throwExceptionIfProjectHasWallet(project)
        logger.debug { "Creating wallet for project: ${project.id}" }
        val txHash = blockchainService.postTransaction(signedTransaction)
        val wallet = createWallet(txHash, WalletType.PROJECT)
        project.wallet = wallet
        projectRepository.save(project)
        logger.debug { "Created wallet for project: ${project.id}" }
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

        logger.debug { "Generating create wallet transaction for organization: ${organization.id}" }
        val data = blockchainService.generateCreateOrganizationTransaction(walletHash)
        val info = transactionInfoService.createOrgTransaction(organization, userUuid)
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    override fun createOrganizationWallet(organization: Organization, signedTransaction: String): Wallet {
        throwExceptionIfOrganizationAlreadyHasWallet(organization)
        logger.debug { "Creating wallet for organization: ${organization.id}" }
        val txHash = blockchainService.postTransaction(signedTransaction)
        val wallet = createWallet(txHash, WalletType.ORG)
        organization.wallet = wallet
        organizationRepository.save(organization)
        logger.debug { "Created wallet for organization: ${organization.id}" }
        return wallet
    }

    @Transactional
    override fun generatePairWalletCode(publicKey: String): PairWalletCode {
        pairWalletCodeRepository.findByPublicKey(publicKey).ifPresent {
            pairWalletCodeRepository.delete(it)
        }
        val code = generatePairWalletCode()
        val pairWalletCode = PairWalletCode(0, publicKey, code, ZonedDateTime.now())
        return pairWalletCodeRepository.save(pairWalletCode)
    }

    @Transactional(readOnly = true)
    override fun getPairWalletCode(code: String): PairWalletCode? {
        return ServiceUtils.wrapOptional(pairWalletCodeRepository.findByCode(code))
    }

    private fun createWallet(activationData: String, type: WalletType): Wallet {
        if (walletRepository.findByActivationData(activationData).isPresent) {
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

    private fun generatePairWalletCode(): String = (1..6)
        .map { kotlin.random.Random.nextInt(0, charPool.size) }
        .map(charPool::get)
        .joinToString("")
}
