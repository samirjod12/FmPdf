package com.example.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.FilterType
import com.example.model.PdfPageSize
import com.example.model.PdfQuality
import com.example.ui.theme.FmBackground
import com.example.ui.theme.FmBorder
import com.example.ui.theme.FmPureWhite
import com.example.ui.theme.FmRed
import com.example.ui.theme.FmRedLight
import com.example.ui.theme.FmSlate
import com.example.ui.theme.FmSlateDark
import com.example.ui.theme.FmSlateMuted
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenEasterEgg: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var defaultPageSize by remember { mutableStateOf(PdfPageSize.A4) }
    var defaultQuality by remember { mutableStateOf(PdfQuality.HIGH) }
    var defaultFilter by remember { mutableStateOf(FilterType.MAGIC_COLOR) }

    // Pulsing animation for the Version badge to invite clicking for Android 11 Easter egg
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings & About",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = FmSlateDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = FmSlateDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FmPureWhite)
            )
        },
        containerColor = FmBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Branding Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = FmPureWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, FmBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.fmpdf_logo),
                        contentDescription = "FmPDF Logo",
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, FmBorder, RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "FmPDF",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = FmSlateDark
                        )
                        Text(
                            text = "Premium PDF Maker & Scanner",
                            fontSize = 14.sp,
                            color = FmRed,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "High-fidelity documents with instant camera scan and smart filters.",
                            fontSize = 12.sp,
                            color = FmSlateMuted,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // PDF Export Defaults
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = FmPureWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, FmBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = FmRed)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "PDF Generation Preferences",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = FmSlateDark
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Default Page Format", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FmSlate)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PdfPageSize.entries.forEach { size ->
                            FilterChip(
                                selected = defaultPageSize == size,
                                onClick = {
                                    defaultPageSize = size
                                    onShowMessage("Default page size set to ${size.displayName}")
                                },
                                label = { Text(size.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FmRedLight,
                                    selectedLabelColor = FmRed
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("Compression Quality", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FmSlate)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PdfQuality.entries.forEach { quality ->
                            FilterChip(
                                selected = defaultQuality == quality,
                                onClick = {
                                    defaultQuality = quality
                                    onShowMessage("Default quality set to ${quality.displayName}")
                                },
                                label = { Text(quality.name) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FmRedLight,
                                    selectedLabelColor = FmRed
                                )
                            )
                        }
                    }
                }
            }

            // Storage & Maintenance
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = FmPureWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, FmBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = FmSlate)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Storage & Cache",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = FmSlateDark
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val cacheDir = context.cacheDir
                                cacheDir.deleteRecursively()
                                onShowMessage("Temporary cache cleared!")
                            }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Clear Scan Cache", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = FmSlateDark)
                            Text("Frees space from temporary camera frames", fontSize = 12.sp, color = FmSlateMuted)
                        }
                        Icon(Icons.Default.CleaningServices, contentDescription = null, tint = FmRed)
                    }
                }
            }

            // Features Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = FmPureWhite),
                border = androidx.compose.foundation.BorderStroke(1.dp, FmBorder)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = FmSlate)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Key Features",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = FmSlateDark
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val features = listOf(
                        "Direct Camera to PDF page capture with live viewfinder",
                        "CamScanner-grade Magic Color & B&W document filters",
                        "One-tap Download to device Downloads/FmPDF folder",
                        "Instant PDF Sharing via WhatsApp, Gmail, Drive",
                        "Built-in full-screen multi-page PDF Reader"
                    )

                    features.forEach { feat ->
                        Row(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text("• ", color = FmRed, fontWeight = FontWeight.Bold)
                            Text(feat, fontSize = 13.sp, color = FmSlate)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ==========================================
            // MANDATORY BOTTOM SECTION PER USER PROMPT:
            // "In setting bottom of it there shows Made By Samir Bin Aziz .
            // and Version 1.0 , when we click on it a animation open like android 11 ."
            // ==========================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = FmPureWhite),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFFEE2E2)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Creator info: Made By Samir Bin Aziz
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(FmRedLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "Heart",
                                tint = FmRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Made By Samir Bin Aziz",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = FmSlateDark
                            )
                            Text(
                                text = "Crafted with passion for FmPDF",
                                fontSize = 12.sp,
                                color = FmSlateMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    HorizontalDivider(color = FmBorder, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(18.dp))

                    // Version 1.0 with Android 11 Easter Egg trigger
                    Box(
                        modifier = Modifier
                            .scale(pulseScale)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                color = Color(0xFF0F172A)
                            )
                            .clickable {
                                onOpenEasterEgg()
                            }
                            .padding(horizontal = 22.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = "Tap",
                                tint = FmRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Version 1.0",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                                Text(
                                    text = "Tap to open Android 11 Easter Egg",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
