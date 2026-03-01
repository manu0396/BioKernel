package com.neogenesis.platform.control.presentation.design

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import java.awt.Cursor

actual fun Modifier.ngPointerHover(): Modifier = pointerHoverIcon(PointerIcon(Cursor(Cursor.HAND_CURSOR)))
