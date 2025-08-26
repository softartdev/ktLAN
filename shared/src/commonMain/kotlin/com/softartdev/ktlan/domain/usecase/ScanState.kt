package com.softartdev.ktlan.domain.usecase

import com.softartdev.ktlan.domain.model.HostModel

sealed interface ScanState {
    data object Idle : ScanState
    data object Loading : ScanState
    data class Success(val hosts: List<HostModel>) : ScanState
    data class Error(val message: String) : ScanState
}
