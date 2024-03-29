package com.ampnet.crowdfundingbackend.persistence.model

import com.ampnet.crowdfundingbackend.enums.Currency
import com.ampnet.crowdfundingbackend.enums.WalletType
import java.time.ZonedDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "wallet")
data class Wallet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Int,

    @Column(nullable = false, length = 128)
    val activationData: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    val type: WalletType,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    val currency: Currency,

    @Column(nullable = false)
    val createdAt: ZonedDateTime,

    @Column(length = 128)
    var hash: String?,

    @Column
    var activatedAt: ZonedDateTime?
)
