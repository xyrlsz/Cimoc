package com.xyrlsz.xcimoc.ui.adapter;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.xyrlsz.xcimoc.R;
import com.xyrlsz.xcimoc.misc.Switcher;
import com.xyrlsz.xcimoc.model.Tag;

import java.util.List;

import butterknife.BindView;

/**
 * Created by Hiroshi on 2016/12/2.
 */

public class TagEditorAdapter extends BaseAdapter<Switcher<Tag>> {

    public TagEditorAdapter(Context context, List<Switcher<Tag>> list) {
        super(context, list);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.item_select, parent, false);
        return new TagHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        TagHolder viewHolder = (TagHolder) holder;
        Switcher<Tag> switcher = mDataSet.get(position);
        viewHolder.tagTitle.setText(switcher.getElement().getTitle());
        viewHolder.tagChoice.setChecked(switcher.isEnable());
    }

    @Override
    public RecyclerView.ItemDecoration getItemDecoration() {
        return null;
    }

    @Override
    protected boolean isClickValid() {
        return true;
    }

    static class TagHolder extends BaseAdapter.BaseViewHolder {
        @BindView(R.id.item_select_title)
        TextView tagTitle;
        @BindView(R.id.item_select_checkbox)
        CheckBox tagChoice;

        TagHolder(View view) {
            super(view);
        }
    }

}
