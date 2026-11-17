package com.motoroll.home;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;

/**
 * Optional recovery helper for operator boxes that reopen their own HOME app.
 *
 * This service only reads the package name attached to window-state events.
 * It does not retrieve window content, text, passwords, or UI nodes.
 */
public class LauncherRecoveryAccessibilityService extends AccessibilityService {
    private static final String PREFS = "motoroll_tv";
    private static final String PREF_HOME_SETUP_REQUESTED = "home_setup_requested";
    private static final long RECOVERY_COOLDOWN_MS = 12000L;

    private long lastRecoveryAttempt = 0L;
    private boolean recoveryHasBeenLaunchedForCurrentHome = false;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return;
        }

        CharSequence packageSequence = event.getPackageName();
        if (packageSequence == null) return;
        String eventPackage = packageSequence.toString();

        // Seeing MOTOROLL means our recovery activity is now in front.
        // Wait until the user leaves it before attempting another recovery.
        if (getPackageName().equals(eventPackage)) {
            recoveryHasBeenLaunchedForCurrentHome = true;
            return;
        }

        // Never interfere unless the user previously asked to make MOTOROLL HOME.
        if (!getSharedPreferences(PREFS, MODE_PRIVATE)
                .getBoolean(PREF_HOME_SETUP_REQUESTED, false)) {
            recoveryHasBeenLaunchedForCurrentHome = false;
            return;
        }

        String selectedHomePackage = getSelectedHomePackage();
        if (selectedHomePackage == null || getPackageName().equals(selectedHomePackage)) {
            recoveryHasBeenLaunchedForCurrentHome = false;
            return;
        }

        // Ignore all normal apps. React only when Android opens the app currently
        // registered as the device's HOME activity (for example, the operator launcher).
        if (!selectedHomePackage.equals(eventPackage)) {
            recoveryHasBeenLaunchedForCurrentHome = false;
            return;
        }

        if (recoveryHasBeenLaunchedForCurrentHome) return;

        long now = SystemClock.elapsedRealtime();
        if (now - lastRecoveryAttempt < RECOVERY_COOLDOWN_MS) return;

        lastRecoveryAttempt = now;
        recoveryHasBeenLaunchedForCurrentHome = true;

        try {
            Intent recover = new Intent(this, TvActivity.class);
            recover.setAction("com.motoroll.tv.action.RECOVER_HOME");
            recover.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(recover);
        } catch (Exception ignored) {
            // Keep the service quiet if this firmware blocks background launches.
            recoveryHasBeenLaunchedForCurrentHome = false;
        }
    }

    private String getSelectedHomePackage() {
        try {
            Intent home = new Intent(Intent.ACTION_MAIN);
            home.addCategory(Intent.CATEGORY_HOME);
            home.addCategory(Intent.CATEGORY_DEFAULT);

            ResolveInfo resolved = getPackageManager().resolveActivity(
                    home, PackageManager.MATCH_DEFAULT_ONLY);

            if (resolved == null || resolved.activityInfo == null) return null;
            return resolved.activityInfo.packageName;
        } catch (Exception ignored) {
            return null;
        }
    }

    @Override
    public void onInterrupt() {
        // No spoken or haptic feedback is produced by this helper.
    }
}
