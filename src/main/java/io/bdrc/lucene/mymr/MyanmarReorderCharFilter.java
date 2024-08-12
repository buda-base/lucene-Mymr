package io.bdrc.lucene.mymr;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Comparator;
import java.util.Collections;

import org.apache.lucene.analysis.charfilter.BaseCharFilter;

public class MyanmarReorderCharFilter extends BaseCharFilter {
    
    // This code comes from http://unicode.org/notes/tn11/ :
    // Hosken, M. Representing Myanmar in Unicode, Details and Examples Version 4
    // Terms of Use of Unicode probably apply: http://www.unicode.org/copyright.html

    // The code has been ported to Java by Elie Roux in 2024

    private static final int REORDER = 12;
    private static final int EXTENDING = 16;
    private static final int SEQFLAG = 32;

    private static final int[] orders0 = new int[] {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 8, 9,
        9, 7, 8, 8, 8, 8, 8, 11, 12, 1, 2, 3, 4, 5, 6, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 9, 9, 0, 0, 0, 0, 3, 3,
        6, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 8, 8, 8, 8, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 5, 0, 7, 8, 10, 12, 12, 12, 12, 12, 12, 12, 0, 12,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 12, 12, 12, 8, 0, 0
    };

    private static final int[] orders1 = new int[] {
        0, 0, 0, 0, 0, 8, 13, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        13, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    private static final int[] orders2 = new int[] {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 12, 12, 12, 0, 0
    };

    private static final int[] flags = new int[] {
        0, 0, 0, 0, 32, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 32, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2,
        2, 0, 8, 0, 0, 0, 8, 1, 0, 16, 4, 0, 0, 0, 1, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 2, 2, 32, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    private static final Map<Integer, int[][]> seqs = new HashMap<>();
    
    static {
        seqs.put(0x1004, new int[][] { {2, 0xFF, 0x103A, 0x1039} });
        seqs.put(0x101B, new int[][] { {2, 0xFF, 0x103A, 0x1039} });
        seqs.put(0x105A, new int[][] { {2, 0xFF, 0x103A, 0x1039} });
    }

    public MyanmarReorderCharFilter(Reader input) {
        super(input);
    }

    private int order(int num) {
        if (0x1000 <= num && num < 0x10A0) {
            return orders0[num - 0x1000];
        } else if (0xAA60 <= num && num < 0xAA80) {
            return orders1[num - 0xAA60];
        } else if (0xA9E0 <= num && num < 0xAA00) {
            return orders2[num - 0xA9E0];
        } else {
            return 0;
        }
    }

    private int flag(int num) {
        if (0x1000 <= num && num < 0x10A0) {
            return flags[num - 0x1000];
        }
        return 0;
    }

    private int[] getVals(final String text, final int index) {
        int num = text.codePointAt(index);
        int order = order(num);
        int flag = flag(num);
        int length = 1;
        
        if ((flag & EXTENDING) != 0) {
            length = 2;
        } else if ((flag & SEQFLAG) != 0) {
            int[][] sequences = seqs.get(num);
            if (sequences != null) {
                for (int[] seq : sequences) {
                    if (seq[0] + index + 1 > text.length())
                        continue;
                    boolean hit = true;
                    for (int i = 0; i < seq[0]; i++) {
                        if (text.codePointAt(index + 1 + i) != seq[2 + i]) {
                            hit = false;
                            break;
                        }
                        if (hit) {
                            length = seq[0] + 1;
                            order = seq[1];
                        }
                    }
                }
            }
        }

        return new int[] {order, flag, length};
    }

    private String canonSubsort(String text, int[] orders, int[] flags, int start, int end) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < (end - start); i++) {
            indices.add(i);
        }

        indices.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer x, Integer y) {
                if (orders[x] == orders[y]) {
                    return Integer.compare(x, y);
                } else {
                    return Integer.compare(orders[x], orders[y]);
                }
            }
        });

        int finalIdx = indices.size() - 1;
        int i = 0;
        while (i < finalIdx) {
            int f = (flags[indices.get(i)] & REORDER) >> 2;
            if (f != 0) {
                int j = i + 1;
                while (j <= finalIdx && (f & flags[indices.get(j)]) != 0) {
                    Collections.swap(indices, j - 1, j);
                    j++;
                }
                if (j > i + 1 && i > 0) {
                    i -= 2;
                }
            }
            i++;
        }

        StringBuilder substr = new StringBuilder();
        for (int idx : indices) {
            substr.append(text.charAt(start + idx));
        }

        return text.substring(0, start) + substr.toString() + text.substring(end);
    }

    @Override
    protected int correct(int currentOff) {
        return currentOff; // Implement any necessary offset correction
    }


    @Override
    public int read() throws IOException {
        char[] buffer = new char[1];
        int numRead = read(buffer, 0, 1);
        if (numRead == -1) {
            return -1; // End of input
        }
        return buffer[0];
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        char[] buffer = new char[len];
        int numCharsRead = input.read(buffer, 0, len);
        if (numCharsRead == -1) {
            return -1; // End of input
        }

        String text = new String(buffer, 0, numCharsRead);
        StringBuilder result = new StringBuilder();

        int index = 0;
        while (index < text.length()) {
            int[] vals = getVals(text, index);

            if (vals[0] != 0) {
                int start = index;
                List<Integer> flags = new ArrayList<>();
                List<Integer> orders = new ArrayList<>();

                flags.add(vals[1]);
                orders.add(vals[0]);
                index += vals[2];

                while (index < text.length()) {
                    vals = getVals(text, index);
                    if (vals[0] == 0) {
                        break;
                    }
                    flags.add(vals[1]);
                    orders.add(vals[0]);
                    index += vals[2];
                }

                String sortedText = canonSubsort(text, 
                    orders.stream().mapToInt(i -> i).toArray(),
                    flags.stream().mapToInt(i -> i).toArray(), 
                    start, index);
                result.append(sortedText);
            } else {
                result.append(text.charAt(index));
                index++;
            }
        }

        // Copy the result to cbuf
        System.arraycopy(result.toString().toCharArray(), 0, cbuf, off, result.length());

        return result.length();
    }
}