package com.enmanuelbergling.corntime.feature.settings.presentation

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.animation.expandIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.enmanuelbergling.corntime.core.designsystem.R
import com.enmanuelbergling.corntime.core.designsystem.components.ArtisticBackground
import com.enmanuelbergling.corntime.core.designsystem.components.RoundedUserImage
import com.enmanuelbergling.corntime.core.designsystem.components.shaders.ColorWarpShader
import com.enmanuelbergling.corntime.core.designsystem.components.shaders.HSLColorSpaceShader
import com.enmanuelbergling.corntime.core.designsystem.components.shaders.TileableWaterCaustic
import com.enmanuelbergling.corntime.core.designsystem.components.shaders.VDropTunnel
import com.enmanuelbergling.corntime.core.designsystem.core.dimen
import com.enmanuelbergling.corntime.core.designsystem.theme.CornTimeTheme
import org.koin.compose.viewmodel.koinViewModel
import com.enmanuelbergling.corntime.core.domain.model.settings.DarkTheme

object SettingsTags {
    const val LOGIN = "settings login"
    const val LOGOUT = "settings logout"
    const val DARK_THEME = "settings dark theme"
    const val DYNAMIC_COLOR = "settings dynamic color"
}

@Composable
fun SettingsRoot(onBack: () -> Unit, onLogin: () -> Unit, onLoggedOut: () -> Unit = {}) {
    val viewModel = koinViewModel<SettingsViewModel>()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.onAction(SettingsAction.OnRefreshAccount) }

    SettingsScreen(
        state = state,
        onAction = { action ->
            viewModel.onAction(action)
            if (action == SettingsAction.OnLogoutClick) onLoggedOut()
        },
        onBack = onBack,
        onLogin = onLogin,
    )
}

const val SPEED = 1.1f

@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
fun SettingsScreen(
    state: SettingsState,
    onAction: (SettingsAction) -> Unit,
    onBack: () -> Unit,
    onLogin: () -> Unit,
) {
    var visibleState by rememberSaveable { mutableStateOf(false) }
    // which picker is open is pure UI state, so it is not part of the MVI state
    var darkThemeMenuOpen by rememberSaveable { mutableStateOf(false) }
    var dynamicColorMenuOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(key1 = Unit) { visibleState = true }

    val systemInDarkTheme = isSystemInDarkTheme()
    val isDarkMode by remember(state.darkTheme) {
        derivedStateOf {
            state.darkTheme == DarkTheme.Yes || (state.darkTheme == DarkTheme.System && systemInDarkTheme)
        }
    }

    Surface {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) || !isDarkMode)
                ArtisticBackground(Modifier.fillMaxSize())
            else AnimatedBackground(
                Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(.5f)
            )

            IconButton(onClick = onBack, modifier = Modifier.statusBarsPadding()) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = "icon back"
                )
            }

            if (state.user != null) LogoutButton(
                modifier = Modifier
                    .statusBarsPadding()
                    .align(Alignment.TopEnd)
                    .testTag(SettingsTags.LOGOUT)
            ) { onAction(SettingsAction.OnLogoutClick) }

            Box(
                Modifier
                    .fillMaxHeight(.4f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ProfileImageUi(
                    userUi = state.user,
                    visibleState = visibleState,
                    modifier = Modifier.size(100.dp)
                )
            }

            SettingOptions(
                userState = state.user,
                darkTheme = state.darkTheme,
                dynamicColor = state.dynamicColor,
                visibleState = visibleState,
                modifier = Modifier
                    .fillMaxHeight(.6f)
                    .align(Alignment.BottomCenter),
                onLogin = onLogin,
                onDarkThemeClick = { darkThemeMenuOpen = true },
                onDynamicColorClick = { dynamicColorMenuOpen = true },
            )
        }
    }

    if (darkThemeMenuOpen) {
        DarkThemeMenu(
            darkTheme = state.darkTheme,
            onDismiss = { darkThemeMenuOpen = false }) { theme ->
            onAction(SettingsAction.OnDarkThemeChange(theme))
        }
    }

    if (dynamicColorMenuOpen) {
        DynamicColorMenu(
            active = state.dynamicColor,
            onDismiss = { dynamicColorMenuOpen = false }) { active ->
            onAction(SettingsAction.OnDynamicColorChange(active))
        }
    }
}

@Composable
private fun AnimatedBackground(modifier: Modifier) {
    val shaderTime by produceState(0f) {
        while (true) {
            withInfiniteAnimationFrameMillis {
                value = it / 1000f * SPEED
            }
        }
    }
    val contentColor = MaterialTheme.colorScheme.secondary
    val backgroundColor = MaterialTheme.colorScheme.background

    var profileShader by remember {
        mutableStateOf(ProfileShader.entries.first())
    }

    Box(
        modifier = modifier
            .combinedClickable(onLongClick = {
                val last = ProfileShader.entries.last()
                if (profileShader == last) {
                    profileShader = ProfileShader.entries.first()
                } else {
                    val index = ProfileShader.entries.indexOf(profileShader)
                    profileShader = ProfileShader.entries[index + 1]
                }
            }) { }
            .drawWithCache {
                drawShader(
                    profileShader = profileShader,
                    shaderTime = shaderTime,
                    contentColor = contentColor,
                    backgroundColor = backgroundColor
                )
            },
    )
}

@Composable
private fun LogoutButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(R.drawable.power_outline),
            contentDescription = "logout icon"
        )
    }
}

enum class ProfileShader(val shader: String) {
    Omelette(HSLColorSpaceShader), AwesomeHole(ColorWarpShader), DropTunnel(VDropTunnel), Sea(
        TileableWaterCaustic
    )
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private fun CacheDrawScope.drawShader(
    profileShader: ProfileShader,
    shaderTime: Float,
    contentColor: Color,
    backgroundColor: Color,
): DrawResult {
    val runtimeShader = RuntimeShader(profileShader.shader)
    val shaderBrush = ShaderBrush(runtimeShader)

    runtimeShader.setColorUniform(
        "backgroundColor", android.graphics.Color.valueOf(
            backgroundColor.red, backgroundColor.green, backgroundColor.blue, backgroundColor.alpha
        )
    )

    if (profileShader == ProfileShader.Sea) {
        runtimeShader.setColorUniform(
            "primaryColor", android.graphics.Color.valueOf(
                contentColor.red, contentColor.green, contentColor.blue, contentColor.alpha
            )
        )
    }

    return onDrawWithContent {
        runtimeShader.setFloatUniform(
            "resolution", size.width, size.height
        )
        runtimeShader.setFloatUniform(
            "time", shaderTime
        )

        drawRect(shaderBrush)

        drawContent()
    }
}

@Composable
private fun SettingOptions(
    userState: UserUi?,
    darkTheme: DarkTheme,
    dynamicColor: Boolean,
    visibleState: Boolean,
    modifier: Modifier,
    onLogin: () -> Unit,
    onDarkThemeClick: () -> Unit,
    onDynamicColorClick: () -> Unit,
) {

    AnimatedVisibility(
        visible = visibleState,
        enter = slideInVertically(spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow)) { it },
        modifier = modifier,
    ) {
        LazyColumn(
            contentPadding = WindowInsets.navigationBars.asPaddingValues(),
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    shape = MaterialTheme.shapes.medium.copy(
                        bottomStart = CornerSize(0), bottomEnd = CornerSize(0)
                    )
                )

        ) {
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    if (userState == null) {
                        OutlinedButton(
                            onClick = onLogin,
                            modifier = Modifier
                                .fillMaxWidth(.8f)
                                .testTag(SettingsTags.LOGIN)
                        ) {
                            Text(text = stringResource(id = R.string.login).uppercase())
                        }
                    } else {
                        Text(
                            text = "Hello " + userState.username,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            item {
                SettingItemUi(
                    item = SettingItem.DarkMode,
                    textValue = darkTheme.label,
                    modifier = Modifier.fillMaxWidth().testTag(SettingsTags.DARK_THEME),
                ) { onDarkThemeClick() }
            }

            item {
                SettingItemUi(
                    item = SettingItem.DynamicColor,
                    textValue = if (dynamicColor) "On" else "Off",
                    modifier = Modifier.fillMaxWidth().testTag(SettingsTags.DYNAMIC_COLOR),
                ) { onDynamicColorClick() }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DynamicColorMenu(
    active: Boolean,
    onDismiss: () -> Unit,
    onDynamicTheme: (Boolean) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.verySmall)
        ) {
            SelectionText(
                text = "On",
                selected = active,
                modifier = Modifier.height(TextFieldDefaults.MinHeight)
            ) {
                onDynamicTheme(true)
                onDismiss()
            }
            SelectionText(
                text = "Off",
                selected = !active,
                modifier = Modifier.height(TextFieldDefaults.MinHeight)
            ) {
                onDynamicTheme(false)
                onDismiss()
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun DarkThemeMenu(
    darkTheme: DarkTheme,
    onDismiss: () -> Unit,
    onDarkTheme: (DarkTheme) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.verySmall)
        ) {
            DarkTheme.entries.forEach { theme ->
                SelectionText(
                    text = theme.label,
                    selected = theme == darkTheme,
                    modifier = Modifier.height(TextFieldDefaults.MinHeight)
                ) {
                    onDarkTheme(theme)
                    onDismiss()
                }
            }
        }
    }
}

@Composable
fun SelectionText(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val dotColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        label = "dot color"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else LocalContentColor.current,
        label = "text color"
    )

    Row(
        modifier
            .clickable { onClick() }
            .padding(horizontal = MaterialTheme.dimen.small),
        verticalAlignment = Alignment.CenterVertically) {


        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = "selected icon",
            modifier = Modifier.size(MaterialTheme.dimen.mediumSmall),
            tint = dotColor
        )

        Spacer(modifier = Modifier.width(MaterialTheme.dimen.superSmall))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = textColor,
            modifier = Modifier.weight(1f)
        )
    }
}

@PreviewLightDark
@Composable
private fun SelectionTextPrev() {
    CornTimeTheme {
        SelectionText(
            text = "System",
            selected = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {}
    }
}

@Composable
internal fun SettingItemUi(
    item: SettingItem,
    modifier: Modifier = Modifier,
    textValue: String = "",
    onClick: () -> Unit,
) {

    Row(
        modifier
            .clickable { onClick() }
            .padding(all = MaterialTheme.dimen.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.dimen.small)) {
        Box(
            modifier = Modifier
                .padding(MaterialTheme.dimen.small),
        ) {
            Icon(
                painter = painterResource(item.iconRes),
                contentDescription = "setting icon",
            )
        }

        Text(
            text = stringResource(item.label),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )

        Text(
            text = textValue,
            style = MaterialTheme.typography.bodyMedium,
        )

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = "forward icon",
            Modifier.size(14.dp)
        )
    }
}

@PreviewLightDark
@Composable
private fun SettingItemUiPrev() {
    CornTimeTheme {

        SettingItemUi(
            SettingItem.DarkMode, Modifier.fillMaxWidth(), "System"
        ) {}
    }
}

@Composable
internal fun ProfileImageUi(
    userUi: UserUi?,
    modifier: Modifier = Modifier,
    visibleState: Boolean = true,
) {
    AnimatedVisibility(
        visible = visibleState, enter = expandIn(
            spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow),
            expandFrom = Alignment.Center
        ), modifier = modifier
    ) {
        RoundedUserImage(
            userUi?.avatarPath, Modifier
                .border(
                    MaterialTheme.dimen.verySmall, MaterialTheme.colorScheme.primary, CircleShape
                )
                .padding(MaterialTheme.dimen.small)
        )
    }
}

@PreviewLightDark
@Composable
private fun ProfileUiPrev() {
    CornTimeTheme {

        ProfileImageUi(UserUi(username = "Tim", avatarPath = ""), modifier = Modifier.size(80.dp))
    }
}

internal data class SettingItem(
    @androidx.annotation.DrawableRes val iconRes: Int,
    @androidx.annotation.StringRes val label: Int,
) {
    companion object {
        val DarkMode = SettingItem(R.drawable.moon, R.string.dark_mode)
        val DynamicColor = SettingItem(R.drawable.paint_brush, R.string.dynamic_color)
    }
}
