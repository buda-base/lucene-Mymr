package io.bdrc.lucene.mymr;

import java.io.Reader;

import org.apache.lucene.analysis.charfilter.MappingCharFilter;
import org.apache.lucene.analysis.charfilter.NormalizeCharMap;

public class MyanmarCharFilter extends MappingCharFilter {
    
    public MyanmarCharFilter(final Reader in) {
        super(getNormalizeCharMapCached(false), in);
    }
    
    public MyanmarCharFilter(final Reader in, final boolean lenient) {
        super(getNormalizeCharMapCached(lenient), in);
    }
    
    private static final NormalizeCharMap[] cache = new NormalizeCharMap[] {null, null};
    private static NormalizeCharMap getNormalizeCharMapCached(final boolean lenient) {
        final int idx = lenient ? 1 : 0;
        if (cache[idx] == null)
            cache[idx] = getNormalizeCharMap(lenient);
        return cache[idx];
    }
    
    public final static NormalizeCharMap getNormalizeCharMap(final boolean lenient) {
        NormalizeCharMap.Builder builder = new NormalizeCharMap.Builder();
        // the only proper Unicode equivalence is 1026 = 1025 102E
        builder.add("\u1025\u102E", "\u1026"); // ဦ -> ဦ
        if (lenient) {
            builder.add("\u104B", "\u104A\u104A"); // ။ -> ၊၊
            builder.add("\u101E\u103C\u1031\u102C\u103A", "\u102A"); // သြော် -> ဪ
            builder.add("\u1029\u1031\u102C\u103A", "\u102A"); // ဩော် -> ဪ
            builder.add("\u101E\u103C", "\u1029"); // သြ -> ဩ
            builder.add("\u1005\u103B", "\u1008"); // စျ -> ဈ
            builder.add("\u1005\u103B", "\u1008"); // စျ -> ဈ
            // digits
            builder.add("၀", "0");
            builder.add("၁", "1");
            builder.add("၂", "2");
            builder.add("၃", "3");
            builder.add("၄", "4");
            builder.add("၅", "5");
            builder.add("၆", "6");
            builder.add("၇", "7");
            builder.add("၈", "8");
            builder.add("၉", "9");
            // shan digits, just in case
            builder.add("႐", "0");
            builder.add("႑", "1");
            builder.add("႒", "2");
            builder.add("႓", "3");
            builder.add("႔", "4");
            builder.add("႕", "5");
            builder.add("႖", "6");
            builder.add("႗", "7");
            builder.add("႘", "8");
            builder.add("႙", "9");
            // TODO: there are two documented common mistypings that I'm not sure how to handle:
            // ဝ (wa) / ၀ (0)
            // ရ (ra) / ၇ (7)
        }
        return builder.build();
    }

}
