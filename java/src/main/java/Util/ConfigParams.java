package Util;

import java.util.HashMap;

public final class ConfigParams {

    /**
     * Default prefix.
     */
    public static String namespace;

    /**
     * all prefix:suffix map
     */
    public static HashMap<String, String> prefixes = new HashMap<>();

    // private constructor, no instantiation
    private ConfigParams() {
    }
}
