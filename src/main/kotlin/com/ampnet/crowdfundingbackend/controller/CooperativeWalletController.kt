package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationWithWalletListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.OrganizationWithWalletResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectWithWalletListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.ProjectWithWalletResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.TransactionResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.UserWithWalletListResponse
import com.ampnet.crowdfundingbackend.controller.pojo.response.UserWithWalletResponse
import com.ampnet.crowdfundingbackend.service.CooperativeWalletService
import com.ampnet.crowdfundingbackend.service.WalletService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CooperativeWalletController(
    private val walletService: WalletService,
    private val cooperativeWalletService: CooperativeWalletService
) {

    companion object : KLogging()

    @PostMapping("/cooperative/wallet/{id}/transaction")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PWA_WALLET)")
    fun activateWalletTransaction(@PathVariable id: Int): ResponseEntity<TransactionResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.info { "Received request to activate wallet: $id by user: ${userPrincipal.uuid}" }
        val transaction = walletService.generateWalletActivationTransaction(id, userPrincipal.uuid)
        return ResponseEntity.ok(TransactionResponse(transaction))
    }

    @GetMapping("/cooperative/wallet/user")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PRA_WALLET)")
    fun getUnactivatedUserWallets(): ResponseEntity<UserWithWalletListResponse> {
        logger.debug { "Received request to get list of users with unactivated wallet" }
        val users = cooperativeWalletService.getAllUserWithUnactivatedWallet()
        val usersResponse = users.map { UserWithWalletResponse(it) }
        return ResponseEntity.ok(UserWithWalletListResponse(usersResponse))
    }

    @GetMapping("/cooperative/wallet/organization")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PRA_WALLET)")
    fun getUnactivatedOrganizationWallets(): ResponseEntity<OrganizationWithWalletListResponse> {
        logger.debug { "Received request to get list of organizations with unactivated wallet" }
        val organizations = cooperativeWalletService.getOrganizationsWithUnactivatedWallet()
        val map = organizations.map { OrganizationWithWalletResponse(it) }
        return ResponseEntity.ok(OrganizationWithWalletListResponse(map))
    }

    @GetMapping("/cooperative/wallet/project")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PRA_WALLET)")
    fun getUnactivatedProjectWallets(): ResponseEntity<ProjectWithWalletListResponse> {
        logger.debug { "Received request to get list of projects with unactivated wallet" }
        val projects = cooperativeWalletService.getProjectsWithUnactivatedWallet()
            .map { ProjectWithWalletResponse(it) }
        return ResponseEntity.ok(ProjectWithWalletListResponse(projects))
    }
}
