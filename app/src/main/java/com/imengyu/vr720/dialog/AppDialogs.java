package com.imengyu.vr720.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Message;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.imengyu.vr720.AboutActivity;
import com.imengyu.vr720.FeedBackActivity;
import com.imengyu.vr720.HelpActivity;
import com.imengyu.vr720.R;
import com.imengyu.vr720.SettingsActivity;
import com.imengyu.vr720.adapter.SimpleListAdapter;
import com.imengyu.vr720.adapter.SmallGalleryListAdapter;
import com.imengyu.vr720.config.MainMessages;
import com.imengyu.vr720.model.GalleryItem;
import com.imengyu.vr720.model.list.GalleryListItem;
import com.imengyu.vr720.service.ListDataService;
import com.imengyu.vr720.utils.AlertDialogTool;
import com.imengyu.vr720.utils.PixelTool;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class AppDialogs {

  public static final int RESULT_SETTING_ACTIVITY = 0;

  public static void showHelp(Activity activity) {
    activity.startActivity(new Intent(activity, HelpActivity.class));
  }
  public static void showAbout(Activity activity) {
    activity.startActivity(new Intent(activity, AboutActivity.class));
  }
  public static void showSettings(Activity activity) {
    activity.startActivityForResult(new Intent(activity, SettingsActivity.class), RESULT_SETTING_ACTIVITY);
  }
  public static void showFeedBack(Activity activity) {
    activity.startActivity(new Intent(activity, FeedBackActivity.class));
  }

  public interface OnAgreementCloseListener {
    void onAgreementClose(boolean allowed);
  }
  public interface OnChooseGalleryListener {
    void onChooseGallery(int galleryId);
  }
  public interface OnChooseItemListener {
    void onChooseItem(boolean choosed, int index, String item);
  }

  public static void showPrivacyPolicyAndAgreement(Activity activity, OnAgreementCloseListener onAgreementCloseListener) {
    LayoutInflater inflater = LayoutInflater.from(activity);
    View v = inflater.inflate(R.layout.dialog_argeement, (ViewGroup) activity.getWindow().getDecorView(), false);

    AlertDialog dialog = AlertDialogTool.buildCustomStylePopupDialogGravity(activity, v, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, R.style.DialogBottomPopup, false);
    dialog.show();

    Button btn_ok = v.findViewById(R.id.btn_ok);
    Button btn_close = v.findViewById(R.id.btn_close);
    Button btn_cancel = v.findViewById(R.id.btn_cancel);
    TextView text_base = v.findViewById(R.id.text_base);
    TextView text_title = v.findViewById(R.id.text_title);

    if(onAgreementCloseListener!=null) {
      btn_ok.setOnClickListener(view -> { dialog.dismiss(); onAgreementCloseListener.onAgreementClose(true); });
      btn_cancel.setOnClickListener(view -> { dialog.dismiss(); onAgreementCloseListener.onAgreementClose(false); });
      text_base.setText(R.string.text_agreement_base_welcome);
    }else {
      btn_ok.setVisibility(View.GONE);
      btn_cancel.setVisibility(View.GONE);
      btn_close.setVisibility(View.VISIBLE);
      btn_close.setOnClickListener(view -> dialog.dismiss());
      text_base.setText(R.string.text_agreement_base);
      text_title.setText(R.string.settings_key_privacy_policy);
    }

    text_base.setMovementMethod(LinkMovementMethod.getInstance());

  }
  public static void showChooseGalleryDialog(Handler handler, Activity activity, ListDataService listDataService, OnChooseGalleryListener onChooseGalleryListener) {
    final List<GalleryListItem> listItems = new ArrayList<>();
    final SmallGalleryListAdapter smallGalleryListAdapter = new SmallGalleryListAdapter(activity,
            R.layout.item_gallery_small, listItems);

    LayoutInflater inflater = LayoutInflater.from(activity);
    View v = inflater.inflate(R.layout.dialog_choose_gallery, (ViewGroup) activity.getWindow().getDecorView(), false);
    AlertDialog dialog = AlertDialogTool.buildCustomBottomPopupDialog(activity, v);

    ListView list_gallery = v.findViewById(R.id.list_gallery);
    list_gallery.setAdapter(smallGalleryListAdapter);
    list_gallery.setDivider(null);
    list_gallery.setDividerHeight(0);

    //添加条目
    GalleryListItem addItem = new GalleryListItem();
    addItem.setId(ListDataService.GALLERY_LIST_ID_ADD);
    addItem.setName(activity.getString(R.string.action_new_gallery));
    listItems.add(addItem);
    for (GalleryItem item : listDataService.getGalleryList()) {
      if(item.id == ListDataService.GALLERY_LIST_ID_VIDEOS) continue;//跳过视频收藏夹
      GalleryListItem nItem = new GalleryListItem(item);
      nItem.refresh(listDataService);
      listItems.add(nItem);
    }
    smallGalleryListAdapter.notifyDataSetChanged();

    AlertDialogTool.OnDialogConfigurationChangedListener listener = dialog1 -> {
      //设置横屏时的列表高度
      Configuration mConfiguration = activity.getResources().getConfiguration();
      if (mConfiguration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        ViewGroup.LayoutParams layoutParams = list_gallery.getLayoutParams();
        layoutParams.height = PixelTool.dp2px(activity, 180);
        list_gallery.setLayoutParams(layoutParams);
      } else if (mConfiguration.orientation == Configuration.ORIENTATION_PORTRAIT) {
        ViewGroup.LayoutParams layoutParams = list_gallery.getLayoutParams();
        layoutParams.height = PixelTool.dp2px(activity, 380);
        list_gallery.setLayoutParams(layoutParams);
      }
    };
    AlertDialogTool.setOnDialogConfigurationChangedListener(dialog, listener);
    listener.onDialogConfigurationChanged(dialog);


    //按钮事件
    v.findViewById(R.id.btn_cancel).setOnClickListener(view -> dialog.dismiss());
    list_gallery.setOnItemClickListener((adapterView, view, i, l) -> {
      GalleryListItem item = listItems.get(i);
      if(item.getId() == ListDataService.GALLERY_LIST_ID_ADD) {
        //新建相册
        new CommonDialog(activity)
                .setEditTextVisible(true)
                .setEditHint(activity.getString(R.string.text_enter_gallery_name))
                .setTitle(activity.getString(R.string.action_new_gallery))
                .setPositiveEnabled(false)
                .setCanCancelable(true)
                .setEditTextOnTextChangedListener((newText, commonDialog) -> commonDialog.setPositiveEnabled(newText.length() > 0))
                .setOnClickBottomListener(new CommonDialog.OnClickBottomListener() {
                  @Override
                  public void onPositiveClick(CommonDialog innerDialog) {
                    dialog.dismiss();
                    innerDialog.dismiss();

                    //添加相册
                    GalleryItem listItem = new GalleryItem();
                    listItem.id = listDataService.getGalleryListMinId();
                    listItem.name = innerDialog.getEditText().getText().toString();
                    listItem.createTime = String.valueOf(new Date().getTime());
                    listDataService.addGalleryItem(listItem);

                    //发送信息到相册界面完成添加 galleryList.addItem(listItem, true);
                    Message message = new Message();
                    message.what = MainMessages.MSG_GALLERY_LIST_ADD_ITEM;
                    message.obj = listItem.id;
                    handler.sendMessage(message);

                    if(onChooseGalleryListener != null)
                      onChooseGalleryListener.onChooseGallery(listItem.id);
                    dialog.dismiss();
                  }
                  @Override
                  public void onNegativeClick(CommonDialog dialog) { dialog.dismiss(); }
                })
                .show();
      }
      else if(onChooseGalleryListener != null) {
        onChooseGalleryListener.onChooseGallery(item.getId());
        dialog.dismiss();
      }
    });

    dialog.show();
  }
  public static void showChooseItemDialog(Activity activity, String title, String[] items, OnChooseItemListener onChooseItemListener) {
    final List<String> listItems = new ArrayList<>();
    final SimpleListAdapter adapter = new SimpleListAdapter(activity, R.layout.item_simple, listItems);

    LayoutInflater inflater = LayoutInflater.from(activity);
    View v = inflater.inflate(R.layout.dialog_choose_item, (ViewGroup) activity.getWindow().getDecorView(), false);
    AlertDialog dialog = AlertDialogTool.buildCustomBottomPopupDialog(activity, v);

    ListView list_simple = v.findViewById(R.id.list_simple);
    ((TextView)v.findViewById(R.id.text_title)).setText(title);
    list_simple.setAdapter(adapter);
    list_simple.setDivider(null);
    list_simple.setDividerHeight(0);

    AlertDialogTool.OnDialogConfigurationChangedListener listener = dialog1 -> {
      //设置横屏时的列表高度
      Configuration mConfiguration = activity.getResources().getConfiguration();
      if (mConfiguration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        ViewGroup.LayoutParams layoutParams = list_simple.getLayoutParams();
        layoutParams.height = PixelTool.dp2px(activity, items.length >= 4 ? 200 : items.length * 50);
        list_simple.setLayoutParams(layoutParams);
      } else if (mConfiguration.orientation == Configuration.ORIENTATION_PORTRAIT) {
        ViewGroup.LayoutParams layoutParams = list_simple.getLayoutParams();
        layoutParams.height = PixelTool.dp2px(activity, items.length >= 8 ? 400 : items.length * 50);
        list_simple.setLayoutParams(layoutParams);
      }
    };

    AlertDialogTool.setOnDialogConfigurationChangedListener(dialog, listener);
    listener.onDialogConfigurationChanged(dialog);

    //添加条目
    listItems.addAll(Arrays.asList(items));
    adapter.notifyDataSetChanged();

    //按钮事件
    v.findViewById(R.id.btn_cancel).setOnClickListener(view -> {
      if(onChooseItemListener != null)
        onChooseItemListener.onChooseItem(false, -1, null);
      dialog.dismiss();
    });
    list_simple.setOnItemClickListener((adapterView, view, i, l) -> {
      if (onChooseItemListener != null)
        onChooseItemListener.onChooseItem(true, i, listItems.get(i));
      dialog.dismiss();
    });

    dialog.show();
  }

}
