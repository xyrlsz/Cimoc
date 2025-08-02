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

    private static final Map<Integer, Source> ComicSourceTable = new HashMap<>();

    public static Map<Integer, Source> getComicSourceTable() {
        if (ComicSourceTable.isEmpty()) {
            initComicSourceTable();
        }
        return ComicSourceTable;
    }

    private static void initComicSourceTable() {

        if (ComicSourceTable.isEmpty()) {
            ComicSourceTable.put(Animx2.TYPE, Animx2.getDefaultSource());
            ComicSourceTable.put(Baozi.TYPE, Baozi.getDefaultSource());
            ComicSourceTable.put(BuKa.TYPE, BuKa.getDefaultSource());
            ComicSourceTable.put(Cartoonmad.TYPE, Cartoonmad.getDefaultSource());
            ComicSourceTable.put(CopyMH.TYPE, CopyMH.getDefaultSource());
            ComicSourceTable.put(DM5.TYPE, DM5.getDefaultSource());
//            ComicSourceTable.put(Dmzj.TYPE, Dmzj.getDefaultSource());
//        ComicSourceHash.put(Dmzjv2.TYPE, Dmzjv2.getDefaultSource());
//        ComicSourceHash.put(Dmzjv3.TYPE, Dmzjv3.getDefaultSource());
//            ComicSourceTable.put(GuFeng.TYPE, GuFeng.getDefaultSource());
//        ComicSourceHash.put(HHAAZZ.TYPE, HHAAZZ.getDefaultSource());
            ComicSourceTable.put(HotManga.TYPE, HotManga.getDefaultSource());
            ComicSourceTable.put(IKanman.TYPE, IKanman.getDefaultSource());
//        ComicSourceHash.put(JMTT.TYPE, JMTT.getDefaultSource());
            ComicSourceTable.put(Mangakakalot.TYPE, Mangakakalot.getDefaultSource());
            ComicSourceTable.put(MangaBZ.TYPE, MangaBZ.getDefaultSource());
//        ComicSourceHash.put(MangaNel.TYPE, MangaNel.getDefaultSource());
            ComicSourceTable.put(Manhuatai.TYPE, Manhuatai.getDefaultSource());
            ComicSourceTable.put(MiGu.TYPE, MiGu.getDefaultSource());
            ComicSourceTable.put(MYCOMIC.TYPE, MYCOMIC.getDefaultSource());
            ComicSourceTable.put(Tencent.TYPE, Tencent.getDefaultSource());
//        ComicSourceHash.put(Webtoon.TYPE, Webtoon.getDefaultSource());
            ComicSourceTable.put(DongManManHua.TYPE, DongManManHua.getDefaultSource());
            ComicSourceTable.put(YKMH.TYPE, YKMH.getDefaultSource());
            ComicSourceTable.put(DuManWu.TYPE, DuManWu.getDefaultSource());
            ComicSourceTable.put(DuManWuOrg.TYPE, DuManWuOrg.getDefaultSource());
            ComicSourceTable.put(Komiic.TYPE, Komiic.getDefaultSource());
            ComicSourceTable.put(Manhuayu.TYPE, Manhuayu.getDefaultSource());
            ComicSourceTable.put(GoDaManHua.TYPE, GoDaManHua.getDefaultSource());
            ComicSourceTable.put(TTKMH.TYPE, TTKMH.getDefaultSource());
            ComicSourceTable.put(Vomicmh.TYPE, Vomicmh.getDefaultSource());
            ComicSourceTable.put(YYManHua.TYPE, YYManHua.getDefaultSource());
            ComicSourceTable.put(DmzjV4.TYPE, DmzjV4.getDefaultSource());
            ComicSourceTable.put(ZaiManhua.TYPE, ZaiManhua.getDefaultSource());
        }
    }

    public static void update(PreferenceManager manager, final DaoSession session) {
        int version = manager.getInt(PreferenceManager.PREF_APP_VERSION, 0);

        if (version != VERSION) {
            if (version < 963 && version != 0) {
                updateChapterTable(session);
            }
            if (version < 1027 && version != 0) {
                updateSourceTable(session);
            }
//            initSource(session);
            initComicSourceTable();
            manager.putInt(PreferenceManager.PREF_APP_VERSION, VERSION);
            updateComicSource(session);

        }

    }

    private static void addChapterCountColumn(Database db) {
        db.beginTransaction();
        db.execSQL("ALTER TABLE COMIC ADD COLUMN CHAPTER_COUNT INTEGER DEFAULT 0");
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    private static void addSourceBaseUrlColumn(Database db) {
        db.beginTransaction();
        db.execSQL("ALTER TABLE SOURCE ADD COLUMN BASE_URL TEXT DEFAULT ''");
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
  /*  private static void initSource(DaoSession session) {
        initComicSourceTable();
//        List<Source> list = new ArrayList<>(ComicSourceTable.values());
//        list.add(IKanman.getDefaultSource());
////        list.add(Dmzjv3.getDefaultSource());
////        list.add(HHAAZZ.getDefaultSource());
////        list.add(CCTuku.getDefaultSource());
////        list.add(U17.getDefaultSource());
//        list.add(DM5.getDefaultSource());
////        list.add(Webtoon.getDefaultSource());
//        //list.add(HHSSEE.getDefaultSource());
////        list.add(MH57.getDefaultSource());
////        list.add(MH50.getDefaultSource());
////        list.add(Dmzjv2.getDefaultSource());
////        list.add(MangaNel.getDefaultSource());
//        list.add(Mangakakalot.getDefaultSource());
////        list.add(PuFei.getDefaultSource());
//        list.add(Cartoonmad.getDefaultSource());
//        list.add(Animx2.getDefaultSource());
////        list.add(MH517.getDefaultSource());
////        list.add(BaiNian.getDefaultSource());
//        list.add(MiGu.getDefaultSource());
//        list.add(Tencent.getDefaultSource());
//        list.add(BuKa.getDefaultSource());
////        list.add(EHentai.getDefaultSource());
////        list.add(QiManWu.getDefaultSource());
////        list.add(Hhxxee.getDefaultSource());
////        list.add(ChuiXue.getDefaultSource());
////        list.add(BaiNian.getDefaultSource());
////        list.add(TuHao.getDefaultSource());
////        list.add(SixMH.getDefaultSource());
//        list.add(MangaBZ.getDefaultSource());
////        list.add(ManHuaDB.getDefaultSource());
//        list.add(Manhuatai.getDefaultSource());
////        list.add(GuFeng.getDefaultSource());
////        list.add(CCMH.getDefaultSource());
//        list.add(Manhuatai.getDefaultSource());
////        list.add(MHLove.getDefaultSource());
////        list.add(YYLS.getDefaultSource());
////        list.add(JMTT.getDefaultSource());
////        list.add(Ohmanhua.getDefaultSource());
//        list.add(CopyMH.getDefaultSource());
//        list.add(HotManga.getDefaultSource());
//        list.add(DongManManHua.getDefaultSource());
////        list.add(MH160.getDefaultSource());
////        list.add(QiMiaoMH.getDefaultSource());
//        list.add(YKMH.getDefaultSource());
////        list.add(DmzjFix.getDefaultSource());
//        list.add(Dmzj.getDefaultSource());
//        list.add(Baozi.getDefaultSource());
//        list.add(MYCOMIC.getDefaultSource());
//        list.add(DuManWu.getDefaultSource());
//        list.add(DuManWuOrg.getDefaultSource());
//        list.add(Komiic.getDefaultSource());
//        list.add(Manhuayu.getDefaultSource());
//        list.add(GoDaManHua.getDefaultSource());
//        list.add(TTKMH.getDefaultSource());
//        list.add(Vomicmh.getDefaultSource());
//        list.add(YYManHua.getDefaultSource());
//        list.add(DmzjV4.getDefaultSource());
//        session.getSourceDao().insertOrReplaceInTx(list);

    }
*/
    private static void updateChapterTable(final DaoSession session) {
        addChapterCountColumn(session.getDatabase());
    }

    private static void updateSourceTable(final DaoSession session) {
        addSourceBaseUrlColumn(session.getDatabase());
    }

    private static void updateComicSource(DaoSession session) {
        SourceDao sourceDao = session.getSourceDao();
        List<Source> sourceList = sourceDao.loadAll();
        List<Source> sourcesToDelete = new ArrayList<>();
        List<Source> sourcesToAdd = new ArrayList<>();
        for (Source source : sourceList) {
            if (!ComicSourceTable.containsKey(source.getType())) {
                sourcesToDelete.add(source);
            }
        }
        for (Integer cType : ComicSourceTable.keySet()) {
            boolean isExist = false;
            for (Source source : sourceList) {
                if (source.getType() == cType) {
                    isExist = true;
                    break;
                }
            }
            if (!isExist) {
                sourcesToAdd.add(ComicSourceTable.get(cType));
            }
        }
        if (!sourcesToDelete.isEmpty()) {
            sourceDao.deleteInTx(sourcesToDelete);
        }
        if (!sourcesToAdd.isEmpty()) {
            sourceDao.insertOrReplaceInTx(sourcesToAdd);
        }
        sourceList = sourceDao.loadAll();
        for (Source source : sourceList) {
            if (ComicSourceTable.containsKey(source.getType())) {
                Source sourceToUpdate = ComicSourceTable.get(source.getType());
//                if (sourceToUpdate != null && (!source.getTitle().equals(sourceToUpdate.getTitle()) ||
//                        !source.getBaseUrl().equals(sourceToUpdate.getBaseUrl()))) {
//                    source.setTitle(sourceToUpdate.getTitle());
//                    source.setBaseUrl(sourceToUpdate.getBaseUrl());
//                    sourceDao.update(source);
//                }
                if (sourceToUpdate != null) {
                    String title1 = source.getTitle();
                    String title2 = sourceToUpdate.getTitle();
                    String baseUrl1 = source.getBaseUrl();
                    String baseUrl2 = sourceToUpdate.getBaseUrl();

                    boolean titleDiff = (title1 == null && title2 != null) || (title1 != null && !title1.equals(title2));
                    boolean baseUrlDiff = (baseUrl1 == null && baseUrl2 != null) || (baseUrl1 != null && !baseUrl1.equals(baseUrl2));

                    if (titleDiff || baseUrlDiff) {
                        source.setTitle(title2);
                        source.setBaseUrl(baseUrl2);
                        sourceDao.update(source);
                    }
                }

            }
        }
    }

}
