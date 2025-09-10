package com.aidestinymaster.data.repository

import android.content.Context
import com.aidestinymaster.data.db.DatabaseProvider
import com.aidestinymaster.data.db.PurchaseDao
import com.aidestinymaster.data.db.PurchaseEntity

class PurchaseRepository private constructor(private val dao: PurchaseDao) {
    suspend fun syncFromBilling(purchases: List<PurchaseEntity>) {
        purchases.forEach { dao.upsert(it) }
    }

    suspend fun isEntitled(sku: String): Boolean = dao.getBySku(sku)?.let { it.state == 1 && it.acknowledged } == true

    suspend fun getActive(): List<PurchaseEntity> = dao.getActive()

    companion object {
        fun from(context: Context): PurchaseRepository = PurchaseRepository(DatabaseProvider.get(context).purchaseDao())
    }
}

