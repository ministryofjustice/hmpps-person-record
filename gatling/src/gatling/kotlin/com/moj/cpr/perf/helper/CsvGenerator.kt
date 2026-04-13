package com.moj.cpr.perf.helper

import java.io.FileWriter
import java.sql.DriverManager

object CsvGenerator {
    val queries = listOf(
        "SELECT prison_number from personrecordservice.person where source_system = 'NOMIS'",
        "SELECT crn from personrecordservice.person where source_system = 'DELIUS'",
        "SELECT defendant_id from personrecordservice.person where source_system = 'COMMON_PLATFORM'"
    )
    @JvmStatic
    fun main(args: Array<String>) {
        val (url, user, pass, outFile) = args
        DriverManager.getConnection(url, user, pass).use { conn ->
            val columns = queries.map { runQuery(conn, it) }
            val rowCount = columns.minOf { it.size }
            val rows = (0 until rowCount).map { i ->
                listOf(columns[0][i], columns[1][i], columns[2][i])
            }
            writeCsv(rows, outFile)
        }
        println("Done -> $outFile")
    }
    fun runQuery(conn: java.sql.Connection, sql: String): List<String> =
    conn.createStatement().use { st ->
        st.executeQuery(sql).use { rs ->
            val result = mutableListOf<String>()
            while (rs.next()) {
                result += rs.getString(1) ?: ""
            }
            result
        }
    }
    fun writeCsv(rows: List<List<String>>, file: String) {
        FileWriter(file).use { w ->
            w.append("prison_number,crn,defendant_id\n")
            rows.forEach { w.append(it.joinToString(",")).append("\n") }
        }
    }
}