package com.softartdev.ktlan.domain.usecase

import com.softartdev.ktlan.domain.repo.ScanRepo
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Use Case for managing network scanning operations.
 * Hosts long-running operations and manages scan state.
 * Follows Clean Architecture principles as an intermediate layer between ViewModel and Repository.
 */
open class ScanUseCase(
    private val scanRepo: ScanRepo
) {
    // Long-lived scope for scan operations that survive ViewModel recreation
    private val scanScope = CoroutineScope(SupervisorJob())
    
    // State management for scan operations
    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    open val scanState: StateFlow<ScanState> = _scanState.asStateFlow()
    
    // Current scan job to allow cancellation
    private var currentScanJob: Job? = null

    /**
     * Start a long-running scan operation that survives ViewModel recreation
     */
    open fun startScan(startIp: String, endIp: String, ports: List<Int>) {
        // Cancel any existing scan
        currentScanJob?.cancel()
        
        currentScanJob = scanScope.launch {
            try {
                _scanState.value = ScanState.Loading
                val hosts = scanRepo.scanRangeParallel(
                    coroutineContext = this.coroutineContext,
                    startIp = startIp,
                    endIp = endIp,
                    ports = ports
                )
                _scanState.value = ScanState.Success(hosts)
            } catch (error: Throwable) {
                Napier.e("Error during scan", error)
                _scanState.value = ScanState.Error(error.message ?: "Unknown error")
            }
        }
    }

    /**
     * Cancel the current scan operation
     */
    open fun cancelScan() {
        currentScanJob?.cancel()
        currentScanJob = null
        _scanState.value = ScanState.Idle
    }

    /**
     * Reset scan state
     */
    open fun resetScan() {
        cancelScan()
        _scanState.value = ScanState.Idle
    }
}
