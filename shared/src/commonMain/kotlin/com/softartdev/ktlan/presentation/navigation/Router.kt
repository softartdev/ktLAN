package com.softartdev.ktlan.presentation.navigation

interface Router {
    fun setController(navController: Any)
    fun setBottomNavController(navController: Any)

    fun <T : Any> navigate(route: T)
    fun <T : Any> navigateClearingBackStack(route: T)
    fun <T : Any> popBackStack(route: T, inclusive: Boolean, saveState: Boolean): Boolean
    fun popBackStack(): Boolean

    fun <T : Any> bottomNavigate(route: T)

    fun releaseController()
    fun releaseBottomNavController()
}
