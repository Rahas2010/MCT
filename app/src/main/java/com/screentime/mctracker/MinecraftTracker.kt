package com.screentime.mctracker

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

object MinecraftTracker {

    // ╔═══════════════════════════════════════════════╗
    // ║  EDIT THESE 3 VALUES WITH YOUR OWN DETAILS    ║
    // ╚═══════════════════════════════════════════════╝
    private const val SUPABASE_URL = "https://YOUR_PROJECT.supabase.co"
    private const val SUPABASE_KEY = "YOUR_ANON_KEY"
    private const val USER_KEY = "rahas"

    private const val MINECRAFT_PACKAGE = "com.mojang.minecraftpe"

    fun getMinecraftMinutesToday(context: Context): Int {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return -1

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            cal.timeInMillis,
            System.currentTimeMillis()
        )

        val mcStats = stats?.find { it.packageName == MINECRAFT_PACKAGE } ?: return 0
        return (mcStats.totalTimeInForeground / 60_000).toInt()
    }

    fun getMinecraftWeekly(context: Context): Map<String, Int> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()

        val result = mutableMapOf<String, Int>()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        for (daysAgo in 6 downTo 0) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -daysAgo)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startTime = cal.timeInMillis
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val endTime = cal.timeInMillis
            val dateKey = dateFormat.format(Date(startTime))

            val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
            val mcStats = stats?.find { it.packageName == MINECRAFT_PACKAGE }
            result[dateKey] = ((mcStats?.totalTimeInForeground ?: 0) / 60_000).toInt()
        }

        return result
    }

    fun syncToSupabase(context: Context): Boolean {
        val minutes = getMinecraftMinutesToday(context)
        if (minutes < 0) return false

        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())

        return try {
            val url = URL("$SUPABASE_URL/rest/v1/minecraft_usage")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("apikey", SUPABASE_KEY)
            conn.setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
            conn.setRequestProperty("Prefer", "resolution=merge-duplicates")
            conn.doOutput = true

            val body = """{"user_key":"$USER_KEY","date":"$dateKey","minutes_played":$minutes,"last_synced":"$now"}"""
            conn.outputStream.use { it.write(body.toByteArray()) }

            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun syncWeekToSupabase(context: Context): Boolean {
        val weekly = getMinecraftWeekly(context)
        if (weekly.isEmpty()) return false

        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())

        return try {
            val url = URL("$SUPABASE_URL/rest/v1/minecraft_usage")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("apikey", SUPABASE_KEY)
            conn.setRequestProperty("Authorization", "Bearer $SUPABASE_KEY")
            conn.setRequestProperty("Prefer", "resolution=merge-duplicates")
            conn.doOutput = true

            val entries = weekly.map { (date, mins) ->
                """{"user_key":"$USER_KEY","date":"$date","minutes_played":$mins,"last_synced":"$now"}"""
            }
            conn.outputStream.use { it.write("[${entries.joinToString(",")}]".toByteArray()) }

            val code = conn.responseCode
            conn.disconnect()
            code in 200..299
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun hasUsagePermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
