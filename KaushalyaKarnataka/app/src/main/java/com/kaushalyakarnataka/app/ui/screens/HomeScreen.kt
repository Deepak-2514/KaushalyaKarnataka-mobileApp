package com.kaushalyakarnataka.app.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kaushalyakarnataka.app.data.KaushalyaRepository
import com.kaushalyakarnataka.app.data.UserProfile
import com.kaushalyakarnataka.app.ui.AppLanguage
import com.kaushalyakarnataka.app.ui.Strings
import com.kaushalyakarnataka.app.ui.categoryTitle
import com.kaushalyakarnataka.app.ui.components.GlassCard
import com.kaushalyakarnataka.app.ui.components.KKTextField
import com.kaushalyakarnataka.app.ui.components.ScreenLoading
import com.kaushalyakarnataka.app.ui.theme.KaushalyaColors

private val HOME_CATEGORIES = listOf(
    "All", "Electrician", "Plumber", "Carpenter", "Painter", "Cleaner", "Gardener", "Other",
)

@Composable
fun HomeScreen(
    profile: UserProfile?,
    strings: Strings,
    language: AppLanguage,
    onToggleLanguage: () -> Unit,
    repo: KaushalyaRepository,
    onOpenWorker: (String) -> Unit,
) {
    var workers by remember { mutableStateOf<List<UserProfile>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var tick by remember { mutableIntStateOf(0) }
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tick) {
        loading = true
        workers = runCatching { repo.fetchWorkers() }.getOrElse { emptyList() }
        loading = false
    }

    Scaffold(
        containerColor = KaushalyaColors.Background
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(56.dp))
            
            HeaderSection(profile, strings, onRefresh = { tick++ })

            Spacer(Modifier.height(28.dp))

            KKTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = "Find your next expert...",
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = KaushalyaColors.Primary) },
                trailingIcon = {
                    IconButton(onClick = {}) { 
                        Icon(Icons.Rounded.Tune, contentDescription = null, tint = KaushalyaColors.TextSecondary) 
                    }
                }
            )

            Spacer(Modifier.height(28.dp))

            CategorySelector(category, strings, onCategorySelected = { category = it })

            Spacer(Modifier.height(24.dp))

            val filtered = workers.filter { w ->
                val q = search.lowercase()
                val matchSearch = w.name.lowercase().contains(q) ||
                        (w.category ?: "").lowercase().contains(q)
                val matchCat = category == null || category == "All" || w.category == category
                matchSearch && matchCat
            }

            if (loading) {
                ScreenLoading(label = strings.loading)
            } else if (filtered.isEmpty()) {
                EmptyState(strings.noWorkers)
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 120.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = "Featured Experts",
                            style = MaterialTheme.typography.titleLarge,
                            color = KaushalyaColors.TextPrimary
                        )
                    }
                    items(filtered, key = { it.userId }) { worker ->
                        WorkerHorizontalCard(strings, worker, onOpenWorker)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderSection(
    profile: UserProfile?,
    strings: Strings,
    onRefresh: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(KaushalyaColors.Secondary)
                .border(BorderStroke(1.dp, KaushalyaColors.Border), RoundedCornerShape(14.dp))
        ) {
            AsyncImage(
                model = profile?.profileImage,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            val first = profile?.name?.split(" ")?.firstOrNull() ?: "there"
            Text(
                "Good Morning, $first",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = KaushalyaColors.TextPrimary
            )
            Text(
                "Find professional help today",
                style = MaterialTheme.typography.labelSmall,
                color = KaushalyaColors.TextSecondary,
            )
        }
        IconButton(
            onClick = onRefresh,
            modifier = Modifier
                .clip(CircleShape)
                .background(KaushalyaColors.Secondary)
        ) {
            Icon(Icons.Rounded.Notifications, contentDescription = null, tint = KaushalyaColors.TextPrimary)
        }
    }
}

@Composable
private fun CategorySelector(
    selectedCategory: String?,
    strings: Strings,
    onCategorySelected: (String?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(HOME_CATEGORIES) { cat ->
            val isSelected = (selectedCategory == null && cat == "All") || selectedCategory == cat
            CategoryChip(
                label = strings.categories.label(cat),
                isSelected = isSelected,
                onClick = { onCategorySelected(if (cat == "All") null else cat) },
                icon = getCategoryIcon(cat)
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector?
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) KaushalyaColors.Primary.copy(alpha = 0.15f) else KaushalyaColors.Secondary,
        border = BorderStroke(1.dp, if (isSelected) KaushalyaColors.Primary.copy(alpha = 0.5f) else KaushalyaColors.Border)
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    icon, 
                    contentDescription = null, 
                    modifier = Modifier.size(16.dp),
                    tint = if (isSelected) KaushalyaColors.Primary else KaushalyaColors.TextMuted
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) KaushalyaColors.Primary else KaushalyaColors.TextSecondary,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

private fun getCategoryIcon(cat: String): ImageVector? = when (cat) {
    "Electrician" -> Icons.Default.ElectricBolt
    "Plumber" -> Icons.Outlined.WaterDrop
    "Carpenter" -> Icons.Outlined.Handyman
    "Painter" -> Icons.Outlined.Brush
    "Cleaner" -> Icons.Outlined.CleaningServices
    "Gardener" -> Icons.Outlined.Yard
    "Other" -> Icons.Outlined.Category
    else -> null
}

@Composable
private fun EmptyState(message: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.SearchOff, 
            contentDescription = null, 
            modifier = Modifier.size(64.dp),
            tint = KaushalyaColors.TextMuted.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = KaushalyaColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun WorkerHorizontalCard(strings: Strings, worker: UserProfile, onOpenWorker: (String) -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = { onOpenWorker(worker.userId) },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box {
                AsyncImage(
                    model = worker.profileImage,
                    contentDescription = worker.name,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                )
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = KaushalyaColors.Warning, modifier = Modifier.size(10.dp))
                        Text(
                            if (worker.rating > 0) String.format("%.1f", worker.rating) else "New",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        worker.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = KaushalyaColors.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Rounded.Verified, contentDescription = "Verified", tint = KaushalyaColors.Primary, modifier = Modifier.size(14.dp))
                }
                Text(
                    strings.categoryTitle(worker.category ?: ""),
                    style = MaterialTheme.typography.labelLarge,
                    color = KaushalyaColors.Primary,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = KaushalyaColors.TextMuted)
                    Text(
                        worker.location.ifBlank { strings.locationUnknown },
                        style = MaterialTheme.typography.labelSmall,
                        color = KaushalyaColors.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = {}) {
                Icon(Icons.Outlined.BookmarkBorder, contentDescription = null, tint = KaushalyaColors.TextMuted)
            }
        }
        
        Spacer(Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Jobs Completed", style = MaterialTheme.typography.labelSmall, color = KaushalyaColors.TextMuted)
                Text("${worker.jobsCompleted}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = KaushalyaColors.TextPrimary)
            }
            Button(
                onClick = { onOpenWorker(worker.userId) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KaushalyaColors.Primary),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text("Hire Expert", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
