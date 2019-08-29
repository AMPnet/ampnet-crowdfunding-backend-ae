package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.blockchain.pojo.BlockchainTransaction
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.service.pojo.ProjectWithInvestment

data class ProjectWithInvestmentResponse(val project: ProjectResponse, val investment: Long) {
    constructor(projectWithInvestment: ProjectWithInvestment) : this(
        ProjectResponse(projectWithInvestment.project),
        projectWithInvestment.investment
    )
}
data class PortfolioResponse(val portfolio: List<ProjectWithInvestmentResponse>)
data class ProjectWithInvestments(val project: ProjectResponse, val transactions: List<BlockchainTransaction>) {
    constructor(project: Project, transactions: List<BlockchainTransaction>) : this (
        ProjectResponse(project),
        transactions
    )
}
