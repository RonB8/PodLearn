package com.example.podlingo.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.podlingo.data.repository.EpisodeDownload
import com.example.podlingo.data.repository.PreprocessingProgress
import com.example.podlingo.ui.strings.LocalAppStrings

/**
 * A slim, tappable row for whatever's downloading/transcribing right now, docked above the
 * mini-player/tab bar so it's visible no matter which screen is on top - the app-wide counterpart
 * to [PlayerViewModel]'s own full-screen preprocessing view for the episode you're actively on.
 * Shows the oldest still-running download; additional ones are summarized rather than listed, to
 * keep this a slim status row rather than a second list.
 */
@Composable
fun DownloadingBanner(
    downloads: List<EpisodeDownload>,
    onClick: (episodeId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalAppStrings.current
    val primary = downloads.firstOrNull() ?: return
    val fraction = (primary.progress as? PreprocessingProgress.Downloading)?.fraction?.takeIf { it >= 0f }

    // Pinned to Ltr regardless of app language, same as MiniPlayerBar - this row sits right above
    // it and reads as the same kind of system status chrome, not user content.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Surface(
            modifier = modifier.fillMaxWidth().clickable { onClick(primary.episodeId) },
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DownloadIndicator(fraction = fraction)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = primary.episodeTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                val trailingText = if (downloads.size > 1) {
                    strings.moreDownloadsSuffix(downloads.size - 1)
                } else {
                    fraction?.let { "${(it * 100).toInt()}%" } ?: primary.progress.label(strings)
                }
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** A small ring: determinate once a download fraction is known, indeterminate before that (still fetching headers) and through transcription/processing (no fraction to report). */
@Composable
private fun DownloadIndicator(fraction: Float?) {
    Icon(
        imageVector = Icons.Filled.Download,
        contentDescription = null,
        modifier = Modifier.size(16.dp),
    )
    Spacer(modifier = Modifier.width(6.dp))
    if (fraction != null) {
        CircularProgressIndicator(progress = { fraction }, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
    } else {
        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
    }
}
