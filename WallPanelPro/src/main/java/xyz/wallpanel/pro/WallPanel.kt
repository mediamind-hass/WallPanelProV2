/*
 * Copyright (c) 2022 WallPanel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.wallpanel.pro

import android.content.ComponentCallbacks2
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.multidex.MultiDex
import androidx.preference.PreferenceManager
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication
import org.mozilla.geckoview.GeckoRuntime
import timber.log.Timber
import xyz.wallpanel.pro.di.DaggerApplicationComponent
import xyz.wallpanel.pro.utils.LauncherShortcuts

class WallPanel : DaggerApplication() {

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerApplicationComponent.builder().create(this)
    }

    override fun onCreate() {
        super.onCreate()
        
        // Apply dark theme setting on app startup
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val useDarkTheme = sharedPreferences.getBoolean("pref_dark_theme", false)
        if (useDarkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
        
        LauncherShortcuts.createShortcuts(this)
    }

    /**
     * GeckoView hands a crash report to [GeckoCrashHandlerService] with
     * startForegroundService() on the application context, on the main thread, and does not
     * catch the refusal Android 12 and above give an application that is in the background.
     * Uncaught, that refusal would take down the browser while it is recovering from the crash.
     * Losing the report is harmless, since the browser's recovery does not depend on it.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun startForegroundService(service: Intent): ComponentName? {
        return try {
            super.startForegroundService(service)
        } catch (e: IllegalStateException) {
            if (service.action != GeckoRuntime.ACTION_CRASHED) {
                throw e
            }
            Timber.w(e, "Unable to start the GeckoView crash handler")
            null
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        System.gc()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when (level) {
            TRIM_MEMORY_UI_HIDDEN,
            TRIM_MEMORY_RUNNING_MODERATE,
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_RUNNING_CRITICAL,
            TRIM_MEMORY_BACKGROUND,
            TRIM_MEMORY_MODERATE,
            TRIM_MEMORY_COMPLETE -> {
                Runtime.getRuntime().gc()
            }
            else -> {
                Timber.w("Unrecognized memory level: $level")
            }
        }
    }


}
