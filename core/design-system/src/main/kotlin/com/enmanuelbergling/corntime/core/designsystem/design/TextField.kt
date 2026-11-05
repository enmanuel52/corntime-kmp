package com.enmanuelbergling.corntime.core.designsystem.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview

/**
 * @param errorText when isn't null turn on isError
 * */
@Composable
fun CtiTextField(
    text: String,
    onTextChange: (String) -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    errorText: String? = null,
    leadingIcon: @Composable () -> Unit = {},
    trailingIcon: @Composable () -> Unit = {},
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = modifier,
        label = {
            Text(
                text = hint
            )
        },
        isError = errorText != null,
        supportingText = if (errorText != null) {
            {
                Text(
                    text = errorText
                )
            }
        } else null,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(15)
    )
}

@Preview
@Composable
private fun CtiTextFieldPrev() {
    CtiTextField("", {}, "username")
}