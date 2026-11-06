package com.cloudhis.common;

import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

public final class Values {
    private Values() {}
    public static String id() { return UUID.randomUUID().toString(); }
    public static String number(String prefix) { return prefix + LocalDate.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.BASIC_ISO_DATE) + UUID.randomUUID().toString().substring(0, 12).replace("-", "").toUpperCase(); }
    public static String str(Map<String, Object> row, String key) { return row.get(key) == null ? "" : row.get(key).toString(); }
    public static long num(Map<String, Object> row, String key) { return ((Number) row.getOrDefault(key, 0L)).longValue(); }
    public static boolean active(Map<String, Object> row) { Object v = row.get("active"); return Boolean.TRUE.equals(v) || (v instanceof Number n && n.intValue() == 1); }
    public static Map<String,Object> map(Object... pairs) { Map<String,Object> m = new LinkedHashMap<>(); for (int i=0; i<pairs.length; i+=2) m.put(pairs[i].toString(),pairs[i+1]); return m; }
    public static Map<String,Object> must(Map<String,Object> row, String message) { if (row == null) throw new BusinessException(404, message); return row; }
}
