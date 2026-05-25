package com.screentime.mctracker

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var minutesText: TextView
    private lateinit var weeklyText: TextView
    private lateinit var syncButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scroll = ScrollView(this)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 80, 48, 48)
        }

        // Title
        layout.addView(TextView(this).apply {
            text = "🎮 Minecraft Tracker"
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
        })

        // Subtitle
        layout.addView(TextView(this).apply {
            text = "Tracks Minecraft usage → syncs to your Screen Time dashboard"
            textSize = 14f
            setTextColor(Color.parseColor("#666666"))
            setPadding(0, 8, 0, 40)
        })

        // Status
        statusText = TextView(this).apply {
            text = "Checking..."
            textSize = 14f
            setPadding(0, 0, 0, 32)
        }
        layout.addView(statusText)

        // Big number
        minutesText = TextView(this).apply {
            text = "—"
            textSize = 64f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        layout.addView(minutesText)

        layout.addView(TextView(this).apply {
            text = "minutes of Minecraft today"
            textSize = 16f
            setTextColor(Color.parseColor("#999999"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        })

        // Weekly
        weeklyText = TextView(this).apply {
            textSize = 13f
            setTextColor(Color.parseColor("#444444"))
            typeface = Typeface.MONOSPACE
            setPadding(0, 0, 0, 40)
        }
        layout.addView(weeklyText)

        // Sync button
        syncButton = Button(this).apply {
            text = "🔄  Sync Now"
            textSize = 16f
            setPadding(0, 24, 0, 24)
            setOnClickListener { doSync() }
        }
        layout.addView(syncButton)

        // Spacer
        layout.addView(Space(this).apply {
            minimumHeight = 16
        })

        // Permission button
        layout.addView(Button(this).apply {
            text = "⚙️  Grant Usage Access Permission"
            textSize = 14f
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        })

        scroll.addView(layout)
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        if (!MinecraftTracker.hasUsagePermission(this)) {
            statusText.text = "⚠️  Usage Access not granted!\nTap the button below → find MC Tracker → enable it"
            statusText.setTextColor(Color.parseColor("#E53935"))
            minutesText.text = "—"
            weeklyText.text = ""
            return
        }

        statusText.text = "✅  Permission granted — tracking active"
        statusText.setTextColor(Color.parseColor("#43A047"))

        val today = MinecraftTracker.getMinecraftMinutesToday(this)
        minutesText.text = "$today"
        minutesText.setTextColor(if (today > 60) Color.parseColor("#E53935") else Color.parseColor("#1a1a1a"))

        val weekly = MinecraftTracker.getMinecraftWeekly(this)
        val sb = StringBuilder("━━━ Last 7 days ━━━\n\n")
        weekly.forEach { (date, mins) ->
            val bar = "█".repeat((mins / 5).coerceAtMost(20))
            val pad = String.format("%3d", mins)
            sb.appendLine("$date   $pad min  $bar")
        }
        weeklyText.text = sb.toString()

        // Start background sync
        SyncWorker.schedule(this)
    }

    private fun doSync() {
        syncButton.isEnabled = false
        syncButton.text = "⏳  Syncing..."

        CoroutineScope(Dispatchers.IO).launch {
            val ok = MinecraftTracker.syncWeekToSupabase(this@MainActivity)
            withContext(Dispatchers.Main) {
                syncButton.isEnabled = true
                syncButton.text = "🔄  Sync Now"
                Toast.makeText(
                    this@MainActivity,
                    if (ok) "✅ Synced to cloud!" else "❌ Sync failed — check URL/key",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
