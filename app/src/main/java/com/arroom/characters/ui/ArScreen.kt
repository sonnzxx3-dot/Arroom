package com.arroom.characters.ui

import android.net.Uri
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.arroom.characters.R
import com.arroom.characters.ar.CharacterNode
import com.arroom.characters.ar.RenderQuality
import com.arroom.characters.ar.ThermalLevel
import com.arroom.characters.ar.characterLimit
import com.arroom.characters.ar.rememberThermalLevel
import com.arroom.characters.data.AppPrefs
import com.arroom.characters.data.BuiltInCatalog
import com.arroom.characters.data.CharacterItem
import com.arroom.characters.data.ImportResult
import com.arroom.characters.data.CatalogRepository
import com.arroom.characters.data.Limits
import com.arroom.characters.data.ModelDownloader
import com.arroom.characters.data.ModelSource
import com.arroom.characters.data.ModelStore
import com.arroom.characters.data.ThumbnailStore
import com.arroom.characters.record.RecEvent
import com.arroom.characters.record.rememberScreenRecorder
import com.arroom.characters.util.captureArView
import com.arroom.characters.util.shareMedia
import com.arroom.characters.util.saveToGallery
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Plane
import com.google.ar.core.TrackingState
import com.google.ar.core.TrackingFailureReason
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.arcore.isValid
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberCollisionSystem
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import io.github.sceneview.rememberNodes
import io.github.sceneview.rememberOnGestureListener
import io.github.sceneview.rememberView
import kotlinx.coroutines.launch

@Composable
fun ArScreen() {
    val context = LocalContext.current
    val hostView = LocalView.current
    val scope = rememberCoroutineScope()
    val modelStore = remember { ModelStore(context) }
    val prefs = remember { AppPrefs(context) }
    val downloader = remember { ModelDownloader(context) }
    val thumbnails = remember { ThumbnailStore(context) }
    val thermalLevel by rememberThermalLevel()
    val catalogRepo = remember { CatalogRepository(context) }

    val engine = rememberEngine()
    val modelLoader = rememberModelLoader(engine)
    val materialLoader = rememberMaterialLoader(engine)
    val cameraNode = rememberARCameraNode(engine)
    val view = rememberView(engine)
    val collisionSystem = rememberCollisionSystem(view)
    val childNodes = rememberNodes()

    var arSceneView by remember { mutableStateOf<ARSceneView?>(null) }
    var frame by remember { mutableStateOf<Frame?>(null) }
    var trackingFailure by remember { mutableStateOf<TrackingFailureReason?>(null) }
    var planesFound by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showPlanes by remember { mutableStateOf(true) }
    var canPlace by remember { mutableStateOf(false) }
    var hitDistance by remember { mutableStateOf<Float?>(null) }
    var sessionError by remember { mutableStateOf<String?>(null) }
    var sessionKey by remember { mutableIntStateOf(0) }
    var uiVisible by remember { mutableStateOf(true) }
    var pendingShare by remember { mutableStateOf<Pair<Uri, String>?>(null) }
    var modelToDelete by remember { mutableStateOf<CharacterItem?>(null) }
    var showCoach by remember { mutableStateOf(false) }
    var loadProgress by remember { mutableStateOf<Float?>(null) }
    var thumbVersion by remember { mutableIntStateOf(0) }
    var thermalWarned by remember { mutableStateOf(false) }

    var catalog by remember {
        mutableStateOf(BuiltInCatalog.items(context) + modelStore.loadImported())
    }
    var selectedItem by remember { mutableStateOf(catalog.first()) }
    var placedCount by remember { mutableIntStateOf(0) }
    var activeCharacter by remember { mutableStateOf<CharacterNode?>(null) }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    fun toast(@StringRes res: Int) = toast(context.getString(res))
    fun haptic() = hostView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)

    LaunchedEffect(Unit) {
        val remote = catalogRepo.load()
        val imported = modelStore.loadImported()
        val merged = (remote + imported).distinctBy { it.id }
        if (merged.isNotEmpty()) {
            catalog = merged
            if (merged.none { it.id == selectedItem.id }) selectedItem = merged.first()
        }
    }

    val thumbnailPaths = remember(catalog, thumbVersion) {
        catalog.associate { it.id to thumbnails.pathFor(it.id) }
    }

    val recorder = rememberScreenRecorder { event ->
        when (event) {
            is RecEvent.Saved -> {
                toast(R.string.toast_video_saved)
                pendingShare = Uri.parse(event.uri) to "video/mp4"
            }
            is RecEvent.Failed -> toast(event.reason)
        }
    }

    LaunchedEffect(thermalLevel) {
        arSceneView?.let { RenderQuality.apply(it.view, highEnd = thermalLevel == ThermalLevel.NORMAL) }
        if (thermalLevel != ThermalLevel.NORMAL && !thermalWarned) {
            thermalWarned = true
            toast(R.string.thermal_throttled)
        }
    }

    LaunchedEffect(recorder.isRecording) {
        uiVisible = !recorder.isRecording
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        scope.launch {
            isLoading = true
            val result = modelStore.import(uri)
            isLoading = false
            when (result) {
                is ImportResult.Success -> {
                    catalog = catalog + result.item
                    selectedItem = result.item
                    toast(context.getString(R.string.toast_import_ok, result.item.title))
                }
                is ImportResult.Failure -> toast(
                    result.arg?.let { context.getString(result.messageRes, it) }
                        ?: context.getString(result.messageRes)
                )
            }
        }
    }

    fun placeAt(x: Float, y: Float) {
        val currentFrame = frame ?: return
        if (isLoading) return
        val limit = thermalLevel.characterLimit(Limits.MAX_CHARACTERS)
        if (placedCount >= limit) {
            toast(context.getString(R.string.toast_limit, limit))
            return
        }

        fun hitAt(px: Float, py: Float) = currentFrame
            .hitTest(px, py)
            .firstOrNull { it.isValid(depthPoint = false, point = false) }

        val v = arSceneView
        val anchor = (hitAt(x, y)
            ?: v?.let { hitAt(it.width / 2f, it.height / 2f) })
            ?.createAnchorOrNull() ?: run {
            toast(R.string.toast_no_surface)
            return
        }

        val item = selectedItem
        scope.launch {
            isLoading = true

            val location = when (val source = item.source) {
                is ModelSource.Remote -> {
                    if (!downloader.isCached(source.url)) loadProgress = 0f
                    downloader.ensureLocal(source.url) { p -> loadProgress = p }
                        ?.let { "file://$it" }
                }
                else -> source.location
            }

            val instance = location?.let {
                runCatching { modelLoader.loadModelInstance(it) }.getOrNull()
            }
            isLoading = false
            loadProgress = null

            if (instance == null) {
                anchor.detach()
                toast(R.string.toast_model_failed)
                return@launch
            }

            val anchorNode = AnchorNode(engine, anchor).apply {
                isEditable = true
                isPositionEditable = true
            }
            val character = CharacterNode.create(
                engine = engine,
                instance = instance,
                characterId = item.id,
                scaleToUnits = item.scaleToUnits
            )
            anchorNode.addChildNode(character)
            childNodes += anchorNode

            haptic()
            character.animateAppearance(scope) { character.startIdleFloat(scope) }
            activeCharacter = character
            placedCount = childNodes.size
            showPlanes = false

            if (!thumbnails.has(item.id)) {
                scope.launch {
                    kotlinx.coroutines.delay(1100)
                    arSceneView?.let { v ->
                        captureArView(v)?.let { bitmap ->
                            if (thumbnails.save(item.id, bitmap, x, y)) thumbVersion++
                            bitmap.recycle()
                        }
                    }
                }
            }

            if (!prefs.gestureCoachShown) {
                kotlinx.coroutines.delay(900)
                showCoach = true
            }
        }
    }

    fun removeNode(node: Node) {
        scope.launch {
            val character = node.childNodes.firstOrNull() as? CharacterNode
            character?.animateDisappearance()
            childNodes.remove(node)
            character?.dispose()
            (node as? AnchorNode)?.destroy()
            placedCount = childNodes.size
            if (placedCount == 0) {
                activeCharacter = null
                showPlanes = true
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            childNodes.forEach { node ->
                (node.childNodes.firstOrNull() as? CharacterNode)?.dispose()
                (node as? AnchorNode)?.destroy()
            }
            childNodes.clear()
        }
    }

    Box(Modifier.fillMaxSize()) {

        key(sessionKey) {
            ARScene(
                modifier = Modifier.fillMaxSize(),
                childNodes = childNodes,
                engine = engine,
                view = view,
                modelLoader = modelLoader,
                materialLoader = materialLoader,
                collisionSystem = collisionSystem,
                cameraNode = cameraNode,
                planeRenderer = showPlanes,
                sessionConfiguration = { session, config ->
                    config.depthMode =
                        if (session.isDepthModeSupported(Config.DepthMode.AUTOMATIC))
                            Config.DepthMode.AUTOMATIC
                        else Config.DepthMode.DISABLED

                    config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                    config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    config.instantPlacementMode = Config.InstantPlacementMode.DISABLED
                },
                onViewCreated = {
                    arSceneView = this
                    RenderQuality.apply(view, highEnd = thermalLevel == ThermalLevel.NORMAL)
                },
                onSessionFailed = { e ->
                    sessionError = e.message ?: context.getString(R.string.session_error_generic)
                },
                onSessionUpdated = { _, updatedFrame ->
                    frame = updatedFrame
                    if (!planesFound) {
                        planesFound = updatedFrame
                            .getUpdatedTrackables(Plane::class.java)
                            .any { it.trackingState == TrackingState.TRACKING }
                    }

                    val v = arSceneView
                    if (v != null && v.width > 0 && v.height > 0) {
                        val hit = updatedFrame
                            .hitTest(v.width / 2f, v.height / 2f)
                            .firstOrNull { it.isValid(depthPoint = false, point = false) }
                        val nowCanPlace = hit != null
                        if (nowCanPlace != canPlace) canPlace = nowCanPlace
                        val d = hit?.distance
                        if (d != null && (hitDistance == null ||
                                kotlin.math.abs(d - hitDistance!!) > 0.1f)
                        ) hitDistance = d
                        if (d == null && hitDistance != null) hitDistance = null
                    }
                },
                onTrackingFailureChanged = { trackingFailure = it },
                onGestureListener = rememberOnGestureListener(
                    onSingleTapConfirmed = { motionEvent: MotionEvent, node: Node? ->
                        val tapped = node?.let { findCharacter(it) }
                        if (tapped != null) {
                            activeCharacter?.setSelected(false)
                            tapped.setSelected(true)
                            tapped.pulse(scope)
                            activeCharacter = tapped
                            haptic()
                        } else {
                            placeAt(motionEvent.x, motionEvent.y)
                        }
                    },
                    onLongPress = { _: MotionEvent, node: Node? ->
                        val target = node?.let { findCharacter(it) }
                            ?: return@rememberOnGestureListener
                        val anchorNode = childNodes.firstOrNull { it.childNodes.contains(target) }
                        if (anchorNode != null) {
                            haptic()
                            removeNode(anchorNode)
                        }
                    }
                )
            )
        }

        AnimatedVisibility(
            visible = uiVisible && !isLoading &&
                placedCount < thermalLevel.characterLimit(Limits.MAX_CHARACTERS),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            PlacementReticle(ready = canPlace, distanceMeters = hitDistance)
        }

        AnimatedVisibility(
            visible = uiVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 12.dp, start = 16.dp, end = 16.dp)
        ) {
            StatusHint(
                trackingFailure = trackingFailure,
                planesFound = planesFound,
                hasCharacters = placedCount > 0
            )
        }

        AnimatedVisibility(
            visible = uiVisible && recorder.isRecording,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 56.dp)
        ) {
            RecordingBadge()
        }

        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            LoadingBubble(progress = loadProgress)
        }

        AnimatedVisibility(
            visible = uiVisible,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) {
            ControlPanel(
                catalog = catalog,
                selected = selectedItem,
                onSelect = { selectedItem = it },
                onLongPressImported = { modelToDelete = it },
                thumbnailFor = { item -> thumbnailPaths[item.id] },
                onImportClick = {
                    importLauncher.launch(
                        arrayOf(
                            "model/gltf-binary",
                            "model/gltf+json",
                            "application/octet-stream",
                            "*/*"
                        )
                    )
                },
                activeCharacter = activeCharacter,
                onSwitchAnimation = { index -> activeCharacter?.playAnimation(index) },
                canUndo = placedCount > 0,
                onUndo = { childNodes.lastOrNull()?.let { removeNode(it) } },
                onClearAll = {
                    childNodes.toList().forEach { removeNode(it) }
                },
                isRecording = recorder.isRecording,
                onToggleRecord = recorder.toggle,
                onCapture = {
                    val v = arSceneView ?: return@ControlPanel
                    scope.launch {
                        val bmp = captureArView(v)
                        if (bmp == null) {
                            toast(R.string.toast_photo_failed)
                        } else {
                            haptic()
                            val uri = saveToGallery(context, bmp)
                            toast(
                                if (uri != null) R.string.toast_photo_saved
                                else R.string.toast_save_error
                            )
                            if (uri != null) pendingShare = uri to "image/jpeg"
                        }
                    }
                },
            )
        }

        AnimatedVisibility(
            visible = !uiVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
        ) {
            ShowUiButton(onClick = { uiVisible = true })
        }

        AnimatedVisibility(
            visible = uiVisible && pendingShare != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 210.dp)
        ) {
            SharePrompt(onShare = {
                pendingShare?.let { (uri, mime) -> shareMedia(context, uri, mime) }
                pendingShare = null
            })
        }

        LaunchedEffect(pendingShare) {
            if (pendingShare != null) {
                kotlinx.coroutines.delay(7000)
                pendingShare = null
            }
        }

        modelToDelete?.let { item ->
            AlertDialog(
                onDismissRequest = { modelToDelete = null },
                title = { Text(stringResource(R.string.delete_model_title)) },
                text = { Text(stringResource(R.string.delete_model_body, item.title)) },
                confirmButton = {
                    TextButton(onClick = {
                        modelStore.remove(item)
                        thumbnails.remove(item.id)
                        catalog = catalog.filterNot { it.id == item.id }
                        if (selectedItem.id == item.id) {
                            selectedItem = catalog.firstOrNull() ?: selectedItem
                        }
                        modelToDelete = null
                    }) { Text(stringResource(R.string.action_delete)) }
                },
                dismissButton = {
                    TextButton(onClick = { modelToDelete = null }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        if (showCoach) {
            GestureCoach(onDismiss = {
                showCoach = false
                prefs.gestureCoachShown = true
            })
        }

        sessionError?.let { message ->
            SessionErrorOverlay(
                message = message,
                onRetry = {
                    sessionError = null
                    sessionKey++
                }
            )
        }
    }
}

private fun findCharacter(node: Node): CharacterNode? {
    var current: Node? = node
    while (current != null) {
        if (current is CharacterNode) return current
        current = current.parent
    }
    return null
}
