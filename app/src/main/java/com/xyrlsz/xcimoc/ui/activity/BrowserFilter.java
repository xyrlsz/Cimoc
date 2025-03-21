package com.xyrlsz.xcimoc.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.xyrlsz.xcimoc.R;
import com.xyrlsz.xcimoc.manager.SourceManager;
import com.xyrlsz.xcimoc.source.Animx2;
import com.xyrlsz.xcimoc.source.BuKa;
import com.xyrlsz.xcimoc.source.Cartoonmad;
import com.xyrlsz.xcimoc.source.DM5;
import com.xyrlsz.xcimoc.source.HotManga;
import com.xyrlsz.xcimoc.source.IKanman;
import com.xyrlsz.xcimoc.source.MiGu;
import com.xyrlsz.xcimoc.source.Tencent;
import com.xyrlsz.xcimoc.source.YKMH;
import com.xyrlsz.xcimoc.utils.HintUtils;

import java.util.ArrayList;
import java.util.List;

public class BrowserFilter extends BaseActivity {

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_browser_filter;
    }

    @Override
    protected String getDefaultTitle() {
        return "jumping...";
    }

//    private Parser parser;
//    private SourceManager mSourceManager;

    public void openDetailActivity(int source, String comicId) {
        Intent intent = DetailActivity.createIntent(this, null, source, comicId);
        startActivity(intent);
    }

//    public void openReaderActivity(int source,String comicId) {
//        Intent intent = DetailActivity.createIntent(this, null, source, comicId);
//        startActivity(intent);
//    }

    private List<Integer> registUrlListener() {
        List<Integer> list = new ArrayList<>();

//        list.add(Dmzjv2.TYPE);
        list.add(BuKa.TYPE);
//        list.add(PuFei.TYPE);
        list.add(Cartoonmad.TYPE);
        list.add(Animx2.TYPE);
//        list.add(MH517.TYPE);
//        list.add(BaiNian.TYPE);
        list.add(MiGu.TYPE);
        list.add(Tencent.TYPE);
//        list.add(U17.TYPE);
//        list.add(MH57.TYPE);
//        list.add(MH50.TYPE);
        list.add(DM5.TYPE);
        list.add(IKanman.TYPE);
//        list.add(Hhxxee.TYPE);
//        list.add(BaiNian.TYPE);
//        list.add(ChuiXue.TYPE);
//        list.add(ManHuaDB.TYPE);
//        list.add(TuHao.TYPE);
        list.add(YKMH.TYPE);
//        list.add(SixMH.TYPE);
        list.add(HotManga.TYPE);
        return list;
    }

    private void openReader(Uri uri) {
        try {
            SourceManager mSourceManager = SourceManager.getInstance(this);
            String comicId;

            for (int i : registUrlListener()) {
                if (mSourceManager.getParser(i).isHere(uri)
                        && ((comicId = mSourceManager.getParser(i).getComicId(uri)) != null)) {
                    openDetailActivity(i, comicId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void openReaderByIntent(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();

        //来自url
        if (Intent.ACTION_VIEW.equals(action)) {
            Uri uri = intent.getData();
            if (uri != null) {
                openReader(uri);
            } else {
//                Toast.makeText(this, "url不合法", Toast.LENGTH_SHORT);
                HintUtils.showToast(this, "url不合法");
            }
        }

        //来自分享
        if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(type)) {
            try {
                openReader(Uri.parse(intent.getStringExtra(Intent.EXTRA_TEXT).replace("https://m.ykmh.commanhua", "https://m.ykmh.com/manhua")));
            } catch (Exception ex) {
//                Toast.makeText(this, "url不合法", Toast.LENGTH_SHORT);
                HintUtils.showToast(this, "url不合法");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browser_filter);

        openReaderByIntent(getIntent());

        finish();
    }
}
