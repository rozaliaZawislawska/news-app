package com.basic.newsapp.viewModel

import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.basic.newsapp.model.Article
import com.basic.newsapp.util.parseRss
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.database.ktx.database
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.net.URL


class ArticleViewModel : ViewModel() {

    private val rssUrl = "https://wiadomosci.gazeta.pl/pub/rss/wiadomosci_kraj.xml"
    private val database = Firebase.database.reference
    private val userFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        Firebase.auth.addAuthStateListener(listener)
        awaitClose { Firebase.auth.removeAuthStateListener(listener) }
    }

    private val _rawArticlesFlow = MutableStateFlow<List<Article>>(emptyList())

    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex = _selectedTabIndex.asStateFlow()

    fun selectTab(index: Int) {
        _selectedTabIndex.value = index
    }

    fun fetchArticles() {
        viewModelScope.launch {
            try {
                val parsedArticles = withContext(Dispatchers.IO) {
                    val inputStream = URL(rssUrl).openStream()
                    parseRss(inputStream)
                    }
                    val cleanedArticles = parsedArticles.map { article ->
                        article.copy(
                            content = Jsoup.parse(article.content).text()
                        )
                    }

                    _rawArticlesFlow.value = cleanedArticles

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    private val visitedArticlesFlow: Flow<Set<String>> = userFlow.flatMapLatest { user ->
        if (user == null) {
            flowOf(emptySet())
        } else {
            callbackFlow {
                val userVisitedRef = database.child("users").child(user.uid).child("visitedArticles")
                val listener = userVisitedRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) { trySend(snapshot.children.mapNotNull { it.key }.toSet()) }
                    override fun onCancelled(error: DatabaseError) { close(error.toException()) }
                })
                awaitClose { userVisitedRef.removeEventListener(listener) }
            }
        }
    }
    fun markArticleAsVisited(article: Article) {
        val currentUserId = Firebase.auth.currentUser?.uid ?: "Unknown"
        if (article.guid.isBlank()) return

        val safeKey = Base64.encodeToString(
            article.guid.toByteArray(),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )

        database.child("users").child(currentUserId).child("visitedArticles")
            .child(safeKey)
            .setValue(true)
    }
    private val favoriteArticlesFlow: Flow<Set<String>> = userFlow.flatMapLatest { user ->
        if (user == null) {
            flowOf(emptySet())
        } else {
            callbackFlow {
                val userFavoritesRef = database.child("users").child(user.uid).child("favoriteArticles")
                val listener = userFavoritesRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) { trySend(snapshot.children.mapNotNull { it.key }.toSet()) }
                    override fun onCancelled(error: DatabaseError) { close(error.toException()) }
                })
                awaitClose { userFavoritesRef.removeEventListener(listener) }
            }
        }
    }

    fun markArticleAsFavorite(article: Article) {
        val currentUserId = Firebase.auth.currentUser?.uid ?: "Unknown"
        if (article.guid.isBlank()) return

        val safeKey = Base64.encodeToString(
            article.guid.toByteArray(),
            Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        )

        val articleRef = database.child("users").child(currentUserId).child("favoriteArticles")
            .child(safeKey)

        if (article.isFavorite) {
            articleRef.removeValue()
        } else {
            articleRef.setValue(true)
        }
    }
    val uiState: StateFlow<ArticleUiState> = combine(
        _rawArticlesFlow,
        visitedArticlesFlow,
        favoriteArticlesFlow,
        selectedTabIndex
    ) { articles, visitedGuids, favoritesArticles, tabIndex  ->
        if (articles.isEmpty()) {
            ArticleUiState.Loading
        } else {
            val articlesWithStatus = articles.map { article ->
                val safeKey = Base64.encodeToString(
                    article.guid.toByteArray(),
                    Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
                )

                val isVisited = safeKey in visitedGuids
                val isFavorite = safeKey in favoritesArticles

                article.copy(isVisited = isVisited, isFavorite = isFavorite)
            }

            val filteredArticles = if (tabIndex == 1) {
                articlesWithStatus.filter { it.isFavorite }
            } else {
                articlesWithStatus
            }

            ArticleUiState.Success(filteredArticles)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ArticleUiState.Loading
    )
}

sealed interface ArticleUiState {
    data object Loading : ArticleUiState
    data class Success(val articles: List<Article>) : ArticleUiState
    data class Error(val message: String) : ArticleUiState
}