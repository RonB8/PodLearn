package com.example.podlingo.ui.vocabulary

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.podlingo.ui.strings.LocalAppStrings

@Composable
fun KnownWordsScreen(
    onBack: () -> Unit,
    viewModel: KnownWordsViewModel = hiltViewModel(),
) {
    val strings = LocalAppStrings.current
    val words by viewModel.knownWords.collectAsStateWithLifecycle()
    val sortMode by viewModel.sortMode.collectAsStateWithLifecycle()

    VocabListScaffold(
        title = strings.wordsYouKnowTitle,
        words = words,
        sortMode = sortMode,
        onSortModeChange = viewModel::setSortMode,
        emptyMessage = strings.noKnownWordsYet,
        onBack = onBack,
        onDeleteWords = viewModel::deleteWords,
        onAddWord = viewModel::addWord,
        strings = strings,
    ) { entry ->
        IconButton(onClick = { viewModel.markUnknown(entry.word) }) {
            Icon(Icons.Filled.Close, contentDescription = strings.iDontKnowThisWordAnymoreContentDescription)
        }
    }
}
