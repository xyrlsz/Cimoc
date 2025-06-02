package com.xyrlsz.xcimoc.ui.fragment.recyclerview.grid;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.xyrlsz.xcimoc.App;
import com.xyrlsz.xcimoc.R;
import com.xyrlsz.xcimoc.model.MiniComic;
import com.xyrlsz.xcimoc.model.Task;
import com.xyrlsz.xcimoc.presenter.BasePresenter;
import com.xyrlsz.xcimoc.presenter.DownloadPresenter;
import com.xyrlsz.xcimoc.service.DownloadService;
import com.xyrlsz.xcimoc.ui.activity.TaskActivity;
import com.xyrlsz.xcimoc.ui.fragment.dialog.ItemDialogFragment;
import com.xyrlsz.xcimoc.ui.fragment.dialog.MessageDialogFragment;
import com.xyrlsz.xcimoc.ui.view.DownloadView;
import com.xyrlsz.xcimoc.utils.ComicUtils;
import com.xyrlsz.xcimoc.utils.HintUtils;
import com.xyrlsz.xcimoc.utils.ServiceUtils;

import java.util.ArrayList;

/**
 * Created by Hiroshi on 2016/9/1.
 */
public class DownloadFragment extends GridFragment implements DownloadView {

    private static final int DIALOG_REQUEST_SWITCH = 1;
    private static final int DIALOG_REQUEST_INFO = 2;
    private static final int DIALOG_REQUEST_DELETE = 3;
    private static final int DIALOG_REQUEST_OUTPUT = 4;
    private static final int OPERATION_INFO = 0;
    private static final int OPERATION_DELETE = 1;
    private static final int OPERATION_OUTPUT = 2;
    private static final int OPERATION_OUTPUT_SIMPLE = 0;
    private static final int OPERATION_OUTPUT_ZIP = 1;
    private static final int OPERATION_OUTPUT_EPUB = 2;
    private static final int OPERATION_OUTPUT_CBZ = 3;
    private DownloadPresenter mPresenter;

    private boolean isDownload;

    @Override
    protected BasePresenter initPresenter() {
        mPresenter = new DownloadPresenter();
        mPresenter.attachView(this);
        return mPresenter;
    }

    @Override
    protected void initView() {
        isDownload = ServiceUtils.isServiceRunning(getActivity(), DownloadService.class);
        super.initView();
    }

    @Override
    protected void initData() {
        mPresenter.load();
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
                                R.string.download_delete_confirm, true, DIALOG_REQUEST_DELETE);
                        fragment.setTargetFragment(this, 0);
                        fragment.show(requireActivity().getSupportFragmentManager(), null);
                        break;
                    case OPERATION_OUTPUT:
                        ItemDialogFragment outputfragment = ItemDialogFragment.newInstance(R.string.common_operation_select,
                                getOutputType(), DIALOG_REQUEST_OUTPUT);
                        outputfragment.setTargetFragment(this, DIALOG_REQUEST_OUTPUT);
                        outputfragment.show(requireActivity().getSupportFragmentManager(), null);
                        break;

                    default:
                        break;
                }
                break;
            case DIALOG_REQUEST_SWITCH:
                if (isDownload) {
                    ServiceUtils.stopService(getActivity(), DownloadService.class);
                    HintUtils.showToast(getActivity(), R.string.download_stop_success);
                } else {
                    showProgressDialog();
                    mPresenter.loadTask();
                }
                break;
            case DIALOG_REQUEST_DELETE:
                if (isDownload) {
                    HintUtils.showToast(getActivity(), R.string.download_ask_stop);
                } else {
                    showProgressDialog();
                    mPresenter.deleteComic(mSavedId);
                }
                break;
            case DIALOG_REQUEST_OUTPUT:
                int output = bundle.getInt(EXTRA_DIALOG_RESULT_INDEX);
                switch (output) {
                    case OPERATION_OUTPUT_SIMPLE:
                        showProgressDialog();
                        ComicUtils.OutputDownloadedComic(this, getContext(), ComicUtils.SIMPLE, mPresenter.load(mSavedId), new ComicUtils.OutputComicCallback() {
                            @Override
                            public void onSuccess() {
                                hideProgressDialog();
                                App.runOnMainThread(() -> HintUtils.showToast(getActivity(), R.string.common_execute_success));
                            }

                            @Override
                            public void onFailure(String message) {
                                hideProgressDialog();
                                App.runOnMainThread(() -> HintUtils.showToast(getActivity(), message));
                            }
                        });
                        break;
                    case OPERATION_OUTPUT_ZIP:
                        showProgressDialog();
                        ComicUtils.OutputDownloadedComic(this, getContext(), ComicUtils.ZIP, mPresenter.load(mSavedId), new ComicUtils.OutputComicCallback() {
                            @Override
                            public void onSuccess() {
                                hideProgressDialog();
                                App.runOnMainThread(() -> HintUtils.showToast(getActivity(), R.string.common_execute_success));
                            }

                            @Override
                            public void onFailure(String message) {
                                hideProgressDialog();
                                App.runOnMainThread(() -> HintUtils.showToast(getActivity(), message));
                            }
                        });
                        break;

                    case OPERATION_OUTPUT_EPUB:
                        showProgressDialog();
                        ComicUtils.OutputDownloadedComic(this, getContext(), ComicUtils.EPUB, mPresenter.load(mSavedId), new ComicUtils.OutputComicCallback() {
                            @Override
                            public void onSuccess() {
                                hideProgressDialog();
                                App.runOnMainThread(() -> HintUtils.showToast(getActivity(), R.string.common_execute_success));
                            }

                            @Override
                            public void onFailure(String message) {
                                hideProgressDialog();
                                App.runOnMainThread(() -> HintUtils.showToast(getActivity(), message));
                            }
                        });
                        break;
                    case OPERATION_OUTPUT_CBZ:
                        showProgressDialog();
                        ComicUtils.OutputDownloadedComic(this, getContext(), ComicUtils.CBZ, mPresenter.load(mSavedId), new ComicUtils.OutputComicCallback() {
                            @Override
                            public void onSuccess() {
                                hideProgressDialog();
                                App.runOnMainThread(() -> HintUtils.showToast(getActivity(), R.string.common_execute_success));
                            }

                            @Override
                            public void onFailure(String message) {
                                hideProgressDialog();
                                App.runOnMainThread(() -> HintUtils.showToast(getActivity(), message));
                            }
                        });
                        break;

                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void performActionButtonClick() {
        if (mGridAdapter.getDateSet().isEmpty()) {
            return;
        }
        MessageDialogFragment fragment = MessageDialogFragment.newInstance(R.string.dialog_confirm,
                R.string.download_action_confirm, true, DIALOG_REQUEST_SWITCH);
        fragment.setTargetFragment(this, 0);
        fragment.show(requireActivity().getSupportFragmentManager(), null);
    }

    @Override
    public void onItemClick(View view, int position) {
        MiniComic comic = (MiniComic) mGridAdapter.getItem(position);
        Intent intent = TaskActivity.createIntent(getActivity(), comic.getId());
        startActivity(intent);
    }

    @Override
    public void onDownloadAdd(MiniComic comic) {
        if (!mGridAdapter.exist(comic)) {
            mGridAdapter.add(0, comic);
        }
    }

    @Override
    public void onDownloadDelete(long id) {
        mGridAdapter.removeItemById(id);
    }

    @Override
    public void onDownloadDeleteSuccess(long id) {
        hideProgressDialog();
        mGridAdapter.removeItemById(id);
        HintUtils.showToast(getActivity(), R.string.common_execute_success);
    }

    @Override
    public void onDownloadStart() {
        if (!isDownload) {
            isDownload = true;
            mActionButton.setImageResource(R.drawable.ic_pause_white_24dp);
        }
    }

    @Override
    public void onDownloadStop() {
        if (isDownload) {
            isDownload = false;
            mActionButton.setImageResource(R.drawable.ic_play_arrow_white_24dp);
        }
    }

    @Override
    public void onTaskLoadSuccess(ArrayList<Task> list) {
        if (list.isEmpty()) {
            HintUtils.showToast(getActivity(), R.string.download_task_empty);
        } else {
            Intent intent = DownloadService.createIntent(getActivity(), list);
            getActivity().startService(intent);
            HintUtils.showToast(getActivity(), R.string.download_start_success);
        }
        hideProgressDialog();
    }

    @Override
    protected int getActionButtonRes() {
        return isDownload ? R.drawable.ic_pause_white_24dp : R.drawable.ic_play_arrow_white_24dp;
    }

    @Override
    protected String[] getOperationItems() {
        return new String[]{getString(R.string.comic_info), getString(R.string.download_delete), getString(R.string.download_output)};
    }

    private String[] getOutputType() {
        return getResources().getStringArray(R.array.download_output_choice);
    }
}
