package com.lezw.transformjson.dto;

import org.fxmisc.richtext.model.StyleSpans;
import java.util.Collection;

/**
 * Highlights strings in various data formats.
 * @author zhongwei.long
 * @date 2021年10月13日 下午5:03
 */

public interface Highlighter {
    /**
     * @param text The string to highlight
     */
    StyleSpans<Collection<String>> computeHighlighting(String text);
}