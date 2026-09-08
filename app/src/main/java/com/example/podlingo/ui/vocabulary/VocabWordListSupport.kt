@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.podlingo.ui.vocabulary

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.podlingo.data.local.entity.WordKnowledgeEntity
import com.example.podlingo.data.repository.WordKnowledgeRepository
import com.example.podlingo.ui.common.ConfirmDialog
import com.example.podlingo.ui.strings.AppStrings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/** Shared by both the "Words you don't know" and "Words you know" settings lists so they sort identically. */
enum class VocabWordSortMode {
    ALPHABETICAL,
    LAST_ADDED,
}

/**
 * Lazily fetches & caches the Hebrew translation for each word the first time it's shown on a
 * vocab list screen - a word only ever gets fetched when a screen actually needs to display it,
 * never speculatively pre-fetched for a whole list. Guarded so a failed fetch isn't retried every
 * time the source Flow re-emits for some other word's change. Shared by the known/unknown-words
 * ViewModels rather than duplicated.
 */
class WordTranslationFetcher(
    private val wordKnowledgeRepository: WordKnowledgeRepository,
    private val scope: CoroutineScope,
) {
    private val fetchAttempted = mutableSetOf<String>()

    fun fetchMissing(words: List<WordKnowledgeEntity>) {
        words.filter { it.hebrewTranslation == null && it.word !in fetchAttempted }
            .forEach { entry ->
                fetchAttempted += entry.word
                scope.launch { wordKnowledgeRepository.getOrFetchTranslation(entry.word) }
            }
    }
}

/** The DAO-backed source flows are already alphabetical, so ALPHABETICAL is a passthrough. */
fun Flow<List<WordKnowledgeEntity>>.sortedForVocabList(sortMode: Flow<VocabWordSortMode>): Flow<List<WordKnowledgeEntity>> =
    combine(this, sortMode) { words, mode ->
        when (mode) {
            VocabWordSortMode.ALPHABETICAL -> words
            VocabWordSortMode.LAST_ADDED -> words.sortedByDescending { it.addedAtEpochMs }
        }
    }

/** Common sort state + delete-all + persistence-free toggling for a vocab list ViewModel. */
open class VocabWordListViewModel(private val wordKnowledgeRepository: WordKnowledgeRepository) : ViewModel() {
    protected val translationFetcher = WordTranslationFetcher(wordKnowledgeRepository, viewModelScope)

    private val _sortMode = MutableStateFlow(VocabWordSortMode.ALPHABETICAL)
    val sortMode: StateFlow<VocabWordSortMode> = _sortMode.asStateFlow()

    fun setSortMode(mode: VocabWordSortMode) {
        _sortMode.value = mode
    }

    protected val sortModeFlow: StateFlow<VocabWordSortMode> get() = _sortMode

    /** Permanently forgets these words - clears status and cached translation, so they can be asked about again in future calibration/quizzes. */
    fun deleteWords(words: List<String>) {
        viewModelScope.launch { wordKnowledgeRepository.deleteWords(words) }
    }
}

/** The sort icon + dropdown shared by both vocab list screens' TopAppBar actions. */
@Composable
fun VocabSortMenuAction(
    sortMode: VocabWordSortMode,
    onSortModeChange: (VocabWordSortMode) -> Unit,
    strings: AppStrings,
) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { showMenu = true }) {
            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = strings.sortMenuContentDescription)
        }
        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
            DropdownMenuItem(
                text = { Text(strings.sortByAlphabetical) },
                onClick = {
                    showMenu = false
                    onSortModeChange(VocabWordSortMode.ALPHABETICAL)
                },
                trailingIcon = {
                    if (sortMode == VocabWordSortMode.ALPHABETICAL) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                },
            )
            DropdownMenuItem(
                text = { Text(strings.sortByLastAdded) },
                onClick = {
                    showMenu = false
                    onSortModeChange(VocabWordSortMode.LAST_ADDED)
                },
                trailingIcon = {
                    if (sortMode == VocabWordSortMode.LAST_ADDED) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                },
            )
        }
    }
}

/** The word/translation list shared by both vocab list screens - only the trailing per-row action differs. */
@Composable
fun VocabWordListContent(
    words: List<WordKnowledgeEntity>,
    emptyMessage: String,
    strings: AppStrings,
    modifier: Modifier = Modifier,
    trailingAction: @Composable (WordKnowledgeEntity) -> Unit,
) {
    if (words.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = emptyMessage,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(words, key = { it.word }) { entry ->
                ListItem(
                    headlineContent = { Text(entry.word) },
                    supportingContent = {
                        val translation = entry.hebrewTranslation
                        if (translation != null) {
                            Text(translation)
                        } else {
                            Text(strings.translatingEllipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    trailingContent = { trailingAction(entry) },
                )
                HorizontalDivider()
            }
        }
    }
}

/**
 * The whole screen shared by "Words you don't know" and "Words you know" - top bar (back, sort,
 * delete-all), a search field that filters by word or translation, the word list, and the
 * delete-all confirmation. "Delete all" only ever acts on the currently search-filtered words, so
 * with no search text it's the same as clearing the whole list.
 */
@Composable
fun VocabListScaffold(
    title: String,
    words: List<WordKnowledgeEntity>,
    sortMode: VocabWordSortMode,
    onSortModeChange: (VocabWordSortMode) -> Unit,
    emptyMessage: String,
    onBack: () -> Unit,
    onDeleteWords: (List<String>) -> Unit,
    onAddWord: (String) -> Unit,
    strings: AppStrings,
    trailingAction: @Composable (WordKnowledgeEntity) -> Unit,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddWordDialog by remember { mutableStateOf(false) }

    val filteredWords = remember(words, searchQuery) {
        if (searchQuery.isBlank()) {
            words
        } else {
            words.filter {
                it.word.contains(searchQuery, ignoreCase = true) ||
                    it.hebrewTranslation?.contains(searchQuery, ignoreCase = true) == true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                actions = {
                    VocabSortMenuAction(sortMode = sortMode, onSortModeChange = onSortModeChange, strings = strings)
                    IconButton(onClick = { showDeleteConfirm = true }, enabled = filteredWords.isNotEmpty()) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = strings.deleteAllContentDescription)
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddWordDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = strings.addWordContentDescription)
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text(strings.searchWordsLabel) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Close, contentDescription = strings.clearSearchContentDescription)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
            VocabWordListContent(
                words = filteredWords,
                emptyMessage = if (searchQuery.isNotBlank()) strings.noWordsMatchSearch else emptyMessage,
                strings = strings,
                modifier = Modifier.weight(1f),
                trailingAction = trailingAction,
            )
        }
    }

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = strings.deleteAllConfirmTitle,
            text = strings.deleteAllConfirmText(filteredWords.size),
            confirmLabel = strings.delete,
            onConfirm = {
                showDeleteConfirm = false
                onDeleteWords(filteredWords.map { it.word })
            },
            onDismiss = { showDeleteConfirm = false },
        )
    }

    if (showAddWordDialog) {
        AddWordDialog(
            onConfirm = onAddWord,
            onDismiss = { showAddWordDialog = false },
            strings = strings,
        )
    }
}

@Composable
private fun AddWordDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    strings: AppStrings,
) {
    var word by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.addWordDialogTitle) },
        text = {
            OutlinedTextField(
                value = word,
                onValueChange = { word = it },
                label = { Text(strings.wordFieldLabel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(word); onDismiss() }, enabled = word.isNotBlank()) {
                Text(strings.add)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        },
    )
}
