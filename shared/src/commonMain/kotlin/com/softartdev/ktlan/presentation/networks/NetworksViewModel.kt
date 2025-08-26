package com.softartdev.ktlan.presentation.networks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softartdev.ktlan.domain.model.NetworkInterfaceInfo
import com.softartdev.ktlan.domain.repo.NetworksRepo
import com.softartdev.ktlan.presentation.navigation.AppNavGraph
import com.softartdev.ktlan.presentation.navigation.Router
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NetworksViewModel(
    private val repo: NetworksRepo,
    private val router: Router
) : ViewModel() {
    private val state = MutableStateFlow(NetworksResult())
    val stateFlow: StateFlow<NetworksResult> = state
    private var launched = false

    fun launch() {
        if (launched) return
        launched = true
        onAction(NetworksAction.Refresh)
        guessLocalIp()
    }

    fun onAction(action: NetworksAction) {
        when (action) {
            is NetworksAction.Refresh -> viewModelScope.launch {
                state.update { it.copy(loading = true, error = null) }
                runCatching { repo.listInterfaces() }
                    .onSuccess { list: List<NetworkInterfaceInfo> ->
                        state.update { it.copy(loading = false, interfaces = list) }
                    }
                    .onFailure { e: Throwable ->
                        Napier.e("Failed to list interfaces", e)
                        state.update { it.copy(loading = false, error = e.message) }
                    }
            }
            is NetworksAction.UseAsBindHost -> {
                router.bottomNavigate(AppNavGraph.BottomTab.Socket(bindHost = action.address))
            }
            is NetworksAction.Scan -> {
                val (startIP: String, endIP: String) = convertToNetworkRange(action.address)
                router.bottomNavigate(AppNavGraph.BottomTab.Scan(startIp = startIP, endIp = endIP))
            }
        }
    }

    fun guessLocalIp() = viewModelScope.launch {
        val ip: String? = repo.guessLocalIPv4()
        state.update { it.copy(yourIp = ip) }
    }

    /**
     * Converts an IP address to a network range.
     * Example: 192.168.1.10 -> (192.168.1.0, 192.168.1.255)
     */
    private fun convertToNetworkRange(ip: String): Pair<String, String> {
        val parts: List<String> = ip.split('.')
        if (parts.size != 4) {
            Napier.e("Invalid IP address format: $ip")
            return "0.0.0.0" to "255.255.255.255"
        }
        val networkPart: String = parts.take(3).joinToString(".")
        return "$networkPart.0" to "$networkPart.255"
    }
}
