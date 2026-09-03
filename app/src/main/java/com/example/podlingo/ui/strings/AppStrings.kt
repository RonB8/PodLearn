package com.example.podlingo.ui.strings

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Every piece of static UI text in the app, in one place, so the whole app (except the player's
 * timeline/controls and the mini-player - see [com.example.podlingo.ui.player.PlaybackControls]
 * and [com.example.podlingo.ui.player.MiniPlayerBar]) can switch between [EnglishStrings] and
 * [HebrewStrings] together with the RTL layout flip. Dynamic content (episode titles, search
 * results, translations) is never part of this - only chrome: labels, buttons, descriptions.
 */
interface AppStrings {
    // Shared across screens
    val back: String
    val cancel: String
    val delete: String
    val save: String
    val done: String
    val add: String
    val rename: String
    val nameLabel: String
    val ready: String
    val play: String
    val pause: String
    val dismiss: String
    val addToPlaylist: String
    val translatingEllipsis: String

    // Home/Search/Library tab bar + top bar
    val tabHome: String
    val tabSearch: String
    val tabLibrary: String
    val settingsContentDescription: String

    // Settings screen
    val settingsTitle: String
    val themeLabel: String
    val themeLight: String
    val themeDark: String
    val themeSystem: String
    val languageLabel: String
    val languageEnglish: String
    val languageHebrew: String
    val hardWordModeTitle: String
    val hardWordModeDescription: String
    val autoFullSentenceTitle: String
    fun autoFullSentenceDescription(hardWordCount: Int): String
    val autoPlayNextTitle: String
    val autoPlayNextDescription: String
    val autoTranslateReadAloudTitle: String
    val autoTranslateReadAloudDescription: String
    val unknownWordsTitle: String
    val unknownWordsDescription: String
    fun aboutContentDescription(settingTitle: String): String
    val storageTitle: String
    fun storageUsageLabel(usedText: String, limitText: String): String
    val storageLimitDescription: String

    // Home tab
    val historyTab: String
    val noEpisodesPlayedYet: String
    val noPodcastsPlayedYet: String
    val justNow: String
    fun minutesAgo(minutes: Long): String
    fun hoursAgo(hours: Long): String
    fun daysAgo(days: Long): String
    fun weeksAgo(weeks: Long): String
    val quizMenuItem: String
    val removeFromHistoryConfirmTitle: String
    val removeFromHistoryConfirmText: String
    val noUnknownWordsToQuizMessage: String

    // Library tab
    val podcastsTab: String
    val playlistsTab: String

    // Search / add podcast
    val searchPodcastsLabel: String
    fun noPodcastsFoundFor(query: String): String
    val hideRssUrlEntry: String
    val addByRssUrlInstead: String
    val rssFeedUrlLabel: String
    val enterRssUrlError: String
    val failedToAddPodcastError: String
    val searchFailedError: String
    val alreadyAdded: String
    fun episodesCountSubtitle(count: Int): String

    // Podcasts list
    val noPodcastsYet: String

    // Episode list
    val episodesTitle: String
    val noEpisodesFound: String
    val transcriptNotYetProcessed: String
    val transcriptDownloading: String
    val transcriptTranscribing: String
    val transcriptProcessing: String
    val transcriptFailedTapToRetry: String
    val episodeFilterAll: String
    val episodeFilterDownloaded: String

    // Playlists
    val newPlaylistTitle: String
    val renamePlaylistTitle: String
    fun deletePlaylistConfirmTitle(playlistName: String): String
    val deletePlaylistConfirmText: String
    val noPlaylistsYet: String
    val moreOptionsContentDescription: String
    val newPlaylistContentDescription: String
    fun episodeCountLabel(count: Int): String
    val addToPlaylistTitle: String
    val noPlaylistsYetCreateBelow: String
    val newPlaylistFieldLabel: String
    val createAndAddContentDescription: String
    val playlistFallbackTitle: String
    val noEpisodesInPlaylist: String
    val removeFromPlaylistContentDescription: String

    // Unknown words / vocabulary
    val wordsYouDontKnowTitle: String
    val noUnknownWordsYet: String
    val iKnowThisWordNowContentDescription: String

    // Player
    val episodeFallbackTitle: String
    val downloadingEpisode: String
    fun transcribingSpeechPart(chunkIndex: Int, chunkCount: Int): String
    val transcribingSpeech: String
    val buildingTranscript: String
    val transcriptChip: String
    val hardWordChip: String
    val autoTranslateChip: String
    val noRelevantSentenceMessage: String
    val doYouKnowTheseWordsTitle: String
    val tapWordsExplanation: String
    val selectAll: String
    val deselectAll: String
    val continueLabel: String
    val reviewWhatYouLearnedTitle: String
    val wantToTryQuizText: String
    val yes: String
    val no: String
    val quizCompleteTitle: String
    fun youGotXOutOfYRight(correct: Int, total: Int): String
    fun questionXOfY(index: Int, total: Int): String
    val seeResults: String
    val next: String
    fun skipForwardSecondsContentDescription(seconds: Long): String
    fun skipBackSecondsContentDescription(seconds: Long): String
    val nextEpisodeContentDescription: String
}

object EnglishStrings : AppStrings {
    override val back = "Back"
    override val cancel = "Cancel"
    override val delete = "Delete"
    override val save = "Save"
    override val done = "Done"
    override val add = "Add"
    override val rename = "Rename"
    override val nameLabel = "Name"
    override val ready = "Ready"
    override val play = "Play"
    override val pause = "Pause"
    override val dismiss = "Dismiss"
    override val addToPlaylist = "Add to playlist"
    override val translatingEllipsis = "Translating…"

    override val tabHome = "Home"
    override val tabSearch = "Search"
    override val tabLibrary = "Library"
    override val settingsContentDescription = "Settings"

    override val settingsTitle = "Settings"
    override val themeLabel = "Theme"
    override val themeLight = "Light"
    override val themeDark = "Dark"
    override val themeSystem = "System"
    override val languageLabel = "Language"
    override val languageEnglish = "English"
    override val languageHebrew = "Hebrew"
    override val hardWordModeTitle = "Translate hardest word only"
    override val hardWordModeDescription = "On trigger, translate just the hardest word in the sentence " +
        "(by Oxford CEFR level). Trigger again right away on the same sentence to reveal the next-hardest word."
    override val autoFullSentenceTitle = "Auto full-sentence for hard sentences"
    override fun autoFullSentenceDescription(hardWordCount: Int) =
        "Within hard-word mode: if a sentence has $hardWordCount or more hard words, translate the whole " +
            "sentence instead of one word at a time."
    override val autoPlayNextTitle = "Auto-play next episode"
    override val autoPlayNextDescription = "When an episode finishes, automatically start the next one in the podcast."
    override val autoTranslateReadAloudTitle = "Read auto-translated words aloud"
    override val autoTranslateReadAloudDescription = "When Auto translate finds a word you don't know, pause and " +
        "read it aloud like the manual trigger does - just the word if hard-word mode is on, or the whole " +
        "sentence otherwise."
    override val unknownWordsTitle = "Words you don't know"
    override val unknownWordsDescription = "View and edit the words flagged for auto-translation."
    override fun aboutContentDescription(settingTitle: String) = "About $settingTitle"
    override val storageTitle = "Downloaded episode storage"
    override fun storageUsageLabel(usedText: String, limitText: String) = "$usedText of $limitText used"
    override val storageLimitDescription = "Once downloaded episodes pass this limit, the least recently " +
        "played ones are deleted automatically to make room. Their transcripts are kept, so they " +
        "re-download instantly if you open them again."

    override val historyTab = "History"
    override val noEpisodesPlayedYet = "No episodes played yet. Find something in Search."
    override val noPodcastsPlayedYet = "No podcast history yet. Play something to see it here."
    override val justNow = "just now"
    override fun minutesAgo(minutes: Long) = "${minutes}m ago"
    override fun hoursAgo(hours: Long) = "${hours}h ago"
    override fun daysAgo(days: Long) = "${days}d ago"
    override fun weeksAgo(weeks: Long) = "${weeks}w ago"
    override val quizMenuItem = "Quiz"
    override val removeFromHistoryConfirmTitle = "Remove from history?"
    override val removeFromHistoryConfirmText = "This removes it from your recently-played list. " +
        "It's still there in the podcast's own episode list."
    override val noUnknownWordsToQuizMessage = "No unknown words to quiz you on yet."

    override val podcastsTab = "Podcasts"
    override val playlistsTab = "Playlists"

    override val searchPodcastsLabel = "Search podcasts"
    override fun noPodcastsFoundFor(query: String) = "No podcasts found for \"$query\""
    override val hideRssUrlEntry = "Hide RSS URL entry"
    override val addByRssUrlInstead = "Add by RSS URL instead"
    override val rssFeedUrlLabel = "RSS feed URL"
    override val enterRssUrlError = "Enter an RSS feed URL"
    override val failedToAddPodcastError = "Failed to add podcast"
    override val searchFailedError = "Search failed"
    override val alreadyAdded = "Already added"
    override fun episodesCountSubtitle(count: Int) = "$count episodes"

    override val noPodcastsYet = "No podcasts yet. Find one in Search."

    override val episodesTitle = "Episodes"
    override val noEpisodesFound = "No episodes found in this feed."
    override val transcriptNotYetProcessed = "Not yet processed"
    override val transcriptDownloading = "Downloading..."
    override val transcriptTranscribing = "Transcribing..."
    override val transcriptProcessing = "Processing..."
    override val transcriptFailedTapToRetry = "Failed - tap to retry"
    override val episodeFilterAll = "All"
    override val episodeFilterDownloaded = "Downloaded"

    override val newPlaylistTitle = "New playlist"
    override val renamePlaylistTitle = "Rename playlist"
    override fun deletePlaylistConfirmTitle(playlistName: String) = "Delete \"$playlistName\"?"
    override val deletePlaylistConfirmText = "This removes the playlist. The episodes in it aren't affected."
    override val noPlaylistsYet = "No playlists yet. Tap + to create one."
    override val moreOptionsContentDescription = "More options"
    override val newPlaylistContentDescription = "New playlist"
    override fun episodeCountLabel(count: Int) = if (count == 1) "1 episode" else "$count episodes"
    override val addToPlaylistTitle = "Add to playlist"
    override val noPlaylistsYetCreateBelow = "No playlists yet - create one below."
    override val newPlaylistFieldLabel = "New playlist"
    override val createAndAddContentDescription = "Create and add"
    override val playlistFallbackTitle = "Playlist"
    override val noEpisodesInPlaylist = "No episodes in this playlist yet. Add some from the player screen."
    override val removeFromPlaylistContentDescription = "Remove from playlist"

    override val wordsYouDontKnowTitle = "Words you don't know"
    override val noUnknownWordsYet = "No unknown words yet. Mark words in the player's \"Auto translate\" " +
        "panel and they'll show up here."
    override val iKnowThisWordNowContentDescription = "I know this word now"

    override val episodeFallbackTitle = "Episode"
    override val downloadingEpisode = "Downloading episode..."
    override fun transcribingSpeechPart(chunkIndex: Int, chunkCount: Int) =
        "Transcribing speech (part $chunkIndex/$chunkCount)..."
    override val transcribingSpeech = "Transcribing speech (this can take a while)..."
    override val buildingTranscript = "Building transcript..."
    override val transcriptChip = "Transcript"
    override val hardWordChip = "Hard word"
    override val autoTranslateChip = "Auto translate"
    override val noRelevantSentenceMessage = "No relevant sentence found (that pause looks like it fell in a quiet stretch)."
    override val doYouKnowTheseWordsTitle = "Do you know these words?"
    override val tapWordsExplanation = "Tap any word you don't know - it'll translate automatically when it comes up."
    override val selectAll = "Select all"
    override val deselectAll = "Deselect all"
    override val continueLabel = "Continue"
    override val reviewWhatYouLearnedTitle = "Review what you learned?"
    override val wantToTryQuizText = "Want to try a quick quiz on the words you didn't know in this episode?"
    override val yes = "Yes"
    override val no = "No"
    override val quizCompleteTitle = "Quiz complete!"
    override fun youGotXOutOfYRight(correct: Int, total: Int) = "You got $correct out of $total right."
    override fun questionXOfY(index: Int, total: Int) = "Question $index/$total"
    override val seeResults = "See results"
    override val next = "Next"
    override fun skipForwardSecondsContentDescription(seconds: Long) = "Skip forward $seconds seconds"
    override fun skipBackSecondsContentDescription(seconds: Long) = "Skip back $seconds seconds"
    override val nextEpisodeContentDescription = "Next episode"
}

object HebrewStrings : AppStrings {
    override val back = "חזרה"
    override val cancel = "ביטול"
    override val delete = "מחיקה"
    override val save = "שמירה"
    override val done = "סיום"
    override val add = "הוספה"
    override val rename = "שינוי שם"
    override val nameLabel = "שם"
    override val ready = "מוכן"
    override val play = "ניגון"
    override val pause = "השהיה"
    override val dismiss = "סגירה"
    override val addToPlaylist = "הוספה לפלייליסט"
    override val translatingEllipsis = "מתרגם…"

    override val tabHome = "בית"
    override val tabSearch = "חיפוש"
    override val tabLibrary = "ספרייה"
    override val settingsContentDescription = "הגדרות"

    override val settingsTitle = "הגדרות"
    override val themeLabel = "ערכת נושא"
    override val themeLight = "בהיר"
    override val themeDark = "כהה"
    override val themeSystem = "ברירת מחדל"
    override val languageLabel = "שפה"
    override val languageEnglish = "אנגלית"
    override val languageHebrew = "עברית"
    override val hardWordModeTitle = "תרגום המילה הקשה ביותר בלבד"
    override val hardWordModeDescription = "בהפעלה, תתורגם רק המילה הקשה ביותר במשפט (לפי רמת Oxford CEFR). " +
        "הפעלה נוספת מיד על אותו משפט תחשוף את המילה הקשה הבאה."
    override val autoFullSentenceTitle = "משפט שלם אוטומטית במשפטים קשים"
    override fun autoFullSentenceDescription(hardWordCount: Int) =
        "במצב מילה קשה: אם יש במשפט $hardWordCount מילים קשות או יותר, יתורגם המשפט כולו במקום מילה אחת בכל פעם."
    override val autoPlayNextTitle = "ניגון אוטומטי של הפרק הבא"
    override val autoPlayNextDescription = "כשפרק מסתיים, הפרק הבא בפודקאסט יתחיל אוטומטית."
    override val autoTranslateReadAloudTitle = "הקראת מילים שתורגמו אוטומטית"
    override val autoTranslateReadAloudDescription = "כשהתרגום האוטומטי מוצא מילה שאינך מכיר/ה, הניגון יושהה " +
        "והיא תוקרא בקול כמו בהפעלה ידנית - רק המילה אם מצב מילה קשה פעיל, או המשפט כולו בכל מקרה אחר."
    override val unknownWordsTitle = "מילים שאינך מכיר/ה"
    override val unknownWordsDescription = "צפייה ועריכה של המילים המסומנות לתרגום אוטומטי."
    override fun aboutContentDescription(settingTitle: String) = "מידע על $settingTitle"
    override val storageTitle = "אחסון פרקים שהורדו"
    override fun storageUsageLabel(usedText: String, limitText: String) = "$usedText מתוך $limitText בשימוש"
    override val storageLimitDescription = "כשפרקים שהורדו עוברים את המגבלה הזו, הפרקים שהושמעו לאחרונה " +
        "הכי פחות יימחקו אוטומטית כדי לפנות מקום. התמלול שלהם נשמר, כך שהם יורדו מחדש באופן מיידי " +
        "אם תפתח אותם שוב."

    override val historyTab = "היסטוריה"
    override val noEpisodesPlayedYet = "עדיין לא הושמעו פרקים. אפשר למצוא משהו במסך החיפוש."
    override val noPodcastsPlayedYet = "עדיין אין היסטוריית פודקאסטים. השמעת משהו תוצג כאן."
    override val justNow = "הרגע"
    override fun minutesAgo(minutes: Long) = "לפני $minutes דק׳"
    override fun hoursAgo(hours: Long) = "לפני $hours שע׳"
    override fun daysAgo(days: Long) = "לפני $days ימים"
    override fun weeksAgo(weeks: Long) = "לפני $weeks שבועות"
    override val quizMenuItem = "חידון"
    override val removeFromHistoryConfirmTitle = "להסיר מההיסטוריה?"
    override val removeFromHistoryConfirmText = "הפעולה תסיר את הפרק מרשימת ההשמעות האחרונות שלך. " +
        "הוא עדיין יופיע ברשימת הפרקים של הפודקאסט עצמו."
    override val noUnknownWordsToQuizMessage = "עדיין אין מילים לא מוכרות לחידון."

    override val podcastsTab = "פודקאסטים"
    override val playlistsTab = "פלייליסטים"

    override val searchPodcastsLabel = "חיפוש פודקאסטים"
    override fun noPodcastsFoundFor(query: String) = "לא נמצאו פודקאסטים עבור \"$query\""
    override val hideRssUrlEntry = "הסתרת הזנת כתובת RSS"
    override val addByRssUrlInstead = "הוספה לפי כתובת RSS במקום זאת"
    override val rssFeedUrlLabel = "כתובת פיד RSS"
    override val enterRssUrlError = "יש להזין כתובת פיד RSS"
    override val failedToAddPodcastError = "הוספת הפודקאסט נכשלה"
    override val searchFailedError = "החיפוש נכשל"
    override val alreadyAdded = "כבר נוסף"
    override fun episodesCountSubtitle(count: Int) = "$count פרקים"

    override val noPodcastsYet = "עדיין אין פודקאסטים. אפשר למצוא אחד במסך החיפוש."

    override val episodesTitle = "פרקים"
    override val noEpisodesFound = "לא נמצאו פרקים בפיד הזה."
    override val transcriptNotYetProcessed = "עדיין לא עובד"
    override val transcriptDownloading = "מוריד..."
    override val transcriptTranscribing = "מתמלל..."
    override val transcriptProcessing = "מעבד..."
    override val transcriptFailedTapToRetry = "נכשל - יש להקיש כדי לנסות שוב"
    override val episodeFilterAll = "הכול"
    override val episodeFilterDownloaded = "שהורדו"

    override val newPlaylistTitle = "פלייליסט חדש"
    override val renamePlaylistTitle = "שינוי שם לפלייליסט"
    override fun deletePlaylistConfirmTitle(playlistName: String) = "למחוק את \"$playlistName\"?"
    override val deletePlaylistConfirmText = "הפעולה תמחק את הפלייליסט. הפרקים שבו לא ייפגעו."
    override val noPlaylistsYet = "עדיין אין פלייליסטים. יש להקיש על + כדי ליצור אחד."
    override val moreOptionsContentDescription = "עוד אפשרויות"
    override val newPlaylistContentDescription = "פלייליסט חדש"
    override fun episodeCountLabel(count: Int) = if (count == 1) "פרק אחד" else "$count פרקים"
    override val addToPlaylistTitle = "הוספה לפלייליסט"
    override val noPlaylistsYetCreateBelow = "עדיין אין פלייליסטים - אפשר ליצור אחד למטה."
    override val newPlaylistFieldLabel = "פלייליסט חדש"
    override val createAndAddContentDescription = "יצירה והוספה"
    override val playlistFallbackTitle = "פלייליסט"
    override val noEpisodesInPlaylist = "עדיין אין פרקים בפלייליסט הזה. אפשר להוסיף פרקים ממסך הנגן."
    override val removeFromPlaylistContentDescription = "הסרה מהפלייליסט"

    override val wordsYouDontKnowTitle = "מילים שאינך מכיר/ה"
    override val noUnknownWordsYet = "עדיין אין מילים לא מוכרות. יש לסמן מילים בפאנל \"תרגום אוטומטי\" " +
        "בנגן והן יופיעו כאן."
    override val iKnowThisWordNowContentDescription = "אני מכיר/ה את המילה הזו עכשיו"

    override val episodeFallbackTitle = "פרק"
    override val downloadingEpisode = "מוריד את הפרק..."
    override fun transcribingSpeechPart(chunkIndex: Int, chunkCount: Int) =
        "מתמלל דיבור (חלק $chunkIndex מתוך $chunkCount)..."
    override val transcribingSpeech = "מתמלל דיבור (זה עשוי לקחת זמן)..."
    override val buildingTranscript = "בונה תמלול..."
    override val transcriptChip = "תמלול"
    override val hardWordChip = "מילה קשה"
    override val autoTranslateChip = "תרגום אוטומטי"
    override val noRelevantSentenceMessage = "לא נמצא משפט רלוונטי (נראה שההשהיה הזו הייתה בקטע שקט)."
    override val doYouKnowTheseWordsTitle = "האם המילים האלה מוכרות לך?"
    override val tapWordsExplanation = "יש להקיש על כל מילה שאינך מכיר/ה - היא תתורגם אוטומטית כשהיא תופיע."
    override val selectAll = "בחירת הכול"
    override val deselectAll = "ביטול בחירת הכול"
    override val continueLabel = "המשך"
    override val reviewWhatYouLearnedTitle = "לסקור את מה שלמדת?"
    override val wantToTryQuizText = "לנסות חידון קצר על המילים שלא הכרת בפרק הזה?"
    override val yes = "כן"
    override val no = "לא"
    override val quizCompleteTitle = "החידון הושלם!"
    override fun youGotXOutOfYRight(correct: Int, total: Int) = "ענית נכון על $correct מתוך $total."
    override fun questionXOfY(index: Int, total: Int) = "שאלה $index מתוך $total"
    override val seeResults = "הצגת התוצאות"
    override val next = "הבא"
    override fun skipForwardSecondsContentDescription(seconds: Long) = "קפיצה קדימה ב-$seconds שניות"
    override fun skipBackSecondsContentDescription(seconds: Long) = "קפיצה אחורה ב-$seconds שניות"
    override val nextEpisodeContentDescription = "הפרק הבא"
}

val LocalAppStrings = staticCompositionLocalOf<AppStrings> { EnglishStrings }
