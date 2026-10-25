package com.example.desafio_iii

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.desafio_iii.data.Book
import com.example.desafio_iii.data.BookGenre
import com.example.desafio_iii.data.BookRepository
import com.example.desafio_iii.data.BookStatus
import com.example.desafio_iii.databinding.FragmentStatsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()

    private var booksListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val uid = auth.currentUser?.uid ?: return
        booksListener = BookRepository.observeBooks(uid) { books -> updateStats(books) }
    }

    private fun updateStats(books: List<Book>) {
        if (_binding == null) return

        val allFinished = books.filter { it.status == BookStatus.TERMINADO }
        if (allFinished.isEmpty()) {
            binding.emptyStatsText.visibility = View.VISIBLE
            binding.statsContent.visibility = View.GONE
            return
        }
        binding.emptyStatsText.visibility = View.GONE
        binding.statsContent.visibility = View.VISIBLE

        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val finishedThisYear = allFinished.filter { it.finishedAt?.let { ts -> yearOf(ts) == currentYear } == true }

        val genreCounts = BookGenre.ALL.associateWith { genre -> finishedThisYear.count { it.genre == genre } }
        binding.genreBreakdownText.text = genreCounts.entries.joinToString("\n") { (genre, count) ->
            getString(R.string.stats_genre_count_format, genre, count)
        }

        val maxCount = genreCounts.values.maxOrNull() ?: 0
        val topGenres = genreCounts.filterValues { it == maxCount }.keys
        binding.mostReadGenreText.text = when {
            maxCount == 0 -> getString(R.string.label_goal_na)
            topGenres.size > 1 -> getString(R.string.stats_most_read_genre_tie)
            else -> topGenres.first()
        }

        val totalPages = finishedThisYear.sumOf { it.pagesRead }
        binding.totalPagesYearText.text = totalPages.toString()

        val bestRatedBook = finishedThisYear
            .filter { it.rating >= 1 }
            .sortedWith(compareByDescending<Book> { it.rating }.thenBy { it.finishedAt ?: Long.MAX_VALUE })
            .firstOrNull()

        if (bestRatedBook == null) {
            binding.bestRatedBookText.text = getString(R.string.label_goal_na)
            binding.bestRatedBookRatingBar.visibility = View.GONE
        } else {
            binding.bestRatedBookText.text = getString(
                R.string.stats_best_rated_book_format, bestRatedBook.title, bestRatedBook.author
            )
            binding.bestRatedBookRatingBar.rating = bestRatedBook.rating.toFloat()
            binding.bestRatedBookRatingBar.visibility = View.VISIBLE
        }
    }

    private fun yearOf(epochMillis: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = epochMillis
        return calendar.get(Calendar.YEAR)
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
