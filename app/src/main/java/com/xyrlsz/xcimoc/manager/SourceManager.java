package com.xyrlsz.xcimoc.manager;

import android.util.SparseArray;

import com.xyrlsz.xcimoc.component.AppGetter;
import com.xyrlsz.xcimoc.model.Source;
import com.xyrlsz.xcimoc.model.SourceDao;
import com.xyrlsz.xcimoc.model.SourceDao.Properties;
import com.xyrlsz.xcimoc.parser.MangaParser;
import com.xyrlsz.xcimoc.source.Animx2;
import com.xyrlsz.xcimoc.source.Baozi;
import com.xyrlsz.xcimoc.source.BuKa;
import com.xyrlsz.xcimoc.source.Cartoonmad;
import com.xyrlsz.xcimoc.source.CopyMH;
import com.xyrlsz.xcimoc.source.DM5;
import com.xyrlsz.xcimoc.source.DongManManHua;
import com.xyrlsz.xcimoc.source.DuManWu;
import com.xyrlsz.xcimoc.source.DuManWuOrg;
import com.xyrlsz.xcimoc.source.GFMH;
import com.xyrlsz.xcimoc.source.GoDaManHua;
import com.xyrlsz.xcimoc.source.HotManga;
import com.xyrlsz.xcimoc.source.IKanman;
import com.xyrlsz.xcimoc.source.Komiic;
import com.xyrlsz.xcimoc.source.Locality;
import com.xyrlsz.xcimoc.source.MYCOMIC;
import com.xyrlsz.xcimoc.source.ManBen;
import com.xyrlsz.xcimoc.source.MangaBZ;
import com.xyrlsz.xcimoc.source.Mangakakalot;
import com.xyrlsz.xcimoc.source.Manhuatai;
import com.xyrlsz.xcimoc.source.Manhuayu;
import com.xyrlsz.xcimoc.source.Null;
import com.xyrlsz.xcimoc.source.TTKMH;
import com.xyrlsz.xcimoc.source.Tencent;
import com.xyrlsz.xcimoc.source.Vomicmh;
import com.xyrlsz.xcimoc.source.YKMH;
import com.xyrlsz.xcimoc.source.YYManHua;
import com.xyrlsz.xcimoc.source.ZaiManhua;

import java.util.List;

import okhttp3.Headers;
import rx.Observable;

/**
 * Created by Hiroshi on 2016/8/11.
 */
public class SourceManager {

    private static SourceManager mInstance;

    private final SourceDao mSourceDao;
    private final SparseArray<MangaParser> mParserArray = new SparseArray<>();

    private SourceManager(AppGetter getter) {
        mSourceDao = getter.getAppInstance().getDaoSession().getSourceDao();
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
        return mSourceDao.queryBuilder()
                .orderAsc(Properties.Type)
                .rx()
                .list();
    }

    public Observable<List<Source>> listEnableInRx() {
        return mSourceDao.queryBuilder()
                .where(Properties.Enable.eq(true))
                .orderAsc(Properties.Type)
                .rx()
                .list();
    }

    public List<Source> listEnable() {
        return mSourceDao.queryBuilder()
                .where(Properties.Enable.eq(true))
                .orderAsc(Properties.Type)
                .list();
    }

    public Source load(int type) {
        return mSourceDao.queryBuilder()
                .where(Properties.Type.eq(type))
                .unique();
    }

    public long insert(Source source) {
        return mSourceDao.insert(source);
    }

    public void update(Source source) {
        mSourceDao.update(source);
    }

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

                //feilong
                case Tencent.TYPE:
                    parser = new Tencent(source);
                    break;
                case BuKa.TYPE:
                    parser = new BuKa(source);
                    break;
                case Cartoonmad.TYPE:
                    parser = new Cartoonmad(source);
                    break;
                case Animx2.TYPE:
                    parser = new Animx2(source);
                    break;
                case Manhuatai.TYPE:
                    parser = new Manhuatai(source);
                    break;

                //haleydu
                case Mangakakalot.TYPE:
                    parser = new Mangakakalot(source);
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

                // xyrlsz
                case Baozi.TYPE:
                    parser = new Baozi(source);
                    break;
                case MYCOMIC.TYPE:
                    parser = new MYCOMIC(source);
                    break;
                case DuManWu.TYPE:
                    parser = new DuManWu(source);
                    break;
                case DuManWuOrg.TYPE:
                    parser = new DuManWuOrg(source);
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
                case TTKMH.TYPE:
                    parser = new TTKMH(source);
                    break;
                case Vomicmh.TYPE:
                    parser = new Vomicmh(source);
                    break;
                case YYManHua.TYPE:
                    parser = new YYManHua(source);
                    break;
//                case DmzjV4.TYPE:
//                    parser = new DmzjV4(source);
//                    break;
                case ZaiManhua.TYPE:
                    parser = new ZaiManhua(source);
                    break;
                case ManBen.TYPE:
                    parser = new ManBen(source);
                    break;
                case GFMH.TYPE:
                    parser = new GFMH(source);
                    break;
                default:
                    parser = new Null();
                    break;
            }
            mParserArray.put(type, parser);
        }
        return parser;
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
