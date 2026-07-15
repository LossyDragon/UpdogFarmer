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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.ui.component.OreoTextField
import com.steevsapps.idledaddy.ui.component.scrollbar
import com.steevsapps.idledaddy.ui.theme.IdleTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import me.zhanghai.compose.preference.MapPreferences
import me.zhanghai.compose.preference.Preferences
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.rememberPreferenceState

@Composable
fun BlacklistEditDialog(onDismiss: () -> Unit) {
    var storedValue by rememberPreferenceState("blacklist", "")
    val ids = remember { storedValue.split(",").filter { it.isNotEmpty() }.toMutableStateList() }
    var input by rememberSaveable { mutableStateOf("") }
    val state = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    fun addItem() {
        val text = input.trim()
        if (text.matches("\\d+".toRegex()) && text !in ids) {
            ids.add(0, text)
            input = ""
            scope.launch { state.animateScrollToItem(0) }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.pref_blacklist)) },
        text = {
            Column {
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
                        keyboardActions = KeyboardActions(onDone = { addItem() }),
                    )
                    IconButton(onClick = { addItem() }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_to_blacklist),
                        )
                    }
                }
                LazyColumn(
                    state = state,
                    modifier = Modifier
                        .heightIn(max = 240.dp)
                        .scrollbar(state),
                ) {
                    items(items = ids, key = { it }) { id ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = id, modifier = Modifier.weight(1f))
                            IconButton(onClick = { ids.remove(id) }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.remove_from_blacklist),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    addItem()
                    storedValue = ids.joinToString(",")
                    onDismiss()
                }
            ) {
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

@Preview
@Composable
private fun Preview() {
    val flow = remember {
        MutableStateFlow<Preferences>(MapPreferences(mapOf("blacklist" to "440,570,730")))
    }
    IdleTheme {
        Box(Modifier.fillMaxSize()) {
            ProvidePreferenceLocals(flow = flow) {
                BlacklistEditDialog {}
            }
        }
    }
}