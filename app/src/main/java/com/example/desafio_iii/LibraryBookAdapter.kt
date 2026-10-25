package com.example.desafio_iii

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.desafio_iii.data.Book
import com.example.desafio_iii.data.BookStatus
import com.example.desafio_iii.data.progressPercent
import com.example.desafio_iii.databinding.ItemBookLibraryBinding

class LibraryBookAdapter(
    private val onItemClick: (Book) -> Unit
) : RecyclerView.Adapter<LibraryBookAdapter.ViewHolder>() {

    private var books: List<Book> = emptyList()

    fun submitList(newBooks: List<Book>) {
        books = newBooks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBookLibraryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(books[position])
    }

    override fun getItemCount(): Int = books.size

    class ViewHolder(
        private val binding: ItemBookLibraryBinding,
        private val onItemClick: (Book) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(book: Book) {
            val percent = book.progressPercent()
            binding.bookTitleText.text = book.title
            binding.bookAuthorText.text = book.author
            binding.bookGenreStatusText.text = binding.root.context.getString(
                R.string.label_genre_status_format, book.genre, book.status
            )
            binding.progressPercentText.text = binding.root.context.getString(R.string.percent_format, percent)
            binding.bookProgressBar.progress = percent

            val isFinished = book.status == BookStatus.TERMINADO
            binding.bookRatingBar.visibility = if (isFinished) View.VISIBLE else View.GONE
            binding.bookRatingBar.rating = book.rating.toFloat()

            binding.root.setOnClickListener { onItemClick(book) }
        }
    }
}
