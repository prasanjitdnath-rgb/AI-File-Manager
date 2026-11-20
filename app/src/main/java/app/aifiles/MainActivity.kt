package app.aifiles

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {
    private val vm: VM by viewModels()
    private var ready by mutableStateOf(false)
    private var allFilesRequested = false

    private val mediaPerms = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        checkAccess()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val dark = androidx.compose.foundation.isSystemInDarkTheme()
            val ctx = LocalContext.current
            val scheme = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(ctx)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(ctx)
                dark -> androidx.compose.material3.darkColorScheme()
                else -> androidx.compose.material3.lightColorScheme()
            }
            MaterialTheme(colorScheme = scheme) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (ready) Root(vm) else PermissionGate(onGrant = ::requestAccess)
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        vm.lockVault()
    }

    override fun onResume() {
        super.onResume()
        checkAccess()
    }

    private fun hasMediaPermission(): Boolean {
        if (Build.VERSION.SDK_INT < 33) {
            return checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        return listOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
        ).all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    }

    /** All-files access is enough on its own; media permissions are the fallback (Amaze-style). */
    private fun checkAccess() {
        ready = Environment.isExternalStorageManager() || hasMediaPermission()
        if (ready) vm.refresh()
    }

    private fun requestAccess() {
        if (Environment.isExternalStorageManager()) {
            if (!hasMediaPermission()) launchMediaPermissions() else checkAccess()
            return
        }
        if (!allFilesRequested) {
            allFilesRequested = true
            runCatching {
                startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:$packageName"),
                    ),
                )
            }.onFailure {
                runCatching { startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
            }
            return
        }
        if (!hasMediaPermission()) launchMediaPermissions() else checkAccess()
    }

    private fun launchMediaPermissions() {
        val perms = if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO,
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        mediaPerms.launch(perms)
    }
}

@Composable
private fun PermissionGate(onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                modifier = Modifier.padding(20.dp).size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text("AI File Manager", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "A modern file manager and gallery with on-device AI search, smart cleanup, and a private vault.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        PermissionPoint(Icons.Outlined.PhotoLibrary, "Access your photos, videos, audio, and documents")
        PermissionPoint(Icons.Outlined.Lock, "Browse and organize all files on this device")
        PermissionPoint(Icons.Outlined.Shield, "Indexing and analysis happen on your device by default")
        Spacer(Modifier.height(28.dp))
        Button(onClick = onGrant) { Text("Continue") }
    }
}

@Composable
private fun PermissionPoint(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.size(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun Root(vm: VM) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(
        Triple("Files", Icons.Outlined.Folder, 0),
        Triple("Gallery", Icons.Outlined.PhotoLibrary, 1),
        Triple("AI Chat", Icons.Outlined.ChatBubbleOutline, 2),
    )
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar {
                    tabs.forEach { (label, icon, index) ->
                        NavigationBarItem(
                            selected = tab == index,
                            onClick = { tab = index },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                        )
                    }
                }
            },
        ) { pad ->
            Box(
                modifier = Modifier
                    .padding(pad)
                    .fillMaxSize(),
            ) {
                when (tab) {
                    0 -> FilesTab(vm)
                    1 -> GalleryTab(vm)
                    else -> ChatTab(vm)
                }
            }
        }

        vm.viewer?.let { state ->
            key(state.items) { Viewer(vm, state.items, state.index) }
        }
        if (vm.showVault) VaultScreen(vm)
        vm.player?.let { file -> VideoPlayer(file) { vm.player = null } }
        if (vm.showAiTools) AiToolsOverlay(vm, onClose = { vm.showAiTools = false })
        if (vm.showAiSettings) AiSettingsScreen(vm, onClose = { vm.showAiSettings = false })
    }
}
