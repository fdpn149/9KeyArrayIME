package com.example.testime

import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.databinding.DataBindingUtil
import com.example.testime.databinding.ArrayKeyboardBinding

class ArrayKeyboard(baseView: FrameLayout, inputMethod: InputMethod) :
	BaseKeyboard(baseView, R.id.array, inputMethod) {
	/*Last Touch Position*/
	private var lastTouchX = 0.0f
	private var lastTouchY = 0.0f

	/*Handler*/
	private val handler = Handler(Looper.getMainLooper())

	val popTexts = { name: String ->
		arrayOf(
			"${name}-",
			name[0].toString(),
			"${name}^",
			"w${name}",
			"${name}v"
		)
	}
	val outTexts = { name: String ->
		arrayOf(
			"${name}-",
			name[0].toString(),
			"${name}↑",
			"w${name}",
			"${name}↓"
		)
	}

	init {
		val binding: ArrayKeyboardBinding = DataBindingUtil.bind(view)!!
		binding.bindVars = bindVars

		candidateLib.loadWords(R.raw.words)
		candidateLib.loadMapping(R.raw.mapping)
		candidateLib.loadFunKeyMap(
			mapOf(
				"enter" to KeyEvent.KEYCODE_NUMPAD_ENTER,
				"space" to KeyEvent.KEYCODE_SPACE,
				"tab" to KeyEvent.KEYCODE_TAB,
				"left" to KeyEvent.KEYCODE_DPAD_LEFT,
				"right" to KeyEvent.KEYCODE_DPAD_RIGHT,
				"shift" to KeyEvent.KEYCODE_SHIFT_LEFT
			)
		)
	}

	override fun onCharButtonTouch(motionEvent: MotionEvent, button: View, name: String): Boolean {
		when (motionEvent.action) {
			MotionEvent.ACTION_DOWN -> {
				button.animate().scaleX(0.95f).scaleY(0.95f).alpha(0.7f).setDuration(0).start()
				lastTouchX = motionEvent.x
				lastTouchY = motionEvent.y
				setPopupText(*IntRange(0, 4).map { popTexts(name)[it] }.toTypedArray())
				showPopup(button)
			}

			MotionEvent.ACTION_MOVE -> {
				val deltaX = motionEvent.x - lastTouchX
				val deltaY = motionEvent.y - lastTouchY
				getSwipeDirection(deltaX, deltaY).also { dir ->
					if (dir == SwipeDirection.NONE)
						setPopupText(*IntRange(0, 4).map { popTexts(name)[it] }.toTypedArray())
					else
						setPopupText(popTexts(name)[dir.value])
				}
				showPopup(button)
			}


			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
				popupWindow.dismiss()
				button.animate().scaleX(1.0f).scaleY(1.0f).alpha(1f).setDuration(0).start()
				val deltaX = motionEvent.x - lastTouchX
				val deltaY = motionEvent.y - lastTouchY
				lastTouchX = 0.0f
				lastTouchY = 0.0f
				getSwipeDirection(deltaX, deltaY).also {
					if (it == SwipeDirection.LEFT)
						commitText(outTexts(name)[it.value])
					else
						updateInputTextView(outTexts(name)[it.value])
				}
			}
		}
		return true
	}

	override fun onFunButtonTouch(motionEvent: MotionEvent, button: View, name: String): Boolean {
		when (motionEvent.action) {
			MotionEvent.ACTION_DOWN -> {
				lastTouchX = motionEvent.x
				lastTouchY = motionEvent.y
				button.animate().scaleX(0.95f).scaleY(0.95f).alpha(0.7f).setDuration(0).start()
				when (name) {
					"back" -> handler.postDelayed(backKeyDown, DELAY_MILLIS)
					"mode" -> {}
					else -> candidateLib.getKeyValue(name)?.let { sendDownKeyEvent(it, false) }
				}
			}

			MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
				val deltaX = motionEvent.x - lastTouchX
				val deltaY = motionEvent.y - lastTouchY
				lastTouchX = 0.0f
				lastTouchY = 0.0f
				button.animate().scaleX(1.0f).scaleY(1.0f).alpha(1f).setDuration(0).start()
				when (name) {
					"back" -> handler.removeCallbacks(backKeyDown)
					"mode" -> getSwipeDirection(deltaX, deltaY).let {
						changeMode(it.value)
					}
					else -> candidateLib.getKeyValue(name)?.let { sendUpKeyEvent(it, false) }
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
}