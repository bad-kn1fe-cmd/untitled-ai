package com.example.ui.theme

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.example.R
import java.io.File
import java.io.FileOutputStream

val MontserratFontFamily = FontFamily(
    Font(R.font.montserrat, FontWeight.Normal),
    Font(R.font.montserrat, FontWeight.Medium),
    Font(R.font.montserrat, FontWeight.Bold)
)

val PlayfairFontFamily = FontFamily(
    Font(R.font.playfair_display, FontWeight.Normal),
    Font(R.font.playfair_display, FontWeight.Medium),
    Font(R.font.playfair_display, FontWeight.Bold)
)

val RobotoMonoFontFamily = FontFamily(
    Font(R.font.roboto_mono, FontWeight.Normal),
    Font(R.font.roboto_mono, FontWeight.Medium),
    Font(R.font.roboto_mono, FontWeight.Bold)
)

data class AppFontOption(
    val id: String,
    val nameRu: String,
    val descriptionRu: String,
    val fontFamily: FontFamily
)

val AvailableBaseFonts = listOf(
    AppFontOption("system", "Системный (По умолчанию)", "Стандартный шрифт системы Android", FontFamily.Default),
    AppFontOption("montserrat", "Montserrat (Современный Sans)", "Чистый геометрический гротеск с превосходной читаемостью", MontserratFontFamily),
    AppFontOption("playfair", "Playfair Display (Антиква)", "Элегантный литературный шрифт для глубокого ролевого погружения", PlayfairFontFamily),
    AppFontOption("roboto_mono", "Roboto Mono (Код & Кибер)", "Четкий моноширинный шрифт в стиле киберпанка и консоли", RobotoMonoFontFamily),
    AppFontOption("serif", "Serif (Классический)", "Академический шрифт с засечками", FontFamily.Serif),
    AppFontOption("sans_serif", "Sans-Serif (Гротеск)", "Универсальный гротеск без засечек", FontFamily.SansSerif)
)

object FontHelper {
    fun resolveFontFamily(choice: String, customPath: String): FontFamily {
        return when (choice) {
            "montserrat" -> MontserratFontFamily
            "playfair" -> PlayfairFontFamily
            "roboto_mono" -> RobotoMonoFontFamily
            "serif" -> FontFamily.Serif
            "sans_serif" -> FontFamily.SansSerif
            "custom" -> {
                if (customPath.isNotBlank()) {
                    try {
                        val file = File(customPath)
                        if (file.exists() && file.length() > 0) {
                            FontFamily(Font(file, FontWeight.Normal))
                        } else {
                            FontFamily.Default
                        }
                    } catch (_: Exception) {
                        FontFamily.Default
                    }
                } else {
                    FontFamily.Default
                }
            }
            else -> FontFamily.Default
        }
    }

    fun importCustomFont(context: Context, uri: Uri): Pair<String, String>? {
        return try {
            val contentResolver = context.contentResolver
            val fileName = getFileName(context, uri) ?: "custom_font.ttf"
            val fontsDir = File(context.filesDir, "custom_fonts")
            if (!fontsDir.exists()) fontsDir.mkdirs()

            val sanitizedName = fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            val destFile = File(fontsDir, sanitizedName)

            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (destFile.exists() && destFile.length() > 0) {
                Pair(destFile.absolutePath, fileName.removeSuffix(".ttf").removeSuffix(".otf"))
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        name = it.getString(index)
                    }
                }
            }
        }
        if (name == null) {
            name = uri.path
            val cut = name?.lastIndexOf('/') ?: -1
            if (cut != -1 && name != null) {
                name = name!!.substring(cut + 1)
            }
        }
        return name
    }
}
