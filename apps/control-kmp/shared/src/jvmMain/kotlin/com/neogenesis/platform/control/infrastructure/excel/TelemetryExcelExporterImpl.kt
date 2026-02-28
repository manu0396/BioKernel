package com.neogenesis.platform.control.infrastructure.excel

import com.neogenesis.platform.control.domain.usecase.TelemetryExcelExporter
import com.neogenesis.platform.shared.telemetry.TelemetryFrame
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.ByteArrayOutputStream

class TelemetryExcelExporterImpl : TelemetryExcelExporter {
    override fun toXlsxBytes(
        frames: List<TelemetryFrame>,
        runId: String,
        createdAtIso: String,
    ): ByteArray {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("telemetry")

        val headerRow = sheet.createRow(0)
        val headers =
            listOf(
                "timestamp", "pressure_kpa", "displacement_mm", "flow_ul_s", "temperature_c",
                "viscosity", "pid_p", "pid_i", "pid_d", "mpc_horizon", "mpc_target_kpa",
            )
        headers.forEachIndexed { index, header ->
            headerRow.createCell(index).setCellValue(header)
        }

        frames.forEachIndexed { index, frame ->
            val row = sheet.createRow(index + 1)
            row.createCell(0).setCellValue(frame.timestamp.toString())
            row.createCell(1).setCellValue(frame.pressure.kpa)
            row.createCell(2).setCellValue(frame.displacement.micrometers)
            row.createCell(3).setCellValue(frame.flowRate.microlitersPerSecond)
            row.createCell(4).setCellValue(frame.temperature.celsius)
            row.createCell(5).setCellValue(frame.viscosity.pascalSecond)
            row.createCell(6).setCellValue(frame.pid.proportional)
            row.createCell(7).setCellValue(frame.pid.integral)
            row.createCell(8).setCellValue(frame.pid.derivative)
            row.createCell(9).setCellValue(frame.mpc.horizonMs.toDouble())
            row.createCell(10).setCellValue(frame.mpc.predictedPressureKpa)
        }

        val outputStream = ByteArrayOutputStream()
        workbook.write(outputStream)
        workbook.close()
        return outputStream.toByteArray()
    }
}
