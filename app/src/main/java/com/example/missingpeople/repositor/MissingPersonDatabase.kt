package com.example.missingpeople.repositor

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import android.widget.Toast
import java.sql.Date

class MissingPersonDatabase(context: Context):SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    companion object {
        private const val DATABASE_NAME = "missing_persons.db"
        private const val DATABASE_VERSION = 2
        private const val TABLE_NAME = "missing_persons"
        private const val TABLE_URLS = "missing_persons_urls"
        private const val COLUMN_ID = "id"
        private const val COLUMN_NAME = "name"
        private const val COLUMN_DESCRIPTION = "description"
        private const val COLUMN_BIRTH_DATE = "birth_date"
        private const val COLUMN_DISAPPEARANCE_DATE = "disappearance_date"
        private const val COLUMN_GENDER = "gender"
        private const val COLUMN_PHOTO_URL = "photo_url"
        private const val COLUMN_URL = "url"
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

        val createUrlsTableQuery = """
            CREATE TABLE $TABLE_URLS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_URL TEXT UNIQUE NOT NULL
            )
        """.trimIndent()
        db.execSQL(createUrlsTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            val createUrlsTableQuery = """
                CREATE TABLE $TABLE_URLS (
                    $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $COLUMN_URL TEXT UNIQUE NOT NULL
                )
            """.trimIndent()
            db.execSQL(createUrlsTableQuery)
        }
    }

    fun addMissingPersonUrl(url: String): Boolean {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_URL, url)
        }

        return try {
            db.insertWithOnConflict(TABLE_URLS, null, values, SQLiteDatabase.CONFLICT_IGNORE) != -1L
        } catch (e: Exception) {
            false
        } finally {
            db.close()
        }
    }

    fun addMissingPersonUrls(urls: List<String>): Int {
        val db = writableDatabase
        var count = 0

        try {
            db.beginTransaction()
            urls.forEach { url ->
                val values = ContentValues().apply {
                    put(COLUMN_URL, url)
                }
                if (db.insertWithOnConflict(TABLE_URLS, null, values, SQLiteDatabase.CONFLICT_IGNORE) != -1L) {
                    count++
                }
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("Database", "Error adding URLs", e)
        } finally {
            db.endTransaction()
            db.close()
        }

        return count
    }

    fun containsUrl(url: String): Boolean {
        val db = readableDatabase
        val query = "SELECT 1 FROM $TABLE_URLS WHERE $COLUMN_URL = ?"

        return db.rawQuery(query, arrayOf(url)).use { cursor ->
            cursor.moveToFirst()
        }
    }

    fun getAllUrls(): List<String> {
        val db = readableDatabase
        val urls = mutableListOf<String>()

        val cursor = db.query(
            TABLE_URLS,
            arrayOf(COLUMN_URL),
            null, null, null, null, null
        )

        cursor.use {
            while (it.moveToNext()) {
                urls.add(it.getString(it.getColumnIndexOrThrow(COLUMN_URL)))
            }
        }

        db.close()
        return urls
    }

    fun addMissingPerson(person: MissingPerson, context: Context): Boolean {
        val db = writableDatabase
        // Проверяем по имени и описанию (они должны быть обязательными)
        val selection = "$COLUMN_NAME = ? AND $COLUMN_DESCRIPTION = ?"
        val selectionArgs = arrayOf(
            person.name,
            person.description ?: "" // Если description null, заменяем на пустую строку
        )

        return try {
            // Проверяем наличие записи
            val cursor = db.query(
                TABLE_NAME,
                null,
                selection,
                selectionArgs,
                null,
                null,
                null
            )

            if (cursor.count > 0) {
                // Запись существует - удаляем её
                cursor.moveToFirst()
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID))
                val deletedRows = db.delete(TABLE_NAME, "$COLUMN_ID = ?", arrayOf(id.toString()))

                cursor.close()

                if (deletedRows > 0) {
                    Toast.makeText(context, "Запись удалена", Toast.LENGTH_SHORT).show()
                    true
                } else {
                    Toast.makeText(context, "Ошибка при удалении записи", Toast.LENGTH_SHORT).show()
                    false
                }
            } else {
                // Записи нет - добавляем новую
                val values = ContentValues().apply {
                    put(COLUMN_NAME, person.name)
                    put(COLUMN_DESCRIPTION, person.description)
                    person.birthDate?.time?.let { put(COLUMN_BIRTH_DATE, it) }
                    person.disappearanceDate?.time?.let { put(COLUMN_DISAPPEARANCE_DATE, it) }
                    put(COLUMN_GENDER, person.gender)
                    put(COLUMN_PHOTO_URL, person.photos)
                }

                val result = db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE)
                if (result == -1L) {
                    Toast.makeText(context, "Ошибка при добавлении записи", Toast.LENGTH_SHORT).show()
                    false
                } else {
                    Toast.makeText(context, "Запись добавлена", Toast.LENGTH_SHORT).show()
                    true
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
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
                        photoUrl,
                        ""
                    )
                )
            }
        }

        db.close()
        return personList
    }

}