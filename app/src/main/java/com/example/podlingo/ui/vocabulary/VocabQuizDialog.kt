package com.example.podlingo.ui.vocabulary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.podlingo.ui.player.VocabQuizState
import com.example.podlingo.ui.strings.LocalAppStrings

/**
 * One question at a time, then a score summary - [quiz]'s current question drives right/wrong
 * reveal via color once [VocabQuizState.answeredThisQuestion] is set. Shared between the
 * end-of-episode quiz prompt in the player and the Home tab's on-demand "Quiz" menu item.
 */
@Composable
fun VocabQuizDialog(
    quiz: VocabQuizState,
    onAnswerSelected: (String) -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit,
) {
    val strings = LocalAppStrings.current
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(modifier = Modifier.padding(24.dp).fillMaxWidth()) {
                // The quiz is otherwise modal with no way out mid-way through - this lets the user
                // bail at any question, same as finishing normally (see the two ViewModels' onQuizDismissed).
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = strings.dismiss)
                    }
                }
                if (quiz.finished) {
                    Text(strings.quizCompleteTitle, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = strings.youGotXOutOfYRight(quiz.correctCount, quiz.questions.size),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text(strings.done)
                    }
                } else {
                    val question = quiz.questions[quiz.currentIndex]
                    val answered = quiz.answeredThisQuestion
                    Text(
                        text = strings.questionXOfY(quiz.currentIndex + 1, quiz.questions.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(question.word, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    question.options.forEach { option ->
                        val isCorrectOption = option == question.correctAnswer
                        val containerColor = when {
                            answered == null -> MaterialTheme.colorScheme.surfaceVariant
                            isCorrectOption -> MaterialTheme.colorScheme.primaryContainer
                            option == answered -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                        val contentColor = when {
                            answered == null -> MaterialTheme.colorScheme.onSurfaceVariant
                            isCorrectOption -> MaterialTheme.colorScheme.onPrimaryContainer
                            option == answered -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Button(
                            onClick = { if (answered == null) onAnswerSelected(option) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = containerColor, contentColor = contentColor),
                        ) {
                            Text(option)
                        }
                    }
                    if (answered != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
                            Text(if (quiz.currentIndex + 1 >= quiz.questions.size) strings.seeResults else strings.next)
                        }
                    }
                }
            }
        }
    }
}
