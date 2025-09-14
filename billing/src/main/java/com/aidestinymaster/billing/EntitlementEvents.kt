package com.aidestinymaster.billing

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object EntitlementEvents {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun emitChanged() {
        _events.tryEmit(Unit)
    }
}
