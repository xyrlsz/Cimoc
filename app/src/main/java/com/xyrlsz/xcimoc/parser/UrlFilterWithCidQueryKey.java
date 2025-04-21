package com.xyrlsz.xcimoc.parser;

public class UrlFilterWithCidQueryKey extends UrlFilter {
    public String CidQueryParameterKey;

    public UrlFilterWithCidQueryKey(String filter, String cidKey) {
        super(filter);
        CidQueryParameterKey = cidKey;
    }
}
