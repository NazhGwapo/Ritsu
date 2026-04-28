package com.example.ritsu.ui.utils

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1[Calendar.YEAR] == cal2[Calendar.YEAR] &&
                cal1[Calendar.DAY_OF_YEAR] == cal2[Calendar.DAY_OF_YEAR]
    }

    fun getDateOptions(selectedRange: String): List<String> {
        val list = mutableListOf<String>()
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        when (selectedRange) {
            "Day" -> {
                for (i in 0..30) {
                    val d = Calendar.getInstance()
                    d.add(Calendar.DAY_OF_YEAR, -i)
                    list.add(when (i) { 0 -> "Today"; 1 -> "Yesterday"; else -> sdf.format(d.time) })
                }
            }
            "Week" -> {
                for (i in 0..12) {
                    val d = Calendar.getInstance()
                    d[Calendar.DAY_OF_WEEK] = Calendar.MONDAY
                    d.add(Calendar.WEEK_OF_YEAR, -i)
                    val end = d.clone() as Calendar
                    end.add(Calendar.DAY_OF_YEAR, 6)
                    list.add(if (i == 0) "This Week" else "${sdf.format(d.time)} - ${sdf.format(end.time)}")
                }
            }
            "Month" -> {
                val monthSdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                for (i in 0..12) {
                    val d = Calendar.getInstance()
                    d.add(Calendar.MONTH, -i)
                    list.add(monthSdf.format(d.time))
                }
            }
            "Year" -> {
                val year = Calendar.getInstance()[Calendar.YEAR]
                for (i in 0..5) list.add((year - i).toString())
            }
            "All Time" -> list.add("Entire History")
            "Custom" -> list.add("Select Custom Range...")
        }
        return list
    }

    fun filterByDate(
        timestamp: Long,
        range: String,
        option: String,
        customStart: Long,
        customEnd: Long
    ): Boolean {
        val playDate = Date(timestamp)
        val playCalendar = Calendar.getInstance().apply { time = playDate }
        val sdfDay = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val sdfMonth = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        return when (range) {
            "Day" -> {
                when (option) {
                    "Today" -> isSameDay(playCalendar, Calendar.getInstance())
                    "Yesterday" -> isSameDay(playCalendar, Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) })
                    else -> try { sdfDay.format(playDate) == option } catch(e: Exception) { false }
                }
            }
            "Week" -> {
                val weekRange = option.split(" - ")
                if (weekRange.size == 2) {
                    try {
                        val start = sdfDay.parse(weekRange[0]); val end = sdfDay.parse(weekRange[1])
                        if (start != null && end != null) playDate.after(start) && playDate.before(Date(end.time + 86400000)) else false
                    } catch (e: Exception) { false }
                } else if (option == "This Week") {
                    val startOfWeek = Calendar.getInstance().apply { set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
                    playDate.after(startOfWeek.time)
                } else false
            }
            "Month" -> sdfMonth.format(playDate) == option
            "Year" -> playCalendar[Calendar.YEAR].toString() == option
            "All Time" -> true
            "Custom" -> timestamp >= customStart && timestamp <= customEnd
            else -> false
        }
    }
}
