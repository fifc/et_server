package com.ethwal.server.repository

import com.ethwal.server.model.Wallet
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

@Repository
interface WalletRepository : ReactiveMongoRepository<Wallet, String>