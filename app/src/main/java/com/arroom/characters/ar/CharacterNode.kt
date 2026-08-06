package com.arroom.characters.ar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import dev.romainguy.kotlin.math.Float3
import io.github.sceneview.model.ModelInstance
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Контейнер персонажа.
 *
 * Иерархия: AnchorNode (ARCore) -> CharacterNode (наши анимации) -> ModelNode (сама модель).
 * Отдельный контейнер нужен, чтобы анимировать масштаб/парение, не ломая
 * нормализацию размера, которую делает ModelNode через scaleToUnits.
 */
class CharacterNode(
    engine: com.google.android.filament.Engine,
    val modelNode: ModelNode,
    val characterId: String
) : Node(engine) {

    /** Базовая высота над якорем — вокруг неё качается idle-анимация. */
    private var baseY = 0f

    /**
     * Все запущенные ноды корутины. Без этого списка idle-анимация
     * продолжает крутиться после удаления персонажа: цикл `while (isActive)`
     * живёт ровно столько, сколько живёт scope экрана, то есть вечно.
     * Тридцать раз в секунду он пишет в уничтоженный Filament-объект —
     * это и утечка, и потенциальный краш.
     */
    private val jobs = mutableListOf<Job>()

    var isSelected: Boolean = false
        private set

    init {
        addChildNode(modelNode)
        isPositionEditable = true
        isRotationEditable = true
        isScaleEditable = true
        isTouchable = true
        scale = Float3(0.001f)
    }

    /** Сколько скелетных анимаций внутри glTF. */
    val animationCount: Int get() = modelNode.animator.animationCount

    /** Имя анимации из glTF. null — если автор модели его не задал. */
    fun animationNameOrNull(index: Int): String? =
        runCatching { modelNode.animator.getAnimationName(index) }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }

    fun playAnimation(index: Int, loop: Boolean = true) {
        if (index !in 0 until animationCount) return
        stopAllAnimations()
        modelNode.playAnimation(index, loop = loop)
    }

    /** Общего stopAnimation() в API нет — гасим каждую по индексу. */
    private fun stopAllAnimations() {
        repeat(animationCount) { i -> runCatching { modelNode.stopAnimation(i) } }
    }

    /** Пружинистое появление «из ниоткуда» + лёгкий доворот. */
    fun animateAppearance(scope: CoroutineScope, onDone: () -> Unit = {}) {
        jobs += scope.launch {
            val s = Animatable(0.001f)
            val spin = Animatable(-40f)
            launch {
                spin.animateTo(0f, tween(durationMillis = 620)) {
                    rotation = Float3(0f, value, 0f)
                }
            }
            s.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.48f,   // ощутимый «отскок»
                    stiffness = Spring.StiffnessLow
                )
            ) {
                scale = Float3(value)
            }
            onDone()
        }
    }

    /** Плавное исчезновение перед удалением. */
    suspend fun animateDisappearance() {
        val s = Animatable(scale.x)
        s.animateTo(0.001f, tween(durationMillis = 260)) { scale = Float3(value) }
    }

    /**
     * Вечное лёгкое «дыхание»: подъём-спуск + медленный поворот.
     * Работает поверх скелетной анимации glTF, не конфликтуя с ней.
     */
    fun startIdleFloat(scope: CoroutineScope, amplitude: Float = 0.012f, spinSpeed: Float = 6f) {
        baseY = position.y
        jobs += scope.launch {
            val start = System.nanoTime()
            while (isActive) {
                val t = (System.nanoTime() - start) / 1_000_000_000f
                position = Float3(position.x, baseY + sin(t * 1.6f) * amplitude, position.z)
                if (!isSelected) {
                    rotation = Float3(rotation.x, rotation.y + spinSpeed * 0.016f, rotation.z)
                }
                delay(16)
            }
        }
    }

    /** Подсветка выбранного персонажа коротким «пульсом». */
    fun pulse(scope: CoroutineScope) {
        jobs += scope.launch {
            val s = Animatable(scale.x)
            s.animateTo(scale.x * 1.12f, tween(130)) { scale = Float3(value) }
            s.animateTo(1f, spring(dampingRatio = 0.4f)) { scale = Float3(value) }
        }
    }

    fun setSelected(selected: Boolean) {
        isSelected = selected
    }

    /**
     * Останавливает анимации и отдаёт ресурсы Filament обратно движку.
     * Вызывать обязательно перед удалением ноды из сцены: меши и текстуры
     * живут в нативной памяти, сборщик мусора Kotlin про них не знает.
     */
    fun dispose() {
        jobs.forEach { it.cancel() }
        jobs.clear()
        runCatching { stopAllAnimations() }
        runCatching { modelNode.destroy() }
        runCatching { destroy() }
    }

    companion object {
        fun create(
            engine: com.google.android.filament.Engine,
            instance: ModelInstance,
            characterId: String,
            scaleToUnits: Float
        ): CharacterNode {
            val model = ModelNode(
                modelInstance = instance,
                scaleToUnits = scaleToUnits
            ).apply {
                isShadowCaster = true
                isShadowReceiver = true
            }
            return CharacterNode(engine, model, characterId).apply {
                // Автостарт первой встроенной анимации, если она есть
                if (animationCount > 0) playAnimation(0)
            }
        }
    }
}
