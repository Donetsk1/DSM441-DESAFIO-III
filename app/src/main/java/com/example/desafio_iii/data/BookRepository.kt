package com.example.desafio_iii.data

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

object BookRepository {

    private fun usersRef() = FirebaseDatabase.getInstance().reference.child("users")

    fun observeGoal(uid: String, onChange: (Int?) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                onChange(snapshot.getValue(Int::class.java))
            }

            override fun onCancelled(error: DatabaseError) {
                onChange(null)
            }
        }
        usersRef().child(uid).child("goal").addValueEventListener(listener)
        return listener
    }

    fun removeGoalListener(uid: String, listener: ValueEventListener) {
        usersRef().child(uid).child("goal").removeEventListener(listener)
    }

    fun setGoal(uid: String, goal: Int, onComplete: (Boolean) -> Unit) {
        usersRef().child(uid).child("goal").setValue(goal)
            .addOnCompleteListener { task -> onComplete(task.isSuccessful) }
    }

    fun observeBooks(uid: String, onChange: (List<Book>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val books = snapshot.children.mapNotNull { child ->
                    child.getValue(Book::class.java)?.copy(id = child.key.orEmpty())
                }
                onChange(books)
            }

            override fun onCancelled(error: DatabaseError) {
                onChange(emptyList())
            }
        }
        usersRef().child(uid).child("books").addValueEventListener(listener)
        return listener
    }

    fun removeBooksListener(uid: String, listener: ValueEventListener) {
        usersRef().child(uid).child("books").removeEventListener(listener)
    }

    fun createBook(uid: String, book: Book, onComplete: (Boolean) -> Unit) {
        val ref = usersRef().child(uid).child("books").push()
        val key = ref.key
        if (key == null) {
            onComplete(false)
            return
        }
        ref.setValue(book.copy(id = key)).addOnCompleteListener { task -> onComplete(task.isSuccessful) }
    }

    fun updateBook(uid: String, book: Book, onComplete: (Boolean) -> Unit) {
        usersRef().child(uid).child("books").child(book.id).setValue(book)
            .addOnCompleteListener { task -> onComplete(task.isSuccessful) }
    }

    fun deleteBook(uid: String, bookId: String, onComplete: (Boolean) -> Unit) {
        usersRef().child(uid).child("books").child(bookId).removeValue()
            .addOnCompleteListener { task -> onComplete(task.isSuccessful) }
    }

    fun deleteAllUserData(uid: String, onComplete: (Boolean) -> Unit) {
        usersRef().child(uid).removeValue()
            .addOnCompleteListener { task -> onComplete(task.isSuccessful) }
    }

    fun getBook(uid: String, bookId: String, onResult: (Book?) -> Unit) {
        usersRef().child(uid).child("books").child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    onResult(snapshot.getValue(Book::class.java)?.copy(id = snapshot.key.orEmpty()))
                }

                override fun onCancelled(error: DatabaseError) {
                    onResult(null)
                }
            })
    }
}
