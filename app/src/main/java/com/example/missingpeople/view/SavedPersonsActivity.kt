package com.example.missingpeople.view

import android.app.DatePickerDialog
import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.missingpeople.R
import com.example.missingpeople.repositor.MissingPerson
import com.example.missingpeople.servic.ConstructView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar
import java.util.Locale
import java.util.Date


class SavedPersonsActivity : AppCompatActivity() {
    private lateinit var searchInput: TextInputEditText
    private var allPersons = mutableListOf<MissingPerson>()
    private var filteredPersons = mutableListOf<MissingPerson>()
    private val construct by lazy { ConstructView(findViewById(R.id.personsContainer)) } // Изменено на lazy
    private var currentGenderFilter: String? = null
    private var currentDateFilter: Pair<Date?, Date?>? = null
    private lateinit var navView: NavigationView
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_persons)

        searchInput = findViewById(R.id.searchInput)

        // Убрали явную инициализацию construct здесь

        setupSearchButton()
        setupFilterButton()
        loadPersonsToArray()

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

                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> false
            }

        }

    }

    private fun setupSearchButton() {
        findViewById<ImageButton>(R.id.btnSearch).setOnClickListener {
            applyFilters()
        }
    }

    private fun setupFilterButton() {
        // Оставили только кнопку фильтра без чипсов
        findViewById<MaterialButton>(R.id.btnFilter).setOnClickListener {
            showFilterDialog()
        }
    }

    private fun showFilterDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_filters, null)

        // Настройка RadioGroup
        val genderFilterGroup = dialogView.findViewById<RadioGroup>(R.id.genderFilterGroup)
        when (currentGenderFilter) {
            "Мужской" -> genderFilterGroup.check(R.id.maleGender)
            "Женский" -> genderFilterGroup.check(R.id.femaleGender)
            else -> genderFilterGroup.check(R.id.allGenders)
        }

        // Настройка кнопок выбора даты
        val btnDateFrom = dialogView.findViewById<MaterialButton>(R.id.btnDateFrom)
        val btnDateTo = dialogView.findViewById<MaterialButton>(R.id.btnDateTo)

        currentDateFilter?.let { (from, to) ->
            from?.let { btnDateFrom.text = formatDate(it) }
            to?.let { btnDateTo.text = formatDate(it) }
        }

        // Обработчики выбора даты
        btnDateFrom.setOnClickListener {
            showDatePickerDialog { date ->
                btnDateFrom.text = formatDate(date)
            }
        }

        btnDateTo.setOnClickListener {
            showDatePickerDialog { date ->
                btnDateTo.text = formatDate(date)
            }
        }

        // Создание и отображение диалога
        MaterialAlertDialogBuilder(this)
            .setTitle("Фильтры")
            .setView(dialogView)
            .setPositiveButton("Применить") { _, _ ->
                // Применяем выбранные фильтры
                currentGenderFilter = when (genderFilterGroup.checkedRadioButtonId) {
                    R.id.maleGender -> "Мужской"
                    R.id.femaleGender -> "Женский"
                    else -> null
                }

                val dateFrom = if (btnDateFrom.text.toString() != "От") {
                    parseDate(btnDateFrom.text.toString())
                } else null

                val dateTo = if (btnDateTo.text.toString() != "До") {
                    parseDate(btnDateTo.text.toString())
                } else null

                currentDateFilter = Pair(dateFrom, dateTo)
                applyFilters()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showDatePickerDialog(onDateSelected: (Date) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                onDateSelected(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadPersonsToArray() {
        allPersons.clear() // Очищаем массив перед заполнением
        findViewById<ProgressBar>(R.id.progressBar).visibility = View.VISIBLE

        Thread {
            try {
                // Получаем базу данных для чтения
                val db = openOrCreateDatabase(DATABASE_NAME, MODE_PRIVATE, null)

                // Выполняем запрос
                val cursor = db.rawQuery("SELECT * FROM ${TABLE_NAME}", null)

                // Обрабатываем результаты
                cursor.use {
                    while (it.moveToNext()) {
                        val person = MissingPerson(
                            name = it.getString(it.getColumnIndexOrThrow(COLUMN_NAME)),
                            description = it.getString(it.getColumnIndexOrThrow(COLUMN_DESCRIPTION)),
                            birthDate = if (!it.isNull(it.getColumnIndexOrThrow(COLUMN_BIRTH_DATE))) {
                                Date(it.getLong(it.getColumnIndexOrThrow(COLUMN_BIRTH_DATE)))
                            } else null,
                            disappearanceDate = if (!it.isNull(it.getColumnIndexOrThrow(COLUMN_DISAPPEARANCE_DATE))) {
                                Date(it.getLong(it.getColumnIndexOrThrow(COLUMN_DISAPPEARANCE_DATE)))
                            } else null,
                            gender = it.getString(it.getColumnIndexOrThrow(COLUMN_GENDER)),
                            photos = it.getString(it.getColumnIndexOrThrow(COLUMN_PHOTO_URL)),
                            url = ""
                        )
                        allPersons.add(person)
                    }
                }

                db.close()

                runOnUiThread {
                    findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                    applyFilters() // Применяем фильтры после загрузки
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    findViewById<ProgressBar>(R.id.progressBar).visibility = View.GONE
                    Toast.makeText(this, "Ошибка загрузки: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun applyFilters() {
        // 1. Фильтрация по текстовому поиску
        val searchText = searchInput.text.toString().lowercase()

        filteredPersons = if (searchText.isBlank()) {
            allPersons.toMutableList()
        } else {
            allPersons.filter {
                it.name.lowercase().contains(searchText) ||
                        it.description?.lowercase()?.contains(searchText) == true
            }.toMutableList()
        }

        // 2. Фильтрация по полу
        currentGenderFilter?.let { gender ->
            filteredPersons = filteredPersons.filter { it.gender == gender }.toMutableList()
        }

        // 3. Фильтрация по дате исчезновения (если реализовано)
        currentDateFilter?.let { (fromDate, toDate) ->
            filteredPersons = filteredPersons.filter { person ->
                person.disappearanceDate?.let { date ->
                    (fromDate == null || date.after(fromDate)) &&
                            (toDate == null || date.before(toDate))
                } ?: false
            }.toMutableList()
        }

        // Обновляем UI
        updatePersonsList()
    }

    private fun updatePersonsList() {
        val container = findViewById<LinearLayout>(R.id.personsContainer)
        container.removeAllViews()

        val darkTheme = getSharedPreferences("app_settings", MODE_PRIVATE)
            .getBoolean("app_theme", false)

        if (filteredPersons.isEmpty()) {
            findViewById<LinearLayout>(R.id.emptyState).visibility = View.VISIBLE
        } else {
            findViewById<LinearLayout>(R.id.emptyState).visibility = View.GONE
            filteredPersons.forEach { person ->
                construct.createDynamicImageTextItem(this, container, person, darkTheme)
            }
        }
    }

    private fun formatDate(date: Date): String {
        return android.text.format.DateFormat.format("dd.MM.yyyy", date).toString()
    }

    private fun parseDate(dateString: String): Date? {
        return try {
            val parts = dateString.split(".")
            if (parts.size == 3) {
                val day = parts[0].toInt()
                val month = parts[1].toInt() - 1 // Месяцы в Calendar начинаются с 0
                val year = parts[2].toInt()

                val calendar = Calendar.getInstance()
                calendar.set(year, month, day)
                calendar.time
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val DATABASE_NAME = "missing_persons.db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "missing_persons"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_BIRTH_DATE = "birth_date"
        private const val COLUMN_DISAPPEARANCE_DATE = "disappearance_date"
        private const val COLUMN_GENDER = "gender"
        private const val COLUMN_PHOTO_URL = "photo_url"
    }
}