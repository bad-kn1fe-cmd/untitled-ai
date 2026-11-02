package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun GeminiTutorialDialog(
    initialStep: Int = 0, // 0 = Welcome offer, 1..3 = Tutorial steps, 4 = Enter API key
    defaultModel: String = "gemini-3.5-flash-lite",
    onDismiss: () -> Unit,
    onCompleteWithApiKey: (apiKey: String, model: String) -> Unit
) {
    var currentStep by rememberSaveable { mutableIntStateOf(initialStep) }
    var apiKeyInput by rememberSaveable { mutableStateOf("") }
    var selectedModel by rememberSaveable { mutableStateOf(defaultModel.ifBlank { "gemini-3.5-flash-lite" }) }
    var isKeyVisible by rememberSaveable { mutableStateOf(false) }
    var keyError by remember { mutableStateOf<String?>(null) }

    val isEn = com.example.util.LocalAppLanguage.current.equals("en", ignoreCase = true)
    val uriHandler = LocalUriHandler.current
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val openUrlSafely: (String) -> Unit = { url ->
        try {
            uriHandler.openUri(url)
        } catch (_: Exception) {
            Toast.makeText(context, if (isEn) "Failed to open URL: $url" else "Не удалось открыть ссылку: $url", Toast.LENGTH_SHORT).show()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 10.dp,
            shadowElevation = 18.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth(0.94f)
                .heightIn(max = 680.dp)
                .testTag("gemini_tutorial_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header with Step Indicator and Close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF4285F4),
                                            Color(0xFF9B72CB),
                                            Color(0xFFD96570)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (currentStep == 0) (if (isEn) "Welcome to Untitled!" else "Добро пожаловать в Untitled!") else (if (isEn) "Google Gemini Setup" else "Подключение Google Gemini"),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = when (currentStep) {
                                    0 -> if (isEn) "Quick Start • Free AI Provider" else "Быстрый старт • Бесплатный ИИ-провайдер"
                                    1 -> if (isEn) "Step 1 of 4 • Sign in to Google AI Studio" else "Шаг 1 из 4 • Вход в Google AI Studio"
                                    2 -> if (isEn) "Step 2 of 4 • Create API key" else "Шаг 2 из 4 • Создание API-ключа"
                                    3 -> if (isEn) "Step 3 of 4 • Copy key" else "Шаг 3 из 4 • Копирование ключа"
                                    else -> if (isEn) "Step 4 of 4 • Auto connect" else "Шаг 4 из 4 • Автоматическое подключение"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(34.dp)
                            .testTag("gemini_tutorial_close_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = if (isEn) "Close" else "Закрыть")
                    }
                }

                // Step progress bar for steps 1..4
                if (currentStep > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (stepIdx in 1..4) {
                            val isActive = stepIdx <= currentStep
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isActive) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { currentStep = stepIdx }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Main Scrollable Content
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth(),
                    label = "gemini_tutorial_step"
                ) { step ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (step) {
                            0 -> {
                                // STEP 0: First-launch offer to watch the Gemini Tutorial
                                WebsitePreviewCardWelcome()

                                Text(
                                    text = if (isEn) "Would you like to watch a short tutorial on connecting Google Gemini for free?" else "Хотите посмотреть короткий туториал по бесплатному подключению Google Gemini?",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = if (isEn) "To chat with characters and use the AI assistant, the app needs an API key. We prepared a step-by-step visual guide with pictures of Google AI Studio and direct navigation buttons." else "Для общения с персонажами и работы ИИ-помощника приложению нужен API-ключ. Мы подготовили наглядную пошаговую инструкцию с картинками сайта Google AI Studio и прямыми кнопками перехода.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Verified,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isEn) "What awaits you in this tutorial:" else "Что вас ждёт в обучении:",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = if (isEn) "• Clear illustrations of Google AI Studio interface\n• Quick navigation buttons to key pages\n• Auto-setup of provider with default model gemini-3.5-flash-lite" else "• Наглядные иллюстрации интерфейса сайта Google AI Studio\n• Быстрые кнопки перехода сразу на нужные страницы\n• Автоматическое добавление провайдера с моделью по умолчанию gemini-3.5-flash-lite",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Button(
                                    onClick = { currentStep = 1 },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("start_gemini_tutorial_btn")
                                ) {
                                    Icon(Icons.Default.School, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isEn) "Watch Tutorial (1 minute)" else "Посмотреть туториал (1 минута)", fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { currentStep = 4 },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                        .testTag("skip_to_gemini_key_btn")
                                ) {
                                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isEn) "I already have a key — Enter now" else "У меня уже есть ключ — ввести сразу")
                                }

                                TextButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .testTag("dismiss_gemini_tutorial_btn")
                                ) {
                                    Text(if (isEn) "Skip (can open later in Providers section)" else "Пропустить (можно открыть позже в разделе «Провайдеры»)", fontSize = 12.sp)
                                }
                            }

                            1 -> {
                                // STEP 1: Open Google AI Studio & Sign In
                                WebsiteMockupStep1(
                                    onOpenSite = { openUrlSafely("https://aistudio.google.com/app/apikey") }
                                )

                                Text(
                                    text = if (isEn) "1. Open Google AI Studio website" else "1. Откройте сайт Google AI Studio",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = if (isEn) "Go to official Google AI Studio website (aistudio.google.com) and sign in with your regular Google account. On first sign-in, accept Terms of Service → Continue." else "Перейдите на официальный сайт Google AI Studio (aistudio.google.com) и войдите в свой обычный аккаунт Google. При первом входе примите условия использования (галочка Terms of Service → Continue).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )

                                // Direct Navigation Buttons
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { openUrlSafely("https://aistudio.google.com/app/apikey") },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (isEn) "Open Keys Page (aistudio.google.com)" else "Открыть страницу ключей (aistudio.google.com)", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { openUrlSafely("https://aistudio.google.com/") },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (isEn) "Google AI Studio Home" else "Главная страница Google AI Studio", fontSize = 12.sp)
                                    }
                                }
                            }

                            2 -> {
                                // STEP 2: Click "Create API key"
                                WebsiteMockupStep2(
                                    onOpenApiKeyPage = { openUrlSafely("https://aistudio.google.com/app/apikey") }
                                )

                                Text(
                                    text = if (isEn) "2. Tap 'Create API key'" else "2. Нажмите «Create API key»",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = if (isEn) "In the 'API keys' section, click blue 'Create API key' button. In popup window, select 'Create API key in new project' — the key will be generated for free in a couple of seconds." else "В разделе «API keys» нажмите синюю кнопку «Create API key». Во всплывающем окне выберите «Create API key in new project» (Создать ключ в новом проекте) — ключ сгенерируется бесплатно за пару секунд.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { openUrlSafely("https://aistudio.google.com/app/apikey") },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A73E8)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (isEn) "Go to 'Create API key' button" else "Перейти к кнопке «Create API key»", color = Color.White, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { openUrlSafely("https://ai.google.dev/gemini-api/docs/api-key") },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(if (isEn) "Official Gemini Key Documentation" else "Официальная справка по ключам Gemini", fontSize = 12.sp)
                                    }
                                }
                            }

                            3 -> {
                                // STEP 3: Copy the generated AIzaSy... key
                                WebsiteMockupStep3()

                                Text(
                                    text = if (isEn) "3. Copy generated key" else "3. Скопируйте полученный ключ",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = if (isEn) "When 'API key generated' appears, you will see a long string starting with 'AIzaSy...'. Tap 'Copy' button to copy key to phone clipboard, and move to next step!" else "Когда появится окно «API key generated», вы увидите длинную строку, начинающуюся на «AIzaSy...». Нажмите кнопку «Copy», чтобы скопировать ключ в буфер обмена телефона, и переходите к следующему шагу!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { openUrlSafely("https://aistudio.google.com/app/apikey") },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isEn) "My Keys" else "Мои ключи", fontSize = 11.5.sp)
                                    }
                                    OutlinedButton(
                                        onClick = { openUrlSafely("https://ai.google.dev/gemini-api/docs/rate-limits") },
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isEn) "Free Tier Limits" else "Лимиты Free Tier", fontSize = 11.5.sp)
                                    }
                                }
                            }

                            4 -> {
                                // STEP 4: Paste API key & Auto-add Gemini Provider
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Column {
                                            Text(
                                                text = if (isEn) "Auto Connect Gemini" else "Автоматическое подключение Gemini",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = if (isEn) "Default model: gemini-3.5-flash-lite" else "Модель по умолчанию: gemini-3.5-flash-lite",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = if (isEn) "Enter or paste your API key below. The app will automatically create a Google Gemini profile, set gemini-3.5-flash-lite model, and make it default provider for chats!" else "Введите или вставьте полученный API-ключ ниже. Приложение автоматически создаст профиль Google Gemini, установит модель gemini-3.5-flash-lite и сделает его основным провайдером для чатов!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 18.sp
                                )

                                OutlinedTextField(
                                    value = apiKeyInput,
                                    onValueChange = {
                                        apiKeyInput = it
                                        keyError = null
                                    },
                                    label = { Text(if (isEn) "Your Google Gemini API Key" else "Ваш Google Gemini API ключ") },
                                    placeholder = { Text("AIzaSy...") },
                                    singleLine = true,
                                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                                Icon(
                                                    imageVector = if (isKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                    contentDescription = if (isEn) "Toggle key visibility" else "Показать/скрыть ключ"
                                                )
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("gemini_tutorial_apikey_input")
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FilledTonalButton(
                                        onClick = {
                                            val clipText = clipboardManager.getText()?.text?.trim().orEmpty()
                                            if (clipText.isNotBlank()) {
                                                apiKeyInput = clipText
                                                keyError = null
                                                Toast.makeText(context, if (isEn) "Key pasted from clipboard" else "Ключ вставлен из буфера обмена", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, if (isEn) "Clipboard is empty" else "Буфер обмена пуст", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isEn) "Paste from Clipboard" else "Вставить из буфера", fontSize = 12.sp)
                                    }

                                    TextButton(
                                        onClick = { openUrlSafely("https://aistudio.google.com/app/apikey") }
                                    ) {
                                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isEn) "Open AI Studio" else "Открыть AI Studio", fontSize = 12.sp)
                                    }
                                }

                                // Model selection (defaults to gemini-3.5-flash-lite)
                                Text(
                                    text = if (isEn) "Google Gemini API Model:" else "Модель Google Gemini API:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                val geminiModels = listOf(
                                    "gemini-3.5-flash-lite" to (if (isEn) "gemini-3.5-flash-lite (Default)" else "gemini-3.5-flash-lite (По умолчанию)"),
                                    "gemini-2.5-flash" to "gemini-2.5-flash",
                                    "gemini-2.5-pro" to "gemini-2.5-pro",
                                    "gemini-2.0-flash" to "gemini-2.0-flash"
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(geminiModels) { (modelId, label) ->
                                        FilterChip(
                                            selected = selectedModel == modelId,
                                            onClick = { selectedModel = modelId },
                                            label = { Text(label, fontSize = 11.5.sp) },
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }
                                }

                                if (keyError != null) {
                                    Text(
                                        text = keyError!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }

                                Button(
                                    onClick = {
                                        val cleanKey = apiKeyInput.trim()
                                        if (cleanKey.length < 10) {
                                            keyError = if (isEn) "Please enter a valid Google Gemini API key (usually starts with AIzaSy...)" else "Пожалуйста, введите корректный API-ключ Google Gemini (обычно начинается с AIzaSy...)"
                                            return@Button
                                        }
                                        onCompleteWithApiKey(cleanKey, selectedModel.trim().ifBlank { "gemini-3.5-flash-lite" })
                                    },
                                    enabled = apiKeyInput.isNotBlank(),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("gemini_tutorial_save_key_btn")
                                ) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isEn) "Connect Gemini Automatically" else "Подключить Gemini автоматически", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Bottom Step Navigation Bar for Steps 1..4
                if (currentStep > 0) {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (currentStep > 0) currentStep -= 1
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isEn) "Back" else "Назад")
                        }

                        if (currentStep < 4) {
                            Button(
                                onClick = { currentStep += 1 },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("gemini_tutorial_next_btn")
                            ) {
                                Text(
                                    text = if (currentStep == 3) (if (isEn) "To Key Entry" else "К вводу ключа") else (if (isEn) "Next" else "Далее"),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        } else {
                            TextButton(onClick = onDismiss) {
                                Text(if (isEn) "Finish Without Key" else "Завершить без ключа")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WebsitePreviewCardWelcome() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0F141C),
        border = BorderStroke(1.dp, Color(0xFF4285F4).copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Browser Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF27C93F)))
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF1E2633),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "🔒 aistudio.google.com/app/apikey",
                        color = Color(0xFF9AA0A6),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF8AB4F8), modifier = Modifier.size(14.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Google AI Studio",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Бесплатный ключ за 3 клика",
                        color = Color(0xFF8AB4F8),
                        fontSize = 11.5.sp
                    )
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1A73E8)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("Get API key", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF1B222D),
                border = BorderStroke(1.dp, Color(0xFF34A853).copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "⚡ Модель по умолчанию:",
                        color = Color(0xFFE8EAED),
                        fontSize = 11.sp
                    )
                    Text(
                        text = "gemini-3.5-flash-lite",
                        color = Color(0xFF81C995),
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun WebsiteMockupStep1(onOpenSite: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0E131A),
        border = BorderStroke(1.dp, Color(0xFF8AB4F8).copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenSite() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Browser URL bar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1C2430))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF81C995), modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "https://aistudio.google.com/app/apikey",
                    color = Color(0xFFE8EAED),
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mockup of Google AI Studio Left Menu + Main Area
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Left sidebar mockup
                Column(
                    modifier = Modifier
                        .width(120.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF161D27))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Google AI Studio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1A73E8).copy(alpha = 0.28f),
                        border = BorderStroke(1.dp, Color(0xFF8AB4F8))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF8AB4F8), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Get API key", color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("Chat Prompt", color = Color(0xFF9AA0A6), fontSize = 9.sp)
                    Text("Dashboard", color = Color(0xFF9AA0A6), fontSize = 9.sp)
                }

                // Right content area mockup
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF161D27))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = Color(0xFFEA4335)) {
                            Text(
                                text = " 1 ",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Войдите в аккаунт Google", color = Color.White, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Откройте меню слева и нажмите «Get API key» (или перейдите по прямой кнопке ниже).",
                        color = Color(0xFFBDC1C6),
                        fontSize = 10.sp,
                        lineHeight = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun WebsiteMockupStep2(onOpenApiKeyPage: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0E131A),
        border = BorderStroke(1.dp, Color(0xFF8AB4F8).copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenApiKeyPage() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("API keys", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Quickly test the Gemini API", color = Color(0xFF9AA0A6), fontSize = 10.sp)
                }

                // Highlighted "+ Create API key" button as on aistudio.google.com
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1A73E8),
                    border = BorderStroke(2.dp, Color(0xFFFDD663))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Create API key", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Modal box inside website mockup
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF1E2634),
                border = BorderStroke(1.dp, Color(0xFF81C995).copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Окно на сайте: Create a new key",
                        color = Color(0xFFE8EAED),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1A73E8).copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, Color(0xFF8AB4F8)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "👉 Create API key in new project",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                            Icon(Icons.Default.TouchApp, contentDescription = null, tint = Color(0xFFFDD663), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WebsiteMockupStep3() {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF0E131A),
        border = BorderStroke(1.dp, Color(0xFF81C995).copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "API key generated",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Text(
                text = "Your API key is ready to use:",
                color = Color(0xFF9AA0A6),
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF1A222D))
                    .border(1.dp, Color(0xFF3C4043), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "AIzaSyD9xK2mN8pQ4vR7...wXz9L",
                    color = Color(0xFF81C995),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.5.sp
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1A73E8),
                    border = BorderStroke(1.5.dp, Color(0xFFFDD663))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "💡 Нажмите «Copy» на сайте, а затем кнопку «К вводу ключа» ниже!",
                color = Color(0xFFFDD663),
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
