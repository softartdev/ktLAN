package com.softartdev.ktlan

import com.softartdev.ktlan.domain.repo.ScanRepo
import com.softartdev.ktlan.presentation.main.MainViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val sharedModules: List<Module>
    get() = dataModule + domainModule + presentationModule

val dataModule: Module = module {}

val domainModule: Module = module {
    factoryOf(::ScanRepo)
}

val presentationModule: Module = module {
    viewModelOf(::MainViewModel)
}