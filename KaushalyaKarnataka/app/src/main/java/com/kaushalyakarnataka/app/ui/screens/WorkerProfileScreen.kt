package com.kaushalyakarnataka.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.kaushalyakarnataka.app.data.*
import com.kaushalyakarnataka.app.ui.Strings
import com.kaushalyakarnataka.app.ui.categoryTitle
import com.kaushalyakarnataka.app.ui.components.GalaxyBackground
import com.kaushalyakarnataka.app.ui.components.GlassCard
import com.kaushalyakarnataka.app.ui.components.GradientPrimaryButton
import com.kaushalyakarnataka.app.ui.components.ScreenLoading
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkerProfileScreen(
    workerId: String,
    customer: UserProfile?,
    strings: Strings,
    repo: KaushalyaRepository,
    onBack: () -> Unit,
    onHired: () -> Unit,
    onMessage: (String) -> Unit,
) {
    var worker by remember { mutableStateOf<UserProfile?>(null) }
    var reviews by remember { mutableStateOf<List<ReviewEntry>>(emptyList()) }
    var services by remember { mutableStateOf<List<ServiceItem>>(emptyList()) }
    var portfolio by remember { mutableStateOf<List<PortfolioItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var hiring by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(workerId) {
        loading = true
        worker = repo.fetchUser(workerId)
        reviews = runCatching { repo.fetchReviewsForWorker(workerId) }.getOrElse { emptyList() }
        services = runCatching { repo.fetchServices(workerId) }.getOrElse { emptyList() }
        portfolio = runCatching { repo.fetchPortfolio(workerId) }.getOrElse { emptyList() }
        loading = false
    }

    Scaffold(
        bottomBar = {
            val w = worker
            if (w != null && customer != null) {
                Surface(
                    color = Color(0xF20F172A),
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp
                ) {
                    GradientPrimaryButton(
                        text = if (hiring) strings.loading else "Hire Now",
                        onClick = {
                            hiring = true
                            scope.launch {
                                val res = repo.hireWorker(customer.userId, workerId)
                                hiring = false
                                res.onSuccess {
                                    onMessage(strings.hireSent)
                                    onHired()
                                }.onFailure { e ->
                                    val msg = when (e.message) {
                                        "duplicate_request" -> strings.duplicateRequest
                                        else -> strings.hireFailed
                                    }
                                    onMessage(msg)
                                }
                            }
                        },
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(16.dp)
                            .fillMaxWidth(),
                        loading = hiring,
                    )
                }
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        GalaxyBackground {
            if (loading) {
                ScreenLoading(label = strings.loading)
            } else if (worker == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(strings.workerNotFound, style = MaterialTheme.typography.titleLarge)
                }
            } else {
                val w = worker!!
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = padding.calculateBottomPadding())
                ) {
                    // Immersive Banner Header
                    Box(Modifier.fillMaxWidth().height(300.dp)) {
                        AsyncImage(
                            model = w.profileImage,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(Brush.verticalGradient(
                                    listOf(Color.Transparent, Color.Black.copy(0.8f))
                                ))
                        )
                        
                        // Back Button
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .padding(16.dp)
                                .statusBarsPadding()
                                .size(40.dp)
                                .background(Color.Black.copy(0.4f), CircleShape)
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = Color.White)
                        }

                        Column(
                            Modifier.align(Alignment.BottomStart).padding(20.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = strings.categoryTitle(w.category ?: ""),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = w.name,
                                style = MaterialTheme.typography.displayLarge,
                                color = Color.White
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = Color.White.copy(0.7f), modifier = Modifier.size(16.dp))
                                Text(
                                    text = w.location.ifBlank { "Location Unknown" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(0.7f)
                                )
                            }
                        }
                    }

                    Column(
                        Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Stats Section
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            WorkerStatCard(
                                modifier = Modifier.weight(1f),
                                label = "Rating",
                                value = if (w.rating > 0) String.format("%.1f", w.rating) else "New",
                                icon = Icons.Outlined.Star,
                                color = Color(0xFFFBBF24)
                            )
                            WorkerStatCard(
                                modifier = Modifier.weight(1f),
                                label = "Jobs Done",
                                value = "${w.jobsCompleted}",
                                icon = Icons.Outlined.CheckCircle,
                                color = Color(0xFF10B981)
                            )
                        }

                        // Bio
                        SectionHeader(title = "About ${w.name.split(" ").first()}")
                        Text(
                            text = w.bio?.takeIf { it.isNotBlank() } ?: strings.bioFallback,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(0.8f)
                        )

                        // Services
                        if (services.isNotEmpty()) {
                            SectionHeader(title = "Services Offered")
                            services.forEach { s ->
                                ServiceCardModern(s)
                            }
                        }

                        // Portfolio
                        if (portfolio.isNotEmpty()) {
                            SectionHeader(title = "Portfolio")
                            Row(
                                Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                portfolio.forEach { item ->
                                    AsyncImage(
                                        model = item.imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(220.dp)
                                            .clip(RoundedCornerShape(20.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                }
                            }
                        }

                        // Reviews
                        if (reviews.isNotEmpty()) {
                            SectionHeader(title = "Customer Reviews")
                            reviews.forEach { r ->
                                WorkerReviewCard(r)
                            }
                        }

                        Spacer(Modifier.height(40.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun WorkerStatCard(modifier: Modifier, label: String, value: String, icon: ImageVector, color: Color) {
    GlassCard(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ServiceCardModern(s: ServiceItem) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(text = s.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (s.description.isNotBlank()) {
                    Text(text = s.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                text = "₹${s.price.toInt()}",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun WorkerReviewCard(r: ReviewEntry) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(5) { idx ->
                    Icon(
                        imageVector = if (idx < r.rating) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = null,
                        tint = if (idx < r.rating) Color(0xFFFBBF24) else Color.White.copy(0.1f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            if (r.comment.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(text = r.comment, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
            }
        }
    }
}
