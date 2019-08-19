package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.enums.WalletType
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.service.impl.TransactionInfoServiceImpl
import com.ampnet.crowdfundingbackend.service.impl.WalletServiceImpl
import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionData
import com.ampnet.crowdfundingbackend.persistence.model.PairWalletCode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import java.time.ZonedDateTime

class WalletServiceTest : JpaServiceTestBase() {

    private val walletService: WalletService by lazy {
        val transactionService = TransactionInfoServiceImpl(transactionInfoRepository)
        WalletServiceImpl(walletRepository, projectRepository, organizationRepository, userWalletRepository,
                mockedBlockchainService, transactionService, pairWalletCodeRepository)
    }
    private lateinit var testContext: TestContext

    private val defaultAddressHash = "0x4e4ee58ff3a9e9e78c2dfdbac0d1518e4e1039f9189267e1dc8d3e35cbdf7892"
    private val defaultPublicKey = "0xC2D7CF95645D33006175B78989035C7c9061d3F9"
    private val defaultSignedTransaction = "SignedTransaction"
    private val defaultTransactionData = TransactionData("data")

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllWalletsAndOwners()
        testContext = TestContext()
    }

    @Test
    fun mustBeAbleToGetWalletForUserId() {
        suppose("User has a wallet") {
            createWalletForUser(userUuid, defaultAddressHash)
        }

        verify("Service must fetch wallet for user with id") {
            val wallet = walletService.getUserWallet(userUuid) ?: fail("User must have a wallet")
            assertThat(wallet.hash).isEqualTo(defaultAddressHash)
            assertThat(wallet.currency).isEqualTo(Currency.EUR)
            assertThat(wallet.type).isEqualTo(WalletType.USER)
        }
    }

    @Test
    fun mustBeAbleToCreateWalletForUser() {
        suppose("Wallet has pair wallet code") {
            databaseCleanerService.deleteAllPairWalletCodes()
            val pairWalletCode = PairWalletCode(0, defaultPublicKey, "000000", ZonedDateTime.now())
            pairWalletCodeRepository.save(pairWalletCode)
        }

        verify("Service can create wallet for a user") {
            val wallet = walletService.createUserWallet(userUuid, defaultPublicKey)
            assertThat(wallet.activationData).isEqualTo(defaultPublicKey)
            assertThat(wallet.currency).isEqualTo(Currency.EUR)
            assertThat(wallet.type).isEqualTo(WalletType.USER)
            assertThat(wallet.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(wallet.hash).isNull()
            assertThat(wallet.activatedAt).isNull()
        }
        verify("Wallet is assigned to the user") {
            val wallet = walletService.getUserWallet(userUuid) ?: fail("User must have a wallet")
            assertThat(wallet.activationData).isEqualTo(defaultPublicKey)
        }
        verify("Pair wallet code is deleted") {
            val optionalPairWalletCode = pairWalletCodeRepository.findByPublicKey(defaultPublicKey)
            assertThat(optionalPairWalletCode).isNotPresent
        }
    }

    @Test
    fun mustBeAbleToCreateWalletForProject() {
        suppose("Project exists") {
            val organization = createOrganization("Org", userUuid)
            testContext.project = createProject("Das project", organization, userUuid)
        }
        suppose("Blockchain service successfully adds wallet") {
            Mockito.`when`(
                mockedBlockchainService.postTransaction(defaultSignedTransaction)
            ).thenReturn(defaultAddressHash)
        }

        verify("Service can create wallet for project") {
            val wallet = walletService.createProjectWallet(testContext.project, defaultSignedTransaction)
            assertThat(wallet.activationData).isEqualTo(defaultAddressHash)
            assertThat(wallet.currency).isEqualTo(Currency.EUR)
            assertThat(wallet.type).isEqualTo(WalletType.PROJECT)
            assertThat(wallet.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(wallet.hash).isNull()
            assertThat(wallet.activatedAt).isNull()
        }
        verify("Wallet is assigned to the project") {
            val optionalProjectWithWallet = projectRepository.findByIdWithWallet(testContext.project.id)
            assertThat(optionalProjectWithWallet).isPresent
            val projectWallet = optionalProjectWithWallet.get().wallet ?: fail("Missing project wallet")
            assertThat(projectWallet.activationData).isEqualTo(defaultAddressHash)
            assertThat(projectWallet.hash).isNull()
        }
    }

    @Test
    fun mustNotBeAbleToCreateMultipleWalletsForOneUser() {
        suppose("User has a wallet") {
            createWalletForUser(userUuid, defaultAddressHash)
        }

        verify("Service cannot create additional account") {
            val exception = assertThrows<ResourceAlreadyExistsException> {
                walletService.createUserWallet(userUuid, defaultPublicKey)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_EXISTS)
        }
    }

    @Test
    fun mustNotBeAbleToCreateMultipleWalletsForOneProject() {
        suppose("Project exists") {
            val organization = createOrganization("Org", userUuid)
            testContext.project = createProject("Das project", organization, userUuid)
        }
        suppose("Project has a wallet") {
            createWalletForProject(testContext.project, defaultAddressHash)
        }

        verify("Service cannot create additional account") {
            val exception = assertThrows<ResourceAlreadyExistsException> {
                walletService.createProjectWallet(testContext.project, defaultSignedTransaction)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_EXISTS)
        }
    }

    @Test
    fun mustBeAbleToGetWalletBalance() {
        suppose("User has a wallet") {
            createWalletForUser(userUuid, defaultAddressHash)
        }
        suppose("User has some funds on a wallet") {
            testContext.balance = 100
            Mockito.`when`(mockedBlockchainService.getBalance(defaultAddressHash)).thenReturn(testContext.balance)
        }

        verify("Service can return wallet balance") {
            val wallet = walletService.getUserWallet(userUuid) ?: fail("User must have a wallet")
            val balance = walletService.getWalletBalance(wallet)
            assertThat(balance).isEqualTo(testContext.balance)
        }
    }

    @Test
    fun mustThrowExceptionIfUserWithoutWalletTriesToGenerateCreateProjectWallet() {
        suppose("Project exists") {
            val organization = createOrganization("Org", userUuid)
            testContext.project = createProject("Das project", organization, userUuid)
        }

        verify("Service will throw InternalException") {
            val exception = assertThrows<ResourceNotFoundException> {
                walletService.generateTransactionToCreateProjectWallet(testContext.project, userUuid)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    fun mustThrowExceptionIfOrganizationWithoutWalletTriesToGenerateCreateProjectWallet() {
        suppose("User has a wallet") {
            createWalletForUser(userUuid, defaultAddressHash)
        }
        suppose("Project exists") {
            val organization = createOrganization("Org", userUuid)
            testContext.project = createProject("Das project", organization, userUuid)
        }

        verify("Service will throw InternalException") {
            val exception = assertThrows<ResourceNotFoundException> {
                walletService.generateTransactionToCreateProjectWallet(testContext.project, userUuid)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    fun mustThrowExceptionWhenGenerateTransactionToCreateOrganizationWalletWithoutUserWallet() {
        suppose("Organization exists") {
            testContext.organization = createOrganization("Org", userUuid)
        }

        verify("Service can generate create organization transaction") {
            val exception = assertThrows<ResourceNotFoundException> {
                walletService.generateTransactionToCreateOrganizationWallet(testContext.organization, userUuid)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_MISSING)
        }
    }

    @Test
    fun mustThrowExceptionIfOrganizationAlreadyHasWallet() {
        suppose("Organization exists") {
            testContext.organization = createOrganization("Org", userUuid)
        }
        suppose("Organization has a wallet") {
            createWalletForOrganization(testContext.organization, defaultAddressHash)
        }

        verify("Service will throw exception that organization already has a wallet") {
            val exception = assertThrows<ResourceAlreadyExistsException> {
                walletService.generateTransactionToCreateOrganizationWallet(testContext.organization, userUuid)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_EXISTS)
        }
    }

    @Test
    fun mustGenerateTransactionToCreateOrganizationWallet() {
        suppose("User has a wallet") {
            testContext.wallet = createWalletForUser(userUuid, defaultAddressHash)
        }
        suppose("Organization exists") {
            testContext.organization = createOrganization("Org", userUuid)
        }
        suppose("Blockchain service will generate transaction") {
            Mockito.`when`(
                mockedBlockchainService.generateCreateOrganizationTransaction(getWalletHash(testContext.wallet))
            ).thenReturn(defaultTransactionData)
        }

        verify("Service can generate transaction") {
            val transaction = walletService
                .generateTransactionToCreateOrganizationWallet(testContext.organization, userUuid)
            assertThat(transaction.transactionData).isEqualTo(defaultTransactionData)
        }
    }

    @Test
    fun mustBeAbleToCreateOrganizationWallet() {
        suppose("Organization exists") {
            testContext.organization = createOrganization("Org", userUuid)
        }
        suppose("Blockchain service successfully adds wallet") {
            Mockito.`when`(
                mockedBlockchainService.postTransaction(defaultSignedTransaction)
            ).thenReturn(defaultAddressHash)
        }

        verify("Service can create wallet for organization") {
            val wallet = walletService.createOrganizationWallet(testContext.organization, defaultSignedTransaction)
            assertThat(wallet.activationData).isEqualTo(defaultAddressHash)
            assertThat(wallet.currency).isEqualTo(Currency.EUR)
            assertThat(wallet.type).isEqualTo(WalletType.ORG)
            assertThat(wallet.createdAt).isBeforeOrEqualTo(ZonedDateTime.now())
            assertThat(wallet.hash).isNull()
            assertThat(wallet.activatedAt).isNull()
        }
        verify("Wallet is assigned to the organization") {
            val optionalOrganization = organizationRepository.findById(testContext.organization.id)
            assertThat(optionalOrganization).isPresent
            val wallet = optionalOrganization.get().wallet ?: fail("Missing organization wallet")
            assertThat(wallet.activationData).isEqualTo(defaultAddressHash)
            assertThat(wallet.hash).isNull()
        }
    }

    @Test
    fun mustThrowExceptionForCreateOrganizationWalletIfOrganizationAlreadyHasWallet() {
        suppose("Organization exists") {
            testContext.organization = createOrganization("Org", userUuid)
        }
        suppose("Organization has a wallet") {
            createWalletForOrganization(testContext.organization, defaultAddressHash)
        }

        verify("Service cannot create additional organization account") {
            val exception = assertThrows<ResourceAlreadyExistsException> {
                walletService.createOrganizationWallet(testContext.organization, defaultSignedTransaction)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_EXISTS)
        }
    }

    @Test
    fun mustNotBeAbleToCreateWalletWithTheSameHash() {
        suppose("User has a wallet") {
            createWalletForUser(userUuid, defaultAddressHash)
        }
        suppose("Project exists") {
            val organization = createOrganization("Org", userUuid)
            testContext.project = createProject("Das project", organization, userUuid)
        }
        suppose("Blockchain service will return same hash for new project wallet transaction") {
            Mockito.`when`(
                mockedBlockchainService.postTransaction(defaultSignedTransaction)
            ).thenReturn(defaultAddressHash)
        }

        verify("User will not be able to create organization wallet with the same hash") {
            val exception = assertThrows<ResourceAlreadyExistsException> {
                walletService.createProjectWallet(testContext.project, defaultSignedTransaction)
            }
            assertThat(exception.errorCode).isEqualTo(ErrorCode.WALLET_HASH_EXISTS)
        }
    }

    @Test
    fun mustGenerateNewPairWalletCodeForExistingAddress() {
        suppose("Pair wallet code exists") {
            databaseCleanerService.deleteAllPairWalletCodes()
            val pairWalletCode = PairWalletCode(0, "adr_423242", "SD432X", ZonedDateTime.now())
            testContext.pairWalletCode = pairWalletCodeRepository.save(pairWalletCode)
        }

        verify("Service will create new pair wallet code") {
            val newPairWalletCode = walletService.generatePairWalletCode(testContext.pairWalletCode.publicKey)
            assertThat(newPairWalletCode.publicKey).isEqualTo(testContext.pairWalletCode.publicKey)
        }
        verify("Old pair wallet code is deleted") {
            val oldPairWalletCode = pairWalletCodeRepository.findById(testContext.pairWalletCode.id)
            assertThat(oldPairWalletCode).isNotPresent
        }
    }

    private class TestContext {
        lateinit var organization: Organization
        lateinit var project: Project
        lateinit var wallet: Wallet
        var balance: Long = -1
        lateinit var pairWalletCode: PairWalletCode
    }
}
