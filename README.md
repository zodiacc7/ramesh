# رامش (Ramesh)

**SMS without the spam — and a look you actually like.**

[English](#english) · [فارسی](#farsi)

[![Version](https://img.shields.io/badge/version-1.0.1_(1011)-blue.svg)](https://github.com/zodiacc7/ramesh/releases)
[![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue.svg)](LICENSE)
[![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3ddc84.svg)](https://developer.android.com/about/versions/oreo)

---

<a id="english"></a>

## English

Ramesh is a full-featured Android SMS app built for Iran's messaging reality, with a look you can
customise down to the last detail.

Iranian SMS is mostly noise: ads, promotions and scams, mixed with the occasional
verification code you cannot afford to miss. Ramesh ships with ready-made regex filters
that silence the noise, while an allowlist makes sure OTPs and banking messages always
get through.

> Ramesh is an independent app published under the `com.ramesh.sms` package id as a free
> (GPL-3.0) open-source project.

**Latest stable: 1.0.0 (build 1000); 1.0.1 (build 1011) is a pre-release.** Release notes live on the
[releases page](https://github.com/zodiacc7/ramesh/releases) and in
[`fastlane/metadata/android/en-US/changelogs/`](fastlane/metadata/android/en-US/changelogs);
design notes are in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) and the open audit in
[docs/REVIEW.md](docs/REVIEW.md).

### Highlights

- **Allowlist-first filtering.** Allowlist rules run before the blocklist, so a promotional
  message that also carries a real login code still reaches you.
- **Built-in Iranian presets.** Spam, ads, phishing and scam patterns are included; keep them,
  disable them, edit them, or delete everything and start from your own list.
- **Filter Studio.** Add regex or plain-text rules, test a pattern against sample text, and
  export/import the whole rule set as JSON.
- **Deep appearance customisation.** Auto/Light/Dark/AMOLED-Black themes, accent colors, four
  bubble shapes with an optional tail, separate sent/received bubble colors, message fonts and
  sizes, six background tones or any gallery photo, and contact photos.
- **Dual SIM done properly.** Pick the SIM per message, see which SIM received a message, and
  filter the inbox by SIM.
- **Peaceful notifications, visible counts.** Filtered messages arrive silently and spam produces
  no notification at all, but a spam arrival still raises the badge on the **All** and **Spam**
  tabs until you open the conversation. Notifications support inline reply, mark-as-read, optional
  delete and a one-tap **Copy code** button for verification SMS.
- **No network permission.** Ramesh has no `INTERNET` permission: filtering, contacts and
  media decoding all happen on-device.
- **RTL-aware.** Persian and Arabic message bodies render with the correct direction and
  punctuation even in the English UI, and dates can be shown in the Shamsi calendar.
- **Reactions that stay in place.** Long-press any message and drop an emoji reaction on it —
  stored locally and shown as a chip under the bubble. Reaction SMS that other clients send as
  text (`Reacted 👍 to "…"`) are recognised and folded onto the message they answer instead of
  appearing as a separate bubble.
- **Animated navigation.** Choose how conversations and other screens open and close — slide,
  fade, pop or no animation — and see the effect in the settings preview straight away.
- **Delivery reports, send delay and auto-cleanup.** Confirm delivery per message, hold an
  outgoing SMS for 3/5/10 seconds with a cancel button, and optionally cap a conversation at
  500–5,000 messages so the oldest ones disappear by themselves.
- **Lock-screen privacy.** Choose whether the lock screen shows the sender and the text, only
  the sender, or nothing at all.
- **Combinable tab filters.** Every custom tab can show spam only, non-spam only, contacts only
  or non-contacts only — and any combination of them.
- **Bilingual, follows the system.** English and Persian UI with a System / English / Persian
  switch in Settings.

### Appearance & settings

Open the **⋮** menu in the inbox and choose **Settings** — or use the quick **Customize
inbox** sheet. Everything applies live.

| Section | What you can change |
| --- | --- |
| **Theme** | Auto, Light, Dark, or Black (true black for AMOLED). Accent follows the system wallpaper color on Android 12+, or pick one of nine hand-tuned accents. |
| **Bubbles** | Classic, Rounded, Pill or Compact shape; bubble tail on/off; independent colors for sent and received bubbles; four text sizes; Vazirmatn, Plus Jakarta or the system font. |
| **Chat background** | Six background tones, or any photo from your gallery (downsampled and scrimmed so bubbles stay readable). Settings warns you if Android has revoked access to the photo, instead of silently showing a blank background. |
| **Conversation** | Contact photos next to received messages, Today/Yesterday dividers, Enter-to-send, SMS character counter, tap a timestamp to switch Gregorian/Shamsi, and the message animation used when a chat opens or closes. |
| **Inbox** | Conversation tabs, SIM filter, contact photos, contacts-only mode. |
| **Notifications** | Delete action, the copy-code button for one-time codes, delivery reports, and what the lock screen may show. |
| **Sending** | Send delay (off / 3 / 5 / 10 seconds) with a countdown you can cancel. |
| **Storage** | Auto-delete old messages per conversation (off by default, 500–5,000). |
| **Language** | System default, English or Persian. |
| **Filtering** | “No spam for contacts”, plus a shortcut to Filter Studio. |
| **Reset** | Restore the default look without touching messages or filters. |

### How filtering works

Every incoming message is checked in this order:

1. **Allowlist** — always delivered with a normal notification. Built-in presets cover login
   codes, banking OTPs (`رمز پویا` / `رمز دوم`) and other verification messages.
2. **Blocklist** — matching messages are marked as spam: no sound, no badge, and collapsed
   into a slim pill inside the conversation (tap to reveal, or mark “not spam”).
3. **Sender settings** — inside a chat you can mute or block one specific sender, choose its
   default action and preferred SIM, and even give that sender its own rules.

Because the allowlist wins, filtering never swallows an OTP.

Ramesh must be your **default SMS app** to receive messages in real time — Android only
delivers incoming SMS to the default messaging app.

### Custom filters

Filter Studio is where rules live:

- add allowlist or blocklist rules (regex or substring)
- enable, disable, edit or delete any rule, including built-ins
- test a pattern against sample text before saving
- export and import the full list as JSON

A prompt that works well with an AI agent:

> Write a Java regex for Ramesh that matches Iranian SMS of this kind: `[paste an example]`.
> It must not match OTPs or bank verification messages.

Then add the pattern in Filter Studio as a blocklist rule.

### Install

1. Download the signed `ramesh-<version>-universal.apk` from
   [GitHub Releases](https://github.com/zodiacc7/ramesh/releases) (latest stable: **1.0.0**, build 1000;
   pre-release: **1.0.1**, build 1011).
2. Install it and open Ramesh.
3. Grant SMS, contacts and notification permissions.
4. Accept the prompt to become the default SMS app.

Android 8.0 (API 26) or newer is required. MMS is not supported.

**Play Protect may warn or block the install.** Ramesh is distributed outside Google Play, requests
sensitive permissions (SMS, contacts) and every release is signed with a key Play Protect has no
history for, so Android shows *"unknown developer"*, *"app scan recommended"* or even *"harmful app
blocked"*. These are reputation verdicts, not a statement about the code. What to do:

* **Before installing, verify the download.** Compare the file hash with `SHA256SUMS` on the same
  release page, then check the signer:
  ```bash
  sha256sum ramesh-<version>-universal.apk
  apksigner verify --print-certs ramesh-<version>-universal.apk
  ```
  The certificate SHA-256 must be
  `C1:7B:EA:F6:B5:FC:6A:DA:59:9E:85:5D:6D:A8:00:B0:D9:C6:D0:75:CB:5F:71:C7:5A:88:3D:45:97:D2:A1:AC`
  (also recorded in [keystore/README.md](keystore/README.md)).
* **"Unknown developer" / "scan recommended"**: tap *More details → Install anyway*. This is normal
  for any app outside Play and fades as an app accumulates installs.
* **"Harmful app blocked"**: install over ADB (`adb install -r ramesh-<version>-universal.apk`),
  which bypasses the Play Protect dialog, and report the false positive from the Play Store
  (profile → Play Protect → help) with the hash and signer above. Builds published before
  `1.0.0` were signed with a key that had been published in this repository by mistake and are
  flagged for that reason; **`1.0.0` and everything after it** use a freshly generated key.
* **Upgrading**: `1.0.0` installs over any build signed with the current key (`1.1.0-beta.18` and
  later). Builds older than that used the retired key, so those need one uninstall and reinstall;
  `allowBackup="true"` normally restores Ramesh's settings.

**Development builds.** Every push to `main` publishes an unsigned debug APK as the
[`debug-latest`](https://github.com/zodiacc7/ramesh/releases/tag/debug-latest) pre-release. It uses
the package id `com.ramesh.sms.dev`, so it installs next to the regular app.

**Signed releases.** Pushing a tag that matches `versionName` (for example `1.0.0`) runs the
release workflow, which builds, signs and publishes `ramesh-<version>-universal.apk` together with
its SHA-256 checksum and the bilingual release notes from `fastlane/metadata`.

**Pre-releases.** A `-pre` tag (for example `1.0.1-pre`, with `versionName = "1.0.1"`) runs the
pre-release workflow instead. It signs the same APK but publishes it with `--prerelease` and
`--latest=false`, so a pre-release never replaces the latest stable release.

> Ramesh is distributed through GitHub Releases; there is no F-Droid listing for
> `com.ramesh.sms` yet.

### Build from source

Requirements: **JDK 21** and the **Android SDK (API 36)**.

```bash
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew test               # unit tests (filter engine, inbox snapshot, database cache, OTP, dates, prefs)
./gradlew assembleRelease    # needs the signing environment variables below
```

Release builds are signed with a key that is **not** in this repository: CI reads it from the
`SIGNING_KEYSTORE` GitHub secret. To build a signed release locally, point the environment variables
`SIGNING_STORE_FILE`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD` and
`SIGNING_STORE_TYPE` at your own copy of the key. See [keystore/README.md](keystore/README.md).

### Project structure

```
app/src/main/java/com/ramesh/sms/
├── data/        SQLite rules DB, SMS repository, SIM telephony, preferences
├── engine/      filter engine, incoming-SMS policy, OTP detector, notifications
├── receiver/    SMS/MMS receivers, notification actions, sent/delivery reports
├── theme/       Material 3 theme, appearance model + store, bubble shapes
└── ui/          inbox, chat, compose, Filter Studio, settings, components
```

### Package and stored data

Ramesh owns its identifiers end to end: package `com.ramesh.sms`, preferences
`ramesh_preferences` / `ramesh_appearance` / `ramesh_test_lab`, the rules database
`ramesh_filters.db`, and notification channels `ramesh_sms_alert_channel` /
`ramesh_sms_silent_channel`.

Every byte Ramesh writes lives under its own identifiers, and filter rules can be exported and
imported as JSON from Filter Studio at any time.

### Privacy

- No internet permission, no analytics, no ads, no third-party SDKs.
- Messages and rules stay on the device.
- Contact photos and wallpapers are read only when you pick them, through the system picker.

### Contributing

Issues and pull requests: <https://github.com/zodiacc7/ramesh>.

### Credits & license

Ramesh is based on [Misga](https://github.com/mirarr-app/Misga) by Mirarr. Its bubble shapes and
theme picker were inspired by several messenger UIs, including
[Textra SMS](https://textra.app); no code, artwork or other asset from those apps is used here, and
Ramesh is not affiliated with, endorsed by or sponsored by any of them. Fonts:
[Vazirmatn](https://github.com/rastikerdar/vazirmatn) and
[Plus Jakarta Sans](https://github.com/tokotype/PlusJakartaSans) (both SIL OFL).
Licensed under the **GNU GPL v3** — see [LICENSE](LICENSE).

---

<a id="farsi"></a>

<div dir="rtl" lang="fa">

## فارسی

**رامش** یک برنامهٔ کامل پیامک برای اندروید است که برای واقعیت پیامک در ایران ساخته شده و
ظاهری دارد که تا آخرین جزئیات قابل شخصی‌سازی است.

پیامک در ایران بیشتر سر و صداست: تبلیغ، تخفیف و کلاهبرداری، لابه‌لای چند کد تأیید که نباید
از دست بروند. رامش از ابتدا فیلترهای آمادهٔ regex دارد تا این سر و صدا را خفه کند، و یک
لیست سفید تضمین می‌کند کدهای تأیید و پیام‌های بانکی همیشه برسند.

> رامش برنامه‌ای مستقل با شناسهٔ بستهٔ `com.ramesh.sms` و پروانهٔ آزاد GPL-3.0 است.

**آخرین نسخهٔ پایدار: ۱.۰.۰ (بیلد ۱۰۰۰)؛ نسخهٔ ۱.۰.۱ (بیلد ۱۰۱۱) پیش‌انتشار است.** یادداشت نسخه‌ها در
[صفحهٔ ریلیزها](https://github.com/zodiacc7/ramesh/releases) و پوشهٔ
[`fastlane/metadata/android/fa-IR/changelogs/`](fastlane/metadata/android/fa-IR/changelogs) است؛
مستندهای طراحی در [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) و بازبینی در
[docs/REVIEW.md](docs/REVIEW.md).

### ویژگی‌ها

- **فیلتر با اولویت لیست سفید.** قوانین لیست سفید پیش از لیست سیاه بررسی می‌شوند؛ پس پیام
  تبلیغاتی‌ای که یک کد ورود واقعی هم دارد، از دست نمی‌رود.
- **پیش‌فرض‌های آمادهٔ ایرانی.** الگوهای اسپم، تبلیغات، فیشینگ و کلاهبرداری از قبل هست؛
  می‌توانید نگه دارید، خاموش کنید، ویرایش کنید یا همه را پاک کنید و از صفر بسازید.
- **Filter Studio.** افزودن قانون regex یا متن ساده، آزمایش الگو روی متن نمونه، و
  خروجی/ورودی گرفتن کل قوانین با JSON.
- **شخصی‌سازی کامل ظاهر.** تم خودکار/روشن/تیره/مشکی AMOLED، رنگ تأکیدی، چهار شکل حباب با
  دنبالهٔ اختیاری، رنگ جداگانه برای حباب ارسالی و دریافتی، فونت و اندازهٔ متن، شش رنگ پس‌زمینه
  یا هر عکسی از گالری، و عکس مخاطب.
- **دو سیم‌کارت، درست و کامل.** انتخاب سیم‌کارت برای هر پیام، نمایش سیم‌کارت دریافت‌کننده و
  فیلتر کردن صندوق بر اساس سیم‌کارت.
- **اعلان آرام، شمارش دیده‌شدنی.** پیام‌های فیلترشده بی‌صدا می‌رسند و اسپم‌ها هیچ اعلانی
  نمی‌سازند، اما رسیدن اسپم تا وقتی گفتگو را باز نکنید روی نشان تب‌های **همه** و **اسپم**
  شمرده می‌شود. اعلان‌ها پاسخ سریع، خوانده‌شدن، حذف اختیاری و دکمهٔ **کپی کد** برای پیامک‌های
  تأیید دارند.
- **بدون دسترسی اینترنت.** رامش اجازهٔ `INTERNET` ندارد؛ فیلتر، مخاطبین و پردازش تصویر
  کاملاً روی گوشی انجام می‌شود.
- **پشتیبانی از راست‌به‌چپ.** متن فارسی و عربی با جهت و نشانه‌گذاری درست نمایش داده می‌شود و
  تاریخ‌ها را می‌توان شمسی دید.
- **واکنش‌ها سر جای خودشان.** با نگه‌داشتن هر پیام می‌توانید به آن واکنش بدهید؛ واکنش‌ها روی
  گوشی ذخیره و زیر حباب نمایش داده می‌شوند. واکنش‌هایی که برنامه‌های دیگر به‌صورت متن می‌فرستند
  (`Reacted 👍 to "…"`) شناسایی و زیر همان پیام می‌نشینند، نه به‌شکل یک پیام جدا.
- **انیمیشن ورود و خروج.** انتخاب کنید گفتگوها و صفحه‌ها با کشویی، محو، جهشی یا بدون انیمیشن
  باز و بسته شوند — و نتیجه را همان‌جا در پیش‌نمایش تنظیمات ببینید.
- **گزارش تحویل، تأخیر ارسال و پاک‌سازی خودکار.** تأیید تحویل هر پیام، نگه‌داشتن پیام برای
  ۳/۵/۱۰ ثانیه با دکمهٔ لغو، و محدودکردن هر گفتگو به ۵۰۰ تا ۵٬۰۰۰ پیام تا پیام‌های قدیمی خودکار
  حذف شوند.
- **حریم خصوصی صفحهٔ قفل.** انتخاب اینکه روی صفحهٔ قفل فرستنده و متن دیده شود، فقط فرستنده، یا
  هیچ‌چیز.
- **فیلترهای ترکیبی تب‌ها.** هر تب می‌تواند فقط اسپم، فقط غیراسپم، فقط مخاطب یا فقط غیرمخاطب را
  نشان دهد — و هر ترکیبی از آن‌ها.
- **دوزبانه و پیرو سیستم.** رابط انگلیسی و فارسی با انتخاب دستی «سیستم / انگلیسی / فارسی».

### ظاهر و تنظیمات

از منوی **⋮** در صندوق ورودی گزینهٔ **Settings** را بزنید، یا از پنجرهٔ سریع **Customize inbox**
استفاده کنید. همهٔ تغییرها همان لحظه اعمال می‌شوند.

| بخش | چه چیزی قابل تغییر است |
| --- | --- |
| **تم** | خودکار، روشن، تیره یا مشکی واقعی (AMOLED). رنگ تأکیدی از رنگ سیستم اندروید ۱۲ به بالا، یا یکی از نُه رنگ دست‌چین‌شده. |
| **حباب‌ها** | شکل کلاسیک، گرد، قرصی یا جمع‌وجور؛ دنبالهٔ حباب روشن/خاموش؛ رنگ مستقل حباب ارسالی و دریافتی؛ چهار اندازهٔ متن؛ فونت Vazirmatn، Plus Jakarta یا فونت سیستم. |
| **پس‌زمینهٔ گفتگو** | شش رنگ پس‌زمینه یا هر عکسی از گالری (با کاهش اندازه و لایهٔ محو برای خوانایی حباب‌ها). اگر اندروید دسترسی به عکس را پس گرفته باشد، تنظیمات هشدار می‌دهد و بی‌صدا پس‌زمینهٔ خالی نشان نمی‌دهد. |
| **گفتگو** | عکس مخاطب کنار پیام‌های دریافتی، جداکنندهٔ امروز/دیروز، ارسال با Enter، شمارندهٔ کاراکتر، لمس تاریخ برای تغییر تقویم شمسی/میلادی، و انیمیشن باز و بسته شدن گفتگو. |
| **صندوق ورودی** | تب‌های گفتگو، فیلتر سیم‌کارت، عکس مخاطب و حالت «فقط مخاطبین». |
| **اعلان‌ها** | دکمهٔ حذف، دکمهٔ کپی کد، گزارش تحویل و اینکه صفحهٔ قفل چه چیزی را نشان دهد. |
| **ارسال** | تأخیر در ارسال (خاموش / ۳ / ۵ / ۱۰ ثانیه) با شمارش معکوس و امکان لغو. |
| **ذخیره‌سازی** | حذف خودکار پیام‌های قدیمی در هر گفتگو (پیش‌فرض خاموش، ۵۰۰ تا ۵٬۰۰۰). |
| **زبان** | پیش‌فرض سیستم، انگلیسی یا فارسی. |
| **فیلتر** | «بدون اسپم برای مخاطبین» و میان‌بر Filter Studio. |
| **بازنشانی** | برگشت به ظاهر پیش‌فرض، بدون دست‌زدن به پیام‌ها و فیلترها. |

### فیلتر چطور کار می‌کند

هر پیام ورودی به این ترتیب بررسی می‌شود:

1. **لیست سفید** — همیشه با اعلان عادی می‌رسد. پیش‌فرض‌ها کد ورود، رمز پویا/رمز دوم بانکی و
   دیگر پیام‌های تأیید را پوشش می‌دهند.
2. **لیست سیاه** — پیام‌های منطبق، اسپم می‌شوند: بدون صدا، بدون نشان اعلان و جمع‌شده در یک
   نوار باریک داخل گفتگو (برای دیدن یا «اسپم نیست» لمس کنید).
3. **تنظیمات فرستنده** — از داخل گفتگو می‌توانید یک فرستنده را بی‌صدا یا مسدود کنید، رفتار
   پیش‌فرض و سیم‌کارت دلخواهش را تعیین کنید و حتی برایش قانون اختصاصی بسازید.

چون لیست سفید اولویت دارد، هیچ کد تأییدی قربانی فیلتر نمی‌شود.

برای دریافت پیام‌ها در لحظه، رامش باید **برنامهٔ پیامک پیش‌فرض** باشد؛ اندروید پیامک ورودی را
فقط به برنامهٔ پیش‌فرض می‌دهد.

### فیلترهای سفارشی

قوانین در Filter Studio نگهداری می‌شوند:

- افزودن قانون لیست سفید یا لیست سیاه (regex یا زیررشته)
- روشن، خاموش، ویرایش یا حذف هر قانون، حتی پیش‌فرض‌ها
- آزمایش الگو روی متن نمونه پیش از ذخیره
- خروجی و ورودی گرفتن کل لیست به‌صورت JSON

نمونهٔ درخواست از ایجنت هوش مصنوعی:

> یک regex جاوا برای رامش بنویس که این نوع پیامک ایرانی را بگیرد: `[یک نمونه بچسبانید]`.
> نباید با کد تأیید یا پیام بانکی منطبق شود.

بعد الگو را در Filter Studio به‌عنوان قانون لیست سیاه اضافه کنید.

### نصب

۱. فایل امضاشدهٔ `ramesh-<version>-universal.apk` را از
   [GitHub Releases](https://github.com/zodiacc7/ramesh/releases) دانلود کنید (آخرین نسخهٔ پایدار: **۱.۰.۰**،
   بیلد ۱۰۰۰؛ پیش‌انتشار: **۱.۰.۱**، بیلد ۱۰۱۱).
۲. نصب کنید و رامش را باز کنید.
۳. دسترسی پیامک، مخاطبین و اعلان را بدهید.
۴. درخواست «برنامهٔ پیامک پیش‌فرض» را بپذیرید.

اندروید ۸٫۰ (API ۲۶) یا جدیدتر لازم است. پیام‌های چندرسانه‌ای (MMS) پشتیبانی نمی‌شوند.

**Play Protect ممکن است هشدار بدهد یا نصب را بلاک کند.** رامش بیرون از Google Play توزیع می‌شود،
مجوزهای حساس (پیامک، مخاطبین) می‌خواهد و هر نسخه‌اش با کلیدی امضا می‌شود که Play Protect سابقه‌ای
از آن ندارد؛ بنابراین اندروید پیام «توسعه‌دهندهٔ ناشناس»، «اسکن برنامه توصیه می‌شود» یا حتی
«برنامهٔ مضر بلاک شد» نشان می‌دهد. این‌ها حکم شهرت‌اند، نه داوری دربارهٔ کد. کار درست:

* **قبل از نصب، دانلود را تأیید کنید.** هش فایل را با `SHA256SUMS` همان صفحهٔ ریلیز مقایسه کنید و
  امضاکننده را ببینید:
  ```bash
  sha256sum ramesh-<version>-universal.apk
  apksigner verify --print-certs ramesh-<version>-universal.apk
  ```
  اثر انگشت گواهی باید
  `C1:7B:EA:F6:B5:FC:6A:DA:59:9E:85:5D:6D:A8:00:B0:D9:C6:D0:75:CB:5F:71:C7:5A:88:3D:45:97:D2:A1:AC`
  باشد (در [keystore/README.md](keystore/README.md) هم ثبت شده است).
* **«توسعه‌دهندهٔ ناشناس» / «اسکن توصیه می‌شود»**: *More details → Install anyway*. برای هر اپ
  بیرون از Play طبیعی است و با زیادشدن نصب‌ها کم‌رنگ می‌شود.
* **«برنامهٔ مضر بلاک شد»**: با ADB نصب کنید (`adb install -r ramesh-<version>-universal.apk`) که
  دیالوگ Play Protect را دور می‌زند، و مثبت‌کاذب را از Play Store (پروفایل → Play Protect → help)
  همراه هش و اثر انگشت بالا گزارش کنید. نسخه‌های منتشرشده پیش از `1.0.0` با کلیدی امضا شده‌اند که
  اشتباهاً در مخزن عمومی منتشر شده بود و به همین دلیل پرچم می‌خورند؛ **`1.0.0` و نسخه‌های بعد از آن**
  با کلید تازه‌ساخته امضا می‌شوند.
* **ارتقا**: `1.0.0` روی هر نسخه‌ای که با کلید فعلی امضا شده باشد (`1.1.0-beta.18` و بعد از آن) درجا
  نصب می‌شود. نسخه‌های قدیمی‌تر با کلید بازنشسته امضا شده‌اند و یک‌بار حذف و نصب لازم دارند؛
  `allowBackup="true"` معمولاً تنظیمات رامش را برمی‌گرداند.

**نسخه‌های آزمایشی.** با هر push روی `main` یک APK دیباگِ امضانشده در پیش‌انتشار
[`debug-latest`](https://github.com/zodiacc7/ramesh/releases/tag/debug-latest) منتشر می‌شود. شناسهٔ
بستهٔ آن `com.ramesh.sms.dev` است، پس کنار نسخهٔ معمولی نصب می‌شود.

**نسخه‌های امضاشده.** با زدن تگی هم‌نام با `versionName` (مثلاً `1.0.0`) گردش‌کار انتشار اجرا
می‌شود: فایل `ramesh-<version>-universal.apk` ساخته، امضا و همراه با چکیدهٔ SHA-256 و یادداشت‌های
دوزبانهٔ نسخه (از `fastlane/metadata`) منتشر می‌شود.

**پیش‌انتشارها.** تگ‌های `-pre` (مثلاً `1.0.1-pre` با `versionName = "1.0.1"`) گردش‌کار پیش‌انتشار را
اجرا می‌کنند: همان فایل امضاشده با پرچم‌های `--prerelease` و `--latest=false` منتشر می‌شود، پس هیچ
پیش‌انتشاری جای «آخرین نسخهٔ پایدار» را نمی‌گیرد.

> رامش از طریق GitHub Releases منتشر می‌شود و هنوز صفحه‌ای در F-Droid برای بستهٔ
> `com.ramesh.sms` وجود ندارد.

### ساخت از روی سورس

نیازمندی‌ها: **JDK 21** و **Android SDK (API 36)**.

```bash
./gradlew assembleDebug      # app/build/outputs/apk/debug/app-debug.apk
./gradlew test               # تست‌های واحد موتور فیلتر، صندوق ورودی، کش پایگاه داده، تشخیص OTP، تاریخ و تنظیمات
./gradlew assembleRelease    # نیازمند متغیرهای امضای زیر
```

نسخهٔ انتشار با کلیدی امضا می‌شود که **داخل این مخزن نیست**؛ CI آن را از secret با نام
`SIGNING_KEYSTORE` می‌خواند. برای ساخت نسخهٔ امضاشده روی سیستم خودت، متغیرهای محیطی
`SIGNING_STORE_FILE`، `SIGNING_STORE_PASSWORD`، `SIGNING_KEY_ALIAS`، `SIGNING_KEY_PASSWORD` و
`SIGNING_STORE_TYPE` را به نسخهٔ شخصی کلید اشاره بده. جزئیات در
[keystore/README.md](keystore/README.md).

### ساختار پروژه

```
app/src/main/java/com/ramesh/sms/
├── data/        پایگاه‌دادهٔ قوانین، مخزن پیام‌ها، سیم‌کارت، تنظیمات
├── engine/      موتور فیلتر، سیاست پیام ورودی، تشخیص OTP، اعلان‌ها
├── receiver/    گیرنده‌های پیامک/MMS، اکشن‌های اعلان، گزارش ارسال و تحویل
├── theme/       تم Material 3، مدل و مخزن ظاهر، شکل حباب‌ها
└── ui/          صندوق ورودی، گفتگو، نوشتن پیام، Filter Studio، تنظیمات، کامپوننت‌ها
```

### بسته و داده‌های ذخیره‌شده

همهٔ شناسه‌ها مخصوص رامش است: بستهٔ `com.ramesh.sms`، تنظیمات `ramesh_preferences`،
`ramesh_appearance` و `ramesh_test_lab`، پایگاه‌دادهٔ `ramesh_filters.db` و کانال‌های اعلان
`ramesh_sms_alert_channel` و `ramesh_sms_silent_channel`.

همهٔ داده‌ها با شناسه‌های خود رامش ذخیره می‌شوند و فیلترها هر زمان از Filter Studio به‌صورت
JSON قابل خروجی و ورودی هستند.

### حریم خصوصی

- بدون دسترسی اینترنت، بدون تحلیل‌گر، بدون تبلیغات و بدون کتابخانهٔ شخص ثالث.
- پیام‌ها و قوانین فقط روی دستگاه می‌مانند.
- عکس مخاطب و پس‌زمینه فقط وقتی خودتان انتخاب کنید و از طریق انتخاب‌گر سیستم خوانده می‌شوند.

### مشارکت

گزارش خطا و درخواست تغییر: <https://github.com/zodiacc7/ramesh>

### اعتبار و پروانه

رامش بر پایهٔ [میسگا](https://github.com/mirarr-app/Misga) ساختهٔ Mirarr است. شکل حباب‌ها و
انتخاب‌گر تم آن از چند رابط پیام‌رسان از جمله [Textra SMS](https://textra.app) الهام گرفته شده؛
هیچ کد، تصویر یا دارایی دیگری از آن برنامه‌ها در رامش استفاده نشده و رامش هیچ وابستگی، تأییدیه یا
حمایت مالی از سوی آن‌ها ندارد. فونت‌ها: [Vazirmatn](https://github.com/rastikerdar/vazirmatn) و
[Plus Jakarta Sans](https://github.com/tokotype/PlusJakartaSans) (هر دو با پروانهٔ SIL OFL).
پروانه: **GNU GPL v3** — فایل [LICENSE](LICENSE).

</div>
