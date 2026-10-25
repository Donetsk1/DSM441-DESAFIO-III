package com.example.desafio_iii.util

import android.content.Context
import android.graphics.Rect
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity

abstract class BaseActivity : AppCompatActivity() {

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val focusedView = currentFocus
            if (focusedView is EditText) {
                val touchedRect = Rect()
                focusedView.getGlobalVisibleRect(touchedRect)
                if (!touchedRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    focusedView.clearFocus()
                    hideKeyboard(focusedView)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
}
