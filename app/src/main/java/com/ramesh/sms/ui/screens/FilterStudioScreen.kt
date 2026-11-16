package com.ramesh.sms.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GppGood
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ramesh.sms.R
import com.ramesh.sms.data.model.FilterAction
import com.ramesh.sms.data.model.FilterRule
import com.ramesh.sms.data.model.RuleListType
import com.ramesh.sms.data.model.displayLabel
import com.ramesh.sms.theme.PillShape
import com.ramesh.sms.theme.SquircleCardShape
import com.ramesh.sms.theme.SquircleMediumShape
import com.ramesh.sms.theme.TagShape
import com.ramesh.sms.ui.util.contentAware
import com.ramesh.sms.ui.viewmodel.DecidedSender
import com.ramesh.sms.ui.viewmodel.FilterStudioUiState
import com.ramesh.sms.ui.viewmodel.FilterStudioViewModel

private enum class FilterStudioTab { Playground, Allowlist, Blocklist, Presets, Decisions }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterStudioScreen(
    onBackClick: () -> Unit,
    viewModel: FilterStudioViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(FilterStudioTab.Playground) }

    var showSaveDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var initialAddListType by remember { mutableStateOf(RuleListType.ALLOWLIST) }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    var editingRule by remember { mutableStateOf<FilterRule?>(null) }
    var ruleToDelete by remember { mutableStateOf<FilterRule?>(null) }

    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.filter_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = PillShape,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.filter_regex_word),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.filter_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.filter_back))
                    }
                },
                actions = {
                    // Export JSON (Allowlist + Blocklist rules)
                    IconButton(onClick = {
                        val json = viewModel.exportRulesJson()
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(context.getString(R.string.filter_export_label), json)
                        )
                        Toast.makeText(
                            context,
                            context.getString(R.string.filter_exported, state.rules.size),
                            Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(Icons.Default.Download, contentDescription = stringResource(R.string.filter_export))
                    }

                    // Import JSON
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.Upload, contentDescription = stringResource(R.string.filter_import))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (selectedTab == FilterStudioTab.Allowlist || selectedTab == FilterStudioTab.Blocklist) {
                FloatingActionButton(
                    onClick = {
                        initialAddListType = if (selectedTab == FilterStudioTab.Allowlist) {
                            RuleListType.ALLOWLIST
                        } else {
                            RuleListType.BLOCKLIST
                        }
                        showAddDialog = true
                    },
                    containerColor = if (selectedTab == FilterStudioTab.Allowlist) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    contentColor = if (selectedTab == FilterStudioTab.Allowlist) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimary
                    },
                    shape = PillShape
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = if (selectedTab == FilterStudioTab.Allowlist) {
                            "Add Allowlist Rule"
                        } else {
                            "Add Blocklist Rule"
                        }
                    )
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                edgePadding = 12.dp
            ) {
                Tab(
                    selected = selectedTab == FilterStudioTab.Playground,
                    onClick = { selectedTab = FilterStudioTab.Playground },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.filter_playground), fontWeight = if (selectedTab == FilterStudioTab.Playground) FontWeight.Bold else FontWeight.Medium)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == FilterStudioTab.Allowlist,
                    onClick = { selectedTab = FilterStudioTab.Allowlist },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GppGood, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.filter_tab_allowlist, state.allowlistRules.size),
                                fontWeight = if (selectedTab == FilterStudioTab.Allowlist) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                )
                Tab(
                    selected = selectedTab == FilterStudioTab.Blocklist,
                    onClick = { selectedTab = FilterStudioTab.Blocklist },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.filter_tab_blocklist, state.blocklistRules.size),
                                fontWeight = if (selectedTab == FilterStudioTab.Blocklist) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                )
                Tab(
                    selected = selectedTab == FilterStudioTab.Presets,
                    onClick = { selectedTab = FilterStudioTab.Presets },
                    text = {
                        Text(
                            stringResource(R.string.filter_tab_presets, state.predefinedRules.size),
                            fontWeight = if (selectedTab == FilterStudioTab.Presets) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
                // The one place that lists senders decided about one by one, from the participant
                // sheet. Without it a number silenced by mistake had no address anywhere in the app.
                Tab(
                    selected = selectedTab == FilterStudioTab.Decisions,
                    onClick = { selectedTab = FilterStudioTab.Decisions },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeMute, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                stringResource(R.string.filter_tab_decisions, state.decidedSenders.size),
                                fontWeight = if (selectedTab == FilterStudioTab.Decisions) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                )
            }

            when (selectedTab) {
                FilterStudioTab.Playground -> PlaygroundTab(
                    state = state,
                    onPatternChange = viewModel::onPlaygroundPatternChanged,
                    onSampleChange = viewModel::onPlaygroundSampleTextChanged,
                    onActionChange = viewModel::onPlaygroundActionChanged,
                    onListTypeChange = viewModel::onPlaygroundListTypeChanged,
                    onPresetSelect = viewModel::setSamplePreset,
                    onSaveClick = { showSaveDialog = true }
                )
                FilterStudioTab.Allowlist -> AllowlistTab(
                    rules = state.allowlistRules,
                    onToggle = viewModel::toggleRule,
                    onEdit = { editingRule = it },
                    onDelete = { ruleToDelete = it }
                )
                FilterStudioTab.Blocklist -> BlocklistTab(
                    rules = state.blocklistRules,
                    onToggle = viewModel::toggleRule,
                    onEdit = { editingRule = it },
                    onDelete = { ruleToDelete = it }
                )
                FilterStudioTab.Presets -> PredefinedRulesTab(
                    rules = state.predefinedRules,
                    onToggle = viewModel::toggleRule,
                    onEdit = { editingRule = it },
                    onDelete = { ruleToDelete = it }
                )
                FilterStudioTab.Decisions -> DecisionsTab(
                    senders = state.decidedSenders,
                    onRestore = viewModel::restoreSender
                )
            }
        }
    }

    // Edit Rule Dialog
    editingRule?.let { rule ->
        EditRuleDialog(
            rule = rule,
            onDismiss = { editingRule = null },
            onSave = { updatedRule ->
                viewModel.updateRule(updatedRule)
                editingRule = null
            }
        )
    }

    // Delete Confirmation Dialog
    ruleToDelete?.let { rule ->
        AlertDialog(
            onDismissRequest = { ruleToDelete = null },
            shape = SquircleCardShape,
            title = { Text(stringResource(R.string.filter_delete_rule_title), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = stringResource(R.string.filter_delete_rule_body, rule.name),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRule(rule.id)
                        ruleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = PillShape
                ) {
                    Text(stringResource(R.string.filter_delete_word), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { ruleToDelete = null }) { Text(stringResource(R.string.filter_cancel_word)) }
            }
        )
    }

    // Save Playground Dialog
    if (showSaveDialog) {
        var ruleName by remember { mutableStateOf("") }
        var ruleDesc by remember { mutableStateOf("") }
        var selectedListType by remember { mutableStateOf(state.simulatedListType) }
        var selectedAction by remember { mutableStateOf(state.simulatedAction) }

        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            shape = SquircleCardShape,
            title = { Text(stringResource(R.string.filter_save_custom_title), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = ruleName,
                        onValueChange = { ruleName = it },
                        label = { Text(stringResource(R.string.filter_rule_name)) },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.contentAware(),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = ruleDesc,
                        onValueChange = { ruleDesc = it },
                        label = { Text(stringResource(R.string.filter_description_optional)) },
                        textStyle = LocalTextStyle.current.contentAware(),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(R.string.filter_list_classification),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                            RadioButton(
                                selected = selectedListType == RuleListType.ALLOWLIST,
                                onClick = {
                                    selectedListType = RuleListType.ALLOWLIST
                                    selectedAction = FilterAction.NORMAL
                                }
                            )
                            Text(stringResource(R.string.filter_allowlist), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedListType == RuleListType.BLOCKLIST,
                                onClick = {
                                    selectedListType = RuleListType.BLOCKLIST
                                    selectedAction = FilterAction.SPAM
                                }
                            )
                            Text(stringResource(R.string.filter_blocklist), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (selectedListType == RuleListType.BLOCKLIST) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.filter_trigger_action),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            listOf(FilterAction.SPAM, FilterAction.SILENT).forEach { act ->
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                                    RadioButton(
                                        selected = selectedAction == act,
                                        onClick = { selectedAction = act }
                                    )
                                    Text(act.displayLabel, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = TagShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.filter_pattern_value, state.testPattern.orEmpty()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        if (ruleName.isNotBlank()) {
                            viewModel.savePlaygroundRuleAsCustom(
                                name = ruleName,
                                description = ruleDesc,
                                listType = selectedListType,
                                action = selectedAction
                            )
                            showSaveDialog = false
                        }
                    },
                    shape = PillShape
                ) {
                    Text(stringResource(R.string.filter_save_rule), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text(stringResource(R.string.filter_cancel_word)) }
            }
        )
    }

    // Add Custom Rule Dialog
    if (showAddDialog) {
        AddGlobalRuleDialog(
            targetAddress = null,
            initialListType = initialAddListType,
            onDismiss = { showAddDialog = false },
            onSave = { pattern, isRegex, action, name, listType ->
                viewModel.onPlaygroundPatternChanged(pattern)
                viewModel.onPlaygroundIsRegexChanged(isRegex)
                viewModel.onPlaygroundActionChanged(action)
                viewModel.onPlaygroundListTypeChanged(listType)
                viewModel.savePlaygroundRuleAsCustom(
                    name = name,
                    description = "",
                    listType = listType,
                    action = action
                )
                showAddDialog = false
            }
        )
    }


    // Import JSON Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            shape = SquircleCardShape,
            title = { Text(stringResource(R.string.filter_import_title), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.filter_import_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        placeholder = { Text("[{\"name\": \"Sample OTP\", \"pattern\": \"...\", \"listType\": \"ALLOWLIST\"}]") },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    )
                }
            },
            confirmButton = {
                FilledTonalButton(
                    onClick = {
                        if (importJsonText.isNotBlank()) {
                            viewModel.importRulesJson(importJsonText) { success, addedCount, skippedCount ->
                                if (success) {
                                    val msg = if (skippedCount > 0) {
                                        "Imported $addedCount rule${if (addedCount != 1) "s" else ""}, skipped $skippedCount duplicate${if (skippedCount != 1) "s" else ""}."
                                    } else {
                                        "Successfully imported $addedCount rule${if (addedCount != 1) "s" else ""}!"
                                    }
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    showImportDialog = false
                                    importJsonText = ""
                                } else {
                                    Toast.makeText(context, "Invalid JSON format", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    shape = PillShape
                ) {
                    Text(stringResource(R.string.filter_import_action), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) { Text(stringResource(R.string.filter_cancel_word)) }
            }
        )
    }
}

@Composable
private fun PlaygroundTab(
    state: FilterStudioUiState,
    onPatternChange: (String) -> Unit,
    onSampleChange: (String) -> Unit,
    onActionChange: (FilterAction) -> Unit,
    onListTypeChange: (RuleListType) -> Unit,
    onPresetSelect: (String, String, RuleListType) -> Unit,
    onSaveClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            // OTP Allowlist Quick-Fills
            Text(
                text = stringResource(R.string.filter_presets_allowlist),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {
                        onPresetSelect(
                            "کد تایید شما برای ورود به اسنپ: 481923",
                            "(?i)(کد\\s*(ت[اأآ]یید|ورود|فعالسازی)|(verification|auth|login)\\s*code|passcode)",
                            RuleListType.ALLOWLIST
                        )
                    },
                    label = { Text(stringResource(R.string.filter_preset_otp)) },
                    leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    shape = PillShape,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                AssistChip(
                    onClick = {
                        onPresetSelect(
                            "بانک سامان: رمز یکبار مصرف پویا برای کارت ۶۲۱۹ شما: 918234",
                            "(رمز\\s*(پویا|دوم|یکبار\\s*مصرف)|(one[ -]?time\\s*password|otp\\s*code))",
                            RuleListType.ALLOWLIST
                        )
                    },
                    label = { Text(stringResource(R.string.filter_preset_bank)) },
                    leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    shape = PillShape,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
                AssistChip(
                    onClick = {
                        onPresetSelect(
                            "دیجی کالا: کد: 582103 برای ورود استفاده شود.",
                            "(?i)(کد\\s*[:=]\\s*[0-9۰-۹]{4,8}|رمز\\s*[:=]\\s*[0-9۰-۹]{4,8}|otp\\s*[:=]?\\s*[0-9]{4,8})",
                            RuleListType.ALLOWLIST
                        )
                    },
                    label = { Text(stringResource(R.string.filter_preset_direct)) },
                    leadingIcon = { Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    shape = PillShape,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Spam Blocklist Quick-Fills
            Text(
                text = stringResource(R.string.filter_presets_blocklist),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {
                        onPresetSelect(
                            "مشترک گرامی، ۵۰ درصد تخفیف ویژه خرید اینترنت برای شما فعال شد. جهت انصراف لغو ۱۱ را ارسال فرمایید.",
                            "(لغو\\s*(11|۱۱)|تخفیف|اینترنت)",
                            RuleListType.BLOCKLIST
                        )
                    },
                    label = { Text(stringResource(R.string.filter_preset_promo)) },
                    shape = PillShape,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
                AssistChip(
                    onClick = {
                        onPresetSelect(
                            "وام فوری بدون ضامن تا سقف ۵۰ میلیون تومان، پرداخت ۲۴ ساعته. ثبت نام سریع:",
                            "وام\\s*(فوری|بدون\\s*ضامن)|پرداخت\\s*۲۴\\s*ساعته",
                            RuleListType.BLOCKLIST
                        )
                    },
                    label = { Text(stringResource(R.string.filter_preset_loan)) },
                    shape = PillShape,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
                AssistChip(
                    onClick = {
                        onPresetSelect(
                            "سامانه ثنا: ابلاغیه جدید علیه شما صادر گردید. مشاهده در: http://sana-adlieh.top/apk",
                            "(ابلاغیه|سامانه\\s*ثنا).*http",
                            RuleListType.BLOCKLIST
                        )
                    },
                    label = { Text(stringResource(R.string.filter_preset_phishing)) },
                    shape = PillShape,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
                AssistChip(
                    onClick = {
                        onPresetSelect(
                            "بسته تخفیف ماهیانه اینترنت همراه ۵۰ گیگابایت با قیمت استثنایی فعال شد.",
                            "(تخفیف\\s*(های\\s*)?ماه[ی]?انه|بسته\\s*(های\\s*)?تخفیف\\s*ماه[ی]?انه|پیشنهاد\\s*ماه[ی]?انه)",
                            RuleListType.BLOCKLIST
                        )
                    },
                    label = { Text(stringResource(R.string.filter_preset_discount)) },
                    shape = PillShape,
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Regex Pattern Input
            OutlinedTextField(
                value = state.testPattern,
                onValueChange = onPatternChange,
                label = { Text(stringResource(R.string.filter_regex_pattern)) },
                supportingText = {
                    val res = state.testResult
                    if (res != null && !res.isValid) {
                        Text(
                            stringResource(R.string.filter_error, res.errorMessage.orEmpty()),
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Text(stringResource(R.string.filter_unicode_help), color = MaterialTheme.colorScheme.outline)
                    }
                },
                isError = state.testResult?.isValid == false,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sample SMS Text Input
            OutlinedTextField(
                value = state.testSampleText,
                onValueChange = onSampleChange,
                label = { Text(stringResource(R.string.filter_test_sample)) },
                minLines = 3,
                maxLines = 6,
                textStyle = LocalTextStyle.current.contentAware(),
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Real-Time Evaluation Result Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = SquircleCardShape,
                colors = CardDefaults.cardColors(
                    containerColor = if (state.simulatedListType == RuleListType.ALLOWLIST)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    else MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isMatch = state.testResult?.isMatch == true
                            Icon(
                                imageVector = if (isMatch) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (isMatch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(if (isMatch) R.string.filter_match_found else R.string.filter_no_match),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isMatch) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Simulated Classification Badge
                        Surface(
                            shape = PillShape,
                            color = if (state.simulatedListType == RuleListType.ALLOWLIST) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                when (state.simulatedAction) {
                                    FilterAction.SPAM -> MaterialTheme.colorScheme.errorContainer
                                    FilterAction.SILENT -> MaterialTheme.colorScheme.secondaryContainer
                                    FilterAction.NORMAL -> MaterialTheme.colorScheme.primaryContainer
                                }
                            }
                        ) {
                            Text(
                                text = if (state.simulatedListType == RuleListType.ALLOWLIST) {
                                    stringResource(R.string.filter_allowlist_badge)
                                } else {
                                    stringResource(R.string.filter_blocklist_badge, state.simulatedAction.displayLabel)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = if (state.simulatedListType == RuleListType.ALLOWLIST) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    when (state.simulatedAction) {
                                        FilterAction.SPAM -> MaterialTheme.colorScheme.onErrorContainer
                                        FilterAction.SILENT -> MaterialTheme.colorScheme.onSecondaryContainer
                                        FilterAction.NORMAL -> MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                }
                            )
                        }
                    }

                    if (state.testResult?.isMatch == true) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(R.string.filter_matched_substring, state.testResult.matchedSubstrings.joinToString()),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // List Classification Selector
                    Text(
                        text = stringResource(R.string.filter_list_target),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 16.dp)) {
                            RadioButton(
                                selected = state.simulatedListType == RuleListType.ALLOWLIST,
                                onClick = { onListTypeChange(RuleListType.ALLOWLIST) }
                            )
                            Text(
                                text = stringResource(R.string.filter_allowlist_priority),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (state.simulatedListType == RuleListType.ALLOWLIST) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = state.simulatedListType == RuleListType.BLOCKLIST,
                                onClick = { onListTypeChange(RuleListType.BLOCKLIST) }
                            )
                            Text(
                                text = stringResource(R.string.filter_blocklist),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (state.simulatedListType == RuleListType.BLOCKLIST) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    if (state.simulatedListType == RuleListType.BLOCKLIST) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.filter_blocklist_action),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                listOf(FilterAction.SPAM, FilterAction.SILENT).forEach { act ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(start = 6.dp)
                                    ) {
                                        RadioButton(
                                            selected = state.simulatedAction == act,
                                            onClick = { onActionChange(act) }
                                        )
                                        Text(
                                            text = act.displayLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (state.simulatedAction == act) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onSaveClick,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.testResult?.isValid == true && state.testPattern.isNotBlank(),
                        shape = PillShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.simulatedListType == RuleListType.ALLOWLIST) {
                                stringResource(R.string.filter_save_as_allowlist)
                            } else {
                                stringResource(R.string.filter_save_as_blocklist)
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AllowlistTab(
    rules: List<FilterRule>,
    onToggle: (FilterRule, Boolean) -> Unit,
    onEdit: (FilterRule) -> Unit,
    onDelete: (FilterRule) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = SquircleMediumShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.GppGood,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.filter_priority_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.filter_priority_body),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        if (rules.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.filter_empty_allowlist),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            items(rules, key = { it.id }) { rule ->
                RuleCard(
                    rule = rule,
                    onToggle = { onToggle(rule, it) },
                    onEdit = { onEdit(rule) },
                    onDelete = { onDelete(rule) }
                )
            }
        }
    }
}

@Composable
private fun BlocklistTab(
    rules: List<FilterRule>,
    onToggle: (FilterRule, Boolean) -> Unit,
    onEdit: (FilterRule) -> Unit,
    onDelete: (FilterRule) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (rules.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.filter_empty_blocklist),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        } else {
            items(rules, key = { it.id }) { rule ->
                RuleCard(
                    rule = rule,
                    onToggle = { onToggle(rule, it) },
                    onEdit = { onEdit(rule) },
                    onDelete = { onDelete(rule) }
                )
            }
        }
    }
}

@Composable
private fun PredefinedRulesTab(
    rules: List<FilterRule>,
    onToggle: (FilterRule, Boolean) -> Unit,
    onEdit: (FilterRule) -> Unit,
    onDelete: (FilterRule) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = SquircleMediumShape,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.filter_presets_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        items(rules, key = { it.id }) { rule ->
            RuleCard(
                rule = rule,
                onToggle = { onToggle(rule, it) },
                onEdit = { onEdit(rule) },
                onDelete = { onDelete(rule) }
            )
        }
    }
}

@Composable
private fun RuleCard(
    rule: FilterRule,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val isAllowlist = rule.listType == RuleListType.ALLOWLIST

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = SquircleMediumShape,
        colors = CardDefaults.cardColors(
            containerColor = if (rule.isEnabled) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else MaterialTheme.colorScheme.surfaceContainerLowest
        ),
        border = BorderStroke(
            if (isAllowlist && rule.isEnabled) 1.dp else 0.5.dp,
            if (isAllowlist && rule.isEnabled) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = rule.name,
                            style = MaterialTheme.typography.titleMedium.contentAware(),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (rule.description.isNotBlank()) {
                        Text(
                            text = rule.description,
                            style = MaterialTheme.typography.bodySmall.contentAware(),
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                Switch(checked = rule.isEnabled, onCheckedChange = onToggle)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = TagShape,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = rule.pattern,
                    style = MaterialTheme.typography.labelSmall.contentAware(),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isAllowlist) {
                        Surface(
                            shape = PillShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.GppGood,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.filter_allowlist_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.filter_action_prefix),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Surface(
                            shape = PillShape,
                            color = when (rule.action) {
                                FilterAction.SPAM -> MaterialTheme.colorScheme.errorContainer
                                FilterAction.SILENT -> MaterialTheme.colorScheme.secondaryContainer
                                FilterAction.NORMAL -> MaterialTheme.colorScheme.primaryContainer
                            },
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                text = rule.action.displayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = when (rule.action) {
                                    FilterAction.SPAM -> MaterialTheme.colorScheme.onErrorContainer
                                    FilterAction.SILENT -> MaterialTheme.colorScheme.onSecondaryContainer
                                    FilterAction.NORMAL -> MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Edit Rule
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.filter_edit_rule),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Delete Rule
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.filter_delete_rule),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditRuleDialog(
    rule: FilterRule,
    onDismiss: () -> Unit,
    onSave: (FilterRule) -> Unit
) {
    var name by remember { mutableStateOf(rule.name) }
    var pattern by remember { mutableStateOf(rule.pattern) }
    var description by remember { mutableStateOf(rule.description) }
    var selectedListType by remember { mutableStateOf(rule.listType) }
    var selectedAction by remember { mutableStateOf(rule.action) }
    var isRegex by remember { mutableStateOf(rule.isRegex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = SquircleCardShape,
        title = {
            Text(
                text = if (rule.isPredefined) "Edit Preset Rule" else "Edit Custom Rule",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.filter_rule_name)) },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.contentAware(),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text(if (isRegex) stringResource(R.string.filter_regex_pattern) else stringResource(R.string.filter_pattern_plain)) },
                    textStyle = LocalTextStyle.current.contentAware(),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                FilterChip(
                    selected = isRegex,
                    onClick = { isRegex = !isRegex },
                    label = { Text(stringResource(R.string.filter_regex_word)) },
                    shape = PillShape
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.filter_description)) },
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.filter_list_classification),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                        RadioButton(
                            selected = selectedListType == RuleListType.ALLOWLIST,
                            onClick = {
                                selectedListType = RuleListType.ALLOWLIST
                                selectedAction = FilterAction.NORMAL
                            }
                        )
                        Text(stringResource(R.string.filter_allowlist), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = selectedListType == RuleListType.BLOCKLIST,
                            onClick = {
                                selectedListType = RuleListType.BLOCKLIST
                                if (selectedAction == FilterAction.NORMAL) selectedAction = FilterAction.SPAM
                            }
                        )
                        Text(stringResource(R.string.filter_blocklist), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }

                if (selectedListType == RuleListType.BLOCKLIST) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.filter_action_tier),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(FilterAction.SPAM, FilterAction.SILENT).forEach { act ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                RadioButton(
                                    selected = selectedAction == act,
                                    onClick = { selectedAction = act }
                                )
                                Text(
                                    text = act.displayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selectedAction == act) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    if (name.isNotBlank() && pattern.isNotBlank()) {
                        onSave(
                            rule.copy(
                                name = name,
                                pattern = pattern,
                                description = description,
                                listType = selectedListType,
                                action = if (selectedListType == RuleListType.ALLOWLIST) FilterAction.NORMAL else selectedAction,
                                isRegex = isRegex
                            )
                        )
                    }
                },
                shape = PillShape
            ) {
                Text(stringResource(R.string.filter_save_changes), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.filter_cancel_word)) }
        }
    )
}

@Composable
fun AddGlobalRuleDialog(
    targetAddress: String?,
    initialListType: RuleListType = RuleListType.ALLOWLIST,
    onDismiss: () -> Unit,
    onSave: (pattern: String, isRegex: Boolean, action: FilterAction, name: String, listType: RuleListType) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf("") }
    var isRegex by remember { mutableStateOf(true) }
    var listType by remember { mutableStateOf(initialListType) }
    var action by remember { mutableStateOf(if (initialListType == RuleListType.ALLOWLIST) FilterAction.NORMAL else FilterAction.SPAM) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = SquircleCardShape,
        title = {
            Text(
                text = when {
                    !targetAddress.isNullOrBlank() -> "Rule for $targetAddress"
                    listType == RuleListType.ALLOWLIST -> "New Allowlist Rule"
                    else -> "New Blocklist Rule"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = {
                        Text(
                            stringResource(
                                if (listType == RuleListType.ALLOWLIST) R.string.filter_rule_name_otp
                                else R.string.filter_rule_name_discount
                            )
                        )
                    },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.contentAware(),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = pattern,
                    onValueChange = { pattern = it },
                    label = { Text(stringResource(R.string.filter_regex_pattern)) },
                    textStyle = LocalTextStyle.current.contentAware(),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.filter_list_classification),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                        RadioButton(
                            selected = listType == RuleListType.ALLOWLIST,
                            onClick = {
                                listType = RuleListType.ALLOWLIST
                                action = FilterAction.NORMAL
                            }
                        )
                        Text(stringResource(R.string.filter_allowlist), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = listType == RuleListType.BLOCKLIST,
                            onClick = {
                                listType = RuleListType.BLOCKLIST
                                action = FilterAction.SPAM
                            }
                        )
                        Text(stringResource(R.string.filter_blocklist), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }

                if (listType == RuleListType.BLOCKLIST) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.filter_trigger_action),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(FilterAction.SPAM, FilterAction.SILENT).forEach { act ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = action == act, onClick = { action = act })
                                Text(
                                    text = act.displayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (action == act) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = {
                    if (pattern.isNotBlank()) {
                        onSave(pattern, isRegex, action, name, listType)
                    }
                },
                shape = PillShape
            ) {
                Text(stringResource(R.string.filter_save_rule), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.filter_cancel_word))
            }
        }
    )
}

/**
 * Senders the user silenced, marked as spam or blocked one by one.
 *
 * Each row can be put back to normal, which is the whole point: a number blocked from a conversation
 * used to be a decision with no undo and no list to find it in.
 */
@Composable
private fun DecisionsTab(
    senders: List<DecidedSender>,
    onRestore: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, senders) {
        val needle = query.trim()
        if (needle.isBlank()) {
            senders
        } else {
            senders.filter { sender ->
                sender.address.contains(needle, ignoreCase = true) ||
                    sender.displayName?.contains(needle, ignoreCase = true) == true
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text(stringResource(R.string.filter_decisions_search)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.VolumeOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.filter_decisions_empty),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.filter_decisions_empty_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items = filtered, key = { it.address }) { sender ->
                    val chipColor = when {
                        sender.isBlocked -> MaterialTheme.colorScheme.errorContainer
                        sender.action == FilterAction.SPAM -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    }
                    val chipContent = when {
                        sender.isBlocked || sender.action == FilterAction.SPAM ->
                            MaterialTheme.colorScheme.onErrorContainer
                        else -> MaterialTheme.colorScheme.onSecondaryContainer
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sender.displayName?.takeIf { it.isNotBlank() }
                                    ?: sender.address,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = sender.address,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Surface(shape = PillShape, color = chipColor) {
                            Text(
                                text = stringResource(
                                    when {
                                        sender.isBlocked -> R.string.filter_decision_blocked
                                        sender.action == FilterAction.SPAM -> R.string.filter_decision_spam
                                        else -> R.string.filter_decision_silent
                                    }
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = chipContent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        TextButton(onClick = { onRestore(sender.address) }) {
                            Text(
                                text = stringResource(R.string.filter_decision_restore),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}
