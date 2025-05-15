package com.example.missingpeople.repositor

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.widget.Toast
import java.sql.Date

class MissingPersonDatabase(context: Context):SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
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

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_DESCRIPTION TEXT,
                $COLUMN_BIRTH_DATE TEXT,
                $COLUMN_DISAPPEARANCE_DATE TEXT,
                $COLUMN_GENDER TEXT,
                $COLUMN_PHOTO_URL TEXT,
                UNIQUE($COLUMN_NAME, $COLUMN_BIRTH_DATE) ON CONFLICT REPLACE
            )
        """.trimIndent()
        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun addMissingPerson(person: MissingPerson, context: Context): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_NAME, person.name)
            put(COLUMN_DESCRIPTION, person.description)
            put(COLUMN_BIRTH_DATE, person.birthDate?.time)
            put(COLUMN_DISAPPEARANCE_DATE, person.disappearanceDate?.time)
            put(COLUMN_GENDER, person.gender)
            put(COLUMN_PHOTO_URL, person.photos)
        }

        return try {
            val result = db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_IGNORE)
            if (result == -1L) {
                // Запись уже существует
                Toast.makeText(context, "Запись уже присутствует", Toast.LENGTH_SHORT).show()
                false
            } else {
                // Запись добавлена
                Toast.makeText(context, "Запись добавлена", Toast.LENGTH_SHORT).show()
                true
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка при сохранении: ${e.message}", Toast.LENGTH_SHORT).show()
            false
        } finally {
            db.close()
        }
    }

    fun getAllMissingPersons(): List<MissingPerson> {
        val db = readableDatabase
        val personList = mutableListOf<MissingPerson>()

        val cursor = db.query(
            TABLE_NAME,
            null, // все столбцы
            null, // без условий WHERE
            null, // без аргументов для WHERE
            null, // без GROUP BY
            null, // без HAVING
            null  // без ORDER BY
        )

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(COLUMN_ID))
                val name = it.getString(it.getColumnIndexOrThrow(COLUMN_NAME))
                val description = it.getString(it.getColumnIndexOrThrow(COLUMN_DESCRIPTION))

                val birthDateMillis = it.getLong(it.getColumnIndexOrThrow(COLUMN_BIRTH_DATE))
                val birthDate = if (!it.isNull(it.getColumnIndexOrThrow(COLUMN_BIRTH_DATE))) {
                    Date(birthDateMillis)
                } else null

                val disappearanceDateMillis = it.getLong(it.getColumnIndexOrThrow(COLUMN_DISAPPEARANCE_DATE))
                val disappearanceDate = if (!it.isNull(it.getColumnIndexOrThrow(COLUMN_DISAPPEARANCE_DATE))) {
                    Date(disappearanceDateMillis)
                } else null

                val gender = it.getString(it.getColumnIndexOrThrow(COLUMN_GENDER))
                val photoUrl = it.getString(it.getColumnIndexOrThrow(COLUMN_PHOTO_URL))

                personList.add(
                    MissingPerson(
                        name,
                        description,
                        birthDate,
                        disappearanceDate,
                        gender,
                        photoUrl
                    )
                )
            }
        }

        db.close()
        return personList
    }

}