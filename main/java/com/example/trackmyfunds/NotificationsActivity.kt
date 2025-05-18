package com.example.trackmyfunds

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.*

class NotificationsActivity : AppCompatActivity() {

    private lateinit var notificationsRecyclerView: RecyclerView
    private lateinit var clearNotificationsButton: MaterialButton
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var emptyStateLayout: View
    private lateinit var swipeRefreshLayout: androidx.swiperefreshlayout.widget.SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        initializeViews()
        setupBottomNavigation()
        setupSwipeRefresh()
        displayNotifications()
        setupClearButton()
    }

    private fun initializeViews() {
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView)
        clearNotificationsButton = findViewById(R.id.clearNotificationsButton)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        sharedPreferences = getSharedPreferences("TrackMyFundsPrefs", MODE_PRIVATE)

        notificationsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setColorSchemeResources(
            R.color.accent_primary,
            R.color.accent_secondary
        )
        swipeRefreshLayout.setOnRefreshListener {
            displayNotifications()
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_add_transaction -> {
                    startActivity(Intent(this, AddTransactionActivity::class.java))
                    true
                }
                R.id.nav_transactions -> {
                    startActivity(Intent(this, TransactionsActivity::class.java))
                    true
                }
                R.id.nav_notifications -> true // Already on notifications page
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
        bottomNavigationView.selectedItemId = R.id.nav_notifications
    }

    private fun setupClearButton() {
        clearNotificationsButton.setOnClickListener {
            clearNotifications()
        }
    }

    private fun displayNotifications() {
        val notifications = sharedPreferences.getStringSet("notifications", mutableSetOf())!!.toList()
        val notificationItems = notifications.map {
            val data = it.split("|")
            NotificationItem(
                title = data[0],
                message = "${data[1]} - Rs.${data[3]}",
                timestamp = data[2]
            )
        }.sortedByDescending { it.timestamp }

        updateEmptyState(notificationItems.isEmpty())
        notificationsRecyclerView.adapter = NotificationsAdapter(notificationItems)
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        notificationsRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
        clearNotificationsButton.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun clearNotifications() {
        sharedPreferences.edit().remove("notifications").apply()
        Toast.makeText(this, "Notifications cleared", Toast.LENGTH_SHORT).show()
        displayNotifications()
    }
}

data class NotificationItem(
    val title: String,
    val message: String,
    val timestamp: String
)

class NotificationsAdapter(private val notifications: List<NotificationItem>) :
    RecyclerView.Adapter<NotificationsAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.notificationTitle)
        val messageText: TextView = view.findViewById(R.id.notificationMessage)
        val timeText: TextView = view.findViewById(R.id.notificationTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notificaion, parent, false)
        return NotificationViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val notification = notifications[position]
        holder.titleText.text = notification.title
        holder.messageText.text = notification.message

        // Format the timestamp
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            val date = inputFormat.parse(notification.timestamp)
            holder.timeText.text = date?.let { outputFormat.format(it) } ?: notification.timestamp
        } catch (e: Exception) {
            holder.timeText.text = notification.timestamp
        }

        // Add ripple effect
        holder.itemView.setOnClickListener {
            // Handle notification click if needed
        }
    }

    override fun getItemCount() = notifications.size
}