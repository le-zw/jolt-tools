package com.lezw.codeeditor;

/**
 * @author longzhongwei
 */
public class JsString {
    public static String quote(String data) {
        StringBuilder writer = new StringBuilder();
        if (data == null) {
            writer.append("null");
            return writer.toString();
        }
        writer.append('"');
        for (int i = 0, n = data.length(); i < n; ++i) {
            final char c = data.charAt(i);
            switch (c) {
                case '\\':
                case '"':
                    writer.append('\\').append(c);
                    break;
                case '\b':
                    writer.append("\\b");
                    break;
                case '\t':
                    writer.append("\\t");
                    break;
                case '\n':
                    writer.append("\\n");
                    break;
                case '\f':
                    writer.append("\\f");
                    break;
                case '\r':
                    writer.append("\\r");
                    break;
                default:
                    writer.append(c);
            }
        }
        writer.append('"');
        return writer.toString();
    }
}
