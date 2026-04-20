package com.smartcampusassist.jpui.assignments

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.view.ViewGroup
import android.webkit.URLUtil
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.util.zip.ZipFile
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

@Composable
fun AssignmentViewerScreen(
    navController: NavController,
    fileUrl: String,
    title: String
) {
    val context = LocalContext.current
    val resolvedTitle = title.ifBlank { "Assignment File" }
    val previewSpec = remember(fileUrl, resolvedTitle) { buildPreviewSpec(fileUrl, resolvedTitle) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 2.dp, vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.size(30.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = resolvedTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 0.dp, end = 4.dp)
            )
        }

        when (previewSpec.type) {
            PreviewType.PDF -> {
                PdfAssignmentViewer(
                    context = context,
                    url = fileUrl,
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth()
                )
            }

            PreviewType.DOCX -> {
                DocxAssignmentViewer(
                    context = context,
                    url = fileUrl,
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth()
                )
            }

            PreviewType.IMAGE,
            PreviewType.DOCUMENT,
            PreviewType.DOC -> {
                WebAssignmentViewer(
                    previewSpec = previewSpec,
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth()
                )
            }
        }

        Button(
            onClick = {
                downloadFile(
                    context = context,
                    url = fileUrl,
                    title = resolvedTitle
                )
            },
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .height(38.dp)
                .padding(top = 4.dp),
            shape = RoundedCornerShape(999.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 18.dp, vertical = 0.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        ) {
            Text("Download")
        }
    }
}

@Composable
private fun DocxAssignmentViewer(
    context: Context,
    url: String,
    modifier: Modifier
) {
    var paragraphs by remember(url) { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember(url) { mutableStateOf(true) }
    var errorMessage by remember(url) { mutableStateOf<String?>(null) }

    LaunchedEffect(url) {
        isLoading = true
        errorMessage = null
        try {
            val file = downloadFileToCache(context, url, "docx")
            paragraphs = parseDocxParagraphs(file)
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Unable to open DOCX."
        } finally {
            isLoading = false
        }
    }

    Box(modifier = modifier) {
        when {
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(errorMessage ?: "Unable to open DOCX.")
                }
            }

            paragraphs.isNotEmpty() -> {
                val zoomState = rememberZoomState()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                        .zoomableContent(zoomState)
                ) {
                    itemsIndexed(
                        items = paragraphs,
                        key = { index, _ -> "$url-$index" },
                        contentType = { _, _ -> "docx_paragraph" }
                    ) { _, paragraph ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White
                        ) {
                            Text(
                                text = paragraph,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }

            !isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No readable DOCX content found.")
                }
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
@SuppressLint("SetJavaScriptEnabled")
private fun WebAssignmentViewer(
    previewSpec: PreviewSpec,
    modifier: Modifier
) {
    val context = LocalContext.current
    var isLoading by remember(previewSpec.url) { mutableStateOf(true) }
    var zoomedIn by remember(previewSpec.url) { mutableStateOf(false) }
    val webView = remember { WebView(context) }

    DisposableEffect(webView) {
        onDispose {
            webView.destroy()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = {
                webView.apply {
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                        }
                    }
                    webChromeClient = WebChromeClient()
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.useWideViewPort = true
                    settings.loadWithOverviewMode = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false
                    settings.allowContentAccess = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    loadPreview(previewSpec)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .doubleTapWebZoom(
                    webView = webView,
                    zoomedIn = zoomedIn,
                    onZoomChange = { zoomedIn = it }
                ),
            update = {
                if (it.url != previewSpec.url) {
                    isLoading = true
                    zoomedIn = false
                    it.loadPreview(previewSpec)
                }
            }
        )

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun PdfAssignmentViewer(
    context: Context,
    url: String,
    modifier: Modifier
) {
    var pdfFile by remember(url) { mutableStateOf<File?>(null) }
    var pageCount by remember(url) { mutableIntStateOf(0) }
    var isLoading by remember(url) { mutableStateOf(true) }
    var errorMessage by remember(url) { mutableStateOf<String?>(null) }
    var pdfScale by remember(url) { mutableFloatStateOf(1f) }
    val listState = rememberLazyListState()
    val horizontalScrollState = rememberScrollState()
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val currentPage by remember(listState) {
        derivedStateOf { (listState.firstVisibleItemIndex + 1).coerceAtLeast(1) }
    }
    val pdfZoomState = rememberTransformableState { zoomChange, _, _ ->
        pdfScale = (pdfScale * zoomChange).coerceIn(1f, 4f)
    }

    LaunchedEffect(url) {
        isLoading = true
        errorMessage = null
        try {
            val file = downloadPdfToCache(context, url)
            pdfFile = file
            pageCount = readPdfPageCount(file)
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Unable to open PDF."
        } finally {
            isLoading = false
        }
    }

    Box(modifier = modifier) {
        when {
            errorMessage != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(errorMessage ?: "Unable to open PDF.")
                }
            }

            pdfFile != null && pageCount > 0 -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF3F4F6))
                        .horizontalScroll(horizontalScrollState)
                        .transformable(state = pdfZoomState)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .requiredWidth((screenWidth * pdfScale) - 22.dp)
                            .fillMaxSize()
                            .padding(end = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        items(
                            items = List(pageCount) { it },
                            key = { index -> "$url-$index" },
                            contentType = { "pdf_page" }
                        ) { index ->
                            val currentPdfFile = pdfFile ?: return@items
                            PdfPageCard(
                                file = currentPdfFile,
                                pageIndex = index
                            )
                        }
                    }
                }
            }
        }

        if (pageCount > 0) {
            val progress = if (pageCount <= 1) 0f else (currentPage - 1).toFloat() / (pageCount - 1).toFloat()
            val railHeight = when {
                pageCount <= 3 -> 72.dp
                pageCount <= 6 -> 92.dp
                pageCount <= 12 -> 112.dp
                pageCount <= 24 -> 132.dp
                else -> 152.dp
            }
            val thumbHeight = when {
                pageCount <= 3 -> 22.dp
                pageCount <= 8 -> 18.dp
                pageCount <= 20 -> 14.dp
                else -> 10.dp
            }
            Surface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp),
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            ) {
                Column(
                    modifier = Modifier
                        .width(24.dp)
                        .padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = currentPage.toString(),
                        style = MaterialTheme.typography.labelSmall
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .size(width = 4.dp, height = railHeight)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(999.dp)
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = ((railHeight.value - thumbHeight.value) * progress).dp)
                                .fillMaxWidth()
                                .height(thumbHeight)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = RoundedCornerShape(999.dp)
                                )
                        )
                    }
                }
            }
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun PdfPageCard(
    file: File,
    pageIndex: Int
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, file, pageIndex) {
        value = renderPdfPage(file, pageIndex)
    }

    DisposableEffect(bitmap) {
        onDispose {
            bitmap?.takeIf { !it.isRecycled }?.recycle()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp)
    ) {
        val pageBitmap = bitmap
        if (pageBitmap == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(Color.White, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Image(
                bitmap = pageBitmap.asImageBitmap(),
                contentDescription = "PDF page ${pageIndex + 1}",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(8.dp)),
                contentScale = ContentScale.FillWidth
            )
        }
    }
}

@Composable
private fun rememberZoomState(): ZoomState {
    var scale by remember { mutableFloatStateOf(1f) }
    return ZoomState(scale = scale) { newScale ->
        scale = newScale
    }
}

private data class ZoomState(
    val scale: Float,
    val update: (Float) -> Unit
)

@Composable
private fun Modifier.zoomableContent(zoomState: ZoomState): Modifier {
    return this
        .graphicsLayer {
            scaleX = zoomState.scale
            scaleY = zoomState.scale
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onDoubleTap = {
                    val newScale = if (zoomState.scale > 1f) 1f else 2f
                    zoomState.update(newScale)
                }
            )
        }
}

@Composable
private fun Modifier.doubleTapWebZoom(
    webView: WebView,
    zoomedIn: Boolean,
    onZoomChange: (Boolean) -> Unit
): Modifier {
    return this.pointerInput(webView) {
        detectTapGestures(
            onDoubleTap = {
                val nextScale = if (zoomedIn) 1f else 2f
                webView.post {
                    webView.setInitialScale((nextScale * 100).toInt())
                    if (nextScale > 1f) {
                        webView.zoomIn()
                    } else {
                        webView.zoomOut()
                        webView.zoomOut()
                    }
                }
                onZoomChange(!zoomedIn)
            }
        )
    }
}

private data class PreviewSpec(
    val url: String,
    val type: PreviewType
)

private enum class PreviewType {
    IMAGE,
    DOCUMENT,
    PDF,
    DOCX,
    DOC
}

private fun buildPreviewSpec(url: String, title: String): PreviewSpec {
    val cleanUrl = url.substringBefore("#").substringBefore("?")
    val normalizedTitle = title.substringBefore("?").lowercase()
    val normalizedUrl = cleanUrl.lowercase()

    val isPdf = normalizedTitle.endsWith(".pdf") || normalizedUrl.endsWith(".pdf")
    val isDocx = normalizedTitle.endsWith(".docx") || normalizedUrl.endsWith(".docx")
    val isDoc = normalizedTitle.endsWith(".doc") || normalizedUrl.endsWith(".doc")
    val isImage = normalizedTitle.endsWith(".png") ||
        normalizedTitle.endsWith(".jpg") ||
        normalizedTitle.endsWith(".jpeg") ||
        normalizedTitle.endsWith(".webp") ||
        normalizedUrl.endsWith(".png") ||
        normalizedUrl.endsWith(".jpg") ||
        normalizedUrl.endsWith(".jpeg") ||
        normalizedUrl.endsWith(".webp")

    return when {
        isPdf -> PreviewSpec(url = url, type = PreviewType.PDF)
        isDocx -> PreviewSpec(url = url, type = PreviewType.DOCX)
        isDoc -> PreviewSpec(url = url, type = PreviewType.DOC)
        isImage -> PreviewSpec(url = url, type = PreviewType.IMAGE)
        else -> PreviewSpec(
            url = "https://docs.google.com/gview?embedded=1&url=${Uri.encode(url)}",
            type = PreviewType.DOCUMENT
        )
    }
}

private fun WebView.loadPreview(spec: PreviewSpec) {
    when (spec.type) {
        PreviewType.IMAGE -> {
            val safeUrl = spec.url.replace("'", "%27")
            loadDataWithBaseURL(
                null,
                """
                <html>
                  <body style="margin:0;background:#ffffff;display:flex;align-items:center;justify-content:center;">
                    <img src='$safeUrl' style="max-width:100%;height:auto;" />
                  </body>
                </html>
                """.trimIndent(),
                "text/html",
                "UTF-8",
                null
            )
        }

        PreviewType.DOCUMENT -> loadUrl(spec.url)
        PreviewType.DOC -> loadUrl(spec.url)
        PreviewType.DOCX -> Unit
        PreviewType.PDF -> Unit
    }
}

private suspend fun downloadPdfToCache(context: Context, url: String): File = withContext(Dispatchers.IO) {
    val fileName = "pdf_${url.hashCode()}_${URLUtil.guessFileName(url, null, "application/pdf")}"
    val target = File(context.cacheDir, fileName.ifBlank { "assignment.pdf" })
    if (target.exists() && target.length() > 0L) {
        return@withContext target
    }
    URL(url).openStream().use { input ->
        target.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    target
}

private suspend fun downloadFileToCache(
    context: Context,
    url: String,
    defaultExtension: String
): File = withContext(Dispatchers.IO) {
    val fileName = "doc_${url.hashCode()}_${URLUtil.guessFileName(url, null, null).ifBlank { "assignment.$defaultExtension" }}"
    val target = File(context.cacheDir, fileName)
    if (target.exists() && target.length() > 0L) {
        return@withContext target
    }
    URL(url).openStream().use { input ->
        target.outputStream().use { output ->
            input.copyTo(output)
        }
    }
    target
}

private suspend fun parseDocxParagraphs(file: File): List<String> = withContext(Dispatchers.IO) {
    val zipFile = ZipFile(file)
    try {
        val entry = zipFile.getEntry("word/document.xml") ?: return@withContext emptyList()
        zipFile.getInputStream(entry).use { input ->
            val parser = XmlPullParserFactory.newInstance().newPullParser()
            parser.setInput(input, "UTF-8")

            val paragraphs = mutableListOf<String>()
            val current = StringBuilder()
            var event = parser.eventType

            while (event != XmlPullParser.END_DOCUMENT) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "tab") {
                            current.append("    ")
                        }
                    }

                    XmlPullParser.TEXT -> {
                        current.append(parser.text.orEmpty())
                    }

                    XmlPullParser.END_TAG -> {
                        if (parser.name == "p") {
                            val text = current.toString().trim()
                            if (text.isNotBlank()) {
                                paragraphs.add(text)
                            }
                            current.clear()
                        }
                    }
                }
                event = parser.next()
            }

            paragraphs
        }
    } finally {
        zipFile.close()
    }
}

private suspend fun readPdfPageCount(file: File): Int = withContext(Dispatchers.IO) {
    val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    val renderer = PdfRenderer(descriptor)

    try {
        renderer.pageCount
    } finally {
        renderer.close()
        descriptor.close()
    }
}

private suspend fun renderPdfPage(file: File, index: Int): Bitmap = withContext(Dispatchers.IO) {
    val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    val renderer = PdfRenderer(descriptor)

    try {
        renderer.openPage(index).use { page ->
            val scale = 1.1f
            val width = (page.width * scale).toInt().coerceAtLeast(1)
            val height = (page.height * scale).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        }
    } finally {
        renderer.close()
        descriptor.close()
    }
}

private fun downloadFile(
    context: Context,
    url: String,
    title: String
) {
    runCatching {
        val safeName = if (URLUtil.guessFileName(url, null, null).isNotBlank()) {
            URLUtil.guessFileName(url, null, null)
        } else {
            title.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        }

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle(title)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                safeName
            )

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
    }.onSuccess {
        Toast.makeText(context, "Download started", Toast.LENGTH_SHORT).show()
    }.onFailure { throwable ->
        Toast.makeText(
            context,
            throwable.localizedMessage ?: "Unable to start download",
            Toast.LENGTH_SHORT
        ).show()
    }
}
