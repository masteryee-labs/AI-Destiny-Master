package com.aidestinymaster.app.report

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.aidestinymaster.data.db.ReportEntity
import com.aidestinymaster.data.repository.ReportRepository
import com.aidestinymaster.sync.ReportSyncBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class ReportViewModel(app: Application) : AndroidViewModel(app) {
    private val repo by lazy { ReportRepository.from(app) }
    private val bridge by lazy { ReportSyncBridge(app) }

    private val _lastId = MutableStateFlow<String?>(null)
    val lastId = _lastId.asStateFlow()

    private val _current = MutableStateFlow<ReportEntity?>(null)
    val current = _current.asStateFlow()

    suspend fun create(type: String, content: String): String = withContext(Dispatchers.IO) {
        val id = repo.createFromAi(type, chartId = "demoChart", content = content)
        _lastId.value = id
        _current.value = repo.getOnce(id)
        id
    }

    suspend fun push() = withContext(Dispatchers.IO) {
        _lastId.value?.let { bridge.push(it) }
    }

    suspend fun pull() = withContext(Dispatchers.IO) {
        _lastId.value?.let {
            bridge.pull(it)
            _current.value = repo.getOnce(it)
        }
    }
}
