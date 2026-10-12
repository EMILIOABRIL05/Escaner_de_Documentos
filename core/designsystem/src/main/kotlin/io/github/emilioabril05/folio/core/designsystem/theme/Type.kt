package io.github.emilioabril05.folio.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.emilioabril05.folio.core.designsystem.R

/** Inter (SIL OFL 1.1), empaquetada para funcionar sin conexión. */
val InterFontFamily =
    FontFamily(
        Font(R.font.inter_regular, FontWeight.Normal),
        Font(R.font.inter_medium, FontWeight.Medium),
        Font(R.font.inter_semibold, FontWeight.SemiBold),
        Font(R.font.inter_bold, FontWeight.Bold),
    )

private val LargeTitle = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, fontSize = 34.sp)
private val Title = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 22.sp)
private val Body = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 17.sp)
private val Action = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp)
private val Note = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp)
private val NoteStrong = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Medium, fontSize = 13.sp)

/** Escala tipográfica de Folio (plan §10.6): título grande 34, título 22, cuerpo 17, nota 13; siempre en sp. */
val FolioTypography =
    Typography(
        headlineLarge = LargeTitle,
        titleLarge = Title,
        bodyLarge = Body,
        labelLarge = Action,
        bodySmall = Note,
        labelMedium = NoteStrong,
    )
