package com.example.missingpeople.view

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.missingpeople.R
import com.example.missingpeople.repositor.MissingPerson
import java.text.SimpleDateFormat
import java.util.Locale

class PersonDetailActivity : AppCompatActivity() {

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
        setContentView(R.layout.activity_missing_person_details)

        val person = if (intent?.hasExtra(EXTRA_PERSON) == true) {
            intent.getSerializableExtra(EXTRA_PERSON) as? MissingPerson
        } else {
            // Можно добавить обработку deep link или других способов открытия
            null
        }
        person?.let { bindPersonData(it) } ?: run {
            Toast.makeText(this, "Данные о человеке не найдены", Toast.LENGTH_SHORT).show()
            finish()
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
}