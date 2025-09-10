package com.aidestinymaster.billing

import android.content.Context
import com.aidestinymaster.data.repository.PurchaseRepository

class Entitlement private constructor(private val repo: PurchaseRepository) {
    suspend fun hasVip(): Boolean {
        // 簡化：有任一有效訂閱 SKU 即視為 VIP
        val active = repo.getActive()
        return active.any { it.sku.startsWith("sub_vip_") && it.acknowledged }
    }

    suspend fun hasPro(kind: String): Boolean {
        val vip = hasVip()
        if (vip) return true
        val sku = when (kind.lowercase()) {
            "bazi" -> "iap_bazi_pro"
            "ziwei" -> "iap_ziwei_pro"
            "design" -> "iap_design_pro"
            "astro", "natal", "astrochart" -> "iap_astro_pro"
            else -> "iap_bazi_pro" // 預設映射
        }
        return repo.isEntitled(sku)
    }

    companion object {
        fun from(context: Context): Entitlement = Entitlement(PurchaseRepository.from(context))
    }
}

