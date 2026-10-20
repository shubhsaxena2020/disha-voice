package org.dishavoice.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.dishavoice.app.DishaViewModel
import org.dishavoice.app.SessionState
import org.dishavoice.app.UiState

/**
 * Main screen: one obvious primary control, a clear status line, and secondary
 * actions that are large enough to hit reliably.
 */
@Composable
fun DishaScreen(viewModel: DishaViewModel, previewView: androidx.camera.view.PreviewView) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Disha Voice",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { contentDescription = "Disha Voice" },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (state.isHindiAvailable) {
                    "बोलकर जानें"
                } else {
                    "Speaking in English: no Hindi voice on this phone"
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            PreviewSurface(previewView = previewView, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(20.dp))

            StatusLine(state)

            Spacer(Modifier.height(20.dp))

            PrimaryControl(
                state = state,
                onStart = viewModel::start,
                onStop = viewModel::stop,
            )

            Spacer(Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                SecondaryControl(
                    label = "Repeat",
                    description = "Repeat the last spoken message",
                    onClick = viewModel::repeatLast,
                    modifier = Modifier.weight(1f),
                )
                SecondaryControl(
                    label = "Help",
                    description = "Explain what this app does",
                    onClick = viewModel::speakHelp,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(18.dp))

            SafetyNote()
        }
    }
}

@Composable
private fun StatusLine(state: UiState) {
    val spoken = state.lastAnnouncement.ifBlank { "Nothing said yet" }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = "Latest message: $spoken"
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = state.statusText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { stateDescription = state.statusText },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = spoken,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PrimaryControl(
    state: UiState,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val running = state.sessionState == SessionState.RUNNING
    val buttonText = if (running) "Stop" else "Start"
    val description = if (running) {
        "Stop the camera and stop speaking"
    } else {
        "Start the camera and begin describing what is visible"
    }

    Button(
        onClick = { if (running) onStop() else onStart() },
        modifier = Modifier
            .size(160.dp)
            .semantics {
                contentDescription = description
                stateDescription = if (running) "Running" else "Stopped"
            },
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (running) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        ),
    ) {
        Text(text = buttonText, fontSize = 26.sp)
    }
}

@Composable
private fun SecondaryControl(
    label: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .height(64.dp)
            .semantics { contentDescription = description },
    ) {
        Text(text = label, fontSize = 18.sp)
    }
}

@Composable
private fun SafetyNote() {
    Text(
        text = "This app describes images. It does not measure distance, detect hazards, or tell you where it is safe to walk.",
        style = MaterialTheme.typography.bodySmall,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 420.dp)
            .padding(top = 8.dp),
    )
}
