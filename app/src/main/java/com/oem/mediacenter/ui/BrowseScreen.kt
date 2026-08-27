package com.oem.mediacenter.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oem.mediacenter.R
import com.oem.mediacenter.data.BrowseNode
import com.oem.mediacenter.data.ConnectionState
import com.oem.mediacenter.ui.theme.TouchMin

@Composable
fun BrowseScreen(
    state: BrowseUiState,
    connectionState: ConnectionState,
    onBack: () -> Unit,
    onRetryRoot: () -> Unit,
    onNodeClick: (BrowseNode) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(TouchMin)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = state.title.ifBlank { stringResource(R.string.browse_title) },
                style = MaterialTheme.typography.headlineMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        when (connectionState) {
            ConnectionState.Connecting -> {
                StatusLine(stringResource(R.string.connecting))
            }
            is ConnectionState.Failed -> {
                StatusLine(connectionState.message)
                TextButton(onClick = onRetryRoot) { Text("Retry") }
            }
            ConnectionState.Disconnected -> {
                StatusLine(stringResource(R.string.disconnected))
                TextButton(onClick = onRetryRoot) { Text("Retry") }
            }
            else -> Unit
        }

        when {
            state.isLoading -> {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Text(text = state.error, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetryRoot) { Text("Retry") }
            }
            state.nodes.isEmpty() -> {
                Text(
                    text = stringResource(R.string.empty_library),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.nodes, key = { it.mediaId }) { node ->
                        BrowseRow(node = node, onClick = { onNodeClick(node) })
                    }
                }
            }
        }
    }
}

@Composable
private fun BrowseRow(node: BrowseNode, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(TouchMin)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = if (node.isBrowsable) Icons.Default.Folder else Icons.Default.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp),
        )
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(
                text = node.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!node.subtitle.isNullOrBlank()) {
                Text(
                    text = node.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StatusLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp, top = 12.dp),
    )
}
