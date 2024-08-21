package com.mlucas.mushu.ui.notifications


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mlucas.mushu.data.database.NotificationDao

class NotificationsViewModelFactory(
    private val notificationDao: NotificationDao
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationsViewModel(notificationDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
