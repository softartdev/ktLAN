package com.softartdev.ktlan.presentation.scan

import com.softartdev.ktlan.domain.model.HostModel

sealed interface ScanResult {

    data object Loading : ScanResult

    data class Success(
        val startIp: String = "192.168.0.100",
        val endIp: String = "192.168.0.125",
        val ports: List<Int> = listOf(22, 80, 443, 8080, 9091),
        val hosts: List<HostModel> = emptyList(),
    ) : ScanResult

    data class Error(val message: String) : ScanResult
}

sealed interface ScanAction {
    data class UpdateStartIp(val startIp: String) : ScanAction
    data class UpdateEndIp(val endIp: String) : ScanAction
    data class UpdatePorts(val ports: String) : ScanAction
    data object LaunchScan : ScanAction
    data object ResetScan : ScanAction
    data object ClearError : ScanAction
}
