# ---- Filament / SceneView / ARCore ----
# Всё это вызывается из нативного кода через JNI: R8 не видит вызовов
# и вырежет классы, если их не удержать. Ломается только в release-сборке,
# поэтому обязательно тестируйте release, а не только debug.
-keep class com.google.android.filament.** { *; }
-keepclassmembers class com.google.android.filament.** { *; }
-keep class com.google.ar.core.** { *; }
-keep class io.github.sceneview.** { *; }
-dontwarn com.google.android.filament.**
-dontwarn com.google.ar.core.**

# ---- Kotlin ----
-keepclassmembers class kotlin.Metadata { public <methods>; }
-dontwarn kotlin.**

# Корутины: сервисный загрузчик диспетчера ищется рефлексией
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ---- Наш код ----
# Сервис записи стартует по имени класса из Intent
-keep class com.arroom.characters.record.RecorderService { *; }

# Строки ресурсов подставляются по id — на всякий случай не трогаем R
-keepclassmembers class **.R$* { public static <fields>; }
