package com.utfpr.safeclimb.ui.item

data class HistoryItem(
    val month: String,
    val day: Int,
    val year: Int,
    val hour: String,
    val periodOfDay: String,
    val windSpeed: String,
    val humidity: String,
    val temperature: String,
    val precipitation: String,
    val soil: String
)
