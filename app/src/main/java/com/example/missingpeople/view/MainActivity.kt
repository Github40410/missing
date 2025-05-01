package com.example.missingpeople.view

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Paint
import android.icu.util.Calendar
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.ConstraintSet.Constraint
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginEnd
import androidx.lifecycle.lifecycleScope
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OutOfQuotaPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.example.missingpeople.R
import com.example.missingpeople.databinding.ActivityMainBinding
import com.example.missingpeople.repositor.MissingPerson
import com.example.missingpeople.repositor.RepWebMVD
import com.example.missingpeople.repositor.RussianRegion
import com.example.missingpeople.servic.AlarmParserMVD
import com.example.missingpeople.servic.ConstructView
import com.example.missingpeople.servic.ParserMVD
import com.example.missingpeople.servic.ParserWorker
import com.example.missingpeople.servic.WorkScheduler
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.internal.ViewUtils.dpToPx
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit



class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val repositMVD: RepWebMVD = RepWebMVD()
    private val parserMVD: ParserMVD = ParserMVD()
    private lateinit var linearLayoutPeople: LinearLayout // Используем lateinit

    private val selectedRegions = mutableSetOf<RussianRegion>(RussianRegion.ALL)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        setupRegionSelection()
        setupFilterButton()
        setupSearchButton()
        updateSelectedRegionsUI()
    }

    private fun setupRegionSelection() {
        binding.btnSelectRegions.setOnClickListener {
            showRegionSelectionDialog()
        }
    }

    private fun showRegionSelectionDialog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Выберите регионы")
            .setMultiChoiceItems(
                getRegionNames(), // Массив названий регионов
                getSelectedBooleanArray() // Массив выбранных регионов
            ) { _, which, isChecked ->
                handleRegionSelection(which, isChecked)
            }
            .setPositiveButton("Готово") { _, _ ->
                updateSelectedRegionsUI()
                applyFilters()
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialog.show()
    }

    private fun getRegionNames(): Array<String> {
        return (listOf(RussianRegion.ALL) + RussianRegion.getSortedRegions())
            .map { it.displayName }
            .toTypedArray()
    }

    private fun getSelectedBooleanArray(): BooleanArray {
        val allRegions = listOf(RussianRegion.ALL) + RussianRegion.getSortedRegions()
        return BooleanArray(allRegions.size) { index ->
            selectedRegions.contains(allRegions[index])
        }
    }

    private fun handleRegionSelection(position: Int, isChecked: Boolean) {
        val region = when (position) {
            0 -> RussianRegion.ALL
            else -> RussianRegion.getSortedRegions()[position - 1]
        }

        if (isChecked) {
            if (region == RussianRegion.ALL) {
                selectedRegions.clear()
                selectedRegions.add(region)
            } else {
                selectedRegions.remove(RussianRegion.ALL)
                selectedRegions.add(region)
            }
        } else {
            selectedRegions.remove(region)
        }
    }

    private fun updateSelectedRegionsUI() {
        binding.selectedRegionsGroup.removeAllViews()

        if (selectedRegions.contains(RussianRegion.ALL)) {
            addRegionChip(RussianRegion.ALL)
        } else {
            selectedRegions.sortedBy { it.displayName }.forEach { region ->
                addRegionChip(region)
            }
        }
    }

    private fun addRegionChip(region: RussianRegion) {
        val chip = Chip(this).apply {
            text = region.displayName
            isCloseIconVisible = true
            setOnCloseIconClickListener {
                selectedRegions.remove(region)
                updateSelectedRegionsUI()
                applyFilters()
            }
            setChipBackgroundColorResource(R.color.chipBackground)
        }
        binding.selectedRegionsGroup.addView(chip)
    }

    private fun applyFilters() {
        // Здесь объединяем фильтрацию по регионам и другим параметрам
        val filterText = buildString {
            append("Применены фильтры: ")
            if (selectedRegions.contains(RussianRegion.ALL)) {
                append("Все регионы")
            } else {
                append("Регионы: ${selectedRegions.joinToString { it.displayName }}")
            }
            // Можно добавить другие параметры фильтрации
        }
        Toast.makeText(this, filterText, Toast.LENGTH_LONG).show()

        // Реальная фильтрация данных
        filterData()
    }

    private fun filterData() {
        // Реализуйте фактическую фильтрацию данных здесь
    }

    private fun setupFilterButton() {
        binding.btnFilter.setOnClickListener {
            val isVisible = binding.extraFiltersPanel.visibility == View.VISIBLE
            binding.extraFiltersPanel.visibility = if (isVisible) View.GONE else View.VISIBLE
        }
    }

    private fun setupSearchButton() {
        binding.btnSearch.setOnClickListener {
            applyFilters()
            Toast.makeText(this, "Применены фильтры и выполнен поиск", Toast.LENGTH_SHORT).show()
        }
    }









    /*
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(R.layout.activity_main)


        // Добавляем обработчик для "Всей России"
        binding.chipAllRussia.setOnCheckedChangeListener { _, isChecked ->
            handleRegionSelection(RussianRegion.ALL, isChecked)
        }

        // Инициализация списка регионов
        addRegionsToChipGroup(this, binding.regionsGroup)

        // Обработчик кнопки фильтра
        binding.btnFilter.setOnClickListener {
            binding.filterPanel.visibility =
                if (binding.filterPanel.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }

        // Настройка системных отступов
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        checkExactAlarmPermission()

        // Запуск периодической работы
        AlarmParserMVD.setAlarm(this)

        val urlMVD = repositMVD.getUrlMVD()

        lifecycleScope.launch(Dispatchers.IO){
            // Запускаем в IO для сетевых/тяжелых операций
            val constructView = ConstructView(findViewById(R.id.linearLayoutAllPeople))
            parserMVD.constructView = constructView
            val people = parserMVD.parserPersonMissing(parserMVD.collectUniqueLinks(parserMVD.extractAllPageUrls(urlMVD)), this@MainActivity)

            // Обновляем UI в главном потоке
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivity,
                    "Найдено ссылок: ${people.size}",
                    Toast.LENGTH_SHORT
                ).show()

                showNotification(people.get(0))
            }
        }
    }

    private fun showNotification(person: MissingPerson) {
        NotificationPeopleMissing(this).showNotification(person)
    }


    private fun checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                // Открываем настройки, если разрешения нет
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }
    }


    private fun addRegionsToChipGroup(context: Context, chipGroup: ChipGroup) {
        // Очищаем существующие чипы (кроме "Вся Россия")
        for (i in 0 until chipGroup.childCount) {
            val chip = chipGroup.getChildAt(i) as? Chip
            if (chip?.id != R.id.chipAllRussia) {
                chipGroup.removeViewAt(i)
            }
        }

        // Добавляем все регионы из enum
        RussianRegion.getAllRegions().forEach { region ->
            // Пропускаем "Вся Россия", так как она уже есть в макете
            if (region != RussianRegion.ALL) {
                val chip = Chip(context).apply {
                    text = region.displayName
                    isCheckable = true
                    isChecked = false
                    setEnsureMinTouchTargetSize(false)
                    setChipBackgroundColorResource(R.color.primary)

                    // Обработка выбора/снятия выбора региона
                    setOnCheckedChangeListener { _, isChecked ->
                        handleRegionSelection(region, isChecked)
                    }
                }
                chipGroup.addView(chip)
            }
        }
    }

    // 3. Обработчик выбора регионов
    private fun handleRegionSelection(region: RussianRegion, isChecked: Boolean) {
        val allRussiaChip = binding.regionChipGroup.findViewById<Chip>(R.id.chipAllRussia)

        when {
            region == RussianRegion.ALL -> {
                // Если выбрали "Всю Россию", снимаем выбор с других регионов
                if (isChecked) {
                    clearOtherRegionsSelection()
                }
            }
            isChecked -> {
                // Если выбрали конкретный регион, снимаем "Всю Россию"
                allRussiaChip?.isChecked = false
            }
        }

        updateSelectedRegions()
    }

    // 4. Метод для сброса выбора других регионов
    private fun clearOtherRegionsSelection() {
        for (i in 0 until binding.regionsGroup.childCount) {
            val chip = binding.regionsGroup.getChildAt(i) as? Chip
            chip?.isChecked = false
        }
    }

    // 5. Обновление списка выбранных регионов
    private fun updateSelectedRegions() {
        val selectedRegions = mutableListOf<String>()
        val allRussiaChip = binding.regionChipGroup.findViewById<Chip>(R.id.chipAllRussia)

        if (allRussiaChip?.isChecked == true) {
            selectedRegions.add(allRussiaChip.text.toString())
        } else {
            for (i in 0 until binding.regionsGroup.childCount) {
                val chip = binding.regionsGroup.getChildAt(i) as? Chip
                if (chip?.isChecked == true) {
                    selectedRegions.add(chip.text.toString())
                }
            }
        }

        // Здесь можно использовать selectedRegions для фильтрации
        Log.d("RegionFilter", "Выбрано: ${selectedRegions.joinToString()}")
    }
     */
}