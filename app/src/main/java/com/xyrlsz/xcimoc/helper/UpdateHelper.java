package com.xyrlsz.xcimoc.helper;

import com.xyrlsz.xcimoc.BuildConfig;
import com.xyrlsz.xcimoc.manager.PreferenceManager;
import com.xyrlsz.xcimoc.model.Comic;
import com.xyrlsz.xcimoc.model.ComicDao;
import com.xyrlsz.xcimoc.model.DaoSession;
import com.xyrlsz.xcimoc.model.Source;
import com.xyrlsz.xcimoc.model.SourceDao;
import com.xyrlsz.xcimoc.source.Animx2;
import com.xyrlsz.xcimoc.source.Baozi;
import com.xyrlsz.xcimoc.source.BuKa;
import com.xyrlsz.xcimoc.source.Cartoonmad;
import com.xyrlsz.xcimoc.source.CopyMH;
import com.xyrlsz.xcimoc.source.DM5;
import com.xyrlsz.xcimoc.source.Dmzj;
import com.xyrlsz.xcimoc.source.DongManManHua;
import com.xyrlsz.xcimoc.source.DuManWu;
import com.xyrlsz.xcimoc.source.DuManWuOrg;
import com.xyrlsz.xcimoc.source.GoDaManHua;
import com.xyrlsz.xcimoc.source.GuFeng;
import com.xyrlsz.xcimoc.source.HotManga;
import com.xyrlsz.xcimoc.source.IKanman;
import com.xyrlsz.xcimoc.source.Komiic;
import com.xyrlsz.xcimoc.source.MYCOMIC;
import com.xyrlsz.xcimoc.source.MangaBZ;
import com.xyrlsz.xcimoc.source.Mangakakalot;
import com.xyrlsz.xcimoc.source.Manhuatai;
import com.xyrlsz.xcimoc.source.Manhuayu;
import com.xyrlsz.xcimoc.source.MiGu;
import com.xyrlsz.xcimoc.source.Tencent;
import com.xyrlsz.xcimoc.source.YKMH;

import org.greenrobot.greendao.database.Database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Hiroshi on 2017/1/18.
 */

public class UpdateHelper {

    // 1.04.08.008
    private static final int VERSION = BuildConfig.VERSION_CODE;

    private static final Map<Integer, Source> ComicSourceHash = new HashMap<>();

    public UpdateHelper() {

        ComicSourceHash.put(Animx2.TYPE, Animx2.getDefaultSource());
        ComicSourceHash.put(Baozi.TYPE, DuManWu.getDefaultSource());
        ComicSourceHash.put(BuKa.TYPE, BuKa.getDefaultSource());
        ComicSourceHash.put(Cartoonmad.TYPE, Cartoonmad.getDefaultSource());
        ComicSourceHash.put(CopyMH.TYPE, CopyMH.getDefaultSource());
        ComicSourceHash.put(DM5.TYPE, DM5.getDefaultSource());
        ComicSourceHash.put(Dmzj.TYPE, Dmzj.getDefaultSource());
//        ComicSourceHash.put(Dmzjv2.TYPE, Dmzjv2.getDefaultSource());
//        ComicSourceHash.put(Dmzjv3.TYPE, Dmzjv3.getDefaultSource());
        ComicSourceHash.put(GuFeng.TYPE, GuFeng.getDefaultSource());
//        ComicSourceHash.put(HHAAZZ.TYPE, HHAAZZ.getDefaultSource());
        ComicSourceHash.put(HotManga.TYPE, HotManga.getDefaultSource());
        ComicSourceHash.put(IKanman.TYPE, IKanman.getDefaultSource());
//        ComicSourceHash.put(JMTT.TYPE, JMTT.getDefaultSource());
        ComicSourceHash.put(Mangakakalot.TYPE, Mangakakalot.getDefaultSource());
        ComicSourceHash.put(MangaBZ.TYPE, MangaBZ.getDefaultSource());
//        ComicSourceHash.put(MangaNel.TYPE, MangaNel.getDefaultSource());
        ComicSourceHash.put(Manhuatai.TYPE, Manhuatai.getDefaultSource());
        ComicSourceHash.put(MiGu.TYPE, MiGu.getDefaultSource());
        ComicSourceHash.put(MYCOMIC.TYPE, MYCOMIC.getDefaultSource());
        ComicSourceHash.put(Tencent.TYPE, Tencent.getDefaultSource());
//        ComicSourceHash.put(Webtoon.TYPE, Webtoon.getDefaultSource());
        ComicSourceHash.put(DongManManHua.TYPE, DongManManHua.getDefaultSource());
        ComicSourceHash.put(YKMH.TYPE, YKMH.getDefaultSource());
        ComicSourceHash.put(DuManWu.TYPE, DuManWu.getDefaultSource());
        ComicSourceHash.put(DuManWuOrg.TYPE, DuManWuOrg.getDefaultSource());
        ComicSourceHash.put(Komiic.TYPE, Komiic.getDefaultSource());
        ComicSourceHash.put(Manhuayu.TYPE, Manhuayu.getDefaultSource());
        ComicSourceHash.put(GoDaManHua.TYPE, GoDaManHua.getDefaultSource());
    }

    public static void update(PreferenceManager manager, final DaoSession session) {
        int version = manager.getInt(PreferenceManager.PREF_APP_VERSION, 0);
        if (version != VERSION) {
            initSource(session);
            manager.putInt(PreferenceManager.PREF_APP_VERSION, VERSION);
            new UpdateHelper().updateComicSource(session);
            if (version < 963 && version != 0) {
                updateChapterTable(session);
            }
        }

    }

    private static void addChapterCountColumn(Database db) {
        db.beginTransaction();
        db.execSQL("ALTER TABLE COMIC ADD COLUMN CHAPTER_COUNT INTEGER DEFAULT 0");
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    /**
     * app: 1.4.8.0 -> 1.4.8.1
     * 删除本地漫画中 download 字段的值
     */
    private static void deleteDownloadFromLocal(final DaoSession session) {
        session.runInTx(new Runnable() {
            @Override
            public void run() {
                ComicDao dao = session.getComicDao();
                List<Comic> list = dao.queryBuilder().where(ComicDao.Properties.Local.eq(true)).list();
                if (!list.isEmpty()) {
                    for (Comic comic : list) {
                        comic.setDownload(null);
                    }
                    dao.updateInTx(list);
                }
            }
        });
    }

    /**
     * 初始化图源
     */
    private static void initSource(DaoSession session) {

        List<Source> list = new ArrayList<>();
        list.add(IKanman.getDefaultSource());
//        list.add(Dmzjv3.getDefaultSource());
//        list.add(HHAAZZ.getDefaultSource());
//        list.add(CCTuku.getDefaultSource());
//        list.add(U17.getDefaultSource());
        list.add(DM5.getDefaultSource());
//        list.add(Webtoon.getDefaultSource());
        //list.add(HHSSEE.getDefaultSource());
//        list.add(MH57.getDefaultSource());
//        list.add(MH50.getDefaultSource());
//        list.add(Dmzjv2.getDefaultSource());
//        list.add(MangaNel.getDefaultSource());
        list.add(Mangakakalot.getDefaultSource());
//        list.add(PuFei.getDefaultSource());
        list.add(Cartoonmad.getDefaultSource());
        list.add(Animx2.getDefaultSource());
//        list.add(MH517.getDefaultSource());
//        list.add(BaiNian.getDefaultSource());
        list.add(MiGu.getDefaultSource());
        list.add(Tencent.getDefaultSource());
        list.add(BuKa.getDefaultSource());
//        list.add(EHentai.getDefaultSource());
//        list.add(QiManWu.getDefaultSource());
//        list.add(Hhxxee.getDefaultSource());
//        list.add(ChuiXue.getDefaultSource());
//        list.add(BaiNian.getDefaultSource());
//        list.add(TuHao.getDefaultSource());
//        list.add(SixMH.getDefaultSource());
        list.add(MangaBZ.getDefaultSource());
//        list.add(ManHuaDB.getDefaultSource());
        list.add(Manhuatai.getDefaultSource());
        list.add(GuFeng.getDefaultSource());
//        list.add(CCMH.getDefaultSource());
        list.add(Manhuatai.getDefaultSource());
//        list.add(MHLove.getDefaultSource());
        list.add(GuFeng.getDefaultSource());
//        list.add(YYLS.getDefaultSource());
//        list.add(JMTT.getDefaultSource());
//        list.add(Ohmanhua.getDefaultSource());
        list.add(CopyMH.getDefaultSource());
        list.add(HotManga.getDefaultSource());
        list.add(DongManManHua.getDefaultSource());
//        list.add(MH160.getDefaultSource());
//        list.add(QiMiaoMH.getDefaultSource());
        list.add(YKMH.getDefaultSource());
//        list.add(DmzjFix.getDefaultSource());
        list.add(Dmzj.getDefaultSource());
        list.add(Baozi.getDefaultSource());
        list.add(MYCOMIC.getDefaultSource());
        list.add(DuManWu.getDefaultSource());
        list.add(DuManWuOrg.getDefaultSource());
        list.add(Komiic.getDefaultSource());
        list.add(Manhuayu.getDefaultSource());
        list.add(GoDaManHua.getDefaultSource());

        session.getSourceDao().insertOrReplaceInTx(list);
    }

    private static void updateChapterTable(final DaoSession session) {
        addChapterCountColumn(session.getDatabase());
    }

    public void updateComicSource(DaoSession session) {
        SourceDao sourceDao = session.getSourceDao();
        List<Source> sourceList = sourceDao.loadAll();
        List<Source> sourcesToDelete = new ArrayList<>();
        List<Source> sourcesToAdd = new ArrayList<>();
        for (Source source : sourceList) {
            if (!ComicSourceHash.containsKey(source.getType())) {
                sourcesToDelete.add(source);
            }
        }

        for (Integer cType : ComicSourceHash.keySet()) {
            boolean isExist = false;
            for (Source source : sourceList) {
                if (source.getType() == cType) {
                    isExist = true;
                    break;
                }
            }
            if (!isExist) {
                sourcesToAdd.add(ComicSourceHash.get(cType));
            }
        }

        if (!sourcesToDelete.isEmpty()) {
            sourceDao.deleteInTx(sourcesToDelete);
        }
        if (!sourcesToAdd.isEmpty()) {
            sourceDao.insertOrReplaceInTx(sourcesToAdd);
        }
    }
}
