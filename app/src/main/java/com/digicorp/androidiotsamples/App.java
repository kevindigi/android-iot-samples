package com.digicorp.androidiotsamples;

import android.app.Application;
import android.util.Log;

import timber.log.Timber;

/**
 * Created by kevin.adesara on 04/07/17.
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Timber.plant(new CrashReportingTree());
        }
    }

    /**
     * A tree which logs important information for crash reporting.
     */
    private static class CrashReportingTree extends Timber.Tree {
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG) {
                return;
            }

            FakeCrashLibrary.log(priority, tag, message);

            if (t != null) {
                if (priority == Log.ERROR) {
                    FakeCrashLibrary.logError(t);
                } else if (priority == Log.WARN) {
                    FakeCrashLibrary.logWarning(t);
                }
            }
        }
    }

    private static final class FakeCrashLibrary {
        private FakeCrashLibrary() {
            throw new AssertionError("No instances.");
        }

        static void log(int priority, String tag, String message) {
            // TODO add log entry to circular buffer.
        }

        static void logWarning(Throwable t) {
            // TODO report non-fatal warning.
        }

        static void logError(Throwable t) {
            // TODO report non-fatal error.
        }
    }
}
