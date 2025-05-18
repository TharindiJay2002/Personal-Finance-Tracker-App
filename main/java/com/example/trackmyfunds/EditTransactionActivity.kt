package com.example.trackmyfunds

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class EditTransactionActivity : AppCompatActivity() {

    private lateinit var transactionTypeToggle: MaterialButtonToggleGroup
    private lateinit var transactionDescription: TextInputEditText
    private lateinit var transactionCategory: MaterialAutoCompleteTextView
    private lateinit var transactionAmount: TextInputEditText
    private lateinit var updateTransactionButton: MaterialButton
    private lateinit var deleteTransactionButton: MaterialButton
    private lateinit var backButton: ImageButton
    private lateinit var lastModifiedDate: TextView
    private lateinit var modifiedBy: TextView
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var originalTransaction: String

    private val incomeCategories = arrayOf("Salary", "Freelance", "Business", "Investments", "Gifts", "Rental Income", "Other")
    private val expenseCategories = arrayOf("Rent/Mortgage", "Utilities", "Groceries", "Transportation", "Insurance", "Loan Payments", "Other")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_transaction)

        initializeViews()
        loadTransaction()
        setupListeners()
    }

    private fun initializeViews() {
        transactionTypeToggle = findViewById(R.id.transactionTypeToggle)
        transactionDescription = findViewById(R.id.transactionDescription)
        transactionCategory = findViewById(R.id.transactionCategorySpinner)
        transactionAmount = findViewById(R.id.transactionAmount)
        updateTransactionButton = findViewById(R.id.updateTransactionButton)
        deleteTransactionButton = findViewById(R.id.deleteTransactionButton)
        backButton = findViewById(R.id.backButton)

        sharedPreferences = getSharedPreferences("TrackMyFundsPrefs", MODE_PRIVATE)
    }



    private fun loadTransaction() {
        originalTransaction = intent.getStringExtra("transaction") ?: return
        val data = originalTransaction.split("|")

        // Set transaction type
        if (data[0] == "Income") {
            transactionTypeToggle.check(R.id.incomeButton)
            updateCategoryAdapter(true)
        } else {
            transactionTypeToggle.check(R.id.expenseButton)
            updateCategoryAdapter(false)
        }

        // Set other fields
        transactionDescription.setText(data[1])
        transactionCategory.setText(data[2])
        transactionAmount.setText(String.format("%.2f", data[3].toFloat()))
    }

    private fun setupListeners() {
        transactionTypeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                updateCategoryAdapter(checkedId == R.id.incomeButton)
            }
        }

        updateTransactionButton.setOnClickListener {
            validateAndUpdateTransaction()
        }

        deleteTransactionButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }

        backButton.setOnClickListener {
            onBackPressed()
        }

        // Add TextWatcher for amount formatting
        transactionAmount.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s.toString().isNotEmpty() && !s.toString().startsWith("Rs.")) {
                    val parsed = s.toString().replace(Regex("[^\\d.]"), "")
                    try {
                        val formatted = String.format("%.2f", parsed.toFloat())
                        transactionAmount.removeTextChangedListener(this)
                        transactionAmount.setText(formatted)
                        transactionAmount.setSelection(formatted.length)
                        transactionAmount.addTextChangedListener(this)
                    } catch (e: Exception) {
                        // Invalid number format
                    }
                }
            }
        })
    }

    private fun updateCategoryAdapter(isIncome: Boolean) {
        val categories = if (isIncome) incomeCategories else expenseCategories
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        transactionCategory.setAdapter(adapter)
    }

    private fun validateAndUpdateTransaction() {
        val type = if (transactionTypeToggle.checkedButtonId == R.id.incomeButton) "Income" else "Expense"
        val description = transactionDescription.text.toString()
        val category = transactionCategory.text.toString()
        val amount = transactionAmount.text.toString().replace(Regex("[^\\d.]"), "").toFloatOrNull()

        when {
            description.isEmpty() -> {
                transactionDescription.error = "Please enter a description"
            }
            category.isEmpty() -> {
                transactionCategory.error = "Please select a category"
            }
            amount == null || amount <= 0 -> {
                transactionAmount.error = "Please enter a valid amount"
            }
            else -> {
                updateTransaction(type, description, category, amount)
                showSuccessAndNavigate()
            }
        }
    }

    private fun updateTransaction(type: String, description: String, category: String, amount: Float) {
        val transactions = sharedPreferences.getStringSet("transactions", mutableSetOf())!!.toMutableSet()
        transactions.remove(originalTransaction)

        val currentDateTime = "2025-04-04 19:28:48" // Using the provided date
        transactions.add("$type|$description|$currentDateTime|$amount|$category")

        val editor = sharedPreferences.edit()
        editor.putStringSet("transactions", transactions)

        // Update budget
        val originalData = originalTransaction.split("|")
        val originalAmount = originalData[3].toFloat()
        val budget = sharedPreferences.getFloat("monthlyBudget", 0f)

        val newBudget = when {
            originalData[0] == "Income" && type == "Income" -> budget - originalAmount + amount
            originalData[0] == "Income" && type == "Expense" -> budget - originalAmount - amount
            originalData[0] == "Expense" && type == "Income" -> budget + originalAmount + amount
            else -> budget + originalAmount - amount
        }

        editor.putFloat("monthlyBudget", newBudget)
        editor.apply()
    }

    private fun showDeleteConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Delete") { _, _ ->
                deleteTransaction()
                showSuccessAndNavigate("Transaction deleted successfully")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTransaction() {
        val transactions = sharedPreferences.getStringSet("transactions", mutableSetOf())!!.toMutableSet()
        transactions.remove(originalTransaction)

        val editor = sharedPreferences.edit()
        editor.putStringSet("transactions", transactions)

        // Update budget
        val originalData = originalTransaction.split("|")
        val originalAmount = originalData[3].toFloat()
        val budget = sharedPreferences.getFloat("monthlyBudget", 0f)

        val newBudget = if (originalData[0] == "Income") {
            budget - originalAmount
        } else {
            budget + originalAmount
        }

        editor.putFloat("monthlyBudget", newBudget)
        editor.apply()
    }

    private fun showSuccessAndNavigate(message: String = "Transaction updated successfully") {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}