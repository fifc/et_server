package com.ethwal.server.repository

import com.ethwal.server.model.UserTrans
import org.springframework.data.mongodb.repository.ReactiveMongoRepository
import org.springframework.stereotype.Repository

@Repository
interface TransRepository : ReactiveMongoRepository<UserTrans, String>