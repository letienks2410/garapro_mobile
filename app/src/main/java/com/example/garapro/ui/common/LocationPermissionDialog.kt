package com.example.garapro.ui.common

import android.app.Activity
import android.view.LayoutInflater
import android.widget.TextView
import com.example.garapro.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object LocationPermissionDialog {
    fun show(activity: Activity, onAllow: () -> Unit, onLater: (() -> Unit)? = null) {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_location_permission, null)
        val tvTitle = view.findViewById<TextView>(R.id.tvLocationTitle)
        val tvMessage = view.findViewById<TextView>(R.id.tvLocationMessage)
        tvTitle.text = activity.getString(R.string.location_permission_title)
        tvMessage.text = activity.getString(R.string.location_permission_message)
        MaterialAlertDialogBuilder(activity)
            .setView(view)
            .setNegativeButton(R.string.location_permission_later) { _, _ -> onLater?.invoke() }
            .setPositiveButton(R.string.location_permission_allow) { _, _ -> onAllow() }
            .show()
    }

    fun showDenied(activity: Activity, onOpenSettings: () -> Unit) {
        MaterialAlertDialogBuilder(activity)
            .setTitle(activity.getString(R.string.location_permission_denied_title))
            .setMessage(activity.getString(R.string.location_permission_denied_message))
            .setNegativeButton(R.string.close, null)
            .setPositiveButton(R.string.open_settings) { _, _ -> onOpenSettings() }
            .show()
    }
}
