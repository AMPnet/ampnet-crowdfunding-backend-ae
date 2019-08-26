package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.service.pojo.ProjectWithInvestment

data class ProjectWithInvestmentResponse(val projectResponse: ProjectResponse, val investment: Long) {
    constructor(projectWithInvestment: ProjectWithInvestment) : this(
        ProjectResponse(projectWithInvestment.project),
        projectWithInvestment.investment
    )
}
data class PortfolioResponse(val portfolio: List<ProjectWithInvestmentResponse>)
