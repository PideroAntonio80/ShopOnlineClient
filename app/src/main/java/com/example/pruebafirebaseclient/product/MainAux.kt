package com.example.pruebafirebaseclient.product

import com.google.firebase.auth.FirebaseUser

interface MainAux {
    fun showButton(isVisible: Boolean)
    fun updateTitle(user: FirebaseUser)
}