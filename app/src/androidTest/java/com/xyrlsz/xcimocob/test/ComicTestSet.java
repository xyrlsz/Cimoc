package com.xyrlsz.xcimocob.test;

import com.xyrlsz.xcimocob.source.Baozi;
import com.xyrlsz.xcimocob.source.BuKa;

import com.xyrlsz.xcimocob.source.CopyMH;
import com.xyrlsz.xcimocob.source.DM5;
import com.xyrlsz.xcimocob.source.DongManManHua;
import com.xyrlsz.xcimocob.source.DuManWu;

import com.xyrlsz.xcimocob.source.GoDaManHua;
import com.xyrlsz.xcimocob.source.HotManga;
import com.xyrlsz.xcimocob.source.ManHuaGui;
import com.xyrlsz.xcimocob.source.Komiic;
import com.xyrlsz.xcimocob.source.MYCOMIC;
import com.xyrlsz.xcimocob.source.MangaBZ;
import com.xyrlsz.xcimocob.source.Manhuatai;
import com.xyrlsz.xcimocob.source.Manhuayu;

import com.xyrlsz.xcimocob.source.Tencent;
import com.xyrlsz.xcimocob.source.Vomicmh;
import com.xyrlsz.xcimocob.source.YKMH;
import com.xyrlsz.xcimocob.source.YYManHua;
import com.xyrlsz.xcimocob.source.ZaiManhua;

import java.util.HashMap;
import java.util.Map;

public class ComicTestSet {
    private static Map<Integer, String> comicTestSet = new HashMap<>();

    private static void init() {
//        comicTestSet.put(Animx2.TYPE, "index-comic-name-女子學院的男生-id-24755");
        comicTestSet.put(Baozi.TYPE, "yirenzhixia-dongmantang");
        comicTestSet.put(BuKa.TYPE, "modoujingbingdenuli");
//        comicTestSet.put(Cartoonmad.TYPE, "3583");
        comicTestSet.put(CopyMH.TYPE, "wueyxingxuanlv");
        comicTestSet.put(DM5.TYPE, "manhua-ruoyetongxuexiangrangnimingbaixinyi");
//        comicTestSet.put(Dmzj.TYPE, "benghuai3rd");
        comicTestSet.put(HotManga.TYPE, "wueyxingxuanlv");
        comicTestSet.put(ManHuaGui.TYPE, "7580");
//        comicTestSet.put(Mangakakalot.TYPE, "jujutsu-kaisen-168");
        comicTestSet.put(MangaBZ.TYPE, "38bz");
        comicTestSet.put(Manhuatai.TYPE, "27417");
        comicTestSet.put(MYCOMIC.TYPE, "7580");
        comicTestSet.put(Tencent.TYPE, "531490");
        comicTestSet.put(DongManManHua.TYPE, "feirenzai");
        comicTestSet.put(YKMH.TYPE, "yiquanchaoren");
        comicTestSet.put(DuManWu.TYPE, "DjbhjZq");
//        comicTestSet.put(DuManWuOrg.TYPE, "Xkq44agdqW");
        comicTestSet.put(Komiic.TYPE, "2814");
        comicTestSet.put(Manhuayu.TYPE, "9113");
        comicTestSet.put(GoDaManHua.TYPE, "quanzhifashi-yuewenmanhua");
//        comicTestSet.put(TTKMH.TYPE, "427435");
        comicTestSet.put(Vomicmh.TYPE, "92561");
        comicTestSet.put(YYManHua.TYPE, "178yy");
        comicTestSet.put(ZaiManhua.TYPE, "65391");
    }

    public static Map<Integer, String> getComicTestSet() {
        if (comicTestSet.isEmpty()) {
            init();
        }
        return comicTestSet;
    }

    public static void setComicTestSet(Map<Integer, String> comicTestSet) {
        ComicTestSet.comicTestSet = comicTestSet;
    }
}
