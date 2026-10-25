package com.example.desafio_iii

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.desafio_iii.data.Book
import com.example.desafio_iii.data.BookRepository
import com.example.desafio_iii.data.BookStatus
import com.example.desafio_iii.databinding.DialogDeleteAccountConfirmBinding
import com.example.desafio_iii.databinding.DialogDeleteAccountPasswordBinding
import com.example.desafio_iii.databinding.DialogEditGoalBinding
import com.example.desafio_iii.databinding.FragmentHomeBinding
import com.example.desafio_iii.util.AuthErrorMapper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val adapter = BookProgressAdapter()

    private var goalListener: ValueEventListener? = null
    private var booksListener: ValueEventListener? = null

    private var currentGoal: Int? = null
    private var currentBooks: List<Book> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.greetingText.text = getString(R.string.greeting_format, auth.currentUser?.email.orEmpty())

        binding.currentlyReadingRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.currentlyReadingRecyclerView.adapter = adapter

        binding.editGoalButton.setOnClickListener { showEditGoalDialog() }
        binding.addBookButton.setOnClickListener { (requireActivity() as MainActivity).selectBottomNavItem(R.id.nav_books) }
        binding.viewLibraryButton.setOnClickListener { (requireActivity() as MainActivity).selectBottomNavItem(R.id.nav_library) }
        binding.viewStatsButton.setOnClickListener { (requireActivity() as MainActivity).selectBottomNavItem(R.id.nav_stats) }

        binding.logoutButton.setOnClickListener {
            auth.signOut()
            goToLogin()
        }

        binding.deleteAccountButton.setOnClickListener { showDeleteAccountPasswordDialog() }

        val uid = auth.currentUser?.uid ?: return
        goalListener = BookRepository.observeGoal(uid) { goal ->
            currentGoal = goal
            updateGoalSummary()
        }
        booksListener = BookRepository.observeBooks(uid) { books ->
            currentBooks = books
            updateGoalSummary()
            updateCurrentlyReading()
        }
    }

    private fun updateGoalSummary() {
        val finishedThisYear = finishedBooksThisYear(currentBooks).size
        val goal = currentGoal

        val goalText = goal?.toString() ?: getString(R.string.label_goal_undefined)
        val remainingText = if (goal != null) {
            (goal - finishedThisYear).coerceAtLeast(0).toString()
        } else {
            getString(R.string.label_goal_na)
        }

        binding.goalSummaryText.text =
            getString(R.string.label_goal_summary, goalText, finishedThisYear, remainingText)
    }

    private fun updateCurrentlyReading() {
        val readingBooks = currentBooks.filter { it.status == BookStatus.LEYENDO }
        adapter.submitList(readingBooks)
        binding.emptyReadingText.visibility = if (readingBooks.isEmpty()) View.VISIBLE else View.GONE
        binding.currentlyReadingRecyclerView.visibility = if (readingBooks.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun finishedBooksThisYear(books: List<Book>): List<Book> {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return books.filter { book ->
            book.status == BookStatus.TERMINADO && book.finishedAt?.let { yearOf(it) == currentYear } == true
        }
    }

    private fun yearOf(epochMillis: Long): Int {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = epochMillis
        return calendar.get(Calendar.YEAR)
    }

    private fun showEditGoalDialog() {
        val dialogBinding = DialogEditGoalBinding.inflate(LayoutInflater.from(requireContext()))
        currentGoal?.let { dialogBinding.goalEditText.setText(it.toString()) }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_edit_goal)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.action_save, null)
            .setNegativeButton(R.string.action_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val input = dialogBinding.goalEditText.text?.toString()?.trim()
                val goal = input?.toIntOrNull()
                val uid = auth.currentUser?.uid
                if (goal == null || goal <= 0 || uid == null) {
                    dialogBinding.goalEditText.error = getString(R.string.error_goal_invalid)
                } else {
                    BookRepository.setGoal(uid, goal) { }
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    private fun showDeleteAccountPasswordDialog() {
        val dialogBinding = DialogDeleteAccountPasswordBinding.inflate(LayoutInflater.from(requireContext()))

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_delete_account_password)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.action_continue, null)
            .setNegativeButton(R.string.action_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val password = dialogBinding.deletePasswordEditText.text?.toString().orEmpty()
                if (password.isEmpty()) {
                    dialogBinding.deletePasswordInputLayout.error = getString(R.string.error_password_required)
                    return@setOnClickListener
                }
                dialogBinding.deletePasswordInputLayout.error = null
                reauthenticateAndContinue(password, dialog, dialogBinding)
            }
        }
        dialog.show()
    }

    private fun reauthenticateAndContinue(
        password: String,
        dialog: AlertDialog,
        dialogBinding: DialogDeleteAccountPasswordBinding
    ) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return
        val credential = EmailAuthProvider.getCredential(email, password)

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        user.reauthenticate(credential).addOnCompleteListener { task ->
            if (_binding == null) return@addOnCompleteListener
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
            if (task.isSuccessful) {
                dialog.dismiss()
                showDeleteAccountConfirmDialog()
            } else {
                val exception = task.exception
                val message = exception?.let { getString(AuthErrorMapper.messageResFor(it)) }
                    ?: getString(R.string.error_generic)
                dialogBinding.deletePasswordInputLayout.error = message
            }
        }
    }

    private fun showDeleteAccountConfirmDialog() {
        val dialogBinding = DialogDeleteAccountConfirmBinding.inflate(LayoutInflater.from(requireContext()))

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.title_delete_account_confirm)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.action_delete, null)
            .setNegativeButton(R.string.action_cancel, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val input = dialogBinding.deleteConfirmEditText.text?.toString().orEmpty()
                if (input != "CONFIRMAR") {
                    dialogBinding.deleteConfirmInputLayout.error =
                        getString(R.string.error_delete_confirmation_mismatch)
                    return@setOnClickListener
                }
                dialogBinding.deleteConfirmInputLayout.error = null
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                performAccountDeletion(dialog)
            }
        }
        dialog.show()
    }

    private fun performAccountDeletion(dialog: AlertDialog) {
        val user = auth.currentUser ?: return
        BookRepository.deleteAllUserData(user.uid) { success ->
            if (_binding == null) return@deleteAllUserData
            if (!success) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                Snackbar.make(binding.root, R.string.error_generic, Snackbar.LENGTH_LONG).show()
                return@deleteAllUserData
            }
            user.delete().addOnCompleteListener { task ->
                if (_binding == null) return@addOnCompleteListener
                if (task.isSuccessful) {
                    dialog.dismiss()
                    goToLogin()
                } else {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                    Snackbar.make(binding.root, R.string.error_generic, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun goToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val uid = auth.currentUser?.uid
        if (uid != null) {
            goalListener?.let { BookRepository.removeGoalListener(uid, it) }
            booksListener?.let { BookRepository.removeBooksListener(uid, it) }
        }
        goalListener = null
        booksListener = null
        _binding = null
    }
}
