package com.example.desafio_iii

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.desafio_iii.data.Book
import com.example.desafio_iii.data.progressPercent
import com.example.desafio_iii.databinding.ItemBookProgressBinding

class BookProgressAdapter : RecyclerView.Adapter<BookProgressAdapter.ViewHolder>() {

    private var books: List<Book> = emptyList()

    fun submitList(newBooks: List<Book>) {
        books = newBooks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBookProgressBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(books[position])
    }

    override fun getItemCount(): Int = books.size

    class ViewHolder(private val binding: ItemBookProgressBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(book: Book) {
            val percent = book.progressPercent()
            binding.bookTitleText.text = book.title
            binding.bookAuthorText.text = book.author
            binding.progressPercentText.text = binding.root.context.getString(R.string.percent_format, percent)
            binding.bookProgressBar.progress = percent
        }
    }
}
