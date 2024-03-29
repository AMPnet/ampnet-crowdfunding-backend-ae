package com.ampnet.crowdfundingbackend.userservice

import com.ampnet.userservice.proto.UserResponse
import java.util.UUID

interface UserService {
    fun getUsers(uuids: Iterable<UUID>): List<UserResponse>
}
