package com.example.testime

import android.content.Context
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.TableLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.math.abs

class CandidateAdapter(private val context: Context) : RecyclerView.Adapter<CandidateAdapter.ViewHolder>() {

    private val candidates = mutableListOf<String>()
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val candidateTextView: TextView = itemView.findViewById(R.id.candidateTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_candidate, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position < candidates.size) {
            val candidate = candidates[position]
            holder.candidateTextView.text = candidate
            holder.itemView.setOnClickListener { handleCandidateClick(candidate) }
        } else {
            // Handle the case when the position is out of bounds for candidates
            holder.candidateTextView.text = ""
            holder.itemView.setOnClickListener(null)
        }
    }

    override fun getItemCount(): Int {
        return if (candidates.isEmpty()) 1 else candidates.size
    }

    private fun handleCandidateClick(candidate: String) {
        (context as TestIME).handleCandidateClick(candidate)
        clearCandidates()
    }

    fun updateCandidates(newCandidates: List<String>) {
        candidates.clear()
        candidates.addAll(newCandidates)
        notifyDataSetChanged()
    }
    fun clearCandidates() {
        candidates.clear()
        notifyDataSetChanged()
    }
}

class TestIME : InputMethodService() {
    data class Candidate(val input: String, val output: String)
    private var lastTouchX = 0.0f
    private var lastTouchY = 0.0f
    private var inputTextView: TextView? = null
    private var candidateRecyclerView: RecyclerView? = null
    private var tableLayout: TableLayout? = null
    private var view: ConstraintLayout? = null
    private var candidateList = emptyList<Candidate>()
    private var mappingTable = emptyMap<String, String>()
    private val handler = Handler(Looper.getMainLooper())
    private val functionKeyMap = mapOf(
        "⏎" to KeyEvent.KEYCODE_ENTER, " " to KeyEvent.KEYCODE_SPACE, "↹" to KeyEvent.KEYCODE_TAB,
        "←" to KeyEvent.KEYCODE_DPAD_LEFT, "→" to KeyEvent.KEYCODE_DPAD_RIGHT, "⇧" to KeyEvent.KEYCODE_SHIFT_LEFT)
    companion object {
        private const val SWIPE_THRESHOLD = 50.0f
        private const val DELAY_MILLIS = 50L  // 設定長按延遲的毫秒數
    }
    override fun onCreate() {
        super.onCreate()
        loadWords()
        loadMapping()
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

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        // 在這裡進行相應的初始化和重置

        // 清除候選字視窗的內容
        clearInputTextView()

        super.onStartInputView(info, restarting)
    }
    private fun clearInputTextView() {
        inputTextView?.text = "" // 或者設置為空字符串，視需求而定
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d("my","onConfigurationChanged")
    }

    override fun onCreateInputView(): View {
        Log.d("my","onCreateInputView")
//        view?.removeAllViewsInLayout()

        view = layoutInflater.inflate(R.layout.keyboard_array, null) as ConstraintLayout

        // Initialize inputTextView
        inputTextView = view?.findViewById(R.id.inputTextView)
        inputTextView?.text = ""


        // 在 onCreateInputView 中初始化 RecyclerView
        candidateRecyclerView = view?.findViewById(R.id.candidateRecyclerView)
        candidateRecyclerView?.layoutManager = LinearLayoutManager(this, RecyclerView.HORIZONTAL, false)
        candidateRecyclerView?.adapter = CandidateAdapter(this)

        tableLayout = view?.findViewById(R.id.tableLayout)
        setupButtons()
        return view as ConstraintLayout
    }
    private fun setupButtons() {
//        val buttonLabels = arrayOf("A", "1", "2", "3", "←", "B", "4", "5", "6", "Enter",
//            "C", "7", "8", "9", "Space", "D", ",", "0", ".", "Mode")
//
//        // 使用迴圈動態生成按鈕，添加到 TableLayout 中
//        var index = 0
//        while (index < buttonLabels.size) {
//            val tableRow = TableRow(this)
//            tableRow.layoutParams = TableLayout.LayoutParams().apply {
//                width = TableLayout.LayoutParams.MATCH_PARENT
//                height = TableLayout.LayoutParams.MATCH_PARENT
//                weight = 1F
//            }
//
//            for (i in 0 until 5) {
//                if (index < buttonLabels.size) {
//                    val label = buttonLabels[index]
//                    val button = Button(this)
//                    button.text = label
//                    button.layoutParams = TableRow.LayoutParams(
//                        TableRow.LayoutParams.MATCH_PARENT,
//                        TableRow.LayoutParams.MATCH_PARENT
//                    )
//                    button.backgroundTintList = ContextCompat.getColorStateList(this, R.color.num_key)
//                    button.setTextColor(ContextCompat.getColor(this, R.color.white))
//                    button.setOnTouchListener { _, event -> handleButtonTouch(label, event, button) }
//                    button.scaleX = 0.95F
//                    button.scaleY = 0.95F
//                    tableRow.addView(button)
//                    index++
//                }
//            }
//
//            tableLayout?.addView(tableRow)
//        }


        for (i in 1..20) {
            val buttonId = resources.getIdentifier("button$i", "id", packageName)
            val button = view?.findViewById<Button>(buttonId)
            button?.setOnTouchListener { _, event -> handleButtonTouch(button.text.toString(), event, button) }
        }
    }

    private fun commitCandidate(candidate: String) {
        // 在這裡執行提交候選字的操作，例如將候選字添加到輸入框
        // 這裡僅做一個簡單的示例，實際上應根據你的應用需求進行相應的操作
        val inputConnection = currentInputConnection
        inputConnection?.commitText(candidate, candidate.length)
    }

    private fun handleButtonTouch(value: String, event: MotionEvent, button: Button): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN ->
            {
                button.animate()
                    .scaleX(0.9f)
                    .scaleY(0.9f)
                    .alpha(0.7f)
                    .setDuration(0)
                    .start()
                // 紀錄按下的位置
                lastTouchX = event.x
                lastTouchY = event.y

                when(value) {
                    "⌫" -> handler.postDelayed(backKeyDown, DELAY_MILLIS)
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
                // 當手指離開時，根據滑動方向執行相應的操作
                val deltaX = event.x - lastTouchX
                val deltaY = event.y - lastTouchY
                lastTouchX = 0.0f
                lastTouchY = 0.0f  // 重置 lastTouchY
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
                }
                else when(value) {
                    "⌫" -> handler.removeCallbacks(backKeyDown)
                    else -> functionKeyMap[value]?.let { sendUpKeyEvent(it) }
                }
            }
        }
        return true
    }

    private val backKeyDown = object : Runnable {
        override fun run() {
            deleteLastInput()
            handler.postDelayed(this, DELAY_MILLIS * 2)
        }
    }

    private fun handleSwipeUp(value: String) {
        // 根據滑動方向執行相應的操作
        updateInputTextView("$value↑")
    }

    private fun handleSwipeDown(value: String) {
        // 根據滑動方向執行相應的操作
        updateInputTextView("$value↓")
    }

    private fun handleSwipeLeft(value: String) {
        // 根據滑動方向執行相應的操作
        when (value) {
            "1" -> updateInputTextView("C")
            // 其他按鍵的處理...
        }
    }

    private fun handleSwipeRight(value: String) {
        // 根據滑動方向執行相應的操作
        when (value) {
            "1" -> updateInputTextView("D")
            // 其他按鍵的處理...
        }
    }

    private fun handleRegularClick(value: String) {
        // 正常的按下操作
        updateInputTextView("$value-")
    }

    // 新增方法，根據按鍵值更新 RecyclerView 的內容
    private fun updateCandidateView() {
        val currentInput = inputTextView?.text?.toString() ?: ""
        val candidates = getCandidatesForInput(currentInput)
        (candidateRecyclerView?.adapter as? CandidateAdapter)?.updateCandidates(candidates)
    }
    private fun getCandidatesForInput(input: String): List<String> {
        // 根據已輸入的內容獲取相應的候選詞列表
        // 這裡你需要根據你的需求實現具體的邏輯，例如從預設的候選詞中篩選
        // 這裡只是一個簡單的示例，你可能需要擴展這個邏輯
        val splitInput = input.split(" ")
        val convertInput = splitInput.joinToString("") { mappingTable[it] ?: it }
//        val filteredCandidates = candidateList.filter { it.input.startsWith(convertInput) }
        val filteredCandidates = candidateList.filter { it.input == convertInput }
//        val sorted = filteredCandidates.sortedWith(compareBy({it.input.length}, {filteredCandidates.indexOf(it)}))
        return filteredCandidates.map { it.output }
    }

    private fun deleteLastInput() {
        inputTextView?.let {
            val currentText = it.text.toString()
            when {
                currentText.isEmpty() -> {
                    // 如果 inputTextView 為空，則模擬按下 Back 鍵，可以觸發删除游標前的文字
                    sendDownKeyEvent(KeyEvent.KEYCODE_DEL)
                }
                else -> {
                    val newText = currentText.substring(0, currentText.lastIndexOf(" "))
                    it.text = newText
                    if(it.text.isEmpty()) {
                        clearInputTextView()
                        (candidateRecyclerView?.adapter as? CandidateAdapter)?.clearCandidates()
                    }
                    else
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
//        val currentText = inputTextView?.text.toString()
//        inputTextView?.text = "$currentText $value"
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