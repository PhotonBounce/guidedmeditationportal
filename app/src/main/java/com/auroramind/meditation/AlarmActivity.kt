package com.auroramind.meditation

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.auroramind.meditation.databinding.ActivityAlarmBinding

/**
 * Meditation Alarm — schedule a daily wake-up that plays either a guided
 * meditation track or one of five gentle synthesized tones.
 */
class AlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmBinding
    private lateinit var prefs: PrefsManager

    private val tracks = SoundType.values().toList()
    private val tones = SynthTone.values().toList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            // The app is always dark — force light bar icons regardless of the
            // device theme (default auto() would draw dark icons on our dark UI)
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        binding = ActivityAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)
        prefs = PrefsManager(this)

        // Edge-to-edge: keep scroll content clear of the status / nav bars
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = bars.top, bottom = bars.bottom)
            insets
        }

        supportActionBar?.title = "Alarm"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.btnAlarmBack.setOnClickListener { finish() }

        loadSavedState()

        binding.radioSourceType.setOnCheckedChangeListener { _, _ -> populateSpinner() }
        binding.btnSaveAlarm.setOnClickListener { saveAlarm() }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadSavedState() {
        binding.switchAlarmEnabled.isChecked = prefs.isAlarmEnabled()
        binding.timePicker.hour = prefs.getAlarmHour()
        binding.timePicker.minute = prefs.getAlarmMinute()

        if (prefs.getAlarmSourceType() == "TRACK") {
            binding.radioTrack.isChecked = true
        } else {
            binding.radioTone.isChecked = true
        }
        populateSpinner()
        updateStatus()
    }

    private fun populateSpinner() {
        val isTrack = binding.radioTrack.isChecked
        val labels = if (isTrack) {
            tracks.map { "${it.emoji} ${it.displayName}" }
        } else {
            tones.map { "${it.emoji} ${it.displayName}" }
        }
        binding.spinnerSound.adapter =
            ArrayAdapter(this, R.layout.item_spinner_dark, labels).apply {
                setDropDownViewResource(R.layout.item_spinner_dark)
            }

        val selectedIndex = if (isTrack) {
            tracks.indexOf(prefs.getAlarmTrack()).coerceAtLeast(0)
        } else {
            tones.indexOf(prefs.getAlarmTone()).coerceAtLeast(0)
        }
        binding.spinnerSound.setSelection(selectedIndex)
    }

    private fun saveAlarm() {
        val enabled = binding.switchAlarmEnabled.isChecked
        val hour = binding.timePicker.hour
        val minute = binding.timePicker.minute
        val isTrack = binding.radioTrack.isChecked
        val sourceType = if (isTrack) "TRACK" else "TONE"

        prefs.setAlarmEnabled(enabled)
        prefs.setAlarmTime(hour, minute)
        prefs.setAlarmSourceType(sourceType)
        if (isTrack) {
            prefs.setAlarmTrack(tracks[binding.spinnerSound.selectedItemPosition])
        } else {
            prefs.setAlarmTone(tones[binding.spinnerSound.selectedItemPosition])
        }

        if (enabled && !canScheduleExact()) {
            requestExactAlarmPermission()
            return
        }

        AlarmScheduler.reschedule(this)
        updateStatus()
        val timeStr = "%02d:%02d".format(hour, minute)
        val msg = if (enabled) "✅ Alarm saved — rings at $timeStr" else "Alarm disabled"
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun canScheduleExact(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
            )
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        binding.alarmStatus.text = if (prefs.isAlarmEnabled()) {
            val h = prefs.getAlarmHour()
            val m = prefs.getAlarmMinute()
            "🌙 Alarm set for %02d:%02d daily".format(h, m)
        } else {
            "Alarm is off"
        }
    }
}
