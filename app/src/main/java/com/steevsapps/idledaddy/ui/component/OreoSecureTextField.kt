package com.steevsapps.idledaddy.ui.component

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.input.KeyboardActionHandler
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.SecureTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp

/**
 * Password version of [OreoTextField]: same flat, underline-only look, but the typed text is
 * hidden and can't be copied or cut.
 *
 * Unlike [OreoTextField] the text lives in a [TextFieldState] (see `rememberTextFieldState()`);
 * read it with `state.text.toString()`.
 */
@Composable
fun OreoSecureTextField(
    state: TextFieldState,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    onKeyboardAction: KeyboardActionHandler? = null,
    colors: TextFieldColors = TextFieldDefaults.colors(
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
    ),
) {
    SecureTextField(
        state = state,
        modifier = modifier,
        placeholder = placeholder?.let {
            {
                Text(
                    text = it,
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 10.sp,
                        maxFontSize = 16.sp,
                        stepSize = 1.sp,
                    ),
                )
            }
        },
        isError = isError,
        supportingText = supportingText,
        keyboardOptions = keyboardOptions,
        onKeyboardAction = onKeyboardAction,
        colors = colors,
    )
}
