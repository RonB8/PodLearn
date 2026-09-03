package com.example.podlingo.ui.vocabulary

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.podlingo.ui.player.VocabQuizState
import com.example.podlingo.ui.strings.LocalAppStrings
import kotlinx.coroutines.flow.SharedFlow

/** Wires a [QuizSessionController][com.example.podlingo.ui.player.QuizSessionController]'s state to the screen: shows [VocabQuizDialog] while a quiz is open, and toasts when Quiz was tapped on an episode with nothing to quiz on. Shared by every episode-list screen that offers the "Quiz" row action. */
@Composable
fun EpisodeQuizHost(
    quiz: VocabQuizState?,
    noUnknownWordsEvent: SharedFlow<Unit>,
    onAnswerSelected: (String) -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val strings = LocalAppStrings.current
    LaunchedEffect(Unit) {
        noUnknownWordsEvent.collect {
            Toast.makeText(context, strings.noUnknownWordsToQuizMessage, Toast.LENGTH_SHORT).show()
        }
    }
    quiz?.let {
        VocabQuizDialog(
            quiz = it,
            onAnswerSelected = onAnswerSelected,
            onNext = onNext,
            onDismiss = onDismiss,
        )
    }
}
