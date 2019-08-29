package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.response.PortfolioResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectWithInvestmentResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectWithInvestments
import com.ampnet.crowdfundingbackend.service.PortfolioService
import com.ampnet.crowdfundingbackend.service.ProjectService
import com.ampnet.crowdfundingbackend.service.pojo.PortfolioStats
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

@RestController
class PortfolioController(
    private val portfolioService: PortfolioService,
    private val projectService: ProjectService
) {

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

    @GetMapping("/portfolio/project/{id}")
    fun getPortfolioForProject(@PathVariable id: Int): ResponseEntity<ProjectWithInvestments> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to get my portfolio for project: $id by user: ${userPrincipal.uuid}" }
        projectService.getProjectById(id)?.let { project ->
            val transactions = portfolioService.getInvestmentsInProject(userPrincipal.uuid, project)
            return ResponseEntity.ok(ProjectWithInvestments(project, transactions))
        }
        return ResponseEntity.notFound().build()
    }
}
