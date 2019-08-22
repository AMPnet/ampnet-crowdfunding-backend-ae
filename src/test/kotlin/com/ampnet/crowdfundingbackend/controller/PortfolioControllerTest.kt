package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfunding.proto.TransactionsResponse
import com.ampnet.crowdfundingbackend.blockchain.pojo.BlockchainTransaction
import com.ampnet.crowdfundingbackend.blockchain.pojo.Portfolio
import com.ampnet.crowdfundingbackend.blockchain.pojo.PortfolioData
import com.ampnet.crowdfundingbackend.controller.pojo.response.PortfolioResponse
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
            assertThat(project.projectResponse.id).isEqualTo(testContext.project.id)
            assertThat(project.investment).isEqualTo(testContext.portfolio.data.first().amount)

            val secondProject = portfolioResponse.portfolio[1]
            assertThat(secondProject.projectResponse.id).isEqualTo(testContext.secondProject.id)
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
                BlockchainTransaction(walletHash, "to", 1000, TransactionsResponse.Transaction.Type.INVEST),
                BlockchainTransaction(walletHash, "to_2", 1000, TransactionsResponse.Transaction.Type.INVEST),
                BlockchainTransaction("from", walletHash, 10, TransactionsResponse.Transaction.Type.SHARE_PAYOUT),
                BlockchainTransaction("from_2", walletHash, 10, TransactionsResponse.Transaction.Type.SHARE_PAYOUT)
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
            assertThat(stats.returnOnInvestment).isEqualTo(1.toDouble())
        }
    }

    private class TestContext {
        lateinit var userWallet: Wallet
        lateinit var project: Project
        lateinit var secondProject: Project
        lateinit var portfolio: Portfolio
    }
}
