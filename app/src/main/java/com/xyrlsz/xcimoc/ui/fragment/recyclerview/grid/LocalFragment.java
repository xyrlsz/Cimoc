package com.xyrlsz.xcimoc.ui.fragment.recyclerview.grid;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import com.xyrlsz.xcimoc.R;
import com.xyrlsz.xcimoc.global.Extra;
import com.xyrlsz.xcimoc.model.MiniComic;
import com.xyrlsz.xcimoc.presenter.BasePresenter;
import com.xyrlsz.xcimoc.presenter.LocalPresenter;
import com.xyrlsz.xcimoc.saf.CimocDocumentFile;
import com.xyrlsz.xcimoc.ui.activity.DirPickerActivity;
import com.xyrlsz.xcimoc.ui.activity.TaskActivity;
import com.xyrlsz.xcimoc.ui.fragment.dialog.MessageDialogFragment;
import com.xyrlsz.xcimoc.ui.view.LocalView;
import com.xyrlsz.xcimoc.utils.HintUtils;
import com.xyrlsz.xcimoc.utils.StringUtils;

import java.io.File;
import java.util.List;

/**
 * Created by Hiroshi on 2017/4/19.
 */

public class LocalFragment extends GridFragment implements LocalView {

    private static final int DIALOG_REQUEST_SCAN = 1;
    private static final int DIALOG_REQUEST_INFO = 2;
    private static final int DIALOG_REQUEST_DELETE = 3;

    private static final int OPERATION_INFO = 0;
    private static final int OPERATION_DELETE = 1;

    private LocalPresenter mPresenter;

    @Override
    protected BasePresenter initPresenter() {
        mPresenter = new LocalPresenter();
        mPresenter.attachView(this);
        return mPresenter;
    }

    @Override
    protected void initData() {
        mPresenter.load();
    }

    @Override
    protected void performActionButtonClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                getActivity().startActivityForResult(intent, DIALOG_REQUEST_SCAN);
            } catch (ActivityNotFoundException e) {
                HintUtils.showToast(getActivity(), R.string.settings_other_storage_not_found);
            }
        } else {
            Intent intent = new Intent(getActivity(), DirPickerActivity.class);
            startActivityForResult(intent, DIALOG_REQUEST_SCAN);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case DIALOG_REQUEST_SCAN:
                    showProgressDialog();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        Uri uri = data.getData();
                        if (uri != null) {
                            // Ensure that you're passing only one of the flags
                            int flags = data.getFlags();
                            if ((flags & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0) {
                                getActivity().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            } else if ((flags & Intent.FLAG_GRANT_WRITE_URI_PERMISSION) != 0) {
                                getActivity().getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            }
                            mPresenter.scan(CimocDocumentFile.fromTreeUri(getActivity(), uri));
                        }
                    } else {
                        String path = data.getStringExtra(Extra.EXTRA_PICKER_PATH);
                        if (path != null) {
                            if (!StringUtils.isEmpty(path)) {
                                mPresenter.scan(CimocDocumentFile.fromFile(new File(path)));
                            } else {
                                onExecuteFail();
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }


    @Override
    public void onItemClick(View view, int position) {
        MiniComic comic = (MiniComic) mGridAdapter.getItem(position);
        Intent intent = TaskActivity.createIntent(getActivity(), comic.getId());
        startActivity(intent);
    }

    @Override
    public void onDialogResult(int requestCode, Bundle bundle) {
        switch (requestCode) {
            case DIALOG_REQUEST_OPERATION:
                int index = bundle.getInt(EXTRA_DIALOG_RESULT_INDEX);
                switch (index) {
                    case OPERATION_INFO:
                        showComicInfo(mPresenter.load(mSavedId), DIALOG_REQUEST_INFO);
                        break;
                    case OPERATION_DELETE:
                        MessageDialogFragment fragment = MessageDialogFragment.newInstance(R.string.dialog_confirm,
                                R.string.local_delete_confirm, true, DIALOG_REQUEST_DELETE);
                        fragment.setTargetFragment(this, 0);
                        fragment.show(requireActivity().getSupportFragmentManager(), null);
                    default:
                        break;
                }
                break;
            case DIALOG_REQUEST_DELETE:
                showProgressDialog();
                mPresenter.deleteComic(mSavedId);
                break;
            default:
                break;
        }
    }

    @Override
    public void onLocalDeleteSuccess(long id) {
        hideProgressDialog();
        mGridAdapter.removeItemById(id);
        HintUtils.showToast(getActivity(), R.string.common_execute_success);
    }

    @Override
    public void onLocalScanSuccess(List<Object> list) {
        hideProgressDialog();
        mGridAdapter.addAll(list);
    }

    @Override
    public void onExecuteFail() {
        hideProgressDialog();
        HintUtils.showToast(getActivity(), R.string.common_execute_fail);
    }

    @Override
    protected int getActionButtonRes() {
        return R.drawable.ic_add_white_24dp;
    }

    @Override
    protected String[] getOperationItems() {
        return new String[]{getString(R.string.comic_info), getString(R.string.local_delete)};
    }
}

