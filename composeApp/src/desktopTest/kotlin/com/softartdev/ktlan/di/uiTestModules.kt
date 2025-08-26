package com.softartdev.ktlan.di

import com.softartdev.ktlan.domain.util.CoroutineDispatchers
import com.softartdev.ktlan.domain.util.CoroutineDispatchersImpl
import com.softartdev.ktlan.navigation.RouterImpl
import com.softartdev.ktlan.presentation.navigation.Router
import com.softartdev.ktlan.presentation.navigation.UiThreadRouter
import org.koin.core.module.Module
import org.koin.dsl.module

val uiTestModules: List<Module>
    get() = listOf(navigationTestModule, utilTestModule)

val navigationTestModule = module {
    single<Router> { UiThreadRouter(router = RouterImpl()) }
}

val utilTestModule = module {
    single<CoroutineDispatchers> { CoroutineDispatchersImpl() }
}
