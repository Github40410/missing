package com.example.missingpeople.servic

import android.content.Context
import android.widget.Toast
import com.example.missingpeople.repositor.MissingPerson
import com.example.missingpeople.repositor.RepWebMVD
import com.example.missingpeople.view.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ParserMVD {

    private val userAgents = listOf(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1",
        "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.210 Mobile Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_2) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (iPad; CPU OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1",
        "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.210 Mobile Safari/537.36",
        "Mozilla/5.0 (X11; Linux x86_64; rv:121.0) Gecko/20100101 Firefox/121.0",
        "Mozilla/5.0 (Windows NT 11.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.2213.1",
        "Mozilla/5.0 (Linux; Android 13; SM-G998B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.6045.163 Mobile Safari/537.36",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1",
        "Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_2) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15",
        "Mozilla/5.0 (Linux; Android 12; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.5993.111 Mobile Safari/537.36",
        "Mozilla/5.0 (iPad; CPU OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.6045.199 Safari/537.36 OPR/105.0.4975.0",
        "Mozilla/5.0 (X11; CrOS x86_64 15359.58.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.6045.212 Safari/537.36",
        "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.210 Mobile Safari/537.36",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 15_7 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.7 Mobile/15E148 Safari/604.1",
        "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:109.0) Gecko/20100101 Firefox/115.0",

        "Mozilla/5.0 (Linux; Android 14; SM-G965F) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.210 Mobile Safari/537.36",
        "Mozilla/5.0 (iPhone14,3; U; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/602.1.50 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/602.1",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Vivaldi/6.2.3105.58",
        "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:121.0) Gecko/20100101 Firefox/121.0",
        "Mozilla/5.0 (PlayStation 5) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Safari/605.1.15",
        "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.210 Mobile Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Whale/3.21.192.22 Safari/537.36",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/120.0.6099.119 Mobile/15E148 Safari/604.1",
        "Mozilla/5.0 (Linux; Android 13; SM-G781B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.6045.163 Mobile Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.6045.199 Safari/537.36",

        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 EdgA/120.0.2213.1",
        "Mozilla/5.0 (Linux; Android 14; SM-G998U) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.210 Mobile Safari/537.36",
        "Mozilla/5.0 (iPad; CPU OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/120.0.6099.119 Mobile/15E148 Safari/604.1",
        "Mozilla/5.0 (X11; Fedora; Linux x86_64; rv:121.0) Gecko/20100101 Firefox/121.0",
        "Mozilla/5.0 (Windows NT 10.0; Trident/7.0; rv:11.0) like Gecko",
        "Mozilla/5.0 (Linux; Android 14; Pixel 6a) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.210 Mobile Safari/537.36",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 16_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.5 Mobile/15E148 Safari/604.1",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_3) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.5993.88 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 OPR/106.0.4998.0",

    )
    private val allLinks = ArrayList<String>()
    private val semaphore = Semaphore(5)
    private val dateFormats = mapOf(
        "birthDate" to SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()),
        "disappearanceDate" to SimpleDateFormat("MMMM dd, yyyy", Locale.US)
    )

    suspend fun extractAllPageUrls(startUrl: String): ArrayList<String> {

        //User-Agent Проверить!!!

        val urlConstruct = "https://поисковая-база.рф/search/sOrder,dt_pub_date/iOrderType,desc/category,1/iPage,"

        try {
            var doc = Jsoup.connect(startUrl).userAgent(userAgents.random()).get()
            allLinks.add(startUrl)

            val lastPageLink = doc.selectFirst("a.searchPaginationLast.list-last")?.absUrl("href")

            lastPageLink?.let { url ->
                doc = Jsoup.connect(url).get()
                val maxPageElement = doc.selectFirst("span.searchPaginationSelected")
                val maxPage = maxPageElement?.text()?.trim()?.toIntOrNull() ?: 1

                if(maxPage > 1) {
                    allLinks.addAll(generateSimplePages(urlConstruct, maxPage))
                }
            }

        } catch (e: IOException) {
            e.printStackTrace()
        }

        return allLinks
    }

    private fun generateSimplePages(baseUrl: String, maxPage: Int): ArrayList<String> {

        for (page in 2..maxPage) {
            allLinks.add("$baseUrl$page")
        }
        return allLinks
    }

    suspend fun collectUniqueLinks(urls: ArrayList<String>): ArrayList<String> {
        return coroutineScope {
            urls.map { url ->
                async(Dispatchers.IO) {
                    semaphore.withPermit {
                        try {
                            parsePageLinks(url)
                        } catch (e: Exception) {
                            emptyList<String>()
                        }
                    }
                }
            }.awaitAll()
                .flatten()
                .toSet()
                .toList() as ArrayList<String>
        }
    }

    // Парсинг отдельной страницы
    private fun parsePageLinks(url: String): List<String> {
        val links = mutableListOf<String>()
        try {
            val doc = Jsoup.connect(url)
                .timeout(15_000)
                .get()

            doc.select("a.item__title").forEach { element ->
                val href = element.absUrl("href")
                if (href.isNotBlank()) {
                    links.add(href)
                }
            }
        } catch (e: IOException) {
            println("Error parsing $url: ${e.message}")
        }
        return links
    }

    // Парсинг пропавшего
    public suspend fun parserPersonMissing(urls: List<String>, context: Context):List<MissingPerson>{
        val listMissingPeople = ArrayList<MissingPerson>()
        var i:Int = 1;

        // Параллельная обработка с ограничением
        val jobs = urls.map { url ->
            CoroutineScope(Dispatchers.IO).async {
                semaphore.withPermit { // Используем семафор для ограничения
                    try {
                        val doc = Jsoup.connect(url).userAgent(userAgents.random()).get()

                        // Оригинальный код парсинга без изменений
                        var fullName =  ""
                        val description = doc.selectFirst("div.tab[data-tab=1] .text")
                            ?.text()
                            ?.replace("\\s+".toRegex(), " ")
                            ?.trim() ?: ""

                        val metaElements = doc.select(".meta_list .meta")
                        val metaData = mutableMapOf<String, String>()

                        metaElements.forEach { element ->
                            val key = element.selectFirst("strong")?.text()?.replace(":", "")?.trim()
                            val value = element.ownText().trim()
                            if (key != null) metaData[key] = value
                        }

                        val gender = when (metaData["Пол"]?.firstOrNull()?.uppercase()) {
                            "М" -> "Мужской"
                            "Ж" -> "Женский"
                            else -> "Не указан"
                        }

                        val birthDate = parseDate(metaData["Дата рождения"], "birthDate")
                        val disappearanceDate = parseDate(metaData["Дата пропажи"], "disappearanceDate")

                        val imgElement = doc.selectFirst("img.imgswipdis")
                        var photoUrl = ""
                        imgElement?.let { img ->
                            val src = img.attr("src")
                            val alt = img.attr("alt")

                            photoUrl = src
                            fullName = alt
                        } ?: Toast.makeText(context, "Не удалось обработать", Toast.LENGTH_SHORT).show()

                        i=i+1
                        MissingPerson(fullName, description, birthDate, disappearanceDate, gender, photoUrl)
                    } catch (e: Exception) {
                        null
                    }
                }
            }
        }

        // Собираем все результаты
        jobs.awaitAll().filterNotNull().forEach { listMissingPeople.add(it) }

        withContext(Dispatchers.Main) {
            Toast.makeText(
                context,
                "Обработано: ${listMissingPeople.size} записей",
                Toast.LENGTH_SHORT
            ).show()
        }
        return listMissingPeople
    }

    private fun parseDate(dateString: String?, formatKey: String): Date? {
        return try {
            dateString?.let { dateFormats[formatKey]?.parse(it) }
        } catch (e: Exception) {
            null
        }
    }
}