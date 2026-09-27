package com.stepup.android.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R

/** Six visual grades. The last two are previews and are not mintable Rarity values. */
private data class GradePreview(
    val number: Int,
    @StringRes val name: Int,
    @DrawableRes val back: Int,
    @DrawableRes val front: Int,
    @DrawableRes val frame: Int,
    val color: Color,
    val available: Boolean,
)

private val grades = listOf(
    GradePreview(1, R.string.rarity_common, R.drawable.shoe_grade_back_01, R.drawable.shoe_grade_front_01, R.drawable.shoe_grade_frame_01, Color(0xFF91A1B8), true),
    GradePreview(2, R.string.rarity_rare, R.drawable.shoe_grade_back_02, R.drawable.shoe_grade_front_02, R.drawable.shoe_grade_frame_02, Color(0xFF4A9FF0), true),
    GradePreview(3, R.string.rarity_epic, R.drawable.shoe_grade_back_03, R.drawable.shoe_grade_front_03, R.drawable.shoe_grade_frame_03, Color(0xFFA778E6), true),
    GradePreview(4, R.string.rarity_legendary, R.drawable.shoe_grade_back_04, R.drawable.shoe_grade_front_04, R.drawable.shoe_grade_frame_04, Color(0xFFE2AE52), true),
    GradePreview(5, R.string.rarity_redline_preview, R.drawable.shoe_grade_back_05, R.drawable.shoe_grade_front_05, R.drawable.shoe_grade_frame_05, Color(0xFFFF654D), false),
    GradePreview(6, R.string.rarity_finish_preview, R.drawable.shoe_grade_back_06, R.drawable.shoe_grade_front_06, R.drawable.shoe_grade_frame_06, Color(0xFF5DE9DE), false),
)

@Composable
fun ShoeGradeGuide(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.shoe_grade_guide_title), style = MaterialTheme.typography.titleMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(grades, key = { it.number }) { grade ->
                GradeCard(grade)
            }
        }
        Text(
            stringResource(R.string.shoe_grade_guide_note),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF9BAEC8),
        )
    }
}

@Composable
private fun GradeCard(grade: GradePreview) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier.width(152.dp).clip(shape).background(Color(0xFF0C1728))
            .border(1.dp, grade.color.copy(alpha = if (grade.available) .28f else .55f), shape)
            .padding(7.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(
            Modifier.fillMaxWidth().aspectRatio(440f / 418f)
                .clip(RoundedCornerShape(11.dp)).background(Color(0xFF081320)),
            contentAlignment = Alignment.Center,
        ) {
            Image(painterResource(grade.back), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            Image(
                painterResource(R.drawable.sneaker_water_06), null,
                Modifier.fillMaxSize().padding(horizontal = 15.dp, vertical = 22.dp),
                contentScale = ContentScale.Fit,
            )
            Image(painterResource(grade.front), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
            Image(painterResource(grade.frame), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        }
        Text(
            text = "%02d  %s".format(grade.number, stringResource(grade.name)),
            color = grade.color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        Text(
            stringResource(if (grade.available) R.string.shoe_grade_live else R.string.shoe_grade_preview),
            color = Color(0xFFB1C0D4),
            fontSize = 12.sp,
        )
    }
}
