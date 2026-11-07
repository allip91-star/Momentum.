# Momentum v8 — Compact Today screen

## What changed in v8

The **Today** view is now designed to show almost all habits immediately on a normal Android phone:

- The app logo and heading take less vertical space. A single horizontal date/% summary replaces the old Today heading, date card and completion overview card.
- A thin three-pixel completion line replaces the larger status card. It updates immediately when goals are completed or undone.
- Daily, Weekly and Monthly appear as quiet, small labels rather than prominent section headings. Groups with no relevant habits are omitted.
- Habits use shorter, flatter rows, smaller icons and tighter spacing while keeping names, goal totals and buttons readable.
- Logged measured habits show a slim amount progress line. Streak and optional deadline appear inline where possible rather than creating an extra row.
- Measured habits keep **quick-add** (`+`) and a small pencil button for custom quantities. All habits retain the undo button when logged.
- The detailed weekly strip is now folded under **Week at a glance**; tap to expand it.
- **Progress and Settings are unchanged**. The native Android widget, reminders, colours, hide-on-completion, units, history, schedules, targets and backup data have not been altered.

**Visual check:** At a simulated 393 × 852 Android viewport, all eight seeded example habits are visible above the bottom tab bar. On smaller phones or with additional habits / large system fonts, the page can still scroll normally.

**Browser testing:** Tested the initial 8-habit layout at 360 × 780, 393 × 852 and 412 × 915. Passed interactions: completed habit hides, overall % changes, quantity quick-add, custom quantity logging, undo, expanding the weekly panel, switching to Progress and Settings. JavaScript syntax checked. This does *not* confirm the Android APK or native widget on a phone, which still require a GitHub/Android Studio build.

**Build APK using GitHub:** Extract this archive. Upload everything **inside** the `Momentum-v8-Project` directory to the root of a GitHub repository, including `.github`. Open Actions → **Build Momentum Android APK** → **Run workflow**, then download the APK artifact after the workflow succeeds. The APK has not been built or tested in this environment.

**Upgrading:** Export your existing Momentum backup before reinstalling or switching debug APKs. Debug builds signed by different keys may not upgrade each other, and uninstalling deletes local data. v8 preserves the v7 storage schema and uses the same Android application ID.

---

# Momentum v7 — Cleaner habit settings

## New in v7

- **Unit dropdown:** Choose common units (times, sessions, runs, rides, repetitions, sets, ml, L, g, kg, kcal, km, m, steps, minutes, hours, pages or £) from a single menu. Pick **Other — enter your own…** to type any custom unit up to 24 characters.
- **Reuse custom units:** Units you've already saved in a habit automatically appear as choices when creating or editing other habits. Units are stored in the existing `unit` field, so all previous history, widget quantities and backup exports remain compatible.
- **Other simplified dropdowns:** Reset frequency, tracking type, when the habit appears, and the frequency/category filters in Settings all use compact menus instead of rows of buttons.
- **Expandable optional settings:** Icon picker, colour palette, schedule, deadlines and reminders stay collapsed unless you choose to open them. Current deadline/reminder time is visible on the row. Week/Month/Year in Progress remain direct buttons for rapid comparison.
- All v6 functionality is retained (widget, individual colours, custom scheduling, reminders, quantity targets, hide when complete, streaks, dot charts, history and backups).

**How to customise a unit:** Open/create a habit → **Unit** → **Other — enter your own…** → type a unit (for example, `cups`) → Save. It will appear in the habit, widget and chart labels. The app records numeric values in the chosen unit; it does *not* automatically convert between units (e.g. L and ml).

**Validation:** The JavaScript parses successfully and browser interaction tests were performed for custom units, weekly frequency, weekday scheduling, filters, saved habits and re-opening. Android APK/widget/reminder behaviour still requires compiling and testing on your phone.

**IMPORTANT:** Export your Momentum backup before upgrading or uninstalling the app. Android debug APKs built on different machines can have different signatures, and uninstalling deletes local data.

## Build for free

1. Extract this ZIP.
2. Create a GitHub repository and upload the **contents inside** `Momentum-v7-Project`, including `.github`.
3. In GitHub, select **Actions → Build Momentum Android APK → Run workflow**.
4. When successful, download the APK artifact, extract it and install `app-debug.apk` on your Android phone.
5. Open Momentum once, then add the Momentum widget from your Android home screen's Widgets menu.

This archive contains *source code*, not an installable APK. I have not compiled it here.

---

# Momentum v6 — Habit Tracker + Streaks

## New in v6

- **Hide when complete**, separately configurable for every habit. If enabled, finished goals vanish from the Today screen and Android home widget until their next period begins. If disabled, they remain visible and marked ✓. Widget inclusion is a separate toggle.
- **Current and best streaks** measured in the habit's own periods: days for daily, weeks for weekly and months for monthly. An unfinished current period is treated as grace time; paused or unscheduled periods do not count against streaks.
- **Quiet milestones**: a small in-app toast on the 3rd, 7th, 14th, 30th, 50th and 100th consecutive completed period. No compulsive notifications or punitive penalties.
- **Migration**: existing v5 habits without the `hideWhenComplete` field retain their former disappearing-widget behaviour (enabled by default). Newly created habits default to **keep showing** until you enable the setting. Old backups import safely; exported backups preserve the choice.
- **Completed widget rows** keep their coloured status and show a ✓ that opens the app rather than adding further units.

# Momentum — Minimalist habit tracker for Android

A free, offline-first Android app project with a resizable home-screen widget, coloured habits and a minimalist Progress page. Includes a browser prototype, source files and a GitHub Actions APK workflow. **This is source code, not a pre-built APK.**

## New features

- **Smart scheduling:** daily, Monday–Sunday weekly or monthly targets, with optional selected weekdays or every N days. On an unscheduled day, an unfinished habit is hidden from Today/the widget; history is kept. Weekly targets remain cumulative, so 3 gym sessions means 3 sessions in one week, not 3 out of 7 calendar days.
- **Native widget:** select habits for your Samsung home-screen, order them with drag/arrow controls, give each a colour or apply one colour to all, and hide finished goals when individually configured to do so. Scroll to view more habits. Sum-based habits support quick-add; latest/best measurements open the app for exact entry.
- **Measured goals:** choose number of times, or amounts with custom unit (ml, kcal, km, kg, hours, etc.). For measurements, choose **Sum** (water/distance/food), **Latest value** (body weight), or **Maximum** (e.g. strength). Set a target of **at least** or **at most**. Calories with an upper-limit target are judged when the day/week/month ENDS; exceeding a budget is not marked as success.
- **Backdate & correct:** use Today navigation or History calendar to log on an earlier date, then tap minus to undo. Measured readings can be logged multiple times; latest and maximum behave correctly on undo. Future logging is blocked.
- **Pausing:** pause or resume any habit from Settings without losing logs. Fully paused periods and rest days are excluded from completion-rate denominators.
- **Categories:** Fitness, Health, Learning, Personal, Other — filterable in Settings.
- **Dot-chart Progress:** Week / Month / Year, personal best streaks, the prior weekly summary, a compact per-habit completion visual and optional measurement trends. Each dot represents a goal period (or an average of periods when compacted for monthly/yearly views). Percentages reflect the proportion of each target achieved; overall circle counts fully completed scheduled periods. The whole week counts as one goal period for weekly gym/runs.
- **Optional deadline and reminders:** per-habit due time and notification time, independently configurable. Weekly or monthly reminders can fire daily or once on a configured period date. An optional weekly summary notification is scheduled for **Monday 8:00 am**.
- **Backups:** export and restore JSON data with historical logs, colours, widget settings, pauses, categories, scheduling and notifications.

## What is and isn't implemented

**Implemented in the browser UI and Android source**: all the features listed above. The **native Android widget**, notification scheduling and Monday summary notification are Android code, so they are not available in the browser preview. Android requests permission for notifications on Android 13+ when reminders or summary notifications are enabled; delivery can be delayed by battery settings. Local notifications require no internet and no account.

**Not yet implemented:** automatic Samsung Health / Health Connect data import. This needs a separate opt-in integration, data-type mapping, permission handling, and validation on a supported phone. Samsung Health can sync supported data via Health Connect, but Momentum currently only logs those measurements manually. No Health data permissions are requested or read by this version.

**Android status:** JavaScript passes syntax and automated browser-interaction tests; layout XML parses successfully. **The Android project has not been compiled into an APK or tested on a physical device in this environment.** GitHub Actions may expose build or device issues requiring correction. In particular, notification behaviour and the native widget need real-device verification.

## Free Android build (GitHub)

1. Create a free GitHub account and a new repository (for example `momentum-habit-tracker`).
2. Upload **all contents** of the `Momentum-v6-Project` folder to the repository root, INCLUDING the hidden `.github` folder.
3. Open **Actions → Build Momentum Android APK → Run workflow**. It may start automatically on the `main` branch.
4. When it succeeds, download the **momentum-app-debug-apk** artifact and extract `app-debug.apk`.
5. Open that APK on your Samsung Android phone. Android may ask you to permit installations from the app you used to open it.
6. Open Momentum once to initialise data. Long-press your home screen → **Widgets** → **Momentum** and add/rescale the widget. Use **Settings → Customise your widget** to choose order, colours and which habits show.
7. Use **Settings → Export backup** before reinstalling/upgrading. Debug APKs produced on separate build machines can have different signing keys; Android may refuse an in-place upgrade. Uninstalling removes local app data, so export first.

Alternative: open the `android/` directory in **Android Studio** with JDK 17 and Android SDK Platform 35; select *Build → Build APK(s)*.

## Files

- `web/index.html` — browser/PWA demo with local storage.
- `android/app/src/main/assets/index.html` — bundled main app UI, synchronized with the web version.
- `android/app/src/main/java/com/momentum/habits/WidgetStore.java` — single local database shared by the native widget and Android WebView.
- `HabitWidgetProvider.java`, `WidgetRemoteViewsService.java` — native Android home-screen widget.
- `ReminderReceiver.java` — on-device alarm and notification scheduling.
- `MainActivity.java` — Android app shell, backup sharing and JavaScript interface.
- `.github/workflows/build-apk.yml` — automated free APK building workflow.
- `Momentum-Preview.html` — convenience local demo.

## Data rules and limitations

- Week starts Monday in device local time; monthly goals reset on the 1st.
- Paused and unscheduled daily dates do not count as misses. Count/quantity thresholds are per configured reset period.
- History can be backdated; scheduling only controls normal presentation, not editing past events.
- Changing a habit's goal or frequency **recalculates its historic percentages using the new settings**; this version does not track historical goal revisions.
- The browser preview and installed Android app use **separate local storage** and do not sync. Back up/restore manually to move data between them.
- Local data is stored on-device. No login, subscription, advertising or app-provided cloud sync.
- App and widget are for personal habit tracking; measured food/water targets are not professional medical or nutritional advice.
