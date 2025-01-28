package com.xyrlsz.xcimoc.utils;

import com.xyrlsz.xcimoc.model.Comic;
import com.xyrlsz.xcimoc.source.Cartoonmad;
import com.xyrlsz.xcimoc.source.DM5;
import com.xyrlsz.xcimoc.source.MiGu;

public class interpretationUtils {

    public static boolean isReverseOrder(Comic comic) {
        int type = comic.getSource();
        return
                //type == MH50.TYPE ||
                type == MiGu.TYPE ||
                        //type == CCMH.TYPE ||
                        type == Cartoonmad.TYPE ||
                        //type == JMTT.TYPE ||
                        //type == Manhuatai.TYPE ||
                        //type == Tencent.TYPE ||
                        //type == GuFeng.TYPE ||
                        //type == CopyMH.TYPE ||
                        type == DM5.TYPE;
    }
}

