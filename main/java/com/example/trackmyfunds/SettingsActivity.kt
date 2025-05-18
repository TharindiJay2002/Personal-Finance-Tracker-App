package com.example.trackmyfunds

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class SettingsActivity : AppCompatActivity() {

    private lateinit var usernameText: TextView
    private lateinit var emailText: TextView
    private lateinit var resetPasswordButton: Button
    private lateinit var resetDataButton: Button
    private lateinit var logoutButton: Button
    private lateinit var exportDataButton: Button
    private lateinit var importDataButton: Button
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        usernameText = findViewById(R.id.usernameText)
        emailText = findViewById(R.id.emailText)
        resetPasswordButton = findViewById(R.id.resetPasswordButton)
        resetDataButton = findViewById(R.id.resetDataButton)
        logoutButton = findViewById(R.id.logoutButton)
        exportDataButton = findViewById(R.id.exportDataButton)
        importDataButton = findViewById(R.id.importDataButton)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        sharedPreferences = getSharedPreferences("TrackMyFundsPrefs", MODE_PRIVATE)

        displayUserInfo()

        resetPasswordButton.setOnClickListener {
            showResetPasswordDialog()
        }

        resetDataButton.setOnClickListener {
            resetAppData()
        }

        logoutButton.setOnClickListener {
            logout()
        }

        exportDataButton.setOnClickListener {
            exportData()
        }

        importDataButton.setOnClickListener {
            importData()
        }

        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
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
                R.id.nav_notifications -> {
                    startActivity(Intent(this, NotificationsActivity::class.java))
                    true
                }
                R.id.nav_settings -> {
                    // Already on settings page, do nothing
                    true
                }
                else -> false
            }
        }

        // Set settings as selected item
        bottomNavigationView.selectedItemId = R.id.nav_settings
    }

    private fun displayUserInfo() {
        val username = sharedPreferences.getString("username", "N/A")
        val email = sharedPreferences.getString("email", "N/A")
        usernameText.text = "Username: $username"
        emailText.text = "Email: $email"
    }

    private fun showResetPasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_reset_password, null)
        val currentPasswordInput = dialogView.findViewById<EditText>(R.id.currentPasswordInput)
        val newPasswordInput = dialogView.findViewById<EditText>(R.id.newPasswordInput)

        AlertDialog.Builder(this)
            .setTitle("Reset Password")
            .setView(dialogView)
            .setPositiveButton("Reset") { _, _ ->
                val currentPassword = currentPasswordInput.text.toString()
                val newPassword = newPasswordInput.text.toString()
                resetPassword(currentPassword, newPassword)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetPassword(currentPassword: String, newPassword: String) {
        val storedPassword = sharedPreferences.getString("password", "")
        if (storedPassword == currentPassword) {
            val editor = sharedPreferences.edit()
            editor.putString("password", newPassword)
            editor.apply()
            Toast.makeText(this, "Password reset successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Current password is incorrect", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetAppData() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
        Toast.makeText(this, "App data reset successfully", Toast.LENGTH_SHORT).show()
    }

    private fun logout() {
        // Clear login state and necessary data
        sharedPreferences.edit().apply {
            putBoolean("isLoggedIn", false)
            remove("lastLoginDate")
            // Don't remove registration data like username, email, password
            apply()
        }

        // Create intent with flags to clear activity stack
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        finish()
    }

    private fun exportData() {
        val transactions = sharedPreferences.getStringSet("transactions", mutableSetOf())!!.joinToString("\n")
        val fileName = "transactions_backup.txt"
        val file = File(filesDir, fileName)
        try {
            val fos = FileOutputStream(file)
            fos.write(transactions.toByteArray())
            fos.close()
            Toast.makeText(this, "Data exported to $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to export data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importData() {
        val fileName = "transactions_backup.txt"
        val file = File(filesDir, fileName)
        if (file.exists()) {
            try {
                val fis = FileInputStream(file)
                val transactions = fis.bufferedReader().use { it.readText() }.split("\n").toMutableSet()
                fis.close()
                val editor = sharedPreferences.edit()
                editor.putStringSet("transactions", transactions)
                editor.apply()
                Toast.makeText(this, "Data imported successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Failed to import data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "No backup file found", Toast.LENGTH_SHORT).show()
        }
    }
}