package com.neurogenesis.data.di

import com.neurogenesis.data.repository.LoginRepositoryImpl
import com.neurogenesis.data.repository.MockRetinaRepository
import com.neurogenesis.data.repository.RetinaRepositoryImpl
import com.neurogenesis.domain.repository.LoginRepository
import com.neurogenesis.domain.repository.RetinaRepository
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val dataModule = module {
    singleOf(::MockRetinaRepository) { bind<RetinaRepository>() }
    singleOf(::LoginRepositoryImpl) { bind<LoginRepository>() }
    singleOf(::RetinaRepositoryImpl) { bind<RetinaRepository>() }
}