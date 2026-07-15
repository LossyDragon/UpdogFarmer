package com.steevsapps.idledaddy.ui.screen.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.steevsapps.idledaddy.BuildConfig
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.ui.component.IdleTopAppBar
import com.steevsapps.idledaddy.ui.component.LinkText
import com.steevsapps.idledaddy.ui.theme.IdleTheme

private const val SOURCE_CODE_URL = "https://github.com/LossyDragon/UpdogFarmer"
private const val STEAM_GROUP_URL = "https://steamcommunity.com/groups/idledaddy"
private const val STEAM_COMMUNITY_URL = "https://steamcommunity.com/id/"
private const val STEAM_COMMUNITY_PROFILE = "https://steamcommunity.com/profiles/"

private data class TranslatorEntry(val language: String, val contributors: Map<String, String?>)

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            IdleTopAppBar(
                title = stringResource(R.string.about),
                onNavClick = onBack
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            item { GeneralInfoText() }
            item { TranslatorsText() }
            item { LicenseText() }
        }
    }
}

@Composable
private fun GeneralInfoText() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader(stringResource(R.string.app_name))
        Column(
            modifier = Modifier.padding(start = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            ProvideTextStyle(
                value = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                content = {
                    Text(text = "Version Name: " + BuildConfig.VERSION_NAME)
                    Text(text = "Version Code: " + BuildConfig.VERSION_CODE)
                },
            )
            LinkText(text = stringResource(R.string.source_code), url = SOURCE_CODE_URL)
            LinkText(text = stringResource(R.string.steam_group), url = STEAM_GROUP_URL)
        }
    }
}

@Composable
private fun TranslatorsText() {
    val nameStyle = SpanStyle(
        fontSize = MaterialTheme.typography.bodySmall.fontSize,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader(stringResource(R.string.translations))
        Column(
            modifier = Modifier.padding(start = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            translators.forEach { entry ->
                Column {
                    Text(text = entry.language, fontWeight = FontWeight.Bold)
                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        text = contributorsText(entry.contributors, nameStyle)
                    )
                }
            }
        }
    }
}

@Composable
private fun LicenseText() {
    Column {
        SectionHeader(stringResource(R.string.license))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Text(
                text = stringResource(R.string.about_license),
                modifier = Modifier.padding(all = 16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text = text, style = MaterialTheme.typography.headlineSmall)
}

private fun contributorsText(contributors: Map<String, String?>, nameStyle: SpanStyle) =
    buildAnnotatedString {
        contributors.entries.forEachIndexed { index, (name, url) ->
            withStyle(nameStyle) {
                if (index > 0) append(", ")
                if (url != null) {
                    withLink(LinkAnnotation.Url(url = url)) {
                        append(name)
                    }
                } else {
                    append(name)
                }
            }
        }
    }

private val translators = listOf(
    TranslatorEntry(
        language = "German",
        contributors = mapOf(
            "Schokoladeneis" to STEAM_COMMUNITY_URL + "Schokoladeneis"
        ),
    ),
    TranslatorEntry(
        language = "Russian",
        contributors = mapOf(
            "tnka" to STEAM_COMMUNITY_URL + "tnka",
            "Nikita Sychev" to null,
            "EgoruOfficial" to null,
            "Andrei Fedoruk" to STEAM_COMMUNITY_URL + "AndreiFedorukKZ",
            "Павел Соснин" to null,
        ),
    ),
    TranslatorEntry(
        language = "Portuguese (European)",
        contributors = mapOf(
            "ZIGS_ARE_WINNER" to null
        ),
    ),
    TranslatorEntry(
        language = "Portuguese (Brazilian)",
        contributors = mapOf(
            "Eustress" to STEAM_COMMUNITY_URL + "rodrigo_dev",
            "FallcoN" to null,
        ),
    ),
    TranslatorEntry(
        language = "Ukrainian",
        contributors = mapOf(
            "Lacki23" to null,
            "younsiamed" to STEAM_COMMUNITY_PROFILE + "76561198042448346",
        ),
    ),
    TranslatorEntry(
        language = "Czech",
        contributors = mapOf(
            "David from CZPortal4Gamers" to null
        ),
    ),
    TranslatorEntry(
        language = "Polish",
        contributors = mapOf(
            "Grzegorz Królikowski" to STEAM_COMMUNITY_URL + "ffecjaz"
        ),
    ),
    TranslatorEntry(
        language = "Turkish",
        contributors = mapOf(
            "Bilal Bağcıoğlu" to STEAM_COMMUNITY_URL + "Lasfe",
            "Abdulkerim Köse" to null,
        ),
    ),
    TranslatorEntry(
        language = "Thai",
        contributors = mapOf(
            "Anit Boonlue" to null,
            "Pohui Somnam" to null
        ),
    ),
    TranslatorEntry(
        language = "Chinese (Simplified)",
        contributors = mapOf(
            "Zomby7e" to STEAM_COMMUNITY_URL + "zomby7e",
            "deluxghost" to STEAM_COMMUNITY_URL + "deluxghost",
        ),
    ),
    TranslatorEntry(
        language = "Chinese (Traditional)",
        contributors = mapOf(
            "Zomby7e" to STEAM_COMMUNITY_URL + "zomby7e"
        ),
    ),
    TranslatorEntry(
        language = "Bulgarian",
        contributors = mapOf(
            "psydex" to STEAM_COMMUNITY_PROFILE + "76561197990087627"
        ),
    ),
    TranslatorEntry(
        language = "Slovenian",
        contributors = mapOf(
            "Game Explorer" to STEAM_COMMUNITY_URL + "RoninHunteer1337"
        ),
    ),
    TranslatorEntry(
        language = "French",
        contributors = mapOf(
            "Saltyman" to STEAM_COMMUNITY_URL + "Saltymanfr"
        ),
    ),
    TranslatorEntry(
        language = "Romanian",
        contributors = mapOf(
            "ediXedi" to STEAM_COMMUNITY_URL + "ediXedi",
            "Pakake" to null,
        ),
    ),
    TranslatorEntry(
        language = "Spanish",
        contributors = mapOf(
            "Lucas Bengualid" to null
        )
    ),
    TranslatorEntry(
        language = "Arabic",
        contributors = mapOf(
            "Saif Jadalla (€ CrAz¥ €)" to STEAM_COMMUNITY_URL + "saifjadalla"
        ),
    ),
    TranslatorEntry(
        language = "Hebrew",
        contributors = mapOf(
            "eyal100" to STEAM_COMMUNITY_URL + "KF8"
        ),
    ),
    TranslatorEntry(
        language = "Indonesian",
        contributors = mapOf(
            "Steinmetz" to STEAM_COMMUNITY_URL + "Ghozizzz"
        ),
    ),
    TranslatorEntry(
        language = "Vietnamese",
        contributors = mapOf(
            "Catou" to STEAM_COMMUNITY_URL + "catouofficial"
        ),
    ),
    TranslatorEntry(
        language = "Persian",
        contributors = mapOf(
            "Amir.P" to STEAM_COMMUNITY_URL + "amircry"
        ),
    ),
    TranslatorEntry(
        language = "Bosnian, Croatian, Serbian",
        contributors = mapOf(
            "Eldin" to STEAM_COMMUNITY_URL + "eldinturkic"
        ),
    ),
)

/**
 * Preview
 */

@Preview
@Composable
private fun Preview() {
    IdleTheme {
        AboutScreen(onBack = {})
    }
}
