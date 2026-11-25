package com.resumematch.log;

import java.util.ArrayList;
import java.util.List;

/**
 * Escreve e le as celulas dos CSVs locais.
 *
 * Um campo que comeca com "=", "+", "-" ou "@" recebe um apostrofo para nao
 * virar formula quando o arquivo e aberto numa planilha.
 */
public final class CsvCells {

    private static final String FORMULA_START = "=+-@\t";

    private CsvCells() {
    }

    public static String cell(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String clean = value.replace("\r\n", " ")
                .replace('\n', ' ')
                .replace('\r', ' ')
                .strip();
        if (!clean.isEmpty() && FORMULA_START.indexOf(clean.charAt(0)) >= 0) {
            clean = "'" + clean;
        }
        if (clean.contains(",") || clean.contains("\"")) {
            return "\"" + clean.replace("\"", "\"\"") + "\"";
        }
        return clean;
    }

    /** Le uma linha escrita com {@link #cell(String)}, desfazendo o apostrofo. */
    public static List<String> parse(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    current.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                cells.add(unescape(current.toString()));
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        cells.add(unescape(current.toString()));
        return cells;
    }

    private static String unescape(String value) {
        if (value.length() > 1 && value.charAt(0) == '\''
                && FORMULA_START.indexOf(value.charAt(1)) >= 0) {
            return value.substring(1);
        }
        return value;
    }
}
