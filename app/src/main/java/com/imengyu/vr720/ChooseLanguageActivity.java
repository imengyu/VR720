package com.imengyu.vr720;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.imengyu.vr720.config.Constants;
import com.imengyu.vr720.utils.AppUtils;
import com.imengyu.vr720.utils.StatusBarUtils;
import com.imengyu.vr720.widget.MyTitleBar;

import java.util.ArrayList;
import java.util.List;

public class ChooseLanguageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_language);
        StatusBarUtils.setLightMode(this);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String language = sharedPreferences.getString("language", "");

        final LanguageListAdapter listAdapter = new LanguageListAdapter(this, R.layout.item_check, list);
        final ListView listView = findViewById(R.id.list_languages);

        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String val = list.get(position).value;
            listAdapter.setCurrentSel(val);
            listAdapter.notifyDataSetChanged();
            updateThisPageLanguage(val);
        });

        LanguageItem languageItem = new LanguageItem();
        languageItem.name = getString(R.string.text_flow_system);
        languageItem.value = "";
        list.add(languageItem);

        Resources resources = getResources();
        String[] language_entries = resources.getStringArray(R.array.language_entries);
        String[] language_values = resources.getStringArray(R.array.language_values);
        for(int i = 0; i < language_values.length; i++) {
            languageItem = new LanguageItem();
            languageItem.name = language_entries[i];
            languageItem.value = language_values[i];
            list.add(languageItem);
        }

        listAdapter.setCurrentSel(language);
        listAdapter.notifyDataSetChanged();

        toolbar = findViewById(R.id.toolbar);
        toolbar.setLeftIconOnClickListener((v) -> finish());
        toolbar.setRightTextOnClickListener((v) -> changeLanguage(listAdapter.getCurrentSel()));

        final View view_help_us_translate = findViewById(R.id.view_help_us_translate);
        final View button_close_help_us_translate = findViewById(R.id.button_close_help_us_translate);
        final View button_help_us_translate = findViewById(R.id.button_help_us_translate);

        String lastSetLanguageType = sharedPreferences.getString("last_set_language_type", "");
        if(!lastSetLanguageType.isEmpty() && !lastSetLanguageType.equals("zh"))
            view_help_us_translate.setVisibility(View.VISIBLE);
        else
            view_help_us_translate.setVisibility(View.GONE);

        button_close_help_us_translate.setOnClickListener((v) -> view_help_us_translate.setVisibility(View.GONE));
        button_help_us_translate.setOnClickListener((v) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Constants.HELP_TRANSLATE_URL))));
    }

    private final List<LanguageItem> list = new ArrayList<>();
    private MyTitleBar toolbar = null;

    private void updateThisPageLanguage(String val) {
        AppUtils.setLanguage(this, val);

        toolbar.setTitle(getString(R.string.title_choose_language));
        toolbar.setRightText(getString(R.string.text_save));
        list.get(0).name = getString(R.string.text_flow_system);
    }
    private void changeLanguage(String val) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().putString("language", val).putString("last_set_language_type", val).apply();
        setResult( Activity.RESULT_OK, new Intent().putExtra("needRestart", true));
        finish();
    }

    private static class LanguageItem {
        public String name;
        public String value;
    }
    private static class LanguageListAdapter extends ArrayAdapter<LanguageItem> {

        private final int layoutId;
        private String currentSel;

        public LanguageListAdapter(Context context, int layoutId, List<LanguageItem> list) {
            super(context, layoutId, list);
            this.layoutId = layoutId;
        }

        public void setCurrentSel(String currentSel) {
            this.currentSel = currentSel;
        }
        public String getCurrentSel() {
            return currentSel;
        }

        private static class ViewHolder {
            public TextView textView;
            public RadioButton radioButton;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            final LanguageItem item = getItem(position);

            ViewHolder viewHolder;

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(layoutId, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.textView = convertView.findViewById(R.id.text);
                viewHolder.radioButton = convertView.findViewById(R.id.radio);

                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            if(item != null) {
                viewHolder.textView.setText(item.name);
                viewHolder.radioButton.setChecked(item.value.equals(currentSel));
            }

            return convertView;
        }
    }
}