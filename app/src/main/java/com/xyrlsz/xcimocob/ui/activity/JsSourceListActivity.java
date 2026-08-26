package com.xyrlsz.xcimocob.ui.activity;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.xyrlsz.xcimocob.R;
import com.xyrlsz.xcimocob.manager.JsSourceManager;
import com.xyrlsz.xcimocob.model.JsSource;

import java.util.ArrayList;
import java.util.List;

/**
 * 已安装的 JS 源列表。点击某个源进入其「按 JS 配置自动生成」的登录/设置页
 * （{@link JsSourceSettingsActivity}），无需逐源硬编码页面。
 */
public class JsSourceListActivity extends BackActivity {

    private ListView mList;

    @Override
    protected String getDefaultTitle() {
        return getString(R.string.comic_source_js_settings);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.activity_js_source_list;
    }

    @Override
    protected void initViewById() {
        super.initViewById();
        mList = findViewById(R.id.js_source_list);
    }

    @Override
    protected void initView() {
        super.initView();
        List<JsSource> sources = JsSourceManager.getInstance(this).listEnabled();
        List<String> titles = new ArrayList<>();
        for (JsSource s : sources) {
            titles.add(s.getTitle());
        }
        mList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, titles));
        mList.setOnItemClickListener((parent, view, position, id) -> {
            JsSource s = sources.get(position);
            startActivity(JsSourceSettingsActivity.createIntent(this, s.getType(), s.getTitle()));
        });
    }
}
