package com.softartdev.ktlan.presentation.networks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.softartdev.ktlan.domain.repo.NetworksRepo
import com.softartdev.ktlan.domain.util.CoroutineDispatchers
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** ViewModel for listing network interfaces. */
class NetworksViewModel(
    private val repo: NetworksRepo,
    private val dispatchers: CoroutineDispatchers
) : ViewModel() {

    private val state = MutableStateFlow(NetworksResult())
    val stateFlow: StateFlow<NetworksResult> = state
    private var launched = false

    fun launch() {
        if (launched) return
        launched = true
        onAction(NetworksAction.Refresh)
    }

    fun onAction(action: NetworksAction) {
        when (action) {
            is NetworksAction.Refresh -> viewModelScope.launch {
                state.update { it.copy(loading = true, error = null) }
                runCatching { repo.listInterfaces() }
                    .onSuccess { list ->
                        val filtered = if (state.value.showAll) list else list.filter { it.isUp && !it.isLoopback }
                        state.update { it.copy(loading = false, interfaces = filtered) }
                    }
                    .onFailure { e ->
                        Napier.e("Failed to list interfaces", e)
                        state.update { it.copy(loading = false, error = e.message) }
                    }
            }
            is NetworksAction.ToggleShowAll -> {
                state.update { it.copy(showAll = action.show) }
                onAction(NetworksAction.Refresh)
            }
            is NetworksAction.Copy -> state.update { it.copy(selectedIp = action.address) }
            is NetworksAction.UseAsBindHost -> state.update { it.copy(selectedIp = action.address) }
        }
    }

    suspend fun guessLocalIp(): String? = repo.guessLocalIPv4()
}
