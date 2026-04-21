package com.sms.common.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.util.AntPathMatcher;
import com.sms.common.constant.Constants;
import com.sms.common.core.text.StrFormatter;

/**
 * 字符串工具類
 *
 */
public class StringUtils extends org.apache.commons.lang3.StringUtils
{
    /** 空字符串 */
    private static final String NULLSTR = "";

    /** 下劃線 */
    private static final char SEPARATOR = '_';

    /** 星號 */
    private static final char ASTERISK = '*';

    /**
     * 獲取參數不為空值
     * 
     * @param value defaultValue 要判斷的 value
     * @return value 返回值
     */
    public static <T> T nvl(T value, T defaultValue)
    {
        return value != null ? value : defaultValue;
    }

    /**
     * * 判斷一個 Collection 是否為空，包含 List，Set，Queue
     * 
     * @param coll 要判斷的 Collection
     * @return true：為空 false：非空
     */
    public static boolean isEmpty(Collection<?> coll)
    {
        return isNull(coll) || coll.isEmpty();
    }

    /**
     * * 判斷一個 Collection 是否非空，包含 List，Set，Queue
     * 
     * @param coll 要判斷的 Collection
     * @return true：非空 false：空
     */
    public static boolean isNotEmpty(Collection<?> coll)
    {
        return !isEmpty(coll);
    }

    /**
     * * 判斷一個對象數組是否為空
     * 
     * @param objects 要判斷的對象數組
     ** @return true：為空 false：非空
     */
    public static boolean isEmpty(Object[] objects)
    {
        return isNull(objects) || (objects.length == 0);
    }

    /**
     * * 判斷一個對象數組是否非空
     * 
     * @param objects 要判斷的對象數組
     * @return true：非空 false：空
     */
    public static boolean isNotEmpty(Object[] objects)
    {
        return !isEmpty(objects);
    }

    /**
     * * 判斷一個 Map 是否為空
     * 
     * @param map 要判斷的 Map
     * @return true：為空 false：非空
     */
    public static boolean isEmpty(Map<?, ?> map)
    {
        return isNull(map) || map.isEmpty();
    }
    
    /**
     * * 判斷一個 Map 是否非空
     * 
     * @param map 要判斷的 Map
     * @return true：非空 false：空
     */
    public static boolean isNotEmpty(Map<?, ?> map)
    {
        return !isEmpty(map);
    }

    /**
     * * 判斷一個字符串是否為空串
     * 
     * @param str String
     * @return true：為空 false：非空
     */
    public static boolean isEmpty(String str)
    {
        return isNull(str) || NULLSTR.equals(str.trim());
    }

    /**
     * * 判斷一個字符串是否為非空串
     * 
     * @param str String
     * @return true：非空串 false：空串
     */
    public static boolean isNotEmpty(String str)
    {
        return !isEmpty(str);
    }

    /**
     * * 判斷一個對象是否為空
     * 
     * @param object Object
     * @return true：為空 false：非空
     */
    public static boolean isNull(Object object)
    {
        return object == null;
    }

    /**
     * * 判斷一個對象是否非空
     * 
     * @param object Object
     * @return true：非空 false：空
     */
    public static boolean isNotNull(Object object)
    {
        return !isNull(object);
    }

    /**
     * * 判斷一個對象是否是數組類型（Java 基本型別的數組）
     * 
     * @param object 對象
     * @return true：是數組 false：不是數組
     */
    public static boolean isArray(Object object)
    {
        return isNotNull(object) && object.getClass().isArray();
    }

    /**
     * 去空格
     */
    public static String trim(String str)
    {
        return (str == null ? "" : str.trim());
    }

    /**
     * 替換指定字符串的指定區間內字元為"*"
     *
     * @param str 字符串
     * @param startInclude 開始位置（包含）
     * @param endExclude 結束位置（不包含）
     * @return 替換後的字符串
     */
    public static String hide(CharSequence str, int startInclude, int endExclude)
    {
        if (isEmpty(str))
        {
            return NULLSTR;
        }
        final int strLength = str.length();
        if (startInclude > strLength)
        {
            return NULLSTR;
        }
        if (endExclude > strLength)
        {
            endExclude = strLength;
        }
        if (startInclude > endExclude)
        {
            // 如果起始位置大于结束位置，不替换
            return NULLSTR;
        }
        final char[] chars = new char[strLength];
        for (int i = 0; i < strLength; i++)
        {
            if (i >= startInclude && i < endExclude)
            {
                chars[i] = ASTERISK;
            }
            else
            {
                chars[i] = str.charAt(i);
            }
        }
        return new String(chars);
    }

    /**
     * 截取字符串
     * 
     * @param str 字符串
     * @param start 開始
     * @return 結果
     */
    public static String substring(final String str, int start)
    {
        if (str == null)
        {
            return NULLSTR;
        }

        if (start < 0)
        {
            start = str.length() + start;
        }

        if (start < 0)
        {
            start = 0;
        }
        if (start > str.length())
        {
            return NULLSTR;
        }

        return str.substring(start);
    }

    /**
     * 截取字符串
     * 
     * @param str 字符串
     * @param start 開始
     * @param end 結束
     * @return 結果
     */
    public static String substring(final String str, int start, int end)
    {
        if (str == null)
        {
            return NULLSTR;
        }

        if (end < 0)
        {
            end = str.length() + end;
        }
        if (start < 0)
        {
            start = str.length() + start;
        }

        if (end > str.length())
        {
            end = str.length();
        }

        if (start > end)
        {
            return NULLSTR;
        }

        if (start < 0)
        {
            start = 0;
        }
        if (end < 0)
        {
            end = 0;
        }

        return str.substring(start, end);
    }

    /**
     * 在字符串中查找第一個出現的 `open` 和最後一個出現的 `close` 之間的子字符串
     * 
     * @param str 要截取的字符串
     * @param open 起始字符串
     * @param close 結束字符串
     * @return 截取結果
     */
    public static String substringBetweenLast(final String str, final String open, final String close)
    {
        if (isEmpty(str) || isEmpty(open) || isEmpty(close))
        {
            return NULLSTR;
        }
        final int start = str.indexOf(open);
        if (start != INDEX_NOT_FOUND)
        {
            final int end = str.lastIndexOf(close);
            if (end != INDEX_NOT_FOUND)
            {
                return str.substring(start + open.length(), end);
            }
        }
        return NULLSTR;
    }

    /**
     * 格式化文本，{} 表示佔位符<br>
     * 此方法只是簡單將佔位符 {} 按照順序替換為參數<br>
     * 如果想輸出 {} 使用 \轉義 { 即可，如果想輸出 {} 之前的 \ 使用雙轉義符 \\\\ 即可<br>
     * 例：<br>
     * 通常使用：format("this is {} for {}", "a", "b") -> this is a for b<br>
     * 轉義{}：format("this is \\{} for {}", "a", "b") -> this is \{} for a<br>
     * 轉義\：format("this is \\\\{} for {}", "a", "b") -> this is \a for b<br>
     * 
     * @param template 文本模板，被替換的部分用 {} 表示
     * @param params 參數值
     * @return 格式化後的文本
     */
    public static String format(String template, Object... params)
    {
        if (isEmpty(params) || isEmpty(template))
        {
            return template;
        }
        return StrFormatter.format(template, params);
    }

    /**
     * 是否為 http(s)://開頭
     * 
     * @param link 鏈接
     * @return 結果
     */
    public static boolean ishttp(String link)
    {
        return StringUtils.startsWithAny(link, Constants.HTTP, Constants.HTTPS);
    }

    /**
     * 字符串轉 set
     * 
     * @param str 字符串
     * @param sep 分隔符
     * @return set 集合
     */
    public static final Set<String> str2Set(String str, String sep)
    {
        return new HashSet<String>(str2List(str, sep, true, false));
    }

    /**
     * 字符串轉 list
     * 
     * @param str 字符串
     * @param sep 分隔符
     * @return list 集合
     */
    public static final List<String> str2List(String str, String sep)
    {
        return str2List(str, sep, true, false);
    }

    /**
     * 字符串轉 list
     * 
     * @param str 字符串
     * @param sep 分隔符
     * @param filterBlank 過濾純空白
     * @param trim 去掉首尾空白
     * @return list 集合
     */
    public static final List<String> str2List(String str, String sep, boolean filterBlank, boolean trim)
    {
        List<String> list = new ArrayList<String>();
        if (StringUtils.isEmpty(str))
        {
            return list;
        }

        // 过滤空白字符串
        if (filterBlank && StringUtils.isBlank(str))
        {
            return list;
        }
        String[] split = str.split(sep);
        for (String string : split)
        {
            if (filterBlank && StringUtils.isBlank(string))
            {
                continue;
            }
            if (trim)
            {
                string = string.trim();
            }
            list.add(string);
        }

        return list;
    }

    /**
     * 判斷給定的 collection 列表中是否包含數組 array 判斷給定的數組 array 中是否包含給定的元素 value
     *
     * @param collection 給定的集合
     * @param array 給定的數組
     * @return boolean 結果
     */
    public static boolean containsAny(Collection<String> collection, String... array)
    {
        if (isEmpty(collection) || isEmpty(array))
        {
            return false;
        }
        else
        {
            for (String str : array)
            {
                if (collection.contains(str))
                {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 查找指定字符串是否包含指定字符串列表中的任意一個字符串同時串忽略大小寫
     *
     * @param cs 指定字符串
     * @param searchCharSequences 需要檢查的字符串數組
     * @return 是否包含任意一個字符串
     */
    public static boolean containsAnyIgnoreCase(CharSequence cs, CharSequence... searchCharSequences)
    {
        if (isEmpty(cs) || isEmpty(searchCharSequences))
        {
            return false;
        }
        for (CharSequence testStr : searchCharSequences)
        {
            if (containsIgnoreCase(cs, testStr))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 駝峰轉下劃線命名
     */
    public static String toUnderScoreCase(String str)
    {
        if (str == null)
        {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        // 前置字符是否大写
        boolean preCharIsUpperCase = true;
        // 当前字符是否大写
        boolean curreCharIsUpperCase = true;
        // 下一字符是否大写
        boolean nexteCharIsUpperCase = true;
        for (int i = 0; i < str.length(); i++)
        {
            char c = str.charAt(i);
            if (i > 0)
            {
                preCharIsUpperCase = Character.isUpperCase(str.charAt(i - 1));
            }
            else
            {
                preCharIsUpperCase = false;
            }

            curreCharIsUpperCase = Character.isUpperCase(c);

            if (i < (str.length() - 1))
            {
                nexteCharIsUpperCase = Character.isUpperCase(str.charAt(i + 1));
            }

            if (preCharIsUpperCase && curreCharIsUpperCase && !nexteCharIsUpperCase)
            {
                sb.append(SEPARATOR);
            }
            else if ((i != 0 && !preCharIsUpperCase) && curreCharIsUpperCase)
            {
                sb.append(SEPARATOR);
            }
            sb.append(Character.toLowerCase(c));
        }

        return sb.toString();
    }

    /**
     * 是否包含字符串
     * 
     * @param str 驗證字符串
     * @param strs 字符串組
     * @return 包含返回 true
     */
    public static boolean inStringIgnoreCase(String str, String... strs)
    {
        if (str != null && strs != null)
        {
            for (String s : strs)
            {
                if (str.equalsIgnoreCase(trim(s)))
                {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 刪除最後一個字符串
     *
     * @param str 輸入字符串
     * @param spit 以什麼類型結尾的
     * @return 截取後的字符串
     */
    public static String lastStringDel(String str, String spit)
    {
        if (!StringUtils.isEmpty(str) && str.endsWith(spit))
        {
            return str.subSequence(0, str.length() - 1).toString();
        }
        return str;
    }

    /**
     * 將下劃線大寫方式命名的字符串轉換為駝峰式。如果轉換前的下劃線大寫方式命名的字符串為空，則返回空字符串。例如：HELLO_WORLD->HelloWorld
     * 
     * @param name 轉換前的下劃線大寫方式命名的字符串
     * @return 轉換後的駝峰式命名的字符串
     */
    public static String convertToCamelCase(String name)
    {
        StringBuilder result = new StringBuilder();
        // 快速检查
        if (name == null || name.isEmpty())
        {
            // 没必要转换
            return "";
        }
        else if (!name.contains("_"))
        {
            // 不含下划线，仅将首字母大写
            return name.substring(0, 1).toUpperCase() + name.substring(1);
        }
        // 用下划线将原始字符串分割
        String[] camels = name.split("_");
        for (String camel : camels)
        {
            // 跳过原始字符串中开头、结尾的下换线或双重下划线
            if (camel.isEmpty())
            {
                continue;
            }
            // 首字母大写
            result.append(camel.substring(0, 1).toUpperCase());
            result.append(camel.substring(1).toLowerCase());
        }
        return result.toString();
    }

    /**
     * 駝峰式命名法
     * 例如：user_name->userName
     */
    public static String toCamelCase(String s)
    {
        if (s == null)
        {
            return null;
        }
        if (s.indexOf(SEPARATOR) == -1)
        {
            return s;
        }
        s = s.toLowerCase();
        StringBuilder sb = new StringBuilder(s.length());
        boolean upperCase = false;
        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);

            if (c == SEPARATOR)
            {
                upperCase = true;
            }
            else if (upperCase)
            {
                sb.append(Character.toUpperCase(c));
                upperCase = false;
            }
            else
            {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 查找指定字符串是否匹配指定字符串列表中的任意一個字符串
     * 
     * @param str 指定字符串
     * @param strs 需要檢查的字符串數組
     * @return 是否匹配
     */
    public static boolean matches(String str, List<String> strs)
    {
        if (isEmpty(str) || isEmpty(strs))
        {
            return false;
        }
        for (String pattern : strs)
        {
            if (isMatch(pattern, str))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 判斷 url 是否與規則配置：
     * ? 表示單個字元；
     * * 表示一層路徑內的任意字符串，不可跨層級；
     * ** 表示任意層路徑;
     * 
     * @param pattern 匹配規則
     * @param url 需要匹配的 url
     * @return
     */
    public static boolean isMatch(String pattern, String url)
    {
        AntPathMatcher matcher = new AntPathMatcher();
        return matcher.match(pattern, url);
    }

    @SuppressWarnings("unchecked")
    public static <T> T cast(Object obj)
    {
        return (T) obj;
    }

    /**
     * 數字左邊補齊 0，使之達到指定長度。注意，如果數字轉換為字符串後，長度大於 size，則只保留 最後 size 個字元。
     * 
     * @param num 數字對象
     * @param size 字符串指定長度
     * @return 返回數字的字符串格式，該字符串為指定長度。
     */
    public static final String padl(final Number num, final int size)
    {
        return padl(num.toString(), size, '0');
    }

    /**
     * 字符串左補齊。如果原始字符串 s 長度大於 size，則只保留最後 size 個字元。
     * 
     * @param s 原始字符串
     * @param size 字符串指定長度
     * @param c 用於補齊的字元
     * @return 返回指定長度的字符串，由原字符串左補齊或截取得到。
     */
    public static final String padl(final String s, final int size, final char c)
    {
        final StringBuilder sb = new StringBuilder(size);
        if (s != null)
        {
            final int len = s.length();
            if (s.length() <= size)
            {
                for (int i = size - len; i > 0; i--)
                {
                    sb.append(c);
                }
                sb.append(s);
            }
            else
            {
                return s.substring(len - size, len);
            }
        }
        else
        {
            for (int i = size; i > 0; i--)
            {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
