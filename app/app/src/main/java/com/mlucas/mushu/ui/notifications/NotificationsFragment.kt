package com.mlucas.mushu.ui.notifications

import android.os.Bundle
import android.text.format.DateFormat
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mlucas.mushu.R
import com.mlucas.mushu.data.database.NotificationDatabase
import com.mlucas.mushu.data.entities.NotificationEntity
import com.mlucas.mushu.data.entities.NotificationType
import com.mlucas.mushu.databinding.FragmentNotificationsBinding
import jp.wasabeef.recyclerview.animators.LandingAnimator
import java.util.Calendar

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private lateinit var viewModel: NotificationsViewModel
    private var previousNotifications: List<NotificationEntity> = emptyList()

    // This property is only valid between onCreateView and onDestroyView
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Create the view model
        val notificationDao = NotificationDatabase.getDatabase(requireContext()).notificationDao()
        viewModel = ViewModelProvider(this, NotificationsViewModelFactory(notificationDao))[NotificationsViewModel::class.java]

        // TODO: TEMPORARY REFRESH OF THE DB
        //viewModel.deleteAllNotifications()

        // Create notification adapter with an empty list
        val notificationsAdapter = NotificationsAdapter(ArrayList<NotificationEntity>())
        // Observe changes to the notification list and order the adapter to update
        viewModel.notifications.observe(viewLifecycleOwner, Observer { notifications ->

            notifications?.let {
                // Get newly inserted notifications
                val newNotifications = it.filterNot { notification ->
                    previousNotifications.contains(notification)
                }

                // Sort the new notifications by date in ascending order (newest first)
                val sortedNewNotifications = newNotifications.sortedBy { notification ->
                    notification.timestamp
                }

                sortedNewNotifications.forEach { notification ->
                    println(notification)
                    notificationsAdapter.addNotification(notification)
                }

                previousNotifications = it
            }

            // Remove notifications that pass the limit
            notificationsAdapter.removeExcessNotifications()
        })

        // Create the recycler view with the adapter above
        val recyclerView: RecyclerView = binding.recyclerViewNotifications
        recyclerView.layoutManager = LinearLayoutManager(this.context)
        recyclerView.itemAnimator = LandingAnimator()
        recyclerView.adapter = notificationsAdapter

        // Example usage:
        //viewModel.insert(NotificationEntity(title = "Title1", message = "Message body 1"))
        //viewModel.insert(NotificationEntity(title = "Title2", message = "Message body 2"))

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class NotificationsAdapter(private var notifications: MutableList<NotificationEntity>) :
        RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

        class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.notificationTitle)
            val message: TextView = view.findViewById(R.id.notificationMessage)
            val date: TextView = view.findViewById(R.id.notificationDate)
            val icon: ImageView = view.findViewById(R.id.notificationMessageImg)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_notification, parent, false)
            return NotificationViewHolder(view)
        }

        override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
            val notification = notifications[position]
            holder.title.text = notification.title
            holder.message.text = notification.message
            if (notification.type == NotificationType.ALARM) {
                holder.icon.setImageResource(android.R.drawable.ic_dialog_alert)
            }

            val calendar: Calendar = Calendar.getInstance()
            calendar.timeInMillis = notification.timestamp
            val date: String = DateFormat.format("dd-MM-yyyy H:mm:ss", calendar).toString()
            holder.date.text = date
        }

        override fun getItemCount(): Int {
            return notifications.size
        }

        fun addNotification(notification: NotificationEntity) {
            this.notifications.add(0, notification)
            notifyItemInserted(0)
        }

        fun removeExcessNotifications() {
            //TODO: Get this value from a setting
            val index = 4
            if (this.notifications.size > 5) {
                val previousSize = this.notifications.size
                this.notifications.subList(index + 1, this.notifications.size).clear()
                notifyItemRangeRemoved(5, previousSize - 5)
            }
        }
    }
}

