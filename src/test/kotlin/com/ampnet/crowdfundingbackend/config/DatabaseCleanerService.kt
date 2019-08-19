package com.ampnet.crowdfundingbackend.config

import org.springframework.stereotype.Service
import javax.persistence.EntityManager
import javax.transaction.Transactional

@Service
class DatabaseCleanerService(val em: EntityManager) {

    @Transactional
    fun deleteAllWalletsAndOwners() {
        em.createNativeQuery("TRUNCATE wallet CASCADE").executeUpdate()
    }

    @Transactional
    fun deleteAllOrganizations() {
        em.createNativeQuery("TRUNCATE organization CASCADE").executeUpdate()
    }

    @Transactional
    fun deleteAllOrganizationMemberships() {
        em.createNativeQuery("DELETE FROM organization_membership").executeUpdate()
    }

    @Transactional
    fun deleteAllOrganizationFollowers() {
        em.createNativeQuery("DELETE FROM organization_follower").executeUpdate()
    }

    @Transactional
    fun deleteAllOrganizationInvitations() {
        em.createNativeQuery("DELETE FROM organization_invitation").executeUpdate()
    }

    @Transactional
    fun deleteAllProjects() {
        em.createNativeQuery("TRUNCATE project CASCADE").executeUpdate()
    }

    @Transactional
    fun deleteAllWallets() {
        em.createNativeQuery("DELETE FROM user_wallet").executeUpdate()
        em.createNativeQuery("DELETE FROM wallet").executeUpdate()
    }

    @Transactional
    fun deleteAllTransactionInfo() {
        em.createNativeQuery("DELETE FROM transaction_info ").executeUpdate()
    }

    @Transactional
    fun deleteAllPairWalletCodes() {
        em.createNativeQuery("DELETE FROM pair_wallet_code").executeUpdate()
    }

    @Transactional
    fun deleteAllWithdraws() {
        em.createNativeQuery("DELETE FROM withdraw").executeUpdate()
    }

    @Transactional
    fun deleteAllDeposits() {
        em.createNativeQuery("DELETE FROM deposit").executeUpdate()
    }
}
