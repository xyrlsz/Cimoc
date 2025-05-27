package com.xyrlsz.xcimoc.test;

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
import com.xyrlsz.xcimoc.source.HotManga;
import com.xyrlsz.xcimoc.source.IKanman;
import com.xyrlsz.xcimoc.source.Komiic;
import com.xyrlsz.xcimoc.source.MYCOMIC;
import com.xyrlsz.xcimoc.source.MangaBZ;
import com.xyrlsz.xcimoc.source.Mangakakalot;
import com.xyrlsz.xcimoc.source.Manhuatai;
import com.xyrlsz.xcimoc.source.Manhuayu;
import com.xyrlsz.xcimoc.source.TTKMH;
import com.xyrlsz.xcimoc.source.Tencent;
import com.xyrlsz.xcimoc.source.Vomicmh;
import com.xyrlsz.xcimoc.source.YKMH;
import com.xyrlsz.xcimoc.source.YYManHua;

import java.util.Map;

public class ComicTestSet {
    private static Map<Integer, String> comicTestSet;

    private static void init() {
        comicTestSet.put(Animx2.TYPE, "index-comic-name-女子學院的男生-id-24755");
        comicTestSet.put(Baozi.TYPE, "yirenzhixia-dongmantang");
        comicTestSet.put(BuKa.TYPE, "modoujingbingdenuli");
        comicTestSet.put(Cartoonmad.TYPE, "3583");
        comicTestSet.put(CopyMH.TYPE, "wueyxingxuanlv");
        comicTestSet.put(DM5.TYPE, "manhua-ruoyetongxuexiangrangnimingbaixinyi");
        comicTestSet.put(Dmzj.TYPE, "benghuai3rd");
        comicTestSet.put(HotManga.TYPE, "wueyxingxuanlv");
        comicTestSet.put(IKanman.TYPE, "7580");
        comicTestSet.put(Mangakakalot.TYPE, "jujutsu-kaisen-168");
        comicTestSet.put(MangaBZ.TYPE, "38bz");
        comicTestSet.put(Manhuatai.TYPE, "27417");
        comicTestSet.put(MYCOMIC.TYPE, "7580");
        comicTestSet.put(Tencent.TYPE, "531490");
        comicTestSet.put(DongManManHua.TYPE, "feirenzai");
        comicTestSet.put(YKMH.TYPE, "yiquanchaoren");
        comicTestSet.put(DuManWu.TYPE, "DjbhjZq");
        comicTestSet.put(DuManWuOrg.TYPE, "Xkq44agdqW");
        comicTestSet.put(Komiic.TYPE, "2814");
        comicTestSet.put(Manhuayu.TYPE, "9113");
        comicTestSet.put(GoDaManHua.TYPE, "quanzhifashi-yuewenmanhua");
        comicTestSet.put(TTKMH.TYPE, "427435");
        comicTestSet.put(Vomicmh.TYPE, "92561");
        comicTestSet.put(YYManHua.TYPE, "178yy");
    }

    public static Map<Integer, String> getComicTestSet() {
        if (comicTestSet == null) {
            init();
        }
        return comicTestSet;
    }

    public static void setComicTestSet(Map<Integer, String> comicTestSet) {
        ComicTestSet.comicTestSet = comicTestSet;
    }
}
