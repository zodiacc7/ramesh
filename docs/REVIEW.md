# Code review — 1.0.0 / 1.0.1

A full pass over the source tree (81 Kotlin files, ~22.8k lines) together with the state of the
data layer, the receivers, the Compose UI, the build and CI. Items are ordered by risk.

## Fixed in 1.0.0

| Issue | Cause | Fix |
| --- | --- | --- |
| A new SMS only appeared after restarting the app | `SmsRepository.getThreads()` wrote filter verdicts *inside* the read. The write emitted `spamMetaChanged`, which called `loadThreads()` again, which cancelled the in-flight `loadJob` — so the list the read had just built was discarded | reads return `ThreadsLoadResult.pendingSpamMarks`; the view model saves them after publishing. `RefreshScheduler` queues refreshes instead of cancelling them |
| Marking a conversation as spam did not move it to the spam tab | the verdict cached in `spam_message_meta` was reused forever, so re-evaluation with the new sender preference never happened | `saveSenderPreference` drops that sender's verdicts; `resolveInboxAction` lets a blocked sender win over a stored verdict |
| Editing or deleting a filter rule left stale results | same, for global rules | `onRulesMutated()` clears cached verdicts before notifying listeners |
| Returning from a chat or Settings showed a stale inbox | single-activity navigation never fires `ON_RESUME`, so the only refresh path was the observers | `MainNavigation` reloads whenever `ConversationsNav` is on top |
| `:app:compileDebugKotlin` failed on `main` (beta.15) | `ACTION_NEW_SMS_RECEIVED` was declared at file scope but referenced as `SmsReceiver.…`; `LocalContext.current` was used inside `DisposableEffect`; `Context` was not imported | constant moved into the existing companion object, context captured before the effect, import added, `registerReceiver` switched to `ContextCompat.registerReceiver(…, RECEIVER_NOT_EXPORTED)` for API 34+ |
| Release-only breakage was invisible until a tag was pushed | CI built debug only | `assembleRelease` (R8) added to CI |
| The release signing key and its passwords were committed to a public repository | `keystore/ramesh-release.p12` + `keystore/keystore.properties` were tracked | both removed, ignored by git, and supplied to the release workflow from GitHub Actions secrets (`SIGNING_KEYSTORE`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`) |
| Play Protect blocked installs as a harmful app | releases were signed with a key that had been public since `35d9657` | key rotated on 2026-11-01; `AllowedAPKSigningKeys` updated, rotation logged in `keystore/README.md`, install/verification guidance added to the README |

## Fixed in 1.0.1 (pre-release)

| Issue | Cause | Fix |
| --- | --- | --- |
| `SmsRepository.loadThreads()` was a ~290-line function with an N+1 query: `threadHasOnlySpam()` re-queried every message of every thread whose latest message was spam | the spam-only check ran `THREAD_ID = ?` inside the per-thread loop | `readInboxRowsForThreads()` reads all candidates in one chunked query and `spamOnlyThreads()` decides from those rows; the number of provider queries per refresh no longer depends on the inbox size |
| `replaceCachedThreads()` deleted and rewrote every row on every refresh | the cache was written as a full replacement | written as a diff (`cachedThreadValues` / `CACHED_THREAD_COLUMNS` shared by the comparison and the upsert): removed conversations are deleted, unchanged rows are skipped |
| A failed provider read wiped the conversation cache | the empty result of a failed read was written like a real empty inbox | the loader returns the cached snapshot and leaves the cache alone; only a successful read that truly has no conversations clears it |
| 13 `onUpgrade` branches and two `onOpen` retries swallowed exceptions | `catch (e: Exception) {}` around every schema step | one `migrate(step)` helper logs the failing step name; a half-applied migration is now visible in logcat |
| Nothing tested the repository, the inbox maths or the database | only pure helpers had tests | `InboxSnapshotTest`, `InboxActionEvaluatorTest` and the Robolectric-backed `RameshDatabaseHelperCacheTest` cover the read path |

## Open — priority order

### P1 — the leaked key still exists in history

The old key is retired and `1.0.0` is signed with a fresh one, but the old material is still in the
git history (from `35d9657` to the pre-release builds of 2026-11-01). Anyone can sign a build with it; such a build
just cannot claim to be ours any more, because the current certificate
(`C1:7B:EA:…:A1:AC`) is published in `keystore/README.md` and in the README install notes. Optional
cleanup: the pre-rotation releases were deleted from GitHub when `1.0.0` was published, so only the
git history keeps the old material.

Distribution outside Play Store remains the reason Play Protect still warns about unknown
developers. Publishing through Play (even internal testing) or F-Droid is the durable fix.

### P1 — performance

* `resolveContactName()` is not `suspend` and runs without `Dispatchers.IO`, yet `ComposeScreen`
  calls it from the main thread while the user types a recipient.

### P1 — robustness

* `SmsSendTracker` leaks an entry when the radio never acknowledges (20 s timeout); clean it up in a
  `finally`.
* `deleteThreads()` drops the thread record even when the message delete failed (`smsOk` is computed
  and ignored).
* `MmsReceiver` is an empty stub: MMS arriving while Ramesh is the default app is silently dropped.

### P2 — quality

* The inbox read path and the database cache are covered now, but the receivers, the view models
  and the Compose UI still have no automated tests, and the CI job does not run `androidTest`.
* Polling-style duplication: `sms:/smsto:` parsing exists twice (`MainActivity` and
  `HeadlessSmsSendService`, 11 duplicated blocks); `TabDialogs` / `TabEditorDialogs` keep two
  parallel copies of the same dialog.
* Enum labels (`ThemeMode`, `MessageAnimation`, `ChatFontFamily`, …) are hard-coded English, so the
  Persian UI shows English option names.
* One hard-coded English toast in `ComposeScreen`.
* `allowBackup="true"` covers the filter database (per-sender metadata) in cloud backups.
* `ComposeScreen`'s send path navigates to the chat even when the message could not be stored,
  showing a toast on top of an empty conversation.

## Verified working

Allowlist-first filtering with the built-in Iranian presets and Persian text normalisation, dual-SIM
badge/filter/per-SIM send, delivery reports, send delay, per-conversation auto-delete, emoji
reactions, combinable tab filters, the full appearance system (theme, accent, bubbles, fonts,
wallpaper, message animation), lock-screen privacy, message search across all threads, per-sender
settings, and EN/FA parity for all 354 strings.
