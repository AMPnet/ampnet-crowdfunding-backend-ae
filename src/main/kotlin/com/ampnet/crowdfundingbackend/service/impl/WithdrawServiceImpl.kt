package com.ampnet.crowdfundingbackend.service.impl

import com.ampnet.crowdfundingbackend.blockchain.BlockchainService
import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionDataAndInfo
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InvalidRequestException
import com.ampnet.crowdfundingbackend.exception.ResourceAlreadyExistsException
import com.ampnet.crowdfundingbackend.exception.ResourceNotFoundException
import com.ampnet.crowdfundingbackend.persistence.model.Withdraw
import com.ampnet.crowdfundingbackend.persistence.repository.UserWalletRepository
import com.ampnet.crowdfundingbackend.persistence.repository.WithdrawRepository
import com.ampnet.crowdfundingbackend.service.MailService
import com.ampnet.crowdfundingbackend.service.StorageService
import com.ampnet.crowdfundingbackend.service.TransactionInfoService
import com.ampnet.crowdfundingbackend.service.WithdrawService
import com.ampnet.crowdfundingbackend.service.pojo.DocumentSaveRequest
import mu.KLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID

@Service
class WithdrawServiceImpl(
    private val withdrawRepository: WithdrawRepository,
    private val userWalletRepository: UserWalletRepository,
    private val blockchainService: BlockchainService,
    private val transactionInfoService: TransactionInfoService,
    private val storageService: StorageService,
    private val mailService: MailService
) : WithdrawService {

    companion object : KLogging()

    @Transactional(readOnly = true)
    override fun getPendingForUser(user: UUID): Withdraw? {
        return withdrawRepository.findByUserUuid(user).find { it.approvedTxHash == null }
    }

    @Transactional(readOnly = true)
    override fun getAllApproved(): List<Withdraw> {
        return withdrawRepository.findAllApproved()
    }

    @Transactional(readOnly = true)
    override fun getAllBurned(): List<Withdraw> {
        return withdrawRepository.findAllBurned()
    }

    @Transactional
    override fun createWithdraw(user: UUID, amount: Long, bankAccount: String): Withdraw {
        validateUserDoesNotHavePendingWithdraw(user)
        checkIfUserHasEnoughFunds(user, amount)
        val withdraw = Withdraw(0, user, amount, ZonedDateTime.now(), bankAccount,
                null, null, null, null, null, null)
        withdrawRepository.save(withdraw)
        mailService.sendWithdrawRequest(user, amount)
        logger.debug { "Created Withdraw for user: $user with amount: $amount" }
        return withdraw
    }

    @Transactional
    override fun deleteWithdraw(withdrawId: Int) {
        val withdraw = getWithdraw(withdrawId)
        if (withdraw.burnedTxHash != null) {
            throw InvalidRequestException(ErrorCode.WALLET_WITHDRAW_BURNED, "Cannot delete burned Withdraw")
        }
        logger.info { "Deleting Withdraw with id: $withdraw" }
        withdrawRepository.delete(withdraw)
        mailService.sendWithdrawInfo(withdraw.userUuid, false)
    }

    @Transactional
    override fun generateApprovalTransaction(withdrawId: Int, user: UUID): TransactionDataAndInfo {
        val withdraw = getWithdraw(withdrawId)
        if (withdraw.userUuid != user) {
            throw InvalidRequestException(ErrorCode.WALLET_WITHDRAW_MISSING, "Withdraw does not belong to this user")
        }
        validateWithdrawForApproval(withdraw)
        val userWallet = ServiceUtils.getUserWalletHash(withdraw.userUuid, userWalletRepository)
        val data = blockchainService.generateApproveBurnTransaction(userWallet, withdraw.amount)
        val info = transactionInfoService.createApprovalTransaction(withdraw.amount, user, withdraw.id)
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    override fun confirmApproval(signedTransaction: String, withdrawId: Int): Withdraw {
        val withdraw = getWithdraw(withdrawId)
        validateWithdrawForApproval(withdraw)
        logger.info { "Approving Withdraw: $withdraw" }
        val approvalTxHash = blockchainService.postTransaction(signedTransaction)
        withdraw.approvedTxHash = approvalTxHash
        withdraw.approvedAt = ZonedDateTime.now()
        logger.info { "Approved Withdraw: $withdraw" }
        return withdrawRepository.save(withdraw)
    }

    @Transactional
    override fun generateBurnTransaction(withdrawId: Int, user: UUID): TransactionDataAndInfo {
        val withdraw = getWithdraw(withdrawId)
        validateWithdrawForBurn(withdraw)
        val userWallet = ServiceUtils.getUserWalletHash(withdraw.userUuid, userWalletRepository)
        val data = blockchainService.generateBurnTransaction(userWallet)
        val info = transactionInfoService.createBurnTransaction(withdraw.amount, user, withdraw.id)
        withdraw.burnedBy = user
        withdrawRepository.save(withdraw)
        return TransactionDataAndInfo(data, info)
    }

    @Transactional
    override fun burn(signedTransaction: String, withdrawId: Int): Withdraw {
        val withdraw = getWithdraw(withdrawId)
        validateWithdrawForBurn(withdraw)
        logger.info { "Burning Withdraw: $withdraw" }
        val burnedTxHash = blockchainService.postTransaction(signedTransaction)
        withdraw.burnedTxHash = burnedTxHash
        withdraw.burnedAt = ZonedDateTime.now()
        withdrawRepository.save(withdraw)
        logger.info { "Burned Withdraw: $withdraw" }
        mailService.sendWithdrawInfo(withdraw.userUuid, true)
        return withdraw
    }

    @Transactional
    override fun addDocument(withdrawId: Int, request: DocumentSaveRequest): Withdraw {
        val withdraw = getWithdraw(withdrawId)
        val document = storageService.saveDocument(request)
        withdraw.document = document
        return withdrawRepository.save(withdraw)
    }

    private fun checkIfUserHasEnoughFunds(user: UUID, amount: Long) {
        val walletHash = ServiceUtils.getUserWalletHash(user, userWalletRepository)
        val balance = blockchainService.getBalance(walletHash)
        if (amount > balance) {
            throw InvalidRequestException(ErrorCode.WALLET_FUNDS, "Insufficient funds")
        }
    }

    private fun validateUserDoesNotHavePendingWithdraw(user: UUID) {
        withdrawRepository.findByUserUuid(user).forEach {
            if (it.approvedTxHash == null) {
                throw ResourceAlreadyExistsException(ErrorCode.WALLET_WITHDRAW_EXISTS, "Unapproved Withdraw: ${it.id}")
            }
            if (it.approvedTxHash != null && it.burnedTxHash == null) {
                throw ResourceAlreadyExistsException(ErrorCode.WALLET_WITHDRAW_EXISTS, "Unburned Withdraw: ${it.id}")
            }
        }
    }

    private fun validateWithdrawForApproval(withdraw: Withdraw) {
        if (withdraw.approvedTxHash != null) {
            throw InvalidRequestException(
                    ErrorCode.WALLET_WITHDRAW_APPROVED, "Approved txHash: ${withdraw.approvedTxHash}")
        }
    }

    private fun validateWithdrawForBurn(withdraw: Withdraw) {
        if (withdraw.approvedTxHash == null) {
            throw InvalidRequestException(
                    ErrorCode.WALLET_WITHDRAW_NOT_APPROVED, "Withdraw must be approved")
        }
        if (withdraw.burnedTxHash != null) {
            throw InvalidRequestException(ErrorCode.WALLET_WITHDRAW_BURNED, "Burned txHash: ${withdraw.burnedTxHash}")
        }
    }

    private fun getWithdraw(withdrawId: Int): Withdraw {
        return withdrawRepository.findById(withdrawId).orElseThrow {
            throw ResourceNotFoundException(ErrorCode.WALLET_WITHDRAW_MISSING, "Missing withdraw with id: $withdrawId")
        }
    }
}
