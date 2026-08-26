package com.xyrlsz.xcimocob.manager;

import android.util.SparseArray;

import com.xyrlsz.xcimocob.component.AppGetter;
import com.xyrlsz.xcimocob.model.JsSource;
import com.xyrlsz.xcimocob.model.Source;
import com.xyrlsz.xcimocob.model.Source_;
import com.xyrlsz.xcimocob.parser.MangaParser;
import com.xyrlsz.xcimocob.source.Locality;
import com.xyrlsz.xcimocob.source.Null;
import com.xyrlsz.xcimocob.source.js.JsMangaParser;

import java.util.List;
import java.util.Objects;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.Headers;

/**
 * Created by Hiroshi on 2016/8/11.
 */
public class SourceManager {

    private static volatile SourceManager mInstance;

    // 1. 修改：使用 ObjectBox 的 Box 替代 SourceDao
    private final Box<Source> mSourceBox;
    private final AppGetter mGetter;
    private final SparseArray<MangaParser> mParserArray = new SparseArray<>();

    private SourceManager(AppGetter getter) {
        mGetter = getter;
        // 2. 修改：从 BoxStore 获取 Box
        BoxStore boxStore = getter.getAppInstance().getBoxStore();
        mSourceBox = boxStore.boxFor(Source.class);
    }

    public static SourceManager getInstance(AppGetter getter) {
        if (mInstance == null) {
            synchronized (SourceManager.class) {
                if (mInstance == null) {
                    mInstance = new SourceManager(getter);
                }
            }
        }
        return mInstance;
    }

    // 3. 修改：使用 ObjectBox Query 查询，包装在 Observable 中
    public Observable<List<Source>> list() {
        return Observable.fromCallable(() ->
                mSourceBox.query()
                        .order(Source_.type) // 升序
                        .build()
                        .find()
        ).subscribeOn(Schedulers.io());
    }

    public Observable<List<Source>> listEnableInRx() {
        return Observable.fromCallable(() ->
                mSourceBox.query()
                        .equal(Source_.enable, true)
                        .order(Source_.type)
                        .build()
                        .find()
        ).subscribeOn(Schedulers.io());
    }

    public List<Source> listEnable() {
        return mSourceBox.query()
                .equal(Source_.enable, true)
                .order(Source_.type)
                .build()
                .find();
    }

    // 4. 修改：load 方法。ObjectBox 没有直接的 unique 方法，使用 findFirst
    public Source load(int type) {
        Source res = mSourceBox.query()
                .equal(Source_.type, type)
                .build()
                .findFirst();
        return Objects.requireNonNullElseGet(res, Source::new);
    }

    // 5. 修改：CRUD 操作
    public long insert(Source source) {
        return mSourceBox.put(source); // put 返回 id
    }

    public void update(Source source) {
        mSourceBox.put(source);
    }

    /** 清空解析器缓存（JS 源更新/启用/禁用后调用，使新配置生效）。 */
    public void clearParserCache() {
        mParserArray.clear();
    }

    // 6. 解析器管理：优先启用状态的 JS 源（覆盖内置），否则回退到内置实现
    public MangaParser getParser(int type) {
        MangaParser parser = mParserArray.get(type);
        if (parser == null) {
            if (type == Locality.TYPE) {
                // 本地漫画源（非网络源），保持内置
                parser = new Locality();
            } else {
                // 网络漫画源统一由 JS 源提供；无对应 JS 源时回退到空实现
                JsSource js = JsSourceManager.getInstance(mGetter).loadEnabledByType(type);
                parser = (js != null) ? new JsMangaParser(js) : new Null();
            }
            mParserArray.put(type, parser);
        }
        return parser;
    }

    // 内部类保持不变
    public class TitleGetter {
        public String getTitle(int type) {
            return getParser(type).getTitle();
        }
    }

    public class HeaderGetter {
        public Headers getHeader(int type) {
            return getParser(type).getHeader();
        }
    }
}