package com.pause.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Application entry point — wires Hilt. Everything else is event-driven, so there is no other startup work. */
@HiltAndroidApp
class PauseApp : Application()
