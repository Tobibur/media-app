package com.oem.mediacenter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.oem.mediacenter.R
import com.oem.mediacenter.data.NowPlayingState
import com.oem.mediacenter.ui.theme.TouchMin
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun NowPlayingScreen(
    state: NowPlayingState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(TouchMin)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Text(
                text = stringResource(R.string.now_playing_title),
                style = MaterialTheme.typography.headlineMedium,
            )
        }

        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 32.dp)
                .size(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(40.dp),
            tint = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = state.title ?: stringResource(R.string.nothing_playing),
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 24.dp),
        )
        if (!state.subtitle.isNullOrBlank()) {
            Text(
                text = state.subtitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        SeekSection(
            state = state,
            onSeek = onSeek,
            modifier = Modifier.padding(top = 24.dp).fillMaxWidth(),
        )

        Row(
            Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = onSkipPrevious,
                enabled = state.canSkipPrevious,
                modifier = Modifier.size(TouchMin),
            ) {
                Icon(Icons.Default.SkipPrevious, contentDescription = stringResource(R.string.skip_previous))
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
                    modifier = Modifier.size(48.dp),
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
}

@Composable
private fun SeekSection(
    state: NowPlayingState,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val duration = state.durationMs.coerceAtLeast(0L)
    var sliding by remember(state.positionMs, duration) {
        mutableFloatStateOf(
            if (duration > 0) state.positionMs.toFloat() / duration.toFloat() else 0f,
        )
    }

    Column(modifier) {
        Slider(
            value = sliding.coerceIn(0f, 1f),
            onValueChange = {
                if (state.canSeek && duration > 0) sliding = it
            },
            onValueChangeFinished = {
                if (state.canSeek && duration > 0) {
                    onSeek((sliding * duration).toLong())
                }
            },
            enabled = state.canSeek && duration > 0,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatTime(state.positionMs), style = MaterialTheme.typography.labelLarge)
            Text(formatTime(duration), style = MaterialTheme.typography.labelLarge)
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0L))
    val minutes = totalSec / 60
    val seconds = totalSec % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}
