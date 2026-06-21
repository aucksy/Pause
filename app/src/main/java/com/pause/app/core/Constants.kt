package com.pause.app.core

/** App-wide constants kept in one place. */
object Constants {
    /** File name for the Preferences DataStore that holds the user's choices. */
    const val DATASTORE_NAME = "pause_settings"

    /** Default interruption interval the onboarding pre-selects. */
    const val DEFAULT_INTERVAL_MINUTES = 15

    /** How long the overlay's Continue button stays disabled, in seconds. */
    const val CONTINUE_COUNTDOWN_SECONDS = 3
}
