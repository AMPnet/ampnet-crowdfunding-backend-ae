package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.repository.OrganizationRepository
import com.ampnet.crowdfundingbackend.persistence.repository.ProjectRepository
import com.ampnet.crowdfundingbackend.persistence.repository.UserWalletRepository
import com.ampnet.crowdfundingbackend.service.CooperativeWalletService
import com.ampnet.crowdfundingbackend.service.pojo.UserWithWallet
import com.ampnet.crowdfundingbackend.userservice.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CooperativeWalletServiceImpl(
    private val userWalletRepository: UserWalletRepository,
    private val organizationRepository: OrganizationRepository,
    private val projectRepository: ProjectRepository,
    private val userService: UserService
) : CooperativeWalletService {

    @Transactional(readOnly = true)
    override fun getAllUserWithUnactivatedWallet(): List<UserWithWallet> {
        val userWallets = userWalletRepository.findAllWithUnactivatedWallet().associateBy { it.userUuid }
        val users = userService.getUsers(userWallets.keys.toList())
        return users.mapNotNull { user ->
            userWallets[UUID.fromString(user.uuid)]?.let { wallet ->
                UserWithWallet(user, wallet.wallet)
            }
        }
    }

    @Transactional(readOnly = true)
    override fun getOrganizationsWithUnactivatedWallet(): List<Organization> {
        return organizationRepository.findAllWithUnactivatedWallet()
    }

    @Transactional(readOnly = true)
    override fun getProjectsWithUnactivatedWallet(): List<Project> {
        return projectRepository.findAllWithUnactivatedWallet()
    }
}
