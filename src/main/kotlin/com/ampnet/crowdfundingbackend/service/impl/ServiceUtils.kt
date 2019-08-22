package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
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
        return wallet.wallet.hash
            ?: throw throw ResourceNotFoundException(ErrorCode.WALLET_NOT_ACTIVATED, "Not activated")
    }
}
