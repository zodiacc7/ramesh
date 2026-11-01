# Code review — 1.1.0-beta.16

A full pass over the source tree (81 Kotlin files, ~22.8k lines) together with the state of the
data layer, the receivers, the Compose UI, the build and CI. Items are ordered by risk.

## Fixed in this release

| Issue | Cause | Fix |
| --- | --- | --- |
| A new SMS only appeared after restarting the app | `SmsRepository.getThreads()` wrote filter verdicts *inside* the read. The write emitted `spamMetaChanged`, which called `loadThreads()` again, which cancelled the in-flight `loadJob` — so the list the read had just built was discarded | reads return `ThreadsLoadResult.pendingSpamMarks`; the view model saves them after publishing. `RefreshScheduler` queues refreshes instead of cancelling them |
| Marking a conversation as spam did not move it to the spam tab | the verdict cached in `spam_message_meta` was reused forever, so re-evaluation with the new sender preference never happened | `saveSenderPreference` drops that sender's verdicts; `resolveInboxAction` lets a blocked sender win over a stored verdict |
| Editing or deleting a filter rule left stale results | same, for global rules | `onRulesMutated()` clears cached verdicts before notifying listeners |
| Returning from a chat or Settings showed a stale inbox | single-activity navigation never fires `ON_RESUME`, so the only refresh path was the observers | `MainNavigation` reloads whenever `ConversationsNav` is on top |
| `:app:compileDebugKotlin` failed on `main` (beta.15) | `ACTION_NEW_SMS_RECEIVED` was declared at file scope but referenced as `SmsReceiver.…`; `LocalContext.current` was used inside `DisposableEffect`; `Context` was not imported | constant moved into the existing companion object, context captured before the effect, import added, `registerReceiver` switched to `ContextCompat.registerReceiver(…, RECEIVER_NOT_EXPORTED)` for API 34+ |
| Release-only breakage was invisible until a tag was pushed | CI built debug only | `assembleRelease` (R8) added to CI |
| The release signing key and its passwords were committed to a public repository | `keystore/ramesh-release.p12` + `keystore/keystore.properties` were tracked | both removed, ignored by git, and supplied to the release workflow from GitHub Actions secrets (`SIGNING_KEYSTORE`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`) |

## Open — priority order

### P0 — rotate the release signing key

The key is out of the repository, but it is still in the git history (from `35d9657` up to
`1.1.0-beta.16`), so it must be treated as **publicly known**: anyone who can read the history can
sign an APK that Android accepts as an update of Ramesh. Removing the files was the first half; the
second half is rotating the key, which costs a one-time reinstall on Android 8.x and requires
updating `AllowedAPKSigningKeys` in `metadata/com.ramesh.sms.yml`. See `keystore/README.md`.

### P1 — performance

* `SmsRepository.loadThreads()` is ~290 lines and contains an N+1 query: `threadHasOnlySpam()`
  re-queries every message of a thread whose latest message is spam. Replacing it with one grouped
  query over `spam_message_meta` (or a `spam_only` column maintained on write) removes one query per
  promotional thread.
* `RameshDatabaseHelper.replaceCachedThreads()` rewrites every row on every refresh. Diffing against
  the previous snapshot, or dropping the cache in favour of the provider + verdict table, would cut
  writes on large inboxes.
* `resolveContactName()` is not `suspend` and runs without `Dispatchers.IO`, yet `ComposeScreen`
  calls it from the main thread while the user types a recipient.

### P1 — robustness

* All 13 branches of `RameshDatabaseHelper.onUpgrade` swallow exceptions, and `onOpen` retries an
  `ALTER TABLE` on every open. A failed migration currently leaves a half-built schema with no
  signal.
* `SmsSendTracker` leaks an entry when the radio never acknowledges (20 s timeout); clean it up in a
  `finally`.
* `deleteThreads()` drops the thread record even when the message delete failed (`smsOk` is computed
  and ignored).
* `MmsReceiver` is an empty stub: MMS arriving while Ramesh is the default app is silently dropped.

### P2 — quality

* No automated test covers the repository, the database, the receivers or the view models; the CI
  job runs unit tests only and does not run `androidTest`.
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
settings, and EN/FA parity for all 345 strings.
