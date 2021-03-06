package com.imengyu.vr720.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.List;

public class MyFragmentAdapter extends FragmentStatePagerAdapter {
  private final List<Fragment> mFragments ;
  private final List<String> mTitles ;
  public MyFragmentAdapter(FragmentManager fm, List<Fragment> fragments, List<String> titles) {
    super(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT);
    mFragments = fragments;
    mTitles = titles;
  }

  @NonNull
  @Override
  public Fragment getItem(int position) {
    return mFragments.get(position);
  }

  @Override
  public int getCount() {
    return mFragments == null ? 0 : mFragments.size();
  }

  @Override
  public CharSequence getPageTitle(int position) {
    return mTitles.get(position);
  }
}
