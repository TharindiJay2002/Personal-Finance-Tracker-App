package com.example.trackmyfunds // Defines the package for the project

import android.content.Intent // For navigation between activities
import android.content.SharedPreferences // For storing user data persistently
import android.os.Bundle // Required for working with activity lifecycle
import android.text.Editable // Represents editable text
import android.text.TextWatcher // Interface for watching text changes
import android.util.Patterns // Provides common regex patterns like email
import android.widget.TextView // Basic text display widget
import android.widget.Toast // For showing small messages on screen
import androidx.appcompat.app.AppCompatActivity // Base class for activities
import com.google.android.material.button.MaterialButton // Material Design button
import com.google.android.material.textfield.TextInputEditText // Text input field
import com.google.android.material.textfield.TextInputLayout // Wrapper for EditText with error/helper display
import java.util.regex.Pattern // For regex matching

class SignupActivity : AppCompatActivity() { // Defines the SignupActivity class

    // Declare layouts for form fields
    private lateinit var emailLayout: TextInputLayout
    private lateinit var usernameLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout

    // Declare editable text inputs
    private lateinit var emailEditText: TextInputEditText
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText

    // Declare signup button and login text link
    private lateinit var signupButton: MaterialButton
    private lateinit var loginLink: TextView

    // SharedPreferences for saving user data
    private lateinit var sharedPreferences: SharedPreferences

    companion object { // Constants and patterns for validation
        private const val MIN_PASSWORD_LENGTH = 8
        private const val MIN_USERNAME_LENGTH = 3
        private val EMAIL_PATTERN: Pattern = Patterns.EMAIL_ADDRESS
        private val USERNAME_PATTERN: Pattern = Pattern.compile("^[a-zA-Z0-9_]+$")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // Call parent method
        setContentView(R.layout.activity_signup) // Set layout for the activity

        initializeViews() // Link XML views to Kotlin
        setupTextWatchers() // Add text change listeners for validation
        setupClickListeners() // Set up click events
    }

    private fun initializeViews() {
        // Bind layout views with their XML IDs
        emailLayout = findViewById(R.id.emailLayout)
        usernameLayout = findViewById(R.id.usernameLayout)
        passwordLayout = findViewById(R.id.passwordLayout)
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout)

        // Bind input fields
        emailEditText = findViewById(R.id.email)
        usernameEditText = findViewById(R.id.username)
        passwordEditText = findViewById(R.id.password)
        confirmPasswordEditText = findViewById(R.id.confirmPassword)

        // Bind button and login link
        signupButton = findViewById(R.id.signupButton)
        loginLink = findViewById(R.id.loginLink)

        // Initialize shared preferences
        sharedPreferences = getSharedPreferences("TrackMyFundsPrefs", MODE_PRIVATE)
    }

    private fun setupTextWatchers() {
        // Add validation on text change for all fields
        emailEditText.addTextChangedListener(createTextWatcher { validateEmail(it) })
        usernameEditText.addTextChangedListener(createTextWatcher { validateUsername(it) })
        passwordEditText.addTextChangedListener(createTextWatcher {
            validatePassword(it)
            validateConfirmPassword(confirmPasswordEditText.text.toString())
        })
        confirmPasswordEditText.addTextChangedListener(createTextWatcher {
            validateConfirmPassword(it)
        })
    }

    private fun createTextWatcher(validationFunction: (String) -> Boolean): TextWatcher {
        // Generic text watcher to run validation logic
        return object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validationFunction(s.toString()) // Run validation
            }
            override fun afterTextChanged(s: Editable?) {}
        }
    }

    private fun setupClickListeners() {
        signupButton.setOnClickListener {
            if (validateForm()) { // Check if all inputs are valid
                attemptSignup() // Register the user
            }
        }

        loginLink.setOnClickListener {
            // Go back to login screen
            startActivity(Intent(this, LoginActivity::class.java))
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
            finish()
        }
    }

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
            sharedPreferences.getString("email", null) == email -> {
                emailLayout.error = "Email already registered"
                false
            }
            else -> {
                emailLayout.error = null
                true
            }
        }
    }

    private fun validateUsername(username: String): Boolean {
        return when {
            username.isEmpty() -> {
                usernameLayout.error = "Username is required"
                false
            }
            username.length < MIN_USERNAME_LENGTH -> {
                usernameLayout.error = "Username must be at least $MIN_USERNAME_LENGTH characters"
                false
            }
            !USERNAME_PATTERN.matcher(username).matches() -> {
                usernameLayout.error = "Username can only contain letters, numbers, and underscores"
                false
            }
            sharedPreferences.getString("username", null) == username -> {
                usernameLayout.error = "Username already taken"
                false
            }
            else -> {
                usernameLayout.error = null
                true
            }
        }
    }

    private fun validatePassword(password: String): Boolean {
        // Password strength criteria
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }

        return when {
            password.isEmpty() -> {
                passwordLayout.error = "Password is required"
                false
            }
            password.length < MIN_PASSWORD_LENGTH -> {
                passwordLayout.error = "Password must be at least $MIN_PASSWORD_LENGTH characters"
                false
            }
            !hasUpperCase -> {
                passwordLayout.error = "Password must contain at least one uppercase letter"
                false
            }
            !hasLowerCase -> {
                passwordLayout.error = "Password must contain at least one lowercase letter"
                false
            }
            !hasDigit -> {
                passwordLayout.error = "Password must contain at least one number"
                false
            }
            !hasSpecialChar -> {
                passwordLayout.error = "Password must contain at least one special character"
                false
            }
            else -> {
                passwordLayout.error = null
                updatePasswordStrengthIndicator(password) // Show strength meter
                true
            }
        }
    }

    private fun validateConfirmPassword(confirmPassword: String): Boolean {
        return when {
            confirmPassword.isEmpty() -> {
                confirmPasswordLayout.error = "Please confirm your password"
                false
            }
            confirmPassword != passwordEditText.text.toString() -> {
                confirmPasswordLayout.error = "Passwords do not match"
                false
            }
            else -> {
                confirmPasswordLayout.error = null
                true
            }
        }
    }

    private fun updatePasswordStrengthIndicator(password: String) {
        // Show helper text depending on strength score
        val strength = calculatePasswordStrength(password)
        val helperText = when {
            strength >= 80 -> "Strong password"
            strength >= 60 -> "Good password"
            strength >= 40 -> "Moderate password"
            else -> "Weak password"
        }
        passwordLayout.helperText = helperText
    }

    private fun calculatePasswordStrength(password: String): Int {
        // Simple scoring system for password strength
        var score = 0
        if (password.length >= MIN_PASSWORD_LENGTH) score += 20
        if (password.any { it.isUpperCase() }) score += 20
        if (password.any { it.isLowerCase() }) score += 20
        if (password.any { it.isDigit() }) score += 20
        if (password.any { !it.isLetterOrDigit() }) score += 20
        return score
    }

    private fun validateForm(): Boolean {
        // Check all validation functions
        val isEmailValid = validateEmail(emailEditText.text.toString())
        val isUsernameValid = validateUsername(usernameEditText.text.toString())
        val isPasswordValid = validatePassword(passwordEditText.text.toString())
        val isConfirmPasswordValid = validateConfirmPassword(confirmPasswordEditText.text.toString())
        return isEmailValid && isUsernameValid && isPasswordValid && isConfirmPasswordValid
    }

    private fun attemptSignup() {
        // Save user input in SharedPreferences
        val email = emailEditText.text.toString()
        val username = usernameEditText.text.toString()
        val password = passwordEditText.text.toString()

        sharedPreferences.edit().apply {
            putString("email", email)
            putString("username", username)
            putString("password", password)
            putString("registrationDate", "2025-04-04 20:18:12") // Static date for now
            apply()
        }

        showSuccessAndNavigate()
    }

    private fun showSuccessAndNavigate() {
        // Show message and go to Login screen
        Toast.makeText(this, "Account created successfully! Please login.", Toast.LENGTH_LONG).show()
        startActivity(Intent(this, LoginActivity::class.java))
        finishAffinity() // Close all previous activities
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out) // Animation transition
    }
}
