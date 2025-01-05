package com.haleydu.cimoc.utils;

import com.haleydu.cimoc.model.Comic;
import com.haleydu.cimoc.source.Cartoonmad;
import com.haleydu.cimoc.source.DM5;
import com.haleydu.cimoc.source.MiGu;

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

