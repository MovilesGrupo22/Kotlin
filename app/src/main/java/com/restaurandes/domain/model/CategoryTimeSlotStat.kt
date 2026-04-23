package com.restaurandes.domain.model

data class CategoryTimeSlotStat(
    val category: String,
    val timeSlot: String,
    val count: Long
)

fun getCurrentTimeSlot(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 6..10 -> "breakfast"
        in 11..14 -> "lunch"
        in 15..17 -> "snack"
        in 18..22 -> "dinner"
        else -> "night"
    }
}
