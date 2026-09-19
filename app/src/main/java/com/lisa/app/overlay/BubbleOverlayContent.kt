package com.lisa.app.overlay

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.lisa.app.domain.SourceType
import com.lisa.app.domain.SummaryResult
import kotlin.math.abs

private val BubblePurple = Color(0xFF6750A4)

/** Distance de déplacement (px) en-deçà de laquelle un geste est considéré comme un tap, pas un drag. */
private const val DragVsTapThresholdPx = 16f

@Composable
fun BubbleOverlayContent(
    state: BubbleUiState,
    onBubbleClick: () -> Unit,
    onBubbleDrag: (dx: Float, dy: Float) -> Unit,
    onYes: () -> Unit,
    onNo: () -> Unit,
    onClose: () -> Unit,
) {
    when (state) {
        is BubbleUiState.Collapsed -> CollapsedBubble(onClick = onBubbleClick, onDrag = onBubbleDrag)
        is BubbleUiState.Prompt -> PromptCard(sourceType = state.content.sourceType, onYes = onYes, onNo = onNo)
        is BubbleUiState.Loading -> LoadingCard()
        is BubbleUiState.Result -> ResultCard(result = state.result, onClose = onClose)
        is BubbleUiState.NothingCaptured -> NothingCapturedCard(onClose = onClose)
    }
}

@Composable
private fun CollapsedBubble(onClick: () -> Unit, onDrag: (dx: Float, dy: Float) -> Unit) {
    var totalDrag by remember { mutableFloatStateOf(0f) }
    Surface(
        shape = CircleShape,
        color = BubblePurple,
        shadowElevation = 6.dp,
        modifier = Modifier
            .size(56.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += abs(dragAmount.x) + abs(dragAmount.y)
                        onDrag(dragAmount.x, dragAmount.y)
                    },
                    onDragEnd = {
                        if (totalDrag < DragVsTapThresholdPx) onClick()
                    },
                )
            },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = "L", color = Color.White, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun PromptCard(sourceType: SourceType, onYes: () -> Unit, onNo: () -> Unit) {
    OverlayCard {
        Text(text = sourceTypeLabel(sourceType), style = MaterialTheme.typography.labelMedium, color = BubblePurple)
        Spacer(Modifier.width(4.dp))
        Text(
            text = "Tu veux que je résume ça ?",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onYes) { Text("Oui") }
            OutlinedButton(onClick = onNo) { Text("Non") }
        }
    }
}

@Composable
private fun LoadingCard() {
    OverlayCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(text = "Résumé en cours…", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ResultCard(result: SummaryResult, onClose: () -> Unit) {
    OverlayCard(onClose = onClose) {
        Text(text = result.shortSummary, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Pourquoi c'est utile pour toi",
            style = MaterialTheme.typography.labelLarge,
            color = BubblePurple,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        )
        Text(text = result.personalRelevance, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun NothingCapturedCard(onClose: () -> Unit) {
    OverlayCard(onClose = onClose) {
        Text(
            text = "Je n'ai pas réussi à lire de texte sur cet écran.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun OverlayCard(onClose: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.widthIn(min = 220.dp, max = 300.dp),
    ) {
        Box {
            Column(modifier = Modifier.padding(16.dp), content = content)
            if (onClose != null) {
                IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(Icons.Filled.Close, contentDescription = "Fermer")
                }
            }
        }
    }
}

private fun sourceTypeLabel(sourceType: SourceType): String = when (sourceType) {
    SourceType.WEB_PAGE -> "Page web"
    SourceType.EMAIL -> "Email"
    SourceType.DOCUMENT -> "Document"
    SourceType.OTHER -> "Contenu"
}
