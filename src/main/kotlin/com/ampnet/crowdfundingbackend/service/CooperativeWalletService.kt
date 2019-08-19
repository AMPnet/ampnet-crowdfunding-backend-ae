package com.ampnet.crowdfundingbackend.service

import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.service.pojo.UserWithWallet

interface CooperativeWalletService {
    fun getAllUserWithUnactivatedWallet(): List<UserWithWallet>
    fun getOrganizationsWithUnactivatedWallet(): List<Organization>
    fun getProjectsWithUnactivatedWallet(): List<Project>
}
