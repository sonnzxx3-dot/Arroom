package com.arroom.characters.collection

import androidx.compose.ui.graphics.Color

/**
 * Редкость карточки. Определяет цвет рамки, вероятность выпадения
 * и цену в магазине. Порядок важен: ordinal используется для сортировки.
 */
enum class Rarity(
    val title: String,
    val color: Color,
    val glow: Color,
    val shopPrice: Int,
    val sellPrice: Int
) {
    COMMON("Обычная", Color(0xFF9AA4B8), Color(0x559AA4B8), 100, 25),
    RARE("Редкая", Color(0xFF3DDCFF), Color(0x553DDCFF), 300, 90),
    EPIC("Эпическая", Color(0xFF8B6CFF), Color(0x668B6CFF), 800, 260),
    LEGENDARY("Легендарная", Color(0xFFFFB020), Color(0x77FFB020), 2000, 700);

    companion object {
        /** Английские подписи подставляются в UI через ресурсы; здесь — дефолт. */
        fun labelRes(rarity: Rarity): Int = when (rarity) {
            COMMON -> com.arroom.characters.R.string.rarity_common
            RARE -> com.arroom.characters.R.string.rarity_rare
            EPIC -> com.arroom.characters.R.string.rarity_epic
            LEGENDARY -> com.arroom.characters.R.string.rarity_legendary
        }
    }
}

/**
 * Карточка коллекции. Привязана к персонажу (characterId), но несёт
 * собственные данные: сколько раз поймана, когда впервые, избранная ли.
 *
 * ownedCount > 0 означает, что карточка в коллекции. Ноль — «силуэт»,
 * ещё не пойманная: показываем затемнённой, чтобы было видно, чего не хватает.
 */
data class Card(
    val characterId: String,
    val title: String,
    val rarity: Rarity,
    val thumbnailPath: String?,
    val ownedCount: Int = 0,
    val firstCaughtAt: Long = 0L,
    val favorite: Boolean = false
) {
    val isOwned: Boolean get() = ownedCount > 0
    val hasDuplicates: Boolean get() = ownedCount > 1
}

/** Кошелёк игрока. Одна валюта — монеты. */
data class Wallet(val coins: Int) {
    fun canAfford(price: Int) = coins >= price
    operator fun plus(amount: Int) = Wallet(coins + amount)
    operator fun minus(amount: Int) = Wallet((coins - amount).coerceAtLeast(0))
}
