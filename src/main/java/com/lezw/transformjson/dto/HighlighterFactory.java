package com.lezw.transformjson.dto;

import java.util.HashMap;

/**
 * Provides Highlighters for computing syntax highlighting of specific data formats.
 * @author zhongwei.long
 * @date 2021/10/14 上午10:16
 */
public class HighlighterFactory {
    private static HashMap<String, Highlighter> highlighters;

    static {
        highlighters = new HashMap<>();
        highlighters.put("JSON", new JSONHighlighter());
    }

    public static Highlighter getHighlighter(String name) {
        return highlighters.get(name);
    }
}