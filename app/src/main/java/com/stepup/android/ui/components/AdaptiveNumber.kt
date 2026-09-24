package com.stepup.android.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.StepUpNumbers

/** Keep a complete timer or amount on one line; respect font scaling until width runs out. */
@Composable
fun AdaptiveNumber(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    color: Color = Snow,
    textAlign: TextAlign = TextAlign.Start,
) {
    val measurer = rememberTextMeasurer()
    val style = TextStyle(fontFamily = StepUpNumbers, fontSize = fontSize, fontWeight = FontWeight.ExtraBold)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val availableWidth = constraints.maxWidth
        val fittedSize = remember(text, style, measurer, availableWidth) {
            val preferredSize = fontSize.value.coerceAtLeast(1f)
            fun widthAt(size: Float) = measurer.measure(
                text, style.copy(fontSize = size.sp), softWrap = false,
            ).size.width
            if (widthAt(preferredSize) <= availableWidth) {
                preferredSize.sp
            } else {
                // Measure each candidate: Android large-font scaling need not be linear.
                var lower = 1f
                var upper = preferredSize
                repeat(8) {
                    val candidate = (lower + upper) / 2f
                    if (widthAt(candidate) <= availableWidth) lower = candidate else upper = candidate
                }
                lower.sp
            }
        }
        Text(
            text, modifier = Modifier.fillMaxWidth(),
            style = style.copy(fontSize = fittedSize),
            color = color, textAlign = textAlign, maxLines = 1, softWrap = false,
        )
    }
}
