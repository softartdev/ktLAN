package com.softartdev.ktlan.domain.usecase

import com.softartdev.ktlan.domain.model.HostModel
import com.softartdev.ktlan.domain.repo.ScanRepoStub
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScanUseCaseStub : ScanUseCase(ScanRepoStub()) {
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    override val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    var isScanRunning: Boolean = false
        private set

    override fun startScan(startIp: String, endIp: String, ports: List<Int>) {
        isScanRunning = true
        _scanState.value = ScanState.Loading
    }

    override fun cancelScan() {
        isScanRunning = false
        _scanState.value = ScanState.Idle
    }

    override fun resetScan() {
        isScanRunning = false
        _scanState.value = ScanState.Idle
    }

    fun completeScanWithSuccess(hosts: List<HostModel>) {
        isScanRunning = false
        _scanState.value = ScanState.Success(hosts)
    }

    fun completeScanWithError(message: String) {
        isScanRunning = false
        _scanState.value = ScanState.Error(message)
    }
}