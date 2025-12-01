package com.xyrlsz.xcimoc.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.xyrlsz.xcimoc.R;
import com.xyrlsz.xcimoc.model.MiniComic;

import java.util.List;

public class CategoryGridAdapter extends RecyclerView.Adapter<CategoryGridAdapter.ViewHolder> {

    private final Context mContext;
    private final List<MiniComic> mList;
    private OnComicClickListener mListener;

    public interface OnComicClickListener {
        void onComicClick(MiniComic comic);
    }

    public CategoryGridAdapter(Context context, List<MiniComic> list) {
        this.mContext = context;
        this.mList = list;
    }

    public void setOnComicClickListener(OnComicClickListener l) {
        this.mListener = l;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext)
                .inflate(R.layout.item_category_comic, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MiniComic comic = mList.get(position);

        // 封面
        Glide.with(mContext)
                .load(comic.getCover())
                .placeholder(R.drawable.ic_about_black_24dp)
                .into(holder.cover);

        // 标题
        holder.title.setText(comic.getTitle());

//        // 点赞 / 收藏数（你的 MiniComic 有什么就填什么）
//        holder.fav.setText(comic.getFavCount());
//
//        // 状态（连载/完结）
//        holder.status.setText(comic.getStatus());
//
//        // 更新时间
//        holder.update.setText(comic.getUpdate());

        // 点击事件
        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onComicClick(comic);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView cover;
        TextView title, fav, status, update;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            cover = itemView.findViewById(R.id.item_cover);
            title = itemView.findViewById(R.id.item_title);
            fav = itemView.findViewById(R.id.item_fav);
            status = itemView.findViewById(R.id.item_status);
            update = itemView.findViewById(R.id.item_update);
        }
    }
}
