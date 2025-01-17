package com.haleydu.cimoc.manager;

import android.util.SparseArray;

import com.haleydu.cimoc.component.AppGetter;
import com.haleydu.cimoc.database.AppDatabase;
import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.parser.MangaParser;
import com.haleydu.cimoc.source.Animx2;
import com.haleydu.cimoc.source.Baozi;
import com.haleydu.cimoc.source.BuKa;
import com.haleydu.cimoc.source.Cartoonmad;
import com.haleydu.cimoc.source.DM5;
import com.haleydu.cimoc.source.Dmzj;
import com.haleydu.cimoc.source.Dmzjv2;
import com.haleydu.cimoc.source.Dmzjv3;
import com.haleydu.cimoc.source.DuManWu;
import com.haleydu.cimoc.source.DuManWuOrg;
import com.haleydu.cimoc.source.GuFeng;
import com.haleydu.cimoc.source.HHAAZZ;
import com.haleydu.cimoc.source.HotManga;
import com.haleydu.cimoc.source.IKanman;
import com.haleydu.cimoc.source.Locality;
import com.haleydu.cimoc.source.MYCOMIC;
import com.haleydu.cimoc.source.MangaBZ;
import com.haleydu.cimoc.source.MangaNel;
import com.haleydu.cimoc.source.Mangakakalot;
import com.haleydu.cimoc.source.Manhuatai;
import com.haleydu.cimoc.source.MiGu;
import com.haleydu.cimoc.source.Null;
import com.haleydu.cimoc.source.Tencent;
import com.haleydu.cimoc.source.Webtoon;
import com.haleydu.cimoc.source.WebtoonDongManManHua;
import com.haleydu.cimoc.source.YKMH;
import com.haleydu.cimoc.utils.ObservableUtils;

import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.functions.Consumer;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.Headers;
import rx.Observable;


/**
 * Created by Hiroshi on 2016/8/11.
 * Modified for Room database.
 */
public class SourceManager {

    private static SourceManager mInstance;

    private final AppDatabase mDatabase;
    private final SparseArray<MangaParser> mParserArray = new SparseArray<>();

    private SourceManager(AppGetter getter) {
        mDatabase = getter.getAppInstance().getAppDatabase();
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

    public Observable<List<Source>> list() {
        return ObservableUtils.V3toV1(mDatabase.sourceDao()
                .getAllSourcesObservable()
                .toObservable());
    }

    public Observable<List<Source>> listEnableInRx() {
        return ObservableUtils.V3toV1(mDatabase.sourceDao()
                .getEnableSourcesObservable()
                .toObservable());
    }
    public void runInTx(Runnable runnable) {
        mDatabase.runInTransaction(runnable);
    }

    public <T> T callInTx(Callable<T> callable) {
        return mDatabase.runInTransaction(callable);
    }

    public List<Source> listEnable() {
        return mDatabase.sourceDao().getEnableSources();
    }

    public Flowable<Source> load(int type) {
        return mDatabase.sourceDao().getSourceByType(type);
    }

    public long insert(Source source) {
        return mDatabase.sourceDao().insert(source);
    }

    public void update(Source source) {
        mDatabase.sourceDao().update(source);
    }

    public MangaParser getParser(int type) {
        final MangaParser[] parser = {mParserArray.get(type)};
        if (parser[0] == null) {
            Disposable disposable =  load(type).subscribeOn(AndroidSchedulers.mainThread())
                    .observeOn(Schedulers.single())
                    .subscribe(source -> {
                        switch (type) {
                            case IKanman.TYPE:
                                parser[0] = new IKanman(source);
                                break;
                            case Dmzjv3.TYPE:
                                parser[0] = new Dmzjv3(source);
                                break;
                            case HHAAZZ.TYPE:
                                parser[0] = new HHAAZZ(source);
                                break;
                            case DM5.TYPE:
                                parser[0] = new DM5(source);
                                break;
                            case Webtoon.TYPE:
                                parser[0] = new Webtoon(source);
                                break;
                            case Dmzjv2.TYPE:
                                parser[0] = new Dmzjv2(source);
                                break;
                            case Locality.TYPE:
                                parser[0] = new Locality();
                                break;
                            case MangaNel.TYPE:
                                parser[0] = new MangaNel(source);
                                break;
                            case Tencent.TYPE:
                                parser[0] = new Tencent(source);
                                break;
                            case BuKa.TYPE:
                                parser[0] = new BuKa(source);
                                break;
                            case Cartoonmad.TYPE:
                                parser[0] = new Cartoonmad(source);
                                break;
                            case Animx2.TYPE:
                                parser[0] = new Animx2(source);
                                break;
                            case MiGu.TYPE:
                                parser[0] = new MiGu(source);
                                break;
                            case Manhuatai.TYPE:
                                parser[0] = new Manhuatai(source);
                                break;
                            case GuFeng.TYPE:
                                parser[0] = new GuFeng(source);
                                break;
                            case Mangakakalot.TYPE:
                                parser[0] = new Mangakakalot(source);
                                break;
                            case HotManga.TYPE:
                                parser[0] = new HotManga(source);
                                break;
                            case MangaBZ.TYPE:
                                parser[0] = new MangaBZ(source);
                                break;
                            case WebtoonDongManManHua.TYPE:
                                parser[0] = new WebtoonDongManManHua(source);
                                break;
                            case YKMH.TYPE:
                                parser[0] = new YKMH(source);
                                break;
                            case Dmzj.TYPE:
                                parser[0] = new Dmzj(source);
                                break;
                            case Baozi.TYPE:
                                parser[0] = new Baozi(source);
                                break;
                            case MYCOMIC.TYPE:
                                parser[0] = new MYCOMIC(source);
                                break;
                            case DuManWu.TYPE:
                                parser[0] = new DuManWu(source);
                                break;
                            case DuManWuOrg.TYPE:
                                parser[0] = new DuManWuOrg(source);
                                break;
                            default:
                                parser[0] = new Null();
                                break;
                        }
                    });

                mParserArray.put(type, parser[0]);
                disposable.dispose();

        }
        return parser[0];
    }

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