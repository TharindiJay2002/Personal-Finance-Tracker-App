package com.example.trackmyfunds

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class AddBudgetActivity : AppCompatActivity() {

    private lateinit var budgetInput: TextInputEditText
    private lateinit var saveBudgetButton: MaterialButton
    private lateinit var backButton: ImageButton
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_budget)

        initializeViews()
        setupClickListeners()
        loadCurrentBudget()
    }

    private fun initializeViews() {
        budgetInput = findViewById(R.id.budgetInput)
        saveBudgetButton = findViewById(R.id.saveBudgetButton)
        backButton = findViewById(R.id.backButton)
        sharedPreferences = getSharedPreferences("TrackMyFundsPrefs", MODE_PRIVATE)
    }



    private fun setupClickListeners() {
        saveBudgetButton.setOnClickListener {
            validateAndSaveBudget()
        }

        backButton.setOnClickListener {
            onBackPressed()
        }

        // Add TextWatcher to format input as currency
        budgetInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (s.toString().isNotEmpty() && !s.toString().startsWith("Rs.")) {
                    val parsed = s.toString().replace(Regex("[^\\d.]"), "")
                    try {
                        val formatted = String.format("%.2f", parsed.toFloat())
                        budgetInput.removeTextChangedListener(this)
                        budgetInput.setText(formatted)
                        budgetInput.setSelection(formatted.length)
                        budgetInput.addTextChangedListener(this)
                    } catch (e: Exception) {
                        // Invalid number format
                    }
                }
            }
        })
    }

    private fun loadCurrentBudget() {
        val currentBudget = sharedPreferences.getFloat("monthlyBudget", 0f)
        if (currentBudget > 0) {
            budgetInput.setText(String.format("%.2f", currentBudget))
        }
    }

    private fun validateAndSaveBudget() {
        val budgetStr = budgetInput.text.toString().replace(Regex("[^\\d.]"), "")
        val budget = budgetStr.toFloatOrNull()

        when {
            budgetStr.isEmpty() -> {
                budgetInput.error = "Please enter a budget amount"
            }
            budget == null || budget <= 0 -> {
                budgetInput.error = "Please enter a valid amount"
            }
            else -> {
                saveBudget(budget)
                showSuccessAndNavigate()
            }
        }
    }

    private fun saveBudget(budget: Float) {
        val editor = sharedPreferences.edit()
        editor.putFloat("monthlyBudget", budget)

        // Save the timestamp of when the budget was set
        editor.putString("budgetSetDate", "2025-04-04 19:24:05") // Using the provided date

        editor.apply()
    }

    private fun showSuccessAndNavigate() {
        Toast.makeText(this, "Budget saved successfully", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}