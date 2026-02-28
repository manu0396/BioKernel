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
        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet("telemetry")

            val headers = listOf(
                "timestamp",
                "pressure_kpa",
                "displacement_mm",
                "flow_ul_s",
                "temperature_c",
                "viscosity_pa_s",
                "pid_p",
                "pid_i",
                "pid_d",
                "mpc_horizon_ms",
                "mpc_predicted_pressure_kpa",
                "run_id",
                "created_at_iso",
            )

            val headerRow = sheet.createRow(0)
            headers.forEachIndexed { index, header ->
                headerRow.createCell(index).setCellValue(header)
            }

            frames.forEachIndexed { index, frame ->
                val row = sheet.createRow(index + 1)

                // NOTE: keep types robust; POI cells want Double for numerics.
                row.createCell(0).setCellValue(frame.timestamp.toString())
                row.createCell(1).setCellValue(frame.pressure.kpa.toDouble())

                // header says mm; your model is micrometers -> convert µm to mm
                val displacementMm = frame.displacement.micrometers.toDouble() / 1000.0
                row.createCell(2).setCellValue(displacementMm)

                row.createCell(3).setCellValue(frame.flowRate.microlitersPerSecond.toDouble())
                row.createCell(4).setCellValue(frame.temperature.celsius.toDouble())
                row.createCell(5).setCellValue(frame.viscosity.pascalSecond.toDouble())

                row.createCell(6).setCellValue(frame.pid.proportional.toDouble())
                row.createCell(7).setCellValue(frame.pid.integral.toDouble())
                row.createCell(8).setCellValue(frame.pid.derivative.toDouble())

                row.createCell(9).setCellValue(frame.mpc.horizonMs.toDouble())
                row.createCell(10).setCellValue(frame.mpc.predictedPressureKpa.toDouble())

                row.createCell(11).setCellValue(runId)
                row.createCell(12).setCellValue(createdAtIso)
            }

            // Optional: autosize a few columns (avoid huge cost on very large datasets)
            val autosizeUpTo = minOf(headers.size, 8)
            for (i in 0 until autosizeUpTo) sheet.autoSizeColumn(i)

            return ByteArrayOutputStream().use { out ->
                workbook.write(out)
                out.toByteArray()
            }
        }
    }
}