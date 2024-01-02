package com.example.testime

import android.widget.Button
import android.widget.FrameLayout
import android.widget.TableLayout
import androidx.constraintlayout.widget.ConstraintLayout

open class BaseKeyboard(baseView: FrameLayout, id: Int) {
    var view: ConstraintLayout
    var tableLayout: TableLayout
    init {
        view = baseView.findViewById(id)
        tableLayout = view.findViewById(R.id.tableLayout)
    }


}