package com.softartdev.ktlan.presentation.navigation

class RouterStub : Router {
    var lastBottomRoute: Any? = null

    override fun setController(navController: Any) {}
    override fun setBottomNavController(navController: Any) {}
    override fun <T : Any> navigate(route: T) {}
    override fun <T : Any> navigateClearingBackStack(route: T) {}
    override fun <T : Any> popBackStack(route: T, inclusive: Boolean, saveState: Boolean): Boolean = false
    override fun popBackStack(): Boolean = false
    override fun <T : Any> bottomNavigate(route: T) {
        lastBottomRoute = route
    }
    override fun releaseController() {}
    override fun releaseBottomNavController() {}
}