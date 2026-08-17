package com.xyrlsz.xcimocob.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Hiroshi on 2016/9/3.
 */
public class StringUtils {
    private static final Gson gson = new Gson();

    public static String getUrlProtocolAndDomain(String urlStr) {
        try {
            // 如果没有协议，添加默认协议以安全解析
            if (!urlStr.startsWith("http://") && !urlStr.startsWith("https://")) {
                urlStr = "http://" + urlStr;
            }
            URL url = new URL(urlStr);
            String protocol = url.getProtocol();
            String host = url.getHost();

            if (host == null || host.isEmpty()) {
                throw new IllegalArgumentException("Invalid URL");
            }

            return protocol + "://" + host;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean endWith(String str, String... args) {
        if (str != null) {
            for (String arg : args) {
                if (str.endsWith(arg)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String filter(String str) {
        if (str == null) {
            return null;
        }
        return str.replaceAll("\\|\\\\\\?\\*<\":\\+\\[\\]/'", "");
    }

    public static boolean isEmpty(String... args) {
        for (String arg : args) {
            if (arg == null || arg.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public static String split(String str, String regex, int position) {
        if (str == null) {
            return null;
        }
        String[] array = str.split(regex);
        if (position < 0) {
            position = array.length + position;
        }
        return position < 0 || position >= array.length ? null : array[position];
    }

    public static String replaceAll(String str, String regex, String replacement) {
        if (str == null) {
            return null;
        }
        return str.replaceAll(regex, replacement);
    }

    public static String substring(String str, int start) {
        return substring(str, start, -1);
    }

    public static String substring(String str, int start, int end) {
        if (str == null) {
            return null;
        }
        if (end < 0) {
            end = str.length() + 1 + end;
        }
        if (start >= 0 && start <= str.length()) {
            return str.substring(start, end);
        }
        return null;
    }

    public static String format(String format, Object... args) {
        return String.format(Locale.getDefault(), format, args);
    }

    public static String getProgress(int progress, int max) {
        return format("%d/%d", progress, max);
    }

    public static String getFormatTime(String format, long time) {
        return new SimpleDateFormat(format, Locale.getDefault()).format(new Date(time));
    }

    public static String getDateStringWithSuffix(String suffix) {
        return new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date()).concat(".").concat(suffix);
    }

    public static String match(String regex, String input, int group) {
        try {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(input);
            if (matcher.find()) {
                return Objects.requireNonNull(matcher.group(group)).trim();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String[] match(String regex, String input, int... group) {
        try {
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(input);
            if (matcher.find()) {
                String[] result = new String[group.length];
                for (int i = 0; i != result.length; ++i) {
                    result[i] = matcher.group(group[i]);
                }
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String join(String[] array, String delimiter) {
        if (array == null || array.length == 0) {
            return "";
        }
        return String.join(delimiter, array);
    }

    public static String getNumber(String str) {
        if (str == null) {
            return null;
        }
        return str.replaceAll("\\D", "");
    }

    public static String extractJson(String input) {
        if (input == null) return null;

        int start = -1;
        int braceCount = 0;
        int bracketCount = 0;
        boolean inString = false;
        char prev = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            // 1. 定位起始位置（第一个 { 或 [）
            if (start == -1) {
                if (c == '{' || c == '[') {
                    start = i;
                    if (c == '{') braceCount++;
                    else bracketCount++;
                }
                continue;
            }

            // 2. 开始计数（必须处理字符串内的干扰符号）
            // 遇到双引号且前一个字符不是反斜杠，则翻转字符串状态
            if (c == '"' && prev != '\\') {
                inString = !inString;
            }

            if (!inString) {
                if (c == '{') braceCount++;
                else if (c == '}') braceCount--;
                else if (c == '[') bracketCount++;
                else if (c == ']') bracketCount--;
            }

            // 3. 判断是否回到根级别（计数归零）
            if (braceCount == 0 && bracketCount == 0) {
                // 确保结束的符号与起始符号匹配
                char startChar = input.charAt(start);
                if ((startChar == '{' && c == '}') || (startChar == '[' && c == ']')) {
                    try {
                        String candidate = input.substring(start, i + 1);
                        JsonElement res = gson.fromJson(candidate, JsonElement.class);
                        if (res.isJsonObject()) {
                            return candidate; // 合法则返回
                        }
                    } catch (JsonSyntaxException e) {
                        // 极少情况：如果起始符误判（如字符串里有个单独的 { 被当成开头），
                        // 但计数法几乎不会误判，若真出错则返回 null
                        return null;
                    }
                }
            }
            prev = c;
        }
        return null; // 未找到完整 JSON
    }

}
