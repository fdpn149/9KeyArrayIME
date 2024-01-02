package com.example.testime

import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TableLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Dictionary
import kotlin.math.abs
class InputMethod : InputMethodService() {
    data class Candidate(val input: String, val output: String)

    private lateinit var baseView: FrameLayout
    
    lateinit var arrayKeyboard: ArrayKeyboard
    
    private lateinit var englishView: ConstraintLayout
    private lateinit var inputTextView: TextView
    private lateinit var candidateRecyclerView: RecyclerView

    private var candidateList = emptyList<Candidate>()
    private var mappingTable = emptyMap<String, String>()
    private val functionKeyMap = mapOf(
        "⏎" to KeyEvent.KEYCODE_NUMPAD_ENTER,
        " " to KeyEvent.KEYCODE_SPACE,
        "↹" to KeyEvent.KEYCODE_TAB,
        "←" to KeyEvent.KEYCODE_DPAD_LEFT,
        "→" to KeyEvent.KEYCODE_DPAD_RIGHT,
        "⇧" to KeyEvent.KEYCODE_SHIFT_LEFT
    )

    private val handler = Handler(Looper.getMainLooper())

    private var lastTouchX = 0.0f
    private var lastTouchY = 0.0f
    private var mode = 0
    companion object {
        private const val SWIPE_THRESHOLD = 50.0f
        private const val DELAY_MILLIS = 50L
    }

    override fun onCreate() {
        super.onCreate()
        loadWords()
        loadMapping()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        clearInputTextView()
        super.onStartInputView(info, restarting)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d("my", "onConfigurationChanged")
    }

    override fun onCreateInputView(): View {
        Log.d("my", "onCreateInputView")
        baseView = layoutInflater.inflate(R.layout.input_method, null) as FrameLayout
        arrayKeyboard = ArrayKeyboard(baseView)
        englishView = baseView.findViewById(R.id.eng)
        inputTextView = arrayKeyboard.view.findViewById(R.id.inputTextView)
        inputTextView.text = ""
        candidateRecyclerView = arrayKeyboard.view.findViewById(R.id.candidateRecyclerView)
        candidateRecyclerView.layoutManager =
            LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        candidateRecyclerView.adapter = CandidateAdapter(this)
        setupButtons()
        return baseView
    }

    private fun loadWords() {
        try {
            val inputStream: InputStream = resources.openRawResource(R.raw.words)
            val candidates = mutableListOf<Candidate>()
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val parts = line?.split("\t") ?: continue
                    if (parts.size == 2) {
                        candidates.add(Candidate(parts[0], parts[1]))
                    }
                }
            }
            candidateList = candidates
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun loadMapping() {
        try {
            val inputStream: InputStream = resources.openRawResource(R.raw.mapping)
            val mapping = mutableMapOf<String, String>()
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val parts = line?.split(" ") ?: continue
                    if (parts.size == 2) {
                        mapping[parts[1]] = parts[0]
                    }
                }
            }
            mappingTable = mapping
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun clearInputTextView() {
        inputTextView?.text = ""
    }
    private fun setupButtons() {
        for (i in 1..20) {
            val buttonId = resources.getIdentifier("button$i", "id", packageName)
            val button = arrayKeyboard.view?.findViewById<Button>(buttonId)
            button?.setOnTouchListener { _, event ->
                handleButtonTouch(
                    button.text.toString(),
                    event,
                    button
                )
            }
        }
        val button = englishView?.findViewById<Button>(R.id.button16)
        button?.setOnTouchListener { _, event ->
            handleButtonTouch(
                button.text.toString(),
                event,
                button
            )
        }
    }

    private fun commitCandidate(candidate: String) {
        val inputConnection = currentInputConnection
        inputConnection?.commitText(candidate, candidate.length)
    }

    private fun handleButtonTouch(value: String, event: MotionEvent, button: Button): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                button.animate()
                    .scaleX(0.9f)
                    .scaleY(0.9f)
                    .alpha(0.7f)
                    .setDuration(0)
                    .start()
                lastTouchX = event.x
                lastTouchY = event.y
                when (value) {
                    "⌫" -> handler.postDelayed(backKeyDown, DELAY_MILLIS)
                    "M" -> {}
                    else -> functionKeyMap[value]?.let { sendDownKeyEvent(it) }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                button.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .alpha(1f)
                    .setDuration(0)
                    .start()
                val deltaX = event.x - lastTouchX
                val deltaY = event.y - lastTouchY
                lastTouchX = 0.0f
                lastTouchY = 0.0f
                if (value[0].isDigit()) {
                    when {
                        abs(deltaX) > abs(deltaY) && abs(deltaX) > SWIPE_THRESHOLD -> {
                            if (deltaX > 0) handleSwipeRight(value)
                            else handleSwipeLeft(value)
                        }

                        abs(deltaY) > abs(deltaX) && abs(deltaY) > SWIPE_THRESHOLD -> {
                            if (deltaY > 0) handleSwipeDown(value)
                            else handleSwipeUp(value)
                        }

                        else -> handleRegularClick(value)
                    }
                } else when (value) {
                    "⌫" -> handler.removeCallbacks(backKeyDown)
                    "M" -> changeMode()
                    else -> functionKeyMap[value]?.let { sendUpKeyEvent(it) }
                }
            }
        }
        return true
    }

    private fun changeMode() {
        Log.d("my", "change")
        arrayKeyboard.view.visibility = mode
        mode = mode xor 8
        englishView.visibility = mode
    }

    private val backKeyDown = object : Runnable {
        override fun run() {
            deleteLastInput()
            handler.postDelayed(this, DELAY_MILLIS * 2)
        }
    }

    private fun handleSwipeUp(value: String) {
        updateInputTextView("$value↑")
    }

    private fun handleSwipeDown(value: String) {
        updateInputTextView("$value↓")
    }

    private fun handleSwipeLeft(value: String) {
        when (value) {
            "1" -> updateInputTextView("C")
        }
    }

    private fun handleSwipeRight(value: String) {
        when (value) {
            "1" -> updateInputTextView("D")
        }
    }

    private fun handleRegularClick(value: String) {
        updateInputTextView("$value-")
    }

    private fun updateCandidateView() {
        val currentInput = inputTextView?.text?.toString() ?: ""
        val candidates = getCandidatesForInput(currentInput)
        (candidateRecyclerView?.adapter as? CandidateAdapter)?.updateCandidates(candidates)
    }

    private fun getCandidatesForInput(input: String): List<String> {
        val splitInput = input.split(" ")
        val convertInput = splitInput.joinToString("") { mappingTable[it] ?: it }
        val filteredCandidates = candidateList.filter { it.input == convertInput }
        return filteredCandidates.map { it.output }
    }

    private fun deleteLastInput() {
        inputTextView?.let {
            val currentText = it.text.toString()
            when {
                currentText.isEmpty() -> {
                    sendDownKeyEvent(KeyEvent.KEYCODE_DEL)
                }

                else -> {
                    val newText = currentText.substring(0, currentText.lastIndexOf(" "))
                    it.text = newText
                    if (it.text.isEmpty()) {
                        clearInputTextView()
                        (candidateRecyclerView?.adapter as? CandidateAdapter)?.clearCandidates()
                    } else
                        updateCandidateView()
                }
            }
        }
    }

    private fun sendUpKeyEvent(keyCode: Int) {
        val event = KeyEvent(KeyEvent.ACTION_UP, keyCode)
        currentInputConnection?.sendKeyEvent(event)
    }

    private fun sendDownKeyEvent(keyCode: Int) {
        val event = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        currentInputConnection?.sendKeyEvent(event)
    }

    private fun updateInputTextView(value: String) {
        inputTextView?.let {
            it.text = "${it.text} $value"
        }
        updateCandidateView()
    }

    fun handleCandidateClick(candidate: String) {
        commitCandidate(candidate)
        clearInputTextView()
    }
}