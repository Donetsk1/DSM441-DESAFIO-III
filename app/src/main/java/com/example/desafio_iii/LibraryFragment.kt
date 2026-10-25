package com.example.desafio_iii

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.desafio_iii.data.Book
import com.example.desafio_iii.data.BookRepository
import com.example.desafio_iii.data.PendingBookEdit
import com.example.desafio_iii.databinding.FragmentLibraryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ValueEventListener

class LibraryFragment : Fragment() {

    private var _binding: FragmentLibraryBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val adapter = LibraryBookAdapter { book -> onBookClicked(book) }

    private var booksListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLibraryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.libraryRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.libraryRecyclerView.adapter = adapter

        val uid = auth.currentUser?.uid ?: return
        booksListener = BookRepository.observeBooks(uid) { books -> updateList(books) }
    }

    private fun updateList(books: List<Book>) {
        if (_binding == null) return
        adapter.submitList(books)
        binding.emptyLibraryText.visibility = if (books.isEmpty()) View.VISIBLE else View.GONE
        binding.libraryRecyclerView.visibility = if (books.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun onBookClicked(book: Book) {
        PendingBookEdit.set(book.id)
        (requireActivity() as MainActivity).selectBottomNavItem(R.id.nav_books)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val uid = auth.currentUser?.uid
        if (uid != null) {
            booksListener?.let { BookRepository.removeBooksListener(uid, it) }
        }
        booksListener = null
        _binding = null
    }
}
