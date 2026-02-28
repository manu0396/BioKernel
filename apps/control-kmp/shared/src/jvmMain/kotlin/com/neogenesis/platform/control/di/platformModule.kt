package com.neogenesis.platform.control.di

import com.neogenesis.platform.control.domain.usecase.TelemetryExcelExporter
import com.neogenesis.platform.control.infrastructure.excel.TelemetryExcelExporterImpl
import org.koin.core.module.Module
import org.koin.dsl.module

actual fun platformModule(): Module =
    module {
        single<TelemetryExcelExporter> { TelemetryExcelExporterImpl() }
    }
