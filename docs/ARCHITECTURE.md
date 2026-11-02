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

`SmsRepository.loadThreads()` is an orchestrator over small readers: `readThreadRows`,
`readUnreadSnapshot`, `readLatestInboxMessages`, `readThreadSubIds` and `readInboxRowsForThreads`.
The number of provider queries per refresh is fixed - threads, unread inbox, latest inbox, SIM ids,
plus one chunked query for the spam-only candidates - instead of growing with the number of
conversations. The inbox maths itself (`buildUnreadSnapshot`, `latestInboxByThread`,
`spamOnlyThreads` in `InboxSnapshot.kt`) and the per-message verdict
(`InboxActionEvaluator`) are plain functions over plain rows, so they are unit-tested on the JVM.

Three rules keep that path honest:

* **A read never writes.** Verdicts computed during the read are returned as
  `ThreadsLoadResult.pendingSpamMarks`; the view model persists them *after* publishing the list.
  Writing mid-read used to emit `spamMetaChanged`, which bounced back into the inbox and cancelled
  the refresh that produced it.
* **A refresh is never cancelled.** `RefreshScheduler` runs one refresh at a time and coalesces
  requests that arrive during a run into a single follow-up pass.
* **A failed read is not an empty inbox.** When the provider cursor cannot be opened, the loader
  returns the cached snapshot and leaves the cache alone; only a successful read that really
  contains no conversations clears it.

`spam_message_meta` is a cache of the filter verdict, not a source of truth: a blocked sender
always wins over a stored verdict (`resolveInboxAction`), and editing a rule or a sender preference
drops the affected verdicts so the next read re-evaluates.

## The message action bar

A single tap on a bubble opens `MessageActionBar`: one horizontal strip floating over the top of the
chat - the Textra layout - carrying copy, react, share, forward, delete, info and mark-as-read in a
single row. Nothing hides behind a "More" submenu, so every action is one tap away, and the strip
sits above the message rather than over it, so the bubble it acts on stays readable. Copy is the one
action with a text label; the rest lean on their icons and on the content description read by
TalkBack. When a verification code was detected, `findOtpHighlight` provides a chip on the left of
the strip that copies the code itself.

Holding a message still enters selection mode, so the two gestures never compete. Full-text
selection lives in the long-press "Message options" dialog, which keeps the body in a
`SelectionContainer` - that is how a *part* of a long message is copied, since selecting inside the
list would fight the scrolling and the tap gesture.

What is tappable in a message body is decided by `MessageLinkScanner`, a pure function with its own
unit tests. URLs are collected first and their ranges are excluded from the phone and date passes,
so `https://example.com/1404/03/23` is one link and not a link plus a date.

The strip is stateless: every action arrives as a lambda, so it holds no view-model or repository
reference and can be reasoned about, and previewed, on its own.

## Dates and calendars

Two stored preferences can ask for Jalali dates - the `JalaliScope` picked in Settings and the older
`showShamsiDate` flag that tapping a date inside a chat toggles. `showsJalaliDates(scope, flag)` is
the only place that combines them, and every screen calls it, so the inbox and the chat cannot
disagree about the calendar any more (they did: the list only looked at the scope).

`AppLanguage.isPersian()` resolves `SYSTEM` against the device locale. Date formatting used to
compare against `PERSIAN` directly, so a Persian phone on "System default" was formatted as English.

Only the digits and month names follow the language; the *calendar* is a separate decision.

`SmsDateFormats` builds its `DateTimeFormatter`s **per locale**, in a
`ConcurrentHashMap<Locale, Formats>`, from `localeFor(appLanguage)`: Persian always maps to
`fa-IR`, English to `en`, and `SYSTEM` to the device locale. Before this, the formatters were built
once at class-load time with `Locale.getDefault()` - the *device* language - so an English phone
running the Persian UI got English weekday and month names. The damage looked random because
`conversationList` picks a different pattern per row age: `HH:mm` for today, `EEE` for this week,
`MMM d`/Jalali names beyond. One list could therefore show `۱۹:۳۳` next to `Sep 14`, and which rows
disagreed depended on how old each message was - the reason the tabs appeared to use different
formats. Every entry point now takes `appLanguage`, and the `remember` keys in `MessageBubble`,
`SpamMessagePill` and the inbox rows include it, so a language change repaints instead of reusing a
stale string.

## Storage

| Store | Contents |
| --- | --- |
| Telephony provider | the messages themselves (shared with every SMS app) |
| `ramesh_filters.db` v7 | `filter_rules`, `predefined_rule_settings`, `sender_preferences`, `spam_message_meta`, `cached_threads`, `sender_tabs` + `sender_tab_members`, `message_reactions` |
| `ramesh_prefs` | 15 behavioural settings (tabs, SIM badge, delivery reports, send delay, auto-delete, lock-screen privacy, language, calendar scope, …) |
| `ramesh_appearance` | 19 look-and-feel settings (`Appearance` / `AppearanceStore`) |

`AppearanceStore` serialises enums by `Enum.name`, so a renamed constant would silently reset a
user's choice. `readEnum` therefore maps legacy names (`BubbleStyle.TEXTRA` → `CLASSIC`, renamed in
1.0.0) instead of falling back to the default.

`cached_threads` is a snapshot that lets the inbox paint immediately on launch. On every successful
provider read it is updated as a **diff**: conversations that disappeared are deleted, and only the
rows whose stored values differ are written (`cachedThreadValues` / `CACHED_THREAD_COLUMNS` are
shared by the comparison and the upsert so a new column cannot be written without being compared).
Rewriting every row on every refresh - including the ones that had not changed at all - was pure
I/O on the hottest path in the app.

Schema steps run through `RameshDatabaseHelper.migrate(step)`, which logs a failure with the step
name. They used to be wrapped in `catch (e: Exception) {}`: a half-applied migration stayed silent
until a SELECT crashed with "no such column" long after the upgrade.

**The "message animation" setting is a navigation transition.** It decides how a conversation (or
any other screen) enters and leaves - slide, fade, pop or none - through
`conversationEnterTransition`/`conversationExitTransition`, which `NavDisplay` uses for both the
push and the pop transition and which mirror themselves for RTL. A bubble that arrives while the
conversation is already open keeps a short fixed entrance of its own.

**Reactions arrive as SMS.** Clients with no reaction protocol send the reaction as text
(`Reacted 👍 to "see you at 8"`). `SmsReactionParser` normalises the body first (Persian SMS carry
invisible RLM/LRM marks and keyboards emit non-breaking spaces, which made an anchored pattern fail)
and then looks for a reaction word, an emoji and a quoted text. `foldSmsReactions` moves the emoji
onto the closest earlier message whose text matches the quote, falling back to the newest earlier
message when the quote no longer matches, and removes the extra SMS from the conversation. Folding
runs on the *merged* list in `ChatScreen`, so paging or a refresh can never split a reaction from
its target. A reaction with nothing to react to stays a normal message, and a reaction picked inside
Ramesh is never overwritten by a network one.

**Silent spam is counted, never notified.** Spam is stored read, so it can never appear in a
thread's `unreadCount`. A spam arrival instead sets `spam_message_meta.is_new`, and
`cached_threads.new_spam_count` carries that number into `ConversationThread.newSpamCount`. The tab
badges use `badgeCountForTab` (`unreadCount + newSpamCount`) so an arrival the user was never
notified about is still visible, including spam-only conversations the inbox itself hides.
`markNewSpamSeen` clears the flag when the conversation is opened.

## Localisation

Every user-facing string lives in `res/values/strings*.xml` with a matching
`res/values-fa/strings*.xml`. Both languages are kept at parity (354 keys each); Persian is applied
by `localizedContext` on the activity base context, never by swapping the composition context.
Message bodies are laid out with `contentAware` so Persian text reads right-to-left even in the
English UI.

## Tests

`app/src/test` holds JVM unit tests for the pure logic: filter engine, OTP detector, Persian
number/phone canonicalisation, tab matching, date formats, send tracker, inbox action resolution,
the refresh scheduler, the inbox snapshot maths (`InboxSnapshotTest`) and the per-message verdicts
(`InboxActionEvaluatorTest`), the bubble's link/OTP scanner (`MessageLinkScannerTest`) and the
calendar/language rules (`DateCalendarTest`). `RameshDatabaseHelperCacheTest` runs under Robolectric against a real
SQLite database and asserts the cache diff with SQLite triggers, so "unchanged row was not rewritten"
is proven by the absence of a write rather than by comparing values. `TestLabScreen` (debug builds only) injects fake SMS through the real
receiver for manual end-to-end checks.
