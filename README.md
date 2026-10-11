# Guardian

> ## Read this first: what you are getting into
> Guardian is a self-control app. You install it to stop YOURSELF reaching porn, nude-leak sites, Telegram, X (Twitter) and VPNs, and it is built so that you cannot easily turn it off.
>
> WHAT IT DOES
> - Becomes the phone's Device Owner (set once from a computer with adb). After that Android itself stops you uninstalling it. The only way to uninstall is the 5-minute window described below.
> - Hides the Telegram, X (Twitter), Reddit and Discord apps (and alternative clients) so they cannot open at all, and blocks the Telegram and X websites. Reddit and Discord in a browser are NOT blocked.
> - Blocks VPNs: VPN configuration is locked, every app that can create a VPN (and apps with vpn/proxy/tor/DNS-changer style names) is hidden, and a VPN connection triggers an alarm.
> - Locks Private DNS to CleanBrowsing's adult filter, so every app's internet lookups go through it. CleanBrowsing sees your DNS lookups, and its adult filter also applies SafeSearch to Google/Bing/YouTube at DNS level (change the DNS host in the app, with the unlock, if that causes wrong blocks).
> - Allows only Chrome and Brave. All other browsers are hidden. In Chrome/Brave: about 300 built-in adult domains are blocked, and incognito is off. SafeSearch is not forced and browsing history/cookies can still be cleared.
> - Reads the on-screen text and web addresses of EVERY app through an accessibility service, and sends you to the home screen with a 15-second pause screen when it sees blocked sites or adult keywords. This is processed on the phone only.
> - Downloads a public adult-domain list from GitHub every week and checks pages against it.
> - Night curfew (default 23:00-06:00 UK time): the Instagram and TikTok apps are hidden. Browsers are not affected.
> - Every build installs as a SEPARATE app with its own package name and display name. It never updates an older copy and never updates itself. Only one app can be Device Owner, so to move to a newer build you must first release the old one (PIN, then the next-day unlock) or factory reset.
> - Hides clone/virtual-space and root-tool apps, scans every newly installed app, blocks safe mode, adding users and work profiles, and takes the time from the internet (UK time) for the unlock window and curfew, so changing the phone's clock does nothing; if the UK time cannot be verified, everything stays locked. Installing from unknown sources stays allowed.
>
> WHAT IT MEANS FOR YOU
> - Any change that weakens protection (removing a block, turning a protection off, changing the PIN) needs your PIN. Typing the PIN does not unlock anything straight away: it schedules a window that opens at 14:00 UK time the NEXT day and lasts only 5 minutes. Open the app during those 5 minutes to make changes. Adding protection is always instant.
> - That 5-minute window is also the ONLY way to uninstall Guardian: tap Release & uninstall inside it. If you miss it you must request again and wait another day.
> - Nothing can speed this up. If you forget the PIN, the only way out is a factory reset of the phone.
> - Work or school VPNs will stop working. Telegram and X apps stay hidden, even if you need them for something legitimate.
> - It may block harmless pages that contain several adult keywords, or domains on the downloaded list that are wrong.
> - Android Settings stays fully usable in this version. Strict mode (off by default) additionally blocks factory reset, USB debugging and force-stop.
>
> WHAT IT CANNOT STOP
> - A factory reset from recovery, flashing the phone, another phone or computer, or switching the accessibility service off in Settings (it is re-enabled within about 5 minutes).
>
> It collects nothing for any company and has no account. Its only network traffic is the weekly list download and DNS via CleanBrowsing.
>
> Do not continue unless you want these restrictions on this phone.

(The same text is the first screen of the app.)

Self-control app for Android. It runs as **Device Owner** (system-level, not an overlay), so it cannot be
uninstalled or force-disabled from the normal UI. It blocks adult sites/keywords, Telegram, X and every VPN
route, while leaving Android Settings usable (per your request; see "Strict mode" to change that).

## Build
- GitHub: every push to `main` builds the APK and publishes it under **Releases** as a new release per build, "Build N" (installable from a phone). Each build is a separate app with its own package and name, and nothing ever auto-updates.
- Or Android Studio / `./gradlew assembleDebug` -> `app/build/outputs/apk/debug/app-debug.apk`.

## Install (one time)
1. Install the APK. Remove **all accounts** from the phone (Settings > Accounts); re-add them afterwards.
2. `<package>` is the build's package name, e.g. `com.guardian.app.b7` (the app shows it in its setup box and the release notes list the exact commands). Enable USB debugging (or Wireless debugging + the LADB app if you have no computer), then run:
   ```
   adb shell dpm set-device-owner <package>/com.guardian.app.GuardianAdminReceiver
   adb shell pm grant <package> android.permission.WRITE_SECURE_SETTINGS
   ```
3. Open Guardian, tap **Apply protections now** (it enables its own accessibility service).
4. Set a **PIN**. Typing it only schedules a 5-minute window at 14:00 the next day (also the only way to uninstall), so pick one you cannot talk yourself into using on impulse.

## What it does
| Layer | How |
|---|---|
| No uninstall | `setUninstallBlocked` |
| Telegram / X / Reddit / Discord | Package-prefix + label match -> `setApplicationHidden` (cannot launch at all); Telegram and X web domains blocked in browsers (Reddit and Discord on the web are allowed) |
| VPN | `no_config_vpn`; every app that declares `VpnService` is hidden; name/package heuristics (vpn, proxy, tor, dns...); tripwire if a VPN network comes up |
| DNS | Private DNS locked to CleanBrowsing Adult Filter (blocks porn; CleanBrowsing also applies SafeSearch to search engines at DNS level); `no_config_private_dns`; restored if changed |
| Browsers | Chrome and Brave allowed (all others hidden). Managed policy: URL blocklist (~304 domains, full list in `DOMAINS.txt`), incognito off, DoH off, dev tools off (no forced SafeSearch; history and cookies can be cleared) |
| Online blocklist | Downloads a public adult-domain list (StevenBlack porn hosts, ~60k+ domains) weekly and checks every URL/domain seen on screen against it. Rejects shrunken or tiny downloads; changing the URL needs the unlock. Chrome policy keeps the built-in ~300 (policy size limit); DNS filtering covers the long tail |
| Everything else | Accessibility service scans on-screen text + URLs in **all** apps: blocked domain = instant block, strong keyword = block, 3 mild words = block |

## The 10 extra protections beyond the brief
1. **Next-day PIN unlock**: any change that weakens protection needs the PIN, and typing it only opens a window at 14:00 the next day for 5 minutes, which is also the only way to uninstall. Strengthening is always free.
2. **Internet time (UK)**: the unlock window and curfew use time from `uk.pool.ntp.org` (fallbacks: Cloudflare, Google, gov.uk), not the phone clock, so changing the phone's date/time does nothing. If the time can't be verified the app stays locked.
3. **Self-healing**: 5 min alarm chain + 15 min persisted job + boot receiver re-apply everything; accessibility is re-enabled via `WRITE_SECURE_SETTINGS` if switched off; DNS restored if changed.
4. **Browser lockdown**: incognito off, DoH off, dev tools off (Chrome / Brave managed config).
5. **Browser allow-list** (Chrome + Brave): every other browser (Firefox, Opera, DuckDuckGo, Samsung...) is hidden, since they can't be policy-managed.
6. **Safe-mode / extra-user blocks**: `no_safe_boot` (safe mode would disable the accessibility scanner), `no_add_user`, `no_add_managed_profile`.
7. **Install watcher**: a new app is scanned the moment it is installed (VPNs, cloners, Telegram clients, browsers get hidden within seconds).
8. **Night curfew** (default 23:00-06:00 UK time): Instagram and TikTok apps are hidden. Browsers are not affected.
9. **Local event log**: blocks, VPN attempts, accessibility/DNS tampering and unlock requests are listed on the main screen. Nothing is sent anywhere.
10. **Block screen with friction**: 15 s forced pause, no back button, running block counter. Plus cloner/virtual-space app hiding (Parallel Space, Island, Lucky Patcher, Magisk...).

Optional: **Self-shield** (kicks you out of Settings pages that show "Guardian") and **Strict mode** (blocks factory reset, USB debugging and force-stop/clear-data). Both default OFF because you asked to keep Settings free.

## Known bypasses (honest list)
- Factory reset from recovery/bootloader or flashing the phone (turn on Strict mode and have someone else hold the bootloader/BIOS-style passwords).
- `adb` while debugging is on: with the device owner set it cannot be removed by adb, but Strict mode also blocks enabling debugging.
- A second phone/laptop, or mobile data on another device. Nothing on this phone can stop that.
- Because Settings is open, you can disable the accessibility service (it is re-enabled within 5 minutes), and turn on a hotspot-sharing device.
- Apps that bring their own browser engine and are not VPNs (e.g. in-app browsers) are only covered by the on-screen text scanner, not the URL blocklist.
