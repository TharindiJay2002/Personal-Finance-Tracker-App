package com.example.trackmyfunds

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var transactionTypeToggle: MaterialButtonToggleGroup
    private lateinit var transactionDescription: TextInputEditText
    private lateinit var transactionCategory: MaterialAutoCompleteTextView
    private lateinit var transactionAmount: TextInputEditText
    private lateinit var addTransactionButton: MaterialButton
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var bottomNavigationView: BottomNavigationView

    private val incomeCategories = arrayOf("Salary", "Freelance", "Business", "Investments", "Gifts", "Rental Income", "Other")
    private val expenseCategories = arrayOf("Rent/Mortgage", "Utilities", "Groceries", "Transportation", "Insurance", "Loan Payments", "Other")

    companion object {
        const val CHANNEL_ID = "TrackMyFunds_channel"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        initializeViews()
        setupTransactionTypeToggle()
        setupCategoryDropdown()
        setupAddButton()
        setupBottomNavigation()
        createNotificationChannel()
    }

    private fun initializeViews() {
        transactionTypeToggle = findViewById(R.id.transactionTypeToggle)
        transactionDescription = findViewById(R.id.transactionDescription)
        transactionCategory = findViewById(R.id.transactionCategorySpinner)
        transactionAmount = findViewById(R.id.transactionAmount)
        addTransactionButton = findViewById(R.id.addTransactionButton)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        sharedPreferences = getSharedPreferences("TrackMyFundsPrefs", MODE_PRIVATE)
    }

    private fun setupTransactionTypeToggle() {
        // Set initial selection
        transactionTypeToggle.check(R.id.incomeButton)
        updateCategoryAdapter(true)

        transactionTypeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                updateCategoryAdapter(checkedId == R.id.incomeButton)
            }
        }
    }

    private fun setupCategoryDropdown() {
        // Set initial categories
        updateCategoryAdapter(true)

        transactionCategory.setOnItemClickListener { _, _, _, _ ->
            // Optional: Handle category selection
        }
    }

    private fun updateCategoryAdapter(isIncome: Boolean) {
        val categories = if (isIncome) incomeCategories else expenseCategories
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        transactionCategory.setAdapter(adapter)
        transactionCategory.setText(categories[0], false)
    }

    private fun setupAddButton() {
        addTransactionButton.setOnClickListener {
            val type = if (transactionTypeToggle.checkedButtonId == R.id.incomeButton) "Income" else "Expense"
            val description = transactionDescription.text.toString()
            val category = transactionCategory.text.toString()
            val amount = transactionAmount.text.toString().toFloatOrNull()

            if (validateInput(description, amount)) {
                val currentTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                addTransaction(type, description, category, amount!!, currentTime)
                createNotification(type, description, category, amount)
                showSuccessAndNavigate()
            }
        }
    }

    private fun validateInput(description: String, amount: Float?): Boolean {
        when {
            description.isEmpty() -> {
                transactionDescription.error = "Please enter a description"
                return false
            }
            amount == null || amount <= 0 -> {
                transactionAmount.error = "Please enter a valid amount"
                return false
            }
            else -> return true
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.nav_add_transaction -> true // Already on add transaction page
                R.id.nav_transactions -> {
                    startActivity(Intent(this, TransactionsActivity::class.java))
                    true
                }
                R.id.nav_notifications -> {
                    startActivity(Intent(this, NotificationsActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    true
                }
                else -> false
            }
        }
        bottomNavigationView.selectedItemId = R.id.nav_add_transaction
    }

    private fun addTransaction(type: String, description: String, category: String, amount: Float, timestamp: String) {
        val transactions = sharedPreferences.getStringSet("transactions", mutableSetOf())!!.toMutableSet()
        transactions.add("$type|$description|$timestamp|$amount|$category")
        val editor = sharedPreferences.edit()
        editor.putStringSet("transactions", transactions)

        val budget = sharedPreferences.getFloat("monthlyBudget", 0f)
        editor.putFloat("monthlyBudget", if (type == "Income") budget + amount else budget - amount)
        editor.apply()
    }

    private fun createNotification(type: String, description: String, category: String, amount: Float) {
        val notificationId = System.currentTimeMillis().toInt()
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(if (type == "Income") R.drawable.ic_income else R.drawable.ic_expense)
            .setContentTitle("New $type Added")
            .setContentText("$description - $category - Rs.${String.format("%.2f", amount)}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(notificationId, notification)

        // Save notification
        val notifications = sharedPreferences.getStringSet("notifications", mutableSetOf())!!.toMutableSet()
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        notifications.add("$type|$description|$timestamp|$amount")
        sharedPreferences.edit().putStringSet("notifications", notifications).apply()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "TrackMyFunds Channel"
            val descriptionText = "Channel for TrackMyFunds notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showSuccessAndNavigate() {
        Toast.makeText(this, "Transaction added successfully", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}