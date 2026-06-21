package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Casino
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DecisionList
import com.example.data.ListWithOption
import com.example.data.WheelOption
import com.example.ui.WheelComponent
import com.example.ui.WheelViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: WheelViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainLayout(viewModel)
            }
        }
    }
}

private val colorsPalette = listOf(
    "#FF5252", "#FF4081", "#E040FB", "#7C4DFF",
    "#448AFF", "#00E5FF", "#1DE9B6", "#00E676",
    "#EEFF41", "#FFD700", "#FF9100", "#FF3D00",
    "#78909C", "#8D6E63"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainLayout(viewModel: WheelViewModel) {
    var currentTab by remember { mutableStateOf("spin") }
    val allLists by viewModel.allLists.collectAsStateWithLifecycle()
    val activeList by viewModel.activeList.collectAsStateWithLifecycle()
    val isSpinning by viewModel.isSpinning.collectAsStateWithLifecycle()
    val spinResult by viewModel.spinResult.collectAsStateWithLifecycle()
    val targetAngle by viewModel.targetAngle.collectAsStateWithLifecycle()

    // Dialog state
    var showEditDialog by remember { mutableStateOf(false) }
    var editingListId by remember { mutableStateOf<Long?>(null) }
    var editTitle by remember { mutableStateOf("") }
    val editOptions = remember { mutableStateListOf<Pair<String, String>>() }

    // About Dialog GPL compliance notice
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Casino,
                            contentDescription = "Wheel Logo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Picker Wheel",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showAboutDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "GPL Info",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp)
            ) {
                NavigationBarItem(
                    selected = currentTab == "spin",
                    onClick = { currentTab = "spin" },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == "spin") Icons.Filled.Casino else Icons.Outlined.Casino,
                            contentDescription = "Spin Wheel"
                        )
                    },
                    label = { Text("Spin Wheel") }
                )
                NavigationBarItem(
                    selected = currentTab == "lists",
                    onClick = { currentTab = "lists" },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == "lists") Icons.Filled.List else Icons.Outlined.List,
                            contentDescription = "My Lists"
                        )
                    },
                    label = { Text("My Lists") }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                "spin" -> WheelTabContent(
                    activeList = activeList,
                    allLists = allLists,
                    targetAngle = targetAngle,
                    isSpinning = isSpinning,
                    onListSelected = { viewModel.selectActiveList(it) },
                    onSpinClick = { currentAngle -> viewModel.startSpin(currentAngle) },
                    onAnimationEnd = { index -> viewModel.onSpinAnimationEnd(index) }
                )
                "lists" -> ListsTabContent(
                    allLists = allLists,
                    activeListId = activeList?.decisionList?.id,
                    onListPlay = {
                        viewModel.selectActiveList(it)
                        currentTab = "spin"
                    },
                    onListEdit = { list ->
                        editingListId = list.decisionList.id
                        editTitle = list.decisionList.title
                        editOptions.clear()
                        list.options.forEach { opt ->
                            editOptions.add(opt.label to opt.colorHex)
                        }
                        showEditDialog = true
                    },
                    onListDelete = { id -> viewModel.deleteList(id) },
                    onCreateNewRequest = {
                        editingListId = null
                        editTitle = ""
                        editOptions.clear()
                        // pre-populate with 4 blank slices for fast onboarding
                        editOptions.add("" to colorsPalette[0])
                        editOptions.add("" to colorsPalette[4])
                        editOptions.add("" to colorsPalette[7])
                        editOptions.add("" to colorsPalette[9])
                        showEditDialog = true
                    }
                )
            }

            // Results Congratulation Dialog overlay of spin winner
            spinResult?.let { winner ->
                WinnerDialog(
                    winner = winner,
                    onDismiss = { viewModel.clearResult() }
                )
            }

            // GPL License and F-Droid About Info Dialog
            if (showAboutDialog) {
                AlertDialog(
                    onDismissRequest = { showAboutDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Verified, contentDescription = "GPL Compliance Logo", tint = Color(0xFF00E676))
                            Text("GNU GPL Open Source")
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                "This is a lightweight offline-first picker wheel application licensed under the GNU GPL v3.0.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "It complies with all F-Droid standards: offline-primary, ads-free, tracks-free, and open source. Create infinite lists and run physical-grade randomized decisions instantly.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Created with Jetpack Compose & SQLite Room database technology.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showAboutDialog = false }) {
                            Text("Got It")
                        }
                    }
                )
            }

            // Write Edit Database Lists Dialog
            if (showEditDialog) {
                EditListDialog(
                    title = if (editingListId == null) "Create New List" else "Edit Choice List",
                    listTitle = editTitle,
                    options = editOptions,
                    onTitleChange = { editTitle = it },
                    onSave = {
                        val validOptions = editOptions.filter { it.first.isNotBlank() }
                        if (editTitle.isBlank()) {
                            // Use automatic placeholder
                            editTitle = "Untitled List"
                        }
                        if (editingListId == null) {
                            viewModel.createNewList(editTitle, validOptions)
                        } else {
                            viewModel.updateList(editingListId!!, editTitle, validOptions)
                        }
                        showEditDialog = false
                    },
                    onDismiss = { showEditDialog = false }
                )
            }
        }
    }
}

@Composable
fun WheelTabContent(
    activeList: ListWithOption?,
    allLists: List<ListWithOption>,
    targetAngle: Float,
    isSpinning: Boolean,
    onListSelected: (ListWithOption) -> Unit,
    onSpinClick: (Float) -> Unit,
    onAnimationEnd: (Int) -> Unit
) {
    var storedAngle by remember { mutableStateOf(0f) }

    // Sync angle of animation across turns
    val derivedCurrentAngle by rememberUpdatedState(targetAngle)
    LaunchedEffect(isSpinning) {
        if (!isSpinning && derivedCurrentAngle > 0f) {
            storedAngle = derivedCurrentAngle
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Selector Header
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Select List to Spin",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allLists) { list ->
                    val isSelected = list.decisionList.id == activeList?.decisionList?.id
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (!isSpinning) {
                                onListSelected(list)
                                storedAngle = 0f
                            }
                        },
                        label = {
                            Text(
                                text = list.decisionList.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.1f))

        // Large Premium Wheel Canvas Area
        if (activeList != null && activeList.options.isNotEmpty()) {
            Text(
                text = activeList.decisionList.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Dynamic Options Count Indicators
            Text(
                text = "${activeList.options.size} divisions loaded",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            Box(
                modifier = Modifier
                    .sizeIn(maxHeight = 340.dp, maxWidth = 340.dp)
                    .weight(1f)
            ) {
                WheelComponent(
                    options = activeList.options,
                    targetAngle = targetAngle,
                    isSpinning = isSpinning,
                    onAnimationEnd = onAnimationEnd,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Big Tactile SPIN Action Trigger Button
            Button(
                onClick = { onSpinClick(storedAngle) },
                enabled = !isSpinning && activeList.options.size >= 2,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(54.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isSpinning) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Text("Spinning...", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    } else {
                        Icon(Icons.Default.Casino, contentDescription = "Dices")
                        Text("SPIN WHEEL", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        } else {
            // Empty wheel state indicator
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "Empty Logo",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "No Chooser List Active",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Go to 'My Lists' tab to add options or play one of the pre-loaded deciders!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.1f))
    }
}

@Composable
fun ListsTabContent(
    allLists: List<ListWithOption>,
    activeListId: Long?,
    onListPlay: (ListWithOption) -> Unit,
    onListEdit: (ListWithOption) -> Unit,
    onListDelete: (Long) -> Unit,
    onCreateNewRequest: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "My Custom Deciders",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${allLists.size} lists",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            if (allLists.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No lists saved. Click (+) to create one!",
                        color = MaterialTheme.colorScheme.secondary,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(allLists) { list ->
                        val isActive = list.decisionList.id == activeListId
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                                }
                            ),
                            border = if (isActive) {
                                borderStroke(2.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onListPlay(list) },
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = list.decisionList.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${list.options.size} items",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        IconButton(onClick = { onListPlay(list) }) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = "Load onto Wheel",
                                                tint = Color(0xFF00E676)
                                            )
                                        }
                                        IconButton(onClick = { onListEdit(list) }) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit List",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(onClick = { onListDelete(list.decisionList.id) }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete list",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }

                                // Preview dots of slices colors
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    list.options.take(12).forEach { option ->
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(Color(android.graphics.Color.parseColor(option.colorHex)))
                                        )
                                    }
                                    if (list.options.size > 12) {
                                        Text(
                                            text = "+${list.options.size - 12}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = onCreateNewRequest,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add List")
        }
    }
}

@Composable
fun WinnerDialog(
    winner: WheelOption,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "🎉 WINNER!",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                // Colored tag panel with Option text
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(android.graphics.Color.parseColor(winner.colorHex)))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val isLight = isColorLightHex(winner.colorHex)
                    val textColor = if (isLight) Color.Black else Color.White
                    Text(
                        text = winner.label,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                }

                Text(
                    text = "The wheel has spoken!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(0.6f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EditListDialog(
    title: String,
    listTitle: String,
    options: MutableList<Pair<String, String>>,
    onTitleChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    var expandedColorIndex by remember { mutableStateOf(-1) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = listTitle,
                    onValueChange = onTitleChange,
                    label = { Text("List Name") },
                    placeholder = { Text("e.g. Activity Decider") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Divider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Wheel Options (${options.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Need at least 2",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (options.size < 2) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    )
                }

                // Scrollable choices options
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(options) { index, option ->
                        val (label, colorHex) = option
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Color Picker Button indicator
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(colorHex)))
                                        .border(2.dp, Color.White, CircleShape)
                                        .clickable {
                                            expandedColorIndex = if (expandedColorIndex == index) -1 else index
                                        }
                                )

                                OutlinedTextField(
                                    value = label,
                                    onValueChange = { newVal ->
                                        options[index] = newVal to colorHex
                                    },
                                    placeholder = { Text("Option ${index + 1}") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color(android.graphics.Color.parseColor(colorHex))
                                    )
                                )

                                IconButton(
                                    onClick = {
                                        options.removeAt(index)
                                        if (expandedColorIndex == index) {
                                            expandedColorIndex = -1
                                        } else if (expandedColorIndex > index) {
                                            expandedColorIndex--
                                        }
                                    },
                                    enabled = options.size > 1
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete segment slice",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }

                            // Horizontal color choosing grid row (only visible when active expanded color picker index)
                            AnimatedVisibility(
                                visible = expandedColorIndex == index,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                ) {
                                    Text(
                                        text = "Choose Slice Color",
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(bottom = 4.dp),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(colorsPalette) { hex ->
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                                    .border(
                                                        width = if (colorHex == hex) 3.dp else 0.dp,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        options[index] = label to hex
                                                        expandedColorIndex = -1
                                                    }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            // Cycle picker colors for convenient setup and add item
                            val nextColor = colorsPalette[options.size % colorsPalette.size]
                            options.add("" to nextColor)
                            expandedColorIndex = options.size - 1
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Option")
                    }
                }

                Divider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onSave,
                        enabled = options.filter { it.first.isNotBlank() }.size >= 2
                    ) {
                        Text("Save List")
                    }
                }
            }
        }
    }
}

// Check Color Luma for popup text overlay contrast
private fun isColorLightHex(colorHex: String): Boolean {
    return try {
        val color = android.graphics.Color.parseColor(colorHex)
        val r = android.graphics.Color.red(color)
        val g = android.graphics.Color.green(color)
        val b = android.graphics.Color.blue(color)
        val luma = 0.299f * r + 0.587f * g + 0.114f * b
        luma > 175f
    } catch (e: Exception) {
        false
    }
}

@Composable
private fun borderStroke(width: androidx.compose.ui.unit.Dp, color: Color) =
    androidx.compose.foundation.BorderStroke(width, color)
