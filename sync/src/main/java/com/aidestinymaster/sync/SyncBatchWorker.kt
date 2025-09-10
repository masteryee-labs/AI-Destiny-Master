package com.aidestinymaster.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aidestinymaster.data.db.DatabaseProvider

class SyncBatchWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val context = applicationContext
        // 未登入或未啟用同步則直接成功（下次再跑）
        if (!GoogleAuthManager.isSignedIn(context)) return Result.success()
        val user = com.aidestinymaster.data.repository.UserRepository.from(context).get()
        if (user?.syncEnabled != true) return Result.success()

        val db = DatabaseProvider.get(context)
        val reportBridge = ReportSyncBridge(context)
        val chartBridge = ChartSyncBridge(context)
        val walletBridge = WalletSyncBridge(context)
        val userBridge = UserSyncBridge(context)

        try {
            // Reports
            db.reportDao().listIds().forEach { id ->
                reportBridge.push(id)
                reportBridge.pull(id)
            }
            // Charts
            db.chartDao().listIds().forEach { id ->
                chartBridge.push(id)
                chartBridge.pull(id)
            }
            // Wallet & User
            walletBridge.push(); walletBridge.pull()
            userBridge.push(); userBridge.pull()
        } catch (e: Exception) {
            return Result.retry()
        }
        return Result.success()
    }
}

