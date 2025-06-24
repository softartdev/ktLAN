package com.softartdev.ktlan.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softartdev.ktlan.domain.model.HostModel
import com.softartdev.ktlan.domain.repo.ScanRepo
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val scanRepo: ScanRepo) : ViewModel() {

    private val mutableStateFlow: MutableStateFlow<MainScanResult> = MutableStateFlow(
        value = MainScanResult.Success()
    )
    val stateFlow: StateFlow<MainScanResult> = mutableStateFlow

    private val currentSuccessStateOrDefault: MainScanResult.Success
        get() = mutableStateFlow.value as? MainScanResult.Success ?: MainScanResult.Success()

    fun onAction(action: MainScanAction) = when (action) {
        is MainScanAction.UpdateStartIp -> updateStartIp(action.startIp)
        is MainScanAction.UpdateEndIp -> updateEndIp(action.endIp)
        is MainScanAction.UpdatePorts -> updatePorts(action.ports)
        is MainScanAction.LaunchScan -> launchScan()
        is MainScanAction.ResetScan -> resetScan()
        is MainScanAction.ClearError -> clearError()
    }

    private fun launchScan() = viewModelScope.launch {
        val scan: MainScanResult.Success = currentSuccessStateOrDefault
        mutableStateFlow.value = MainScanResult.Loading
        try {
            val hostModels: List<HostModel> = scanRepo.scanRangeParallel(
                coroutineContext = Dispatchers.Default,
                startIp = scan.startIp,
                endIp = scan.endIp,
                ports = scan.ports
            )
            mutableStateFlow.value = scan.copy(hosts = hostModels)
        } catch (error: Throwable) {
            Napier.e("error during scan", error)
            mutableStateFlow.value = MainScanResult.Error(error.message ?: "Unknown error")
        }
    }

    private fun updateStartIp(string: String) {
        mutableStateFlow.value = currentSuccessStateOrDefault.copy(startIp = string)
        Napier.d("Start IP updated to: $string")
    }

    private fun updateEndIp(string: String) {
        mutableStateFlow.value = currentSuccessStateOrDefault.copy(endIp = string)
        Napier.d("End IP updated to: $string")
    }

    private fun updatePorts(string: String) {
        val ports: List<Int> = string.split(",")
            .map<String, String>(String::trim)
            .mapNotNull(String::toIntOrNull)
        mutableStateFlow.value = currentSuccessStateOrDefault.copy(ports = ports)
        Napier.d("Ports updated to: $ports")
    }

    private fun resetScan() {
        mutableStateFlow.value = MainScanResult.Success()
        Napier.d("Scan reset to default values")
        clearError()
    }

    private fun clearError() {
        if (mutableStateFlow.value is MainScanResult.Error) {
            mutableStateFlow.value = currentSuccessStateOrDefault
            Napier.d("Error cleared")
        } else {
            Napier.w("No error to clear")
        }
    }
}