package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.service.pojo.PortfolioStats
import com.ampnet.crowdfundingbackend.service.pojo.ProjectWithInvestment
import java.util.UUID

interface PortfolioService {
    fun getPortfolio(user: UUID): List<ProjectWithInvestment>
    fun getPortfolioStats(user: UUID): PortfolioStats
}
