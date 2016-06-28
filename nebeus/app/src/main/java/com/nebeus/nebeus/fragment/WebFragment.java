package com.nebeus.nebeus.fragment;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.WebSettings.PluginState;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.jaredrummler.android.device.DeviceName;
import com.nebeus.nebeus.App;
import com.nebeus.nebeus.BuildConfig;
import com.nebeus.nebeus.Config;
import com.nebeus.nebeus.GetFileInfo;
import com.nebeus.nebeus.R;
import com.nebeus.nebeus.activity.MainActivity;
import com.nebeus.nebeus.widget.AdvancedWebView;
import com.nebeus.nebeus.widget.scrollable.ToolbarWebViewScrollListener;
import com.nebeus.nebeus.widget.webview.WebToAppChromeClient;
import com.nebeus.nebeus.widget.webview.WebToAppWebClient;
import com.onesignal.OneSignal;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;

public class WebFragment extends Fragment implements AdvancedWebView.Listener, SwipeRefreshLayout.OnRefreshListener {

    public FrameLayout rl;
    public AdvancedWebView browser;
    public SwipeRefreshLayout swipeLayout;
    public ProgressBar progressBar;

    public WebToAppChromeClient chromeClient;
    public WebToAppWebClient webClient;

    public String mainUrl = null;
    static String URL = "url";
    public int firstLoad = 0;

    private String languagePrefix;
    private int deviceType;
    private int timeZone;
    private String versionName;
    private String modelName;
    private String osVersion;
    private String registrationID;

    public WebFragment() {
        // Required empty public constructor
    }

    public static WebFragment newInstance(String url) {
        WebFragment fragment = new WebFragment();
        Bundle args = new Bundle();
        args.putString(URL, url);
        fragment.setArguments(args);
        return fragment;
    }

    public void setBaseUrl(String url) {
        this.mainUrl = url;
        browser.loadUrl(mainUrl);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null && mainUrl == null) {
            mainUrl = getArguments().getString(URL);
            firstLoad = 0;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rl = (FrameLayout) inflater.inflate(R.layout.fragment_observable_web_view, container,
                false);

        progressBar = (ProgressBar) rl.findViewById(R.id.progressbar);
        browser = (AdvancedWebView) rl.findViewById(R.id.scrollable);
        swipeLayout = (SwipeRefreshLayout) rl.findViewById(R.id.swipe_container);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            browser.setWebContentsDebuggingEnabled(true);
        }

        return rl;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (Config.PULL_TO_REFRESH)
            swipeLayout.setOnRefreshListener(this);
        else
            swipeLayout.setEnabled(false);

        // Setting the webview listeners
        browser.setListener(this, this);

        // Setting the scroll listeners (if applicable)
        if (MainActivity.getCollapsingActionBar()) {

            ((MainActivity) getActivity()).showToolbar(this);

            browser.setOnScrollChangeListener(browser, new ToolbarWebViewScrollListener() {
                @Override
                public void onHide() {
                    ((MainActivity) getActivity()).hideToolbar();
                }

                @Override
                public void onShow() {
                    ((MainActivity) getActivity()).showToolbar(WebFragment.this);
                }
            });

        }

        // set javascript and zoom and some other settings
        browser.requestFocus();
        browser.getSettings().setJavaScriptEnabled(true);
        browser.getSettings().setBuiltInZoomControls(false);
        browser.getSettings().setAppCacheEnabled(true);
        browser.getSettings().setDatabaseEnabled(true);
        browser.getSettings().setDomStorageEnabled(true);
        // Below required for geolocation
        browser.setGeolocationEnabled(true);
        // 3RD party plugins (on older devices)
        browser.getSettings().setPluginState(PluginState.ON);

        if (Config.MULTI_WINDOWS) {
            browser.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
            browser.getSettings().setSupportMultipleWindows(true);
        }

        webClient = new WebToAppWebClient(getActivity(), browser);
        browser.setWebViewClient(webClient);

        chromeClient = new WebToAppChromeClient(getActivity(), rl, browser, swipeLayout, progressBar);
        browser.setWebChromeClient(chromeClient);

        // load url (if connection available
        if (webClient.hasConnectivity(mainUrl, true)) {
            String pushurl = ((App) getActivity().getApplication()).getPushUrl();
            if (pushurl != null) {
                browser.loadUrl(pushurl);
            } else {
                browser.loadUrl(mainUrl);
            }

        }

        //Call fill required data method
        collectInfo();
    }

    @Override
    public void onRefresh() {
        browser.reload();
    }

    @SuppressLint("NewApi")
    @Override
    public void onPause() {
        super.onPause();
        browser.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        browser.onDestroy();
    }

    @SuppressLint("NewApi")
    @Override
    public void onResume() {
        super.onResume();
        browser.onResume();

        //If push url available load it
        if (webClient.hasConnectivity(mainUrl, true)) {
            String pushurl = ((App) getActivity().getApplication()).getPushUrl();
            if (pushurl != null) {
                browser.loadUrl(pushurl);
            }
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onDownloadRequested(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {

        String filename = null;
        try {
            filename = new GetFileInfo().execute(url).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        if (filename == null) {
            String fileExtenstion = MimeTypeMap.getFileExtensionFromUrl(url);
            filename = URLUtil.guessFileName(url, null, fileExtenstion);
        }

        if (AdvancedWebView.handleDownload(getActivity(), url, filename)) {
            Toast.makeText(getActivity(), getResources().getString(R.string.download_done), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getActivity(), getResources().getString(R.string.download_fail), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPageStarted(String url, Bitmap favicon) {
        if (firstLoad == 0 && MainActivity.getCollapsingActionBar()) {
            ((MainActivity) getActivity()).showToolbar(this);
            firstLoad = 1;
        } else if (firstLoad == 0) {
            firstLoad = 1;
        }
    }

    @Override
    public void onPageFinished(String url) {
        // TODO Auto-generated method stub
        //Inject remote CSS
        injectCSS();

        if (url.contains("nebeus.com/signin")||url.contains("nebeus.com/signup")) {
            //Send tags to OneSignal
//            sendTags();

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                browser.evaluateJavascript("javascript:document.getElementsByName('language')[0].value = '" + languagePrefix + "';", null);
                browser.evaluateJavascript("javascript:document.getElementsByName('device_type')[0].value = '" + deviceType + "';", null);
                browser.evaluateJavascript("javascript:document.getElementsByName('timezone')[0].value = '" + timeZone + "';", null);
                browser.evaluateJavascript("javascript:document.getElementsByName('app_version')[0].value = '" + versionName + "';", null);
                browser.evaluateJavascript("javascript:document.getElementsByName('device_model')[0].value = '" + modelName + "';", null);
                browser.evaluateJavascript("javascript:document.getElementsByName('device_os')[0].value = '" + osVersion + "';", null);
                browser.evaluateJavascript("javascript:document.getElementsByName('identifier')[0].value = '" + registrationID + "';", null);
            } else {
                browser.loadUrl("javascript:document.getElementsByName('language')[0].value = '" + languagePrefix + "';");
                browser.loadUrl("javascript:document.getElementsByName('device_type')[0].value = '" + deviceType + "';");
                browser.loadUrl("javascript:document.getElementsByName('timezone')[0].value = '" + timeZone + "';");
                browser.loadUrl("javascript:document.getElementsByName('app_version')[0].value = '" + versionName + "';");
                browser.loadUrl("javascript:document.getElementsByName('device_model')[0].value = '" + modelName + "';");
                browser.loadUrl("javascript:document.getElementsByName('device_os')[0].value = '" + osVersion + "';");
                browser.loadUrl("javascript:document.getElementsByName('identifier')[0].value = '" + registrationID + "';");
            }
        }
    }

    @Override
    public void onPageError(int errorCode, String description, String failingUrl) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onExternalPageRequest(String url) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        browser.onActivityResult(requestCode, resultCode, data);
    }

    // sharing
    public void shareURL() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        String appname = getString(R.string.app_name);
        // This will put the share text:
        // "I came across "BrowserTitle" using "appname"
        shareIntent
                .putExtra(
                        Intent.EXTRA_TEXT,
                        (getText(R.string.share1)
                                + " "
                                + browser.getTitle()
                                + " "
                                + getText(R.string.share2)
                                + " "
                                + appname
                                + " https://play.google.com/store/apps/details?id=" + getActivity()
                                .getPackageName()));
        startActivity(Intent.createChooser(shareIntent,
                getText(R.string.sharetitle)));
    }

    //Collect required data
    public void collectInfo() {
        //Get datas to fill
        languagePrefix = Locale.getDefault().getDisplayLanguage();
        deviceType = 1;
        timeZone = getUTCOffset();
        versionName = BuildConfig.VERSION_NAME;
        modelName = DeviceName.getDeviceName();
        osVersion = Build.VERSION.RELEASE;

        OneSignal.idsAvailable(new OneSignal.IdsAvailableHandler() {
            @Override
            public void idsAvailable(String userId, String registrationId) {
                Log.d("debug", "User:" + userId);
                if (registrationId != null)
                    registrationID = registrationId;
                    Log.d("debug", "registrationId:" + registrationId);
            }
        });
    }

    // Inject CSS method: read style.css from assets folder
// Append stylesheet to document head
    private void injectCSS(){
        try {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                browser.evaluateJavascript("javascript:(function() {" +
                        "var parent = document.getElementsByTagName('head').item(0);" +
                        "var style = document.createElement('link');" +
                        "style.type = 'text/css';" +
                        "style.rel = 'stylesheet';" +
                        "style.href = 'https://cdn.nebeus.com/assets/application.app.css';" +
                        "parent.appendChild(style)" +
                        "})()",null);

                browser.evaluateJavascript("javascript:(function() {" +
                        "var parent = document.getElementsByTagName('head').item(0);" +
                        "var script = document.createElement('link');" +
                        "script.type = 'text/javascript';" +
                        "script.rel = 'script';" +
                        "script.href = 'https://cdn.nebeus.com/assets/application.app.js';" +
                        "parent.appendChild(script)" +
                        "})()",null);
            }else{
                browser.loadUrl("javascript:(function() {" +
                        "var parent = document.getElementsByTagName('head').item(0);" +
                        "var style = document.createElement('link');" +
                        "style.type = 'text/css';" +
                        "style.rel = 'stylesheet';" +
                        "style.href = 'https://cdn.nebeus.com/assets/application.app.css';" +
                        "parent.appendChild(style)" +
                        "})()");

                browser.loadUrl("javascript:(function() {" +
                        "var parent = document.getElementsByTagName('head').item(0);" +
                        "var script = document.createElement('link');" +
                        "script.type = 'text/javascript';" +
                        "script.rel = 'script';" +
                        "script.href = 'https://cdn.nebeus.com/assets/application.app.js';" +
                        "parent.appendChild(script)" +
                        "})()");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //Send tags to OneSignal
    private void sendTags(){
        new Thread() {
            @Override
            public void run() {
                JSONObject tags = new JSONObject();
                try {
                    tags.put("device_type", deviceType);
                    tags.put("identifier", registrationID);
                    tags.put("language", languagePrefix);
                    tags.put("timezone", timeZone);
                    tags.put("app_version", versionName);
                    tags.put("device_model", modelName);
                    tags.put("device_os", osVersion);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                OneSignal.sendTags(tags);
            }
        }.start();
    }

    //Get Offset from UTC
    private int getUTCOffset(){
        TimeZone tz = TimeZone.getDefault();
        Date now = new Date();
        int offsetFromUtc = tz.getOffset(now.getTime()) / 1000;

        return offsetFromUtc;
    }
}
