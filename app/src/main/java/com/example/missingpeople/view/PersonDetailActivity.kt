package com.example.missingpeople.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.example.missingpeople.R
import com.example.missingpeople.repositor.MissingPerson
import com.example.missingpeople.repositor.MissingPersonDatabase
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import java.text.SimpleDateFormat
import java.util.Locale

class PersonDetailActivity : AppCompatActivity() {

    private lateinit var database: MissingPersonDatabase
    private lateinit var currentPerson: MissingPerson
    private lateinit var navView: NavigationView
    private lateinit var drawerLayout: DrawerLayout

    companion object {
        const val EXTRA_PERSON = "extra_person"

        fun createIntent(context: Context, person: MissingPerson): Intent {
            return Intent(context, PersonDetailActivity::class.java).apply {
                putExtra(EXTRA_PERSON, person)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySavedTheme()
        setContentView(R.layout.activity_missing_person_details)

        // Инициализация базы данных
        database = MissingPersonDatabase(this)

        val person = if (intent?.hasExtra(EXTRA_PERSON) == true) {
            intent.getSerializableExtra(EXTRA_PERSON) as? MissingPerson
        } else {
            null
        }

        person?.let {
            currentPerson = it
            bindPersonData(it)
            setupSaveButton(it)
        } ?: run {
            Toast.makeText(this, "Данные о человеке не найдены", Toast.LENGTH_SHORT).show()
            finish()
        }

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
                    startActivity(Intent(this, SettingsActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> false
            }

        }
    }

    private fun applySavedTheme() {
        val sharedPreferences = getSharedPreferences("app_settings", MODE_PRIVATE)
        val theme = sharedPreferences.getBoolean("app_theme", false) ?: false

        when (theme) {
            true -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun setupSaveButton(person: MissingPerson) {
        findViewById<FloatingActionButton>(R.id.saveButton).setOnClickListener {
            // Сохраняем в базу данных
            database.addMissingPerson(person, this)
        }
    }

    private fun bindPersonData(person: MissingPerson) {
        // Центрированные элементы
        findViewById<TextView>(R.id.personName).text = person.name

        // Загрузка фото
        Glide.with(this)
            .load(person.photos)
            .placeholder(R.drawable.error_image)
            .error(R.drawable.error_image)
            .into(findViewById(R.id.personPhoto))

        // Левосторонние элементы с подписями
        findViewById<TextView>(R.id.personGender).text = person.gender.ifEmpty { "не указан" }

        findViewById<TextView>(R.id.personBirthDate).text =
            person.birthDate?.let {
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(it)
            } ?: "не указана"

        findViewById<TextView>(R.id.personDisappearanceDate).text =
            person.disappearanceDate?.let {
                SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(it)
            } ?: "не указана"

        findViewById<TextView>(R.id.personDescription).text =
            person.description.ifEmpty { "Описание отсутствует" }
    }

    override fun onDestroy() {
        database.close()
        super.onDestroy()
    }
}