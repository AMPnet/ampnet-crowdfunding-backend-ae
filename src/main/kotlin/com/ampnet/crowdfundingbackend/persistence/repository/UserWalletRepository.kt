package com.ampnet.crowdfundingbackend.persistence.repository

import com.ampnet.crowdfundingbackend.persistence.model.UserWallet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.util.Optional
import java.util.UUID

interface UserWalletRepository : JpaRepository<UserWallet, Int> {
    fun findByUserUuid(userUuid: UUID): Optional<UserWallet>

    @Query("SELECT userWallet FROM UserWallet userWallet JOIN FETCH userWallet.wallet wallet " +
        "WHERE wallet.hash IS NULL")
    fun findAllWithUnactivatedWallet(): List<UserWallet>
}
