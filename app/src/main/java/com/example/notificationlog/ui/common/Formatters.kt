package com.example.notificationlog.ui.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

object Formatters {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val timeOnly = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)
    private val monthDayTime = DateTimeFormatter.ofPattern("M월 d일 a h:mm", Locale.KOREAN)
    private val fullDateTime = DateTimeFormatter.ofPattern("yyyy년 M월 d일 a h:mm", Locale.KOREAN)
    private val detailDateTime = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E) a h:mm:ss", Locale.KOREAN)
    private val shortDateFormat = DateTimeFormatter.ofPattern("yy.MM.dd", Locale.KOREAN)

    /** 목록용: 오늘이면 시간만, 올해면 월/일, 그 외에는 연도까지 */
    fun listTime(millis: Long): String {
        val dateTime = Instant.ofEpochMilli(millis).atZone(zone)
        val today = LocalDate.now(zone)
        return when {
            dateTime.toLocalDate() == today -> timeOnly.format(dateTime)
            dateTime.year == today.year -> monthDayTime.format(dateTime)
            else -> fullDateTime.format(dateTime)
        }
    }

    fun detailTime(millis: Long): String = detailDateTime.format(Instant.ofEpochMilli(millis).atZone(zone))

    fun shortDate(millis: Long): String = shortDateFormat.format(Instant.ofEpochMilli(millis).atZone(zone))

    /** 로컬 날짜의 시작 시각(ms) */
    fun startOfDay(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    /** DatePicker 가 반환하는 UTC 자정 값을 로컬 날짜로 변환 */
    fun utcMillisToLocalDate(utcMillis: Long): LocalDate =
        Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()

    fun localDateToUtcMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun today(): LocalDate = LocalDate.now(zone)
}

/** 검색어/필터 키워드를 대소문자 구분 없이 강조 표시한다. */
fun highlight(text: String, terms: List<String>, highlightColor: Color): AnnotatedString =
    buildAnnotatedString {
        append(text)
        if (text.isEmpty()) return@buildAnnotatedString
        val style = SpanStyle(background = highlightColor, fontWeight = FontWeight.Bold)
        terms.filter { it.isNotBlank() }.forEach { term ->
            var index = text.indexOf(term, startIndex = 0, ignoreCase = true)
            while (index >= 0) {
                addStyle(style, index, index + term.length)
                index = text.indexOf(term, startIndex = index + term.length, ignoreCase = true)
            }
        }
    }
