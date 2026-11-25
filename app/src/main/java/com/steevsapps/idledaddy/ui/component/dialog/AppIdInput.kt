package com.steevsapps.idledaddy.ui.component.dialog

// Anything that isn't a digit separates appids. Keyboards differ in which comma they
// type (",", "，", "،", ...), so don't depend on a fixed set of separators.
private val APP_ID_SEPARATOR = Regex("[^\\p{Nd}]+")

/** What the user typed into an appid field, split into usable appids and everything else. */
internal data class AppIdInput(
    val appIds: List<Int>,
    /** The tokens that aren't appids, joined back up so they can stay in the text field. */
    val rejected: String,
)

/** Parse one or more appids separated by spaces and/or commas. */
internal fun parseAppIdInput(text: String): AppIdInput {
    val appIds = mutableListOf<Int>()
    val rejected = mutableListOf<String>()
    text.split(APP_ID_SEPARATOR).filter { it.isNotEmpty() }.forEach { token ->
        val id = token.toIntOrNull()
        if (id != null && id > 0) appIds.add(id) else rejected.add(token)
    }
    return AppIdInput(appIds.distinct(), rejected.joinToString(" "))
}
