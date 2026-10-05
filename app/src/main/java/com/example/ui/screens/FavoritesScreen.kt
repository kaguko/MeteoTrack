package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.FavoritePlaceEntity
import com.example.data.model.GeocodingLocation
import com.example.ui.components.CenteredContent
import com.example.ui.components.EmptyState
import com.example.ui.components.InfoBanner
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenLight
import com.example.ui.theme.WarningAmber
import com.example.ui.theme.WarningAmberLight
import com.example.ui.viewmodel.WeatherViewModel

private data class CategoryOption(val key: String, val label: String, val icon: ImageVector)

private val Categories = listOf(
    CategoryOption("HOME", "Nhà", Icons.Default.Home),
    CategoryOption("WORK", "Công ty", Icons.Default.Business),
    CategoryOption("TRAVEL", "Du lịch", Icons.Default.Flight),
    CategoryOption("CUSTOM", "Khác", Icons.Default.Place),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FavoritesScreen(
    viewModel: WeatherViewModel,
    onLocationSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    val favorites by viewModel.favoritePlaces.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val searchError by viewModel.searchError.collectAsStateWithLifecycle()
    val searchedQuery by viewModel.searchedQuery.collectAsStateWithLifecycle()

    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("CUSTOM") }
    val focusManager = LocalFocusManager.current
    val searching = query.isNotBlank()

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { TopAppBar(title = { Text("Địa điểm", style = MaterialTheme.typography.titleLarge) }) }
    ) { innerPadding ->
        CenteredContent(modifier = Modifier.padding(innerPadding)) {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        viewModel.searchLocations(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_location_input"),
                    placeholder = { Text("Tìm thành phố, địa danh…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = {
                                query = ""
                                viewModel.clearSearch()
                            }) { Icon(Icons.Default.Clear, contentDescription = "Xóa nội dung tìm kiếm") }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(16.dp)
                )

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (searching) {
                        item {
                            Text(
                                "Lưu với nhãn",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Categories.forEach { option ->
                                    FilterChip(
                                        selected = category == option.key,
                                        onClick = { category = option.key },
                                        label = { Text(option.label) },
                                        leadingIcon = {
                                            Icon(option.icon, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    )
                                }
                            }
                        }

                        when {
                            isSearching -> item {
                                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                                }
                            }

                            searchError != null -> item {
                                InfoBanner(
                                    text = searchError.orEmpty(),
                                    icon = Icons.Default.WifiOff,
                                    containerColor = MaterialTheme.colorScheme.errorContainer,
                                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }

                            searchResults.isEmpty() && searchedQuery == query.trim() -> item {
                                EmptyState(
                                    icon = Icons.Default.SearchOff,
                                    title = "Không tìm thấy “${query.trim()}”",
                                    message = "Hãy thử tên khác hoặc viết đầy đủ hơn."
                                )
                            }

                            else -> items(searchResults, key = { it.id }) { location ->
                                SearchResultItem(
                                    location = location,
                                    onView = {
                                        focusManager.clearFocus()
                                        viewModel.selectSearchResult(location)
                                        query = ""
                                        viewModel.clearSearch()
                                        onLocationSelected()
                                    },
                                    onSave = {
                                        focusManager.clearFocus()
                                        viewModel.addFavorite(location, category)
                                        query = ""
                                    }
                                )
                            }
                        }
                    } else {
                        item {
                            Text(
                                text = "Đã lưu (${favorites.size})",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .semantics { heading() }
                            )
                        }
                        if (favorites.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = Icons.Default.StarBorder,
                                    title = "Chưa có địa điểm nào",
                                    message = "Tìm một thành phố ở trên để xem thời tiết nhanh và lưu lại cho lần sau."
                                )
                            }
                        } else {
                            items(favorites, key = { it.id }) { favorite ->
                                FavoriteItemCard(
                                    favorite = favorite,
                                    onSelect = {
                                        viewModel.selectFavoriteLocation(favorite)
                                        onLocationSelected()
                                    },
                                    onDelete = { viewModel.removeFavorite(favorite) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    location: GeocodingLocation,
    onView: () -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(role = Role.Button, onClickLabel = "Xem thời tiết", onClick = onView)
                    .heightIn(min = 64.dp)
                    .padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 8.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(location.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = location.fullDisplayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onSave, modifier = Modifier.testTag("add_favorite_button")) {
                Icon(Icons.Default.Add, contentDescription = "Lưu ${location.name}", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun FavoriteItemCard(
    favorite: FavoritePlaceEntity,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val dark = isSystemInDarkTheme()
    val (icon, color) = when (favorite.category) {
        "HOME" -> Icons.Default.Home to if (dark) SuccessGreenLight else SuccessGreen
        "WORK" -> Icons.Default.Business to MaterialTheme.colorScheme.primary
        "TRAVEL" -> Icons.Default.Flight to if (dark) WarningAmberLight else WarningAmber
        else -> Icons.Default.Place to MaterialTheme.colorScheme.secondary
    }
    val categoryLabel = Categories.firstOrNull { it.key == favorite.category }?.label ?: "Khác"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("favorite_item_${favorite.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(role = Role.Button, onClickLabel = "Xem thời tiết", onClick = onSelect)
                    .heightIn(min = 72.dp)
                    .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)
                    .semantics(mergeDescendants = true) { contentDescription = "${favorite.name}, $categoryLabel" },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(favorite.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = favorite.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.testTag("delete_favorite_${favorite.id}")) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Xóa ${favorite.name}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
