package com.softartdev.ktlan.presentation.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softartdev.ktlan.domain.usecase.ScanState
import com.softartdev.ktlan.domain.usecase.ScanUseCase
import com.softartdev.ktlan.presentation.navigation.AppNavGraph
import com.softartdev.ktlan.presentation.navigation.Router
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ScanViewModel(
    private val scanUseCase: ScanUseCase,
    private val router: Router,
    private val navParameters: AppNavGraph.BottomTab.Scan
) : ViewModel() {
    
    // Local UI state (input fields)
    private val _uiState = MutableStateFlow(
        ScanResult.Success()
    )
    
    // Combined state from use case and local UI state
    private val _combinedState = MutableStateFlow<ScanResult>(ScanResult.Success())
    val stateFlow: StateFlow<ScanResult> = _combinedState

    private val currentSuccessStateOrDefault: ScanResult.Success
        get() = _uiState.value

    private var launched = false

    init {
        // Combine use case state with local UI state
        combine(
            scanUseCase.scanState,
            _uiState
        ) { useCaseState, uiState ->
            when (useCaseState) {
                is ScanState.Idle -> uiState
                is ScanState.Loading -> ScanResult.Loading
                is ScanState.Success -> uiState.copy(hosts = useCaseState.hosts)
                is ScanState.Error -> ScanResult.Error(useCaseState.message)
            }
        }.onEach { combinedState ->
            _combinedState.value = combinedState
        }.launchIn(viewModelScope)
    }

    fun launch() {
        if (launched) return
        navParameters.startIp?.let(::updateStartIp)
        navParameters.endIp?.let(::updateEndIp)
        launched = true
    }

    fun onAction(action: ScanAction) = when (action) {
        is ScanAction.UpdateStartIp -> updateStartIp(action.startIp)
        is ScanAction.UpdateEndIp -> updateEndIp(action.endIp)
        is ScanAction.UpdatePorts -> updatePorts(action.ports)
        is ScanAction.LaunchScan -> launchScan()
        is ScanAction.ResetScan -> resetScan()
        is ScanAction.ClearError -> clearError()
        is ScanAction.UseAsRemoteHost -> useAsRemoteHost(action.address)
    }

    private fun launchScan() {
        val scan: ScanResult.Success = currentSuccessStateOrDefault
        scanUseCase.startScan(
            startIp = scan.startIp,
            endIp = scan.endIp,
            ports = scan.ports
        )
    }

    private fun updateStartIp(string: String) {
        _uiState.value = currentSuccessStateOrDefault.copy(startIp = string)
        Napier.d("Start IP updated to: $string")
    }

    private fun updateEndIp(string: String) {
        _uiState.value = currentSuccessStateOrDefault.copy(endIp = string)
        Napier.d("End IP updated to: $string")
    }

    private fun updatePorts(string: String) {
        val ports: List<Int> = string.split(",")
            .map(String::trim)
            .mapNotNull(String::toIntOrNull)
        _uiState.value = currentSuccessStateOrDefault.copy(ports = ports)
        Napier.d("Ports updated to: $ports")
    }

    private fun resetScan() {
        scanUseCase.resetScan()
        _uiState.value = ScanResult.Success()
        Napier.d("Scan reset to default values")
        clearError()
    }

    private fun clearError() {
        if (_combinedState.value is ScanResult.Error) {
            scanUseCase.resetScan()
        } else {
            Napier.w("No error to clear")
        }
    }

    private fun useAsRemoteHost(address: String) {
        router.bottomNavigate(AppNavGraph.BottomTab.Socket(remoteHost = address))
        Napier.d("Navigating to Socket with remote host: $address")
    }
}