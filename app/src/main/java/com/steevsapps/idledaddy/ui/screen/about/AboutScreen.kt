package com.steevsapps.idledaddy.ui.screen.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.steevsapps.idledaddy.R
import com.steevsapps.idledaddy.ui.component.LinkText
import com.steevsapps.idledaddy.ui.theme.IdleTheme

private const val SOURCE_CODE_URL = "https://github.com/LossyDragon/UpdogFarmer"
private const val STEAM_GROUP_URL = "https://steamcommunity.com/groups/idledaddy"

private data class Contributor(val name: String, val url: String? = null)
private data class TranslatorEntry(val language: String, val contributors: List<Contributor>)

@Composable
fun AboutScreen(onBack: () -> Unit) {
    IdleTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = stringResource(R.string.about)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    }
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
}

@Composable
private fun SectionHeader(text: String) {
    Text(text = text, style = MaterialTheme.typography.headlineSmall)
}

@Composable
private fun GeneralInfoText() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        SectionHeader(stringResource(R.string.app_name))
        Column(
            modifier = Modifier.padding(start = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
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

private fun contributorsText(contributors: List<Contributor>, nameStyle: SpanStyle) =
    buildAnnotatedString {
        contributors.forEachIndexed { index, contributor ->
            withStyle(nameStyle) {
                if (index > 0) append(", ")
                val url = contributor.url
                if (url != null) {
                    withLink(LinkAnnotation.Url(url = url)) {
                        append(contributor.name)
                    }
                } else {
                    append(contributor.name)
                }
            }
        }
    }

private val translators = listOf(
    TranslatorEntry(
        "German",
        listOf(Contributor("Schokoladeneis", "https://steamcommunity.com/id/Schokoladeneis/")),
    ),
    TranslatorEntry(
        "Russian",
        listOf(
            Contributor("Nikita Sychev"),
            Contributor("EgoruOfficial"),
            Contributor("Andrei Fedoruk", "https://steamcommunity.com/id/AndreiFedorukKZ"),
            Contributor("Павел Соснин"),
        ),
    ),
    TranslatorEntry("Portuguese (European)", listOf(Contributor("ZIGS_ARE_WINNER"))),
    TranslatorEntry(
        "Portuguese (Brazilian)",
        listOf(
            Contributor("Eustress", "https://steamcommunity.com/id/rodrigo_dev/"),
            Contributor("FallcoN"),
        ),
    ),
    TranslatorEntry(
        "Ukrainian",
        listOf(
            Contributor("Lacki23"),
            Contributor("younsiamed", "https://steamcommunity.com/profiles/76561198042448346/"),
        ),
    ),
    TranslatorEntry("Czech", listOf(Contributor("David from CZPortal4Gamers"))),
    TranslatorEntry(
        "Polish",
        listOf(Contributor("Grzegorz Królikowski", "https://steamcommunity.com/id/ffecjaz/")),
    ),
    TranslatorEntry(
        "Turkish",
        listOf(
            Contributor("Bilal Bağcıoğlu", "https://steamcommunity.com/id/Lasfe"),
            Contributor("Abdulkerim Köse"),
        ),
    ),
    TranslatorEntry("Thai", listOf(Contributor("Anit Boonlue"), Contributor("Pohui Somnam"))),
    TranslatorEntry(
        "Chinese (Simplified)",
        listOf(
            Contributor("Zomby7e", "https://steamcommunity.com/id/zomby7e"),
            Contributor("deluxghost", "https://steamcommunity.com/id/deluxghost"),
        ),
    ),
    TranslatorEntry(
        "Chinese (Traditional)",
        listOf(Contributor("Zomby7e", "https://steamcommunity.com/id/zomby7e")),
    ),
    TranslatorEntry(
        "Bulgarian",
        listOf(Contributor("psydex", "https://steamcommunity.com/profiles/76561197990087627/")),
    ),
    TranslatorEntry(
        "Slovenian",
        listOf(Contributor("Game Explorer", "https://steamcommunity.com/id/RoninHunteer1337/")),
    ),
    TranslatorEntry(
        "French",
        listOf(Contributor("Saltyman", "https://steamcommunity.com/id/Saltymanfr/"))
    ),
    TranslatorEntry(
        "Romanian",
        listOf(
            Contributor("ediXedi", "https://steamcommunity.com/id/ediXedi/"),
            Contributor("Pakake"),
        ),
    ),
    TranslatorEntry("Spanish", listOf(Contributor("Lucas Bengualid"))),
    TranslatorEntry(
        "Arabic",
        listOf(
            Contributor(
                "Saif Jadalla (€ CrAz¥ €)",
                "https://steamcommunity.com/id/saifjadalla"
            )
        ),
    ),
    TranslatorEntry("Hebrew", listOf(Contributor("eyal100", "https://steamcommunity.com/id/KF8"))),
    TranslatorEntry(
        "Indonesian",
        listOf(Contributor("Steinmetz", "https://steamcommunity.com/id/Ghozizzz"))
    ),
    TranslatorEntry(
        "Vietnamese",
        listOf(Contributor("Catou", "https://steamcommunity.com/id/catouofficial/"))
    ),
    TranslatorEntry(
        "Persian",
        listOf(Contributor("Amir.P", "https://steamcommunity.com/id/amircry/"))
    ),
    TranslatorEntry(
        "Bosnian, Croatian, Serbian",
        listOf(Contributor("Eldin", "https://steamcommunity.com/id/eldinturkic")),
    ),
)

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
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

/**
 * Preview
 */

@Preview
@Composable
private fun Preview() {
    AboutScreen(onBack = {})
}