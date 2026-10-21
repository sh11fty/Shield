package com.guardian.app

/** Built-in lists. Users can only ADD to these (extras in Prefs); defaults can't be removed from the UI. */
object Blocklists {

    val DOMAINS: List<String> = listOf(
        // adult tubes / aggregators
        "pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com", "xhamster.desi", "redtube.com", "youporn.com",
        "spankbang.com", "eporner.com", "tube8.com", "beeg.com", "tnaflix.com", "porn.com", "pornone.com",
        "thumbzilla.com", "hqporner.com", "txxx.com", "hclips.com", "porntrex.com", "daftsex.com", "sxyprn.com",
        "pornhat.com", "motherless.com", "xvideos2.com", "xvideos3.com", "xnxx.tv", "xtube.com", "youjizz.com",
        "drtuber.com", "fuq.com", "4tube.com", "porndig.com", "pornktube.com", "vporn.com", "analdin.com",
        "ixxx.com", "fapster.xxx", "fapello.com", "fapality.com", "heavy-r.com", "nuvid.com", "sunporno.com",
        "tubegalore.com", "perfectgirls.net", "pornpics.com", "freeones.com", "iafd.com", "babepedia.com",
        "javhd.com", "jav.guru", "missav.com", "supjav.com", "javmost.com", "hpjav.com", "cavporn.com",
        "xmoviesforyou.com", "pornzog.com", "stripchat.com", "chaturbate.com", "livejasmin.com", "bongacams.com",
        "cam4.com", "camsoda.com", "myfreecams.com", "flirt4free.com", "imlive.com", "xhamsterlive.com",
        // studios / premium
        "brazzers.com", "bangbros.com", "realitykings.com", "naughtyamerica.com", "mofos.com", "digitalplayground.com",
        "blacked.com", "tushy.com", "vixen.com", "nubiles.net", "teamskeet.com", "evilangel.com", "kink.com",
        "twistys.com", "babes.com", "metart.com", "playboy.com", "penthouse.com", "hustler.com",
        // creator platforms / leaks
        "onlyfans.com", "fansly.com", "manyvids.com", "clips4sale.com", "loyalfans.com", "fanvue.com", "justfor.fans",
        "coomer.su", "coomer.party", "kemono.su", "kemono.party", "erome.com", "redgifs.com", "thothub.to",
        "simpcity.su", "bunkr.si", "leakedmodels.com",
        // hentai / drawn
        "nhentai.net", "hentaihaven.xxx", "hanime.tv", "hentaifox.com", "hitomi.la", "rule34.xxx", "rule34.paheal.net",
        "e621.net", "e-hentai.org", "exhentai.org", "gelbooru.com", "danbooru.donmai.us", "rule34video.com",
        "pururin.to", "tsumino.com", "fakku.net", "luscious.net", "hentai2read.com", "8muses.com",
        "multporn.net", "porncomix.info", "myreadingmanga.info",
        // erotica / dating hookups
        "literotica.com", "asstr.org", "adultfriendfinder.com", "ashleymadison.com", "fling.com", "sex.com",
        "xxx.com", "sexier.com", "tinder.com",
        // more leak / desi-MMS sites (cross-checked against two public adult blocklists)
        "kamababa.cam", "kamababa.click", "kamababa.cfd", "kamababa.am.in", "desileaks.in",
        "hotdesileaks.com", "desikamababa.com", "leak.xxx", "nude.com", "1leak.com",
        "leak5.com", "thothd.to", "aznude.com", "danude.com", "knudes.com",
        "leaked.xxx", "leaksx.com", "nunude.com", "okthot.com", "thothd.com",
        "thothub.su", "xnudes.net", "xxxnude.cc", "anynude.net", "danudes.com",
        "desinude.me", "erothots.co", "gfleaks.com", "javleak.com", "leakxxx.com",
        "of2leaks.co", "onlyfams.tv", "pienude.com", "pornude.net", "thenude.com",
        "thotbay.com", "thotbook.tv", "thothub.org", "thotmax.com", "thotporn.tv",
        "thotshub.tv", "thottok.com", "xhdnude.com", "camleaks.net", "fotonude.org",
        "leakhive.com", "leakonly.com", "leakporn.net", "leaktape.com", "leaktube.net",
        "leaktube.org", "nude-art.net", "1indian.cc", "desibp.com", "desibp.org",
        "desisex.cc", "hindi6.com", "mydesi.net", "desi-xxx.cc", "desigirl.cc",
        "desiporn.su", "desisex.xxx", "desivdo.com", "desixxx.org", "desizex.com",
        "dropmms.com", "dropmms.net", "mallusex.me", "mmsdose.com", "mydesi3.com",
        "tamilma.com", "tamilsex.in", "tamilxxx.me", "videmms.com", "xchudai.com",
        "xxxhindi.to", "3gpindian.me", "69indian.com", "ahindian.com", "beegdesi.com",
        "chudaisex.cc", "desi-cum.com", "desi-porn.me", "desi-xnxx.cc", "desijimo.com",
        "desimaal.net", "desipapa.com", "desipapa.xxx", "desisexo.com", "desisexy.org",
        "desitube.org", "desixnxx.net", "goindian.net", "hindi-xxx.cc", "hindiporn.tv",
        "indianal.com", "indianxxx.su",
        // telegram
        "t.me", "telegram.me", "telegram.org", "telegram.dog", "web.telegram.org", "telegra.ph", "telesco.pe",
        "tgstat.com", "tgstat.ru", "telemetr.io", "tlgrm.me",
        // X / twitter
        "x.com", "twitter.com", "t.co", "mobile.twitter.com", "twimg.com", "nitter.net", "twstalker.com",
        "twitterwebviewer.com",
        // vpn / proxy / bypass
        "nordvpn.com", "expressvpn.com", "surfshark.com", "protonvpn.com", "windscribe.com", "mullvad.net",
        "privateinternetaccess.com", "cyberghostvpn.com", "hide.me", "hidemyass.com", "tunnelbear.com", "ipvanish.com",
        "purevpn.com", "vyprvpn.com", "hotspotshield.com", "psiphon.ca", "torproject.org", "lantern.io",
        "getlantern.org", "openvpn.net", "wireguard.com", "one.one.one.one", "warp.plus",
        "proxysite.com", "hidemy.name", "croxyproxy.com", "kproxy.com", "hide.me", "vpnbook.com", "vpngate.net",
        "freeproxy.win", "proxyium.com", "blockaway.net", "fastvpn.com", "betternet.co", "turbovpn.com",
        "ultrasurf.us", "browsec.com", "zenmate.com", "urban-vpn.com", "hola.org", "setupvpn.com",
        "unblock-websites.com", "unblocked.pw", "unblockproxy.org", "4everproxy.com", "proxyorb.com",
        // bypass: doh / dns / safe-search disablers
        "dns.google", "dns.cloudflare.com", "cloudflare-dns.com", "dns.quad9.net", "dns.adguard.com",
        "doh.opendns.com", "doh.cleanbrowsing.org", "nextdns.io"
    )

    /** One of these (word-start match) is enough to trigger a block. */
    val STRONG: List<String> = listOf(
        "porn", "pornhub", "xvideos", "xnxx", "xhamster", "redtube", "youporn", "spankbang", "hentai", "xxx+",
        "onlyfans", "chaturbate", "stripchat", "livejasmin", "bongacams", "camsoda", "myfreecams",
        "blowjob", "handjob", "rimjob", "footjob", "creampie", "cumshot", "gangbang", "bukkake", "milf",
        "sex video", "sex videos", "sex tape", "sex cam", "sex chat", "live sex", "free sex", "sex movie",
        "sex movies", "nude video", "nude videos", "nude pics", "nude photos", "nude girls", "naked girls",
        "naked women", "cam girl", "cam girls", "camgirl", "camgirls", "nudes leak", "leaked nudes",
        "rule34", "rule 34", "jerk off", "jerking off", "masturbat", "threesome", "orgy", "incest", "bdsm",
        "dildo", "pussy", "deepthroat", "titfuck", "cunnilingus", "fellatio", "ejaculat", "erotic massage",
        "happy ending massage", "escort service", "call girl", "lolicon", "shotacon", "jailbait", "onlyfans leak",
        "kamababa", "desileaks", "thothub", "desi mms", "viral mms", "leaked mms",
        "fansly leak", "milfs", "xxx videos", "adult video", "adult videos", "hardcore sex", "anal sex",
        "oral sex", "sex scene", "sex scenes", "uncensored sex"
    )

    /** Each is worth 1 point (whole word). Three distinct mild words = block. */
    val MILD: List<String> = listOf(
        "sex", "sexy", "sexual", "nude", "nudes", "naked", "nsfw", "erotic", "erotica", "fetish", "boobs",
        "boobies", "tits", "titties", "horny", "orgasm", "stripper", "strippers", "escort", "escorts",
        "lingerie", "topless", "bottomless", "cleavage", "kinky", "slut", "whore", "hookup", "hookups",
        "sexting", "vibrator", "anal", "cock", "dick", "pussy", "fap", "fapping", "r34", "18+", "adults only",
        "hot girls", "thot", "thots", "booty", "twerk", "twerking", "camgirl", "dating app", "no strings",
        "nipple", "nipples", "playmate", "bikini", "lust", "seduce", "seductive", "xxx rated"
    )

    /** Apps hidden outright (package prefixes). */
    val HIDE_PACKAGE_PREFIXES: List<String> = listOf(
        "org.telegram.", "org.thunderdog.challegram", "nekox.", "tw.nekomimi.", "xyz.nextalone.", "com.exteragram.",
        "org.telegram.plus", "it.owlgram.", "ir.ilmili.", "com.twitter.", "com.x.android", "tv.periscope.",
        "app.nicegram", "org.forkgram", "com.nekogram", "com.reddit.", "com.discord",
        "org.telegram", "org.thunderdog", "uz.unnarsx", "com.radolyn", "ellipi.messenger", "org.ushastoe",
        "com.cool2645.nekolite", "org.aka.messenger", "com.gram", "ir.hotgram", "com.telegraph",
        "com.twitter", "com.twidere", "org.mariotaku", "com.tweetcaster", "com.handmark.tweetcaster",
        "com.levelup.touiteur", "com.klinker.android.twitter", "com.sw.twitter", "com.yinzcam.twitter",
        "ru.zdevs", "com.laurencedawson.reddit_sync", "free.reddit.news", "reddit.news", "com.andrewshu.android.reddit",
        "ml.docilealligator.infinityforreddit", "com.onelouder.baconreader", "com.rubenmayayo.reddit",
        "me.ccrama.redditslide", "com.ccrama.redditslide", "com.reddit.frontpage", "io.syncapps.lemmy_sync"
    )

    /** Reddit and Discord apps/clients (Reddit in a browser is allowed). */
    val HIDE_PACKAGE_REGEX = Regex("reddit|discord|telegram|nekogram|nekox|plusmessenger|twitter|tweet|redditslide|infinityforreddit|baconreader", RegexOption.IGNORE_CASE)

    /** Apps hidden when their label matches. Case-insensitive. */
    val HIDE_LABEL_REGEX = Regex(
        "^x$|^x lite$|\\btwitter\\b|\\btelegram\\b|nekogram|challegram|exteragram|nagram|nicegram|\\bowlgram\\b|\\bunigram\\b|\\bplus messenger\\b|reddit|discord|tele ?gram|\\bgram\\b|messenger plus|\\bturrit\\b|\\btweet|\\btalon\\b|\\bfenix\\b|\\bsync for reddit|\\bboost for reddit|\\bslide for reddit|\\binfinity for reddit|\\bbacon reader|\\brelay for reddit|\\bjoey\\b|\\bnarwhal\\b|\\bx beta\\b|\\bgraph messenger|\\bkutegram|\\bmandrill|\\bsignal ?gram",
        RegexOption.IGNORE_CASE
    )

    /** Packages never treated as VPNs (Brave declares a VpnService for its shields). */
    val NOT_VPN: List<String> = listOf("com.brave.browser", "com.brave.browser_beta", "com.brave.browser_nightly")

    /** VPN / proxy / tunnel / Tor / DNS-changer apps, matched against label and package name. */
    val VPN_REGEX = Regex(
        "vpn|proxy|proxies|tunnel|\\btor\\b|orbot|onion|psiphon|wireguard|openvpn|shadowsocks|v2ray|xray|trojan|" +
            "\\bclash\\b|clash.?(meta|for android)|\\boutline\\b|lantern|hotspot ?shield|ultrasurf|\\bhola\\b|" +
            "dns ?changer|\\bdns\\b|private ?dns|1\\.1\\.1\\.1|cloudflare\\.onedot|\\bwarp\\b|\\bnetguard\\b|" +
            "adguard|\\bblokada\\b|\\bintra\\b|\\bnextdns\\b|\\bwindscribe\\b|\\bmullvad\\b|\\bsurfshark\\b|" +
            "\\bsnap ?vpn\\b|\\bbetternet\\b|\\bbrowsec\\b|\\bzenmate\\b|\\bsetupvpn\\b|\\bturbo\\b.*\\bvpn\\b|" +
            "unblock|bypass|tethering|hotspot",
        RegexOption.IGNORE_CASE
    )

    /** Dual-space / cloner / work-profile style apps used to dodge hidden-app policies. */
    val CLONE_REGEX = Regex(
        "parallel ?space|dual ?space|app ?cloner|clone ?app|\\bcloner\\b|multiple ?accounts|2accounts|^island$|" +
            "^shelter$|insular|second ?space|dual ?apps?|ml manager|lucky ?patcher|\\blspatch\\b|\\bmagisk\\b",
        RegexOption.IGNORE_CASE
    )

    /** Browsers allowed by default (they receive managed policies: incognito off, blocklist, safe search). */
    val BROWSER_ALLOW: List<String> = listOf("com.android.chrome", "com.brave.browser", "com.brave.browser_beta", "com.brave.browser_nightly")

    /** Browsers that get managed restrictions pushed. Package -> uses Chrome policy names. */
    val MANAGED_BROWSERS: List<String> = listOf(
        "com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.chrome.canary",
        "com.microsoft.emmx", "com.brave.browser", "com.brave.browser_beta", "com.brave.browser_nightly"
    )

    /** Never hide these, whatever the scanners say. */
    val NEVER_HIDE_PREFIXES: List<String> = listOf(
        "android", "com.android.settings", "com.android.systemui", "com.android.vending", "com.google.android.gms",
        "com.google.android.gsf", "com.google.android.googlequicksearchbox", "com.google.android.webview",
        "com.android.webview", "com.google.android.permissioncontroller", "com.android.permissioncontroller",
        "com.android.phone", "com.android.server.telecom", "com.android.dialer", "com.google.android.dialer",
        "com.android.packageinstaller", "com.google.android.packageinstaller", "com.android.providers.",
        "com.android.inputmethod", "com.google.android.inputmethod", "com.samsung.android.honeyboard",
        "com.android.launcher", "com.google.android.apps.nexuslauncher", "com.sec.android.app.launcher",
        "com.android.certinstaller", "com.android.shell", "com.android.nfc", "com.android.bluetooth",
        "com.google.android.apps.messaging", "com.android.mms", "com.samsung.android.messaging",
        "com.android.emergency", "com.google.android.ext.services", "com.google.android.networkstack",
        "com.android.networkstack", "com.android.captiveportallogin", "com.google.android.captiveportallogin",
        "com.android.cellbroadcast", "com.google.android.cellbroadcast"
    )

    /** Apps hidden only during the night curfew. */
    /** Apps blocked the moment they open (overlay), as well as hidden. TikTok included. */
    val OPEN_BLOCK_REGEX = Regex(
        "telegram|nekogram|nekox|plusmessenger|challegram|exteragram|twitter|com\\.x\\.android|tweet|reddit|discord|musically|ugc\\.trill|tiktok|redditslide|infinityforreddit|baconreader",
        RegexOption.IGNORE_CASE
    )

    val CURFEW_APPS: List<String> = listOf(
        "com.instagram.android", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill"
    )

    /** Downloaded lists can contain mistakes; these are never blocked by them. */
    val NEVER_BLOCK: Set<String> = setOf(
        "google.com", "gstatic.com", "googleapis.com", "youtube.com", "wikipedia.org", "github.com",
        "githubusercontent.com", "microsoft.com", "apple.com", "amazon.com", "cloudflare.com", "reddit.com",
        "facebook.com", "instagram.com", "whatsapp.com", "tiktok.com", "bing.com", "yahoo.com",
        "cleanbrowsing.org", "nhs.uk", "gov.uk", "bbc.co.uk", "bbc.com", "android.com", "mozilla.org"
    )
}
