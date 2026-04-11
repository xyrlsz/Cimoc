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
 * Modified to use ObjectBox (参照 ComicManager)
 */
public class SourceManager {

    private static SourceManager mInstance;

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
            switch (type) {
                case IKanman.TYPE:
                    parser = new IKanman(source);
                    break;
                case DM5.TYPE:
                    parser = new DM5(source);
                    break;
                case Locality.TYPE:
                    parser = new Locality();
                    break;
                // ... 其他 case 保持不变 ...
                case Tencent.TYPE:
                    parser = new Tencent(source);
                    break;
                case BuKa.TYPE:
                    parser = new BuKa(source);
                    break;
                case Manhuatai.TYPE:
                    parser = new Manhuatai(source);
                    break;
                case CopyMH.TYPE:
                    parser = new CopyMH(source);
                    break;
                case HotManga.TYPE:
                    parser = new HotManga(source);
                    break;
                case MangaBZ.TYPE:
                    parser = new MangaBZ(source);
                    break;
                case DongManManHua.TYPE:
                    parser = new DongManManHua(source);
                    break;
                case YKMH.TYPE:
                    parser = new YKMH(source);
                    break;
                case Baozi.TYPE:
                    parser = new Baozi(source);
                    break;
                case MYCOMIC.TYPE:
                    parser = new MYCOMIC(source);
                    break;
                case DuManWu.TYPE:
                    parser = new DuManWu(source);
                    break;
                case Komiic.TYPE:
                    parser = new Komiic(source);
                    break;
                case Manhuayu.TYPE:
                    parser = new Manhuayu(source);
                    break;
                case GoDaManHua.TYPE:
                    parser = new GoDaManHua(source);
                    break;
                case Vomicmh.TYPE:
                    parser = new Vomicmh(source);
                    break;
                case YYManHua.TYPE:
                    parser = new YYManHua(source);
                    break;
                case ZaiManhua.TYPE:
                    parser = new ZaiManhua(source);
                    break;
                case ManBen.TYPE:
                    parser = new ManBen(source);
                    break;
                case GFMH.TYPE:
                    parser = new GFMH(source);
                    break;
                case ManWa.TYPE:
                    parser = new ManWa(source);
                    break;
                case MH5.TYPE:
                    parser = new MH5(source);
                    break;
                case DuManWuApp.TYPE:
                    parser = new DuManWuApp(source);
                    break;
                case CopyMHWeb.TYPE:
                    parser = new CopyMHWeb(source);
                    break;
                default:
                    parser = new Null();
                    break;
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
            Headers headers = getParser(type).getHeader();
            App.setHeaders(headers);
            return headers;
        }
    }
}