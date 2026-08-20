package com.xyrlsz.xcimocob.ui.fragment.dialog;

import androidx.appcompat.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.xyrlsz.xcimocob.R;
import com.xyrlsz.xcimocob.component.DialogCaller;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MultiAdpaterDialogFragment extends DialogFragment implements DialogInterface.OnClickListener{

    private boolean[] mCheckArray;
    private SimpleAdapter adapter;
    private ArrayList<Map<String, Object>> arrayList = new ArrayList<Map<String, Object>>();
    private View getlistview;

    public static MultiAdpaterDialogFragment newInstance(int title, String[] item, boolean[] check, int requestCode) {
        MultiAdpaterDialogFragment fragment = new MultiAdpaterDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(DialogCaller.EXTRA_DIALOG_TITLE, title);
        bundle.putStringArray(DialogCaller.EXTRA_DIALOG_ITEMS, item);
        bundle.putBooleanArray(DialogCaller.EXTRA_DIALOG_CHOICE_ITEMS, check);
        bundle.putInt(DialogCaller.EXTRA_DIALOG_REQUEST_CODE, requestCode);
        fragment.setArguments(bundle);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String[] item = requireArguments().getStringArray(DialogCaller.EXTRA_DIALOG_ITEMS);
        if (item == null) {
            item = new String[0];
        }
        initCheckArray(item.length);

        for (int i = 0; i < item.length; i++) {
            Map<String, Object> hashMap = new HashMap<String, Object>();
            hashMap.put("text", item[i]);
            arrayList.add(hashMap);
        }

        LayoutInflater inflater = getLayoutInflater();
        // 提供父容器（由 AlertDialog 内部的 android.R.id/custom 区域作为 parent）
        // 否则 inflate 时布局根元素的 layout_width/height 会被丢弃（换成 WRAP_CONTENT），
        // 在某些机型上整个自定义区域被压得只剩一小条。attachToRoot=false，
        // 因为稍后会通过 builder.setView(getlistview) 再附加到真正的容器上。
        android.widget.FrameLayout dialogContentParent =
                new android.widget.FrameLayout(requireContext());
        getlistview = inflater.inflate(R.layout.listview_adapter, dialogContentParent, false);
        ListView listview = (ListView) getlistview.findViewById(R.id.listview_adapter);

        adapter = new SetSimpleAdapter(getActivity(), arrayList, R.layout.item_select_mutil, new String[]{"text"}, new int[]{R.id.item_select_title_mutil});
        listview.setAdapter(adapter);
        listview.setCacheColorHint(Color.TRANSPARENT);
        listview.setDivider(null);
        listview.setSelector(new ColorDrawable());
        listview.setItemsCanFocus(true);
        listview.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckBox cBox = (CheckBox) view.findViewById(R.id.item_select_checkbox_mutil);
                if (cBox.isChecked()) {
                    cBox.setChecked(false);
                    mCheckArray[position] = false;
                } else {
                    cBox.setChecked(true);
                    mCheckArray[position] = true;
                }
                adapter.notifyDataSetChanged();
            }
        });

        builder.setTitle(getArguments().getInt(DialogCaller.EXTRA_DIALOG_TITLE))
                .setView(getlistview)
                .setPositiveButton(R.string.dialog_positive, this)
                .setNeutralButton(R.string.comic_inverse_selection,this);
        return builder.create();
    }

    private void initCheckArray(int length) {
        mCheckArray = requireArguments().getBooleanArray(DialogCaller.EXTRA_DIALOG_CHOICE_ITEMS);
        if (mCheckArray == null) {
            mCheckArray = new boolean[length];
            for (int i = 0; i != length; ++i) {
                mCheckArray[i] = false;
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int which) {
        switch (which) {
            case Dialog.BUTTON_POSITIVE:
                int requestCode = requireArguments().getInt(DialogCaller.EXTRA_DIALOG_REQUEST_CODE);
                Bundle bundle = new Bundle();
                bundle.putBooleanArray(DialogCaller.EXTRA_DIALOG_RESULT_VALUE, mCheckArray);
                DialogCaller target = (DialogCaller) (getTargetFragment() != null ? getTargetFragment() : getActivity());
                Objects.requireNonNull(target).onDialogResult(requestCode, bundle);
                isCloseDialog(dialogInterface,true);
                break;
            case Dialog.BUTTON_NEUTRAL:
                for (int i=0;i<mCheckArray.length;i++){
                    mCheckArray[i]=!mCheckArray[i];
                }
                adapter.notifyDataSetChanged();
                isCloseDialog(dialogInterface,false);
                break;
            default:
                isCloseDialog(dialogInterface,true);
                break;
        }
    }

    private void isCloseDialog(DialogInterface dialog, boolean close) {
        try {
            Field field = Objects.requireNonNull(dialog.getClass().getSuperclass()).getDeclaredField("mShowing");
            field.setAccessible(true);
            field.set(dialog, close);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    class SetSimpleAdapter extends SimpleAdapter {

        public SetSimpleAdapter(Context context, List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
            super(context, data, resource, from, to);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                // 提供 ListView 作为 parent + attachToRoot=false；否则 item 的
                // android:layout_width="match_parent" 等 layout_* 属性将被丢弃，
                // 导致条目宽度仅占内容（CheckBox/标题挤在左侧）。
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.item_select_mutil, parent, false);
            }
            CheckBox ckBox = (CheckBox) convertView.findViewById(R.id.item_select_checkbox_mutil);
            if (mCheckArray[position] == true) {
                ckBox.setChecked(true);
            } else if (mCheckArray[position] == false) {
                ckBox.setChecked(false);
            }
            return super.getView(position, convertView, parent);
        }
    }
}
