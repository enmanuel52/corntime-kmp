package com.enmanuelbergling.corntime.feature.auth.presentation.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.enmanuelbergling.corntime.core.designsystem.R as DesignR
import com.enmanuelbergling.corntime.core.designsystem.components.ArtisticBackground
import com.enmanuelbergling.corntime.core.designsystem.components.DefaultErrorDialog
import com.enmanuelbergling.corntime.core.designsystem.components.LoadingScreen
import com.enmanuelbergling.corntime.core.designsystem.core.dimen
import com.enmanuelbergling.corntime.core.designsystem.design.CtiTextField
import com.enmanuelbergling.corntime.core.designsystem.theme.CornTimeTheme
import com.enmanuelbergling.corntime.core.presentation.error.UiError
import com.enmanuelbergling.corntime.core.presentation.error.messageResource
import com.enmanuelbergling.corntime.core.presentation.event.ObserveAsEvents
import com.enmanuelbergling.corntime.feature.auth.domain.login.LoginFieldError
import com.enmanuelbergling.corntime.feature.auth.presentation.R
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.liquid
import io.github.fletchmckee.liquid.rememberLiquidState
import org.koin.compose.viewmodel.koinViewModel

object LoginTags {
    const val USERNAME = "login username"
    const val PASSWORD = "login password"
    const val SUBMIT = "login submit"
    const val LOADING = "login loading"
    const val SIGN_UP = "login sign up"
}

private const val SIGN_UP_URL = "https://www.themoviedb.org/signup"

@Composable
fun LoginRoot(onLoginSucceed: () -> Unit, onBack: () -> Unit) {
    val viewModel = koinViewModel<LoginViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            LoginEvent.LoginSuccess -> onLoginSucceed()
        }
    }

    LoginScreen(state = state, onAction = viewModel::onAction, onBack = onBack)
}

@Composable
fun LoginScreen(
    state: LoginState,
    onAction: (LoginAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        LoadingScreen(Modifier.testTag(LoginTags.LOADING))
        return
    }

    state.error?.let { error ->
        DefaultErrorDialog(
            onDismissDialog = { onAction(LoginAction.OnDismissError) },
            message = stringResource(error.loginMessageResource),
        )
    }

    Scaffold { paddingValues ->
        LoginContent(state, onAction, onBack, modifier.padding(paddingValues))
    }
}

/** Wrong credentials come back as unauthorized; everywhere else that reads as "session expired". */
private val UiError.loginMessageResource: Int
    get() = if (this == UiError.Unauthorized) R.string.invalid_credentials else messageResource

@Composable
private fun LoginContent(
    state: LoginState,
    onAction: (LoginAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier,
) {
    val cardShape = CardDefaults.shape
    val liquidState = rememberLiquidState()

    Surface {
        Box(modifier = modifier.fillMaxSize()) {
            ArtisticBackground(
                Modifier
                    .fillMaxSize()
                    .liquefiable(liquidState),
            )

            TopBar(onBack)

            LoginForm(
                state = state,
                onAction = onAction,
                modifier = Modifier
                    .align(Alignment.Center)
                    .liquid(liquidState) {
                        shape = cardShape
                        edge = .07f
                        refraction = .2f
                    }
                    .padding(MaterialTheme.dimen.medium, MaterialTheme.dimen.lessLarge),
            )

            val uriHandler = LocalUriHandler.current

            CreateAccount(
                Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding(),
            ) { uriHandler.openUri(SIGN_UP_URL) }
        }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Column(modifier = Modifier.statusBarsPadding()) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.Rounded.ArrowBackIosNew,
                contentDescription = stringResource(R.string.back_icon_description),
            )
        }

        Spacer(modifier = Modifier.height(MaterialTheme.dimen.medium))

        Text(
            text = stringResource(DesignR.string.welcome_back).split(" ").joinToString("\n"),
            style = MaterialTheme.typography.displaySmall,
            maxLines = 2,
            modifier = Modifier.padding(start = MaterialTheme.dimen.small),
        )
    }
}

@Composable
private fun CreateAccount(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(text = stringResource(R.string.no_account_question), style = MaterialTheme.typography.bodySmall)

        Spacer(modifier = Modifier.width(MaterialTheme.dimen.verySmall))

        TextButton(onClick = onClick, modifier = Modifier.testTag(LoginTags.SIGN_UP)) {
            Text(text = stringResource(DesignR.string.sign_in))
        }
    }
}

@Composable
private fun LoginForm(state: LoginState, onAction: (LoginAction) -> Unit, modifier: Modifier) {
    val autofillManager = LocalAutofillManager.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.large),
        modifier = modifier,
    ) {
        CtiTextField(
            text = state.username,
            onTextChange = { onAction(LoginAction.OnUsernameChange(it)) },
            hint = stringResource(DesignR.string.username),
            errorText = state.usernameError?.let { stringResource(it.messageResource) },
            modifier = Modifier
                .testTag(LoginTags.USERNAME)
                .semantics { contentType = ContentType.Username + ContentType.NewUsername },
        )

        CtiTextField(
            text = state.password,
            onTextChange = { onAction(LoginAction.OnPasswordChange(it)) },
            hint = stringResource(DesignR.string.password),
            errorText = state.passwordError?.let { stringResource(it.messageResource) },
            trailingIcon = {
                IconButton(onClick = { onAction(LoginAction.OnPasswordVisibilityClick) }) {
                    Icon(
                        imageVector = if (state.isPasswordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                        contentDescription = stringResource(DesignR.string.password_visibility_icon),
                    )
                }
            },
            visualTransformation = if (state.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier
                .testTag(LoginTags.PASSWORD)
                .semantics { contentType = ContentType.Password + ContentType.NewPassword },
        )

        Button(
            onClick = {
                autofillManager?.commit()
                onAction(LoginAction.OnLoginClick)
            },
            // an error stays until the field is edited, which is when it clears
            enabled = state.usernameError == null && state.passwordError == null,
            modifier = Modifier.testTag(LoginTags.SUBMIT),
        ) {
            Text(
                text = stringResource(DesignR.string.log_in),
                modifier = Modifier.padding(horizontal = MaterialTheme.dimen.small),
            )
        }
    }
}

private val LoginFieldError.messageResource: Int
    get() = when (this) {
        LoginFieldError.UsernameRequired -> R.string.username_required
        LoginFieldError.PasswordRequired -> R.string.password_required
        LoginFieldError.PasswordTooShort -> R.string.password_too_short
    }

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    CornTimeTheme {
        LoginScreen(LoginState(), onAction = {}, onBack = {}, modifier = Modifier.fillMaxSize())
    }
}
