package com.aidestinymaster.data.repository

import android.content.Context
import com.aidestinymaster.data.db.DatabaseProvider
import com.aidestinymaster.data.db.WalletDao
import com.aidestinymaster.data.db.WalletEntity

class WalletRepository private constructor(private val dao: WalletDao) {
    suspend fun ensure(): WalletEntity {
        val cur = dao.get()
        if (cur != null) return cur
        val def = WalletEntity(coins = 0, lastEarnedAt = null, lastSpentAt = null)
        dao.upsert(def)
        return def
    }

    suspend fun earnCoins(source: String, amount: Int) {
        ensure()
        dao.updateCoins(amount.coerceAtLeast(0))
    }

    suspend fun spendCoins(reason: String, amount: Int) {
        ensure()
        dao.updateCoins(-amount.coerceAtLeast(0))
    }

    suspend fun get(): WalletEntity? = dao.get()

    companion object {
        fun from(context: Context): WalletRepository = WalletRepository(DatabaseProvider.get(context).walletDao())
    }
}

