package com.smartlib.i18n;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * English → Marathi translation of rendered pages. Exact phrases come from i18n/mr.tsv (English TAB Marathi);
 * phrases containing dynamic values come from i18n/mr-patterns.tsv (regex TAB replacement with $1..$n).
 * Anything not found is left in English, so a missing entry can never break a page.
 */
@Component
public class Translator {
    private final Map<String, String> exact = new HashMap<>();
    private final List<Object[]> patterns = new ArrayList<>();

    private static final Pattern TEXT_NODE = Pattern.compile(">([^<>]+)<");
    private static final Pattern ATTR = Pattern.compile("(\\s(?:placeholder|title|aria-label|alt|data-confirm)=)(\"[^\"]*\")");
    private static final Pattern SCRIPT_STYLE = Pattern.compile("(?is)<(script|style|textarea)\\b[^>]*>.*?</\\1>");

    @PostConstruct
    void load() throws IOException {
        read("i18n/mr.tsv", (a, b) -> exact.put(norm(a), b));
        read("i18n/mr-patterns.tsv", (a, b) -> patterns.add(new Object[]{Pattern.compile("^" + a + "$"), b}));
    }

    private interface Row { void accept(String a, String b); }
    private void read(String path, Row row) throws IOException {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(new ClassPathResource(path).getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                if (line.isBlank() || line.startsWith("#")) continue;
                int t = line.indexOf('\t');
                if (t < 0) continue;
                row.accept(line.substring(0, t).trim(), line.substring(t + 1).trim());
            }
        }
    }

    static String unescape(String s) {
        return s.replace("&#39;", "'").replace("&quot;", "\"").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
    }
    static String escape(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); }
    static String norm(String s) { return s.replaceAll("\\s+", " ").trim(); }

    private static final Pattern DATE = Pattern.compile("\\b(\\d{1,2}) (Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\b");
    private static final Map<String, String> MONTHS = Map.ofEntries(
            Map.entry("Jan", "जाने"), Map.entry("Feb", "फेब्रु"), Map.entry("Mar", "मार्च"), Map.entry("Apr", "एप्रिल"),
            Map.entry("May", "मे"), Map.entry("Jun", "जून"), Map.entry("Jul", "जुलै"), Map.entry("Aug", "ऑगस्ट"),
            Map.entry("Sep", "सप्टें"), Map.entry("Oct", "ऑक्टो"), Map.entry("Nov", "नोव्हें"), Map.entry("Dec", "डिसें"));
    /** Dates such as "12 Nov 2026" become "12 नोव्हें 2026". */
    static String months(String s) {
        Matcher m = DATE.matcher(s); StringBuilder sb = new StringBuilder();
        while (m.find()) m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + " " + MONTHS.get(m.group(2))));
        m.appendTail(sb);
        return sb.toString();
    }

    /** Translates one trimmed phrase, or returns null if unknown. */
    public String phrase(String raw) {
        String key = norm(unescape(raw));
        if (key.isEmpty()) return null;
        String hit = exact.get(key);
        if (hit != null) return hit;
        for (Object[] p : patterns) {
            Matcher m = ((Pattern) p[0]).matcher(key);
            if (m.matches()) {
                String out = (String) p[1];
                for (int i = 1; i <= m.groupCount(); i++) {
                    String g = m.group(i);
                    String tg = g == null ? "" : (exact.containsKey(norm(g)) ? exact.get(norm(g)) : g);
                    out = out.replace("$" + i, Matcher.quoteReplacement(tg));
                }
                return out;
            }
        }
        // "A · B" style joins: translate each part when every part is known
        if (key.contains(" · ")) {
            String[] parts = key.split(" · ");
            StringBuilder sb = new StringBuilder(); boolean any = false;
            for (int i = 0; i < parts.length; i++) {
                String t = phrase(parts[i]);
                if (t != null) any = true;
                sb.append(i > 0 ? " · " : "").append(t != null ? t : parts[i]);
            }
            if (any) return sb.toString();
        }
        return null;
    }

    public String html(String html) {
        // protect script/style/textarea blocks
        List<String> keep = new ArrayList<>();
        Matcher sm = SCRIPT_STYLE.matcher(html);
        StringBuilder protectedHtml = new StringBuilder();
        while (sm.find()) { keep.add(sm.group()); sm.appendReplacement(protectedHtml, "\u0000" + (keep.size() - 1) + "\u0000"); }
        sm.appendTail(protectedHtml);

        String out = replaceAll(TEXT_NODE, protectedHtml.toString(), m -> {
            String raw = m.group(1), t = raw.strip();
            if (t.isEmpty()) return m.group();
            String tr = phrase(t);
            if (tr == null) { String d = months(t); if (d.equals(t)) return m.group(); tr = d; }
            else tr = months(tr);
            int lead = raw.indexOf(t.charAt(0)), end = lead + t.length();
            return ">" + raw.substring(0, lead) + escape(tr) + raw.substring(end) + "<";
        });
        out = replaceAll(ATTR, out, m -> {
            String v = m.group(2); String inner = v.substring(1, v.length() - 1);
            String tr = phrase(inner);
            return tr == null ? m.group() : m.group(1) + "\"" + escape(tr).replace("\"", "&quot;") + "\"";
        });
        // restore protected blocks
        for (int i = 0; i < keep.size(); i++) out = out.replace("\u0000" + i + "\u0000", keep.get(i));
        return out.replace("<html lang=\"en\"", "<html lang=\"mr\"");
    }

    private interface Fn { String apply(Matcher m); }
    private static String replaceAll(Pattern p, String s, Fn f) {
        Matcher m = p.matcher(s); StringBuilder sb = new StringBuilder();
        while (m.find()) m.appendReplacement(sb, Matcher.quoteReplacement(f.apply(m)));
        m.appendTail(sb);
        return sb.toString();
    }
}
