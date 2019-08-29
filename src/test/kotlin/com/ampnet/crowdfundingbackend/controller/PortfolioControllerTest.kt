package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfunding.proto.TransactionsResponse
import com.ampnet.crowdfundingbackend.blockchain.pojo.BlockchainTransaction
import com.ampnet.crowdfundingbackend.blockchain.pojo.Portfolio
import com.ampnet.crowdfundingbackend.blockchain.pojo.PortfolioData
import com.ampnet.crowdfundingbackend.controller.pojo.response.PortfolioResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectWithInvestments
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.security.WithMockCrowdfoundUser
import com.ampnet.crowdfundingbackend.service.pojo.PortfolioStats
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.ZonedDateTime
import java.util.UUID

class PortfolioControllerTest : ControllerTestBase() {

    private val portfolioPath = "/portfolio"

    private lateinit var testContext: TestContext

    @BeforeEach
    fun init() {
        databaseCleanerService.deleteAllWalletsAndOwners()
        testContext = TestContext()
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetMyPortfolio() {
        suppose("User has wallet") {
            testContext.userWallet = createWalletForUser(userUuid, "user-wallet-hash")
        }
        suppose("Project has wallet") {
            val organization = createOrganization("Port org", UUID.randomUUID())
            testContext.project = createProject("Portfolio project", organization, UUID.randomUUID())
            createWalletForProject(testContext.project, "1-project-wallet-hash")
            testContext.secondProject = createProject("Second project", organization, UUID.randomUUID())
            createWalletForProject(testContext.secondProject, "2-project-wallet-hash")
        }
        suppose("Blockchain service will return portfolio") {
            testContext.portfolio = Portfolio(listOf(
                PortfolioData(getWalletHash(testContext.project.wallet), 10_000_00),
                PortfolioData(getWalletHash(testContext.secondProject.wallet), 50_000_00)
            ))
            Mockito.`when`(
                blockchainService.getPortfolio(getWalletHash(testContext.userWallet))
            ).thenReturn(testContext.portfolio)
        }

        verify("User can get my portfolio") {
            val result = mockMvc.perform(get(portfolioPath))
                .andExpect(status().isOk)
                .andReturn()

            val portfolioResponse: PortfolioResponse = objectMapper.readValue(result.response.contentAsString)
            assertThat(portfolioResponse.portfolio).hasSize(2)
            val project = portfolioResponse.portfolio.first()
            assertThat(project.project.id).isEqualTo(testContext.project.id)
            assertThat(project.investment).isEqualTo(testContext.portfolio.data.first().amount)

            val secondProject = portfolioResponse.portfolio[1]
            assertThat(secondProject.project.id).isEqualTo(testContext.secondProject.id)
            assertThat(secondProject.investment).isEqualTo(testContext.portfolio.data[1].amount)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetMyPortfolioStats() {
        suppose("User has wallet") {
            testContext.userWallet = createWalletForUser(userUuid, "user-wallet-hash")
        }
        suppose("Blockchain service will return portfolio stats") {
            val walletHash = getWalletHash(testContext.userWallet)
            val transactions = listOf(
                BlockchainTransaction(walletHash, "to", 1000,
                    TransactionsResponse.Transaction.Type.INVEST, ZonedDateTime.now()),
                BlockchainTransaction(walletHash, "to_2", 1000, TransactionsResponse.Transaction.Type.INVEST,
                    ZonedDateTime.now()),
                BlockchainTransaction("from", walletHash, 10, TransactionsResponse.Transaction.Type.SHARE_PAYOUT,
                    ZonedDateTime.now()),
                BlockchainTransaction("from_2", walletHash, 10, TransactionsResponse.Transaction.Type.SHARE_PAYOUT,
                    ZonedDateTime.now())
            )
            Mockito.`when`(
                blockchainService.getTransactions(getWalletHash(testContext.userWallet))
            ).thenReturn(transactions)
        }

        verify("User can get portfolio stats") {
            val result = mockMvc.perform(get("$portfolioPath/stats"))
                .andExpect(status().isOk)
                .andReturn()

            val stats: PortfolioStats = objectMapper.readValue(result.response.contentAsString)
            assertThat(stats.investments).isEqualTo(2000)
            assertThat(stats.earnings).isEqualTo(20)
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustBeAbleToGetInvestmentsInProject() {
        suppose("User has wallet") {
            testContext.userWallet = createWalletForUser(userUuid, "user-wallet-hash")
        }
        suppose("Project has wallet") {
            val organization = createOrganization("Port org", UUID.randomUUID())
            testContext.project = createProject("Portfolio project", organization, UUID.randomUUID())
            createWalletForProject(testContext.project, "project-wallet-hash")
        }
        suppose("Blockchain service will return investments in project") {
            testContext.transactions = listOf(
                createInvestmentInProject(10_000_00),
                createInvestmentInProject(5000_00)
            )
            Mockito.`when`(
                blockchainService.getInvestmentsInProject(
                    getWalletHash(testContext.userWallet), getWalletHash(testContext.project.wallet))
            ).thenReturn(testContext.transactions)
        }

        verify("User can get a list of investments in project") {
            val result = mockMvc.perform(get("$portfolioPath/project/${testContext.project.id}"))
                .andExpect(status().isOk)
                .andReturn()

            val projectWithInvestments: ProjectWithInvestments = objectMapper.readValue(result.response.contentAsString)
            assertThat(projectWithInvestments.project.id).isEqualTo(testContext.project.id)
            assertThat(projectWithInvestments.transactions.map { it.amount }).hasSize(2)
                .containsAll(testContext.transactions.map { it.amount })
        }
    }

    @Test
    @WithMockCrowdfoundUser
    fun mustGetNotFoundForInvestmentsInNonExistingProject() {
        verify("Controller will return not found for non existing project") {
            mockMvc.perform(get("$portfolioPath/project/0"))
                .andExpect(status().isNotFound)
        }
    }

    private fun createInvestmentInProject(amount: Long): BlockchainTransaction =
        BlockchainTransaction(
            getWalletHash(testContext.userWallet),
            getWalletHash(testContext.project.wallet), amount,
            TransactionsResponse.Transaction.Type.INVEST,
            ZonedDateTime.now()
        )

    private class TestContext {
        lateinit var userWallet: Wallet
        lateinit var project: Project
        lateinit var secondProject: Project
        lateinit var portfolio: Portfolio
        lateinit var transactions: List<BlockchainTransaction>
    }
}
