package com.example.dz2

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.compose.material3.ExperimentalMaterial3Api

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: MainViewModel = viewModel(factory = MainViewModelFactory())
            MainScreen(viewModel)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = { TopAppBar(title = { Text("GIF Gallery") }) },
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when {
                    state.isLoading && state.gifs.isEmpty() -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    state.error != null -> {
                        val errorMessage = state.error
                        Column(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(errorMessage ?: "Неизвестная ошибка")
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.retry() }) {
                                Text("Повторить")
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(8.dp)
                        ) {
                            items(state.gifs) { gif ->
                                GifItem(gif = gif) {
                                    val index = state.gifs.indexOf(gif)
                                    Toast.makeText(context, "Картинка №${index + 1}", Toast.LENGTH_SHORT).show()
                                }
                            }

                            if (state.isLoadingMore) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp)
                                            .wrapContentWidth(Alignment.CenterHorizontally)
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }

                            item {
                                Button(
                                    onClick = { viewModel.loadMore() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    enabled = !state.isLoadingMore && state.canLoadMore
                                ) {
                                    Text("Загрузить ещё")
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun GifItem(gif: Gif, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .clickable { onClick() }
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(gif.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = gif.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

// --- Models ---
data class Gif(val id: String, val title: String, val imageUrl: String)

data class GiphyResponse(val data: List<GiphyItem>)
data class GiphyItem(val id: String, val title: String, val images: Images)
data class Images(val original: ImageData)
data class ImageData(val url: String)

// --- API ---
interface GiphyApi {
    @GET("v1/gifs/trending")
    suspend fun getTrendingGifs(
        @Query("api_key") apiKey: String,
        @Query("limit") limit: Int = 25,
        @Query("offset") offset: Int
    ): GiphyResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://api.giphy.com/"
    val giphyApi: GiphyApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GiphyApi::class.java)
    }
}

// --- State & ViewModel ---
data class MainViewState(
    val gifs: List<Gif> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val canLoadMore: Boolean = true
)

class MainViewModel : ViewModel() {
    private var page = 0
    private val _state = MutableStateFlow(MainViewState(isLoading = true))
    val state: StateFlow<MainViewState> = _state.asStateFlow()

    private val cachedGifs = mutableListOf<Gif>()

    init {
        loadFirstPage()
    }

    private fun loadFirstPage() {
        loadGifs(0)
    }

    fun loadMore() {
        loadGifs(page * 25)
    }

    private fun loadGifs(offset: Int) {
        viewModelScope.launch {
            try {
                _state.value = _state.value.copy(
                    isLoading = offset == 0,
                    isLoadingMore = offset != 0,
                    error = null
                )

                val response = RetrofitClient.giphyApi.getTrendingGifs(
                    apiKey = BuildConfig.GIPHY_API_KEY,
                    limit = 25,
                    offset = offset
                )

                val newGifs = response.data.map { item ->
                    Gif(id = item.id, title = item.title, imageUrl = item.images.original.url)
                }

                if (newGifs.isNotEmpty()) {
                    cachedGifs.addAll(newGifs)
                    page++
                }

                _state.value = _state.value.copy(
                    gifs = cachedGifs.toList(),
                    isLoading = false,
                    isLoadingMore = false,
                    canLoadMore = newGifs.isNotEmpty()
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = "Ошибка загрузки"
                )
            }
        }
    }

    fun retry() {
        if (_state.value.error != null) {
            if (page == 0) {
                loadFirstPage()
            } else {
                loadMore()
            }
        }
    }
}

class MainViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel")
    }
}