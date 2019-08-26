package com.ampnet.crowdfundingbackend.service.pojo

import com.ampnet.crowdfundingbackend.persistence.model.Project

data class ProjectWithInvestment(val project: Project, val investment: Long)
