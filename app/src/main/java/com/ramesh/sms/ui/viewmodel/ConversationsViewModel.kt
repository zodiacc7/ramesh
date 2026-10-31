package com.ramesh.sms.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ramesh.sms.data.db.RameshDatabaseHelper
import com.ramesh.sms.data.model.ConversationThread
import com.ramesh.sms.data.model.SearchMessageResult
import com.ramesh.sms.data.model.SenderTab
import com.ramesh.sms.data.model.SimInfo
import com.ramesh.sms.data.repository.SmsRepository
import com.ramesh.sms.data.util.AppLanguage
import com.ramesh.sms.data.util.AppPreferences
import com.ramesh.sms.data.util.JalaliScope
import com.ramesh.sms.data.util.LockScreenPrivacy
import com.ramesh.sms.data.util.PhoneNumberKeys
import com.ramesh.sms.data.util.UserPreferences
import com.ramesh.sms.data.util.threadMatchesTab
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

data class ConversationsUiState(
    val isLoading: Boolean = true,
    val isSearching: Boolean = false,
    val threads: List<ConversationThread> = emptyList(),
    val filteredThreads: List<ConversationThread> = emptyList(),
    val matchingMessages: List<SearchMessageResult> = emptyList(),
    val searchQuery: String = "",
    val showContactsOnly: Boolean = false,
    val isDefaultSmsApp: Boolean = false,
    val hasSmsPermission: Boolean = true,
    val error: String? = null,
    val tabs: List<SenderTab> = emptyList(),
    val selectedTabId: Long? = null,
    val availableSims: List<SimInfo> = emptyList(),
    val selectedSimFilterSubId: Int? = null,
    val enableDateTapShamsiToggle: Boolean = true,
    val showNotificationDeleteAction: Boolean = false,
    val showTabs: Boolean = true,
    val showSimFilter: Boolean = true,
    val noSpamForContacts: Boolean = false,
    val deliveryReports: Boolean = false,
    val sendDelaySeconds: Int = 0,
    val autoDeleteLimit: Int = 0,
    val lockScreenPrivacy: LockScreenPrivacy = LockScreenPrivacy.FULL,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    /** Show a SIM slot badge next to each conversation (only meaningful with 2+ SIMs). */
    val showSimBadge: Boolean = true,
    /** Jalali calendar scope: inside chats only, or everywhere. */
    val jalaliScope: JalaliScope = JalaliScope.CHAT_ONLY,
    /** Position of the "All" pseudo tab inside the tab strip. */
    val allTabIndex: Int = 0,
    /** Conversations waiting out the undo window before they are really deleted. */
    val pendingDeleteIds: Set<Long> = emptySet(),
    /** Changes whenever a delete request came back from the provider empty handed. */
    val deleteFailureTick: Long = 0L
)

/** Id used for the "All" pseudo tab while reordering the tab strip (matches ALL_TAB_ID). */
const val ALL_TAB_SENTINEL = -1L

/** How long a deleted conversation can be brought back. */
const val UNDO_DELETE_WINDOW_MILLIS = 5_000L

class ConversationsViewModel(
    application: Application,
    private val userPreferences: UserPreferences
) : AndroidViewModel(application) {

    constructor(application: Application) : this(application, AppPreferences(application))

    private val repository = SmsRepository(application, userPreferences)
    private val dbHelper = RameshDatabaseHelper.getInstance(application)
    private var searchJob: Job? = null
    private val pendingDeletes = mutableMapOf<Long, Job>()
    private var loadJob: Job? = null
    private var defaultSmsCheckJob: Job? = null
    private var observerDebounceJob: Job? = null

    /** True once the tab strip has been read, so the launch default is applied only once. */
    private var initialTabApplied = false
    private var smsContentObserver: ContentObserver? = null

    private val _uiState = MutableStateFlow(
        ConversationsUiState(
            showContactsOnly = userPreferences.showContactsOnly,
            enableDateTapShamsiToggle = userPreferences.enableDateTapShamsiToggle,
            showNotificationDeleteAction = userPreferences.showNotificationDeleteAction,
            showTabs = userPreferences.showTabs,
            showSimFilter = userPreferences.showSimFilter,
            noSpamForContacts = userPreferences.noSpamForContacts,
            deliveryReports = userPreferences.deliveryReports,
            sendDelaySeconds = userPreferences.sendDelaySeconds,
            autoDeleteLimit = userPreferences.autoDeleteLimit,
            lockScreenPrivacy = userPreferences.lockScreenPrivacy,
            appLanguage = userPreferences.appLanguage,
            allTabIndex = userPreferences.allTabIndex,
            showSimBadge = userPreferences.showSimBadge,
            jalaliScope = userPreferences.jalaliScope
        )
    )
    val uiState: StateFlow<ConversationsUiState> = _uiState.asStateFlow()

    val selectedThreadIds = mutableStateSetOf<Long>()
    var isSelectionMode by mutableStateOf(false)
        private set

    init {
        registerSmsContentObserver()
        loadTabs()
        loadThreads()
        viewModelScope.launch {
            dbHelper.rulesChanged.drop(1).collect { loadThreads(silent = true, force = true) }
        }
        viewModelScope.launch {
            dbHelper.prefsChanged.drop(1).collect { loadThreads(silent = true, force = true) }
        }
        viewModelScope.launch {
            dbHelper.spamMetaChanged.drop(1).collect { loadThreads(silent = true, force = true) }
        }
        viewModelScope.launch {
            dbHelper.tabsChanged.collect { loadTabs() }
        }
        viewModelScope.launch {
            repository.simRepository.observeActiveSims().collect { sims ->
                val current = _uiState.value
                val validFilter = if (current.selectedSimFilterSubId != null && sims.any { it.subscriptionId == current.selectedSimFilterSubId }) {
                    current.selectedSimFilterSubId
                } else {
                    null
                }
                _uiState.value = current.copy(
                    availableSims = sims,
                    selectedSimFilterSubId = validFilter,
                    filteredThreads = filterThreads(
                        threads = current.threads,
                        query = current.searchQuery.trim(),
                        selectedTabId = current.selectedTabId,
                        tabs = current.tabs,
                        selectedSimFilterSubId = validFilter
                    )
                )
            }
        }
    }

    fun onInboxResumed() {
        _uiState.value = _uiState.value.copy(
            enableDateTapShamsiToggle = userPreferences.enableDateTapShamsiToggle,
            showNotificationDeleteAction = userPreferences.showNotificationDeleteAction,
            showTabs = userPreferences.showTabs,
            showSimFilter = userPreferences.showSimFilter,
            noSpamForContacts = userPreferences.noSpamForContacts,
            deliveryReports = userPreferences.deliveryReports,
            sendDelaySeconds = userPreferences.sendDelaySeconds,
            autoDeleteLimit = userPreferences.autoDeleteLimit,
            lockScreenPrivacy = userPreferences.lockScreenPrivacy,
            appLanguage = userPreferences.appLanguage,
            allTabIndex = userPreferences.allTabIndex,
            showSimBadge = userPreferences.showSimBadge,
            jalaliScope = userPreferences.jalaliScope
        )
        repository.invalidateLookupCaches()
        checkDefaultSmsStatus()
        loadTabs()
        loadThreads(silent = true, force = true)
    }

    fun clearUnreadForThread(threadId: Long) {
        val current = _uiState.value
        val updatedThreads = current.threads.map { thread ->
            if (thread.threadId == threadId && (thread.unreadCount > 0 || thread.isUnreadSpam)) {
                thread.copy(unreadCount = 0, isUnreadSpam = false)
            } else {
                thread
            }
        }
        if (updatedThreads != current.threads) {
            _uiState.value = current.copy(
                threads = updatedThreads,
                filteredThreads = current.filteredThreads.map { thread ->
                    if (thread.threadId == threadId && (thread.unreadCount > 0 || thread.isUnreadSpam)) {
                        thread.copy(unreadCount = 0, isUnreadSpam = false)
                    } else {
                        thread
                    }
                }
            )
        }
        viewModelScope.launch {
            repository.markThreadRead(threadId)
            if (updatedThreads != current.threads) {
                repository.saveCachedThreads(updatedThreads)
            }
        }
    }

    fun loadThreads(silent: Boolean = false, force: Boolean = false) {
        val hasPermission = ContextCompat.checkSelfPermission(
            getApplication<Application>(),
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            _uiState.value = _uiState.value.copy(
                hasSmsPermission = false,
                isLoading = false,
                error = SMS_PERMISSION_ERROR
            )
            return
        }
        val permissionError = _uiState.value.error == SMS_PERMISSION_ERROR
        if (!_uiState.value.hasSmsPermission || permissionError) {
            _uiState.value = _uiState.value.copy(
                hasSmsPermission = true,
                error = if (permissionError) null else _uiState.value.error
            )
        }

        if (loadJob?.isActive == true) {
            if (silent && !force) return
            loadJob?.cancel()
        }
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isDefaultSmsApp = repository.isDefaultSmsApp()
            )

            val cached = try {
                repository.getCachedThreads()
            } catch (e: Exception) {
                emptyList()
            }
            val query = _uiState.value.searchQuery.trim()
            if (cached.isNotEmpty() && _uiState.value.threads.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    threads = cached,
                    filteredThreads = filterThreads(cached, query)
                )
            } else {
                val showLoading = !silent && _uiState.value.threads.isEmpty()
                if (showLoading) {
                    _uiState.value = _uiState.value.copy(isLoading = true)
                }
            }

            try {
                val threads = repository.getThreads()
                val currentQuery = _uiState.value.searchQuery.trim()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    threads = threads,
                    filteredThreads = filterThreads(threads, currentQuery),
                    error = null
                )
                if (currentQuery.length >= SEARCH_MIN_QUERY_LENGTH) {
                    val messages = repository.searchAllMessages(currentQuery)
                    _uiState.value = _uiState.value.copy(
                        matchingMessages = messages,
                        isSearching = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        matchingMessages = emptyList(),
                        isSearching = false
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load messages"
                )
            }
        }
    }

    private fun scheduleSilentReload() {
        observerDebounceJob?.cancel()
        observerDebounceJob = viewModelScope.launch {
            delay(SMS_OBSERVER_DEBOUNCE_MS)
            loadThreads(silent = true, force = true)
        }
    }

    private fun registerSmsContentObserver() {
        val resolver = getApplication<Application>().contentResolver
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                scheduleSilentReload()
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                scheduleSilentReload()
            }
        }
        try {
            resolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, observer)
            smsContentObserver = observer
        } catch (e: Exception) {
            Log.e("ConversationsViewModel", "Failed to register SMS observer", e)
            return
        }
        try {
            resolver.registerContentObserver(Telephony.Threads.CONTENT_URI, true, observer)
        } catch (e: Exception) {
            Log.e("ConversationsViewModel", "Failed to register threads observer", e)
        }
    }

    override fun onCleared() {
        observerDebounceJob?.cancel()
        val observer = smsContentObserver
        if (observer != null) {
            try {
                getApplication<Application>().contentResolver.unregisterContentObserver(observer)
            } catch (e: Exception) {
                Log.e("ConversationsViewModel", "Failed to unregister SMS observer", e)
            }
            smsContentObserver = null
        }
        super.onCleared()
    }

    private fun filterThreads(
        threads: List<ConversationThread>,
        query: String,
        selectedTabId: Long? = _uiState.value.selectedTabId,
        tabs: List<SenderTab> = _uiState.value.tabs,
        selectedSimFilterSubId: Int? = _uiState.value.selectedSimFilterSubId,
        showTabs: Boolean = _uiState.value.showTabs,
        showSimFilter: Boolean = _uiState.value.showSimFilter,
        showContactsOnly: Boolean = _uiState.value.showContactsOnly
    ): List<ConversationThread> {
        val tab = if (showTabs && selectedTabId != null) tabs.find { it.id == selectedTabId } else null
        // A spam-only conversation has no content left to open, so it never becomes an empty row
        // in the inbox or in a "not spam" filter tab.
        val spamTabSelected = tab?.spamOnly == true
        val usable = threads.filter { it.messageCount > 0 && (spamTabSelected || !it.spamOnly) }
        val tabFiltered = if (tab != null) {
            usable.filter { threadMatchesTab(it, tab) }
        } else {
            usable
        }
        // "Contacts only" is a scope of the All tab; a custom tab defines its own scope.
        val contactFiltered = if (tab == null && showContactsOnly) {
            tabFiltered.filter { it.isContact }
        } else {
            tabFiltered
        }
        val effectiveSubId = if (showSimFilter) selectedSimFilterSubId else null
        val simFiltered = if (effectiveSubId != null) {
            contactFiltered.filter { it.subId == effectiveSubId }
        } else {
            contactFiltered
        }
        if (query.isBlank()) return simFiltered
        return simFiltered.filter {
            (it.contactName?.contains(query, ignoreCase = true) == true) ||
                    it.address.contains(query, ignoreCase = true) ||
                    it.snippet.contains(query, ignoreCase = true)
        }
    }

    fun selectSimFilter(subId: Int?) {
        val current = _uiState.value
        if (current.selectedSimFilterSubId == subId) return
        _uiState.value = current.copy(
            selectedSimFilterSubId = subId,
            filteredThreads = filterThreads(
                threads = current.threads,
                query = current.searchQuery.trim(),
                selectedTabId = current.selectedTabId,
                tabs = current.tabs,
                selectedSimFilterSubId = subId
            )
        )
    }

    fun onSearchQueryChanged(query: String) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _uiState.value = _uiState.value.copy(
                searchQuery = query,
                filteredThreads = filterThreads(_uiState.value.threads, ""),
                matchingMessages = emptyList(),
                isSearching = false
            )
            return
        }

        val filtered = filterThreads(_uiState.value.threads, trimmed)
        if (trimmed.length < SEARCH_MIN_QUERY_LENGTH) {
            _uiState.value = _uiState.value.copy(
                searchQuery = query,
                filteredThreads = filtered,
                matchingMessages = emptyList(),
                isSearching = false
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            filteredThreads = filtered,
            isSearching = true
        )

        searchJob = viewModelScope.launch {
            delay(200)
            val messageResults = repository.searchAllMessages(trimmed)
            _uiState.value = _uiState.value.copy(
                matchingMessages = messageResults,
                isSearching = false
            )
        }
    }

    fun toggleContactsOnly() {
        // Only the All tab has a "contacts only" scope.
        val current = _uiState.value
        if (current.selectedTabId != null) return
        val nextValue = !current.showContactsOnly
        userPreferences.showContactsOnly = nextValue
        _uiState.value = current.copy(
            showContactsOnly = nextValue,
            filteredThreads = filterThreads(
                threads = current.threads,
                query = current.searchQuery.trim(),
                selectedTabId = current.selectedTabId,
                tabs = current.tabs,
                showContactsOnly = nextValue
            )
        )
    }

    fun setDateTapShamsiEnabled(enabled: Boolean) {
        userPreferences.enableDateTapShamsiToggle = enabled
        _uiState.value = _uiState.value.copy(
            enableDateTapShamsiToggle = enabled
        )
    }

    fun setNotificationDeleteActionEnabled(enabled: Boolean) {
        userPreferences.showNotificationDeleteAction = enabled
        _uiState.value = _uiState.value.copy(
            showNotificationDeleteAction = enabled
        )
    }

    fun setShowTabsEnabled(enabled: Boolean) {
        userPreferences.showTabs = enabled
        val current = _uiState.value
        _uiState.value = current.copy(
            showTabs = enabled,
            filteredThreads = filterThreads(
                threads = current.threads,
                query = current.searchQuery.trim(),
                selectedTabId = current.selectedTabId,
                tabs = current.tabs,
                selectedSimFilterSubId = current.selectedSimFilterSubId,
                showTabs = enabled,
                showSimFilter = current.showSimFilter
            )
        )
    }

    fun setShowSimBadgeEnabled(enabled: Boolean) {
        userPreferences.showSimBadge = enabled
        _uiState.value = _uiState.value.copy(showSimBadge = enabled)
    }

    fun setJalaliScope(scope: JalaliScope) {
        userPreferences.jalaliScope = scope
        _uiState.value = _uiState.value.copy(jalaliScope = scope)
    }

    fun setShowSimFilterEnabled(enabled: Boolean) {
        userPreferences.showSimFilter = enabled
        val current = _uiState.value
        _uiState.value = current.copy(
            showSimFilter = enabled,
            filteredThreads = filterThreads(
                threads = current.threads,
                query = current.searchQuery.trim(),
                selectedTabId = current.selectedTabId,
                tabs = current.tabs,
                selectedSimFilterSubId = current.selectedSimFilterSubId,
                showTabs = current.showTabs,
                showSimFilter = enabled
            )
        )
    }

    fun setNoSpamForContactsEnabled(enabled: Boolean) {
        userPreferences.noSpamForContacts = enabled
        _uiState.value = _uiState.value.copy(noSpamForContacts = enabled)
        loadThreads(silent = true, force = true)
    }

    fun setDeliveryReportsEnabled(enabled: Boolean) {
        userPreferences.deliveryReports = enabled
        _uiState.value = _uiState.value.copy(deliveryReports = enabled)
    }

    fun setSendDelaySeconds(seconds: Int) {
        userPreferences.sendDelaySeconds = seconds
        _uiState.value = _uiState.value.copy(sendDelaySeconds = seconds)
    }

    fun setAutoDeleteLimit(limit: Int) {
        userPreferences.autoDeleteLimit = limit
        _uiState.value = _uiState.value.copy(autoDeleteLimit = limit)
    }

    fun setAppLanguage(language: AppLanguage) {
        userPreferences.appLanguage = language
        _uiState.value = _uiState.value.copy(appLanguage = language)
    }

    fun setLockScreenPrivacy(privacy: LockScreenPrivacy) {
        userPreferences.lockScreenPrivacy = privacy
        _uiState.value = _uiState.value.copy(lockScreenPrivacy = privacy)
    }

    fun checkDefaultSmsStatus() {
        _uiState.value = _uiState.value.copy(isDefaultSmsApp = repository.isDefaultSmsApp())
    }

    fun refreshDefaultSmsStatus() {
        defaultSmsCheckJob?.cancel()
        checkDefaultSmsStatus()
        if (_uiState.value.isDefaultSmsApp) return
        defaultSmsCheckJob = viewModelScope.launch {
            repeat(10) {
                delay(250)
                checkDefaultSmsStatus()
                if (_uiState.value.isDefaultSmsApp) return@launch
            }
        }
    }

    fun enterSelectionMode(threadId: Long) {
        selectedThreadIds.clear()
        selectedThreadIds.add(threadId)
        isSelectionMode = true
    }

    fun toggleSelectThread(threadId: Long) {
        if (threadId in selectedThreadIds) {
            selectedThreadIds.remove(threadId)
        } else {
            selectedThreadIds.add(threadId)
        }
        isSelectionMode = selectedThreadIds.isNotEmpty()
    }

    fun selectAllThreads() {
        val allFilteredIds = _uiState.value.filteredThreads.map { it.threadId }
        val allSelected = selectedThreadIds.containsAll(allFilteredIds) && allFilteredIds.isNotEmpty()
        if (allSelected) {
            selectedThreadIds.clear()
            isSelectionMode = false
        } else {
            selectedThreadIds.clear()
            selectedThreadIds.addAll(allFilteredIds)
            isSelectionMode = true
        }
    }

    fun clearSelection() {
        selectedThreadIds.clear()
        isSelectionMode = false
    }

    fun deleteSelectedConversations(onComplete: (Int) -> Unit = {}) {
        val targetIds = selectedThreadIds.toSet()
        if (targetIds.isEmpty()) return

        viewModelScope.launch {
            val count = targetIds.size
            val success = repository.deleteThreads(targetIds)
            if (!success) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete conversations"
                )
                onComplete(0)
                return@launch
            }
            val updatedThreads = _uiState.value.threads.filter { it.threadId !in targetIds }
            val updatedFiltered = _uiState.value.filteredThreads.filter { it.threadId !in targetIds }
            selectedThreadIds.clear()
            isSelectionMode = false
            _uiState.value = _uiState.value.copy(
                threads = updatedThreads,
                filteredThreads = updatedFiltered,
                error = null
            )
            repository.saveCachedThreads(updatedThreads)
            onComplete(count)
        }
    }

    fun markSelectedConversationsRead(onComplete: (Int) -> Unit = {}) {
        val targetIds = selectedThreadIds.toSet()
        if (targetIds.isEmpty()) return

        viewModelScope.launch {
            val count = targetIds.size
            repository.markThreadsRead(targetIds)
            clearSelection()
            loadThreads(silent = true, force = true)
            onComplete(count)
        }
    }

    fun markAllConversationsRead(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.markAllMessagesRead()
            loadThreads(silent = true, force = true)
            onComplete()
        }
    }

    fun loadTabs() {
        viewModelScope.launch {
            val tabs = repository.getAllTabs()
            val current = _uiState.value
            val storedSelection = current.selectedTabId
            val storedStillExists = storedSelection != null && tabs.any { it.id == storedSelection }
            val validSelectedTabId = when {
                storedStillExists -> storedSelection
                // On the very first load the app opens on the first real tab instead of the All
                // pseudo tab, no matter where All currently sits in the strip.
                !initialTabApplied -> tabs.firstOrNull()?.id
                storedSelection == null -> null
                else -> tabs.firstOrNull()?.id
            }
            initialTabApplied = true
            _uiState.value = current.copy(
                tabs = tabs,
                selectedTabId = validSelectedTabId,
                filteredThreads = filterThreads(current.threads, current.searchQuery.trim(), validSelectedTabId, tabs)
            )
        }
    }

    fun selectTab(tabId: Long?) {
        val current = _uiState.value
        if (current.selectedTabId == tabId) return
        // The contacts-only filter is remembered, but filterThreads applies it to the All tab only.
        _uiState.value = current.copy(
            selectedTabId = tabId,
            filteredThreads = filterThreads(current.threads, current.searchQuery.trim(), tabId, current.tabs)
        )
    }

    fun createTab(
        name: String,
        addresses: Collection<String> = emptyList(),
        filterMask: Int = 0,
        onCreated: (Long) -> Unit = {}
    ) {
        viewModelScope.launch {
            val id = repository.createTab(name, addresses, filterMask)
            if (id != -1L) {
                selectTab(id)
                onCreated(id)
            }
        }
    }

    /**
     * Saves a tab edited in the unified editor: name, senders and the content filter mask.
     * Writes everything first and then reloads from the database, so the refreshed state can
     * never clobber the values that were just written.
     */
    fun saveTab(tabId: Long, name: String, addresses: Collection<String>, filterMask: Int) {
        viewModelScope.launch {
            repository.updateTabName(tabId, name)
            repository.setTabSenders(tabId, addresses)
            repository.updateTabFilterMask(tabId, filterMask)
            val tabs = repository.getAllTabs()
            val current = _uiState.value
            val selected = current.selectedTabId?.takeIf { id -> tabs.any { it.id == id } }
            _uiState.value = current.copy(
                tabs = tabs,
                selectedTabId = selected,
                filteredThreads = filterThreads(
                    threads = current.threads,
                    query = current.searchQuery.trim(),
                    selectedTabId = selected,
                    tabs = tabs,
                    selectedSimFilterSubId = current.selectedSimFilterSubId
                )
            )
        }
    }

    /** Turns a content filter flag on or off for [tabId] and re-applies the inbox filter. */
    fun setTabFilterFlag(tabId: Long, mask: Int, enabled: Boolean) {
        val current = _uiState.value
        val tab = current.tabs.find { it.id == tabId } ?: return
        val updatedTab = tab.withFlag(mask, enabled)
        val updatedTabs = current.tabs.map { if (it.id == tabId) updatedTab else it }
        _uiState.value = current.copy(
            tabs = updatedTabs,
            filteredThreads = filterThreads(
                threads = current.threads,
                query = current.searchQuery.trim(),
                selectedTabId = current.selectedTabId,
                tabs = updatedTabs,
                selectedSimFilterSubId = current.selectedSimFilterSubId
            )
        )
        viewModelScope.launch {
            repository.updateTabFilterMask(tabId, updatedTab.filterMask)
            // Re-read from the database so the UI matches what was actually persisted.
            val tabs = repository.getAllTabs()
            val fresh = tabs.find { it.id == tabId }
            if (fresh != null && fresh.filterMask != updatedTab.filterMask) {
                val current = _uiState.value
                val revertedTabs = current.tabs.map { if (it.id == tabId) fresh else it }
                _uiState.value = current.copy(
                    tabs = revertedTabs,
                    filteredThreads = filterThreads(
                        threads = current.threads,
                        query = current.searchQuery.trim(),
                        selectedTabId = current.selectedTabId,
                        tabs = revertedTabs,
                        selectedSimFilterSubId = current.selectedSimFilterSubId
                    )
                )
            }
        }
    }

    fun renameTab(tabId: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        val current = _uiState.value
        // Update the in-memory list first so the tab strip shows the new name without a reload.
        _uiState.value = current.copy(
            tabs = current.tabs.map { if (it.id == tabId) it.copy(name = trimmed) else it }
        )
        viewModelScope.launch {
            repository.updateTabName(tabId, trimmed)
        }
    }

    /**
     * Persists a new tab order. [orderedIds] may contain [ALL_TAB_SENTINEL], which moves the
     * "All" pseudo tab; only real tabs are written to the database.
     */
    fun reorderTabs(orderedIds: List<Long>) {
        val current = _uiState.value
        val allIndex = orderedIds.indexOf(ALL_TAB_SENTINEL).takeIf { it >= 0 } ?: current.allTabIndex
        val realIds = orderedIds.filter { it != ALL_TAB_SENTINEL }
        val byId = current.tabs.associateBy { it.id }
        val reordered = realIds.mapNotNull { byId[it] }
        if (reordered.size != current.tabs.size) return
        userPreferences.allTabIndex = allIndex
        _uiState.value = current.copy(tabs = reordered, allTabIndex = allIndex)
        viewModelScope.launch {
            repository.updateTabOrder(realIds)
        }
    }

    /**
     * Hides one conversation right away and deletes it after the undo window, so the user can
     * still take the deletion back without losing messages.
     */
    fun requestDeleteConversation(threadId: Long) {
        val current = _uiState.value
        if (threadId in current.pendingDeleteIds) return
        _uiState.value = current.copy(pendingDeleteIds = current.pendingDeleteIds + threadId)
        pendingDeletes.remove(threadId)?.cancel()
        pendingDeletes[threadId] = viewModelScope.launch {
            delay(UNDO_DELETE_WINDOW_MILLIS)
            _uiState.value = _uiState.value.copy(
                pendingDeleteIds = _uiState.value.pendingDeleteIds - threadId
            )
            pendingDeletes.remove(threadId)
            selectedThreadIds.remove(threadId)
            if (selectedThreadIds.isEmpty()) isSelectionMode = false
            val deleted = repository.deleteThread(threadId)
            if (deleted) {
                loadThreads(silent = true, force = true)
            } else {
                // The provider refuses writes when Ramesh is not the default SMS app; tell the UI
                // instead of silently putting the conversation back.
                _uiState.value = _uiState.value.copy(deleteFailureTick = System.currentTimeMillis())
            }
        }
    }

    /** Cancels a queued deletion and brings the conversation back. */
    fun undoDeleteConversation(threadId: Long) {
        pendingDeletes.remove(threadId)?.cancel()
        val current = _uiState.value
        if (threadId in current.pendingDeleteIds) {
            _uiState.value = current.copy(pendingDeleteIds = current.pendingDeleteIds - threadId)
        }
    }

    fun deleteTab(tabId: Long) {
        viewModelScope.launch {
            if (_uiState.value.selectedTabId == tabId) {
                selectTab(null)
            }
            repository.deleteTab(tabId)
        }
    }

    fun addSelectedToTab(tabId: Long, onComplete: (Int) -> Unit = {}) {
        val targetIds = selectedThreadIds.toSet()
        if (targetIds.isEmpty()) return
        val addresses = _uiState.value.threads
            .filter { it.threadId in targetIds }
            .map { it.address }
        viewModelScope.launch {
            repository.addSendersToTab(tabId, addresses)
            clearSelection()
            onComplete(addresses.size)
        }
    }

    fun addSelectedToTabs(tabIds: Collection<Long>, onComplete: (Int) -> Unit = {}) {
        val targetIds = selectedThreadIds.toSet()
        if (targetIds.isEmpty()) return
        val addresses = _uiState.value.threads
            .filter { it.threadId in targetIds }
            .map { it.address }
        viewModelScope.launch {
            for (tabId in tabIds) {
                repository.addSendersToTab(tabId, addresses)
            }
            clearSelection()
            onComplete(addresses.size)
        }
    }

    fun createTabWithSelected(name: String, onCreated: (Long) -> Unit = {}) {
        val targetIds = selectedThreadIds.toSet()
        val addresses = _uiState.value.threads
            .filter { it.threadId in targetIds }
            .map { it.address }
        viewModelScope.launch {
            val id = repository.createTab(name, addresses)
            clearSelection()
            if (id != -1L) {
                selectTab(id)
                onCreated(id)
            }
        }
    }

    fun setTabSenders(tabId: Long, addresses: Collection<String>) {
        viewModelScope.launch {
            repository.setTabSenders(tabId, addresses)
        }
    }
}

private const val SMS_OBSERVER_DEBOUNCE_MS = 400L
private const val SEARCH_MIN_QUERY_LENGTH = 2
private const val SMS_PERMISSION_ERROR = "SMS permission required"
