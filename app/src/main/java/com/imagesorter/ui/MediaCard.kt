package com.imagesorter.ui

import android.widget.VideoView
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.imagesorter.data.MediaOps
import com.imagesorter.domain.MediaItem
import com.imagesorter.domain.MediaKind
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Foto o video al frente del mazo. Tocar la mitad izquierda o deslizar a la izquierda = borrar; la mitad
 * derecha o deslizar a la derecha = conservar ([swapSides] los invierte). Un deslizamiento corto vuelve a su
 * lugar sin decidir. Los videos se reproducen solos aquí mismo; el botón central los pausa o reanuda.
 * @param swipeThreshold cuánto hay que deslizar para decidir, como fracción del ancho.
 * @param tapToDecide si tocar una mitad decide; si no, solo deslizar. Las etiquetas siguen mostrando cada lado.
 */
@Composable
fun MediaCard(
    item: MediaItem,
    onTrash: () -> Unit,
    onKeep: () -> Unit,
    modifier: Modifier = Modifier,
    swipeThreshold: Float = AppSettings.SWIPE_DEFAULT,
    swapSides: Boolean = false,
    tapToDecide: Boolean = true,
) {
    // Los videos se reproducen solos al aparecer; el botón los pausa.
    var playing by remember(item.key) { mutableStateOf(item.kind == MediaKind.VIDEO) }

    val onLeft by rememberUpdatedState(if (swapSides) onKeep else onTrash)
    val onRight by rememberUpdatedState(if (swapSides) onTrash else onKeep)
    val offsetX = remember(item.key) { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var cardWidth by remember { mutableFloatStateOf(1f) }

    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = offsetX.value
                rotationZ = offsetX.value / 60f
            }
            // Deslizar: los toques siguen yendo a las zonas de abajo; solo un arrastre horizontal llega aquí.
            .onSizeChanged { cardWidth = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(item.key, swipeThreshold) {
                val threshold = size.width * swipeThreshold
                detectHorizontalDragGestures(
                    onDragEnd = {
                        val x = offsetX.value
                        when {
                            x <= -threshold -> onLeft()
                            x >= threshold -> onRight()
                        }
                        // Si se decidió, la tarjeta ya cambió; si no (corto o ignorado), vuelve a su lugar.
                        scope.launch { offsetX.animateTo(0f) }
                    },
                    onDragCancel = { scope.launch { offsetX.animateTo(0f) } },
                ) { change, dragAmount ->
                    change.consume()
                    scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                }
            }
            // TalkBack no puede deslizar: estas acciones deciden igual que el gesto, esté o no activado tocar.
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction("Borrar") { onTrash(); true },
                    CustomAccessibilityAction("Conservar") { onKeep(); true },
                )
            }
            .testTag("card")
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black),
    ) {
        if (playing) {
            AndroidView(
                factory = { context ->
                    VideoView(context).apply {
                        setVideoURI(MediaOps.uriOf(item))
                        setOnPreparedListener { player ->
                            player.isLooping = true
                            start()
                        }
                        setOnErrorListener { _, _, _ ->
                            playing = false
                            true
                        }
                    }
                },
                onRelease = { it.stopPlayback() },
                modifier = Modifier.align(Alignment.Center).fillMaxSize(),
            )
        } else {
            AsyncImage(
                model = imageOf(LocalContext.current, item),
                contentDescription = item.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Zonas de toque encima de la imagen o el video.
        Row(Modifier.fillMaxSize()) {
            val trash = SideLabel(Icons.Filled.Delete, "Borrar")
            val keep = SideLabel(Icons.Filled.Check, "Conservar")
            TapZone(if (swapSides) keep else trash, Alignment.BottomStart, Modifier.weight(1f).testTag("tap-left"), tapToDecide) { onLeft() }
            TapZone(if (swapSides) trash else keep, Alignment.BottomEnd, Modifier.weight(1f).testTag("tap-right"), tapToDecide) { onRight() }
        }

        // Mientras se desliza: color y texto de lo que pasará al soltar. Llega a su máximo justo donde soltar decide.
        val progress = (offsetX.value / (cardWidth * swipeThreshold)).coerceIn(-1f, 1f)
        if (progress != 0f) {
            val deleting = (progress < 0f) != swapSides
            val strength = abs(progress)
            Box(
                Modifier
                    .matchParentSize()
                    .background((if (deleting) Color(0xFFD32F2F) else Color(0xFF2E7D32)).copy(alpha = 0.5f * strength)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (deleting) "BORRAR" else "CONSERVAR",
                    color = Color.White.copy(alpha = strength),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (item.kind == MediaKind.VIDEO) {
            Surface(
                onClick = { playing = !playing },
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f),
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(64.dp)
                    .semantics { contentDescription = if (playing) "Pausar" else "Reproducir" }
                    .testTag("play"),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (playing) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            repeat(2) {
                                Box(Modifier.width(5.dp).height(20.dp).background(Color.White, RoundedCornerShape(2.dp)))
                            }
                        }
                    } else {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(32.dp))
                    }
                }
            }
        }

        Text(
            "${item.displayName}\n${item.relativePath}",
            color = Color.White,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(8.dp),
        )
    }
}

private data class SideLabel(val icon: ImageVector, val text: String)

@Composable
private fun TapZone(label: SideLabel, align: Alignment, modifier: Modifier, enabled: Boolean, onTap: () -> Unit) {
    Box(modifier.fillMaxHeight().then(if (enabled) Modifier.clickable(onClick = onTap) else Modifier)) {
        Row(
            Modifier
                .align(align)
                .padding(12.dp)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(label.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text(label.text, color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
}
