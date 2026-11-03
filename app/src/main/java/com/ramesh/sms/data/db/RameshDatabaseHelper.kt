package com.ramesh.sms.data.db

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import android.telephony.SubscriptionManager
import com.ramesh.sms.data.model.ConversationThread
import com.ramesh.sms.data.model.FilterAction
import com.ramesh.sms.data.model.FilterRule
import com.ramesh.sms.data.model.PredefinedRules
import com.ramesh.sms.data.model.PreferredSimMode
import com.ramesh.sms.data.model.RuleCategory
import com.ramesh.sms.data.model.SenderPreference
import com.ramesh.sms.data.model.SenderTab
import com.ramesh.sms.data.util.PhoneNumberKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class RameshDatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    private val _rulesChanged = MutableStateFlow(System.currentTimeMillis())
    val rulesChanged: Flow<Long> = _rulesChanged.asStateFlow()

    private val _prefsChanged = MutableStateFlow(System.currentTimeMillis())
    val prefsChanged: Flow<Long> = _prefsChanged.asStateFlow()

    private val _spamMetaChanged = MutableStateFlow(System.currentTimeMillis())
    val spamMetaChanged: Flow<Long> = _spamMetaChanged.asStateFlow()

    private val _tabsChanged = MutableStateFlow(System.currentTimeMillis())
    val tabsChanged: Flow<Long> = _tabsChanged.asStateFlow()

    @Volatile
    private var predefinedRulesSeeded = false

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS filter_rules (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                pattern TEXT NOT NULL,
                is_regex INTEGER NOT NULL DEFAULT 1,
                action TEXT NOT NULL DEFAULT 'SPAM',
                list_type TEXT NOT NULL DEFAULT 'BLOCKLIST',
                is_enabled INTEGER NOT NULL DEFAULT 1,
                is_predefined INTEGER NOT NULL DEFAULT 0,
                category TEXT NOT NULL DEFAULT 'CUSTOM',
                sender_target TEXT,
                description TEXT,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sender_preferences (
                address TEXT PRIMARY KEY,
                display_name TEXT,
                default_action TEXT NOT NULL DEFAULT 'NORMAL',
                custom_sound_uri TEXT,
                is_blocked INTEGER NOT NULL DEFAULT 0,
                notes TEXT,
                updated_at INTEGER NOT NULL,
                preferred_sub_id INTEGER NOT NULL DEFAULT -1
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS spam_message_meta (
                message_id INTEGER PRIMARY KEY,
                address TEXT NOT NULL,
                matched_rule_name TEXT,
                is_revealed INTEGER NOT NULL DEFAULT 0,
                is_spam INTEGER NOT NULL DEFAULT 1,
                action TEXT NOT NULL DEFAULT 'SPAM',
                is_new INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS predefined_rule_settings (
                rule_id INTEGER PRIMARY KEY,
                name TEXT,
                pattern TEXT,
                is_regex INTEGER,
                action TEXT NOT NULL,
                list_type TEXT NOT NULL DEFAULT 'BLOCKLIST',
                is_enabled INTEGER NOT NULL,
                is_deleted INTEGER NOT NULL DEFAULT 0,
                description TEXT,
                is_customized INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )

        createCachedThreadsTable(db)
        createSenderTabsTables(db)
        createMessageReactionsTable(db)
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_spam_meta_address ON spam_message_meta(address)")
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_filter_rules_sender_target ON filter_rules(sender_target)"
        )

        seedPredefinedRules(db)
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        setWriteAheadLoggingEnabled(true)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        // Defensive: older builds created sender_tabs without the filter mask, and a DB that was
        // already stamped with the new version would never run the migration. Adding the column
        // here is idempotent (a duplicate column is expected and ignored by [migrate]).
        migrate("sender_tabs.filter_mask") {
            db.execSQL("ALTER TABLE sender_tabs ADD COLUMN filter_mask INTEGER NOT NULL DEFAULT 0")
        }
        // On an up-to-date database the column is already there; that is expected and quiet.
        migrate("spam_message_meta.is_new") {
            db.execSQL("ALTER TABLE spam_message_meta ADD COLUMN is_new INTEGER NOT NULL DEFAULT 0")
        }
        migrate("cached_threads.new_spam_count") {
            db.execSQL("ALTER TABLE cached_threads ADD COLUMN new_spam_count INTEGER NOT NULL DEFAULT 0")
        }
        if (predefinedRulesSeeded) return
        seedPredefinedRules(db)
        predefinedRulesSeeded = true
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            migrate("filter_rules.list_type") {
                db.execSQL("ALTER TABLE filter_rules ADD COLUMN list_type TEXT DEFAULT 'BLOCKLIST'")
            }
            migrate("predefined_rule_settings.name") {
                db.execSQL("ALTER TABLE predefined_rule_settings ADD COLUMN name TEXT")
            }
            migrate("predefined_rule_settings.pattern") {
                db.execSQL("ALTER TABLE predefined_rule_settings ADD COLUMN pattern TEXT")
            }
            migrate("predefined_rule_settings.is_regex") {
                db.execSQL("ALTER TABLE predefined_rule_settings ADD COLUMN is_regex INTEGER DEFAULT 1")
            }
            migrate("predefined_rule_settings.list_type") {
                db.execSQL("ALTER TABLE predefined_rule_settings ADD COLUMN list_type TEXT DEFAULT 'BLOCKLIST'")
            }
            migrate("predefined_rule_settings.is_deleted") {
                db.execSQL("ALTER TABLE predefined_rule_settings ADD COLUMN is_deleted INTEGER DEFAULT 0")
            }
            migrate("predefined_rule_settings.description") {
                db.execSQL("ALTER TABLE predefined_rule_settings ADD COLUMN description TEXT")
            }
            migrate("predefined_rule_settings.is_customized") {
                db.execSQL("ALTER TABLE predefined_rule_settings ADD COLUMN is_customized INTEGER DEFAULT 0")
            }
            createCachedThreadsTable(db)
            migrate("cached_threads.last_message_action") {
                db.execSQL("ALTER TABLE cached_threads ADD COLUMN last_message_action TEXT DEFAULT 'NORMAL'")
            }
            migrate("index idx_spam_meta_address") {
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_spam_meta_address ON spam_message_meta(address)")
            }
        }
        if (oldVersion < 3) {
            migrate("index idx_filter_rules_sender_target") {
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_filter_rules_sender_target ON filter_rules(sender_target)")
            }
        }
        if (oldVersion < 4) {
            migrate("createSenderTabsTables") { createSenderTabsTables(db) }
        }
        if (oldVersion < 5) {
            migrate("sender_preferences.preferred_sub_id") {
                db.execSQL("ALTER TABLE sender_preferences ADD COLUMN preferred_sub_id INTEGER NOT NULL DEFAULT -1")
            }
            migrate("cached_threads.sub_id") {
                db.execSQL("ALTER TABLE cached_threads ADD COLUMN sub_id INTEGER NOT NULL DEFAULT -1")
            }
        }
        if (oldVersion < 6) {
            migrate("createMessageReactionsTable") { createMessageReactionsTable(db) }
            migrate("sender_tabs.filter_mask") {
                db.execSQL("ALTER TABLE sender_tabs ADD COLUMN filter_mask INTEGER NOT NULL DEFAULT 0")
            }
        }
        if (oldVersion < 7) {
            // Spam is delivered silently, but it still has to be countable: every spam verdict
            // carries a "not looked at yet" flag that the tab badges read and the inbox clears.
            migrate("spam_message_meta.is_new") {
                db.execSQL("ALTER TABLE spam_message_meta ADD COLUMN is_new INTEGER NOT NULL DEFAULT 0")
            }
            migrate("cached_threads.new_spam_count") {
                db.execSQL("ALTER TABLE cached_threads ADD COLUMN new_spam_count INTEGER NOT NULL DEFAULT 0")
            }
        }
    }

    /**
     * Runs one schema step of [onUpgrade] or [onOpen].
     *
     * Steps used to be wrapped in `catch (e: Exception) {}`: a migration that failed left the
     * database half upgraded and the failure only surfaced later, as a "no such column" crash in a
     * SELECT. Two outcomes are now distinguished: a step that finds its column already present is
     * the normal state of an up-to-date database and stays quiet, everything else is logged with the
     * step name so a damaged schema is visible in logcat instead of silent.
     */
    private fun migrate(step: String, action: () -> Unit) {
        try {
            action()
        } catch (e: SQLiteException) {
            if (e.message?.contains("duplicate column", ignoreCase = true) != true) {
                Log.w(TAG, "Schema migration step '$step' failed", e)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Schema migration step '$step' failed", e)
        }
    }

    // --- Filter Rules Operations ---

    suspend fun getAllRules(): List<FilterRule> = withContext(Dispatchers.IO) {
        val rules = mutableListOf<FilterRule>()

        // 1. Get predefined rules with overridden states
        val predefined = PredefinedRules.getDefaultRules().toMutableList()
        val db = readableDatabase
        val overrides = mutableMapOf<Long, FilterRuleOverride>()

        try {
            val cursor = db.query(
                "predefined_rule_settings",
                arrayOf("rule_id", "is_enabled", "action", "name", "pattern", "is_regex", "list_type", "is_deleted", "description"),
                null, null, null, null, null
            )
            cursor.use {
                val idIdx = it.getColumnIndex("rule_id")
                val enabledIdx = it.getColumnIndex("is_enabled")
                val actionIdx = it.getColumnIndex("action")
                val nameIdx = it.getColumnIndex("name")
                val patternIdx = it.getColumnIndex("pattern")
                val isRegexIdx = it.getColumnIndex("is_regex")
                val listTypeIdx = it.getColumnIndex("list_type")
                val isDeletedIdx = it.getColumnIndex("is_deleted")
                val descIdx = it.getColumnIndex("description")

                while (it.moveToNext()) {
                    val rId = it.getLong(idIdx)
                    val enabled = it.getInt(enabledIdx) == 1
                    val act = try { FilterAction.valueOf(it.getString(actionIdx)) } catch (e: Exception) { FilterAction.SPAM }
                    val name = if (nameIdx >= 0) it.getString(nameIdx) else null
                    val pattern = if (patternIdx >= 0) it.getString(patternIdx) else null
                    val isRegex = if (isRegexIdx >= 0 && !it.isNull(isRegexIdx)) it.getInt(isRegexIdx) == 1 else true
                    val listType = if (listTypeIdx >= 0 && !it.isNull(listTypeIdx)) {
                        try { com.ramesh.sms.data.model.RuleListType.valueOf(it.getString(listTypeIdx)) } catch (e: Exception) { null }
                    } else null
                    val isDeleted = if (isDeletedIdx >= 0 && !it.isNull(isDeletedIdx)) it.getInt(isDeletedIdx) == 1 else false
                    val desc = if (descIdx >= 0) it.getString(descIdx) else null

                    overrides[rId] = FilterRuleOverride(
                        isEnabled = enabled,
                        action = act,
                        name = name,
                        pattern = pattern,
                        isRegex = isRegex,
                        listType = listType,
                        isDeleted = isDeleted,
                        description = desc
                    )
                }
            }
        } catch (e: Exception) {
            // Fallback for earlier database tables
        }

        predefined.forEach { rule ->
            val ov = overrides[rule.id]
            if (ov != null) {
                if (!ov.isDeleted) {
                    rules.add(
                        rule.copy(
                            name = ov.name ?: rule.name,
                            pattern = ov.pattern ?: rule.pattern,
                            isRegex = ov.isRegex,
                            isEnabled = ov.isEnabled,
                            action = ov.action,
                            listType = ov.listType ?: rule.listType,
                            description = ov.description ?: rule.description
                        )
                    )
                }
            } else {
                rules.add(rule)
            }
        }

        // 2. Get custom user rules
        val userCursor = db.query(
            "filter_rules",
            null,
            null, null, null, null,
            "created_at DESC"
        )
        userCursor.use {
            val indices = FilterRuleIndices(it)
            while (it.moveToNext()) {
                rules.add(readFilterRule(it, indices))
            }
        }

        rules
    }

    suspend fun getSenderRules(address: String): List<FilterRule> = withContext(Dispatchers.IO) {
        val variants = addressLookupKeys(address)
        if (variants.isEmpty()) return@withContext emptyList()
        val rules = mutableListOf<FilterRule>()
        val placeholders = variants.joinToString(",") { "?" }
        val cursor = readableDatabase.query(
            "filter_rules",
            null,
            "sender_target IN ($placeholders)",
            variants,
            null, null,
            "created_at DESC"
        )
        cursor.use {
            val indices = FilterRuleIndices(it)
            while (it.moveToNext()) {
                rules.add(readFilterRule(it, indices))
            }
        }
        rules
    }

    /**
     * Drops every cached filter verdict and tells the UI that the rules changed.
     *
     * Verdicts are stored per message with the rule that produced them. Once a rule is added,
     * edited, disabled or removed they can no longer be trusted: a conversation that a deleted rule
     * marked as spam would otherwise stay in the spam tab until the app restarted. Re-evaluating is
     * cheap because only messages without a verdict are run through the engine again.
     */
    private fun onRulesMutated() {
        try {
            writableDatabase.delete("spam_message_meta", null, null)
        } catch (e: Exception) {
            Log.w(TAG, "Could not invalidate cached filter verdicts", e)
        }
        _rulesChanged.value = System.currentTimeMillis()
    }

    suspend fun insertCustomRule(rule: FilterRule): Long = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("name", rule.name)
            put("pattern", rule.pattern)
            put("is_regex", if (rule.isRegex) 1 else 0)
            put("action", rule.action.name)
            put("list_type", rule.listType.name)
            put("is_enabled", if (rule.isEnabled) 1 else 0)
            put("is_predefined", 0)
            put("category", rule.category.name)
            put("sender_target", rule.senderTarget?.let { normalizeAddress(it) })
            put("description", rule.description)
            put("created_at", System.currentTimeMillis())
        }
        val id = writableDatabase.insert("filter_rules", null, cv)
        onRulesMutated()
        id
    }

    suspend fun updateRule(rule: FilterRule): Boolean = withContext(Dispatchers.IO) {
        if (rule.isPredefined || rule.id < 0) {
            val cv = ContentValues().apply {
                put("rule_id", rule.id)
                put("name", rule.name)
                put("pattern", rule.pattern)
                put("is_regex", if (rule.isRegex) 1 else 0)
                put("action", rule.action.name)
                put("list_type", rule.listType.name)
                put("is_enabled", if (rule.isEnabled) 1 else 0)
                put("is_deleted", 0)
                put("description", rule.description)
                put("is_customized", 1)
            }
            val rows = writableDatabase.insertWithOnConflict(
                "predefined_rule_settings",
                null,
                cv,
                SQLiteDatabase.CONFLICT_REPLACE
            )
            onRulesMutated()
            rows > 0
        } else {
            val cv = ContentValues().apply {
                put("name", rule.name)
                put("pattern", rule.pattern)
                put("is_regex", if (rule.isRegex) 1 else 0)
                put("action", rule.action.name)
                put("list_type", rule.listType.name)
                put("is_enabled", if (rule.isEnabled) 1 else 0)
                put("category", rule.category.name)
                put("sender_target", rule.senderTarget?.let { normalizeAddress(it) })
                put("description", rule.description)
            }
            val rows = writableDatabase.update(
                "filter_rules",
                cv,
                "id = ?",
                arrayOf(rule.id.toString())
            )
            onRulesMutated()
            rows > 0
        }
    }

    suspend fun deleteRule(ruleId: Long): Boolean = withContext(Dispatchers.IO) {
        val db = writableDatabase
        if (ruleId < 0) {
            val flags = ContentValues().apply {
                put("is_deleted", 1)
                put("is_enabled", 0)
            }
            val ok = updateOrInsertPredefinedSettings(db, ruleId, flags)
            onRulesMutated()
            ok
        } else {
            val rows = db.delete("filter_rules", "id = ?", arrayOf(ruleId.toString()))
            onRulesMutated()
            rows > 0
        }
    }

    suspend fun setRuleEnabled(ruleId: Long, isEnabled: Boolean, isPredefined: Boolean) = withContext(Dispatchers.IO) {
        if (isPredefined || ruleId < 0) {
            val flags = ContentValues().apply {
                put("is_enabled", if (isEnabled) 1 else 0)
            }
            updateOrInsertPredefinedSettings(writableDatabase, ruleId, flags)
        } else {
            val cv = ContentValues().apply {
                put("is_enabled", if (isEnabled) 1 else 0)
            }
            writableDatabase.update("filter_rules", cv, "id = ?", arrayOf(ruleId.toString()))
        }
        onRulesMutated()
    }

    // --- Sender Preferences Operations ---

    suspend fun getSenderPreference(address: String): SenderPreference? = withContext(Dispatchers.IO) {
        val variants = addressLookupKeys(address)
        val placeholders = variants.joinToString(",") { "?" }
        val cursor = readableDatabase.query(
            "sender_preferences",
            null,
            "address IN ($placeholders)",
            variants,
            null, null,
            "updated_at DESC"
        )
        cursor.use {
            if (it.moveToFirst()) {
                readSenderPreference(it, SenderPreferenceIndices(it))
            } else {
                null
            }
        }
    }

    suspend fun getAllSenderPreferences(): Map<String, SenderPreference> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<String, SenderPreference>()
        val cursor = readableDatabase.query(
            "sender_preferences",
            null,
            null, null, null, null, null
        )
        cursor.use {
            val indices = SenderPreferenceIndices(it)
            while (it.moveToNext()) {
                val pref = readSenderPreference(it, indices)
                val key = PhoneNumberKeys.canonical(pref.address)
                val existing = result[key]
                if (existing == null || pref.updatedAt >= existing.updatedAt) {
                    result[key] = pref
                }
            }
        }
        result
    }

    suspend fun saveSenderPreference(pref: SenderPreference) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("address", normalizeAddress(pref.address))
            put("display_name", pref.displayName)
            put("default_action", pref.defaultAction.name)
            put("custom_sound_uri", pref.customSoundUri)
            put("is_blocked", if (pref.isBlocked) 1 else 0)
            put("notes", pref.notes)
            put("updated_at", System.currentTimeMillis())
            put("preferred_sub_id", pref.preferredSubId)
        }
        writableDatabase.insertWithOnConflict(
            "sender_preferences",
            null,
            cv,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        // The verdicts stored for this sender were computed with the *old* preference. Blocking a
        // sender (or changing its tier) must win immediately, so drop them and let the next read
        // re-evaluate. Without this a conversation keeps its stale NORMAL/SILENT verdict and
        // never moves to the spam tab until the app restarts.
        deleteSpamMetaForAddressLocked(pref.address)
        _prefsChanged.value = System.currentTimeMillis()
    }

    /** Blocking variant of [deleteSpamMetaForAddress], for callers already inside an IO block. */
    private fun deleteSpamMetaForAddressLocked(address: String) {
        if (address.isBlank()) return
        val variants = addressLookupKeys(address)
        if (variants.isEmpty()) return
        val placeholders = variants.joinToString(",") { "?" }
        try {
            writableDatabase.delete("spam_message_meta", "address IN ($placeholders)", variants)
        } catch (e: Exception) {
            Log.w(TAG, "Could not drop spam verdicts for a stored sender", e)
        }
    }

    suspend fun updateSenderPreferredSubId(address: String, preferredSubId: Int) = withContext(Dispatchers.IO) {
        val existing = getSenderPreference(address)
        val updated = existing?.copy(preferredSubId = preferredSubId, updatedAt = System.currentTimeMillis())
            ?: SenderPreference(
                address = address,
                preferredSubId = preferredSubId,
                updatedAt = System.currentTimeMillis()
            )
        saveSenderPreference(updated)
    }

    // --- Spam Metadata Operations ---

    suspend fun saveMessageFilterMeta(
        messageId: Long,
        address: String,
        matchedRuleName: String?,
        action: FilterAction
    ) = withContext(Dispatchers.IO) {
        saveMessagesFilterMeta(
            listOf(
                SpamMetaWrite(
                    messageId = messageId,
                    address = address,
                    matchedRuleName = matchedRuleName,
                    action = action
                )
            )
        )
    }

    /**
     * Stores filter verdicts for a batch of messages.
     *
     * @param notify whether `spamMetaChanged` should fire. Reads pass `false`: the caller already
     *   used these verdicts to build the list it is about to publish, and notifying here would
     *   schedule a second, identical pass.
     */
    suspend fun saveMessagesFilterMeta(
        entries: List<SpamMetaWrite>,
        notify: Boolean = true,
        /** False when the verdict is being refreshed for an old message rather than a new arrival. */
        countAsNewSpam: Boolean = false
    ) = withContext(Dispatchers.IO) {
        if (entries.isEmpty()) return@withContext
        val db = writableDatabase
        val now = System.currentTimeMillis()
        db.beginTransaction()
        try {
            for (entry in entries) {
                val cv = ContentValues().apply {
                    put("message_id", entry.messageId)
                    put("address", normalizeAddress(entry.address))
                    put("matched_rule_name", entry.matchedRuleName)
                    put("is_revealed", 0)
                    put("is_spam", if (entry.action == FilterAction.SPAM) 1 else 0)
                    put("action", entry.action.name)
                    // Spam never reaches the notification shade, so the unread badges would stay
                    // at zero. This flag is what the tab badges count.
                    put("is_new", if (countAsNewSpam && entry.action == FilterAction.SPAM) 1 else 0)
                    put("created_at", now)
                }
                db.insertWithOnConflict(
                    "spam_message_meta",
                    null,
                    cv,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        if (notify) _spamMetaChanged.value = now
    }

    suspend fun getAllReactions(): Map<Long, String> = withContext(Dispatchers.IO) {
        val reactions = HashMap<Long, String>()
        try {
            readableDatabase.query(
                "message_reactions",
                arrayOf("message_id", "emoji"),
                null,
                null,
                null,
                null,
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    reactions[cursor.getLong(0)] = cursor.getString(1)
                }
            }
        } catch (e: Exception) {
            Log.w("RameshDatabaseHelper", "Could not read message reactions", e)
        }
        reactions
    }

    suspend fun setReaction(messageId: Long, emoji: String?) = withContext(Dispatchers.IO) {
        try {
            if (emoji.isNullOrBlank()) {
                writableDatabase.delete(
                    "message_reactions",
                    "message_id = ?",
                    arrayOf(messageId.toString())
                )
            } else {
                val cv = ContentValues().apply {
                    put("message_id", messageId)
                    put("emoji", emoji)
                    put("created_at", System.currentTimeMillis())
                }
                writableDatabase.insertWithOnConflict(
                    "message_reactions",
                    null,
                    cv,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
        } catch (e: Exception) {
            Log.w("RameshDatabaseHelper", "Could not store reaction for message $messageId", e)
        }
    }

    suspend fun setMessageRevealed(messageId: Long, isRevealed: Boolean) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("is_revealed", if (isRevealed) 1 else 0)
        }
        writableDatabase.update("spam_message_meta", cv, "message_id = ?", arrayOf(messageId.toString()))
        _spamMetaChanged.value = System.currentTimeMillis()
    }

    suspend fun unmarkSpam(messageId: Long) = withContext(Dispatchers.IO) {
        val cv = ContentValues().apply {
            put("is_spam", 0)
            put("action", FilterAction.NORMAL.name)
            put("is_revealed", 1)
        }
        writableDatabase.update("spam_message_meta", cv, "message_id = ?", arrayOf(messageId.toString()))
        _spamMetaChanged.value = System.currentTimeMillis()
    }

    suspend fun getSpamMetaMapForThread(address: String): Map<Long, SpamMessageMeta> =
        withContext(Dispatchers.IO) {
            val result = mutableMapOf<Long, SpamMessageMeta>()
            val variants = addressLookupKeys(address)
            val placeholders = variants.joinToString(",") { "?" }
            val cursor = readableDatabase.query(
                "spam_message_meta",
                arrayOf("message_id", "is_spam", "matched_rule_name", "is_revealed", "action", "is_new"),
                "address IN ($placeholders)",
                variants,
                null, null, null
            )
            cursor.use {
                while (it.moveToNext()) {
                    result[it.getLong(0)] = cursorToSpamMessageMeta(it)
                }
            }
            result
        }

    suspend fun getAllSpamMetaMap(): Map<Long, SpamMessageMeta> = withContext(Dispatchers.IO) {
        val result = mutableMapOf<Long, SpamMessageMeta>()
        val cursor = readableDatabase.query(
            "spam_message_meta",
            arrayOf("message_id", "is_spam", "matched_rule_name", "is_revealed", "action", "is_new"),
            null, null, null, null, null
        )
        cursor.use {
            while (it.moveToNext()) {
                result[it.getLong(0)] = cursorToSpamMessageMeta(it)
            }
        }
        result
    }

    suspend fun getSpamMetaMapForIds(messageIds: Collection<Long>): Map<Long, SpamMessageMeta> =
        withContext(Dispatchers.IO) {
            if (messageIds.isEmpty()) return@withContext emptyMap()
            val result = mutableMapOf<Long, SpamMessageMeta>()
            val db = readableDatabase
            for (chunk in messageIds.distinct().chunked(SQLITE_IN_CHUNK_SIZE)) {
                val placeholders = chunk.joinToString(",") { "?" }
                val args = chunk.map { it.toString() }.toTypedArray()
                val cursor = db.query(
                    "spam_message_meta",
                    arrayOf("message_id", "is_spam", "matched_rule_name", "is_revealed", "action", "is_new"),
                    "message_id IN ($placeholders)",
                    args,
                    null, null, null
                )
                cursor.use {
                    while (it.moveToNext()) {
                        result[it.getLong(0)] = cursorToSpamMessageMeta(it)
                    }
                }
            }
            result
        }

    suspend fun deleteSpamMetaForMessageIds(messageIds: Collection<Long>) = withContext(Dispatchers.IO) {
        if (messageIds.isEmpty()) return@withContext
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (chunk in messageIds.distinct().chunked(SQLITE_IN_CHUNK_SIZE)) {
                val placeholders = chunk.joinToString(",") { "?" }
                val args = chunk.map { it.toString() }.toTypedArray()
                db.delete("spam_message_meta", "message_id IN ($placeholders)", args)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _spamMetaChanged.value = System.currentTimeMillis()
    }

    suspend fun deleteSpamMetaForAddress(address: String) = withContext(Dispatchers.IO) {
        if (address.isBlank()) return@withContext
        val variants = addressLookupKeys(address)
        if (variants.isEmpty()) return@withContext
        val placeholders = variants.joinToString(",") { "?" }
        writableDatabase.delete("spam_message_meta", "address IN ($placeholders)", variants)
        _spamMetaChanged.value = System.currentTimeMillis()
    }

    /**
     * Spam messages per sender that the user has not looked at yet.
     *
     * Spam is stored read (it never notifies), so it has no unread count of its own. These numbers
     * are what the inbox tab badges add on top of [ConversationThread.unreadCount].
     */
    suspend fun getNewSpamCounts(): Map<String, Int> = withContext(Dispatchers.IO) {
        val counts = mutableMapOf<String, Int>()
        try {
            readableDatabase.query(
                "spam_message_meta",
                arrayOf("address", "COUNT(*) AS new_count"),
                "is_new = 1 AND is_spam = 1",
                null,
                "address",
                null,
                null
            ).use { cursor ->
                val addressIdx = cursor.getColumnIndexOrThrow("address")
                val countIdx = cursor.getColumnIndexOrThrow("new_count")
                while (cursor.moveToNext()) {
                    val address = cursor.getString(addressIdx) ?: continue
                    val key = PhoneNumberKeys.canonical(address)
                    counts[key] = (counts[key] ?: 0) + cursor.getInt(countIdx)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not count new spam", e)
        }
        counts
    }

    /** Marks every spam message from [address] as seen, so the badges stop counting it. */
    suspend fun markNewSpamSeen(address: String) = withContext(Dispatchers.IO) {
        if (address.isBlank()) return@withContext
        val variants = addressLookupKeys(address)
        if (variants.isEmpty()) return@withContext
        val placeholders = variants.joinToString(",") { "?" }
        val values = ContentValues().apply { put("is_new", 0) }
        val updated = try {
            writableDatabase.update(
                "spam_message_meta",
                values,
                "is_new = 1 AND address IN ($placeholders)",
                variants
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not clear the new-spam flag", e)
            0
        }
        if (updated > 0) _spamMetaChanged.value = System.currentTimeMillis()
    }

    suspend fun getCachedThreadAddresses(threadIds: Collection<Long>): Map<Long, String> =
        withContext(Dispatchers.IO) {
            if (threadIds.isEmpty()) return@withContext emptyMap()
            val result = mutableMapOf<Long, String>()
            val db = readableDatabase
            for (chunk in threadIds.distinct().chunked(SQLITE_IN_CHUNK_SIZE)) {
                val placeholders = chunk.joinToString(",") { "?" }
                val args = chunk.map { it.toString() }.toTypedArray()
                val cursor = db.query(
                    "cached_threads",
                    arrayOf("thread_id", "address"),
                    "thread_id IN ($placeholders)",
                    args,
                    null, null, null
                )
                cursor.use {
                    val idIdx = it.getColumnIndexOrThrow("thread_id")
                    val addrIdx = it.getColumnIndexOrThrow("address")
                    while (it.moveToNext()) {
                        result[it.getLong(idIdx)] = it.getString(addrIdx) ?: ""
                    }
                }
            }
            result
        }

    // --- Cached conversation list ---

    suspend fun getCachedThreads(): List<ConversationThread> = withContext(Dispatchers.IO) {
        val threads = mutableListOf<ConversationThread>()
        val cursor = readableDatabase.query(
            "cached_threads",
            null,
            null, null, null, null,
            "date DESC"
        )
        cursor.use {
            val idIdx = it.getColumnIndexOrThrow("thread_id")
            val addrIdx = it.getColumnIndexOrThrow("address")
            val nameIdx = it.getColumnIndexOrThrow("contact_name")
            val snippetIdx = it.getColumnIndexOrThrow("snippet")
            val dateIdx = it.getColumnIndexOrThrow("date")
            val countIdx = it.getColumnIndexOrThrow("message_count")
            val unreadIdx = it.getColumnIndexOrThrow("unread_count")
            val spamIdx = it.getColumnIndexOrThrow("is_unread_spam")
            val hasSpamIdx = it.getColumnIndexOrThrow("has_spam")
            val actionIdx = it.getColumnIndex("last_message_action")
            val subIdIdx = it.getColumnIndex("sub_id")
            val newSpamIdx = it.getColumnIndex("new_spam_count")
            while (it.moveToNext()) {
                val contactName = if (it.isNull(nameIdx)) null else it.getString(nameIdx)
                val lastMessageAction = if (actionIdx >= 0 && !it.isNull(actionIdx)) {
                    try {
                        FilterAction.valueOf(it.getString(actionIdx))
                    } catch (e: Exception) {
                        FilterAction.NORMAL
                    }
                } else {
                    FilterAction.NORMAL
                }
                val subId = if (subIdIdx >= 0 && !it.isNull(subIdIdx)) {
                    it.getInt(subIdIdx)
                } else {
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID
                }
                threads.add(
                    ConversationThread(
                        threadId = it.getLong(idIdx),
                        address = it.getString(addrIdx) ?: "",
                        contactName = contactName?.ifBlank { null },
                        snippet = it.getString(snippetIdx) ?: "",
                        date = it.getLong(dateIdx),
                        messageCount = it.getInt(countIdx),
                        unreadCount = it.getInt(unreadIdx),
                        hasSpam = it.getInt(hasSpamIdx) == 1,
                        isUnreadSpam = it.getInt(spamIdx) == 1,
                        lastMessageAction = lastMessageAction,
                        subId = subId,
                        newSpamCount = if (newSpamIdx >= 0) it.getInt(newSpamIdx) else 0
                    )
                )
            }
        }
        threads
    }

    /**
     * Replaces the cached snapshot with [threads].
     *
     * This is a diff, not a rewrite: only rows whose stored values actually changed are written and
     * conversations that disappeared are deleted. The inbox writes this snapshot on every refresh
     * (each incoming SMS, each return from a chat), so rewriting every column of every row for a
     * single new message was wasted I/O on the main refresh path.
     */
    suspend fun replaceCachedThreads(threads: List<ConversationThread>) = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            val keepIds = threads.map { it.threadId }.distinct()
            if (keepIds.isEmpty()) {
                db.delete("cached_threads", null, null)
            } else if (keepIds.size <= SQLITE_IN_CHUNK_SIZE) {
                val placeholders = keepIds.joinToString(",") { "?" }
                val args = keepIds.map { it.toString() }.toTypedArray()
                db.delete("cached_threads", "thread_id NOT IN ($placeholders)", args)
            } else {
                val keepSet = keepIds.toHashSet()
                val toDelete = mutableListOf<Long>()
                db.query(
                    "cached_threads",
                    arrayOf("thread_id"),
                    null, null, null, null, null
                ).use { cursor ->
                    val idIdx = cursor.getColumnIndexOrThrow("thread_id")
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idIdx)
                        if (id !in keepSet) toDelete.add(id)
                    }
                }
                for (chunk in toDelete.chunked(SQLITE_IN_CHUNK_SIZE)) {
                    val placeholders = chunk.joinToString(",") { "?" }
                    val args = chunk.map { it.toString() }.toTypedArray()
                    db.delete("cached_threads", "thread_id IN ($placeholders)", args)
                }
            }

            if (threads.isNotEmpty()) {
                val stored = HashMap<Long, List<String?>>(threads.size)
                db.query("cached_threads", CACHED_THREAD_COLUMNS, null, null, null, null, null).use { cursor ->
                    while (cursor.moveToNext()) {
                        stored[cursor.getLong(0)] = CACHED_THREAD_COLUMNS.indices.map { index ->
                            if (cursor.isNull(index)) null else cursor.getString(index)
                        }
                    }
                }

                for (thread in threads) {
                    val values = cachedThreadValues(thread)
                    val signature = CACHED_THREAD_COLUMNS.map { values.getAsString(it) }
                    if (stored[thread.threadId] == signature) continue
                    db.insertWithOnConflict(
                        "cached_threads",
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_REPLACE
                    )
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    // --- Sender Tabs Operations ---

    suspend fun getAllTabs(): List<SenderTab> = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val membersMap = mutableMapOf<Long, MutableSet<String>>()
        try {
            db.query(
                "sender_tab_members",
                arrayOf("tab_id", "address"),
                null, null, null, null, null
            ).use { cursor ->
                val tabIdIdx = cursor.getColumnIndexOrThrow("tab_id")
                val addrIdx = cursor.getColumnIndexOrThrow("address")
                while (cursor.moveToNext()) {
                    val tabId = cursor.getLong(tabIdIdx)
                    val addr = cursor.getString(addrIdx)
                    membersMap.getOrPut(tabId) { mutableSetOf() }.add(addr)
                }
            }
        } catch (e: Exception) {
            // Table might not exist in early migration
        }

        val tabs = mutableListOf<SenderTab>()
        val readTabs: (Array<String>) -> Unit = { projection ->
            db.query(
                "sender_tabs",
                projection,
                null, null, null, null,
                "sort_order ASC, id ASC"
            ).use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow("id")
                val nameIdx = cursor.getColumnIndexOrThrow("name")
                val sortIdx = cursor.getColumnIndexOrThrow("sort_order")
                val createdIdx = cursor.getColumnIndexOrThrow("created_at")
                val maskIdx = cursor.getColumnIndex("filter_mask")
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIdx)
                    val name = cursor.getString(nameIdx)
                    val sortOrder = cursor.getInt(sortIdx)
                    val createdAt = cursor.getLong(createdIdx)
                    val mask = if (maskIdx >= 0 && !cursor.isNull(maskIdx)) cursor.getInt(maskIdx) else 0
                    tabs.add(
                        SenderTab(
                            id = id,
                            name = name,
                            sortOrder = sortOrder,
                            createdAt = createdAt,
                            senderAddresses = membersMap[id] ?: emptySet(),
                            filterMask = mask
                        )
                    )
                }
            }
        }
        try {
            readTabs(arrayOf("id", "name", "sort_order", "created_at", "filter_mask"))
        } catch (_: Exception) {
            // Very old schema without filter_mask: read the tabs without it.
            tabs.clear()
            try {
                readTabs(arrayOf("id", "name", "sort_order", "created_at"))
            } catch (_: Exception) {
                // Table might not exist yet
            }
        }
        tabs
    }

    /**
     * Moves every existing tab [delta] places down the strip.
     *
     * Used once, when the built-in tabs are created in front of whatever tabs the user already
     * had: it keeps their order intact instead of interleaving the new tabs with theirs.
     */
    suspend fun shiftTabSortOrders(delta: Int): Boolean = withContext(Dispatchers.IO) {
        if (delta == 0) return@withContext true
        val db = writableDatabase
        db.execSQL("UPDATE sender_tabs SET sort_order = sort_order + ?", arrayOf<Any>(delta))
        _tabsChanged.value = System.currentTimeMillis()
        true
    }

    suspend fun createTab(
        name: String,
        addresses: Collection<String> = emptyList(),
        filterMask: Int = 0,
        /** Explicit position in the strip; null appends the tab at the end. */
        sortOrder: Int? = null
    ): Long =
        withContext(Dispatchers.IO) {
            val trimmed = name.trim()
            if (trimmed.isBlank()) return@withContext -1L
            val db = writableDatabase
            val now = System.currentTimeMillis()
            var tabId = -1L
            db.beginTransaction()
            try {
                var nextOrder = sortOrder ?: 0
                if (sortOrder == null) {
                    db.rawQuery("SELECT MAX(sort_order) FROM sender_tabs", null).use { cursor ->
                        if (cursor.moveToFirst() && !cursor.isNull(0)) {
                            nextOrder = cursor.getInt(0) + 1
                        }
                    }
                }
                val cv = ContentValues().apply {
                    put("name", trimmed)
                    put("created_at", now)
                    put("sort_order", nextOrder)
                    put("filter_mask", filterMask)
                }
                tabId = db.insert("sender_tabs", null, cv)
                if (tabId != -1L && addresses.isNotEmpty()) {
                    val canonicals = addresses.map { normalizeAddress(it) }.filter { it.isNotBlank() }.distinct()
                    for (addr in canonicals) {
                        val mcv = ContentValues().apply {
                            put("tab_id", tabId)
                            put("address", addr)
                        }
                        db.insertWithOnConflict("sender_tab_members", null, mcv, SQLiteDatabase.CONFLICT_IGNORE)
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            if (tabId != -1L) {
                _tabsChanged.value = System.currentTimeMillis()
            }
            tabId
        }

    suspend fun updateTabOrder(tabIds: List<Long>): Boolean = withContext(Dispatchers.IO) {
        if (tabIds.isEmpty()) return@withContext false
        val db = writableDatabase
        db.beginTransaction()
        try {
            val cv = ContentValues()
            tabIds.forEachIndexed { index, id ->
                cv.clear()
                cv.put("sort_order", index)
                db.update("sender_tabs", cv, "id = ?", arrayOf(id.toString()))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _tabsChanged.value = System.currentTimeMillis()
        true
    }

    suspend fun updateTabFilterMask(tabId: Long, filterMask: Int): Boolean = withContext(Dispatchers.IO) {
        val rows = writableDatabase.update(
            "sender_tabs",
            ContentValues().apply { put("filter_mask", filterMask) },
            "id = ?",
            arrayOf(tabId.toString())
        )
        if (rows > 0) {
            _tabsChanged.value = System.currentTimeMillis()
        }
        rows > 0
    }

    suspend fun updateTabName(tabId: Long, newName: String): Boolean = withContext(Dispatchers.IO) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return@withContext false
        val cv = ContentValues().apply {
            put("name", trimmed)
        }
        val count = writableDatabase.update("sender_tabs", cv, "id = ?", arrayOf(tabId.toString()))
        if (count > 0) {
            _tabsChanged.value = System.currentTimeMillis()
            true
        } else false
    }

    suspend fun deleteTab(tabId: Long): Boolean = withContext(Dispatchers.IO) {
        val db = writableDatabase
        db.beginTransaction()
        val count = try {
            db.delete("sender_tab_members", "tab_id = ?", arrayOf(tabId.toString()))
            val deleted = db.delete("sender_tabs", "id = ?", arrayOf(tabId.toString()))
            db.setTransactionSuccessful()
            deleted
        } finally {
            db.endTransaction()
        }
        if (count > 0) {
            _tabsChanged.value = System.currentTimeMillis()
            true
        } else false
    }

    suspend fun addSendersToTab(tabId: Long, addresses: Collection<String>): Boolean =
        withContext(Dispatchers.IO) {
            if (addresses.isEmpty()) return@withContext false
            val canonicals = addresses.map { normalizeAddress(it) }.filter { it.isNotBlank() }.distinct()
            if (canonicals.isEmpty()) return@withContext false
            val db = writableDatabase
            db.beginTransaction()
            try {
                for (addr in canonicals) {
                    val cv = ContentValues().apply {
                        put("tab_id", tabId)
                        put("address", addr)
                    }
                    db.insertWithOnConflict("sender_tab_members", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            _tabsChanged.value = System.currentTimeMillis()
            true
        }

    suspend fun removeSendersFromTab(tabId: Long, addresses: Collection<String>): Boolean =
        withContext(Dispatchers.IO) {
            if (addresses.isEmpty()) return@withContext false
            val canonicals = addresses.map { normalizeAddress(it) }.filter { it.isNotBlank() }.distinct()
            if (canonicals.isEmpty()) return@withContext false
            val db = writableDatabase
            db.beginTransaction()
            try {
                for (addr in canonicals) {
                    db.delete("sender_tab_members", "tab_id = ? AND address = ?", arrayOf(tabId.toString(), addr))
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            _tabsChanged.value = System.currentTimeMillis()
            true
        }

    suspend fun setTabSenders(tabId: Long, addresses: Collection<String>): Boolean =
        withContext(Dispatchers.IO) {
            val canonicals = addresses.map { normalizeAddress(it) }.filter { it.isNotBlank() }.distinct()
            val db = writableDatabase
            db.beginTransaction()
            try {
                db.delete("sender_tab_members", "tab_id = ?", arrayOf(tabId.toString()))
                for (addr in canonicals) {
                    val cv = ContentValues().apply {
                        put("tab_id", tabId)
                        put("address", addr)
                    }
                    db.insertWithOnConflict("sender_tab_members", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            _tabsChanged.value = System.currentTimeMillis()
            true
        }

    suspend fun getTabsForSender(address: String): List<SenderTab> = withContext(Dispatchers.IO) {
        val allTabs = getAllTabs()
        val canonical = normalizeAddress(address)
        val variants = addressLookupKeys(address).toSet()
        allTabs.filter { tab ->
            tab.senderAddresses.contains(canonical) || tab.senderAddresses.any { it in variants }
        }
    }

    suspend fun setSenderTabs(address: String, tabIds: Collection<Long>) = withContext(Dispatchers.IO) {
        val canonical = normalizeAddress(address)
        if (canonical.isBlank()) return@withContext
        val db = writableDatabase
        val allTabs = getAllTabs()
        val targetIds = tabIds.toSet()
        db.beginTransaction()
        try {
            for (tab in allTabs) {
                if (tab.id in targetIds) {
                    val cv = ContentValues().apply {
                        put("tab_id", tab.id)
                        put("address", canonical)
                    }
                    db.insertWithOnConflict("sender_tab_members", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
                } else {
                    db.delete(
                        "sender_tab_members",
                        "tab_id = ? AND address = ?",
                        arrayOf(tab.id.toString(), canonical)
                    )
                }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        _tabsChanged.value = System.currentTimeMillis()
    }

    private fun seedPredefinedRules(db: SQLiteDatabase) {
        PredefinedRules.getDefaultRules().forEach { rule ->
            val cv = ContentValues().apply {
                put("rule_id", rule.id)
                put("name", rule.name)
                put("pattern", rule.pattern)
                put("is_regex", if (rule.isRegex) 1 else 0)
                put("action", rule.action.name)
                put("list_type", rule.listType.name)
                put("is_enabled", if (rule.isEnabled) 1 else 0)
                put("is_deleted", 0)
                put("description", rule.description)
                put("is_customized", 0)
            }
            db.insertWithOnConflict("predefined_rule_settings", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
            val shipped = ContentValues().apply {
                put("name", rule.name)
                put("pattern", rule.pattern)
                put("is_regex", if (rule.isRegex) 1 else 0)
                put("list_type", rule.listType.name)
                put("description", rule.description)
            }
            db.update(
                "predefined_rule_settings",
                shipped,
                "rule_id = ? AND IFNULL(is_deleted, 0) = 0 AND IFNULL(is_customized, 0) = 0",
                arrayOf(rule.id.toString())
            )
        }
    }

    private fun createMessageReactionsTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS message_reactions (
                message_id INTEGER PRIMARY KEY,
                emoji TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    private fun createCachedThreadsTable(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS cached_threads (
                thread_id INTEGER PRIMARY KEY,
                address TEXT NOT NULL,
                contact_name TEXT,
                snippet TEXT,
                date INTEGER NOT NULL,
                message_count INTEGER NOT NULL DEFAULT 0,
                unread_count INTEGER NOT NULL DEFAULT 0,
                is_unread_spam INTEGER NOT NULL DEFAULT 0,
                has_spam INTEGER NOT NULL DEFAULT 0,
                last_message_action TEXT NOT NULL DEFAULT 'NORMAL',
                sub_id INTEGER NOT NULL DEFAULT -1,
                new_spam_count INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    private fun createSenderTabsTables(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sender_tabs (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                sort_order INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL,
                filter_mask INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS sender_tab_members (
                tab_id INTEGER NOT NULL,
                address TEXT NOT NULL,
                PRIMARY KEY (tab_id, address)
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sender_tab_members_address ON sender_tab_members(address)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_sender_tab_members_tab ON sender_tab_members(tab_id)")
    }

    private class SenderPreferenceIndices(cursor: Cursor) {
        val address = cursor.getColumnIndexOrThrow("address")
        val displayName = cursor.getColumnIndexOrThrow("display_name")
        val defaultAction = cursor.getColumnIndexOrThrow("default_action")
        val customSoundUri = cursor.getColumnIndexOrThrow("custom_sound_uri")
        val isBlocked = cursor.getColumnIndexOrThrow("is_blocked")
        val notes = cursor.getColumnIndexOrThrow("notes")
        val updatedAt = cursor.getColumnIndexOrThrow("updated_at")
        val preferredSubId = cursor.getColumnIndex("preferred_sub_id")
    }

    private fun readSenderPreference(cursor: Cursor, idx: SenderPreferenceIndices): SenderPreference {
        val preferredSubId = if (idx.preferredSubId >= 0 && !cursor.isNull(idx.preferredSubId)) {
            cursor.getInt(idx.preferredSubId)
        } else {
            PreferredSimMode.AUTO
        }
        return SenderPreference(
            address = cursor.getString(idx.address),
            displayName = cursor.getString(idx.displayName),
            defaultAction = try {
                FilterAction.valueOf(cursor.getString(idx.defaultAction))
            } catch (e: Exception) {
                FilterAction.NORMAL
            },
            customSoundUri = cursor.getString(idx.customSoundUri),
            isBlocked = cursor.getInt(idx.isBlocked) == 1,
            notes = cursor.getString(idx.notes) ?: "",
            updatedAt = cursor.getLong(idx.updatedAt),
            preferredSubId = preferredSubId
        )
    }

    private class FilterRuleIndices(cursor: Cursor) {
        val id = cursor.getColumnIndexOrThrow("id")
        val name = cursor.getColumnIndexOrThrow("name")
        val pattern = cursor.getColumnIndexOrThrow("pattern")
        val isRegex = cursor.getColumnIndexOrThrow("is_regex")
        val action = cursor.getColumnIndexOrThrow("action")
        val listType = cursor.getColumnIndex("list_type")
        val isEnabled = cursor.getColumnIndexOrThrow("is_enabled")
        val isPredefined = cursor.getColumnIndexOrThrow("is_predefined")
        val category = cursor.getColumnIndexOrThrow("category")
        val senderTarget = cursor.getColumnIndexOrThrow("sender_target")
        val description = cursor.getColumnIndexOrThrow("description")
        val createdAt = cursor.getColumnIndexOrThrow("created_at")
    }

    private fun readFilterRule(cursor: Cursor, idx: FilterRuleIndices): FilterRule {
        val categoryStr = cursor.getString(idx.category)
        val category = try {
            RuleCategory.valueOf(categoryStr)
        } catch (e: Exception) {
            RuleCategory.CUSTOM
        }

        val listType = if (idx.listType >= 0 && !cursor.isNull(idx.listType)) {
            try {
                com.ramesh.sms.data.model.RuleListType.valueOf(cursor.getString(idx.listType))
            } catch (e: Exception) {
                if (category == RuleCategory.OTP_ALLOWLIST || category == RuleCategory.CUSTOM_ALLOWLIST) {
                    com.ramesh.sms.data.model.RuleListType.ALLOWLIST
                } else {
                    com.ramesh.sms.data.model.RuleListType.BLOCKLIST
                }
            }
        } else {
            if (category == RuleCategory.OTP_ALLOWLIST || category == RuleCategory.CUSTOM_ALLOWLIST) {
                com.ramesh.sms.data.model.RuleListType.ALLOWLIST
            } else {
                com.ramesh.sms.data.model.RuleListType.BLOCKLIST
            }
        }

        return FilterRule(
            id = cursor.getLong(idx.id),
            name = cursor.getString(idx.name),
            pattern = cursor.getString(idx.pattern),
            isRegex = cursor.getInt(idx.isRegex) == 1,
            action = try {
                FilterAction.valueOf(cursor.getString(idx.action))
            } catch (e: Exception) {
                FilterAction.SPAM
            },
            listType = listType,
            isEnabled = cursor.getInt(idx.isEnabled) == 1,
            isPredefined = cursor.getInt(idx.isPredefined) == 1,
            category = category,
            senderTarget = cursor.getString(idx.senderTarget),
            description = cursor.getString(idx.description) ?: "",
            createdAt = cursor.getLong(idx.createdAt)
        )
    }

    fun normalizeAddress(address: String): String {
        return PhoneNumberKeys.canonical(address)
    }

    private fun addressLookupKeys(address: String): Array<String> {
        val keys = LinkedHashSet<String>()
        keys.add(normalizeAddress(address))
        keys.addAll(PhoneNumberKeys.keys(address))
        return keys.toTypedArray()
    }

    private fun updateOrInsertPredefinedSettings(
        db: SQLiteDatabase,
        ruleId: Long,
        flags: ContentValues
    ): Boolean {
        val updated = db.update(
            "predefined_rule_settings",
            flags,
            "rule_id = ?",
            arrayOf(ruleId.toString())
        )
        if (updated > 0) return true
        val defaults = PredefinedRules.getDefaultRules().find { it.id == ruleId } ?: return false
        val cv = ContentValues().apply {
            put("rule_id", defaults.id)
            put("name", defaults.name)
            put("pattern", defaults.pattern)
            put("is_regex", if (defaults.isRegex) 1 else 0)
            put("action", defaults.action.name)
            put("list_type", defaults.listType.name)
            put("is_enabled", if (defaults.isEnabled) 1 else 0)
            put("is_deleted", 0)
            put("description", defaults.description)
            put("is_customized", 0)
            putAll(flags)
        }
        return db.insert("predefined_rule_settings", null, cv) != -1L
    }

    private fun cursorToSpamMessageMeta(cursor: Cursor): SpamMessageMeta {
        val isSpam = cursor.getInt(1) == 1
        val rule = if (!cursor.isNull(2)) cursor.getString(2) else null
        val isRevealed = cursor.getInt(3) == 1
        val action = try {
            FilterAction.valueOf(cursor.getString(4))
        } catch (e: Exception) {
            if (isSpam) FilterAction.SPAM else FilterAction.NORMAL
        }
        // `is_new` is what the tab badge counts for silent spam; the same flag has to reach the
        // message so a row and a bubble can say *which* arrival that number is about.
        val isNew = cursor.getInt(5) == 1
        return SpamMessageMeta(
            isSpam = isSpam,
            matchedRuleName = rule,
            isRevealed = isRevealed,
            action = action,
            isNew = isNew
        )
    }

    companion object {
        private const val TAG = "RameshDatabaseHelper"
        private const val DATABASE_NAME = "ramesh_filters.db"
        private const val DATABASE_VERSION = 7
        private const val SQLITE_IN_CHUNK_SIZE = 500

        @Volatile
        private var INSTANCE: RameshDatabaseHelper? = null

        fun getInstance(context: Context): RameshDatabaseHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RameshDatabaseHelper(context).also { INSTANCE = it }
            }
        }
    }
}

/** Columns of `cached_threads`, in the order [cachedThreadValues] fills them. */
private val CACHED_THREAD_COLUMNS = arrayOf(
    "thread_id",
    "address",
    "contact_name",
    "snippet",
    "date",
    "message_count",
    "unread_count",
    "is_unread_spam",
    "has_spam",
    "last_message_action",
    "sub_id",
    "new_spam_count"
)

/**
 * The stored form of one cached conversation.
 *
 * Extracted so the "did anything change?" comparison and the upsert share one definition: a column
 * added to the table can no longer be written here while staying invisible to the diff.
 */
fun cachedThreadValues(thread: ConversationThread): ContentValues = ContentValues().apply {
    put("thread_id", thread.threadId)
    put("address", thread.address)
    put("contact_name", thread.contactName)
    put("snippet", thread.snippet)
    put("date", thread.date)
    put("message_count", thread.messageCount)
    put("unread_count", thread.unreadCount)
    put("is_unread_spam", if (thread.isUnreadSpam) 1 else 0)
    put("has_spam", if (thread.hasSpam) 1 else 0)
    put("last_message_action", thread.lastMessageAction.name)
    put("sub_id", thread.subId)
    put("new_spam_count", thread.newSpamCount)
}

data class SpamMetaWrite(
    val messageId: Long,
    val address: String,
    val matchedRuleName: String?,
    val action: FilterAction
)

data class SpamMessageMeta(
    val isSpam: Boolean,
    val matchedRuleName: String?,
    val isRevealed: Boolean,
    val action: FilterAction,
    /** True until the conversation is opened: drives the tab badge and the "new spam" marks. */
    val isNew: Boolean = false
)

data class FilterRuleOverride(
    val isEnabled: Boolean,
    val action: FilterAction,
    val name: String?,
    val pattern: String?,
    val isRegex: Boolean,
    val listType: com.ramesh.sms.data.model.RuleListType? = null,
    val isDeleted: Boolean,
    val description: String?
)

