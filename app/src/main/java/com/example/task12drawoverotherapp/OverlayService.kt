package com.example.task12drawoverotherapp


import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.*
import android.view.*
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingIcon: View
    private var popupView: View? = null
    private lateinit var fileAdapter: FileAdapter
    private var fileObserver: FileObserver? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showFloatingIcon()
    }

    private fun showFloatingIcon() {
        floatingIcon = LayoutInflater.from(this).inflate(R.layout.floating_icon, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 300

        windowManager.addView(floatingIcon, params)

        floatingIcon.findViewById<ImageView>(R.id.floatingIcon).setOnClickListener {
            if (popupView == null) {
                showPopupWindow()
            } else {
                removePopup()
            }
        }
    }

    private fun showPopupWindow() {
        popupView = LayoutInflater.from(this).inflate(R.layout.popup_layout, null)
        val recyclerView = popupView!!.findViewById<RecyclerView>(R.id.popupRecyclerView)
        fileAdapter = FileAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = fileAdapter

        val layoutParams = WindowManager.LayoutParams(
            600, 800,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.CENTER
        windowManager.addView(popupView, layoutParams)

        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        fileAdapter.submitList(dir.listFiles()?.toList() ?: emptyList())

        fileObserver = @RequiresApi(Build.VERSION_CODES.Q)
        object : FileObserver(dir, CREATE or DELETE or MOVED_FROM or MOVED_TO) {
            override fun onEvent(event: Int, path: String?) {
                Handler(Looper.getMainLooper()).post {
                    fileAdapter.submitList(dir.listFiles()?.toList() ?: emptyList())
                }
            }
        }
        fileObserver?.startWatching()
    }

    private fun removePopup() {
        popupView?.let { windowManager.removeView(it) }
        popupView = null
        fileObserver?.stopWatching()
    }

    override fun onDestroy() {
        try {
            windowManager.removeView(floatingIcon)
            popupView?.let { windowManager.removeView(it) }
        } catch (_: Exception) {}
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
