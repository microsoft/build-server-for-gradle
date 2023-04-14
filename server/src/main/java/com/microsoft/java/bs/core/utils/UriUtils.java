package com.microsoft.java.bs.core.utils;

import java.net.URI;
import java.net.URISyntaxException;

public class UriUtils {
    private UriUtils() {}

    public static URI uriWithoutQuery(String uriString) throws URISyntaxException {
        URI uri = new URI(uriString);
        if (uri.getQuery() == null) {
            return uri;
        }

        return new URI(uri.getScheme(), uri.getHost(), uri.getPath(), null, uri.getFragment());
    }
}
