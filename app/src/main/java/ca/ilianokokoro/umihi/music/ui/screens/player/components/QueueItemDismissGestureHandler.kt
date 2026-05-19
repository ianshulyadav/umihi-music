package ca.ilianokokoro.umihi.music.ui.screens.player.components

import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Density
import androidx.core.view.HapticFeedbackConstantsCompat
import androidx.core.view.ViewCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

private enum class QueueDismissDragPhase { IDLE, TENSION, SNAPPING, FREE_DRAG }

internal class QueueItemDismissGestureHandler(
    private val scope: CoroutineScope,
    private val density: Density,
    private val hapticView: View,
    private val offsetAnimatable: Animatable<Float, AnimationVector1D>,
    private val itemWidthPx: Float,
    private val onDismiss: () -> Unit
) {
    private var dragPhase: QueueDismissDragPhase = QueueDismissDragPhase.IDLE
    private var accumulatedDragX: Float = 0f

    var isInDismissZone: Boolean by mutableStateOf(false)
        private set

    var isDismissing: Boolean by mutableStateOf(false)
        private set

    fun onDragStart() {
        if (isDismissing) return
        dragPhase = QueueDismissDragPhase.TENSION
        accumulatedDragX = 0f
        isInDismissZone = false
        scope.launch { offsetAnimatable.stop() }
    }

    fun onHorizontalDrag(dragAmount: Float) {
        if (isDismissing) return
        accumulatedDragX += dragAmount
        // Only allow end-to-start (negative / left) swipes
        if (accumulatedDragX > 0f) {
            accumulatedDragX = 0f
            scope.launch { offsetAnimatable.snapTo(0f) }
            return
        }

        when (dragPhase) {
            QueueDismissDragPhase.TENSION -> {
                val tensionThresholdPx = 60f * density.density
                if (abs(accumulatedDragX) < tensionThresholdPx) {
                    val maxTensionOffsetPx = 20f * density.density
                    val dragFraction = (abs(accumulatedDragX) / tensionThresholdPx).coerceIn(0f, 1f)
                    val tensionOffset = maxTensionOffsetPx * dragFraction
                    scope.launch {
                        offsetAnimatable.snapTo(-tensionOffset)
                    }
                } else {
                    dragPhase = QueueDismissDragPhase.SNAPPING
                }
            }

            QueueDismissDragPhase.SNAPPING -> {
                ViewCompat.performHapticFeedback(hapticView, HapticFeedbackConstantsCompat.GESTURE_THRESHOLD_ACTIVATE)
                scope.launch {
                    offsetAnimatable.animateTo(
                        targetValue = accumulatedDragX,
                        animationSpec = spring(
                            dampingRatio = 0.8f,
                            stiffness = Spring.StiffnessLow
                        )
                    )
                }
                dragPhase = QueueDismissDragPhase.FREE_DRAG
            }

            QueueDismissDragPhase.FREE_DRAG -> {
                val dismissThreshold = itemWidthPx * 0.40f
                val nowInZone = abs(accumulatedDragX) > dismissThreshold
                if (nowInZone != isInDismissZone) {
                    isInDismissZone = nowInZone
                    ViewCompat.performHapticFeedback(
                        hapticView,
                        if (nowInZone) HapticFeedbackConstantsCompat.GESTURE_THRESHOLD_ACTIVATE
                        else HapticFeedbackConstantsCompat.GESTURE_THRESHOLD_DEACTIVATE
                    )
                }
                scope.launch {
                    offsetAnimatable.animateTo(
                        targetValue = accumulatedDragX,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessHigh
                        )
                    )
                }
            }

            QueueDismissDragPhase.IDLE -> Unit
        }
    }

    fun onDragEnd() {
        if (isDismissing) return
        dragPhase = QueueDismissDragPhase.IDLE
        val dismissThreshold = itemWidthPx * 0.40f

        if (abs(accumulatedDragX) > dismissThreshold) {
            isDismissing = true
            ViewCompat.performHapticFeedback(hapticView, HapticFeedbackConstantsCompat.GESTURE_END)
            scope.launch {
                offsetAnimatable.animateTo(
                    targetValue = -itemWidthPx,
                    animationSpec = tween(
                        durationMillis = 180,
                        easing = FastOutSlowInEasing
                    )
                )
                onDismiss()
                offsetAnimatable.snapTo(0f)
                isDismissing = false
                isInDismissZone = false
            }
        } else {
            isInDismissZone = false
            scope.launch {
                offsetAnimatable.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
        }
    }

    fun onDragCancel() {
        if (isDismissing) return
        dragPhase = QueueDismissDragPhase.IDLE
        isInDismissZone = false
        scope.launch {
            offsetAnimatable.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }
}

@Composable
internal fun rememberQueueItemDismissGestureHandler(
    scope: CoroutineScope,
    density: Density,
    hapticView: View,
    offsetAnimatable: Animatable<Float, AnimationVector1D>,
    itemWidthPx: Float,
    onDismiss: () -> Unit
): QueueItemDismissGestureHandler {
    return remember(scope, density, hapticView, offsetAnimatable, itemWidthPx, onDismiss) {
        QueueItemDismissGestureHandler(
            scope = scope,
            density = density,
            hapticView = hapticView,
            offsetAnimatable = offsetAnimatable,
            itemWidthPx = itemWidthPx,
            onDismiss = onDismiss
        )
    }
}
