package app.aifiles

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val ChatHeaderLight = Color(0xFFF0F2F5)
private val ChatHeaderDark = Color(0xFF1F2C34)
private val ChatBgLight = Color(0xFFEFEAE2)
private val ChatBgDark = Color(0xFF0B141A)
private val OutLight = Color(0xFFD9FDD3)
private val OutDark = Color(0xFF005C4B)
private val InLight = Color(0xFFFFFFFF)
private val InDark = Color(0xFF202C33)
private val MetaLight = Color(0xFF667781)
private val MetaDark = Color(0xFF8696A0)
private val SentTick = Color(0xFF53BDEB)
private val ErrorLight = Color(0xFFFFD6D6)
private val ErrorDark = Color(0xFF5A2323)
private val SendGreen = Color(0xFF1FA855)
private val AvatarColors = listOf(
    Color(0xFF00A884),
    Color(0xFF128C7E),
    Color(0xFF546E7A),
    Color(0xFF6D4C41),
    Color(0xFF5D4037),
    Color(0xFF37474F),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTab(vm: VM) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var input by rememberSaveable { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    val messages = vm.messages
    val name = vm.aiConfig.name

    LaunchedEffect(vm.notice) {
        vm.notice?.let {
            snackbar.showSnackbar(it)
            vm.notice = null
        }
    }

    LaunchedEffect(messages.size, vm.chatBusy) {
        delay(60)
        val count = listState.layoutInfo.totalItemsCount
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    fun submit() {
        val text = input.trim()
        if (text.isBlank()) return
        vm.sendChat(text)
        input = ""
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (dark) ChatHeaderDark else ChatHeaderLight,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChatAvatar(name)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                            Text(
                                if (vm.chatBusy) "typing…" else vm.aiConfig.model,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (dark) MetaDark else MetaLight,
                                maxLines = 1,
                            )
                        }
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More options")
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(
                                text = { Text("AI tools") },
                                leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) },
                                onClick = {
                                    menu = false
                                    vm.showAiSettings = false
                                    vm.showAiTools = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Settings") },
                                leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                onClick = {
                                    menu = false
                                    vm.showAiTools = false
                                    vm.showAiSettings = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Clear chat") },
                                onClick = {
                                    menu = false
                                    vm.clearChat()
                                },
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            Surface(color = if (dark) ChatHeaderDark else ChatHeaderLight) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp, max = 132.dp),
                        shape = RoundedCornerShape(26.dp),
                        placeholder = { Text("Message $name") },
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { submit() }),
                    )
                    Spacer(Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = ::submit,
                        modifier = Modifier.size(52.dp),
                        containerColor = SendGreen,
                        contentColor = Color.White,
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send")
                    }
                }
            }
        },
    ) { pad ->
        Box(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .background(if (dark) ChatBgDark else ChatBgLight)
                .chatDots(dark),
        ) {
            if (messages.isEmpty() && !vm.chatBusy) {
                ChatEmptyState(name) { vm.sendChat(it) }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    var lastDay = ""
                    messages.forEach { message ->
                        val day = dayKey(message.timeMs)
                        if (day != lastDay) {
                            item(key = "day_$day") { DatePill(dayLabel(message.timeMs), dark) }
                            lastDay = day
                        }
                        item(key = message.id) { ChatBubble(message, name, dark) }
                    }
                    if (vm.chatBusy) {
                        item(key = "typing") { TypingBubble(name, dark) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatEmptyState(name: String, onPrompt: (String) -> Unit) {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = if (dark) ChatHeaderDark else Color.White,
            tonalElevation = 2.dp,
            modifier = Modifier.size(76.dp),
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = SendGreen,
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            "Chat with $name",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Ask about your files, search, or cleanup. Set the AI name and API details in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = if (dark) MetaDark else MetaLight,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        listOf(
            "Find my bills",
            "Show screenshots from last week",
            "How do I clean large files?",
        ).forEach { prompt ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = if (dark) InDark else Color.White,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onPrompt(prompt) },
            ) {
                Text(
                    prompt,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage, aiName: String, dark: Boolean) {
    val mine = message.isUser
    val bg = when {
        message.isError -> if (dark) ErrorDark else ErrorLight
        mine -> if (dark) OutDark else OutLight
        else -> if (dark) InDark else InLight
    }
    val fg = if (dark) Color.White else Color(0xFF1F2C34)
    val meta = if (dark) MetaDark else MetaLight

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (mine) 12.dp else 2.dp,
                bottomEnd = if (mine) 2.dp else 12.dp,
            ),
            color = bg,
            tonalElevation = 1.dp,
            shadowElevation = 1.dp,
            modifier = Modifier.widthIn(max = 340.dp),
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (!mine) {
                    Text(
                        if (message.isError) "Error" else aiName,
                        style = MaterialTheme.typography.labelSmall,
                        color = SendGreen,
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = fg,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.timeMs)),
                        style = MaterialTheme.typography.labelSmall,
                        color = meta,
                    )
                    if (mine) {
                        Spacer(Modifier.width(3.dp))
                        Text("✓✓", style = MaterialTheme.typography.labelSmall, color = SentTick)
                    }
                }
            }
        }
    }
}

@Composable
private fun TypingBubble(aiName: String, dark: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(
            shape = RoundedCornerShape(12.dp, 12.dp, 12.dp, 2.dp),
            color = if (dark) InDark else InLight,
            tonalElevation = 1.dp,
            shadowElevation = 1.dp,
        ) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("$aiName is typing", style = MaterialTheme.typography.bodySmall, color = if (dark) MetaDark else MetaLight)
                Spacer(Modifier.width(6.dp))
                Text("•••", style = MaterialTheme.typography.bodySmall, color = SendGreen)
            }
        }
    }
}

@Composable
private fun DatePill(label: String, dark: Boolean) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (dark) Color(0xFF233138) else Color(0xFFE1F2FB),
        ) {
            Text(
                label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = if (dark) MetaDark else Color(0xFF54656F),
            )
        }
    }
}

@Composable
private fun ChatAvatar(name: String) {
    val index = (name.hashCode() and Int.MAX_VALUE) % AvatarColors.size
    Surface(shape = CircleShape, color = AvatarColors[index], modifier = Modifier.size(40.dp)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                initials(name),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun initials(name: String): String = name.trim()
    .split(Regex("\\s+"))
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercaseChar().toString() }
    .ifBlank { "AI" }

private fun dayKey(timeMs: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timeMs }
    return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
}

private fun dayLabel(timeMs: Long): String {
    val today = Calendar.getInstance()
    val msg = Calendar.getInstance().apply { timeInMillis = timeMs }
    val sameDay = { a: Calendar, b: Calendar ->
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
            a.get(Calendar.MONTH) == b.get(Calendar.MONTH) &&
            a.get(Calendar.DAY_OF_MONTH) == b.get(Calendar.DAY_OF_MONTH)
    }
    return when {
        sameDay(today, msg) -> "TODAY"
        sameDay(today.apply { add(Calendar.DAY_OF_YEAR, -1) }, msg) -> "YESTERDAY"
        else -> SimpleDateFormat("EEE, d MMM", Locale.getDefault()).format(Date(timeMs)).uppercase(Locale.getDefault())
    }
}

private fun Modifier.chatDots(dark: Boolean): Modifier = drawBehind {
    val step = 24.dp.toPx()
    val radius = 1.dp.toPx()
    val color = if (dark) Color(0x0AFFFFFF) else Color(0x0A54656F)
    var x = 0f
    while (x < size.width) {
        var y = 0f
        while (y < size.height) {
            drawCircle(color = color, radius = radius, center = Offset(x, y))
            y += step
        }
        x += step
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(vm: VM, onClose: () -> Unit) {
    var draft by remember { mutableStateOf(vm.aiConfig) }
    var revealKey by remember { mutableStateOf(false) }
    var providerExpanded by remember { mutableStateOf(false) }

    BackHandler(onBack = onClose)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI settings") },
                navigationIcon = {
                    IconButton(onClose) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    androidx.compose.material3.TextButton(
                        onClick = {
                            vm.saveAiConfig(draft)
                            onClose()
                        },
                    ) { Text("Save") }
                },
            )
        },
    ) { pad ->
        LazyColumn(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text("Assistant", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            item {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { draft = draft.copy(name = it) },
                    label = { Text("AI name") },
                    placeholder = { Text("Nova") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = draft.provider.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("API provider") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { providerExpanded = true },
                    )
                    DropdownMenu(
                        expanded = providerExpanded,
                        onDismissRequest = { providerExpanded = false },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        AiProvider.entries.forEach { provider ->
                            DropdownMenuItem(
                                text = { Text(provider.label) },
                                onClick = {
                                    val defaultModel = if (provider == AiProvider.GEMINI) "gemini-2.5-flash" else "gpt-4o-mini"
                                    draft = draft.copy(provider = provider, model = defaultModel)
                                    providerExpanded = false
                                },
                            )
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = draft.apiKey,
                    onValueChange = { draft = draft.copy(apiKey = it) },
                    label = { Text("API key") },
                    singleLine = true,
                    visualTransformation = if (revealKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { revealKey = !revealKey }) {
                            Icon(
                                if (revealKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (revealKey) "Hide key" else "Show key",

                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                OutlinedTextField(
                    value = draft.model,
                    onValueChange = { draft = draft.copy(model = it) },
                    label = { Text("Model") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (draft.provider == AiProvider.OPENAI) {
                item {
                    OutlinedTextField(
                        value = draft.baseUrl,
                        onValueChange = { draft = draft.copy(baseUrl = it) },
                        label = { Text("Base URL") },
                        placeholder = { Text("https://api.openai.com/v1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = draft.systemPrompt,
                    onValueChange = { draft = draft.copy(systemPrompt = it) },
                    label = { Text("System prompt") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text(
                    "Cloud OCR",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = draft.cloudOcrAuto,
                        onCheckedChange = { draft = draft.copy(cloudOcrAuto = it) },
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Auto-read screenshots during indexing")
                }
            }
            item {
                Text(
                    "Your key and chat history are stored only in this app's private preferences. " +
                        "For Gemini, the same key is also used by the optional cloud OCR feature.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun AiToolsOverlay(vm: VM, onClose: () -> Unit) {
    BackHandler(onBack = onClose)
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        AiTab(vm = vm, onBack = onClose)
    }
}
