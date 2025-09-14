package com.google.android.gms.ads.initialization

interface AdapterStatus {
    val initializationState: State
    val description: String

    enum class State { READY, NOT_READY }
}

class SimpleAdapterStatus(
    override val initializationState: AdapterStatus.State = AdapterStatus.State.NOT_READY,
    override val description: String = "stub"
) : AdapterStatus
