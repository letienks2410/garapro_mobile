package com.example.garapro.ui.common

import android.content.Context
import android.view.LayoutInflater
import android.widget.TextView
import com.example.garapro.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object SuccessDialog {
    fun show(context: Context, title: String = "Success!", message: String, onOk: (() -> Unit)? = null) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_success, null)
        val tvTitle = view.findViewById<TextView>(R.id.tvSuccessTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvSuccessMessage)
        tvTitle.text = title
        tvMessage.text = message
        MaterialAlertDialogBuilder(context)
            .setView(view)
            .setPositiveButton("OK") { _, _ -> onOk?.invoke() }
            .show()
    }
}
