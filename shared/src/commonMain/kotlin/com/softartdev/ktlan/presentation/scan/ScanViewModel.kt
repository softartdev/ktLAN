package com.softartdev.ktlan.presentation.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softartdev.ktlan.domain.model.HostModel
import com.softartdev.ktlan.domain.repo.ScanRepo
import com.softartdev.ktlan.presentation.navigation.Router
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ScanViewModel(
    private val scanRepo: ScanRepo,
    private val router: Router
) : ViewModel() {

    private val mutableStateFlow: MutableStateFlow<ScanResult> = MutableStateFlow(
        value = ScanResult.Success()
    )
    val stateFlow: StateFlow<ScanResult> = mutableStateFlow

    private val currentSuccessStateOrDefault: ScanResult.Success
        get() = mutableStateFlow.value as? ScanResult.Success ?: ScanResult.Success()

    fun onAction(action: ScanAction) = when (action) {
        is ScanAction.UpdateStartIp -> updateStartIp(action.startIp)
        is ScanAction.UpdateEndIp -> updateEndIp(action.endIp)
        is ScanAction.UpdatePorts -> updatePorts(action.ports)
        is ScanAction.LaunchScan -> launchScan()
        is ScanAction.ResetScan -> resetScan()
        is ScanAction.ClearError -> clearError()
    }

    private fun launchScan() = viewModelScope.launch {
        val scan: ScanResult.Success = currentSuccessStateOrDefault
        mutableStateFlow.value = ScanResult.Loading
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
            mutableStateFlow.value = ScanResult.Error(error.message ?: "Unknown error")
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
        mutableStateFlow.value = ScanResult.Success()
        Napier.d("Scan reset to default values")
        clearError()
    }

    private fun clearError() {
        if (mutableStateFlow.value is ScanResult.Error) {
            mutableStateFlow.value = currentSuccessStateOrDefault
            Napier.d("Error cleared")
        } else {
            Napier.w("No error to clear")
        }
    }
}