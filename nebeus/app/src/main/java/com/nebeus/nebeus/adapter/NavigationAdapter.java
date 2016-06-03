package com.nebeus.nebeus.adapter;

import com.nebeus.nebeus.Config;
import com.nebeus.nebeus.fragment.WebFragment;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.ViewGroup;

public class NavigationAdapter extends CacheFragmentStatePagerAdapter {

    private Fragment mCurrentFragment;
    private Activity mContext;

    public NavigationAdapter(FragmentManager fm, Activity activity) {
        super(fm);
        mContext = activity;
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        if (getCurrentFragment() != object) {
            mCurrentFragment = ((Fragment) object);
        }
        super.setPrimaryItem(container, position, object);
    }

    @Override
    protected Fragment createItem(int position) {
        // Initialize fragments.
        // Please be sure to pass scroll position to each fragments using setArguments.
        Fragment f;
        //final int pattern = position % 3;
        f = WebFragment.newInstance(Config.URLS[position]);
        return f;
    }

    @Override
    public int getCount() {
        if (Config.USE_DRAWER)
            return 1;
        else
            return Config.TITLES.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        //If there is a localized title available, use it
        Object title = Config.TITLES[position];
//        if (title instanceof Integer && title != 0){
//            return mContext.getResources().getString((int) title);
//        } else {
            return (String) title;
//        }
    }

    public Fragment getCurrentFragment() {
        return mCurrentFragment;
    }
}
