package com.example.notificationlog.export

import java.io.BufferedWriter
import java.io.Closeable
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * 의존성 없는 스트리밍 XLSX(Office Open XML) 작성기.
 *
 * Apache POI 는 Android 에서 StAX/AWT 의존성, 메서드 수, APK 크기 문제가 있어
 * XLSX 의 최소 구성(ZIP + XML 6개)을 직접 작성한다. 행을 하나씩 ZIP 스트림에 바로 쓰므로
 * 수십만 건을 내보내도 메모리 사용량이 일정하다.
 *
 * - 문자열은 inlineStr 로 기록 (공유 문자열 테이블 불필요)
 * - 첫 행은 굵은 헤더 + 틀 고정, 지정한 열은 줄바꿈 표시
 */
class XlsxWriter(
    output: OutputStream,
    private val sheetName: String,
    private val columnWidths: List<Int>,
    private val wrapColumns: Set<Int> = emptySet(),
) : Closeable {

    private val zip = ZipOutputStream(output)
    private val writer = BufferedWriter(OutputStreamWriter(zip, Charsets.UTF_8))
    private var rowNumber = 0
    private var closed = false

    init {
        writeEntry("[Content_Types].xml", CONTENT_TYPES)
        writeEntry("_rels/.rels", ROOT_RELS)
        writeEntry("xl/workbook.xml", workbookXml())
        writeEntry("xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
        writeEntry("xl/styles.xml", STYLES)
        beginSheet()
    }

    fun writeHeader(cells: List<String>) = writeRow(cells, header = true)

    fun writeRow(cells: List<String>) = writeRow(cells, header = false)

    private fun writeRow(cells: List<String>, header: Boolean) {
        rowNumber++
        writer.write("<row r=\"$rowNumber\">")
        cells.forEachIndexed { index, value ->
            val style = when {
                header -> STYLE_HEADER
                index in wrapColumns -> STYLE_WRAP
                else -> STYLE_DEFAULT
            }
            writer.write("<c r=\"${columnName(index)}$rowNumber\" t=\"inlineStr\"")
            if (style != STYLE_DEFAULT) writer.write(" s=\"$style\"")
            writer.write("><is><t xml:space=\"preserve\">")
            writer.write(escapeXml(value.take(MAX_CELL_LENGTH)))
            writer.write("</t></is></c>")
        }
        writer.write("</row>")
    }

    override fun close() {
        if (closed) return
        closed = true
        writer.write("</sheetData></worksheet>")
        writer.flush()
        zip.closeEntry()
        zip.finish()
        writer.close()
    }

    private fun beginSheet() {
        zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
        writer.write(XML_HEADER)
        writer.write("<worksheet xmlns=\"$NS_MAIN\" xmlns:r=\"$NS_REL\">")
        writer.write(
            "<sheetViews><sheetView workbookViewId=\"0\">" +
                "<pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/>" +
                "</sheetView></sheetViews>"
        )
        writer.write("<sheetFormatPr defaultRowHeight=\"15\"/>")
        if (columnWidths.isNotEmpty()) {
            writer.write("<cols>")
            columnWidths.forEachIndexed { i, width ->
                writer.write("<col min=\"${i + 1}\" max=\"${i + 1}\" width=\"$width\" customWidth=\"1\"/>")
            }
            writer.write("</cols>")
        }
        writer.write("<sheetData>")
    }

    private fun writeEntry(name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        writer.write(content)
        writer.flush()
        zip.closeEntry()
    }

    private fun workbookXml() = XML_HEADER +
        "<workbook xmlns=\"$NS_MAIN\" xmlns:r=\"$NS_REL\"><sheets>" +
        "<sheet name=\"${escapeXml(sheetName.take(31))}\" sheetId=\"1\" r:id=\"rId1\"/>" +
        "</sheets></workbook>"

    companion object {
        /** 엑셀 셀 최대 글자 수 */
        private const val MAX_CELL_LENGTH = 32_767

        private const val STYLE_DEFAULT = 0
        private const val STYLE_HEADER = 1
        private const val STYLE_WRAP = 2

        private const val XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
        private const val NS_MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
        private const val NS_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"

        private const val CONTENT_TYPES = XML_HEADER +
            "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
            "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
            "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
            "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
            "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
            "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>" +
            "</Types>"

        private const val ROOT_RELS = XML_HEADER +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
            "</Relationships>"

        private const val WORKBOOK_RELS = XML_HEADER +
            "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
            "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
            "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>" +
            "</Relationships>"

        // 0: 기본, 1: 헤더(굵게+배경), 2: 줄바꿈+위쪽 정렬
        private const val STYLES = XML_HEADER +
            "<styleSheet xmlns=\"$NS_MAIN\">" +
            "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
            "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>" +
            "<fills count=\"3\"><fill><patternFill patternType=\"none\"/></fill>" +
            "<fill><patternFill patternType=\"gray125\"/></fill>" +
            "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFE8EAF6\"/><bgColor indexed=\"64\"/></patternFill></fill></fills>" +
            "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>" +
            "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
            "<cellXfs count=\"3\">" +
            "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
            "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"/>" +
            "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyAlignment=\"1\">" +
            "<alignment wrapText=\"1\" vertical=\"top\"/></xf>" +
            "</cellXfs>" +
            "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>" +
            "</styleSheet>"

        /** 0 → A, 25 → Z, 26 → AA ... */
        fun columnName(index: Int): String {
            var n = index + 1
            val sb = StringBuilder()
            while (n > 0) {
                val rem = (n - 1) % 26
                sb.append('A' + rem)
                n = (n - 1) / 26
            }
            return sb.reverse().toString()
        }

        /**
         * XML 특수문자 이스케이프 + XML 1.0 에서 허용되지 않는 제어문자/깨진 서로게이트 제거.
         * (알림 본문에는 이모지나 제어문자가 섞여 있을 수 있다.)
         */
        fun escapeXml(value: String): String {
            val sb = StringBuilder(value.length + 16)
            var i = 0
            while (i < value.length) {
                val ch = value[i]
                when {
                    ch == '&' -> sb.append("&amp;")
                    ch == '<' -> sb.append("&lt;")
                    ch == '>' -> sb.append("&gt;")
                    ch == '"' -> sb.append("&quot;")
                    ch == '\t' || ch == '\n' || ch == '\r' -> sb.append(ch)
                    ch < ' ' || ch == '￾' || ch == '￿' -> Unit
                    Character.isHighSurrogate(ch) -> {
                        if (i + 1 < value.length && Character.isLowSurrogate(value[i + 1])) {
                            sb.append(ch).append(value[i + 1])
                            i++
                        }
                    }
                    Character.isLowSurrogate(ch) -> Unit
                    else -> sb.append(ch)
                }
                i++
            }
            return sb.toString()
        }
    }
}

/** 알림 기록 XLSX 의 컬럼 정의와 파일명 규칙 */
object XlsxExporter {
    const val MIME_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    const val SHEET_NAME = "알림 기록"

    val COLUMNS = listOf("수신 시각", "앱 이름", "패키지명", "제목", "내용", "카테고리", "즐겨찾기")
    val COLUMN_WIDTHS = listOf(20, 16, 28, 30, 60, 22, 10)
    /** 내용(4) 열은 줄바꿈 표시 */
    val WRAP_COLUMNS = setOf(4)

    private val fileNameFormat = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")

    /** 예: notification_log_20261027_0744.xlsx */
    fun defaultFileName(now: LocalDateTime = LocalDateTime.now()): String =
        "notification_log_${fileNameFormat.format(now)}.xlsx"

    fun newWriter(output: OutputStream): XlsxWriter =
        XlsxWriter(output, SHEET_NAME, COLUMN_WIDTHS, WRAP_COLUMNS).also { it.writeHeader(COLUMNS) }
}
