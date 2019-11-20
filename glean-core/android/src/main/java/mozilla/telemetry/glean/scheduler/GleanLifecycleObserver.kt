/* This Source Code Form is subject to the terms of the Mozilla Public
* License, v. 2.0. If a copy of the MPL was not distributed with this
* file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package mozilla.telemetry.glean.scheduler

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import mozilla.telemetry.glean.Glean
import mozilla.telemetry.glean.GleanMetrics.GleanBaseline

/**
 * Connects process lifecycle events from Android to Glean's handleEvent
 * functionality (where the actual work of sending pings is done).
 */
internal class GleanLifecycleObserver : DefaultLifecycleObserver {
    /**
     * Calls the "background" event when entering the background.
     */
    override fun onStop(owner: LifecycleOwner) {
        // We're going to background, so store how much time we spent
        // on foreground.
        GleanBaseline.duration.stop()
        Glean.handleBackgroundEvent()
    }

    /**
     * Updates the baseline.duration metric when entering the foreground.
     * We use ON_START here because we don't want to incorrectly count metrics in ON_RESUME as
     * pause/resume can happen when interacting with things like the navigation shade which could
     * lead to incorrectly recording the start of a duration, etc.
     *
     * https://developer.android.com/reference/android/app/Activity.html#onStart()
     */
    override fun onStart(owner: LifecycleOwner) {
        // Note that this is sending the length of the last foreground session
        // because it belongs to the baseline ping and that ping is sent every
        // time the app goes to background.
        GleanBaseline.duration.start()
    }
}