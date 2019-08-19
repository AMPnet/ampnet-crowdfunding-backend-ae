package com.ampnet.crowdfundingbackend.service.pojo

import com.ampnet.crowdfundingbackend.persistence.model.Wallet
import com.ampnet.userservice.proto.UserResponse

data class UserWithWallet(val userResponse: UserResponse, val wallet: Wallet)
