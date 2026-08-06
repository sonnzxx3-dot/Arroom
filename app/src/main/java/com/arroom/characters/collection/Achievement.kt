package com.arroom.characters.collection

import com.arroom.characters.R

/**
 * Достижения. Дают монеты за вехи коллекции — цель, ради которой стоит
 * ловить не только любимых, но и «скучных» персонажей.
 *
 * Проверяются на клиенте по текущему инвентарю. Награда выдаётся один раз;
 * факт выдачи хранится там же, где инвентарь. При переезде на сервер
 * проверка и выдача уедут в серверную функцию — по тому же принципу
 * «клиент шлёт намерение, сервер считает».
 */
enum class Achievement(
    val titleRes: Int,
    val descRes: Int,
    val reward: Int,
    val check: (AchievementStats) -> Boolean
) {
    FIRST_CATCH(
        R.string.ach_first_title, R.string.ach_first_desc, 50,
        { it.totalOwned >= 1 }
    ),
    COLLECTOR_5(
        R.string.ach_five_title, R.string.ach_five_desc, 150,
        { it.uniqueOwned >= 5 }
    ),
    RARE_HUNTER(
        R.string.ach_rare_title, R.string.ach_rare_desc, 200,
        { it.rareOrBetter >= 3 }
    ),
    EPIC_OWNER(
        R.string.ach_epic_title, R.string.ach_epic_desc, 300,
        { it.epicOrBetter >= 1 }
    ),
    LEGENDARY_OWNER(
        R.string.ach_legend_title, R.string.ach_legend_desc, 600,
        { it.legendary >= 1 }
    ),
    COMPLETIONIST(
        R.string.ach_complete_title, R.string.ach_complete_desc, 1000,
        { it.total > 0 && it.uniqueOwned >= it.total }
    );
}

/** Срез коллекции, по которому проверяются достижения. */
data class AchievementStats(
    val totalOwned: Int,
    val uniqueOwned: Int,
    val rareOrBetter: Int,
    val epicOrBetter: Int,
    val legendary: Int,
    val total: Int
)
