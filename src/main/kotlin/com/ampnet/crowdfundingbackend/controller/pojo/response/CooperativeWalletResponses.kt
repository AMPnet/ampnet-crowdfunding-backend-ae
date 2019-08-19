package com.ampnet.crowdfundingbackend.controller.pojo.response

import com.ampnet.crowdfundingbackend.persistence.model.Organization
import com.ampnet.crowdfundingbackend.persistence.model.Project
import com.ampnet.crowdfundingbackend.service.pojo.UserWithWallet

data class UserWithWalletResponse(val user: UserControllerResponse, val wallet: WalletResponse) {
    constructor(userWithWallet: UserWithWallet) : this(
        UserControllerResponse(userWithWallet.userResponse), WalletResponse(userWithWallet.wallet)
    )
}
data class UserWithWalletListResponse(val users: List<UserWithWalletResponse>)

data class OrganizationWithWalletResponse(val organization: OrganizationResponse, val wallet: WalletResponse?) {
    constructor(org: Organization) : this(
        OrganizationResponse(org),
        org.wallet?.let { WalletResponse(it) }
    )
}
data class OrganizationWithWalletListResponse(val organizations: List<OrganizationWithWalletResponse>)

data class ProjectWithWalletResponse(val project: ProjectResponse, val wallet: WalletResponse?) {
    constructor(project: Project) : this(
        ProjectResponse(project),
        project.wallet?.let { WalletResponse(it) }
    )
}
data class ProjectWithWalletListResponse(val projects: List<ProjectWithWalletResponse>)
