package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.datastore.UserSettings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sin

enum class VerityStage(
    val title: String,
    val badge: String,
    val drawableRes: Int,
    val accentColor: Color,
    val secondaryColor: Color,
    val description: String,
    val quotes: List<String>
) {
    OBESITY(
        title = "Обессити",
        badge = "📦 УЛЬТРА-ПЫШКА",
        drawableRes = R.drawable.verity_obesity,
        accentColor = Color(0xFFF97316),
        secondaryColor = Color(0xFFEA580C),
        description = "Откормлен бургерами, пиццей и пончиками! Теперь он счастливо сидит в картонной коробке.",
        quotes = listOf(
            "«Ммм... ещё один бургер, и коробка точно треснет! 📦🍔»",
            "«Штанга? Нет, спасибо, я выбираю пиццу и чилл в коробке!»",
            "«Идеальная форма — это шар! Подай ещё пончик 🍩»",
            "«Мне так уютно в этой коробке, главное — не закрывайте крышку!»"
        )
    ),
    VERITY(
        title = "Верити",
        badge = "😊 КЛАССИЧЕСКИЙ",
        drawableRes = R.drawable.verity_normal,
        accentColor = Color(0xFFEAB308),
        secondaryColor = Color(0xFFCA8A04),
        description = "Жизнерадостный классический Верити! Готов либо вкусно покушать, либо пойти тягать штангу.",
        quotes = listOf(
            "«Привет! Я Верити 😊 Чем займёмся — бургеры или качалка?»",
            "«Обожаю, когда обо мне заботятся! Покорми меня чем-нибудь!»",
            "«Лови фастфуд — стану Обессити, лови ПП и штангу — стану Моггити!»",
            "«Двигай меня по лужайке и лови вкусности!»"
        )
    ),
    MOGGITY(
        title = "Моггити",
        badge = "🤫🧏‍♂️ ГИГА-СИГМА",
        drawableRes = R.drawable.verity_moggity,
        accentColor = Color(0xFF10B981),
        secondaryColor = Color(0xFF059669),
        description = "Накачан штангой и полезной пищей! Острые скулы, стальной взгляд и бесконечный моггинг.",
        quotes = listOf(
            "«🤫🧏‍♂️ Тише... сейчас идёт серия мьюинга и жим 150 кг.»",
            "«Брокколи, куриная грудка и штанга сотворили эти скулы 💪»",
            "«Никаких пончиков. Только чистый протеин и жёсткий Моггинг.»",
            "«Твой Верити официально перешёл в режим Гига-Сигмы.»"
        )
    );

    fun displayTitle(isEn: Boolean): String = if (!isEn) title else when (this) {
        OBESITY -> "Obessity"
        VERITY -> "Verity"
        MOGGITY -> "Moggiti"
    }

    fun displayBadge(isEn: Boolean): String = if (!isEn) badge else when (this) {
        OBESITY -> "📦 ULTRA-CHUNKY"
        VERITY -> "😊 CLASSIC"
        MOGGITY -> "🤫🧏‍♂️ GIGA-SIGMA"
    }

    fun displayDescription(isEn: Boolean): String = if (!isEn) description else when (this) {
        OBESITY -> "Overfed with burgers, pizza, and donuts! Now he happily sits in a cardboard box."
        VERITY -> "Cheerful classic Verity! Ready to eat tasty snacks or lift weights."
        MOGGITY -> "Pumped with barbells and healthy food! Sharp jawline, steel gaze, and non-stop mogging."
    }

    fun displayQuotes(isEn: Boolean): List<String> = if (!isEn) quotes else when (this) {
        OBESITY -> listOf(
            "«Mmm... one more burger and this box will burst! 📦🍔»",
            "«Barbell? No thanks, I choose pizza and chilling in a box!»",
            "«The ideal shape is a circle! Pass another donut 🍩»",
            "«I feel so cozy in this box, just don't close the lid!»"
        )
        VERITY -> listOf(
            "«Hi! I'm Verity 😊 What should we do — burgers or gym?»",
            "«I love being taken care of! Feed me something yummy!»",
            "«Catch fast food to become Obessity, catch healthy food & barbells to become Moggiti!»",
            "«Drag me across the lawn and catch treats!»"
        )
        MOGGITY -> listOf(
            "«🤫🧏‍♂️ Quiet... mewing session and 150kg bench press in progress.»",
            "«Broccoli, chicken breast, and barbells created this jawline 💪»",
            "«No donuts. Pure protein and hard Mogging only.»",
            "«Your Verity has officially entered Giga-Sigma mode.»"
        )
    }

    companion object {
        fun fromScore(score: Int): VerityStage {
            return when {
                score <= -25 -> OBESITY
                score >= 25 -> MOGGITY
                else -> VERITY
            }
        }
    }
}

private data class FallingFoodItem(
    val id: Int,
    val emoji: String,
    val title: String,
    val categoryName: String,
    val categoryColor: Color,
    val scoreDelta: Int,
    val satietyDelta: Int,
    val energyDelta: Int,
    val moodDelta: Int,
    val muscleDelta: Int,
    val fatDelta: Int,
    val reactionText: String,
    var xPercent: Float,
    var yPercent: Float,
    var speed: Float
) {
    fun displayTitle(isEn: Boolean): String = if (!isEn) title else when (id) {
        0 -> "Triple Cheeseburger"
        1 -> "Pepperoni Pizza"
        2 -> "Donut"
        3 -> "French Fries"
        4 -> "Broccoli"
        5 -> "Barbell"
        6 -> "Apple"
        7 -> "Avocado"
        8 -> "Soup"
        9 -> "Milk"
        10 -> "Sandwich"
        else -> title
    }

    fun displayCategoryName(isEn: Boolean): String = if (!isEn) categoryName else when (categoryName) {
        "Фастфуд" -> "Fast Food"
        "ПП" -> "Healthy"
        "Качалка" -> "Gym"
        "Обычная еда" -> "Regular Food"
        else -> categoryName
    }

    fun displayReactionText(isEn: Boolean): String = if (!isEn) reactionText else when (id) {
        0 -> "«Nom nom nom! Juicy burger! 🍔»"
        1 -> "«Pizza! The box will come in handy... 🍕📦»"
        2 -> "«Sweet donut! 🍩»"
        3 -> "«Crispy fries! 🍟»"
        4 -> "«Vitamins and Mogging! 🥦»"
        5 -> "«150kg bench press! Mewing active! 🏋️‍♂️💪»"
        6 -> "«Juicy apple for abs! 🍏»"
        7 -> "«Healthy fats! 🥑»"
        8 -> "«Homemade soup — hearty and delicious! 🍲»"
        9 -> "«Fresh milk! 🥛»"
        10 -> "«Hearty sandwich! 🥪»"
        else -> reactionText
    }
}

private val FOOD_TEMPLATES = listOf(
    // Fastfood (Category: Фастфуд)
    FallingFoodItem(0, "🍔", "Тройной Чизбургер", "Фастфуд", Color(0xFFF97316), -14, +15, -2, +10, -2, +12, "«Ом-ном-ном! Сочный бургер! 🍔»", 0f, 0f, 0f),
    FallingFoodItem(1, "🍕", "Пицца Пепперони", "Фастфуд", Color(0xFFEA580C), -12, +12, -2, +8, -2, +10, "«Пицца! Коробка мне пригодится... 🍕📦»", 0f, 0f, 0f),
    FallingFoodItem(2, "🍩", "Пончик", "Фастфуд", Color(0xFFEC4899), -16, +10, -1, +12, -3, +14, "«Сладкий пончик! 🍩»", 0f, 0f, 0f),
    FallingFoodItem(3, "🍟", "Картошка Фри", "Фастфуд", Color(0xFFEAB308), -10, +8, -1, +6, -1, +8, "«Хрустящая картошечка! 🍟»", 0f, 0f, 0f),

    // Healthy/PP (Category: ПП / Качалка)
    FallingFoodItem(4, "🥦", "Брокколи", "ПП", Color(0xFF10B981), +12, +8, +8, +4, +10, -5, "«Витамины и Моггинг! 🥦»", 0f, 0f, 0f),
    FallingFoodItem(5, "🏋️‍♂️", "Штанга", "Качалка", Color(0xFF059669), +16, +5, +12, +8, +16, -8, "«Жим 150 кг! Мьюинг активен! 🏋️‍♂️💪»", 0f, 0f, 0f),
    FallingFoodItem(6, "🍏", "Яблоко", "ПП", Color(0xFF22C55E), +8, +6, +6, +6, +6, -4, "«Сочное яблоко для пресса! 🍏»", 0f, 0f, 0f),
    FallingFoodItem(7, "🥑", "Авокадо", "ПП", Color(0xFF16A34A), +10, +8, +6, +5, +8, -3, "«Полезные жиры! 🥑»", 0f, 0f, 0f),

    // Regular Food (Category: Обычная еда)
    FallingFoodItem(8, "🍲", "Супчик", "Обычная еда", Color(0xFF3B82F6), 0, +12, +6, +8, +2, +1, "«Домашний суп — сытно и вкусно! 🍲»", 0f, 0f, 0f),
    FallingFoodItem(9, "🥛", "Молоко", "Обычная еда", Color(0xFF60A5FA), 0, +8, +5, +6, +3, +0, "«Парное молоко! 🥛»", 0f, 0f, 0f),
    FallingFoodItem(10, "🥪", "Сэндвич", "Обычная еда", Color(0xFF8B5CF6), 0, +10, +4, +6, +2, +2, "«Сытный сэндвич! 🥪»", 0f, 0f, 0f)
)

private fun spawnRandomFood(id: Int): FallingFoodItem {
    val template = FOOD_TEMPLATES.random()
    return template.copy(
        id = id,
        xPercent = (0.12f + (0.76f * Math.random().toFloat())),
        yPercent = -0.1f - (Math.random().toFloat() * 0.4f),
        speed = 0.007f + (Math.random().toFloat() * 0.008f)
    )
}

@Composable
fun VerityCareGameDialog(
    settings: UserSettings,
    onUpdateSettings: (UserSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isEn = com.example.util.LocalAppLanguage.current.equals("en", ignoreCase = true)
    val currentSettings by rememberUpdatedState(settings)

    val score = settings.verityFormScore.coerceIn(-100, 100)
    val stage = remember(score) { VerityStage.fromScore(score) }

    var speechText by remember(stage, isEn) {
        mutableStateOf(stage.displayQuotes(isEn).first())
    }
    var floatingBadgeText by remember { mutableStateOf<String?>(null) }

    val avatarScale = remember { Animatable(1f) }
    val avatarRotation = remember { Animatable(0f) }

    fun triggerBounce(reaction: String, badge: String) {
        speechText = reaction
        floatingBadgeText = badge
        HapticHelper.performLightHaptic(context, true)
        scope.launch {
            avatarScale.snapTo(0.86f)
            avatarScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        scope.launch {
            avatarRotation.snapTo(-6f)
            avatarRotation.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    fun catchFood(food: FallingFoodItem) {
        val current = currentSettings
        val oldStage = VerityStage.fromScore(current.verityFormScore)
        val newScore = (current.verityFormScore + food.scoreDelta).coerceIn(-100, 100)
        val newStage = VerityStage.fromScore(newScore)

        val newSatiety = (current.veritySatiety + food.satietyDelta).coerceIn(0, 100)
        val newEnergy = (current.verityEnergy + food.energyDelta).coerceIn(0, 100)
        val newMood = (current.verityMood + food.moodDelta).coerceIn(0, 100)
        val newMuscle = (current.verityMuscleMass + food.muscleDelta).coerceIn(0, 100)
        val newFat = (current.verityFatMass + food.fatDelta).coerceIn(0, 100)
        val newTotal = current.verityTotalActions + 1

        onUpdateSettings(
            current.copy(
                verityUnlocked = true,
                verityFormScore = newScore,
                veritySatiety = newSatiety,
                verityEnergy = newEnergy,
                verityMood = newMood,
                verityMuscleMass = newMuscle,
                verityFatMass = newFat,
                verityTotalActions = newTotal
            )
        )

        val finalSpeech = if (oldStage != newStage) {
            when (newStage) {
                VerityStage.OBESITY -> if (isEn) "🎉 EVOLUTION! Verity overate and turned into OBESSITY in a box! 📦🍔" else "🎉 ЭВОЛЮЦИЯ! Верити отъелся и превратился в ОБЕССИТИ в коробке! 📦🍔"
                VerityStage.MOGGITY -> if (isEn) "🔥 EVOLUTION! From barbells and healthy food Verity became giga-sigma MOGGITI! 🤫🧏‍♂️" else "🔥 ЭВОЛЮЦИЯ! От штанги и полезной еды Верити стал гига-сигмой МОГГИТИ! 🤫🧏‍♂️"
                VerityStage.VERITY -> if (isEn) "✨ Verity returned to his classic round form! 😊" else "✨ Верити вернулся в свою классическую круглую форму! 😊"
            }
        } else {
            food.displayReactionText(isEn)
        }

        val effectText = when {
            food.scoreDelta < 0 -> "🍔 ${food.displayCategoryName(isEn)} (${food.scoreDelta})"
            food.scoreDelta > 0 -> "🥦 ${food.displayCategoryName(isEn)} (+${food.scoreDelta})"
            else -> "🍲 ${food.displayTitle(isEn)}"
        }

        triggerBounce(
            reaction = finalSpeech,
            badge = "${food.emoji} $effectText"
        )
    }

    // Lawn & Falling Game state
    var activeFoods by remember {
        mutableStateOf(List(4) { spawnRandomFood(it) })
    }
    var verityXPercent by remember { mutableFloatStateOf(0.5f) }
    var verityYPercent by remember { mutableFloatStateOf(0.72f) }
    var isDraggingVerity by remember { mutableStateOf(false) }

    // Auto-movement & Falling loop
    LaunchedEffect(Unit) {
        var animTime = 0f
        while (true) {
            delay(16)
            animTime += 0.03f

            // If user is not dragging Verity, Verity hops and moves automatically on lawn
            if (!isDraggingVerity) {
                verityXPercent = 0.5f + (sin(animTime * 1.6f) * 0.35f)
                verityYPercent = 0.72f - (abs(sin(animTime * 4.5f)) * 0.08f)
            }

            // Update falling food positions
            val updatedList = activeFoods.map { food ->
                val newY = food.yPercent + food.speed
                if (newY > 1.05f) {
                    // Respawn food at top
                    spawnRandomFood(food.id)
                } else {
                    // Collision check with Verity
                    val dx = food.xPercent - verityXPercent
                    val dy = food.yPercent - verityYPercent
                    val distSq = (dx * dx) + (dy * dy)
                    if (distSq < 0.016f) { // Collision radius
                        catchFood(food)
                        spawnRandomFood(food.id)
                    } else {
                        food.copy(yPercent = newY)
                    }
                }
            }
            activeFoods = updatedList
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 12.dp,
            border = BorderStroke(1.5.dp, stage.accentColor.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Header Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    stage.accentColor.copy(alpha = 0.22f),
                                    stage.secondaryColor.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = stage.accentColor.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, stage.accentColor.copy(alpha = 0.5f)),
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("😊", fontSize = 20.sp)
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isEn) "Verity's Adventures" else "Приключения Верити",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = if (isEn) "Close" else "Закрыть"
                            )
                        }
                    }
                }

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Stage Badge
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = stage.accentColor.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, stage.accentColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "${stage.displayBadge(isEn)} • ${stage.displayTitle(isEn).uppercase()}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = stage.accentColor,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Evolution Scale Bar (-100 Obesity <-> 0 Verity <-> +100 Moggity)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📦 " + (if (isEn) "Obessity" else "Обессити"),
                                    fontSize = 11.sp,
                                    fontWeight = if (stage == VerityStage.OBESITY) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (stage == VerityStage.OBESITY) Color(0xFFF97316) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "😊 " + (if (isEn) "Verity" else "Верити") + " ($score)",
                                    fontSize = 11.sp,
                                    fontWeight = if (stage == VerityStage.VERITY) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (stage == VerityStage.VERITY) Color(0xFFEAB308) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "🏋️ " + (if (isEn) "Moggiti" else "Моггити"),
                                    fontSize = 11.sp,
                                    fontWeight = if (stage == VerityStage.MOGGITY) FontWeight.ExtraBold else FontWeight.Medium,
                                    color = if (stage == VerityStage.MOGGITY) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Progress Track with pointer
                            val normalizedProgress by animateFloatAsState(
                                targetValue = ((score + 100f) / 200f).coerceIn(0f, 1f),
                                animationSpec = tween(260),
                                label = "score_progress"
                            )

                            BoxWithConstraints(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(18.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                val trackWidth = this.maxWidth
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    Color(0xFFF97316),
                                                    Color(0xFFEAB308),
                                                    Color(0xFF10B981)
                                                )
                                            )
                                        )
                                )
                                val thumbOffset = (trackWidth - 18.dp) * normalizedProgress
                                Box(
                                    modifier = Modifier
                                        .offset(x = thumbOffset)
                                        .size(18.dp)
                                        .shadow(4.dp, CircleShape)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(3.dp, stage.accentColor, CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Stat Pills Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                VerityStatPill(
                                    label = if (isEn) "Fullness" else "Сытость",
                                    value = "${settings.veritySatiety}%",
                                    icon = "🍽️",
                                    color = Color(0xFFF59E0B),
                                    modifier = Modifier.weight(1f)
                                )
                                VerityStatPill(
                                    label = if (isEn) "Muscles" else "Мышцы",
                                    value = "${settings.verityMuscleMass}%",
                                    icon = "💪",
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.weight(1f)
                                )
                                VerityStatPill(
                                    label = if (isEn) "Weight" else "Масса",
                                    value = "${settings.verityFatMass}%",
                                    icon = "🍔",
                                    color = Color(0xFFF97316),
                                    modifier = Modifier.weight(1f)
                                )
                                VerityStatPill(
                                    label = if (isEn) "Mood" else "Настрой",
                                    value = "${settings.verityMood}%",
                                    icon = "💛",
                                    color = Color(0xFFEC4899),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // LAWN PLAYGROUND MINI-GAME CANVAS
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .border(2.dp, stage.accentColor.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF38BDF8).copy(alpha = 0.35f), // Sky
                                        Color(0xFF818CF8).copy(alpha = 0.15f),
                                        Color(0xFF22C55E).copy(alpha = 0.5f),  // Lawn
                                        Color(0xFF15803D).copy(alpha = 0.85f)
                                    )
                                )
                            )
                    ) {
                        val canvasWidth = this.maxWidth
                        val canvasHeight = this.maxHeight

                        // Instruction overlay at top of lawn
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.Black.copy(alpha = 0.45f),
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp, start = 12.dp, end = 12.dp)
                        ) {
                            Text(
                                text = if (isEn) "🖐️ Drag Verity to catch falling food!\n🍔 Fast Food = Obessity • 🥦 Healthy = Moggiti • 🍲 Regular = Balance" else "🖐️ Зажмите и тяните Верити, чтобы ловить падающую еду!\n🍔 Фастфуд = Обессити • 🥦 ПП = Моггити • 🍲 Обычная = Баланс",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }

                        // Render falling food items
                        activeFoods.forEach { food ->
                            val foodX = (canvasWidth - 44.dp) * food.xPercent.coerceIn(0f, 1f)
                            val foodY = canvasHeight * food.yPercent.coerceIn(-0.1f, 1.1f)

                            Surface(
                                shape = CircleShape,
                                color = food.categoryColor.copy(alpha = 0.25f),
                                border = BorderStroke(1.5.dp, food.categoryColor),
                                modifier = Modifier
                                    .offset { IntOffset(foodX.roundToPx(), foodY.roundToPx()) }
                                    .size(42.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = food.emoji, fontSize = 22.sp)
                                }
                            }
                        }

                        // Render Verity character (Draggable / Jumping)
                        val verityX = (canvasWidth - 84.dp) * verityXPercent.coerceIn(0f, 1f)
                        val verityY = (canvasHeight - 84.dp) * verityYPercent.coerceIn(0f, 1f)

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .offset { IntOffset(verityX.roundToPx(), verityY.roundToPx()) }
                                .size(84.dp)
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { isDraggingVerity = true },
                                        onDragEnd = { isDraggingVerity = false },
                                        onDragCancel = { isDraggingVerity = false },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            isDraggingVerity = true
                                            val dx = dragAmount.x / (canvasWidth.toPx() - 84.dp.toPx())
                                            val dy = dragAmount.y / (canvasHeight.toPx() - 84.dp.toPx())
                                            verityXPercent = (verityXPercent + dx).coerceIn(0.05f, 0.95f)
                                            verityYPercent = (verityYPercent + dy).coerceIn(0.10f, 0.90f)
                                        }
                                    )
                                }
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = stage.accentColor.copy(alpha = 0.2f),
                                border = BorderStroke(3.dp, stage.accentColor),
                                shadowElevation = 8.dp,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = avatarScale.value
                                        scaleY = avatarScale.value
                                        rotationZ = avatarRotation.value
                                    }
                            ) {
                                Image(
                                    painter = painterResource(id = stage.drawableRes),
                                    contentDescription = stage.displayTitle(isEn),
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Speech Bubble & Reaction
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, stage.accentColor.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = speechText,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stage.displayDescription(isEn),
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            AnimatedVisibility(
                                visible = floatingBadgeText != null,
                                enter = fadeIn() + scaleIn(),
                                exit = fadeOut() + scaleOut()
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = stage.accentColor,
                                    modifier = Modifier.padding(top = 6.dp)
                                ) {
                                    Text(
                                        text = floatingBadgeText ?: "",
                                        color = Color.Black,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Reset Form Button
                    OutlinedButton(
                        onClick = {
                            onUpdateSettings(
                                settings.copy(
                                    verityFormScore = 0,
                                    veritySatiety = 75,
                                    verityEnergy = 85,
                                    verityMood = 95,
                                    verityMuscleMass = 25,
                                    verityFatMass = 25
                                )
                            )
                            triggerBounce(
                                reaction = if (isEn) "«Phew! I'm regular classic Verity again! 😊»" else "«Фух! Я снова обычный классический Верити! 😊»",
                                badge = if (isEn) "🔄 Reset Form" else "🔄 Сброс формы"
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isEn) "Reset Verity to default form (Photo 1)" else "Вернуть обычную форму Верити (Фото 1)")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Hotbar Smiley Button Visibility Setting inside Game
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Text(
                                    text = if (isEn) "Verity Emoji Button (😊) in Hotbar" else "Кнопка со смайликом 😊 в хотбаре",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isEn) "If turned off, emoji button in hotbar will be hidden" else "Если выключить, кнопка со смайликом в хотбаре будет скрыта",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.showVerityInHotbar,
                                onCheckedChange = { show ->
                                    onUpdateSettings(
                                        settings.copy(
                                            verityUnlocked = true,
                                            showVerityInHotbar = show,
                                            isHotbarVisible = if (show) true else settings.isHotbarVisible
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VerityStatPill(
    label: String,
    value: String,
    icon: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "$icon $value", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
