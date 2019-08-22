package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.response.PortfolioResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectWithInvestmentResponse
import com.ampnet.crowdfundingbackend.service.PortfolioService
import com.ampnet.crowdfundingbackend.service.pojo.PortfolioStats
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class PortfolioController(private val portfolioService: PortfolioService) {

    companion object : KLogging()

    @GetMapping("/portfolio")
    fun getMyPortfolio(): ResponseEntity<PortfolioResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get my portfolio by user: ${userPrincipal.uuid}" }
        val responseList = portfolioService.getPortfolio(userPrincipal.uuid)
            .map { ProjectWithInvestmentResponse(it) }
        return ResponseEntity.ok(PortfolioResponse(responseList))
    }

    @GetMapping("/portfolio/stats")
    fun getMyPortfolioStats(): ResponseEntity<PortfolioStats> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get my portfolio stats by user: ${userPrincipal.uuid}" }
        val portfolioStats = portfolioService.getPortfolioStats(userPrincipal.uuid)
        return ResponseEntity.ok(portfolioStats)
    }
}
