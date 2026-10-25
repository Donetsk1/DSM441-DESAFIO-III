package com.example.desafio_iii.data

object PendingBookEdit {
    private var bookId: String? = null

    fun set(id: String) {
        bookId = id
    }

    fun consume(): String? {
        val id = bookId
        bookId = null
        return id
    }
}
