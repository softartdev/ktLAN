package com.softartdev.ktlan.di

import com.softartdev.ktlan.domain.util.CoroutineDispatchers
import com.softartdev.ktlan.domain.util.CoroutineDispatchersImpl
import com.softartdev.ktlan.navigation.RouterImpl
import com.softartdev.ktlan.presentation.navigation.Router
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val uiModules: List<Module>
    get() = navigationModule + utilModule

val navigationModule = module {
    singleOf<Router>(::RouterImpl)
}

val utilModule = module {
    singleOf<CoroutineDispatchers>(::CoroutineDispatchersImpl)
}