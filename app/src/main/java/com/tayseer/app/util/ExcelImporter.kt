package com.tayseer.app.util

import android.content.Context
import android.net.Uri
import com.tayseer.app.data.Student
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.BufferedReader
import java.io.InputStreamReader

object ExcelImporter {

    fun parseFile(context: Context, uri: Uri, defaultGrade: String, defaultSection: String): List<Student> {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri) ?: ""
        val fileName = uri.lastPathSegment?.lowercase() ?: ""

        return if (fileName.endsWith(".csv") || mimeType.contains("csv") || mimeType.contains("text")) {
            parseCsv(context, uri, defaultGrade, defaultSection)
        } else {
            parseExcel(context, uri, defaultGrade, defaultSection)
        }
    }

    private fun parseCsv(context: Context, uri: Uri, defaultGrade: String, defaultSection: String): List<Student> {
        val students = mutableListOf<Student>()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                var line = reader.readLine()
                var isHeader = true
                while (line != null) {
                    val tokens = line.split(",").map { it.trim().replace("\"", "") }
                    if (tokens.isNotEmpty() && tokens[0].isNotBlank()) {
                        if (isHeader && (tokens[0].contains("اسم") || tokens[0].contains("Name", ignoreCase = true))) {
                            isHeader = false
                            line = reader.readLine()
                            continue
                        }
                        isHeader = false
                        val name = tokens.getOrNull(0) ?: ""
                        val grade = tokens.getOrNull(1)?.ifBlank { defaultGrade } ?: defaultGrade
                        val section = tokens.getOrNull(2)?.ifBlank { defaultSection } ?: defaultSection
                        val phone = tokens.getOrNull(3) ?: ""

                        if (name.isNotBlank()) {
                            students.add(Student(name = name, grade = grade, section = section, parentPhone = phone))
                        }
                    }
                    line = reader.readLine()
                }
            }
        }
        return students
    }

    private fun parseExcel(context: Context, uri: Uri, defaultGrade: String, defaultSection: String): List<Student> {
        val students = mutableListOf<Student>()
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)
            val rowIterator = sheet.iterator()

            var isHeader = true
            while (rowIterator.hasNext()) {
                val row = rowIterator.next()
                val firstCell = row.getCell(0)?.toString()?.trim() ?: ""

                if (firstCell.isNotBlank()) {
                    if (isHeader && (firstCell.contains("اسم") || firstCell.contains("Name", ignoreCase = true))) {
                        isHeader = false
                        continue
                    }
                    isHeader = false
                    val name = firstCell
                    val grade = row.getCell(1)?.toString()?.trim()?.ifBlank { defaultGrade } ?: defaultGrade
                    val section = row.getCell(2)?.toString()?.trim()?.ifBlank { defaultSection } ?: defaultSection
                    val phone = row.getCell(3)?.toString()?.trim() ?: ""

                    if (name.isNotBlank()) {
                        students.add(Student(name = name, grade = grade, section = section, parentPhone = phone))
                    }
                }
            }
            workbook.close()
        }
        return students
    }
}
