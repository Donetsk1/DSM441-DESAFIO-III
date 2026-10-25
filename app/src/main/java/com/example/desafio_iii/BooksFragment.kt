package com.example.desafio_iii

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.desafio_iii.data.Book
import com.example.desafio_iii.data.BookGenre
import com.example.desafio_iii.data.BookRepository
import com.example.desafio_iii.data.BookStatus
import com.example.desafio_iii.data.PendingBookEdit
import com.example.desafio_iii.databinding.FragmentBooksBinding
import com.example.desafio_iii.util.NoFilterArrayAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class BooksFragment : Fragment() {

    private var _binding: FragmentBooksBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()

    private var bookId: String? = null
    private var editingBook: Book? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBooksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bookId = arguments?.getString("bookId") ?: PendingBookEdit.consume()

        binding.genreDropdown.setAdapter(
            NoFilterArrayAdapter(requireContext(), BookGenre.ALL)
        )
        binding.genreDropdown.keyListener = null

        binding.statusDropdown.setAdapter(
            NoFilterArrayAdapter(requireContext(), BookStatus.ALL)
        )
        binding.statusDropdown.keyListener = null
        binding.statusDropdown.setOnItemClickListener { _, _, position, _ ->
            toggleRatingSection(BookStatus.ALL[position] == BookStatus.TERMINADO)
        }

        binding.ratingBar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    val starWidth = binding.ratingBar.width.toFloat() / binding.ratingBar.numStars
                    val star = (event.x / starWidth).toInt() + 1
                    binding.ratingBar.rating = star.coerceIn(1, binding.ratingBar.numStars).toFloat()
                    true
                }
                MotionEvent.ACTION_UP -> true
                else -> false
            }
        }

        binding.saveBookButton.setOnClickListener { onSaveClicked() }
        binding.deleteBookButton.setOnClickListener { confirmDeleteBook() }

        val id = bookId
        val uid = auth.currentUser?.uid
        if (id != null && uid != null) {
            binding.formTitleText.text = getString(R.string.title_edit_book)
            BookRepository.getBook(uid, id) { book ->
                if (_binding == null || book == null) return@getBook
                editingBook = book
                prefill(book)
                binding.deleteBookButton.visibility = View.VISIBLE
            }
        }
    }

    private fun prefill(book: Book) {
        binding.titleEditText.setText(book.title)
        binding.authorEditText.setText(book.author)
        binding.genreDropdown.setText(book.genre, false)
        binding.totalPagesEditText.setText(book.totalPages.toString())
        binding.pagesReadEditText.setText(book.pagesRead.toString())
        binding.statusDropdown.setText(book.status, false)
        binding.ratingBar.rating = book.rating.toFloat()
        toggleRatingSection(book.status == BookStatus.TERMINADO)
    }

    private fun toggleRatingSection(show: Boolean) {
        binding.ratingSection.visibility = if (show) View.VISIBLE else View.GONE
        if (!show) {
            binding.ratingBar.rating = 0f
            binding.ratingErrorText.visibility = View.GONE
        }
    }

    private fun onSaveClicked() {
        val uid = auth.currentUser?.uid ?: return

        val title = binding.titleEditText.text?.toString()?.trim().orEmpty()
        val author = binding.authorEditText.text?.toString()?.trim().orEmpty()
        val genre = binding.genreDropdown.text?.toString()?.trim().orEmpty()
        val status = binding.statusDropdown.text?.toString()?.trim().orEmpty()
        val totalPagesText = binding.totalPagesEditText.text?.toString()?.trim().orEmpty()
        val pagesReadText = binding.pagesReadEditText.text?.toString()?.trim().orEmpty()

        var isValid = true

        when {
            title.isEmpty() -> {
                binding.titleInputLayout.error = getString(R.string.error_title_required)
                isValid = false
            }
            title.length < 2 || title.length > 60 -> {
                binding.titleInputLayout.error = getString(R.string.error_title_length)
                isValid = false
            }
            else -> binding.titleInputLayout.error = null
        }

        when {
            author.isEmpty() -> {
                binding.authorInputLayout.error = getString(R.string.error_author_required)
                isValid = false
            }
            author.length < 2 || author.length > 60 -> {
                binding.authorInputLayout.error = getString(R.string.error_author_length)
                isValid = false
            }
            else -> binding.authorInputLayout.error = null
        }

        if (genre !in BookGenre.ALL) {
            binding.genreInputLayout.error = getString(R.string.error_genre_required)
            isValid = false
        } else {
            binding.genreInputLayout.error = null
        }

        if (status !in BookStatus.ALL) {
            binding.statusInputLayout.error = getString(R.string.error_status_required)
            isValid = false
        } else {
            binding.statusInputLayout.error = null
        }

        val totalPages = totalPagesText.toIntOrNull()
        if (totalPages == null || totalPages <= 0) {
            binding.totalPagesInputLayout.error = getString(R.string.error_total_pages_invalid)
            isValid = false
        } else {
            binding.totalPagesInputLayout.error = null
        }

        val pagesRead = pagesReadText.toIntOrNull()
        if (pagesRead == null || pagesRead < 0 || (totalPages != null && pagesRead > totalPages)) {
            binding.pagesReadInputLayout.error = getString(R.string.error_pages_read_invalid)
            isValid = false
        } else {
            binding.pagesReadInputLayout.error = null
        }

        val isTerminado = status == BookStatus.TERMINADO
        val rating = binding.ratingBar.rating.toInt()
        if (isTerminado && rating < 1) {
            binding.ratingErrorText.setText(R.string.error_rating_required)
            binding.ratingErrorText.visibility = View.VISIBLE
            isValid = false
        } else {
            binding.ratingErrorText.visibility = View.GONE
        }

        if (!isValid) return

        val now = System.currentTimeMillis()
        val book = Book(
            id = editingBook?.id.orEmpty(),
            title = title,
            author = author,
            genre = genre,
            totalPages = totalPages ?: 0,
            pagesRead = pagesRead ?: 0,
            status = status,
            rating = if (isTerminado) rating else 0,
            finishedAt = if (isTerminado) (editingBook?.finishedAt ?: now) else null,
            createdAt = editingBook?.createdAt ?: now
        )

        binding.saveBookButton.isEnabled = false
        if (book.id.isBlank()) {
            BookRepository.createBook(uid, book) { success -> handleSaveResult(success, isUpdate = false) }
        } else {
            BookRepository.updateBook(uid, book) { success -> handleSaveResult(success, isUpdate = true) }
        }
    }

    private fun handleSaveResult(success: Boolean, isUpdate: Boolean) {
        if (_binding == null) return
        binding.saveBookButton.isEnabled = true

        if (!success) {
            Snackbar.make(binding.root, R.string.error_generic, Snackbar.LENGTH_LONG).show()
            return
        }

        if (isUpdate) {
            Snackbar.make(binding.root, R.string.message_book_updated, Snackbar.LENGTH_SHORT).show()
        } else {
            Snackbar.make(binding.root, R.string.message_book_saved, Snackbar.LENGTH_SHORT).show()
            clearForm()
        }
    }

    private fun confirmDeleteBook() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_delete_book_confirm)
            .setMessage(R.string.message_delete_book_confirm)
            .setPositiveButton(R.string.action_delete) { _, _ -> performDeleteBook() }
            .setNegativeButton(R.string.action_cancel, null)
            .show()
    }

    private fun performDeleteBook() {
        val uid = auth.currentUser?.uid ?: return
        val id = editingBook?.id ?: return
        binding.deleteBookButton.isEnabled = false
        binding.saveBookButton.isEnabled = false
        BookRepository.deleteBook(uid, id) { success ->
            if (_binding == null) return@deleteBook
            binding.deleteBookButton.isEnabled = true
            binding.saveBookButton.isEnabled = true
            if (success) {
                Snackbar.make(binding.root, R.string.message_book_deleted, Snackbar.LENGTH_SHORT).show()
                (requireActivity() as MainActivity).selectBottomNavItem(R.id.nav_library)
            } else {
                Snackbar.make(binding.root, R.string.error_generic, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun clearForm() {
        binding.titleEditText.text = null
        binding.authorEditText.text = null
        binding.genreDropdown.setText("", false)
        binding.totalPagesEditText.text = null
        binding.pagesReadEditText.text = null
        binding.statusDropdown.setText("", false)
        binding.titleInputLayout.error = null
        binding.authorInputLayout.error = null
        binding.genreInputLayout.error = null
        binding.statusInputLayout.error = null
        binding.totalPagesInputLayout.error = null
        binding.pagesReadInputLayout.error = null
        toggleRatingSection(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
