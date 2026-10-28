package com.mitrc.ac.`in`.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.mitrc.ac.`in`.R

/**
 * Bundled across all devices so glyphs, widths and line breaks never vary by OEM
 * (Pixel ships Roboto, Samsung One UI Sans, Xiaomi MiSans, etc.).
 */
val MitrcFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans_extralight, FontWeight.ExtraLight, FontStyle.Normal),
    Font(R.font.plus_jakarta_sans_extralight_italic, FontWeight.ExtraLight, FontStyle.Italic),
    Font(R.font.plus_jakarta_sans_light, FontWeight.Light, FontStyle.Normal),
    Font(R.font.plus_jakarta_sans_light_italic, FontWeight.Light, FontStyle.Italic),
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal, FontStyle.Normal),
    Font(R.font.plus_jakarta_sans_italic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium, FontStyle.Normal),
    Font(R.font.plus_jakarta_sans_medium_italic, FontWeight.Medium, FontStyle.Italic),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold, FontStyle.Normal),
    Font(R.font.plus_jakarta_sans_semibold_italic, FontWeight.SemiBold, FontStyle.Italic),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold, FontStyle.Normal),
    Font(R.font.plus_jakarta_sans_bold_italic, FontWeight.Bold, FontStyle.Italic),
    Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold, FontStyle.Normal),
    Font(R.font.plus_jakarta_sans_extrabold_italic, FontWeight.ExtraBold, FontStyle.Italic),
)
