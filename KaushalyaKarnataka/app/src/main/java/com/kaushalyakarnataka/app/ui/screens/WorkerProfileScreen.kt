package com.kaushalyakarnataka.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.LocationOn
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
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.kaushalyakarnataka.app.data.*
import com.kaushalyakarnataka.app.ui.Strings
import com.kaushalyakarnataka.app.ui.categoryTitle
import com.kaushalyakarnataka.app.ui.components.GlassCard
import com.kaushalyakarnataka.app.ui.components.GradientPrimaryButton
import com.kaushalyakarnataka.app.ui.components.ScreenLoading
import com.kaushalyakarnataka.app.ui.theme.KaushalyaColors
import kotlinx.coroutines.launch

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
        containerColor = KaushalyaColors.Background,
        bottomBar = {
            val w = worker
            if (w != null && customer != null) {
                Surface(
                    color = KaushalyaColors.NavBackground,
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    border = BorderStroke(1.dp, KaushalyaColors.Border),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    GradientPrimaryButton(
                        text = if (hiring) strings.loading else "Confirm Hire",
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
                            .padding(20.dp)
                            .fillMaxWidth(),
                        loading = hiring,
                    )
                }
            }
        }
    ) { padding ->
        if (loading) {
            ScreenLoading(label = strings.loading)
        } else if (worker == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(strings.workerNotFound, style = MaterialTheme.typography.titleLarge, color = KaushalyaColors.TextPrimary)
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
                Box(Modifier.fillMaxWidth().height(320.dp)) {
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
                                listOf(Color.Transparent, KaushalyaColors.Background.copy(0.9f), KaushalyaColors.Background)
                            ))
                    )
                    
                    // Back Button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(16.dp)
                            .statusBarsPadding()
                            .size(44.dp)
                            .background(KaushalyaColors.Background.copy(0.4f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = Color.White)
                    }

                    Column(
                        Modifier.align(Alignment.BottomStart).padding(20.dp)
                    ) {
                        Surface(
                            color = KaushalyaColors.Primary,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = strings.categoryTitle(w.category ?: ""),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = w.name,
                            style = MaterialTheme.typography.displayLarge,
                            color = KaushalyaColors.TextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = KaushalyaColors.TextSecondary, modifier = Modifier.size(16.dp))
                            Text(
                                text = w.location.ifBlank { "Remote" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = KaushalyaColors.TextSecondary
                            )
                        }
                    }
                }

                Column(
                    Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(28.dp)
                ) {
                    // Stats Section
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        WorkerStatCard(
                            modifier = Modifier.weight(1f),
                            label = "User Rating",
                            value = if (w.rating > 0) String.format("%.1f", w.rating) else "NEW",
                            icon = Icons.Outlined.Star,
                            color = KaushalyaColors.Warning
                        )
                        WorkerStatCard(
                            modifier = Modifier.weight(1f),
                            label = strings.myHires,
                            value = "${w.jobsCompleted}",
                            icon = Icons.Outlined.DoneAll,
                            color = KaushalyaColors.Success
                        )
                    }

                    // Bio
                    SectionHeader(title = "Professional Bio")
                    Text(
                        text = w.bio?.takeIf { it.isNotBlank() } ?: strings.bioFallback,
                        style = MaterialTheme.typography.bodyLarge,
                        color = KaushalyaColors.TextSecondary,
                        lineHeight = 24.sp
                    )

                    // Services
                    if (services.isNotEmpty()) {
                        SectionHeader(title = "Services & Pricing")
                        services.forEach { s ->
                            ServiceCardModern(s)
                        }
                    }

                    // Portfolio
                    if (portfolio.isNotEmpty()) {
                        SectionHeader(title = "Featured Work")
                        Row(
                            Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            portfolio.forEach { item ->
                                AsyncImage(
                                    model = item.imageUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(240.dp, 160.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .border(BorderStroke(1.dp, KaushalyaColors.Border), RoundedCornerShape(16.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                    }

                    // Reviews
                    if (reviews.isNotEmpty()) {
                        SectionHeader(title = "Client Testimonials")
                        reviews.forEach { r ->
                            WorkerReviewCard(r)
                        }
                    }

                    Spacer(Modifier.height(60.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = KaushalyaColors.TextPrimary
    )
}

@Composable
private fun WorkerStatCard(modifier: Modifier, label: String, value: String, icon: ImageVector, color: Color) {
    GlassCard(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = KaushalyaColors.TextPrimary)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = KaushalyaColors.TextMuted)
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
                Text(text = s.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = KaushalyaColors.TextPrimary)
                if (s.description.isNotBlank()) {
                    Text(text = s.description, style = MaterialTheme.typography.bodySmall, color = KaushalyaColors.TextMuted)
                }
            }
            Text(
                text = "₹${s.price.toInt()}",
                style = MaterialTheme.typography.headlineLarge,
                color = KaushalyaColors.Primary,
                fontWeight = FontWeight.Bold
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
                                tint = if (idx < r.rating) KaushalyaColors.Warning else KaushalyaColors.Border,
                                modifier = Modifier.size(14.dp)
                            )
                        }
            }
            if (r.comment.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(text = r.comment, style = MaterialTheme.typography.bodyMedium, color = KaushalyaColors.TextPrimary.copy(alpha = 0.9f))
            }
        }
    }
}
