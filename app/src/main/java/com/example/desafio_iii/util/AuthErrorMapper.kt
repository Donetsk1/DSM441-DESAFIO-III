package com.example.desafio_iii.util

import com.example.desafio_iii.R
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

object AuthErrorMapper {

    fun messageResFor(exception: Exception): Int = when (exception) {
        is FirebaseAuthUserCollisionException -> R.string.error_user_collision
        is FirebaseAuthWeakPasswordException -> R.string.error_weak_password
        is FirebaseNetworkException -> R.string.error_network
        is FirebaseTooManyRequestsException -> R.string.error_too_many_requests
        is FirebaseAuthInvalidCredentialsException,
        is FirebaseAuthInvalidUserException -> R.string.error_invalid_credentials
        else -> R.string.error_generic
    }
}
