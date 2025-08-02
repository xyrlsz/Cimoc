package com.xyrlsz.xcimoc.ui.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.xyrlsz.xcimoc.R;
import com.xyrlsz.xcimoc.manager.SourceManager;
import com.xyrlsz.xcimoc.source.Animx2;
import com.xyrlsz.xcimoc.source.Baozi;
import com.xyrlsz.xcimoc.source.BuKa;
import com.xyrlsz.xcimoc.source.Cartoonmad;
import com.xyrlsz.xcimoc.source.CopyMH;
import com.xyrlsz.xcimoc.source.DM5;
//import com.xyrlsz.xcimoc.source.Dmzj;
import com.xyrlsz.xcimoc.source.DmzjV4;
import com.xyrlsz.xcimoc.source.DongManManHua;
import com.xyrlsz.xcimoc.source.DuManWu;
import com.xyrlsz.xcimoc.source.DuManWuOrg;
import com.xyrlsz.xcimoc.source.GoDaManHua;
import com.xyrlsz.xcimoc.source.HotManga;
import com.xyrlsz.xcimoc.source.IKanman;
import com.xyrlsz.xcimoc.source.Komiic;
import com.xyrlsz.xcimoc.source.MYCOMIC;
import com.xyrlsz.xcimoc.source.MangaBZ;
import com.xyrlsz.xcimoc.source.Mangakakalot;
import com.xyrlsz.xcimoc.source.Manhuatai;
import com.xyrlsz.xcimoc.source.Manhuayu;
import com.xyrlsz.xcimoc.source.MiGu;
import com.xyrlsz.xcimoc.source.TTKMH;
import com.xyrlsz.xcimoc.source.Tencent;
import com.xyrlsz.xcimoc.source.Vomicmh;
import com.xyrlsz.xcimoc.source.YKMH;
import com.xyrlsz.xcimoc.source.YYManHua;
import com.xyrlsz.xcimoc.source.ZaiManhua;
import com.xyrlsz.xcimoc.utils.HintUtils;

import java.util.ArrayList;
import java.util.List;

public class BrowserFilter extends BaseActivity {
    public static final String URL_KEY = "url";

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
        list.add(Animx2.TYPE);
        list.add(Baozi.TYPE);
        list.add(BuKa.TYPE);
        list.add(Cartoonmad.TYPE);
        list.add(CopyMH.TYPE);
        list.add(DM5.TYPE);
//        list.add(Dmzj.TYPE);
//        list.add(GuFeng.TYPE);
        list.add(HotManga.TYPE);
        list.add(IKanman.TYPE);
        list.add(Mangakakalot.TYPE);
        list.add(MangaBZ.TYPE);
        list.add(Manhuatai.TYPE);
        list.add(MiGu.TYPE);
        list.add(MYCOMIC.TYPE);
        list.add(Tencent.TYPE);
        list.add(DongManManHua.TYPE);
        list.add(YKMH.TYPE);
        list.add(DuManWu.TYPE);
        list.add(DuManWuOrg.TYPE);
        list.add(Komiic.TYPE);
        list.add(Manhuayu.TYPE);
        list.add(GoDaManHua.TYPE);
        list.add(TTKMH.TYPE);
        list.add(Vomicmh.TYPE);
        list.add(YYManHua.TYPE);
        list.add(DmzjV4.TYPE);
        list.add(ZaiManhua.TYPE);
        return list;
    }

    private void openReader(Uri uri) {
        try {
            SourceManager mSourceManager = SourceManager.getInstance(this);
            String comicId;

            for (int i : registUrlListener()) {
                boolean isHere = mSourceManager.getParser(i).isHere(uri);
                comicId = mSourceManager.getParser(i).getComicId(uri);
                if (isHere && comicId != null) {
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

        // 来自输入链接
        else if (intent.hasExtra(URL_KEY)) {
            String url = intent.getStringExtra(URL_KEY);
            openReader(Uri.parse(url));
        }

        //来自分享
        else if (Intent.ACTION_SEND.equals(action) && "text/plain".equals(type)) {
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
