package com.example.budgetin.util

import java.text.NumberFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Helper format mata uang & tanggal Indonesia. */
object Money {
    private val formatter: NumberFormat = NumberFormat.getNumberInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    fun format(amount: Double): String = "Rp ${formatter.format(amount)}"
}

object DateUtil {

    fun monthName(year: Int, month: Int): String {
        val c = Calendar.getInstance().apply { set(year, month, 1) }
        return c.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("id", "ID")).orEmpty()
            .replaceFirstChar { it.titlecase(Locale("id", "ID")) }
    }

    /** Label bulan penuh: "Agustus 2026". */
    fun monthLabel(calendar: Calendar): String =
        "${monthName(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH))} " +
            calendar.get(Calendar.YEAR)

    /** Label ringkas tanggal: "10 Agu 2026". */
    fun shortDate(timestamp: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = timestamp }
        val day = c.get(Calendar.DAY_OF_MONTH)
        val month = c.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale("id", "ID")).orEmpty()
        return "$day $month ${c.get(Calendar.YEAR)}"
    }

    /** Header grup di riwayat: "Senin, 10 Agustus 2026". */
    fun fullDate(timestamp: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = timestamp }
        val dayName = c.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale("id", "ID")).orEmpty()
        val month = c.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("id", "ID")).orEmpty()
        return "$dayName, ${c.get(Calendar.DAY_OF_MONTH)} $month ${c.get(Calendar.YEAR)}"
    }

    /** Awal bulan untuk [calendar] (hari 1, jam 00:00). */
    fun startOfMonth(calendar: Calendar): Calendar =
        (calendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

    /** Akhir bulan untuk [calendar] (menit terakhir sebelum bulan berikutnya). */
    fun endOfMonth(calendar: Calendar): Calendar {
        val next = (calendar.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return (next.clone() as Calendar).apply { add(Calendar.MILLISECOND, -1) }
    }

    /** Waktu tanggal murni (tidak peduli jam) dari epoch millis. */
    fun dateOnlyMillis(calendar: Calendar): Long =
        Calendar.getInstance().apply {
            timeInMillis = calendar.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    /** Konversi tanggal lokal ke UTC midnight (format yang dipakai DatePicker). */
    fun toUtcDatePickerMillis(localMillis: Long): Long {
        val local = Calendar.getInstance().apply { timeInMillis = localMillis }
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(Calendar.YEAR, local.get(Calendar.YEAR))
            set(Calendar.MONTH, local.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, local.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return utc.timeInMillis
    }

    /** Konversi UTC midnight kembali ke tanggal lokal (pertahankan jam saat ini). */
    fun fromUtcDatePickerMillis(utcMillis: Long): Long {
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
        val local = Calendar.getInstance().apply {
            set(Calendar.YEAR, utc.get(Calendar.YEAR))
            set(Calendar.MONTH, utc.get(Calendar.MONTH))
            set(Calendar.DAY_OF_MONTH, utc.get(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return local.timeInMillis
    }
}
