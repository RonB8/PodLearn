package com.example.podlingo.ui.common

import java.text.SimpleDateFormat
import java.util.Locale

private val pubDateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)

fun formatPubDate(epochMs: Long): String = pubDateFormat.format(epochMs)

fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, secs) else "%d:%02d".format(minutes, secs)
}
