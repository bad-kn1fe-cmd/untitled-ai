package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class HotbarItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val badgeCount: Int = 0
)

@Composable
fun PillNavigationHotbar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    uiVariant: String = "vault_hub",
    hapticsEnabled: Boolean = true,
    animationMode: String = "spring",
    showVerityButton: Boolean = false,
    onOpenVerityGame: () -> Unit = {}
) {
    val context = LocalContext.current

    val isEn = com.example.util.LocalAppLanguage.current.equals("en", ignoreCase = true)

    val items = remember(uiVariant, isEn) {
        if (uiVariant == "vault_hub") {
            listOf(
                HotbarItem(
                    route = "vault_general",
                    title = if (isEn) "Hub" else "Общее",
                    icon = Icons.Default.Inventory2
                ),
                HotbarItem(
                    route = "vault_chats",
                    title = if (isEn) "Chats" else "Чаты",
                    icon = Icons.AutoMirrored.Filled.Chat
                ),
                HotbarItem(
                    route = "personas",
                    title = if (isEn) "Personas" else "Персоны",
                    icon = Icons.Default.AccountCircle
                ),
                HotbarItem(
                    route = "providers",
                    title = if (isEn) "AI Keys" else "ИИ Ключи",
                    icon = Icons.Default.Hub
                ),
                HotbarItem(
                    route = "settings",
                    title = if (isEn) "Settings" else "Настройки",
                    icon = Icons.Default.Settings
                )
            )
        } else {
            listOf(
                HotbarItem(
                    route = "characters",
                    title = if (isEn) "Chats" else "Чаты",
                    icon = Icons.AutoMirrored.Filled.Chat
                ),
                HotbarItem(
                    route = "personas",
                    title = if (isEn) "Personas" else "Персоны",
                    icon = Icons.Default.AccountCircle
                ),
                HotbarItem(
                    route = "providers",
                    title = if (isEn) "AI Keys" else "ИИ Ключи",
                    icon = Icons.Default.Hub
                ),
                HotbarItem(
                    route = "settings",
                    title = if (isEn) "Settings" else "Настройки",
                    icon = Icons.Default.Settings
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 10.dp,
            shadowElevation = 8.dp,
            border = BorderStroke(
                width = 1.2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
            ),
            modifier = Modifier
                .wrapContentWidth()
                .height(56.dp)
        ) {
            val density = LocalDensity.current
            val itemPositions = remember { mutableStateMapOf<Int, Pair<Float, Float>>() }

            val selectedIndex = remember(items, currentRoute) {
                val idx = items.indexOfFirst { it.route == currentRoute }
                if (idx >= 0) idx else 0
            }

            val targetCoords = itemPositions[selectedIndex]
            val targetLeftPx = targetCoords?.first ?: 0f
            val targetWidthPx = targetCoords?.second ?: 0f

            val animatedLeftPx by animateFloatAsState(
                targetValue = targetLeftPx,
                animationSpec = if (animationMode == "instant") androidx.compose.animation.core.snap() else spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "pillFloatLeft"
            )

            val animatedWidthPx by animateFloatAsState(
                targetValue = targetWidthPx,
                animationSpec = if (animationMode == "instant") androidx.compose.animation.core.snap() else spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                ),
                label = "pillFloatWidth"
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // Floating sliding pill indicator that smoothly glides underneath tabs
                if (animatedWidthPx > 0f) {
                    Box(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = animatedLeftPx
                            }
                            .width(with(density) { animatedWidthPx.toDp() })
                            .height(44.dp)
                            .shadow(
                                elevation = 6.dp,
                                shape = RoundedCornerShape(22.dp),
                                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                            )
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(22.dp)
                            )
                    )
                }

                Row(
                    modifier = Modifier.wrapContentWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, item ->
                        val isSelected = currentRoute == item.route

                        val contentColor by animateColorAsState(
                            targetValue = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = if (animationMode == "instant") androidx.compose.animation.core.snap() else spring(stiffness = Spring.StiffnessMediumLow),
                            label = "pillContentColor"
                        )

                        Box(
                            modifier = Modifier
                                .height(44.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .onGloballyPositioned { coordinates ->
                                    val pos = coordinates.positionInParent()
                                    val widthPx = coordinates.size.width.toFloat()
                                    itemPositions[index] = Pair(pos.x, widthPx)
                                }
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (!isSelected) {
                                        HapticHelper.performLightHaptic(context, hapticsEnabled)
                                        onNavigate(item.route)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = if (isSelected) 14.dp else 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = contentColor,
                                    modifier = Modifier.size(20.dp)
                                )

                                if (isSelected) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.title,
                                        color = contentColor,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = showVerityButton,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(start = 2.dp)
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    color = Color(0xFFEAB308).copy(alpha = 0.22f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    HapticHelper.performLightHaptic(context, hapticsEnabled)
                                    onOpenVerityGame()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.verity_normal),
                                contentDescription = "Игра Верити",
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}
