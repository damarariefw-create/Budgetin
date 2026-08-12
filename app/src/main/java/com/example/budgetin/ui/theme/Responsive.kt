package com.example.budgetin.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

/**
 * Lebar konten maksimal agar layar lebar (tablet / foldable / HP besar) tidak
 * meregangkan kartu & teks sampai tidak nyaman dibaca.
 */
val MaxContentWidth = 640.dp

/**
 * Pembungkus responsif satu layar:
 * - HP biasa (<= 640dp): konten memenuhi layar seperti sebelumnya.
 * - Layar lebar: konten dibatasi [MaxContentWidth] dan dipusatkan.
 *
 * Konten di dalamnya mengisi lebar yang tersedia (maksimal [MaxContentWidth]);
 * lebar layar bisa dibaca lewat `maxWidth` di scope [BoxWithConstraintsScope]
 * bila layar butuh penyesuaian tambahan.
 */
@Composable
fun ResponsiveContainer(
    modifier: Modifier = Modifier,
    content: @Composable BoxWithConstraintsScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val scope: BoxWithConstraintsScope = this
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxHeight()
                .widthIn(max = MaxContentWidth),
        ) {
            scope.content()
        }
    }
}

/**
 * Ukuran font adaptif untuk nominal besar: mengecil otomatis bila teksnya
 * panjang, agar tidak terpotong di layar sempit (mis. "Rp 999.999.999.999").
 */
fun amountFontSize(text: String, base: TextUnit): TextUnit = when {
    text.length > 20 -> base * 0.6f
    text.length > 15 -> base * 0.75f
    text.length > 11 -> base * 0.85f
    else -> base
}
