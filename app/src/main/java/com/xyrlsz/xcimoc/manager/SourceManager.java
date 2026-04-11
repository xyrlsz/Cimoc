package com.xyrlsz.xcimoc.manager;

import android.util.SparseArray;

import com.xyrlsz.xcimoc.App;
import com.xyrlsz.xcimoc.component.AppGetter;
import com.xyrlsz.xcimoc.model.Source;
import com.xyrlsz.xcimoc.model.Source_;
import com.xyrlsz.xcimoc.parser.MangaParser;
import com.xyrlsz.xcimoc.source.Baozi;
import com.xyrlsz.xcimoc.source.BuKa;
import com.xyrlsz.xcimoc.source.CopyMH;
import com.xyrlsz.xcimoc.source.CopyMHWeb;
import com.xyrlsz.xcimoc.source.DM5;
import com.xyrlsz.xcimoc.source.DongManManHua;
import com.xyrlsz.xcimoc.source.DuManWu;
import com.xyrlsz.xcimoc.source.DuManWuApp;
import com.xyrlsz.xcimoc.source.GFMH;
import com.xyrlsz.xcimoc.source.GoDaManHua;
import com.xyrlsz.xcimoc.source.HotManga;
import com.xyrlsz.xcimoc.source.IKanman;
import com.xyrlsz.xcimoc.source.Komiic;
import com.xyrlsz.xcimoc.source.Locality;
import com.xyrlsz.xcimoc.source.MH5;
import com.xyrlsz.xcimoc.source.MYCOMIC;
import com.xyrlsz.xcimoc.source.ManBen;
import com.xyrlsz.xcimoc.source.ManWa;
import com.xyrlsz.xcimoc.source.MangaBZ;
import com.xyrlsz.xcimoc.source.Manhuatai;
import com.xyrlsz.xcimoc.source.Manhuayu;
import com.xyrlsz.xcimoc.source.Null;
import com.xyrlsz.xcimoc.source.Tencent;
import com.xyrlsz.xcimoc.source.Vomicmh;
import com.xyrlsz.xcimoc.source.YKMH;
import com.xyrlsz.xcimoc.source.YYManHua;
import com.xyrlsz.xcimoc.source.ZaiManhua;

import java.util.List;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import io.objectbox.query.QueryBuilder;
import okhttp3.Headers;
import rx.Observable;
import rx.schedulers.Schedulers;

/**
 * Created by Hiroshi on 2016/8/11.
 */
public class SourceManager {

    private static volatile SourceManager mInstance;

    // 1. 修改：使用 ObjectBox 的 Box 替代 SourceDao
    private final Box<Source> mSourceBox;
    private final SparseArray<MangaParser> mParserArray = new SparseArray<>();

    private SourceManager(AppGetter getter) {
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
        return mSourceBox.query()
                .equal(Source_.type, type)
                .build()
                .findFirst();
    }

    // 5. 修改：CRUD 操作
    public long insert(Source source) {
        return mSourceBox.put(source); // put 返回 id
    }

    public void update(Source source) {
        mSourceBox.put(source);
    }

    // 6. 保持不变：解析器管理逻辑（这部分与数据库无关）
    public MangaParser getParser(int type) {
        MangaParser parser = mParserArray.get(type);
        if (parser == null) {
            Source source = load(type);
            parser = switch (type) {
                case IKanman.TYPE -> new IKanman(source);
                case DM5.TYPE -> new DM5(source);
                case Locality.TYPE -> new Locality();
                case Tencent.TYPE -> new Tencent(source);
                case BuKa.TYPE -> new BuKa(source);
                case Manhuatai.TYPE -> new Manhuatai(source);
                case CopyMH.TYPE -> new CopyMH(source);
                case HotManga.TYPE -> new HotManga(source);
                case MangaBZ.TYPE -> new MangaBZ(source);
                case DongManManHua.TYPE -> new DongManManHua(source);
                case YKMH.TYPE -> new YKMH(source);
                case Baozi.TYPE -> new Baozi(source);
                case MYCOMIC.TYPE -> new MYCOMIC(source);
                case DuManWu.TYPE -> new DuManWu(source);
                case Komiic.TYPE -> new Komiic(source);
                case Manhuayu.TYPE -> new Manhuayu(source);
                case GoDaManHua.TYPE -> new GoDaManHua(source);
                case Vomicmh.TYPE -> new Vomicmh(source);
                case YYManHua.TYPE -> new YYManHua(source);
                case ZaiManhua.TYPE -> new ZaiManhua(source);
                case ManBen.TYPE -> new ManBen(source);
                case GFMH.TYPE -> new GFMH(source);
                case ManWa.TYPE -> new ManWa(source);
                case MH5.TYPE -> new MH5(source);
                case DuManWuApp.TYPE -> new DuManWuApp(source);
                case CopyMHWeb.TYPE -> new CopyMHWeb(source);
                default -> new Null();
            };
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
            Headers headers = getParser(type).getHeader();
            App.setHeaders(headers);
            return headers;
        }
    }
}