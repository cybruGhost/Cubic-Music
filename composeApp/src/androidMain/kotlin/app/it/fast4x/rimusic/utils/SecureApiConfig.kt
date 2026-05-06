package app.it.fast4x.rimusic.utils

object SecureApiConfig {
    private const val API_URL =
        "https://dywyagjcuvxtjgtksyjn.supabase.co/functions/v1/app-config" +
        "?app=cubic_music&key=system_notification"

    private fun reveal(vararg fragments: String): String = buildString {
        fragments.forEach { append(it.reversed()) }
    }

    val cubicAppConfigBaseUrl: String by lazy {
        reveal("//:sptth", "njysktgjtxvucjgaywyd", "oc.esabapus.", "/1v/snoitcnuf/", "gifnoc-ppa")
    }

    val cubicNotificationConfigUrl: String by lazy {
        "$cubicAppConfigBaseUrl?app=cubic_music&key=notification"
    }

    val cubicSystemNotificationConfigUrl: String
        get() = API_URL

    val ytmSessionEndpoint: String by lazy {
        "https://ytm-cookie-sparkle.lovable.app/api/ytm-session"
    }

    val spotifyCanvasApi: String by lazy {
        reveal("tops//:sptth", "ammag-ipayfi", "/ppa.lecrev.", "savnac/ipa")
    }

    val spotifyMatchApi: String by lazy {
        reveal("ynhs//:sptth", "foepzltvojod", "sabapus.qgtz", "oitcnuf/oc.e", "fitops/1v/sn", "hctam-y")
    }

    val spotifyMatchApiKey: String by lazy {
        reveal(
            "IJiOicGbhJye", "5RnIsIiN1IzU", "9JCVXpkI6ICc", "JiOiM3cpJye.",
            "ISZzFmYhBXdz", "NnI6IiZlJnIs", "Rndvp2bklnbo", "dGd6Z2blBnes",
            "ISZs9mciwiIx", "JCLi42buFmI6", "kjN3EjOiQXYp", "VmIsQzNwYzN2",
            "ITN4AjM6ICc4", "bzL.0HN3AjM1", "-GAGpLY7Ab-V", "-eVknmKBrvYL",
            "gcXSHSulDN__", "EO0t"
        )
    }

    val spotifySecretsUrl: String by lazy {
        reveal(
            ".war//:sptth", "ocresubuhtig", "yx/moc.tnetn", "tops/ekalfol",
            "/og-sterces-", "m/sdaeh/sfer", "/sterces/nia", "j.tciDterces", "nos"
        )
    }

    val spotifyServerTimeUrl: String by lazy {
        reveal("nepo//:sptth", "moc.yfitops.", "-revres/ipa/", "emit")
    }

    val spotifyTokenUrl: String by lazy {
        reveal("nepo//:sptth", "moc.yfitops.", "nekot/ipa/")
    }

    val spotifyWebAccessTokenUrl: String by lazy {
        reveal("nepo//:sptth", "moc.yfitops.", "_ssecca_teg/", "nekot")
    }

    val spotifySearchUrl: String by lazy {
        reveal(".ipa//:sptth", "/moc.yfitops", "hcraes/1v")
    }

    val shazamBaseUrl: String by lazy {
        reveal(".pma//:sptth", "/moc.mazahs")
    }

    val shazamProxyUrl: String by lazy {
        reveal("vfxx//:sptth", "dylniimcuzga", "sabapus.dhnc", "oitcnuf/oc.e", "mazahs/1v/sn", "yxorp-")
    }

    val shazamProxyApiKey: String by lazy {
        reveal(
            "IJiOicGbhJye", "5RnIsIiN1IzU", "9JCVXpkI6ICc", "JiOiM3cpJye.",
            "ISZzFmYhBXdz", "hnI6IiZlJnIs", "12Y1p3ZhZnZ4", "hmbjRWes5Wap",
            "ISZs9mciwiIk", "JCLi42buFmI6", "UzN3EjOiQXYp", "VmIsAjM2QzM3",
            "MTM5AjM6ICc4", "OM-.0HMyYDMx", "XXDYiioNgy_J", "LG5HsH-c9J-R",
            "Q21br6GJa94w", "UUIx"
        )
    }

    val weatherConfigUrl: String by lazy {
        reveal("tsez//:sptth", "8e-kivodem-y", "yfilten.24f8", "kmatnaw/ppa.", ".ehehukisali", "nosj")
    }

    val weatherApiBaseUrl: String by lazy {
        reveal(".ipa//:sptth", "mrehtaewnepo", "/atad/gro.pa", "rehtaew/5.2")
    }

    val ipInfoUrl: String by lazy {
        reveal("nipi//:sptth", "/nosj/oi.of")
    }
}
