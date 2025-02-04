package com.xyrlsz.xcimoc.ui.fragment.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.xyrlsz.xcimoc.component.DialogCaller;

/**
 * Created by Hiroshi on 2016/12/2.
 */

public class ItemDialogFragment extends DialogFragment implements DialogInterface.OnClickListener {

    public static ItemDialogFragment newInstance(int title, String[] item, int requestCode) {
        ItemDialogFragment fragment = new ItemDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(DialogCaller.EXTRA_DIALOG_TITLE, title);
        bundle.putStringArray(DialogCaller.EXTRA_DIALOG_ITEMS, item);
        bundle.putInt(DialogCaller.EXTRA_DIALOG_REQUEST_CODE, requestCode);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        String[] item = getArguments().getStringArray(DialogCaller.EXTRA_DIALOG_ITEMS);
        builder.setTitle(getArguments().getInt(DialogCaller.EXTRA_DIALOG_TITLE))
                .setItems(item, this);
        return builder.create();
    }

    @Override
    public void onClick(DialogInterface dialogInterface, int which) {
        int requestCode = getArguments().getInt(DialogCaller.EXTRA_DIALOG_REQUEST_CODE);
        Bundle bundle = new Bundle();
        bundle.putInt(DialogCaller.EXTRA_DIALOG_RESULT_INDEX, which);
        DialogCaller target = (DialogCaller) (getTargetFragment() != null ? getTargetFragment() : getActivity());
        target.onDialogResult(requestCode, bundle);
    }

}
