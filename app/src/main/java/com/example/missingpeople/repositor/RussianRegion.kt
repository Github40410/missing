package com.example.missingpeople.repositor

enum class RussianRegion(val displayName: String) {
    ALL("Вся Россия"),
    ADYGEA("Адыгея"),
    ALTAI("Алтай"),
    ALTAI_REPUBLIC("Алтайский край"),
    AMUR("Амурская область"),
    ARKHANGELSK("Архангельская область"),
    // ... все остальные регионы
    YAKUTIA("Якутия"),
    YAMAL("Ямало-Ненецкий АО");

    companion object {
        fun getAllRegions(): List<RussianRegion> = values().toList()
    }
}