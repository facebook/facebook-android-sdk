package com.facebook.samples.kotlinsampleapp.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MenuItem(text: String, onClick: () -> Unit = {}) {
  val interactionSource = remember { MutableInteractionSource() }
  Column {
    Row(
        modifier =
            Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = rememberRipple(),
                    role = Role.Button,
                    onClick = onClick)
                .fillMaxWidth(1.0f)) {
          Text(text, fontSize = 24.sp)
        }
    Divider(modifier = Modifier.padding(vertical = 4.dp))
  }
}
