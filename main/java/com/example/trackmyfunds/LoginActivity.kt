package com.example.trackmyfunds

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.util.regex.Pattern

class LoginActivity : AppCompatActivity() {

    // Layouts for showing validation errors
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    // EditTexts for user input
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var loginButton: MaterialButton
    private lateinit var signupLink: TextView
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        // Minimum length requirement for password
        private const val MIN_PASSWORD_LENGTH = 8
        // Use Android's built-in email pattern matcher
        private val EMAIL_PATTERN: Pattern = Patterns.EMAIL_ADDRESS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initializeViews()
        setupTextWatchers()
        setupClickListeners()
        checkPreviousLogin()
    }

    private fun initializeViews() {
        emailLayout = findViewById(R.id.emailLayout)
        passwordLayout = findViewById(R.id.passwordLayout)
        emailEditText = findViewById(R.id.email)
        passwordEditText = findViewById(R.id.password)
        loginButton = findViewById(R.id.loginButton)
        signupLink = findViewById(R.id.signupLink)
        sharedPreferences = getSharedPreferences("TrackMyFundsPrefs", MODE_PRIVATE)
    }

    private fun setupTextWatchers() {
        // Re-validate email on every change
        emailEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateEmail(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Re-validate password on every change
        passwordEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validatePassword(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupClickListeners() {
        loginButton.setOnClickListener {
            if (validateForm()) {
                attemptLogin()
            }
        }

        signupLink.setOnClickListener {
            // Navigate to signup screen with animation
            startActivity(Intent(this, SignupActivity::class.java))
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    private fun checkPreviousLogin() {
        // If user already logged in, skip login screen
        if (sharedPreferences.getBoolean("isLoggedIn", false)) {
            navigateToMain()
        }
    }

    /**
     * Validates email input: non-empty and matches email regex
     */
    private fun validateEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                emailLayout.error = "Email is required"
                false
            }
            !EMAIL_PATTERN.matcher(email).matches() -> {
                emailLayout.error = "Invalid email format"
                false
            }
            else -> {
                emailLayout.error = null
                true
            }
        }
    }

    /**
     * Validates password input: non-empty and minimum length
     */
    private fun validatePassword(password: String): Boolean {
        return when {
            password.isEmpty() -> {
                passwordLayout.error = "Password is required"
                false
            }
            password.length < MIN_PASSWORD_LENGTH -> {
                passwordLayout.error = "Password must be at least $MIN_PASSWORD_LENGTH characters"
                false
            }
            else -> {
                passwordLayout.error = null
                true
            }
        }
    }

    /**
     * Validates both fields before attempting login
     */
    private fun validateForm(): Boolean {
        val isEmailValid = validateEmail(emailEditText.text.toString())
        val isPasswordValid = validatePassword(passwordEditText.text.toString())
        return isEmailValid && isPasswordValid
    }

    /**
     * Checks entered credentials against saved ones in SharedPreferences
     */
    private fun attemptLogin() {
        val email = emailEditText.text.toString()
        val password = passwordEditText.text.toString()

        val savedEmail = sharedPreferences.getString("email", null)
        val savedPassword = sharedPreferences.getString("password", null)

        if (email == savedEmail && password == savedPassword) {
            loginSuccess()
        } else {
            loginFailure()
        }
    }

    private fun loginSuccess() {
        // Mark user as logged in and save timestamp
        sharedPreferences.edit().apply {
            putBoolean("isLoggedIn", true)
            putString("lastLoginDate", "2025-04-04 19:43:28") // Ideally use current timestamp
            putString("currentUser", "") // Store user identifier if needed
            apply()
        }

        showToast("Login successful")
        navigateToMain()
    }

    private fun loginFailure() {
        showToast("Invalid email or password")
        // Clear and focus password field
        passwordEditText.text?.clear()
        passwordEditText.requestFocus()

        // Show inline errors
        passwordLayout.error = "Incorrect password"
        emailLayout.error = "Email not found"

        // Temporarily disable login to prevent rapid retries
        loginButton.isEnabled = false
        loginButton.postDelayed({
            loginButton.isEnabled = true
            passwordLayout.error = null
            emailLayout.error = null
        }, 2000)
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
