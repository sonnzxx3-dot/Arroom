package com.arroom.characters.collection

import android.content.Context
import com.arroom.characters.data.CharacterItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import kotlin.random.Random

/**
 * Вся клиентская экономика коллекции: инвентарь, монеты, поимки, награды.
 *
 * Сейчас хранится локально в SharedPreferences. Когда появится сервер,
 * этот класс станет тонкой обёрткой над сетевым API — публичный интерфейс
 * (потоки cards/wallet и методы catch/buy/sell) менять не придётся, поэтому
 * весь остальной UI переживёт переход на бэкенд без изменений.
 *
 * ВАЖНО про честность экономики: пока данные лежат на устройстве, монеты и
 * инвентарь можно отредактировать вручную. Для одиночной коллекции это
 * приемлемо. В момент, когда включится торговля между людьми, источником
 * правды обязан стать сервер — иначе любой сможет «напечатать» себе карточки.
 */
class CollectionRepository private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("collection", Context.MODE_PRIVATE)

    private val _wallet = MutableStateFlow(Wallet(prefs.getInt(KEY_COINS, START_COINS)))
    val wallet: StateFlow<Wallet> = _wallet.asStateFlow()

    // characterId -> запись инвентаря
    private val _owned = MutableStateFlow(loadOwned())
    val owned: StateFlow<Map<String, OwnedRecord>> = _owned.asStateFlow()

    data class OwnedRecord(val count: Int, val firstCaughtAt: Long, val favorite: Boolean)

    /** Событие поимки для UI: показать награду и, если это дубль, монеты. */
    data class CatchResult(
        val firstTime: Boolean,
        val rarity: Rarity,
        val coinsAwarded: Int
    )

    /**
     * Персонаж пойман (поставлен в комнату). Первая поимка даёт карточку и
     * награду по редкости; повторная — небольшие монеты за дубликат.
     */
    fun catch(item: CharacterItem, thumbnailPath: String?): CatchResult {
        val rarity = rarityOf(item.id)
        val current = _owned.value.toMutableMap()
        val existing = current[item.id]

        return if (existing == null) {
            current[item.id] = OwnedRecord(1, System.currentTimeMillis(), false)
            _owned.value = current
            persistOwned()
            val reward = firstCatchReward(rarity)
            addCoins(reward)
            CatchResult(firstTime = true, rarity = rarity, coinsAwarded = reward)
        } else {
            current[item.id] = existing.copy(count = existing.count + 1)
            _owned.value = current
            persistOwned()
            val reward = DUPLICATE_REWARD
            addCoins(reward)
            CatchResult(firstTime = false, rarity = rarity, coinsAwarded = reward)
        }
    }

    /** Покупка карточки за монеты в магазине. true — если хватило монет. */
    fun buy(item: CharacterItem): Boolean {
        val price = rarityOf(item.id).shopPrice
        if (!_wallet.value.canAfford(price)) return false
        spendCoins(price)
        val current = _owned.value.toMutableMap()
        val existing = current[item.id]
        current[item.id] = existing?.copy(count = existing.count + 1)
            ?: OwnedRecord(1, System.currentTimeMillis(), false)
        _owned.value = current
        persistOwned()
        return true
    }

    /** Продажа дубликата за монеты. Последнюю карту продать нельзя. */
    fun sellDuplicate(characterId: String): Boolean {
        val current = _owned.value.toMutableMap()
        val record = current[characterId] ?: return false
        if (record.count <= 1) return false
        current[characterId] = record.copy(count = record.count - 1)
        _owned.value = current
        persistOwned()
        addCoins(rarityOf(characterId).sellPrice)
        return true
    }

    fun toggleFavorite(characterId: String) {
        val current = _owned.value.toMutableMap()
        val record = current[characterId] ?: return
        current[characterId] = record.copy(favorite = !record.favorite)
        _owned.value = current
        persistOwned()
    }

    // --- Ежедневная награда ---

    /** Сколько монет даст следующий ежедневный вход, растёт со стриком. */
    fun dailyRewardAmount(): Int {
        val streak = prefs.getInt(KEY_STREAK, 0)
        return (DAILY_BASE + streak * DAILY_STEP).coerceAtMost(DAILY_MAX)
    }

    fun canClaimDaily(): Boolean {
        val last = prefs.getLong(KEY_LAST_DAILY, 0L)
        return daysSinceEpoch(System.currentTimeMillis()) > daysSinceEpoch(last)
    }

    /**
     * Забирает ежедневную награду. Стрик растёт при входе день-в-день и
     * сбрасывается, если пропущен день. Возвращает начисленные монеты или 0.
     */
    fun claimDaily(): Int {
        if (!canClaimDaily()) return 0
        val now = System.currentTimeMillis()
        val lastDay = daysSinceEpoch(prefs.getLong(KEY_LAST_DAILY, 0L))
        val today = daysSinceEpoch(now)

        val streak = prefs.getInt(KEY_STREAK, 0)
        val newStreak = if (today - lastDay == 1L) streak + 1 else 1

        val reward = (DAILY_BASE + (newStreak - 1) * DAILY_STEP).coerceAtMost(DAILY_MAX)
        prefs.edit()
            .putLong(KEY_LAST_DAILY, now)
            .putInt(KEY_STREAK, newStreak)
            .apply()
        addCoins(reward)
        return reward
    }

    fun currentStreak(): Int = prefs.getInt(KEY_STREAK, 0)

    // --- Паки ---

    /** Открывает пак: списывает цену, разыгрывает карту, добавляет в инвентарь. */
    fun openPack(pack: Pack, catalog: List<CharacterItem>): PackResult? {
        if (!_wallet.value.canAfford(pack.price)) return null
        if (catalog.isEmpty()) return null

        val rarity = pack.rollRarity(Random.Default)

        // Ищем персонажей нужной редкости; если таких нет — берём любую близкую
        val pool = catalog.filter { rarityOf(it.id) == rarity }
            .ifEmpty { catalog.filter { rarityOf(it.id).ordinal <= rarity.ordinal } }
            .ifEmpty { catalog }

        val item = pool[Random.nextInt(pool.size)]
        spendCoins(pack.price)

        val current = _owned.value.toMutableMap()
        val existing = current[item.id]
        val isNew = existing == null
        current[item.id] = existing?.copy(count = existing.count + 1)
            ?: OwnedRecord(1, System.currentTimeMillis(), false)
        _owned.value = current
        persistOwned()

        return PackResult(item, rarityOf(item.id), isNew)
    }

    // --- Достижения ---

    /** Достижения, готовые к выдаче (условие выполнено, награда ещё не взята). */
    fun claimableAchievements(catalog: List<CharacterItem>): List<Achievement> {
        val stats = statsOf(catalog)
        val claimed = claimedAchievements()
        return Achievement.values().filter { it.check(stats) && it.name !in claimed }
    }

    fun claimedAchievements(): Set<String> =
        prefs.getStringSet(KEY_ACHIEVEMENTS, emptySet()) ?: emptySet()

    /** Забирает награду за достижение, если оно ещё не взято и условие верно. */
    fun claimAchievement(achievement: Achievement, catalog: List<CharacterItem>): Int {
        val claimed = claimedAchievements().toMutableSet()
        if (achievement.name in claimed) return 0
        if (!achievement.check(statsOf(catalog))) return 0
        claimed += achievement.name
        prefs.edit().putStringSet(KEY_ACHIEVEMENTS, claimed).apply()
        addCoins(achievement.reward)
        return achievement.reward
    }

    private fun statsOf(catalog: List<CharacterItem>): AchievementStats {
        val owned = _owned.value
        val ownedItems = catalog.filter { owned[it.id]?.let { r -> r.count > 0 } == true }
        return AchievementStats(
            totalOwned = owned.values.sumOf { it.count },
            uniqueOwned = ownedItems.size,
            rareOrBetter = ownedItems.count { rarityOf(it.id).ordinal >= Rarity.RARE.ordinal },
            epicOrBetter = ownedItems.count { rarityOf(it.id).ordinal >= Rarity.EPIC.ordinal },
            legendary = ownedItems.count { rarityOf(it.id) == Rarity.LEGENDARY },
            total = catalog.size
        )
    }

    private fun daysSinceEpoch(millis: Long): Long = millis / 86_400_000L

    /**
     * Редкость персонажа детерминирована его id: один и тот же герой всегда
     * одной редкости у всех игроков. Хэш даёт стабильное распределение
     * примерно 55/30/12/3 (обычные/редкие/эпические/легендарные).
     */
    fun rarityOf(characterId: String): Rarity {
        val bucket = (Random(characterId.hashCode().toLong()).nextInt(100))
        return when {
            bucket < 55 -> Rarity.COMMON
            bucket < 85 -> Rarity.RARE
            bucket < 97 -> Rarity.EPIC
            else -> Rarity.LEGENDARY
        }
    }

    private fun firstCatchReward(rarity: Rarity): Int = when (rarity) {
        Rarity.COMMON -> 30
        Rarity.RARE -> 80
        Rarity.EPIC -> 200
        Rarity.LEGENDARY -> 500
    }

    private fun addCoins(amount: Int) {
        _wallet.value += amount
        prefs.edit().putInt(KEY_COINS, _wallet.value.coins).apply()
    }

    private fun spendCoins(amount: Int) {
        _wallet.value -= amount
        prefs.edit().putInt(KEY_COINS, _wallet.value.coins).apply()
    }

    private fun loadOwned(): Map<String, OwnedRecord> = runCatching {
        val json = JSONObject(prefs.getString(KEY_OWNED, "{}") ?: "{}")
        buildMap {
            json.keys().forEach { id ->
                val o = json.getJSONObject(id)
                put(id, OwnedRecord(
                    count = o.getInt("count"),
                    firstCaughtAt = o.optLong("first", 0L),
                    favorite = o.optBoolean("fav", false)
                ))
            }
        }
    }.getOrDefault(emptyMap())

    private fun persistOwned() {
        val json = JSONObject()
        _owned.value.forEach { (id, r) ->
            json.put(id, JSONObject()
                .put("count", r.count)
                .put("first", r.firstCaughtAt)
                .put("fav", r.favorite))
        }
        prefs.edit().putString(KEY_OWNED, json.toString()).apply()
    }

    /** Строит карточки для всех известных персонажей: пойманных и силуэтов. */
    fun buildCards(
        catalog: List<CharacterItem>,
        thumbnailFor: (String) -> String?
    ): List<Card> {
        val ownedMap = _owned.value
        return catalog.map { item ->
            val record = ownedMap[item.id]
            Card(
                characterId = item.id,
                title = item.title,
                rarity = rarityOf(item.id),
                thumbnailPath = thumbnailFor(item.id),
                ownedCount = record?.count ?: 0,
                firstCaughtAt = record?.firstCaughtAt ?: 0L,
                favorite = record?.favorite ?: false
            )
        }.sortedWith(
            compareByDescending<Card> { it.isOwned }
                .thenByDescending { it.rarity.ordinal }
                .thenByDescending { it.favorite }
        )
    }

    companion object {
        private const val KEY_COINS = "coins"
        private const val KEY_OWNED = "owned"
        private const val KEY_LAST_DAILY = "last_daily"
        private const val KEY_STREAK = "streak"
        private const val KEY_ACHIEVEMENTS = "achievements"
        private const val START_COINS = 250
        private const val DUPLICATE_REWARD = 15

        // Ежедневная награда: 50 в первый день, +25 за каждый день стрика, потолок 250
        private const val DAILY_BASE = 50
        private const val DAILY_STEP = 25
        private const val DAILY_MAX = 250

        @Volatile private var instance: CollectionRepository? = null

        fun get(context: Context): CollectionRepository =
            instance ?: synchronized(this) {
                instance ?: CollectionRepository(context.applicationContext).also { instance = it }
            }
    }
}
