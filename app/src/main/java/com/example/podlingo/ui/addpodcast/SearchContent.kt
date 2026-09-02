package com.example.podlingo.ui.addpodcast

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/** The Search tab - the sole way to add a podcast, replacing the old "+" flow. */
@Composable
fun SearchContent(
    onAdded: (String) -> Unit,
    viewModel: AddPodcastViewModel = hiltViewModel(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val searchState by viewModel.searchState.collectAsStateWithLifecycle()
    val addState by viewModel.addState.collectAsStateWithLifecycle()
    var showManualEntry by rememberSaveable { mutableStateOf(false) }
    var manualUrl by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(addState) {
        val state = addState
        if (state is AddPodcastUiState.Success) {
            onAdded(state.podcastId)
            // Otherwise addState is still Success the moment this screen is recomposed (e.g. after
            // navigating back from the episode list this just opened), and this effect fires again
            // immediately - re-navigating forward and making back navigation look like it does nothing.
            viewModel.consumeAddResult()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChanged,
                label = { Text("Search podcasts") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (val state = searchState) {
                    is PodcastSearchUiState.Idle -> Unit
                    is PodcastSearchUiState.Loading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                    )
                    is PodcastSearchUiState.Results -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(state.items, key = { it.result.feedUrl }) { item ->
                            PodcastSearchResultCard(item = item, onClick = { viewModel.selectResult(item) })
                        }
                    }
                    is PodcastSearchUiState.NoResults -> Text(
                        text = "No podcasts found for \"$query\"",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    )
                    is PodcastSearchUiState.NetworkError -> Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                TextButton(onClick = { showManualEntry = !showManualEntry }) {
                    Text(if (showManualEntry) "Hide RSS URL entry" else "Add by RSS URL instead")
                }
                if (showManualEntry) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualUrl,
                        onValueChange = { manualUrl = it },
                        label = { Text("RSS feed URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.addPodcast(manualUrl) },
                        enabled = addState !is AddPodcastUiState.Loading,
                    ) {
                        Text("Add")
                    }
                }
                val error = (addState as? AddPodcastUiState.Error)?.message
                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = error, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        if (addState is AddPodcastUiState.Loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
