package com.softartdev.ktlan.di

import com.softartdev.ktlan.domain.repo.ConnectRepo
import com.softartdev.ktlan.domain.repo.ScanRepo
import com.softartdev.ktlan.presentation.connect.ConnectViewModel
import com.softartdev.ktlan.presentation.scan.ScanViewModel
import com.softartdev.ktlan.presentation.settings.SettingsViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val sharedModules: List<Module>
    get() = dataModule + domainModule + presentationModule

expect val dataModule: Module

val domainModule: Module = module {
    factoryOf(::ScanRepo)
    singleOf(::ConnectRepo)
}

val presentationModule: Module = module {
    viewModelOf(::ScanViewModel)
    viewModelOf(::ConnectViewModel)
    viewModelOf(::SettingsViewModel)
}