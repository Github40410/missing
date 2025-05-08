package com.example.missingpeople.servic

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.text.TextUtils
import android.util.TypedValue
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import com.bumptech.glide.Glide
import com.example.missingpeople.R
import com.example.missingpeople.repositor.MissingPerson
import com.example.missingpeople.view.PersonDetailActivity

class ConstructView(val linearLayout: LinearLayout) {
    fun createDynamicImageTextItem(
        context: Context,
        parent: LinearLayout,
        missingPerson: MissingPerson,
        them: Boolean

    ) {

        // Создаем контейнер
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dpToPx(context, 16f), dpToPx(context, 16f), dpToPx(context, 16f), dpToPx(context, 16f))

            // Добавляем Ripple-эффект
            val outValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            setBackgroundResource(outValue.resourceId)
        }

        // Создаем ImageView
        val imageView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(context, 80f),
                dpToPx(context, 80f)
            ).apply {
                marginEnd = dpToPx(context, 16f)
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }


        // Создаем TextView
        val textView = TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                weight = 1f
            }
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            if(them){
                setTextColor(context.resources.getColor(android.R.color.white, null))
            }
            else setTextColor(context.resources.getColor(android.R.color.black, null))
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
        }

        // Добавляем элементы в контейнер
        container.addView(imageView)
        container.addView(textView)

        // Загружаем изображение Glide
        Glide.with(context)
            .load(missingPerson.photos)
            .placeholder(R.drawable.error_image)
            .error(R.drawable.error_image)
            .override(300, 300)
            .centerCrop()
            .into(imageView)

        // Устанавливаем текст
        textView.text = missingPerson.name

        container.setOnClickListener{
            val intentPerson = Intent(context, PersonDetailActivity::class.java).apply {
                putExtra(PersonDetailActivity.EXTRA_PERSON, missingPerson)
            }

            context.startActivity(intentPerson)

        }


        // Добавляем контейнер в родительский лайаут
        parent.addView(container)

    }

    fun dpToPx(context: Context, dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            context.resources.displayMetrics
        ).toInt()
    }
}