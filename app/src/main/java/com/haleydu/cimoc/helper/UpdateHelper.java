package com.haleydu.cimoc.helper;

import com.haleydu.cimoc.BuildConfig;
import com.haleydu.cimoc.manager.PreferenceManager;
import com.haleydu.cimoc.database.AppDatabase;
import com.haleydu.cimoc.model.Comic;
import com.haleydu.cimoc.model.Source;
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
import com.haleydu.cimoc.source.MYCOMIC;
import com.haleydu.cimoc.source.MangaBZ;
import com.haleydu.cimoc.source.MangaNel;
import com.haleydu.cimoc.source.Mangakakalot;
import com.haleydu.cimoc.source.Manhuatai;
import com.haleydu.cimoc.source.MiGu;
import com.haleydu.cimoc.source.Tencent;
import com.haleydu.cimoc.source.Webtoon;
import com.haleydu.cimoc.source.WebtoonDongManManHua;
import com.haleydu.cimoc.source.YKMH;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Hiroshi on 2017/1/18.
 * Modified for Room database.
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
        ComicSourceHash.put(DM5.TYPE, DM5.getDefaultSource());
        ComicSourceHash.put(Dmzj.TYPE, Dmzj.getDefaultSource());
        ComicSourceHash.put(Dmzjv2.TYPE, Dmzjv2.getDefaultSource());
        ComicSourceHash.put(Dmzjv3.TYPE, Dmzjv3.getDefaultSource());
        ComicSourceHash.put(GuFeng.TYPE, GuFeng.getDefaultSource());
        ComicSourceHash.put(HHAAZZ.TYPE, HHAAZZ.getDefaultSource());
        ComicSourceHash.put(HotManga.TYPE, HotManga.getDefaultSource());
        ComicSourceHash.put(IKanman.TYPE, IKanman.getDefaultSource());
        ComicSourceHash.put(Mangakakalot.TYPE, Mangakakalot.getDefaultSource());
        ComicSourceHash.put(MangaBZ.TYPE, MangaBZ.getDefaultSource());
        ComicSourceHash.put(MangaNel.TYPE, MangaNel.getDefaultSource());
        ComicSourceHash.put(Manhuatai.TYPE, Manhuatai.getDefaultSource());
        ComicSourceHash.put(MiGu.TYPE, MiGu.getDefaultSource());
        ComicSourceHash.put(MYCOMIC.TYPE, MYCOMIC.getDefaultSource());
        ComicSourceHash.put(Tencent.TYPE, Tencent.getDefaultSource());
        ComicSourceHash.put(Webtoon.TYPE, Webtoon.getDefaultSource());
        ComicSourceHash.put(WebtoonDongManManHua.TYPE, WebtoonDongManManHua.getDefaultSource());
        ComicSourceHash.put(YKMH.TYPE, YKMH.getDefaultSource());
        ComicSourceHash.put(DuManWu.TYPE, DuManWu.getDefaultSource());
        ComicSourceHash.put(DuManWuOrg.TYPE, DuManWuOrg.getDefaultSource());
    }

    public static void update(PreferenceManager manager, AppDatabase database) {
        int version = manager.getInt(PreferenceManager.PREF_APP_VERSION, 0);
        if (version != VERSION) {
            initSource(database);
            manager.putInt(PreferenceManager.PREF_APP_VERSION, VERSION);
        }
        new UpdateHelper().updateComicSource(database);
    }

    /**
     * app: 1.4.8.0 -> 1.4.8.1
     * 删除本地漫画中 download 字段的值
     */
    private static void deleteDownloadFromLocal(final AppDatabase database) {
        database.runInTransaction(() -> {
            List<Comic> list = database.comicDao().findLocalComics();
            if (!list.isEmpty()) {
                for (Comic comic : list) {
                    comic.setDownload(null);
                }
                database.comicDao().updateComics(list);
            }
        });
    }

    /**
     * 初始化图源
     */
    private static void initSource(AppDatabase database) {
        List<Source> list = new ArrayList<>();
        list.add(IKanman.getDefaultSource());
        list.add(Dmzjv3.getDefaultSource());
        list.add(HHAAZZ.getDefaultSource());
        list.add(DM5.getDefaultSource());
        list.add(Webtoon.getDefaultSource());
        list.add(Dmzjv2.getDefaultSource());
        list.add(MangaNel.getDefaultSource());
        list.add(Mangakakalot.getDefaultSource());
        list.add(Cartoonmad.getDefaultSource());
        list.add(Animx2.getDefaultSource());
        list.add(MiGu.getDefaultSource());
        list.add(Tencent.getDefaultSource());
        list.add(BuKa.getDefaultSource());
        list.add(MangaBZ.getDefaultSource());
        list.add(Manhuatai.getDefaultSource());
        list.add(GuFeng.getDefaultSource());
        list.add(HotManga.getDefaultSource());
        list.add(WebtoonDongManManHua.getDefaultSource());
        list.add(YKMH.getDefaultSource());
        list.add(Dmzj.getDefaultSource());
        list.add(Baozi.getDefaultSource());
        list.add(MYCOMIC.getDefaultSource());
        list.add(DuManWu.getDefaultSource());
        list.add(DuManWuOrg.getDefaultSource());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            database.sourceDao().insertSources(list);
        });

    }

    public void updateComicSource(AppDatabase database) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            List<Source> sourceList = database.sourceDao().getAllSources();
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
                database.sourceDao().deleteSources(sourcesToDelete);
            }
            if (!sourcesToAdd.isEmpty()) {
                database.sourceDao().insertSources(sourcesToAdd);
            }
        });

    }
}