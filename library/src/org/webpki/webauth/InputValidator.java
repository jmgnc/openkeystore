/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.webpki.webauth;

import java.io.IOException;

import java.math.BigInteger;

import java.net.URI;

import java.net.URISyntaxException;

import java.util.Vector;

import org.webpki.json.JSONArrayReader;
import org.webpki.json.JSONDecoder;
import org.webpki.json.JSONObjectReader;

abstract class InputValidator extends JSONDecoder {

    private static final long serialVersionUID = 1L;

    static String getID(JSONObjectReader rd, String name) throws IOException {
        return rd.getString(name);
    }

    static String getURL(JSONObjectReader rd, String name) throws IOException {
        String url = getURI(rd, name);
        if (!url.matches("https?://.*")) {
            bad("Bad URL: " + url);
        }
        return url;
    }

    static private void validateURI(String uriString) throws IOException {
        try {
            URI uri = new URI(uriString);
            if (!uri.isAbsolute()) {
                bad("Bad URI: " + uri);
            }
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    static String getURI(JSONObjectReader rd, String name) throws IOException {
        String uri = rd.getString(name);
        validateURI(uri);
        return uri;
    }

    static void bad(String message) throws IOException {
        throw new IOException(message);
    }

    static String[] getNonEmptyList(JSONObjectReader rd, String name) throws IOException {
        String[] list = rd.getStringArray(name);
        if (list.length == 0) {
            bad("Empty list not allowed: " + name);
        }
        return list;
    }

    static String[] getListConditional(JSONObjectReader rd, String name) throws IOException {
        return rd.hasProperty(name) ? getNonEmptyList(rd, name) : null;
    }

    static String[] getURIList(JSONObjectReader rd, String name) throws IOException {
        String[] uris = getNonEmptyList(rd, name);
        for (String uri : uris) {
            validateURI(uri);
        }
        return uris;
    }

    static String[] getURIListConditional(JSONObjectReader rd, String name) throws IOException {
        return rd.hasProperty(name) ? getURIList(rd, name) : null;
    }

    static BigInteger getBigIntegerConditional(JSONObjectReader rd, String name) throws IOException {
        return rd.hasProperty(name) ? rd.getBigInteger(name) : null;
    }

    static Vector<JSONObjectReader> getObjectArrayConditional(JSONObjectReader rd, String name) throws IOException {
        if (rd.hasProperty(name)) {
            return getObjectArray(rd, name);
        }
        return new Vector<JSONObjectReader>();
    }

    static Vector<JSONObjectReader> getObjectArray(JSONObjectReader rd, String name) throws IOException {
        Vector<JSONObjectReader> result = new Vector<JSONObjectReader>();
        JSONArrayReader arr = rd.getArray(name);
        do {
            result.add(arr.getObject());
        } while (arr.hasMore());
        return result;
    }

    @Override
    final public String getContext() {
        return WebAuthConstants.WEBAUTH_NS;
    }
}
