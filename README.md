# Guardian

> ## Read this first: what you are getting into
> Guardian is a self-control app. You install it to stop YOURSELF reaching porn, nude-leak sites, Telegram, X (Twitter) and VPNs, and it is built so that you cannot easily turn it off.
>
> WHAT IT DOES
> - Becomes the phone's Device Owner (set once from a computer with adb). After that Android itself stops you uninstalling it.
> - Hides the Telegram and X apps (and Telegram clients) so they cannot open at all, and blocks their websites.
> - Blocks VPNs: VPN configuration is locked, every app that can create a VPN (and apps with vpn/proxy/tor/DNS-changer style names) is hidden, and a VPN connection triggers an alarm.
> - Locks Private DNS to CleanBrowsing's adult filter, so every app's internet lookups go through it. CleanBrowsing sees your DNS lookups.
> - Allows only Chrome and Brave. All other browsers are hidden. In Chrome/Brave: about 300 built-in adult domains are blocked, incognito is off, SafeSearch is forced, and history cannot be deleted.
> - Reads the on-screen text and web addresses of EVERY app through an accessibility service, and sends you to the home screen with a 15-second pause screen when it sees blocked sites or adult keywords. This is processed on the phone only.
> - Downloads a public adult-domain list from GitHub every week and checks pages against it.
> - Night curfew (default 23:00-06:00): all browsers plus Reddit, Instagram, Snapchat, TikTok and Discord are hidden.
> - If you set a partner alert URL (ntfy.sh, Discord or Slack), it sends them short alerts: blocks (app name only, never the page), VPN attempts, accessibility turned off, DNS changes, wrong PIN attempts and unlock requests.
> - Hides clone/virtual-space and root-tool apps, scans every newly installed app, disables installing from unknown sources, safe mode, adding users and work profiles, and locks the date and time.
>
> WHAT IT MEANS FOR YOU
> - Any change that weakens protection (removing a block, turning a protection off, releasing the app) needs your PIN, then a cooling-off wait (default 24 hours), then the PIN again within 30 minutes. Your partner is alerted at every step. Adding protection is always instant.
> - Set the PIN together with a trusted person and let THEM keep it. If you forget it, the only way out is a factory reset of the phone.
> - Work or school VPNs will stop working. Telegram and X apps stay hidden, even if you need them for something legitimate.
> - It may block harmless pages that contain several adult keywords, or domains on the downloaded list that are wrong.
> - Android Settings stays fully usable in this version. Strict mode (off by default) additionally blocks factory reset, USB debugging and force-stop.
>
> WHAT IT CANNOT STOP
> - A factory reset from recovery, flashing the phone, another phone or computer, or switching the accessibility service off in Settings (it is re-enabled within about 5 minutes and your partner is told).
>
> It collects nothing for any company and has no account. Its only network traffic is the weekly list download, DNS via CleanBrowsing, and the alert URL you choose.
>
> Do not continue unless you want these restrictions on this phone.

(The same text is the first screen of the app.)

Self-control app for Android. It runs as **Device Owner** (system-level, not an overlay), so it cannot be
uninstalled or force-disabled from the normal UI. It blocks adult sites/keywords, Telegram, X and every VPN
route, while leaving Android Settings usable (per your request; see "Strict mode" to change that).

## Build
- GitHub: every push to `main` builds the APK and publishes it under **Releases > Guardian (latest build)** (installable from a phone).
- Or Android Studio / `./gradlew assembleDebug` -> `app/build/outputs/apk/debug/app-debug.apk`.

## Install (one time)
1. Install the APK. Remove **all accounts** from the phone (Settings > Accounts); re-add them afterwards.
2. Enable USB debugging (or Wireless debugging + the LADB app if you have no computer), then run:
   ```
   adb shell dpm set-device-owner com.guardian.app/.GuardianAdminReceiver
   adb shell pm grant com.guardian.app android.permission.WRITE_SECURE_SETTINGS
   ```
3. Open Guardian, tap **Apply protections now** (it enables its own accessibility service).
4. Have your accountability partner set the **PIN**, and put their ntfy.sh / Discord / Slack URL in *Partner alert URL*.

## What it does
| Layer | How |
|---|---|
| No uninstall | `setUninstallBlocked`, device-admin deactivation warns your partner |
| Telegram / X | Package-prefix + label match -> `setApplicationHidden` (cannot launch at all); web domains blocked in browsers |
| VPN | `no_config_vpn`; every app that declares `VpnService` is hidden; name/package heuristics (vpn, proxy, tor, dns...); tripwire if a VPN network comes up |
| DNS | Private DNS locked to CleanBrowsing Adult Filter (blocks porn, forces SafeSearch); `no_config_private_dns`; restored if changed |
| Browsers | Chrome and Brave allowed (all others hidden). Managed policy: URL blocklist (~304 domains, full list in `DOMAINS.txt`), incognito off, SafeSearch + Google SafeSites, DoH off, dev tools off, history can't be deleted |
| Online blocklist | Downloads a public adult-domain list (StevenBlack porn hosts, ~60k+ domains) weekly and checks every URL/domain seen on screen against it. Rejects shrunken or tiny downloads; changing the URL needs the unlock. Chrome policy keeps the built-in ~300 (policy size limit); DNS filtering covers the long tail |
| Everything else | Accessibility service scans on-screen text + URLs in **all** apps: blocked domain = instant block, strong keyword = block, 3 mild words = block |

## The 10 extra protections beyond the brief
1. **Cooling-off unlock**: any change that weakens protection needs PIN -> 24h wait -> PIN again -> 15 min edit window. Partner is alerted at each step. Strengthening is always free.
2. **Tamper-proof time**: auto-time forced + `no_config_date_time`, so you cannot clock-skip the 24h wait.
3. **Self-healing**: 5 min alarm chain + 15 min persisted job + boot receiver re-apply everything; accessibility is re-enabled via `WRITE_SECURE_SETTINGS` if switched off; DNS restored if changed.
4. **Browser lockdown**: incognito off, SafeSearch/SafeSites, DoH off, history can't be cleared (Chrome / Edge / Brave managed config).
5. **Browser allow-list** (Chrome + Brave): every other browser (Firefox, Opera, DuckDuckGo, Samsung...) is hidden, since they can't be policy-managed.
6. **Sideload / safe-mode / extra-user blocks**: `no_install_unknown_sources(_globally)`, `no_safe_boot` (safe mode would disable the accessibility scanner), `no_add_user`, `no_add_managed_profile`.
7. **Install watcher**: a new app is scanned the moment it is installed (VPNs, cloners, Telegram clients, browsers get hidden within seconds).
8. **Night curfew** (default 23:00-06:00): all browsers plus Reddit/Instagram/Snapchat/TikTok/Discord are hidden.
9. **Partner alerts**: ntfy.sh/Discord/Slack webhook for blocks, VPN, accessibility off, DNS change, PIN failures, unlock steps. Content is never sent, only the category and app.
10. **Block screen with friction**: 15 s forced pause, no back button, running block counter. Plus cloner/virtual-space app hiding (Parallel Space, Island, Lucky Patcher, Magisk...).

Optional: **Self-shield** (kicks you out of Settings pages that show "Guardian") and **Strict mode** (blocks factory reset, USB debugging and force-stop/clear-data). Both default OFF because you asked to keep Settings free.

## Known bypasses (honest list)
- Factory reset from recovery/bootloader or flashing the phone (turn on Strict mode and have someone else hold the bootloader/BIOS-style passwords).
- `adb` while debugging is on: with the device owner set it cannot be removed by adb, but Strict mode also blocks enabling debugging.
- A second phone/laptop, or mobile data on another device. Nothing on this phone can stop that; that is what the partner alerts are for.
- Because Settings is open, you can disable the accessibility service (it is re-enabled within 5 minutes and the partner is told), and turn on a hotspot-sharing device.
- Apps that bring their own browser engine and are not VPNs (e.g. in-app browsers) are only covered by the on-screen text scanner, not the URL blocklist.
