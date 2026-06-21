package com.pause.app.domain.model

/**
 * The fixed set of attention-heavy apps Pause can interrupt. Package names are the stable,
 * documented application IDs of each app's primary global build. Because the list is fixed
 * Pause needs no QUERY_ALL_PACKAGES permission — it never enumerates installed apps.
 */
object AppCatalog {

    val apps: List<AppDefinition> = listOf(
        AppDefinition("com.instagram.android", "Instagram", 0xFFE1306C),
        AppDefinition("com.zhiliaoapp.musically", "TikTok", 0xFF111114),
        AppDefinition("com.google.android.youtube", "YouTube", 0xFFFF0000),
        AppDefinition("com.reddit.frontpage", "Reddit", 0xFFFF4500),
        AppDefinition("com.twitter.android", "X", 0xFF14171A),
        AppDefinition("com.facebook.katana", "Facebook", 0xFF1877F2),
        AppDefinition("com.snapchat.android", "Snapchat", 0xFFFFD400),
    )

    private val byPackage: Map<String, AppDefinition> = apps.associateBy { it.packageName }

    fun definitionFor(packageName: String): AppDefinition? = byPackage[packageName]

    /** Human label for a package, falling back to the raw package name if it isn't in the catalog. */
    fun labelFor(packageName: String): String = byPackage[packageName]?.label ?: packageName
}
