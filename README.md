# 🎮 MC Tracker — Minecraft Screen Time Companion

Auto-tracks Minecraft usage on Android and syncs to your Supabase/LOG dashboard.

## How to Run

### 1. Open in Android Studio
- Open Android Studio
- File → Open → select this `MCTracker` folder
- Wait for Gradle sync to finish (2-3 min first time)

### 2. Edit your Supabase details
Open `app/src/main/java/com/screentime/mctracker/MinecraftTracker.kt`

Change these 3 lines:
```kotlin
private const val SUPABASE_URL = "https://YOUR_PROJECT.supabase.co"
private const val SUPABASE_KEY = "YOUR_ANON_KEY"
private const val USER_KEY = "rahas"
```

### 3. Create the Supabase table
Run this SQL in your Supabase SQL Editor:
```sql
CREATE TABLE IF NOT EXISTS minecraft_usage (
  id SERIAL PRIMARY KEY,
  user_key TEXT NOT NULL,
  date TEXT NOT NULL,
  minutes_played INTEGER NOT NULL DEFAULT 0,
  last_synced TIMESTAMPTZ DEFAULT NOW(),
  UNIQUE(user_key, date)
);
ALTER TABLE minecraft_usage ENABLE ROW LEVEL SECURITY;
CREATE POLICY "open_minecraft_usage" ON minecraft_usage
  FOR ALL USING (true) WITH CHECK (true);
```

### 4. Run
- Plug in your Android phone via USB (enable USB debugging first)
- Or use an emulator
- Click ▶ Run

### 5. Grant permission
- App will show a warning about Usage Access
- Tap "Grant Usage Access Permission"
- Find "MC Tracker" in the list → toggle ON
- Go back to the app

Done! Minecraft usage syncs every 15 minutes.
