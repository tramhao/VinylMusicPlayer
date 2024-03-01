package com.poupa.vinylmusicplayer.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.Charset;
import java.text.Collator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author SC (soncaokim)
 */

public class StringUtil {
    public enum ClosestMatch {
        FIRST,
        SECOND,
        EQUAL
    }

    private static final Collator collator = Collator.getInstance();
    private static final Pattern accentStripRegex = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    public static int compareIgnoreAccent(@Nullable final String s1, @Nullable final String s2) {
        // Null-proof comparison
        if (s1 == null) {
            return s2 == null ? 0 : -1;
        } else if (s2 == null) {
            return 1;
        }

        return collator.compare(s1, s2);
    }

    /**
     * Returns which string contains a closer match for compareTo.
     * <p>
     * Shortest string > string with compareTo closer to front
     * or equal if neither of those two cases satisfied
     *
     * @param compareTo string being searched for
     * @param first     string that contains compareTo
     * @param second    string that contains compareTo
     * @return ClosestMatch
     */
    @NonNull
    public static ClosestMatch closestOfMatches(
            @NonNull final String compareTo,
            @NonNull final String first,
            @NonNull final String second) {
        // shortest name is a closer match
        if (first.length() < second.length()) {
            return ClosestMatch.FIRST;
        } else if (second.length() < first.length()) {
            return ClosestMatch.SECOND;
        }

        // lengths equal :(

        // name with search term closer to front is a closer match
        final int indexFirst = first.indexOf(compareTo);
        final int indexSecond = second.indexOf(compareTo);
        if (indexFirst < indexSecond) {
            return ClosestMatch.FIRST;
        } else if (indexSecond < indexFirst) {
            return ClosestMatch.SECOND;
        }

        // indexes equal

        return ClosestMatch.EQUAL;
    }

    public static String unicodeNormalize(@NonNull final String text) {
        try {
            return Normalizer.normalize(text, Normalizer.Form.NFD);
        } catch (Exception ignored) {
            return text;
        }
    }

    @NonNull
    public static String stripAccent(@NonNull final String text) {
        return accentStripRegex.matcher(text).replaceAll("");
    }

    public static List<String> wideCharacterSplit(String src, int bytes) {

        if (src == null) {
            return null;
        }
        List<String> splitList = new ArrayList<String>();
        src = src.trim();

        // If the line is less than bytes, there is no need to split
        if (src.getBytes(Charset.forName("GBK")).length <= bytes) {
            splitList.add(src);
            return splitList;
        }

        // Firstly try to split the lyric by space
        String[] separated = src.split("\\s+");
        if (separated.length < 4) {
            boolean canReturn = true;
            for (String s : separated) {
                if (s.getBytes(Charset.forName("GBK")).length > bytes) {
                    canReturn = false;
                }
            }
            if (canReturn) {
                splitList.addAll(Arrays.asList(separated));
                return splitList;
            }
        }

        // Secondly try to split the lyric by fixed width
        int startIndex = 0;    // start of string
        int endIndex;
        while (startIndex < src.length()) {
            // At the end, need to compare with src.length(), in order not to cause index out of range.
            endIndex = Math.min((startIndex + bytes), src.length());
            String subString = src.substring(startIndex, endIndex);
            // If string length is bigger than bytes, means wide character are in the string
            // In GBK encoding, chinese character took 2 bytes, and in UTF-8, it took 3 bytes.
            while (subString.getBytes(Charset.forName("GBK")).length > bytes) {
                --endIndex;
                subString = src.substring(startIndex, endIndex);
            }
            splitList.add(subString);
            startIndex = endIndex;
        }
        return splitList;

    }


    @NonNull
    public static String join(@NonNull String... s) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < s.length; i++) {
            stringBuilder = stringBuilder.append(s[i]);
        }
        return stringBuilder.toString();
    }
}
