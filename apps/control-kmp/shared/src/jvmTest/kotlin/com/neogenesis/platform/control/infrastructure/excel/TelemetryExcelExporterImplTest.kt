package com.neogenesis.platform.control.infrastructure.excel

import com.neogenesis.platform.shared.telemetry.*
import kotlinx.datetime.Instant
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TelemetryExcelExporterImplTest {
    private lateinit var exporter: TelemetryExcelExporterImpl

    @Before
    fun setup() {
        exporter = TelemetryExcelExporterImpl()
    }

    @Test
    fun `toXlsxBytes generates valid Excel with correct headers and data`() {
        val frames =
            listOf(
                TelemetryFrame(
                    timestamp = Instant.parse("2024-01-01T10:00:00Z"),
                    pressure = PressureReading(100.0),
                    displacement = NozzleDisplacement(10.0),
                    flowRate = FlowRate(50.0),
                    temperature = Temperature(25.0),
                    viscosity = ViscosityEstimation(1.0),
                    pid = PIDState(0.5, 0.2, 0.1),
                    mpc = MPCPrediction(10, 120.0),
                ),
                TelemetryFrame(
                    timestamp = Instant.parse("2024-01-01T10:00:01Z"),
                    pressure = PressureReading(101.0),
                    displacement = NozzleDisplacement(10.1),
                    flowRate = FlowRate(51.0),
                    temperature = Temperature(25.1),
                    viscosity = ViscosityEstimation(1.1),
                    pid = PIDState(0.51, 0.21, 0.11),
                    mpc = MPCPrediction(10, 121.0),
                ),
            )
        val runId = "test_run_123"
        val createdAtIso = "2024-01-01T10:00:05Z"

        val bytes = exporter.toXlsxBytes(frames, runId, createdAtIso)

        assertNotNull(bytes)
        assertTrue(bytes.isNotEmpty())

        val workbook = XSSFWorkbook(bytes.inputStream())
        val sheet = workbook.getSheet("telemetry")
        assertNotNull(sheet)

        // Verify header row
        val headerRow = sheet.getRow(0)
        assertNotNull(headerRow)
        assertEquals("timestamp", headerRow.getCell(0).stringCellValue)
        assertEquals("pressure_kpa", headerRow.getCell(1).stringCellValue)
        assertEquals("displacement_mm", headerRow.getCell(2).stringCellValue)
        assertEquals("flow_ul_s", headerRow.getCell(3).stringCellValue)
        assertEquals("temperature_c", headerRow.getCell(4).stringCellValue)
        assertEquals("viscosity", headerRow.getCell(5).stringCellValue)
        assertEquals("pid_p", headerRow.getCell(6).stringCellValue)
        assertEquals("pid_i", headerRow.getCell(7).stringCellValue)
        assertEquals("pid_d", headerRow.getCell(8).stringCellValue)
        assertEquals("mpc_horizon", headerRow.getCell(9).stringCellValue)
        assertEquals("mpc_target_kpa", headerRow.getCell(10).stringCellValue)

        // Verify data rows
        val row1 = sheet.getRow(1)
        assertNotNull(row1)
        assertEquals("2024-01-01T10:00:00Z", row1.getCell(0).stringCellValue)
        assertEquals(100.0, row1.getCell(1).numericCellValue, 0.001)
        assertEquals(10.0, row1.getCell(2).numericCellValue, 0.001)
        assertEquals(50.0, row1.getCell(3).numericCellValue, 0.001)
        assertEquals(25.0, row1.getCell(4).numericCellValue, 0.001)
        assertEquals(1.0, row1.getCell(5).numericCellValue, 0.001)
        assertEquals(0.5, row1.getCell(6).numericCellValue, 0.001)
        assertEquals(0.2, row1.getCell(7).numericCellValue, 0.001)
        assertEquals(0.1, row1.getCell(8).numericCellValue, 0.001)
        assertEquals(10.0, row1.getCell(9).numericCellValue, 0.001)
        assertEquals(120.0, row1.getCell(10).numericCellValue, 0.001)

        val row2 = sheet.getRow(2)
        assertNotNull(row2)
        assertEquals("2024-01-01T10:00:01Z", row2.getCell(0).stringCellValue)
        assertEquals(101.0, row2.getCell(1).numericCellValue, 0.001)
        assertEquals(10.1, row2.getCell(2).numericCellValue, 0.001)
        assertEquals(51.0, row2.getCell(3).numericCellValue, 0.001)
        assertEquals(25.1, row2.getCell(4).numericCellValue, 0.001)
        assertEquals(1.1, row2.getCell(5).numericCellValue, 0.001)
        assertEquals(0.51, row2.getCell(6).numericCellValue, 0.001)
        assertEquals(0.21, row2.getCell(7).numericCellValue, 0.001)
        assertEquals(0.11, row2.getCell(8).numericCellValue, 0.001)
        assertEquals(10.0, row2.getCell(9).numericCellValue, 0.001) // MPC Horizon is Int, no .0 here
        assertEquals(121.0, row2.getCell(10).numericCellValue, 0.001)

        workbook.close()
    }

    @Test
    fun `toXlsxBytes handles empty frames list`() {
        val frames = emptyList<TelemetryFrame>()
        val runId = "empty_run"
        val createdAtIso = "2024-01-01T11:00:00Z"

        val bytes = exporter.toXlsxBytes(frames, runId, createdAtIso)

        assertNotNull(bytes)
        assertTrue(bytes.isNotEmpty()) // Workbook will still have a sheet and potentially headers

        val workbook = XSSFWorkbook(bytes.inputStream())
        val sheet = workbook.getSheet("telemetry")
        assertNotNull(sheet)
        assertEquals(1, sheet.physicalNumberOfRows) // Only header row

        workbook.close()
    }
}
