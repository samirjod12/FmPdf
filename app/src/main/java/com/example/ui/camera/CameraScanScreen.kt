package com.example.ui.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.ui.theme.FmPureWhite
import com.example.ui.theme.FmRed
import com.example.ui.theme.FmSlateDark
import java.io.File
import java.util.concurrent.Executors

@Composable
fun CameraScanScreen(
    currentPagesCount: Int,
    lastCapturedBitmap: Bitmap?,
    onPageCaptured: (Bitmap) -> Unit,
    onImportUris: (List<Uri>) -> Unit,
    onDoneScanning: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportUris(uris)
        }
    }

    var isFlashOn by remember { mutableStateOf(false) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var camera: Camera? by remember { mutableStateOf(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var shutterFlash by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // Camera Preview
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val capture = ImageCapture.Builder()
                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                                .build()

                            imageCapture = capture

                            val cameraSelector = CameraSelector.Builder()
                                .requireLensFacing(lensFacing)
                                .build()

                            cameraProvider.unbindAll()
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                capture
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Document Scanner Viewfinder Overlay (Grid and Corner brackets)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val padH = 32.dp.toPx()
                val padV = 110.dp.toPx()
                val docWidth = size.width - (padH * 2)
                val docHeight = size.height - (padV * 2)

                // Darken outside doc area slightly
                drawRoundRect(
                    color = Color(0x33000000),
                    topLeft = Offset(0f, 0f),
                    size = size
                )

                // Cutout document frame with red/white corners
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.25f),
                    topLeft = Offset(padH, padV),
                    size = Size(docWidth, docHeight),
                    cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Corner brackets
                val bracketLen = 28.dp.toPx()
                val bracketStroke = 4.dp.toPx()
                val cornerColor = Color(0xFFE53935)

                // Top-left
                drawLine(cornerColor, Offset(padH, padV), Offset(padH + bracketLen, padV), bracketStroke)
                drawLine(cornerColor, Offset(padH, padV), Offset(padH, padV + bracketLen), bracketStroke)

                // Top-right
                drawLine(cornerColor, Offset(padH + docWidth, padV), Offset(padH + docWidth - bracketLen, padV), bracketStroke)
                drawLine(cornerColor, Offset(padH + docWidth, padV), Offset(padH + docWidth, padV + bracketLen), bracketStroke)

                // Bottom-left
                drawLine(cornerColor, Offset(padH, padV + docHeight), Offset(padH + bracketLen, padV + docHeight), bracketStroke)
                drawLine(cornerColor, Offset(padH, padV + docHeight), Offset(padH, padV + docHeight - bracketLen), bracketStroke)

                // Bottom-right
                drawLine(cornerColor, Offset(padH + docWidth, padV + docHeight), Offset(padH + docWidth - bracketLen, padV + docHeight), bracketStroke)
                drawLine(cornerColor, Offset(padH + docWidth, padV + docHeight), Offset(padH + docWidth, padV + docHeight - bracketLen), bracketStroke)
            }
        } else {
            // Permission Request State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.PhotoCamera,
                    contentDescription = null,
                    tint = FmRed,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Permission Required",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "FmPDF uses your camera to scan documents and directly add them as PDF pages.",
                    fontSize = 14.sp,
                    color = Color(0xFFCBD5E1),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = FmRed)
                ) {
                    Text("Grant Permission", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Shutter white flash animation
        AnimatedVisibility(
            visible = shutterFlash,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.White))
        }

        // Top Control Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 44.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Surface(
                color = Color(0x99000000),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(FmRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Document Scanner",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            IconButton(
                onClick = {
                    val nextFlash = !isFlashOn
                    isFlashOn = nextFlash
                    camera?.cameraControl?.enableTorch(nextFlash)
                },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
            ) {
                Icon(
                    imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                    contentDescription = "Flashlight",
                    tint = if (isFlashOn) Color.Yellow else Color.White
                )
            }
        }

        // Bottom Capture & Action Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color(0xCC0F172A))
                .padding(bottom = 36.dp, top = 20.dp, start = 24.dp, end = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Import from Gallery
                IconButton(
                    onClick = {
                        galleryPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF))
                ) {
                    Icon(
                        Icons.Default.Collections,
                        contentDescription = "Import Photos",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }

                // Shutter Capture Button
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(if (isCapturing) Color.Gray else FmRed)
                        .clickable(enabled = !isCapturing && hasCameraPermission) {
                            val capture = imageCapture ?: return@clickable
                            isCapturing = true
                            shutterFlash = true

                            capture.takePicture(
                                cameraExecutor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    override fun onCaptureSuccess(image: ImageProxy) {
                                        val rotation = image.imageInfo.rotationDegrees
                                        val buffer = image.planes[0].buffer
                                        val bytes = ByteArray(buffer.remaining())
                                        buffer.get(bytes)
                                        val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                                        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                                        val orientedBitmap = Bitmap.createBitmap(
                                            rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
                                        )

                                        image.close()

                                        ContextCompat.getMainExecutor(context).execute {
                                            shutterFlash = false
                                            isCapturing = false
                                            onPageCaptured(orientedBitmap)
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        exception.printStackTrace()
                                        ContextCompat.getMainExecutor(context).execute {
                                            shutterFlash = false
                                            isCapturing = false
                                        }
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp))
                    } else {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = "Capture Document Page",
                            tint = Color.White,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                // Finish / Pages Counter & Next
                if (currentPagesCount > 0) {
                    Surface(
                        onClick = onDoneScanning,
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF10B981),
                        modifier = Modifier
                            .height(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$currentPagesCount Pages",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Done →",
                                    fontSize = 11.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                } else {
                    // Camera flip lens
                    IconButton(
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                    ) {
                        Icon(
                            Icons.Default.FlipCameraAndroid,
                            contentDescription = "Flip Camera",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Hold steady • Snap pages continuously",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
        }
    }
}
