package com.basic.newsapp.view;

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.basic.newsapp.R
import com.basic.newsapp.model.Article
import com.basic.newsapp.viewModel.ArticleUiState
import com.basic.newsapp.viewModel.ArticleViewModel
import com.basic.newsapp.viewModel.AuthViewModel
import org.jsoup.Jsoup
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.IconButton


@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun ArticleApp(useremail: String, articleViewModel: ArticleViewModel = viewModel(), authViewModel: AuthViewModel =viewModel()) {
    val selectedTabIndex by articleViewModel.selectedTabIndex.collectAsStateWithLifecycle()
    val tabTitles = listOf("Wszystkie", "Ulubione")

    MaterialTheme {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = {
                            (Text("Wiadomości dla " + useremail))
                        },
                        actions = {
                            IconButton(onClick = {
                                authViewModel.signOut()
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Wyloguj"
                                )
                            }
                        }
                    )
                    TabRow(selectedTabIndex = selectedTabIndex) {
                        tabTitles.forEachIndexed { index, title ->
                            Tab(
                                selected = (selectedTabIndex == index),
                                onClick = { articleViewModel.selectTab(index) },
                                text = { Text(title) }
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            ArticleListScreen(
                modifier = Modifier.padding(innerPadding),
                articleViewModel = articleViewModel
            )
        }
    }

}


@Composable
fun ArticleListScreen(
    modifier: Modifier = Modifier,
    articleViewModel: ArticleViewModel
) {
    val context = LocalContext.current
    LaunchedEffect(key1 = Unit) {
        articleViewModel.fetchArticles()
    }
    val uiState by articleViewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is ArticleUiState.Loading -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is ArticleUiState.Success -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = state.articles) { article ->
                    ArticleCard(
                        article = article,
                        onArticleClicked = {
                            articleViewModel.markArticleAsVisited(article)

                            val intent = Intent(Intent.ACTION_VIEW, article.link.toUri())
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Nie można otworzyć linku", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onFavoriteClicked = {
                            articleViewModel.markArticleAsFavorite(article)
                        }
                    )
                }
            }
        }
        is ArticleUiState.Error -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Błąd ładowania danych: ${state.message}")
            }
        }
    }
}

@Composable
fun ArticleCard(
    article: Article,
    onArticleClicked: () -> Unit,
    onFavoriteClicked: () -> Unit) {
    val textColor = if (article.isVisited) {
        Color.Gray
    } else {
        Color.Black
    }
    Log.d("","visited? = "+article.isVisited.toString())
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onArticleClicked
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(article.imageUrl)
                    .crossfade(true)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .build(),
                contentDescription = "Zdjęcie do artykułu: ${article.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = article.title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = Jsoup.parse(article.content).text(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor
                )
                IconButton(
                    onClick = onFavoriteClicked,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Icon(
                        imageVector = if (article.isFavorite) {
                            Icons.Filled.Favorite
                        } else {
                            Icons.Outlined.FavoriteBorder
                        },
                        contentDescription = "Dodaj do ulubionych",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

