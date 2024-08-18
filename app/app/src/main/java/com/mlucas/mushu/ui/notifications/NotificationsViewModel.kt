package com.mlucas.mushu.ui.notifications

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.mlucas.mushu.data.database.NotificationDao
import com.mlucas.mushu.data.entities.NotificationEntity

class NotificationsViewModel(private val notificationDao: NotificationDao) : ViewModel() {

    private val _notifications = MutableLiveData<List<NotificationEntity>>()
    val notifications: LiveData<List<NotificationEntity>> get() = _notifications

    private val _text = MutableLiveData<String>().apply {
        value = "This is notifications Fragment"
    }
    val text: LiveData<String> = _text

    fun insert(notification: NotificationEntity) = viewModelScope.launch {
        notificationDao.insert(notification)
        notificationDao.removeExcessNotifications()
        getLastNotifications()
    }

    fun getLastNotifications() = viewModelScope.launch {
        _notifications.value = notificationDao.getLastNotifications()
    }

    fun deleteAllNotifications() = viewModelScope.launch {
        notificationDao.deleteAllNotifications()
    }
}