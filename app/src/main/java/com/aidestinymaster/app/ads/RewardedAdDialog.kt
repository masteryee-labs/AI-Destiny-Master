package com.aidestinymaster.app.ads

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.rewarded.RewardItem
import com.aidestinymaster.data.repository.WalletRepository
import kotlinx.coroutines.launch
import com.aidestinymaster.app.util.AppLogger

@Composable
fun RewardedAdDialog(
    activity: ComponentActivity,
    visible: Boolean,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val status = remember { mutableStateOf("未載入") }
    val lastReward = remember { mutableStateOf("-") }
    val errorMsg = remember { mutableStateOf("") }

    LaunchedEffect(visible) {
        if (visible) {
            val (ok, reason) = RewardedAdsManager.canShow(activity)
            if (!ok) {
                status.value = reason ?: "冷卻中"
                errorMsg.value = reason ?: "冷卻中"
                return@LaunchedEffect
            }
            status.value = "載入中..."
            RewardedAdsManager.loadRewardedAd(
                context = activity,
                adUnitId = com.aidestinymaster.app.BuildConfig.ADMOB_REWARDED_ID,
                onLoaded = { ad ->
                    status.value = "已載入，可顯示"
                    AppLogger.log(activity, "Rewarded loaded")
                    // 立即顯示（簡化流程）
                    RewardedAdsManager.showRewardedAd(
                        activity = activity,
                        ad = ad,
                        onUserEarnedReward = { rewardItem: RewardItem ->
                            lastReward.value = "${rewardItem.type}+${rewardItem.amount}"
                            AppLogger.log(activity, "Rewarded earned: ${lastReward.value}")
                            RewardedAdsManager.recordShown(activity)
                            scope.launch {
                                WalletRepository.from(activity).earnCoins("ad", 10)
                            }
                            onDismiss()
                        },
                        onFailed = { err ->
                            status.value = "顯示失敗：$err"
                            errorMsg.value = err
                            AppLogger.log(activity, "Rewarded show failed: $err")
                        }
                    )
                },
                onFailed = { err ->
                    status.value = "載入失敗：$err"
                    errorMsg.value = err
                    AppLogger.log(activity, "Rewarded load failed: $err")
                }
            )
        }
    }

    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = { /* 顯示流程於載入完成後自動觸發，這裡僅作為確定與關閉 */ onDismiss() }) { Text("關閉") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("取消") } },
        title = { Text("看廣告＋10 幣") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("說明：觀看一支獎勵型廣告可獲得 10 幣，用於解鎖 AI 或深度內容。")
                Text("狀態：${status.value} / 上次回饋：${lastReward.value}")
                if (errorMsg.value.isNotEmpty()) Text("錯誤：${errorMsg.value}")
            }
        }
    )
}
