package com.mlucas.mushu.ui.notifications

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mlucas.mushu.R
import com.mlucas.mushu.data.database.NotificationDatabase
import com.mlucas.mushu.data.entities.NotificationEntity
import com.mlucas.mushu.databinding.FragmentNotificationsBinding

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private lateinit var viewModel: NotificationsViewModel

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

        val textView: TextView = binding.textNotifications
        viewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }

        // TODO: TEMPORARY REFRESH OF THE DB
        viewModel.deleteAllNotifications()

        // Create notification adapter with an empty list
        val notificationsAdapter = NotificationsAdapter(emptyList())
        // Observe changes to the notification list and order the adapter to update
        viewModel.notifications.observe(viewLifecycleOwner, Observer { notifications ->
            notifications?.let {
                // Print the notifications
                it.forEach { notification ->
                    println(notification)
                }
            }
            notificationsAdapter.updateNotifications(notifications)
        })

        // Create the recycler view with the adapter above
        val recyclerView: RecyclerView = binding.recyclerViewNotifications
        recyclerView.layoutManager = LinearLayoutManager(this.context)
        recyclerView.setHasFixedSize(true)
        recyclerView.itemAnimator = DefaultItemAnimator()
        recyclerView.adapter = notificationsAdapter

        // Example usage:
        viewModel.insert(NotificationEntity(title = "Title1", body = "Message body 1"))
        viewModel.insert(NotificationEntity(title = "Title2", body = "Message body 2"))

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    class NotificationsAdapter(private var notifications: List<NotificationEntity>) :
        RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

        class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.findViewById(R.id.notificationTitle)
            val message: TextView = view.findViewById(R.id.notificationContent)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_notification, parent, false)
            return NotificationViewHolder(view)
        }

        override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
            val notification = notifications[position]
            holder.title.text = notification.title
            holder.message.text = notification.body
        }

        override fun getItemCount(): Int {
            return notifications.size
        }

        fun updateNotifications(newData: List<NotificationEntity>) {
            this.notifications = newData
            notifyDataSetChanged()
        }
    }
}

