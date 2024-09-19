package com.mlucas.mushu.data.entities

enum class NotificationType(val title: String, val description: String) {
    ALARM("Alert", "This is an alarm notification."),
    NOTIFIER("Notifier", "This is a message notification.");

    fun getFullDescription(): String {
        return "$title: $description"
    }

    companion object {
        fun fromString(type: String?): NotificationType {
            return entries.find { it.title.equals(type, ignoreCase = true) } ?: NOTIFIER
        }
    }
}