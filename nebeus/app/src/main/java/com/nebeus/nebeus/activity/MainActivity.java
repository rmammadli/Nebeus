package com.nebeus.nebeus.activity;

import com.nebeus.nebeus.App;
import com.nebeus.nebeus.Config;
import com.nebeus.nebeus.R;
import com.nebeus.nebeus.drawer.DrawerFragment;
import com.nebeus.nebeus.widget.SwipeableViewPager;
import com.tjeannin.apprate.AppRate;

import com.nebeus.nebeus.adapter.NavigationAdapter;
import com.nebeus.nebeus.fragment.WebFragment;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import android.app.AlertDialog;
import android.content.res.Configuration;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.MenuInflater;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity implements DrawerFragment.DrawerFragmentListener{

    //Views
    public Toolbar mToolbar;
    public View mHeaderView;
    public TabLayout mSlidingTabLayout;
    public SwipeableViewPager mViewPager;

    private NavigationAdapter mAdapter;
    private DrawerFragment drawerFragment;

    private WebFragment CurrentAnimatingFragment = null;
    private int CurrentAnimation = 0;

    private static int NO = 0;
    private static int HIDING = 1;
    private static int SHOWING = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mHeaderView = (View) findViewById(R.id.header_container);
        mSlidingTabLayout = (TabLayout) findViewById(R.id.tabs);
        mViewPager = (SwipeableViewPager) findViewById(R.id.pager);

        setSupportActionBar(mToolbar);

        mAdapter = new NavigationAdapter(getSupportFragmentManager(), this);

        final Intent intent = getIntent();
        final String action = intent.getAction();

        if (Intent.ACTION_VIEW.equals(action)) {
            String data = intent.getDataString();
            ((App) getApplication()).setPushUrl(data);
        }

        if (Config.HIDE_ACTIONBAR)
            getSupportActionBar().hide();

        if (getHideTabs())
            mSlidingTabLayout.setVisibility(View.GONE);

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) mViewPager.getLayoutParams();
        if ((Config.HIDE_ACTIONBAR && getHideTabs()) || ((Config.HIDE_ACTIONBAR || getHideTabs()) && getCollapsingActionBar())){
            lp.topMargin = 0;
        } else if ((Config.HIDE_ACTIONBAR || getHideTabs()) || (!Config.HIDE_ACTIONBAR && !getHideTabs() && getCollapsingActionBar())){
            lp.topMargin = getActionBarHeight(this);
        } else if (!Config.HIDE_ACTIONBAR && !getHideTabs()){
            lp.topMargin = getActionBarHeight(this) * 2;
        }

        if (Config.USE_DRAWER) {
            getSupportActionBar().setDisplayShowHomeEnabled(true);

            drawerFragment = (DrawerFragment)
                    getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);
            drawerFragment.setUp(R.id.fragment_navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout), mToolbar);
            drawerFragment.setDrawerListener(this);
        } else {
            ((DrawerLayout) findViewById(R.id.drawer_layout)).setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }

        mViewPager.setLayoutParams(lp);

        mViewPager.setAdapter(mAdapter);
        mViewPager.setOffscreenPageLimit(mViewPager.getAdapter().getCount() - 1);

        mSlidingTabLayout.setSelectedTabIndicatorColor(getResources().getColor(R.color.accent));
        mSlidingTabLayout.setupWithViewPager(mViewPager);
        mSlidingTabLayout.setOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (getCollapsingActionBar()) {
                    showToolbar(getFragment());
                }
                mViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        for (int i = 0; i < mSlidingTabLayout.getTabCount(); i++) {
            if (Config.ICONS.length > i  && Config.ICONS[i] != 0) {
                mSlidingTabLayout.getTabAt(i).setIcon(Config.ICONS[i]);
            }
        }


        // application rating
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.rate_title))
                .setMessage(String.format(getString(R.string.rate_message), getString(R.string.app_name)))
                .setPositiveButton(getString(R.string.rate_yes), null)
                .setNegativeButton(getString(R.string.rate_never), null)
                .setNeutralButton(getString(R.string.rate_later), null);

        new AppRate(this)
                .setShowIfAppHasCrashed(false)
                .setMinDaysUntilPrompt(2)
                .setMinLaunchesUntilPrompt(2)
                .setCustomDialog(builder)
                .init();

        // showing the splash screen
        if (Config.SPLASH) {
            findViewById(R.id.imageLoading1).setVisibility(View.VISIBLE);
            findViewById(R.id.progress_splash).setVisibility(View.VISIBLE);

            //getFragment().browser.setVisibility(View.GONE);
        }
    }

    // using the back button of the device
    @Override
    public void onBackPressed() {
        View customView = null;
        WebChromeClient.CustomViewCallback customViewCallback = null;
        if (getFragment().chromeClient != null) {
            customView = getFragment().chromeClient.getCustomView();
            customViewCallback = getFragment().chromeClient.getCustomViewCallback();
        }

        if ((customView == null)
                && getFragment().browser.canGoBack()) {
            getFragment().browser.goBack();
        } else if (customView != null
                && customViewCallback != null) {
            customViewCallback.onCustomViewHidden();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        WebView browser = getFragment().browser;
        if (item.getItemId() == (R.id.next)) {
            browser.goForward();
            return true;
        } else if (item.getItemId() == R.id.previous) {
            browser.goBack();
            return true;
        } else if (item.getItemId() == R.id.share) {
            getFragment().shareURL();
            return true;
        } else if (item.getItemId() == R.id.about) {
            AboutDialog();
            return true;
        } else if (item.getItemId() == R.id.home) {
            browser.loadUrl(getFragment().mainUrl);
            return true;
        } else if (item.getItemId() == R.id.close) {
            finish();
            Toast.makeText(getApplicationContext(),
                    getText(R.string.exit_message), Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {

        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    // showing about dialog
    private void AboutDialog() {
        // setting the dialogs text, and making the links clickable
        final TextView message = new TextView(this);
        // i.e.: R.string.dialog_message =>
        final SpannableString s = new SpannableString(
                this.getText(R.string.dialog_about));
        Linkify.addLinks(s, Linkify.WEB_URLS);
        message.setTextSize(15f);
        message.setPadding(20, 15, 15, 15);
        message.setText(Html.fromHtml(getString(R.string.dialog_about)));
        message.setMovementMethod(LinkMovementMethod.getInstance());

        // creating the actual dialog

        AlertDialog.Builder AlertDialog = new AlertDialog.Builder(this);
        AlertDialog.setTitle(Html.fromHtml(getString(R.string.about)))
                // .setTitle(R.string.about)
                .setCancelable(true)
                        // .setIcon(android.R.drawable.ic_dialog_info)
                .setPositiveButton("ok", null).setView(message).create().show();
    }

    public void setTitle(String title) {
        if (mAdapter != null &&
                mAdapter.getCount() == 1 &&
                !Config.USE_DRAWER &&
                !Config.STATIC_TOOLBAR_TITLE &&
                getSupportActionBar() != null)
            getSupportActionBar().setTitle(title);
    }

    public WebFragment getFragment(){
        return (WebFragment) mAdapter.getCurrentFragment();
    }

    public void hideSplash() {
        if (Config.SPLASH) {
            Handler mHandler = new Handler();
            mHandler.postDelayed(new Runnable() {
                public void run() {
                    // hide splash image
                    if (findViewById(R.id.imageLoading1).getVisibility() == View.VISIBLE) {
                        findViewById(R.id.imageLoading1).setVisibility(
                                View.GONE);
                        findViewById(R.id.progress_splash).setVisibility(View.GONE);
                        // show webview
                    }
                }
                // set a delay before splashscreen is hidden
            }, Config.SPLASH_SCREEN_DELAY);
        }
    }

    public void hideToolbar() {
        if (CurrentAnimation != HIDING) {
            CurrentAnimation = HIDING;
            AnimatorSet animSetXY = new AnimatorSet();

            ObjectAnimator animY = ObjectAnimator.ofFloat(getFragment().rl, "y", 0);
            ObjectAnimator animY1 = ObjectAnimator.ofFloat(mHeaderView, "y", -getActionBarHeight(this));
            animSetXY.playTogether(animY, animY1);

            animSetXY.start();
            animSetXY.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    CurrentAnimation = NO;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });

        }
    }

    public void showToolbar(WebFragment fragment) {
        if (CurrentAnimation != SHOWING || fragment != CurrentAnimatingFragment) {
            CurrentAnimation = SHOWING;
            CurrentAnimatingFragment = fragment;
            AnimatorSet animSetXY = new AnimatorSet();
            ObjectAnimator animY = ObjectAnimator.ofFloat(fragment.rl, "y", getActionBarHeight(this));
            ObjectAnimator animY1 = ObjectAnimator.ofFloat(mHeaderView, "y", 0);
            animSetXY.playTogether(animY, animY1);

            animSetXY.start();
            animSetXY.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    CurrentAnimation = NO;
                    CurrentAnimatingFragment = null;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });

        }
    }

    public static int getActionBarHeight(Context context) {
        TypedValue typedValue = new TypedValue();
        int[] textSizeAttr = new int[]{R.attr.actionBarSize};

        int indexOfAttrTextSize = 0;

        TypedArray a = context.obtainStyledAttributes(typedValue.data, textSizeAttr);
        int actionBarSize = a.getDimensionPixelSize(indexOfAttrTextSize, -1);
        a.recycle();

        return actionBarSize;
    }

    boolean getHideTabs(){
        if (mAdapter.getCount() == 1 || Config.USE_DRAWER){
            return true;
        } else {
            return Config.HIDE_TABS;
        }
    }

    public static boolean getCollapsingActionBar(){
        if (Config.COLLAPSING_ACTIONBAR && !Config.HIDE_ACTIONBAR){
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onDrawerItemSelected(View view, int position) {
        getFragment().browser.loadUrl("about:blank");
        getFragment().setBaseUrl(Config.URLS[position]);
    }
}