package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionData
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationWithWalletListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectWithWalletListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.TransactionResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.UserWithWalletListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.WalletResponse
import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.enums.PrivilegeType
import com.ampnet.crowdfundingbackend.enums.TransactionType
import com.ampnet.crowdfundingbackend.enums.WalletType
import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.UserWallet
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime
import java.util.UUID

class CooperativeWalletControllerTest : ControllerTestBase() {

    private val cooperativeWalletPath = "/cooperative/wallet/"

    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllWalletsAndOwners()
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PWA_WALLET])
    fun mustBeAbleToGetActivateWalletTransaction() {
        suppose("User created wallet") {
            testContext.wallet = createUnactivatedWallet(testContext.activationData, WalletType.USER)
            saveWalletForUser(UUID.randomUUID(), testContext.wallet)
        }
        suppose("Blockchain service will return transaction") {
            testContext.transactionData = generateTransactionData(testContext.walletHash)
            Mockito.`when`(
                blockchainService.addWallet(testContext.activationData)
            ).thenReturn(testContext.transactionData)
        }

        verify("Admin can generate activate wallet transaction") {
            val result = mockMvc.perform(
                post("$cooperativeWalletPath/${testContext.wallet.id}/transaction"))
                .andExpect(status().isOk)
                .andReturn()

            val transactionResponse: TransactionResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(transactionResponse.tx).isEqualTo(testContext.transactionData)
            assertThat(transactionResponse.txId).isNotNull()
            assertThat(transactionResponse.info.txType).isEqualTo(TransactionType.WALLET_ACTIVATE)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_WALLET])
    fun mustBeAbleToGetUsersWithUnactivatedWallet() {
        suppose("There are users with unactivated wallets") {
            val unactivatedWallet = createUnactivatedWallet("activation-data-1", WalletType.USER)
            val user = UUID.randomUUID()
            testContext.users.add(user)
            saveWalletForUser(user, unactivatedWallet)
            val secondUnactivatedWallet = createUnactivatedWallet("activation-data-2", WalletType.USER)
            val secondUser = UUID.randomUUID()
            testContext.users.add(secondUser)
            saveWalletForUser(secondUser, secondUnactivatedWallet)
        }
        suppose("There is user with activated wallet") {
            testContext.wallet = createWalletForUser(userUuid, testContext.walletHash)
        }
        suppose("Blockchain service will return data for users") {
            Mockito.`when`(
                userService.getUsers(testContext.users)
            ).thenReturn(listOf(createUserResponse(testContext.users[0]), createUserResponse(testContext.users[1])))
            Mockito.`when`(
                userService.getUsers(testContext.users.reversed())
            ).thenReturn(listOf(createUserResponse(testContext.users[1]), createUserResponse(testContext.users[0])))
        }

        verify("Admin can get a list of users with unactivated wallet") {
            val result = mockMvc.perform(get("$cooperativeWalletPath/user"))
                .andExpect(status().isOk)
                .andReturn()

            val userListResponse: UserWithWalletListResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(userListResponse.users).hasSize(2)
            assertThat(userListResponse.users.map { it.user.uuid })
                .containsAll(testContext.users)
                .doesNotContain(userUuid)
            assertThat(userListResponse.users.map { it.wallet }).doesNotContain(WalletResponse(testContext.wallet, 0))
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_WALLET])
    fun mustBeAbleToGetOrganizationsWithUnactivatedWallet() {
        suppose("There are organizations with unactivated wallets") {
            val organization = createOrganization("Unapproved 1", UUID.randomUUID())
            createUnactivatedWalletForOrganization(organization, "org-1")
            testContext.organizations.add(organization)
            val secondOrg = createOrganization("Unapproved 2", UUID.randomUUID())
            createUnactivatedWalletForOrganization(secondOrg, "org-2")
            testContext.organizations.add(secondOrg)
        }
        suppose("There is organization with activated wallet") {
            val organization = createOrganization("Org", userUuid)
            testContext.wallet = createWalletForOrganization(organization, testContext.walletHash)
        }

        verify("Admin can get a list of organization with unactivated wallet") {
            val result = mockMvc.perform(get("$cooperativeWalletPath/organization"))
                .andExpect(status().isOk)
                .andReturn()

            val orgListResponse: OrganizationWithWalletListResponse =
                objectMapper.readValue(result.response.contentAsString)
            assertThat(orgListResponse.organizations).hasSize(2)
            assertThat(orgListResponse.organizations.map { it.organization.id })
                .containsAll(testContext.organizations.map { it.id })
            assertThat(orgListResponse.organizations.map { it.wallet?.activationData })
                .doesNotContain(testContext.wallet.hash)
        }
    }

    @Test
    @WithMockCrowdfoundUser(privileges = [PrivilegeType.PRA_WALLET])
    fun mustBeAbleToGetProjectsWithUnactivatedWallet() {
        suppose("There are projects with unactivated wallets") {
            testContext.organization = createOrganization("Das org", UUID.randomUUID())
            val project = createProject("Unapproved project 1", testContext.organization, UUID.randomUUID())
            createUnactivatedWalletForProject(project, "project-1")
            testContext.projects.add(project)
            val secondProject = createProject("Unapproved project 2", testContext.organization, UUID.randomUUID())
            createUnactivatedWalletForProject(secondProject, "project-2")
            testContext.projects.add(secondProject)
        }
        suppose("There is projects with activated wallet") {
            val project = createProject("Approved", testContext.organization, userUuid)
            testContext.wallet = createWalletForProject(project, testContext.walletHash)
        }

        verify("Admin can get a list of projects with unactivated wallet") {
            val result = mockMvc.perform(get("$cooperativeWalletPath/project"))
                .andExpect(status().isOk)
                .andReturn()

            val projectListResponse: ProjectWithWalletListResponse =
                objectMapper.readValue(result.response.contentAsString)
            assertThat(projectListResponse.projects).hasSize(2)
            assertThat(projectListResponse.projects.map { it.project.id })
                .containsAll(testContext.projects.map { it.id })
            assertThat(projectListResponse.projects.map { it.wallet?.activationData })
                .doesNotContain(testContext.wallet.hash)
        }
    }

    private fun createUnactivatedWallet(activationData: String, type: WalletType): Wallet {
        val wallet = Wallet(0, activationData, type, Currency.EUR, ZonedDateTime.now(), null, null)
        return walletRepository.save(wallet)
    }

    private fun saveWalletForUser(user: UUID, wallet: Wallet): UserWallet {
        val userWallet = UserWallet(0, user, wallet)
        return userWalletRepository.save(userWallet)
    }

    protected fun createUnactivatedWalletForOrganization(organization: Organization, activationData: String): Wallet {
        val wallet = createUnactivatedWallet(activationData, WalletType.ORG)
        organization.wallet = wallet
        organizationRepository.save(organization)
        return wallet
    }

    protected fun createUnactivatedWalletForProject(project: Project, activationData: String): Wallet {
        val wallet = createUnactivatedWallet(activationData, WalletType.PROJECT)
        project.wallet = wallet
        projectRepository.save(project)
        return wallet
    }

    private class TestContext {
        val activationData = "0x5CA9a71B1d01849C0a95490Cc00559717fCF0D1d"
        val walletHash = "th_R26wx2hTnhmgDKJhXC9GAH3evCRnTyyXg4fivLLEAyiAcVW2K"
        val users = mutableListOf<UUID>()
        val organizations = mutableListOf<Organization>()
        val projects = mutableListOf<Project>()
        lateinit var transactionData: TransactionData
        lateinit var wallet: Wallet
        lateinit var organization: Organization
    }
}
