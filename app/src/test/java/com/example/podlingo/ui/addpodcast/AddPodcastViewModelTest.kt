package com.example.podlingo.ui.addpodcast

import com.example.podlingo.data.local.entity.PodcastEntity
import com.example.podlingo.data.remote.podcastsearch.PodcastSearchResult
import com.example.podlingo.data.repository.PodcastRepository
import com.example.podlingo.testutil.FakeCallFactory
import com.example.podlingo.testutil.FakeEpisodeDao
import com.example.podlingo.testutil.FakePodcastDao
import com.example.podlingo.testutil.FakePodcastSearchRepository
import com.example.podlingo.testutil.okXmlResponse
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AddPodcastViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        searchRepository: FakePodcastSearchRepository = FakePodcastSearchRepository(),
        podcastDao: FakePodcastDao = FakePodcastDao(),
        episodeDao: FakeEpisodeDao = FakeEpisodeDao(),
        callFactory: FakeCallFactory = FakeCallFactory { okXmlResponse(it, SAMPLE_RSS) },
    ): AddPodcastViewModel {
        val podcastRepository = PodcastRepository(callFactory, podcastDao, episodeDao)
        return AddPodcastViewModel(podcastRepository, searchRepository)
    }

    @Test
    fun `idle query produces idle search state`() = runTest {
        val vm = viewModel()

        assertEquals(PodcastSearchUiState.Idle, vm.searchState.value)
    }

    @Test
    fun `search debounces rapid query changes into a single call`() = runTest {
        val search = FakePodcastSearchRepository().apply { result = Result.success(listOf(sampleResult())) }
        val vm = viewModel(searchRepository = search)
        val job = launch { vm.searchState.collect {} }

        vm.onQueryChanged("s")
        advanceTimeBy(100)
        vm.onQueryChanged("sw")
        advanceTimeBy(100)
        vm.onQueryChanged("swift")
        advanceUntilIdle()

        assertEquals(1, search.callCount)
        assertEquals("swift", search.lastQuery)
        job.cancel()
    }

    @Test
    fun `successful search with results emits Results state`() = runTest {
        val search = FakePodcastSearchRepository().apply { result = Result.success(listOf(sampleResult())) }
        val vm = viewModel(searchRepository = search)
        val job = launch { vm.searchState.collect {} }

        vm.onQueryChanged("swift")
        advanceUntilIdle()

        val state = vm.searchState.value
        assertTrue(state is PodcastSearchUiState.Results)
        val items = (state as PodcastSearchUiState.Results).items
        assertEquals(1, items.size)
        assertFalse(items.single().alreadyAdded)
        job.cancel()
    }

    @Test
    fun `empty results emit NoResults`() = runTest {
        val search = FakePodcastSearchRepository().apply { result = Result.success(emptyList()) }
        val vm = viewModel(searchRepository = search)
        val job = launch { vm.searchState.collect {} }

        vm.onQueryChanged("nonexistent")
        advanceUntilIdle()

        assertEquals(PodcastSearchUiState.NoResults, vm.searchState.value)
        job.cancel()
    }

    @Test
    fun `search failure emits NetworkError`() = runTest {
        val search = FakePodcastSearchRepository().apply { result = Result.failure(IOException("offline")) }
        val vm = viewModel(searchRepository = search)
        val job = launch { vm.searchState.collect {} }

        vm.onQueryChanged("swift")
        advanceUntilIdle()

        assertEquals(PodcastSearchUiState.NetworkError("offline"), vm.searchState.value)
        job.cancel()
    }

    @Test
    fun `results already in the library are flagged as already added`() = runTest {
        val podcastDao = FakePodcastDao()
        podcastDao.insert(samplePodcastEntity(feedUrl = FEED_URL))
        val search = FakePodcastSearchRepository().apply { result = Result.success(listOf(sampleResult())) }
        val vm = viewModel(searchRepository = search, podcastDao = podcastDao)
        val job = launch { vm.searchState.collect {} }

        vm.onQueryChanged("swift")
        advanceUntilIdle()

        val state = vm.searchState.value as PodcastSearchUiState.Results
        assertTrue(state.items.single().alreadyAdded)
        job.cancel()
    }

    /**
     * [com.example.podlingo.data.remote.rss.RssParser] depends on `android.util.Xml`, which
     * plain JVM unit tests stub to throw (this project has no Robolectric, matching every other
     * repository test here) - so the fetch's outcome can't be driven to Success. What's still
     * genuinely verifiable without Robolectric: selecting a result requests exactly its feed URL
     * (the selection -> persistence wiring), and a fetch failure correctly surfaces as an error
     * state rather than silently stalling.
     */
    @Test
    fun `selecting a new result requests its feed url and surfaces the outcome`() = runTest {
        val podcastDao = FakePodcastDao()
        var requestedUrl: String? = null
        val callFactory = FakeCallFactory { request ->
            requestedUrl = request.url.toString()
            okXmlResponse(request, SAMPLE_RSS)
        }
        val vm = viewModel(podcastDao = podcastDao, callFactory = callFactory)

        vm.selectResult(PodcastSearchResultItem(sampleResult(), alreadyAdded = false))
        val state = awaitSettledAddState(vm)

        assertEquals(FEED_URL, requestedUrl)
        assertTrue(state is AddPodcastUiState.Error)
    }

    @Test
    fun `selecting an already-added result resolves locally without a network call`() = runTest {
        val podcastDao = FakePodcastDao()
        val existing = samplePodcastEntity(feedUrl = FEED_URL)
        podcastDao.insert(existing)
        val callFactory = FakeCallFactory { throw AssertionError("should not hit the network") }
        val vm = viewModel(podcastDao = podcastDao, callFactory = callFactory)

        vm.selectResult(PodcastSearchResultItem(sampleResult(), alreadyAdded = true))
        val state = awaitSettledAddState(vm)

        assertEquals(AddPodcastUiState.Success(existing.id), state)
    }

    /**
     * [PodcastRepository.addPodcastByRssUrl] hops onto the real [kotlinx.coroutines.Dispatchers.IO]
     * (unrelated to this feature, left untouched), which [advanceUntilIdle] can't fast-forward -
     * it only controls the test dispatcher. A real suspension via [kotlinx.coroutines.flow.first]
     * lets the underlying event loop actually wait for that hop to finish.
     */
    private suspend fun awaitSettledAddState(vm: AddPodcastViewModel): AddPodcastUiState =
        kotlinx.coroutines.withContext(Dispatchers.Default.limitedParallelism(1)) {
            kotlinx.coroutines.withTimeout(5_000) {
                vm.addState.first { it !is AddPodcastUiState.Loading && it !is AddPodcastUiState.Idle }
            }
        }

    @Test
    fun `blank manual url shows an error`() = runTest {
        val vm = viewModel()

        vm.addPodcast("   ")

        assertEquals(AddPodcastUiState.Error("Enter an RSS feed URL"), vm.addState.value)
    }

    private fun sampleResult(feedUrl: String = FEED_URL) = PodcastSearchResult(
        feedUrl = feedUrl,
        title = "Swift Talk",
        author = "Some Author",
        artworkUrl = "https://example.com/art.jpg",
        genre = "Technology",
        episodeCount = 10,
    )

    private fun samplePodcastEntity(feedUrl: String) = PodcastEntity(
        id = "existing-id",
        feedUrl = feedUrl,
        title = "Swift Talk",
        imageUrl = null,
        description = null,
    )

    companion object {
        private const val FEED_URL = "https://example.com/feed.xml"
        private const val SAMPLE_RSS = """<?xml version="1.0"?>
<rss><channel>
<title>Swift Talk</title>
<description>desc</description>
<item><title>Episode 1</title><guid>ep1</guid><enclosure url="https://example.com/ep1.mp3" /></item>
</channel></rss>"""
    }
}
