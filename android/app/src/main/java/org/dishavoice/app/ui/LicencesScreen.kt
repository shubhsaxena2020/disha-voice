package org.dishavoice.app.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Shows the app's third-party notices and the bundled licence text.
 *
 * ## Why this screen exists
 *
 * The app redistributes Apache-2.0 components, and Apache-2.0 section 4(a)
 * requires that the licence be conveyed to anyone who receives the work. The
 * notices and the licence text are therefore shipped **inside the APK** as
 * assets, so the artifact itself carries them.
 *
 * Shipping them in the APK is necessary but not sufficient for this app. The
 * users of a screen reader cannot read a file inside an APK, so a screen that
 * makes the notices reachable is the only way the obligation is actually met
 * for them. The text is therefore rendered as ordinary, selectable Compose
 * text, which TalkBack reads normally, and the screen is reachable from the
 * main screen in one tap.
 *
 * The full text is long, so the screen opens with a spoken summary rather
 * than making the user scroll to find out what they are looking at.
 */
@Composable
fun LicencesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val notices = remember { readAsset(context, "THIRD_PARTY_NOTICES.txt") }
    val licence = remember { readAsset(context, "LICENSE-Apache-2.0.txt") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Text(
            text = "Open source licences",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { contentDescription = "Open source licences" },
        )
        Text(
            text = "This app bundles third-party software. " +
                "The full list and the licence texts are below, and are also " +
                "published in the project repository.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp),
        ) {
            Text(
                text = notices,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                modifier = Modifier.semantics {
                    contentDescription = "Third party notices. ${notices.length} characters."
                },
            )
            Text(
                text = licence,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 12.sp,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .semantics {
                        contentDescription = "Apache License version 2.0 full text."
                    },
            )
        }

        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .semantics { contentDescription = "Back to the main screen" },
        ) {
            Text("Back", fontSize = 18.sp)
        }
    }
}

/**
 * Reads a bundled asset, returning a short placeholder rather than throwing
 * if it is missing: a missing licence file must not crash the app.
 */
internal fun readAsset(context: Context, name: String): String = runCatching {
    context.assets.open(name).bufferedReader().use { it.readText() }
}.getOrElse {
    "This notice file could not be read on this device. " +
        "The full notices are published in the project repository."
}
