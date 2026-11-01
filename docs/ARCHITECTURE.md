# Ramesh architecture

Ramesh is a single-activity Jetpack Compose app (one `MainActivity`, Navigation3 back stack) that
acts as an Android SMS client when the user makes it the default SMS app.

```
MainActivity (applies the chosen locale in attachBaseContext)
└── MainNavigation (Navigation3 back stack)
    ├── ConversationsNav ── ConversationsViewModel ─┐
    ├── ChatNav ─────────── ChatViewModel           │
    ├── SettingsNav ─────── ConversationsViewModel  ├── SmsRepository ── Telephony provider
    ├── FilterStudioNav ─── FilterStudioViewModel ──┤                    (Sms / Threads / Contacts)
    └── TestLabNav (debug only) ────────────────────┘   ├── RameshDatabaseHelper (SQLite v7)
                                                        ├── FilterRulesCache
                                                        └── SimRepository
engine/    SmsFilterEngine · OtpDetector · NotificationHelper
           IncomingSmsPolicy · IncomingMultipartAssembler
receiver/  SmsReceiver · MmsReceiver (stub) · SmsSentReceiver · NotificationActionReceiver
service/   HeadlessSmsSendService (respond-via-message)
```

## Incoming SMS

1. `SmsReceiver` receives `SMS_DELIVER` (as default app) or `SMS_RECEIVED` (fallback, ignored while
   we are the default app so a message is never stored twice).
2. `IncomingMultipartAssembler` joins the parts of one message by sender.
3. `IncomingSmsPolicy.shouldKeepInInbox` drops Class-0, Type-0, status reports and MWI messages.
4. `SmsFilterEngine.evaluateMessage` decides `NORMAL` / `SILENT` / `SPAM`.
5. As the default app the message is written to `Telephony.Sms.Inbox`; the verdict is stored in
   `spam_message_meta`.
6. `NotificationHelper` notifies: `SPAM` on the silent channel, everything else on the alert channel
   with inline reply, mark-as-read, optional delete and a one-tap copy-code action for OTPs.
7. `pruneThread` applies the per-conversation message cap, then
   `ACTION_NEW_SMS_RECEIVED` tells a visible inbox to reload.

## Outgoing SMS

`ChatViewModel.sendMessage` → optional send-delay countdown (cancellable) →
`SmsRepository.sendSms`:

* the SIM is resolved from the sender preference, then from the last received message;
* the row is inserted into `Telephony.Sms.Sent` with `STATUS_PENDING` *before* dispatch so every
  part can be tracked;
* per-part `PendingIntent`s feed `SmsSentReceiver`, and `SmsSendTracker` only reports success when
  every part was accepted by the radio (20 s ack timeout);
* delivery reports (when enabled) update `STATUS_COMPLETE` / `STATUS_FAILED`.

## Filter hierarchy

Order matters and is the promise the app makes in its README: an allowlisted code always wins.

1. sender allowlist → 2. global allowlist → 3. blocked sender → 4. "no spam for contacts" →
5. sender blocklist → 6. user global blocklist → 7. built-in Iranian presets →
8. sender default action → `NORMAL`.

`FilterRulesCache` keeps the prepared rule set and sender preferences in memory and invalidates them
through `rulesChanged` / `prefsChanged`. Patterns are compiled once and cached, and message bodies
are normalised (`ي`→`ی`, `ك`→`ک`, ZWNJ, kashida, diacritics) before matching.

## Inbox read path

`SmsRepository.loadThreads()` reads threads, unread counts, latest messages and SIM ids from the
provider, then decides an action per thread. Two rules keep that path honest:

* **A read never writes.** Verdicts computed during the read are returned as
  `ThreadsLoadResult.pendingSpamMarks`; the view model persists them *after* publishing the list.
  Writing mid-read used to emit `spamMetaChanged`, which bounced back into the inbox and cancelled
  the refresh that produced it.
* **A refresh is never cancelled.** `RefreshScheduler` runs one refresh at a time and coalesces
  requests that arrive during a run into a single follow-up pass.

`spam_message_meta` is a cache of the filter verdict, not a source of truth: a blocked sender
always wins over a stored verdict (`resolveInboxAction`), and editing a rule or a sender preference
drops the affected verdicts so the next read re-evaluates.

## Storage

| Store | Contents |
| --- | --- |
| Telephony provider | the messages themselves (shared with every SMS app) |
| `ramesh_filters.db` v7 | `filter_rules`, `predefined_rule_settings`, `sender_preferences`, `spam_message_meta`, `cached_threads`, `sender_tabs` + `sender_tab_members`, `message_reactions` |
| `ramesh_prefs` | 14 behavioural settings (tabs, SIM badge, delivery reports, send delay, auto-delete, lock-screen privacy, language, calendar scope, …) |
| `ramesh_appearance` | 19 look-and-feel settings (`Appearance` / `AppearanceStore`) |

`cached_threads` is a snapshot that lets the inbox paint immediately on launch; it is replaced on
every successful provider read.

**Silent spam is counted, never notified.** Spam is stored read, so it can never appear in a
thread's `unreadCount`. A spam arrival instead sets `spam_message_meta.is_new`, and
`cached_threads.new_spam_count` carries that number into `ConversationThread.newSpamCount`. The tab
badges use `badgeCountForTab` (`unreadCount + newSpamCount`) so an arrival the user was never
notified about is still visible, including spam-only conversations the inbox itself hides.
`markNewSpamSeen` clears the flag when the conversation is opened.

## Localisation

Every user-facing string lives in `res/values/strings*.xml` with a matching
`res/values-fa/strings*.xml`. Both languages are kept at parity (352 keys each); Persian is applied
by `localizedContext` on the activity base context, never by swapping the composition context.
Message bodies are laid out with `contentAware` so Persian text reads right-to-left even in the
English UI.

## Tests

`app/src/test` holds JVM unit tests for the pure logic: filter engine, OTP detector, Persian
number/phone canonicalisation, tab matching, date formats, send tracker, inbox action resolution and
the refresh scheduler. `TestLabScreen` (debug builds only) injects fake SMS through the real
receiver for manual end-to-end checks.
