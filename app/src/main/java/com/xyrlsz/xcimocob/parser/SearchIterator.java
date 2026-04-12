package com.xyrlsz.xcimocob.parser;

import com.xyrlsz.xcimocob.model.Comic;

/**
 * Created by Hiroshi on 2016/9/21.
 */

public interface SearchIterator {

    boolean empty();

    boolean hasNext();

    Comic next();

}
