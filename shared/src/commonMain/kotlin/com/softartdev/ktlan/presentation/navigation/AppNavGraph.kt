package com.softartdev.ktlan.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface AppNavGraph {

    sealed interface BottomTab : AppNavGraph {

        @Serializable
        data object Scan : BottomTab

        @Serializable
        data object Connect : BottomTab

        @Serializable
        data object Settings : BottomTab
    }

    @Serializable
    data object MainBottomNav : AppNavGraph

    @Serializable
    data class QrDialog(val text: String) : AppNavGraph

    @Serializable
    data object ThemeDialog : AppNavGraph

    @Serializable
    data class ErrorDialog(val message: String?) : AppNavGraph
}
