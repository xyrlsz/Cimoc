package com.xyrlsz.xcimoc.ui.fragment.config;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import android.view.View;

import com.xyrlsz.xcimoc.R;
import com.xyrlsz.xcimoc.component.DialogCaller;
import com.xyrlsz.xcimoc.databinding.FragmentPageConfigBinding;
import com.xyrlsz.xcimoc.global.ClickEvents;
import com.xyrlsz.xcimoc.manager.PreferenceManager;
import com.xyrlsz.xcimoc.ui.activity.settings.EventSettingsActivity;
import com.xyrlsz.xcimoc.ui.fragment.BaseFragment;
import com.xyrlsz.xcimoc.ui.fragment.dialog.ChoiceDialogFragment;
import com.xyrlsz.xcimoc.ui.widget.preference.CheckBoxPreference;
import com.xyrlsz.xcimoc.ui.widget.preference.ChoicePreference;
import com.xyrlsz.xcimoc.ui.widget.preference.SliderPreference;

import java.util.Objects;

//import butterknife.BindView;
//import butterknife.OnClick;

/**
 * Created by Hiroshi on 2016/10/13.
 */

public class PageConfigFragment extends BaseFragment implements DialogCaller {

    private static final int DIALOG_REQUEST_ORIENTATION = 0;
    private static final int DIALOG_REQUEST_TURN = 1;
    private static final int DIALOG_REQUEST_TRIGGER = 2;
    private static final int DIALOG_REQUEST_OPERATION = 3;

    private static final int OPERATION_VOLUME_UP = 0;
    private static final int OPERATION_VOLUME_DOWN = 1;
    private FragmentPageConfigBinding binding;
    //    @BindView(R.id.settings_reader_load_prev)
    CheckBoxPreference mReaderLoadPrev;
    //    @BindView(R.id.settings_reader_load_next)
    CheckBoxPreference mReaderLoadNext;
    //    @BindView(R.id.settings_reader_ban_turn)
    CheckBoxPreference mReaderBanTurn;
    //    @BindView(R.id.settings_reader_quick_turn)
    CheckBoxPreference mReaderQuickTurn;
    //    @BindView(R.id.settings_reader_orientation)
    ChoicePreference mReaderOrientation;
    //    @BindView(R.id.settings_reader_turn)
    ChoicePreference mReaderTurn;
    //    @BindView(R.id.settings_reader_trigger)
    SliderPreference mReaderTrigger;

//    @BindView(R.id.settings_reader_volume_click_event) View mReaderVolumeEvent;

    @Override
    protected void initView() {
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
//            mReaderVolumeEvent.setVisibility(View.VISIBLE);
//        } else {
//            mReaderVolumeEvent.setVisibility(View.GONE);
//        }
        binding = FragmentPageConfigBinding.bind(requireView());
        mReaderLoadPrev = binding.settingsReaderLoadPrev;
        mReaderLoadNext = binding.settingsReaderLoadNext;
        mReaderBanTurn = binding.settingsReaderBanTurn;
        mReaderQuickTurn = binding.settingsReaderQuickTurn;
        mReaderOrientation = binding.settingsReaderOrientation;
        mReaderTurn = binding.settingsReaderTurn;
        mReaderTrigger = binding.settingsReaderTrigger;

        binding.settingsReaderClickEvent.setOnClickListener(this::onReaderEventClick);
        binding.settingsReaderLongClickEvent.setOnClickListener(this::onReaderEventClick);

        mReaderLoadPrev.bindPreference(PreferenceManager.PREF_READER_PAGE_LOAD_PREV, true);
        mReaderLoadNext.bindPreference(PreferenceManager.PREF_READER_PAGE_LOAD_NEXT, true);
        mReaderBanTurn.bindPreference(PreferenceManager.PREF_READER_PAGE_BAN_TURN, false);
        mReaderQuickTurn.bindPreference(PreferenceManager.PREF_READER_PAGE_QUICK_TURN, false);
        mReaderOrientation.bindPreference(requireActivity().getSupportFragmentManager(), this, PreferenceManager.PREF_READER_PAGE_ORIENTATION,
                PreferenceManager.READER_ORIENTATION_AUTO, R.array.reader_orientation_items, DIALOG_REQUEST_ORIENTATION);
        mReaderTurn.bindPreference(requireActivity().getSupportFragmentManager(), this, PreferenceManager.PREF_READER_PAGE_TURN,
                PreferenceManager.READER_TURN_LTR, R.array.reader_turn_items, DIALOG_REQUEST_TURN);
        mReaderTrigger.bindPreference(requireActivity().getSupportFragmentManager(), this, PreferenceManager.PREF_READER_PAGE_TRIGGER, 10,
                R.string.settings_reader_trigger, DIALOG_REQUEST_TRIGGER);
    }

    //    @OnClick({R.id.settings_reader_click_event, R.id.settings_reader_long_click_event})
    void onReaderEventClick(View view) {
        boolean isLong = view.getId() == R.id.settings_reader_long_click_event;
        Intent intent = EventSettingsActivity.createIntent(getActivity(), isLong,
                mReaderOrientation.getValue(), false);
        startActivity(intent);
    }

//    @OnClick(R.id.settings_reader_volume_click_event)
//    void onReaderVolumeEventClick() {
//        String[] items = {"音量上键", "音量下键"};
//        ItemDialogFragment fragment = ItemDialogFragment.newInstance(R.string.common_operation_select,
//                items, DIALOG_REQUEST_OPERATION);
//        fragment.setTargetFragment(this, 0);
//        fragment.show(getSupportFragmentManager(), null);
//    }

    private void showEventList(int index) {
        int[] mChoiceArray = ClickEvents.getPageClickEventChoice(mPreference);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            Context context = this.getContext();
            ChoiceDialogFragment fragment = ChoiceDialogFragment.newInstance(R.string.event_select,
                    ClickEvents.getEventTitleArray(context), mChoiceArray[index], index);
            fragment.show(requireActivity().getSupportFragmentManager(), null);
        }
    }

    @Override
    public void onDialogResult(int requestCode, Bundle bundle) {
        switch (requestCode) {
            case DIALOG_REQUEST_ORIENTATION:
                mReaderOrientation.setValue(bundle.getInt(EXTRA_DIALOG_RESULT_INDEX));
                break;
            case DIALOG_REQUEST_TURN:
                mReaderTurn.setValue(bundle.getInt(EXTRA_DIALOG_RESULT_INDEX));
                break;
            case DIALOG_REQUEST_TRIGGER:
                mReaderTrigger.setValue(bundle.getInt(EXTRA_DIALOG_RESULT_VALUE));
                break;
            case DIALOG_REQUEST_OPERATION:
                int index = bundle.getInt(EXTRA_DIALOG_RESULT_INDEX);
                switch (index) {
                    case OPERATION_VOLUME_UP:
                        showEventList(5);
                        break;
                    case OPERATION_VOLUME_DOWN:
                        showEventList(6);
                        break;
                }
                break;
        }
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.fragment_page_config;
    }

}
