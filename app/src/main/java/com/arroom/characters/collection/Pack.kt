package com.arroom.characters.collection

import com.arroom.characters.data.CharacterItem
import kotlin.random.Random

/**
 * Наборы-паки: покупаешь за монеты, получаешь случайную карточку.
 * Классический коллекционный крючок — открытие с неизвестным исходом
 * держит сильнее, чем прямая покупка нужной карты.
 *
 * Веса выпадения намеренно смещены: даже дешёвый пак иногда даёт эпик,
 * иначе открывать неинтересно. Но матожидание пака ниже суммы прямых
 * покупок его содержимого — иначе паки ломают экономику магазина.
 */
enum class Pack(
    val price: Int,
    val titleRes: Int,
    // Веса по редкости: common, rare, epic, legendary
    val weights: IntArray
) {
    BASIC(150, com.arroom.characters.R.string.pack_basic, intArrayOf(70, 24, 5, 1)),
    PREMIUM(500, com.arroom.characters.R.string.pack_premium, intArrayOf(35, 45, 17, 3)),
    LEGENDARY(1500, com.arroom.characters.R.string.pack_legendary, intArrayOf(0, 40, 45, 15));

    /** Разыгрывает редкость по весам пака. */
    fun rollRarity(random: Random): Rarity {
        val total = weights.sum()
        var roll = random.nextInt(total)
        Rarity.values().forEachIndexed { i, rarity ->
            if (roll < weights[i]) return rarity
            roll -= weights[i]
        }
        return Rarity.COMMON
    }
}

/** Результат открытия пака — конкретная выпавшая карточка. */
data class PackResult(
    val item: CharacterItem,
    val rarity: Rarity,
    val isNew: Boolean
)
