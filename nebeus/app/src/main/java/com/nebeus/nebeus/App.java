package com.nebeus.nebeus;

import android.app.Application;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.onesignal.OneSignal;

import org.json.JSONObject;

public class App extends Application { 
	
	  public static GoogleAnalytics analytics;
	  public static Tracker tracker;

      private String push_url = null;

    @Override public void onCreate() {
        super.onCreate();

        if (Config.ANALYTICS_ID.length() > 0) {
            analytics = GoogleAnalytics.getInstance(this);
            analytics.setLocalDispatchPeriod(1800);

            tracker = analytics.newTracker(Config.ANALYTICS_ID); // Replace with actual tracker/property Id
            tracker.enableExceptionReporting(true);
            tracker.enableAdvertisingIdCollection(true);
            tracker.enableAutoActivityTracking(true);
        }

        //OneSignal Push
        OneSignal.startInit(this)
                .setNotificationOpenedHandler(new NotificationHandler())
                .init();

    }

    // This fires when a notification is opened by tapping on it or one is received while the app is running.
    private class NotificationHandler implements OneSignal.NotificationOpenedHandler {
        @Override
        public void notificationOpened(String message, JSONObject additionalData, boolean isActive) {
            try {
                //If the app is not on foreground, clicking the notification will start the app, and push_url will be used.
                if (!isActive) {
                    if (additionalData != null && additionalData.has("nburl")) {
                        push_url = additionalData.getString("nburl");
                    }
                } else { //If the app is in foreground, don't interup the current activities, but open webview in a new window.
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(additionalData.getString("nburl")));
                    startActivity(browserIntent);
                    Log.v("INFO", "Received notification while app was on foreground");
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    public synchronized String getPushUrl(){
        String url = push_url;
        push_url = null;
        return url;
    }

    public synchronized void setPushUrl(String url){
        this.push_url = url;
    }
} 