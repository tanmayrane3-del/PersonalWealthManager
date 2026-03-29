package com.example.personalwealthmanager.core.utils

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.Animatable
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.example.personalwealthmanager.R

object AppLoader {

    private var dialog: Dialog? = null

    fun show(context: Context, message: String? = null) {
        if (dialog?.isShowing == true) return

        dialog = Dialog(context, R.style.LoaderDialogTheme).apply {
            setContentView(R.layout.layout_custom_loader)
            setCancelable(false)
            window?.apply {
                setBackgroundDrawableResource(android.R.color.transparent)
                setLayout(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT
                )
            }

            val ivLoader = findViewById<ImageView>(R.id.ivLoader)
            val tvMessage = findViewById<TextView>(R.id.tvLoaderMessage)

            (ivLoader.drawable as? Animatable)?.start()

            if (!message.isNullOrEmpty()) {
                tvMessage.text = message
                tvMessage.visibility = View.VISIBLE
            }
        }
        dialog?.show()
    }

    fun hide() {
        dialog?.dismiss()
        dialog = null
    }

    fun updateMessage(message: String) {
        dialog?.findViewById<TextView>(R.id.tvLoaderMessage)?.apply {
            text = message
            visibility = View.VISIBLE
        }
    }

    val isShowing: Boolean get() = dialog?.isShowing == true
}
