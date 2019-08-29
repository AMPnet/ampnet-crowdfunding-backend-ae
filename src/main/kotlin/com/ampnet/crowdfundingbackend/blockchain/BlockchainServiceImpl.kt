package com.ampnet.crowdfundingbackend.blockchain

import com.ampnet.crowdfunding.proto.BalanceRequest
import com.ampnet.crowdfunding.proto.BlockchainServiceGrpc
import com.ampnet.crowdfunding.proto.GenerateAddWalletTxRequest
import com.ampnet.crowdfunding.proto.GenerateApproveWithdrawTxRequest
import com.ampnet.crowdfunding.proto.GenerateBurnFromTxRequest
import com.ampnet.crowdfunding.proto.GenerateCreateOrganizationTxRequest
import com.ampnet.crowdfunding.proto.GenerateCreateProjectTxRequest
import com.ampnet.crowdfunding.proto.GenerateInvestTxRequest
import com.ampnet.crowdfunding.proto.GenerateMintTxRequest
import com.ampnet.crowdfunding.proto.InvestmentsInProjectRequest
import com.ampnet.crowdfunding.proto.PortfolioRequest
import com.ampnet.crowdfunding.proto.PostTxRequest
import com.ampnet.crowdfunding.proto.TransactionsRequest
import com.ampnet.crowdfundingbackend.blockchain.pojo.BlockchainTransaction
import com.ampnet.crowdfundingbackend.blockchain.pojo.ProjectInvestmentTxRequest
import com.ampnet.crowdfundingbackend.exception.ErrorCode
import com.ampnet.crowdfundingbackend.exception.InternalException
import com.ampnet.crowdfundingbackend.blockchain.pojo.GenerateProjectWalletRequest
import com.ampnet.crowdfundingbackend.blockchain.pojo.Portfolio
import com.ampnet.crowdfundingbackend.blockchain.pojo.PortfolioData
import com.ampnet.crowdfundingbackend.blockchain.pojo.TransactionData
import io.grpc.Status
import io.grpc.StatusRuntimeException
import mu.KLogging
import net.devh.boot.grpc.client.channelfactory.GrpcChannelFactory
import org.springframework.stereotype.Service

@Service
class BlockchainServiceImpl(
    private val grpcChannelFactory: GrpcChannelFactory
) : BlockchainService {

    companion object : KLogging()

    private val serviceBlockingStub: BlockchainServiceGrpc.BlockchainServiceBlockingStub by lazy {
        val channel = grpcChannelFactory.createChannel("blockchain-service")
        BlockchainServiceGrpc.newBlockingStub(channel)
    }

    override fun getBalance(hash: String): Long {
        logger.debug { "Fetching balance for hash: $hash" }
        try {
            val response = serviceBlockingStub.getBalance(
                BalanceRequest.newBuilder()
                    .setWalletTxHash(hash)
                    .build()
            )
            logger.info { "Received response: $response" }
            return response.balance.toLongOrNull()
                ?: throw InternalException(ErrorCode.INT_GRPC, "Cannot get balance as number")
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(ex, "Could not get balance for wallet: $hash")
        }
    }

    override fun addWallet(activationData: String): TransactionData {
        logger.info { "Adding wallet with activation data: $activationData" }
        try {
            val response = serviceBlockingStub.generateAddWalletTx(
                GenerateAddWalletTxRequest.newBuilder()
                    .setWallet(activationData)
                    .build()
            )
            logger.info { "Successfully added wallet: $response" }
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(ex, "Could not add wallet: $activationData")
        }
    }

    override fun generateCreateOrganizationTransaction(userWalletHash: String): TransactionData {
        logger.info { "Generating create organization: $userWalletHash" }
        try {
            val response = serviceBlockingStub.generateCreateOrganizationTx(
                GenerateCreateOrganizationTxRequest.newBuilder()
                    .setFromTxHash(userWalletHash)
                    .build()
            )
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(ex,
                "Could not generate transaction create organization: $userWalletHash")
        }
    }

    override fun generateProjectWalletTransaction(request: GenerateProjectWalletRequest): TransactionData {
        logger.info { "Generating create Project transaction" }
        try {
            val endTimeInMilliSeconds = request.endDate.toInstant().toEpochMilli()
            val response = serviceBlockingStub.generateCreateProjectTx(
                GenerateCreateProjectTxRequest.newBuilder()
                    .setFromTxHash(request.userWalletHash)
                    .setOrganizationTxHash(request.organizationHash)
                    .setMaxInvestmentPerUser(request.maxPerUser.toString())
                    .setMinInvestmentPerUser(request.minPerUser.toString())
                    .setInvestmentCap(request.investmentCap.toString())
                    .setEndInvestmentTime(endTimeInMilliSeconds.toString())
                    .build()
            )
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(ex, "Could not generate create Project transaction: $request")
        }
    }

    override fun postTransaction(transaction: String): String {
        logger.info { "Post transaction" }
        try {
            val response = serviceBlockingStub.postTransaction(
                PostTxRequest.newBuilder()
                    .setData(transaction)
                    .build()
            )
            return response.txHash
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(ex, "Could not post transaction: $transaction")
        }
    }

    override fun generateProjectInvestmentTransaction(request: ProjectInvestmentTxRequest): TransactionData {
        logger.info { "User: ${request.userWalletHash} is investing to project: ${request.projectWalletHash} " +
            "with amount ${request.amount}" }
        try {
            val response = serviceBlockingStub.generateInvestTx(
                GenerateInvestTxRequest.newBuilder()
                    .setFromTxHash(request.userWalletHash)
                    .setProjectTxHash(request.projectWalletHash)
                    .setAmount(request.amount.toString())
                    .build()
            )
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(
                ex, "Could not invest in project: ${request.projectWalletHash}")
        }
    }

    override fun generateMintTransaction(toHash: String, amount: Long): TransactionData {
        logger.warn { "Generating Mint transaction toHash: $toHash with amount = $amount" }
        try {
            val response = serviceBlockingStub.generateMintTx(
                GenerateMintTxRequest.newBuilder()
                    .setToTxHash(toHash)
                    .setAmount(amount.toString())
                    .build()
            )
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(ex, "Could not Mint toHash: $toHash")
        }
    }

    override fun generateBurnTransaction(burnFromTxHash: String): TransactionData {
        logger.warn { "Generating Burn transaction burnFromTxHash: $burnFromTxHash" }
        try {
            val response = serviceBlockingStub.generateBurnFromTx(
                GenerateBurnFromTxRequest.newBuilder()
                    .setBurnFromTxHash(burnFromTxHash)
                    .build()
            )
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(ex, "Could not Burn toHash: $burnFromTxHash")
        }
    }

    override fun generateApproveBurnTransaction(burnFromTxHash: String, amount: Long): TransactionData {
        logger.info { "Generating Approve Burn Transaction burnFromTxHash: $burnFromTxHash with amount = $amount" }
        try {
            val response = serviceBlockingStub.generateApproveWithdrawTx(
                GenerateApproveWithdrawTxRequest.newBuilder()
                    .setFromTxHash(burnFromTxHash)
                    .setAmount(amount.toString())
                    .build()
            )
            return TransactionData(response)
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(ex, "Could not Burn toHash: $burnFromTxHash")
        }
    }

    override fun getPortfolio(hash: String): Portfolio {
        logger.debug { "Get user portfolio for wallet hash: $hash" }
        try {
            val response = serviceBlockingStub.getPortfolio(
                PortfolioRequest.newBuilder()
                    .setTxHash(hash)
                    .build()
            )
            logger.debug { "Get user portfolio response: $response" }
            val portfolioData = response.portfolioList.map { PortfolioData(it) }
            return Portfolio(portfolioData)
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(ex, "Could not get portfolio for wallet: $hash")
        }
    }

    override fun getTransactions(hash: String): List<BlockchainTransaction> {
        logger.debug { "Get transactions for wallet hash: $hash" }
        try {
            val response = serviceBlockingStub.getTransactions(
                TransactionsRequest.newBuilder()
                    .setTxHash(hash)
                    .build()
            )
            return response.transactionsList.map { BlockchainTransaction(it) }
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(ex, "Could not get transactions for wallet: $hash")
        }
    }

    override fun getInvestmentsInProject(
        userWalletHash: String,
        projectWalletHash: String
    ): List<BlockchainTransaction> {
        logger.debug { "Get investments by user: $userWalletHash in project: $projectWalletHash" }
        try {
            val response = serviceBlockingStub.getInvestmentsInProject(
                InvestmentsInProjectRequest.newBuilder()
                    .setFromTxHash(userWalletHash)
                    .setProjectTxHash(projectWalletHash)
                    .build()
            )
            return response.transactionsList.map { BlockchainTransaction(it) }
        } catch (ex: StatusRuntimeException) {
            throw getInternalExceptionFromStatusException(ex,
                "Could not get investments by user: $userWalletHash in project: $projectWalletHash")
        }
    }

    private fun getInternalExceptionFromStatusException(
        ex: StatusRuntimeException,
        message: String
    ): InternalException {
        val grpcErrorCode = getErrorDescriptionFromExceptionStatus(ex.status)
        val errorCode = ErrorCode.INT_GRPC
        errorCode.specificCode = grpcErrorCode.code
        errorCode.message = grpcErrorCode.message
        return InternalException(errorCode, message)
    }

    // Status defined in ampenet-blockchain service, for more info see:
    // ampnet-blockchain-service/src/main/kotlin/com/ampnet/crowdfunding/blockchain/enums/ErrorCode.kt
    private fun getErrorDescriptionFromExceptionStatus(status: Status): GrpcErrorCode {
        val description = status.description?.split(" > ")
            ?: return GrpcErrorCode("90", "Could not parse error: ${status.description}")
        if (description.size != 2) {
            return GrpcErrorCode("91", "Wrong size of error message: $description")
        }
        return GrpcErrorCode(description[0], description[1])
    }

    private data class GrpcErrorCode(val code: String, val message: String)
}
