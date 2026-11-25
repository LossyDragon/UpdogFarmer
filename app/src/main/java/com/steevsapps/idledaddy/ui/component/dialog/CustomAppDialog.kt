package com.steevsapps.idledaddy.ui.component.dialog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.steam.model.Game
import com.steevsapps.idledaddy.ui.component.OreoTextField
import com.steevsapps.idledaddy.ui.component.scrollbar
import com.steevsapps.idledaddy.ui.theme.IdleTheme
import kotlinx.coroutines.launch

private const val TYPE_APPID_LIST = 0
private const val TYPE_CUSTOM = 1

@Composable
fun CustomAppDialog(
    onConfirm: (List<Game>) -> Unit,
    onDismiss: () -> Unit,
    initialTypeIndex: Int = TYPE_APPID_LIST,
    initialAppIds: List<Int> = emptyList(),
) {
    val resources = LocalResources.current
    val typeOptions = stringArrayResource(R.array.custom_app_type_options)
    var typeIndex by rememberSaveable { mutableIntStateOf(initialTypeIndex) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var input by rememberSaveable { mutableStateOf("") }

    // Collected app IDs for TYPE_APPID_LIST. Nothing is idled until OK is pressed.
    val appIds = remember { initialAppIds.toMutableStateList() }
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(typeIndex) {
        focusRequester.requestFocus()
    }

    fun unknownApp(appId: Int) =
        Game(appId, resources.getString(R.string.playing_unknown_app, appId), 0f, 0)

    fun addAppIds() {
        // Several appids can be added at once, separated by spaces and/or commas
        val parsed = parseAppIdInput(input)
        val newIds = parsed.appIds.filter { it !in appIds }
        // Anything that isn't an appid stays behind so it can be corrected
        input = parsed.rejected
        if (newIds.isNotEmpty()) {
            appIds.addAll(0, newIds)
            scope.launch { state.animateScrollToItem(0) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            val text = if (typeIndex == TYPE_CUSTOM) stringResource(R.string.idle_custom_app)
            else stringResource(R.string.idle_custom_app_ids)
            Text(text = text) },
        text = {
            Column {
                Box {
                    TextButton(onClick = { typeMenuExpanded = true }) {
                        Text(text = typeOptions[typeIndex])
                        Icon(
                            imageVector = if (typeMenuExpanded) {
                                Icons.Default.ArrowDropUp
                            } else {
                                Icons.Default.ArrowDropDown
                            },
                            contentDescription = null,
                        )
                    }
                    DropdownMenu(
                        expanded = typeMenuExpanded,
                        onDismissRequest = { typeMenuExpanded = false },
                    ) {
                        typeOptions.forEachIndexed { index, option ->
                            DropdownMenuItem(
                                text = { Text(text = option) },
                                onClick = {
                                    typeIndex = index
                                    typeMenuExpanded = false
                                },
                            )
                        }
                    }
                }
                if (typeIndex == TYPE_APPID_LIST) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OreoTextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                            placeholder = stringResource(R.string.enter_an_appid),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(onDone = { addAppIds() }),
                        )
                        IconButton(onClick = { addAppIds() }) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = stringResource(R.string.add_app_id),
                            )
                        }
                    }
                    LazyColumn(
                        state = state,
                        modifier = Modifier
                            .heightIn(max = 240.dp)
                            .scrollbar(state),
                    ) {
                        items(items = appIds, key = { it }) { id ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = id.toString(), modifier = Modifier.weight(1f))
                                IconButton(onClick = { appIds.remove(id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.remove_app_id),
                                    )
                                }
                            }
                        }
                    }
                } else {
                    OreoTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.focusRequester(focusRequester),
                        placeholder = stringResource(R.string.desc_custom_app),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                // Pick up anything typed but not yet added with "+"
                if (typeIndex == TYPE_APPID_LIST) addAppIds()
                val text = input.trim()
                when (typeIndex) {
                    TYPE_CUSTOM ->
                        if (text.isNotEmpty()) onConfirm(listOf(Game(0, text, 0f, 0))) else onDismiss()

                    TYPE_APPID_LIST ->
                        if (appIds.isNotEmpty()) onConfirm(appIds.map(::unknownApp)) else onDismiss()

                    else -> onDismiss()
                }
            }) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
    )
}

/**
 * Preview
 */

private class CustomAppTypePreview : PreviewParameterProvider<Int> {
    private val types = listOf(
        "App ID list" to TYPE_APPID_LIST,
        "Custom name" to TYPE_CUSTOM,
    )

    override val values = types.asSequence().map { it.second }

    override fun getDisplayName(index: Int) = types[index].first
}

@Preview
@Composable
private fun Preview(@PreviewParameter(CustomAppTypePreview::class) typeIndex: Int) {
    IdleTheme {
        Box(Modifier.fillMaxSize()) {
            CustomAppDialog(
                onConfirm = {},
                onDismiss = {},
                initialTypeIndex = typeIndex,
                initialAppIds = listOf(440, 570, 730),
            )
        }
    }
}