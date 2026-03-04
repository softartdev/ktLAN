package com.softartdev.ktlan.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.softartdev.ktlan.presentation.navigation.AppNavGraph
import com.softartdev.ktlan.presentation.navigation.Router

class RouterImpl : Router {
    private var navController: NavHostController? = null
    private var bottomNavController: NavHostController? = null

    override fun setController(navController: Any) {
        this.navController = navController as NavHostController
    }

    override fun setBottomNavController(navController: Any) {
        this.bottomNavController = navController as NavHostController
    }

    override fun <T : Any> navigate(route: T) = navController!!.navigate(route)

    override fun <T : Any> navigateClearingBackStack(route: T) {
        var popped = true
        while (popped) {
            popped = navController!!.popBackStack()
        }
        navController!!.navigate(route)
    }

    override fun <T : Any> popBackStack(route: T, inclusive: Boolean, saveState: Boolean): Boolean =
        navController!!.popBackStack(route, inclusive, saveState)

    override fun popBackStack() = navController!!.popBackStack()

    override fun <T : Any> bottomNavigate(route: T) = bottomNavController!!.navigate(route) {
        // Pop up to the start destination of the graph to avoid building up a large stack of destinations on the back stack as users select items
        popUpTo(bottomNavController!!.graph.findStartDestination().id) {
            saveState = true
        }
        
        // Check if the route has parameters that need fresh ViewModel instances
        val hasParameters = when (route) {
            is AppNavGraph.BottomTab.Scan -> 
                route.startIp != null || route.endIp != null
            is AppNavGraph.BottomTab.Socket -> 
                route.remoteHost != null || route.bindHost != null
            else -> false
        }
        
        if (hasParameters) {
            // For routes with parameters, create fresh ViewModel
            launchSingleTop = false
            popUpTo(bottomNavController!!.graph.findStartDestination().id) {
                inclusive = true
                saveState = false
            }
        } else {
            // For routes without parameters, reuse existing instance
            launchSingleTop = true
            restoreState = true
        }
    }

    override fun releaseController() {
        navController = null
    }

    override fun releaseBottomNavController() {
        bottomNavController = null
    }
}