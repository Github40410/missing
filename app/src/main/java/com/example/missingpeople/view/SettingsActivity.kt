package com.example.missingpeople.view

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.missingpeople.R
import com.example.missingpeople.databinding.ActivitySettingsBinding
import com.example.missingpeople.repositor.RussianRegion
import com.example.missingpeople.servic.AlarmParserMVD
import com.google.android.material.chip.Chip
import com.google.android.material.navigation.NavigationView
import com.google.firebase.crashlytics.buildtools.reloc.com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.util.Calendar

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sharedPref: SharedPreferences
    private val selectedRegions = mutableSetOf<RussianRegion>()
    private lateinit var navView: NavigationView
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)

        setupToolbar()
        loadSettings()
        setupThemeSelection()
        setupMonitoringSwitch()  // <-- Основные изменения здесь
        setupRegionSelection()
        setupSaveButton()

        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)


        // Обработка выбора пунктов меню
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_saved -> {
                    startActivity(Intent(this, SavedPersonsActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_settings -> {

                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> false
            }

        }
    }

    override fun onStart() {
        applySavedTheme()
        super.onStart()
    }

    private fun applySavedTheme() {
        val sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)
        val theme = sharedPreferences.getBoolean("app_theme", false) ?: false

        when (theme) {
            true -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    private fun loadSettings() {
        // Загрузка темы
        val theme = sharedPref.getBoolean("app_theme", false) ?: false
        binding.themeRadioGroup.check(
            when (theme) {
                true -> R.id.theme_dark
                else -> R.id.theme_light
            }
        )

        // Загрузка мониторинга
        binding.monitoringSwitch.isChecked = sharedPref.getBoolean("monitoring_enabled", false)

        // Загрузка регионов
        val regionsJson = sharedPref.getString("monitoring_regions", null)
        regionsJson?.let {
            val type = object : TypeToken<Set<String>>() {}.type
            val regionNames = Gson().fromJson<Set<String>>(it, type)
            selectedRegions.addAll(
                RussianRegion.values().filter { region -> regionNames.contains(region.displayName) }
            )
            updateSelectedRegionsUI()
        }
    }

    private fun setupThemeSelection() {
        binding.themeRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val theme = when (checkedId) {
                R.id.theme_dark -> true
                else -> false
            }

            // Сохраняем настройку темы
            sharedPref.edit().putBoolean("app_theme", theme).apply()

            // Меняем тему
            AppCompatDelegate.setDefaultNightMode(
                if (theme == true) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )

            recreate()
        }
    }

    private fun setupMonitoringSwitch() {
        binding.monitoringSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && selectedRegions.isEmpty()) {
                Toast.makeText(this, "Выберите хотя бы один регион", Toast.LENGTH_SHORT).show()
                binding.monitoringSwitch.isChecked = false
            } else {
                sharedPref.edit().putBoolean("monitoring_enabled", isChecked).apply()
                if (isChecked) {
                    AlarmParserMVD.setAlarm(this)
                    Toast.makeText(this, "Мониторинг включен", Toast.LENGTH_SHORT).show()
                } else {
                    AlarmParserMVD.cancelAlarm(this)
                    Toast.makeText(this, "Мониторинг выключен", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupRegionSelection() {
        binding.btnSelectRegions.setOnClickListener {
            showRegionsDialog()
        }
    }

    private fun showRegionsDialog() {
        val regions = RussianRegion.values()
        val regionNames = regions.map { it.displayName }.toTypedArray()
        val checkedItems = BooleanArray(regions.size) { i ->
            selectedRegions.contains(regions[i])
        }

        AlertDialog.Builder(this)
            .setTitle("Выберите регионы")
            .setMultiChoiceItems(regionNames, checkedItems) { _, which, isChecked ->
                val region = regions[which]
                if (isChecked) selectedRegions.add(region)
                else selectedRegions.remove(region)
            }
            .setPositiveButton("OK") { _, _ -> updateSelectedRegionsUI() }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun updateSelectedRegionsUI() {
        binding.selectedRegionsGroup.removeAllViews()
        selectedRegions.forEach { region ->
            val chip = Chip(this).apply {
                text = region.displayName
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    selectedRegions.remove(region)
                    updateSelectedRegionsUI()
                }
            }
            binding.selectedRegionsGroup.addView(chip)
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveSettings()
            Toast.makeText(this, "Настройки сохранены", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun saveSettings() {
        sharedPref.edit().apply {
            putBoolean("app_theme",
                if (binding.themeRadioGroup.checkedRadioButtonId == R.id.theme_dark) true else false
            )
            putBoolean("monitoring_enabled", binding.monitoringSwitch.isChecked)
            putString("monitoring_regions",
                Gson().toJson(selectedRegions.map { it.srcName })
            )
            apply()
        }

        setupMonitoringAlarm()
    }

    private fun setupMonitoringAlarm() {

    }
}