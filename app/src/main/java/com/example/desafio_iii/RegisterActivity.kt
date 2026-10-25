package com.example.desafio_iii

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import com.example.desafio_iii.databinding.ActivityRegisterBinding
import com.example.desafio_iii.util.AuthErrorMapper
import com.example.desafio_iii.util.BaseActivity
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : BaseActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.registerButton.setOnClickListener { attemptRegister() }
        binding.goLoginText.setOnClickListener { finish() }
    }

    private fun attemptRegister() {
        val email = binding.emailEditText.text?.toString()?.trim().orEmpty()
        val password = binding.passwordEditText.text?.toString().orEmpty()
        val confirmPassword = binding.confirmPasswordEditText.text?.toString().orEmpty()

        binding.emailInputLayout.error = null
        binding.passwordInputLayout.error = null
        binding.confirmPasswordInputLayout.error = null

        var isValid = true

        if (email.isEmpty()) {
            binding.emailInputLayout.error = getString(R.string.error_email_required)
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailInputLayout.error = getString(R.string.error_email_invalid)
            isValid = false
        }

        if (password.isEmpty()) {
            binding.passwordInputLayout.error = getString(R.string.error_password_required)
            isValid = false
        } else if (password.length < 6) {
            binding.passwordInputLayout.error = getString(R.string.error_password_short)
            isValid = false
        }

        if (confirmPassword.isEmpty()) {
            binding.confirmPasswordInputLayout.error = getString(R.string.error_confirm_password_required)
            isValid = false
        } else if (password.isNotEmpty() && password != confirmPassword) {
            binding.confirmPasswordInputLayout.error = getString(R.string.error_password_mismatch)
            isValid = false
        }

        if (!isValid) return

        setLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                setLoading(false)
                if (task.isSuccessful) {
                    goToMain()
                } else {
                    val exception = task.exception
                    val message = exception?.let { getString(AuthErrorMapper.messageResFor(it)) }
                        ?: getString(R.string.error_generic)
                    binding.emailInputLayout.error = message
                }
            }
    }

    private fun setLoading(loading: Boolean) {
        binding.registerProgress.visibility = if (loading) View.VISIBLE else View.GONE
        binding.registerButton.isEnabled = !loading
    }

    private fun goToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
