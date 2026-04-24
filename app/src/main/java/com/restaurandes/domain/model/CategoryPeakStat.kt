package com.restaurandes.domain.model

data class CategoryPeakStat(
    val category: String,
    val timeSlot: String,
    val dayOfWeek: String,
    val count: Long
)

fun getCurrentDayOfWeek(): String {
    return when (java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)) {
        java.util.Calendar.MONDAY -> "monday"
        java.util.Calendar.TUESDAY -> "tuesday"
        java.util.Calendar.WEDNESDAY -> "wednesday"
        java.util.Calendar.THURSDAY -> "thursday"
        java.util.Calendar.FRIDAY -> "friday"
        java.util.Calendar.SATURDAY -> "saturday"
        java.util.Calendar.SUNDAY -> "sunday"
        else -> "monday"
    }
}
