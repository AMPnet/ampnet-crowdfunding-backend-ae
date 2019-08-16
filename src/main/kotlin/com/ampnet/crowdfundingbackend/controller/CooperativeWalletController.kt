package com.ampnet.crowdfundingbackend.controller

import com.ampnet.crowdfundingbackend.controller.pojo.response.TransactionResponse
import com.ampnet.crowdfundingbackend.service.WalletService
import mu.KLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class CooperativeWalletController(private val walletService: WalletService) {

    companion object : KLogging()

    @PostMapping("/cooperative/wallet/{id}/transaction")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PWA_WALLET)")
    fun activateWallet(@PathVariable id: Int): ResponseEntity<TransactionResponse> {
        val userPrincipal = ControllerUtils.getUserPrincipalFromSecurityContext()
        logger.debug { "Received request to activate wallet: $id by user: ${userPrincipal.uuid}" }
        val transaction = walletService.generateWalletActivationTransaction(id, userPrincipal.uuid)
        return ResponseEntity.ok(TransactionResponse(transaction))
    }

    @GetMapping("/cooperative/wallet/user")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PRA_WALLET)")
    fun getUnactivatedUserWallets(): ResponseEntity<Unit> {
        // TODO: return a list of users with unactivated wallet
        return ResponseEntity.ok().build()
    }

    @GetMapping("/cooperative/wallet/organization")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PRA_WALLET)")
    fun getUnactivatedOrganizationWallets(): ResponseEntity<Unit> {
        // TODO: return a list of organizations with unactivated wallet
        return ResponseEntity.ok().build()
    }

    @GetMapping("/cooperative/wallet/project")
    @PreAuthorize("hasAuthority(T(com.ampnet.crowdfundingbackend.enums.PrivilegeType).PRA_WALLET)")
    fun getUnactivatedProjectWallets(): ResponseEntity<Unit> {
        // TODO: return a list of projects with unactivated wallet
        return ResponseEntity.ok().build()
    }
}
