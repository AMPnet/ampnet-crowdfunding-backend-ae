package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.crowdfundingbackend.persistence.repository.UserWalletRepository
import java.util.Optional
import java.util.UUID

internal object ServiceUtils {
    fun <T> wrapOptional(optional: Optional<T>): T? {
        return if (optional.isPresent) optional.get() else null
    }

    fun getUserWalletHash(user: UUID, userWalletRepository: UserWalletRepository): String {
        val wallet = userWalletRepository.findByUserUuid(user).orElseThrow {
            throw ResourceNotFoundException(
                ErrorCode.WALLET_MISSING,
                "User must have a wallet to make Withdraw request")
        }
        return getWalletHash(wallet.wallet)
    }

    fun getProjectWalletHash(project: Project): String {
        val projectWallet = project.wallet
            ?: throw ResourceNotFoundException(ErrorCode.WALLET_MISSING, "Project does not have the wallet")
        return getWalletHash(projectWallet)
    }

    private fun getWalletHash(wallet: Wallet) = wallet.hash
        ?: throw ResourceNotFoundException(ErrorCode.WALLET_NOT_ACTIVATED, "Wallet not activated")
}
