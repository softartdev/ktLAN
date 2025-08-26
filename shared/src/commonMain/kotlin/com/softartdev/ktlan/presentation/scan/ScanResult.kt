package com.softartdev.ktlan.presentation.scan

import com.softartdev.ktlan.domain.model.HostModel

sealed interface ScanResult {

    data object Loading : ScanResult

    data class Success(
        val startIp: String = "192.168.0.100",
        val endIp: String = "192.168.0.125",
        val ports: List<Int> = listOf(22, 80, 443, 8080, 9091),
        val hosts: List<HostModel> = emptyList(),
    ) : ScanResult {
        companion object {
            val previewHosts: List<HostModel>
                get() = listOf(
                    HostModel(ip = "192.168.0.1", openPorts = listOf(22, 80, 443)),
                    HostModel(ip = "192.168.0.2", openPorts = listOf(21, 25, 8080)),
                    HostModel(ip = "192.168.0.3", openPorts = listOf(53, 3306, 6379))
                )
        }
    }

    data class Error(val message: String) : ScanResult
}

sealed interface ScanAction {
    data class UpdateStartIp(val startIp: String) : ScanAction
    data class UpdateEndIp(val endIp: String) : ScanAction
    data class UpdatePorts(val ports: String) : ScanAction
    data object LaunchScan : ScanAction
    data object ResetScan : ScanAction
    data object ClearError : ScanAction
    data class UseAsRemoteHost(val address: String) : ScanAction
}
