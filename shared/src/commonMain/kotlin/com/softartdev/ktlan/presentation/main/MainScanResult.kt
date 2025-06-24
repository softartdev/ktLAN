package com.softartdev.ktlan.presentation.main

import com.softartdev.ktlan.domain.model.HostModel

sealed interface MainScanResult {

    data object Loading : MainScanResult

    data class Success(
        val startIp: String = "192.168.0.100",
        val endIp: String = "192.168.0.125",
        val ports: List<Int> = listOf(22, 80, 443, 8080),
        val hosts: List<HostModel> = emptyList(),
    ) : MainScanResult

    data class Error(val message: String) : MainScanResult
}

sealed interface MainScanAction {
    data class UpdateStartIp(val startIp: String) : MainScanAction
    data class UpdateEndIp(val endIp: String) : MainScanAction
    data class UpdatePorts(val ports: String) : MainScanAction
    data object LaunchScan : MainScanAction
    data object ResetScan : MainScanAction
    data object ClearError : MainScanAction
}
