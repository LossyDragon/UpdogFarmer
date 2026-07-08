package com.steevsapps.idledaddy.ui.component.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.steevsapps.idledaddy.R
import me.zhanghai.compose.preference.rememberPreferenceState

@Composable
fun BlacklistEditDialog(onDismiss: () -> Unit) {
    var storedValue by rememberPreferenceState("blacklist", "")
    val ids = remember { storedValue.split(",").filter { it.isNotEmpty() }.toMutableStateList() }
    var input by rememberSaveable { mutableStateOf("") }

    fun addItem() {
        val text = input.trim()
        if (text.matches("\\d+".toRegex()) && text !in ids) {
            ids.add(0, text)
            input = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.pref_blacklist)) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            // autoSize seems neat.
                            Text(
                                text = stringResource(R.string.blacklist_hint),
                                maxLines = 1,
                                autoSize = TextAutoSize.StepBased(
                                    minFontSize = 10.sp,
                                    stepSize = 1.sp,
                                ),
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { addItem() }),
                        singleLine = true,
                    )
                    IconButton(onClick = { addItem() }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.add_to_blacklist),
                        )
                    }
                }
                LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
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