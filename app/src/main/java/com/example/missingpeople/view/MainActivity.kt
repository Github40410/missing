package com.example.missingpeople.view

import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.missingpeople.R
import com.example.missingpeople.repositor.RepWebMVD
import com.example.missingpeople.servic.ParserMVD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Semaphore


class MainActivity : AppCompatActivity() {

    private val repositMVD: RepWebMVD = RepWebMVD()
    private val parserMVD: ParserMVD = ParserMVD()
    private lateinit var linearLayoutPeople: LinearLayout // Используем lateinit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Настройка системных отступов
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val urlMVD = repositMVD.getUrlMVD()

        val constraintLayoutListPeople = findViewById<ConstraintLayout>(R.id.main)

        // Инициализация LinearLayout
        linearLayoutPeople = LinearLayout(this).apply {
            layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topToTop = ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            }
            orientation = LinearLayout.VERTICAL
            setPadding(32.dpToPx(), 32.dpToPx(), 32.dpToPx(), 32.dpToPx()) // Утилита для конвертации dp
            id = View.generateViewId()
        }
        constraintLayoutListPeople.addView(linearLayoutPeople)

        lifecycleScope.launch(Dispatchers.IO){
            // Запускаем в IO для сетевых/тяжелых операций
            val people = parserMVD.parserPersonMissing(parserMVD.collectUniqueLinks(parserMVD.extractAllPageUrls(urlMVD)), this@MainActivity)



            // Обновляем UI в главном потоке
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivity,
                    "Найдено ссылок: ${people.size}",
                    Toast.LENGTH_SHORT
                ).show()
                people.forEach { element ->
                    val textView = TextView(this@MainActivity).apply {
                        text = element.description
                        textSize = 15f
                        setTextIsSelectable(true)
                        setTextColor(
                            ContextCompat.getColor(
                                context,
                                android.R.color.holo_blue_dark
                            )
                        )
                        paintFlags = paintFlags or Paint.UNDERLINE_TEXT_FLAG
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(0, 16.dpToPx(), 0, 16.dpToPx())
                        }
                    }
                    linearLayoutPeople.addView(textView)
                }
            }
        }
    }

    // Утилита для конвертации dp в пиксели
    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()
}