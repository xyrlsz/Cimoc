package com.haleydu.cimoc.manager;

import android.util.SparseArray;

import com.haleydu.cimoc.component.AppGetter;
import com.haleydu.cimoc.model.Source;
import com.haleydu.cimoc.model.SourceDao;
import com.haleydu.cimoc.model.SourceDao.Properties;
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
                case Dmzjv3.TYPE:
                    parser = new Dmzjv3(source);
                    break;
                case HHAAZZ.TYPE:
                    parser = new HHAAZZ(source);
                    break;
//                case CCTuku.TYPE:
//                    parser = new CCTuku(source);
//                    break;
//                case U17.TYPE:
//                    parser = new U17(source);
//                    break;
                case DM5.TYPE:
                    parser = new DM5(source);
                    break;
                case Webtoon.TYPE:
                    parser = new Webtoon(source);
                    break;
//                case HHSSEE.TYPE:
//                    parser = new HHSSEE(source);
//                    break;
//                case MH57.TYPE:
//                    parser = new MH57(source);
//                    break;
//                case MH50.TYPE:
//                    parser = new MH50(source);
//                    break;
                case Dmzjv2.TYPE:
                    parser = new Dmzjv2(source);
                    break;
                case Locality.TYPE:
                    parser = new Locality();
                    break;
                case MangaNel.TYPE:
                    parser = new MangaNel(source);
                    break;

                //feilong
//                case PuFei.TYPE:
//                    parser = new PuFei(source);
//                    break;
                case Tencent.TYPE:
                    parser = new Tencent(source);
                    break;
                case BuKa.TYPE:
                    parser = new BuKa(source);
                    break;
//                case EHentai.TYPE:
//                    parser = new EHentai(source);
//                    break;
//                case QiManWu.TYPE:
//                    parser = new QiManWu(source);
//                    break;
//                case Hhxxee.TYPE:
//                    parser = new Hhxxee(source);
//                    break;
                case Cartoonmad.TYPE:
                    parser = new Cartoonmad(source);
                    break;
                case Animx2.TYPE:
                    parser = new Animx2(source);
                    break;
//                case MH517.TYPE:
//                    parser = new MH517(source);
//                    break;
                case MiGu.TYPE:
                    parser = new MiGu(source);
                    break;
//                case BaiNian.TYPE:
//                    parser = new BaiNian(source);
//                    break;
//                case ChuiXue.TYPE:
//                    parser = new ChuiXue(source);
//                    break;
//                case TuHao.TYPE:
//                    parser = new TuHao(source);
//                    break;
//                case SixMH.TYPE:
//                    parser = new SixMH(source);
//                    break;
//                case ManHuaDB.TYPE:
//                    parser = new ManHuaDB(source);
//                    break;
                case Manhuatai.TYPE:
                    parser = new Manhuatai(source);
                    break;
                case GuFeng.TYPE:
                    parser = new GuFeng(source);
                    break;
//                case CCMH.TYPE:
//                    parser = new CCMH(source);
//                    break;
//                case MHLove.TYPE:
//                    parser = new MHLove(source);
//                    break;
//                case YYLS.TYPE:
//                    parser = new YYLS(source);
//                    break;
//                case JMTT.TYPE:
//                    parser = new JMTT(source);
//                    break;

                //haleydu
                case Mangakakalot.TYPE:
                    parser = new Mangakakalot(source);
                    break;
//                case Ohmanhua.TYPE:
//                    parser = new Ohmanhua(source);
//                    break;
//                case CopyMH.TYPE:
//                    parser = new CopyMH(source);
//                    break;
                case HotManga.TYPE:
                    parser = new HotManga(source);
                    break;
                case MangaBZ.TYPE:
                    parser = new MangaBZ(source);
                    break;
                case WebtoonDongManManHua.TYPE:
                    parser = new WebtoonDongManManHua(source);
                    break;
//                case MH160.TYPE:
//                    parser = new MH160(source);
//                    break;
//                case QiMiaoMH.TYPE:
//                    parser = new QiMiaoMH(source);
//                    break;
                case YKMH.TYPE:
                    parser = new YKMH(source);
                    break;
//                case DmzjFix.TYPE:
//                    parser = new DmzjFix(source);
//                    break;
                case Dmzj.TYPE:
                    parser = new Dmzj(source);
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
                case DuManWuOrg.TYPE:
                    parser = new DuManWuOrg(source);
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
