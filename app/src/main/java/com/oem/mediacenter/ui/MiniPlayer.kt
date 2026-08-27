package com.oem.mediacenter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oem.mediacenter.R
import com.oem.medialib.NowPlayingState
import com.oem.mediacenter.ui.theme.TouchMin

@Composable
fun MiniPlayer(
    state: NowPlayingState,
    visible: Boolean,
    onOpenNowPlaying: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    rootModifier: Modifier = Modifier,
) {
    val title = state.title
    if (!visible || title.isNullOrBlank()) return

    Row(
        rootModifier
            .fillMaxWidth()
            .height(TouchMin + 8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onOpenNowPlaying)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = state.subtitle
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(
            onClick = onPlayPause,
            enabled = state.canPlayPause,
            modifier = Modifier.size(TouchMin),
        ) {
            Icon(
                imageVector = if (state.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = stringResource(
                    if (state.isPlaying) R.string.pause else R.string.play,
                ),
            )
        }
        IconButton(
            onClick = onSkipNext,
            enabled = state.canSkipNext,
            modifier = Modifier.size(TouchMin),
        ) {
            Icon(Icons.Default.SkipNext, contentDescription = stringResource(R.string.skip_next))
        }
    }
}
