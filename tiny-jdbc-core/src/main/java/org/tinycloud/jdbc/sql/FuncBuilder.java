package org.tinycloud.jdbc.sql;

import org.tinycloud.jdbc.criteria.TypeFunction;
import org.tinycloud.jdbc.util.LambdaUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 *     SQL 函数构建器。
 * </p>
 * <p>
 *     返回不可变的 {@link FuncExpr}，支持嵌套组合与链式别名 .as(alias)。参数约定：
 *     <ul>
 *         <li>{@link String} → 裸 SQL（列名 / 表达式），原样拼接，不加引号；</li>
 *         <li>{@link TypeFunction} → 解析为列名；</li>
 *         <li>{@link FuncExpr} → 已组合的表达式；</li>
 *         <li>{@code lit(Object)} → 值字面量，参数化（? + 绑定参数）。</li>
 *     </ul>
 * </p>
 *
 * @author liuxingyu01
 * @since 2025-08-24
 */
public final class FuncBuilder {

    private FuncBuilder() {
    }

    // ------ 列 / 字面量 ------

    public static FuncExpr col(String field) {
        return FuncExpr.raw(field);
    }

    public static <T, R> FuncExpr col(TypeFunction<T, R> function) {
        return FuncExpr.raw(LambdaUtils.getLambdaColumnName(function));
    }

    public static FuncExpr lit(Object value) {
        return FuncExpr.value(value);
    }

    // ------ 聚合 ------

    public static FuncExpr count() {
        return FuncExpr.call("COUNT", FuncExpr.raw("*"));
    }

    public static FuncExpr count(String field) {
        return FuncExpr.call("COUNT", FuncExpr.raw(field));
    }

    public static <T, R> FuncExpr count(TypeFunction<T, R> function) {
        return FuncExpr.call("COUNT", FuncExpr.raw(LambdaUtils.getLambdaColumnName(function)));
    }

    public static FuncExpr count(FuncExpr field) {
        return FuncExpr.call("COUNT", field);
    }

    public static FuncExpr countDistinct(String field) {
        return FuncExpr.call("COUNT", FuncExpr.raw("DISTINCT " + field));
    }

    public static <T, R> FuncExpr countDistinct(TypeFunction<T, R> function) {
        return FuncExpr.call("COUNT", FuncExpr.raw("DISTINCT " + LambdaUtils.getLambdaColumnName(function)));
    }

    public static FuncExpr sum(String field) {
        return FuncExpr.call("SUM", FuncExpr.raw(field));
    }

    public static <T, R> FuncExpr sum(TypeFunction<T, R> function) {
        return FuncExpr.call("SUM", FuncExpr.raw(LambdaUtils.getLambdaColumnName(function)));
    }

    public static FuncExpr sum(FuncExpr field) {
        return FuncExpr.call("SUM", field);
    }

    public static FuncExpr avg(String field) {
        return FuncExpr.call("AVG", FuncExpr.raw(field));
    }

    public static <T, R> FuncExpr avg(TypeFunction<T, R> function) {
        return FuncExpr.call("AVG", FuncExpr.raw(LambdaUtils.getLambdaColumnName(function)));
    }

    public static FuncExpr avg(FuncExpr field) {
        return FuncExpr.call("AVG", field);
    }

    public static FuncExpr max(String field) {
        return FuncExpr.call("MAX", FuncExpr.raw(field));
    }

    public static <T, R> FuncExpr max(TypeFunction<T, R> function) {
        return FuncExpr.call("MAX", FuncExpr.raw(LambdaUtils.getLambdaColumnName(function)));
    }

    public static FuncExpr max(FuncExpr field) {
        return FuncExpr.call("MAX", field);
    }

    public static FuncExpr min(String field) {
        return FuncExpr.call("MIN", FuncExpr.raw(field));
    }

    public static <T, R> FuncExpr min(TypeFunction<T, R> function) {
        return FuncExpr.call("MIN", FuncExpr.raw(LambdaUtils.getLambdaColumnName(function)));
    }

    public static FuncExpr min(FuncExpr field) {
        return FuncExpr.call("MIN", field);
    }

    public static FuncExpr groupConcat(String field) {
        return FuncExpr.call("GROUP_CONCAT", FuncExpr.raw(field));
    }

    public static <T, R> FuncExpr groupConcat(TypeFunction<T, R> function) {
        return FuncExpr.call("GROUP_CONCAT", FuncExpr.raw(LambdaUtils.getLambdaColumnName(function)));
    }

    public static FuncExpr groupConcat(String field, String separator) {
        return FuncExpr.call("GROUP_CONCAT", FuncExpr.raw(field), FuncExpr.raw("SEPARATOR " + separator));
    }

    public static FuncExpr groupConcatDistinct(String field) {
        return FuncExpr.call("GROUP_CONCAT", FuncExpr.raw("DISTINCT " + field));
    }

    // ------ 字符串 ------

    public static FuncExpr concat(String... fields) {
        return FuncExpr.call("CONCAT", toRawArray(fields));
    }

    public static <T, R> FuncExpr concat(TypeFunction<T, R>... functions) {
        return FuncExpr.call("CONCAT", toRawArray(functions));
    }

    public static FuncExpr concat(FuncExpr... fields) {
        return FuncExpr.call("CONCAT", fields);
    }

    public static FuncExpr concat_ws(String separator, String... fields) {
        return FuncExpr.call("CONCAT_WS", merge(new FuncExpr[]{FuncExpr.value(separator)}, toRawArray(fields)));
    }

    public static FuncExpr concat_ws(String separator, FuncExpr... fields) {
        return FuncExpr.call("CONCAT_WS", merge(new FuncExpr[]{FuncExpr.value(separator)}, fields));
    }

    public static FuncExpr upper(String field) {
        return FuncExpr.call("UPPER", FuncExpr.raw(field));
    }

    public static <T, R> FuncExpr upper(TypeFunction<T, R> function) {
        return FuncExpr.call("UPPER", FuncExpr.raw(LambdaUtils.getLambdaColumnName(function)));
    }

    public static FuncExpr lower(String field) {
        return FuncExpr.call("LOWER", FuncExpr.raw(field));
    }

    public static <T, R> FuncExpr lower(TypeFunction<T, R> function) {
        return FuncExpr.call("LOWER", FuncExpr.raw(LambdaUtils.getLambdaColumnName(function)));
    }

    public static FuncExpr trim(String field) {
        return FuncExpr.call("TRIM", FuncExpr.raw(field));
    }

    public static <T, R> FuncExpr trim(TypeFunction<T, R> function) {
        return FuncExpr.call("TRIM", FuncExpr.raw(LambdaUtils.getLambdaColumnName(function)));
    }

    public static FuncExpr ltrim(String field) {
        return FuncExpr.call("LTRIM", FuncExpr.raw(field));
    }

    public static FuncExpr rtrim(String field) {
        return FuncExpr.call("RTRIM", FuncExpr.raw(field));
    }

    public static FuncExpr length(String field) {
        return FuncExpr.call("LENGTH", FuncExpr.raw(field));
    }

    public static <T, R> FuncExpr length(TypeFunction<T, R> function) {
        return FuncExpr.call("LENGTH", FuncExpr.raw(LambdaUtils.getLambdaColumnName(function)));
    }

    public static FuncExpr charLength(String field) {
        return FuncExpr.call("CHAR_LENGTH", FuncExpr.raw(field));
    }

    public static FuncExpr substring(String field, int start) {
        return FuncExpr.call("SUBSTRING", FuncExpr.raw(field), FuncExpr.value(start));
    }

    public static FuncExpr substring(String field, int start, int length) {
        return FuncExpr.call("SUBSTRING", FuncExpr.raw(field), FuncExpr.value(start), FuncExpr.value(length));
    }

    public static FuncExpr left(String field, int index) {
        return FuncExpr.call("LEFT", FuncExpr.raw(field), FuncExpr.value(index));
    }

    public static FuncExpr right(String field, int index) {
        return FuncExpr.call("RIGHT", FuncExpr.raw(field), FuncExpr.value(index));
    }

    public static FuncExpr locate(String field, String find) {
        return FuncExpr.call("LOCATE", FuncExpr.raw(field), FuncExpr.raw(find));
    }

    public static FuncExpr instr(String field, String find) {
        return FuncExpr.call("INSTR", FuncExpr.raw(field), FuncExpr.raw(find));
    }

    public static FuncExpr replace(String field, String from, String to) {
        return FuncExpr.call("REPLACE", FuncExpr.raw(field), FuncExpr.value(from), FuncExpr.value(to));
    }

    // ------ 数值 ------

    public static FuncExpr abs(String field) {
        return FuncExpr.call("ABS", FuncExpr.raw(field));
    }

    public static FuncExpr ceil(String field) {
        return FuncExpr.call("CEIL", FuncExpr.raw(field));
    }

    public static FuncExpr floor(String field) {
        return FuncExpr.call("FLOOR", FuncExpr.raw(field));
    }

    public static FuncExpr round(String field, int digits) {
        return FuncExpr.call("ROUND", FuncExpr.raw(field), FuncExpr.value(digits));
    }

    public static FuncExpr mod(String field, int divisor) {
        return FuncExpr.call("MOD", FuncExpr.raw(field), FuncExpr.value(divisor));
    }

    // ------ 日期 ------

    public static FuncExpr now() {
        return FuncExpr.raw("NOW()");
    }

    public static FuncExpr curdate() {
        return FuncExpr.raw("CURDATE()");
    }

    public static FuncExpr curtime() {
        return FuncExpr.raw("CURTIME()");
    }

    public static FuncExpr dateFormat(String field, String format) {
        return FuncExpr.call("DATE_FORMAT", FuncExpr.raw(field), FuncExpr.value(format));
    }

    public static FuncExpr year(String field) {
        return FuncExpr.call("YEAR", FuncExpr.raw(field));
    }

    public static FuncExpr month(String field) {
        return FuncExpr.call("MONTH", FuncExpr.raw(field));
    }

    public static FuncExpr day(String field) {
        return FuncExpr.call("DAY", FuncExpr.raw(field));
    }

    public static FuncExpr dateAdd(String field, int days) {
        return FuncExpr.call("DATE_ADD", FuncExpr.raw(field), FuncExpr.raw("INTERVAL " + days + " DAY"));
    }

    // ------ 条件 ------

    public static FuncExpr ifNull(String field, Object value) {
        return FuncExpr.call("IFNULL", FuncExpr.raw(field), FuncExpr.value(value));
    }

    public static FuncExpr ifNull(FuncExpr field, Object value) {
        return FuncExpr.call("IFNULL", field, FuncExpr.value(value));
    }

    public static FuncExpr coalesce(String... fields) {
        return FuncExpr.call("COALESCE", toRawArray(fields));
    }

    public static FuncExpr coalesce(FuncExpr... fields) {
        return FuncExpr.call("COALESCE", fields);
    }

    public static FuncExpr nullIf(String field, Object value) {
        return FuncExpr.call("NULLIF", FuncExpr.raw(field), FuncExpr.value(value));
    }

    public static FuncExpr distinct(String field) {
        return FuncExpr.raw("DISTINCT " + field);
    }

    public static <T, R> FuncExpr distinct(TypeFunction<T, R> function) {
        return FuncExpr.raw("DISTINCT " + LambdaUtils.getLambdaColumnName(function));
    }

    // ------ CASE WHEN ------

    /**
     * 创建 CASE WHEN 表达式构建器。条件与结果统一按 {@link FuncBuilder} 参数约定归一化：
     * FuncExpr 直接使用；TypeFunction 解析为列名；String 视为裸 SQL；其它字面量参数化（? + 绑定）。
     */
    public static CaseWhenBuilder caseWhen() {
        return new CaseWhenBuilder();
    }

    public static class CaseWhenBuilder {
        private final List<Object[]> whenClauses = new ArrayList<>();
        private FuncExpr elseExpr;

        /**
         * 添加 WHEN 条件与 THEN 结果。
         */
        public CaseWhenBuilder when(Object condition, Object result) {
            whenClauses.add(new Object[]{toExpr(condition), toExpr(result)});
            return this;
        }

        /**
         * 添加 ELSE 结果。
         */
        public CaseWhenBuilder otherwise(Object result) {
            this.elseExpr = toExpr(result);
            return this;
        }

        /**
         * 构建 CASE WHEN 表达式。
         */
        public FuncExpr build() {
            if (whenClauses.isEmpty()) {
                throw new org.tinycloud.jdbc.exception.TinyJdbcException("At least one WHEN clause is required for CASE WHEN");
            }
            StringBuilder sb = new StringBuilder("CASE");
            List<Object> merged = new ArrayList<>();
            for (Object[] clause : whenClauses) {
                FuncExpr cond = (FuncExpr) clause[0];
                FuncExpr res = (FuncExpr) clause[1];
                sb.append(" WHEN ").append(cond.getExpression()).append(" THEN ").append(res.getExpression());
                merged.addAll(cond.getParameters());
                merged.addAll(res.getParameters());
            }
            if (elseExpr != null) {
                sb.append(" ELSE ").append(elseExpr.getExpression());
                merged.addAll(elseExpr.getParameters());
            }
            sb.append(" END");
            return FuncExpr.ofRaw(sb.toString(), merged);
        }
    }

    /**
     * CASE WHEN 快捷版：{@code CASE WHEN cond THEN then ELSE otherwise END}。
     */
    public static FuncExpr caseWhen(Object condition, Object then, Object otherwise) {
        return caseWhen().when(condition, then).otherwise(otherwise).build();
    }

    // ------ 通用函数调用 ------

    /**
     * 通用函数调用：name(arg1, arg2, ...)，支持任意嵌套（参数可为 col / lit / 其它函数结果）。
     */
    public static FuncExpr func(String name, FuncExpr... args) {
        return FuncExpr.call(name, args);
    }

    // ------ 缺失函数补齐：字符串 / 数值 / 日期 / 条件 / JSON / 其它 ------

    public static FuncExpr findInSet(String find, String str) {
        return func("FIND_IN_SET", FuncExpr.raw(find), FuncExpr.raw(str));
    }

    public static FuncExpr position(String sub, String str) {
        return FuncExpr.raw("POSITION(" + sub + " IN " + str + ")");
    }

    public static FuncExpr elt(int index, String... fields) {
        return func("ELT", merge(new FuncExpr[]{FuncExpr.raw(String.valueOf(index))}, toRawArray(fields)));
    }

    public static FuncExpr insert(String str, int pos, int len, String replacement) {
        return func("INSERT", FuncExpr.raw(str), FuncExpr.raw(String.valueOf(pos)),
                FuncExpr.raw(String.valueOf(len)), FuncExpr.raw(replacement));
    }

    public static FuncExpr truncate(String field, int decimals) {
        return func("TRUNCATE", FuncExpr.raw(field), FuncExpr.raw(String.valueOf(decimals)));
    }

    public static FuncExpr rand() {
        return FuncExpr.call("RAND");
    }

    public static FuncExpr dateSub(String field, int days) {
        return FuncExpr.call("DATE_SUB", FuncExpr.raw(field), FuncExpr.raw("INTERVAL " + days + " DAY"));
    }

    public static FuncExpr format(String field, int decimals) {
        return func("FORMAT", FuncExpr.raw(field), FuncExpr.raw(String.valueOf(decimals)));
    }

    public static FuncExpr strToDate(String field, String format) {
        return func("STR_TO_DATE", FuncExpr.raw(field), FuncExpr.value(format));
    }

    public static FuncExpr monthname(String field) {
        return FuncExpr.call("MONTHNAME", FuncExpr.raw(field));
    }

    public static FuncExpr week(String field) {
        return FuncExpr.call("WEEK", FuncExpr.raw(field));
    }

    public static FuncExpr hour(String field) {
        return FuncExpr.call("HOUR", FuncExpr.raw(field));
    }

    public static FuncExpr minute(String field) {
        return FuncExpr.call("MINUTE", FuncExpr.raw(field));
    }

    public static FuncExpr second(String field) {
        return FuncExpr.call("SECOND", FuncExpr.raw(field));
    }

    public static FuncExpr weekday(String field) {
        return FuncExpr.call("WEEKDAY", FuncExpr.raw(field));
    }

    public static FuncExpr dayname(String field) {
        return FuncExpr.call("DAYNAME", FuncExpr.raw(field));
    }

    public static FuncExpr date(String field) {
        return FuncExpr.call("DATE", FuncExpr.raw(field));
    }

    public static FuncExpr _if(Object condition, Object then, Object otherwise) {
        return func("IF", toExpr(condition), toExpr(then), toExpr(otherwise));
    }

    public static FuncExpr jsonExtract(String field, String path) {
        return func("JSON_EXTRACT", FuncExpr.raw(field), FuncExpr.value(path));
    }

    public static FuncExpr jsonUnquote(String field) {
        return FuncExpr.call("JSON_UNQUOTE", FuncExpr.raw(field));
    }

    public static FuncExpr jsonContains(String field, String value) {
        return func("JSON_CONTAINS", FuncExpr.raw(field), FuncExpr.value(value));
    }

    public static FuncExpr jsonSet(String field, String path, Object value) {
        return func("JSON_SET", FuncExpr.raw(field), FuncExpr.value(path), FuncExpr.value(value));
    }

    public static FuncExpr jsonRemove(String field, String... paths) {
        FuncExpr[] arr = new FuncExpr[paths.length];
        for (int i = 0; i < paths.length; i++) {
            arr[i] = FuncExpr.value(paths[i]);
        }
        return func("JSON_REMOVE", merge(new FuncExpr[]{FuncExpr.raw(field)}, arr));
    }

    public static FuncExpr jsonObject(FuncExpr... values) {
        return FuncExpr.call("JSON_OBJECT", values);
    }

    public static FuncExpr jsonArray(FuncExpr... values) {
        return FuncExpr.call("JSON_ARRAY", values);
    }

    public static FuncExpr unixTimeStamp() {
        return FuncExpr.call("UNIX_TIMESTAMP");
    }

    public static FuncExpr fromUnixTime(String field, String format) {
        return func("FROM_UNIXTIME", FuncExpr.raw(field), FuncExpr.value(format));
    }

    // ------ FuncExpr 重载（实现嵌套，如 year(now())） ------

    public static FuncExpr year(FuncExpr expr) { return FuncExpr.call("YEAR", expr); }
    public static FuncExpr month(FuncExpr expr) { return FuncExpr.call("MONTH", expr); }
    public static FuncExpr day(FuncExpr expr) { return FuncExpr.call("DAY", expr); }
    public static FuncExpr hour(FuncExpr expr) { return FuncExpr.call("HOUR", expr); }
    public static FuncExpr minute(FuncExpr expr) { return FuncExpr.call("MINUTE", expr); }
    public static FuncExpr second(FuncExpr expr) { return FuncExpr.call("SECOND", expr); }
    public static FuncExpr week(FuncExpr expr) { return FuncExpr.call("WEEK", expr); }
    public static FuncExpr upper(FuncExpr expr) { return FuncExpr.call("UPPER", expr); }
    public static FuncExpr lower(FuncExpr expr) { return FuncExpr.call("LOWER", expr); }
    public static FuncExpr trim(FuncExpr expr) { return FuncExpr.call("TRIM", expr); }
    public static FuncExpr length(FuncExpr expr) { return FuncExpr.call("LENGTH", expr); }
    public static FuncExpr abs(FuncExpr expr) { return FuncExpr.call("ABS", expr); }
    public static FuncExpr ceil(FuncExpr expr) { return FuncExpr.call("CEIL", expr); }
    public static FuncExpr floor(FuncExpr expr) { return FuncExpr.call("FLOOR", expr); }
    public static FuncExpr dateFormat(FuncExpr expr, String format) { return func("DATE_FORMAT", expr, FuncExpr.value(format)); }
    public static FuncExpr substring(FuncExpr expr, int start) { return func("SUBSTRING", expr, FuncExpr.raw(String.valueOf(start))); }

    // ------ TypeFunction（Lambda）重载：列名自动取自实体 @Column，编译期防错 ------

    // 字符串
    public static <T, R> FuncExpr ltrim(TypeFunction<T, R> field) {
        return ltrim(LambdaUtils.getLambdaColumnName(field));
    }

    public static <T, R> FuncExpr rtrim(TypeFunction<T, R> field) {
        return rtrim(LambdaUtils.getLambdaColumnName(field));
    }

    public static <T, R> FuncExpr charLength(TypeFunction<T, R> field) {
        return charLength(LambdaUtils.getLambdaColumnName(field));
    }

    public static <T, R> FuncExpr substring(TypeFunction<T, R> field, int start) {
        return substring(LambdaUtils.getLambdaColumnName(field), start);
    }

    public static <T, R> FuncExpr substring(TypeFunction<T, R> field, int start, int length) {
        return substring(LambdaUtils.getLambdaColumnName(field), start, length);
    }

    public static <T, R> FuncExpr left(TypeFunction<T, R> field, int index) {
        return left(LambdaUtils.getLambdaColumnName(field), index);
    }

    public static <T, R> FuncExpr right(TypeFunction<T, R> field, int index) {
        return right(LambdaUtils.getLambdaColumnName(field), index);
    }

    public static <T, R> FuncExpr locate(TypeFunction<T, R> field, String find) {
        return locate(LambdaUtils.getLambdaColumnName(field), find);
    }

    public static <T, R> FuncExpr instr(TypeFunction<T, R> field, String find) {
        return instr(LambdaUtils.getLambdaColumnName(field), find);
    }

    public static <T, R> FuncExpr replace(TypeFunction<T, R> field, String from, String to) {
        return replace(LambdaUtils.getLambdaColumnName(field), from, to);
    }

    // 数值
    public static <T, R> FuncExpr abs(TypeFunction<T, R> field) {
        return abs(LambdaUtils.getLambdaColumnName(field));
    }

    public static <T, R> FuncExpr ceil(TypeFunction<T, R> field) {
        return ceil(LambdaUtils.getLambdaColumnName(field));
    }

    public static <T, R> FuncExpr floor(TypeFunction<T, R> field) {
        return floor(LambdaUtils.getLambdaColumnName(field));
    }

    public static <T, R> FuncExpr round(TypeFunction<T, R> field, int digits) {
        return round(LambdaUtils.getLambdaColumnName(field), digits);
    }

    public static <T, R> FuncExpr mod(TypeFunction<T, R> field, int divisor) {
        return mod(LambdaUtils.getLambdaColumnName(field), divisor);
    }

    // 日期
    public static <T, R> FuncExpr dateFormat(TypeFunction<T, R> field, String format) {
        return dateFormat(LambdaUtils.getLambdaColumnName(field), format);
    }

    public static <T, R> FuncExpr year(TypeFunction<T, R> field) {
        return year(LambdaUtils.getLambdaColumnName(field));
    }

    public static <T, R> FuncExpr month(TypeFunction<T, R> field) {
        return month(LambdaUtils.getLambdaColumnName(field));
    }

    public static <T, R> FuncExpr day(TypeFunction<T, R> field) {
        return day(LambdaUtils.getLambdaColumnName(field));
    }

    public static <T, R> FuncExpr dateAdd(TypeFunction<T, R> field, int days) {
        return dateAdd(LambdaUtils.getLambdaColumnName(field), days);
    }

    public static <T, R> FuncExpr dateSub(TypeFunction<T, R> field, int days) {
        return dateSub(LambdaUtils.getLambdaColumnName(field), days);
    }

    // 条件
    public static <T, R> FuncExpr ifNull(TypeFunction<T, R> field, Object value) {
        return ifNull(LambdaUtils.getLambdaColumnName(field), value);
    }

    public static <T, R> FuncExpr nullIf(TypeFunction<T, R> field, Object value) {
        return nullIf(LambdaUtils.getLambdaColumnName(field), value);
    }

    @SafeVarargs
    public static <T, R> FuncExpr coalesce(TypeFunction<T, R>... fields) {
        return coalesce(toColumnArray(fields));
    }

    // 其它补齐
    public static <T, R> FuncExpr findInSet(String find, TypeFunction<T, R> str) {
        return findInSet(find, LambdaUtils.getLambdaColumnName(str));
    }

    public static <T, R> FuncExpr position(String sub, TypeFunction<T, R> str) {
        return position(sub, LambdaUtils.getLambdaColumnName(str));
    }

    @SafeVarargs
    public static <T, R> FuncExpr elt(int index, TypeFunction<T, R>... fields) {
        return elt(index, toColumnArray(fields));
    }

    public static <T, R> FuncExpr insert(TypeFunction<T, R> str, int pos, int len, String replacement) {
        return insert(LambdaUtils.getLambdaColumnName(str), pos, len, replacement);
    }

    public static <T, R> FuncExpr truncate(TypeFunction<T, R> field, int decimals) {
        return truncate(LambdaUtils.getLambdaColumnName(field), decimals);
    }

    public static <T, R> FuncExpr format(TypeFunction<T, R> field, int decimals) {
        return format(LambdaUtils.getLambdaColumnName(field), decimals);
    }

    public static <T, R> FuncExpr strToDate(TypeFunction<T, R> field, String format) {
        return strToDate(LambdaUtils.getLambdaColumnName(field), format);
    }

    public static <T, R> FuncExpr monthname(TypeFunction<T, R> field) {
        return monthname(LambdaUtils.getLambdaColumnName(field));
    }

    public static <T, R> FuncExpr week(TypeFunction<T, R> field) {
        return week(LambdaUtils.getLambdaColumnName(field));
    }

    public static <T, R> FuncExpr hour(TypeFunction<T, R> field) {
        return hour(LambdaUtils.getLambdaColumnName(field));
    }

    public static <T, R> FuncExpr minute(TypeFunction<T, R> field) {
        return minute(LambdaUtils.getLambdaColumnName(field));
    }

    public static <T, R> FuncExpr second(TypeFunction<T, R> field) {
        return second(LambdaUtils.getLambdaColumnName(field));
    }

    public static <T, R> FuncExpr weekday(TypeFunction<T, R> field) {
        return weekday(LambdaUtils.getLambdaColumnName(field));
    }

    public static <T, R> FuncExpr dayname(TypeFunction<T, R> field) {
        return dayname(LambdaUtils.getLambdaColumnName(field));
    }

    public static <T, R> FuncExpr date(TypeFunction<T, R> field) {
        return date(LambdaUtils.getLambdaColumnName(field));
    }

    public static <T, R> FuncExpr jsonExtract(TypeFunction<T, R> field, String path) {
        return jsonExtract(LambdaUtils.getLambdaColumnName(field), path);
    }

    public static <T, R> FuncExpr jsonUnquote(TypeFunction<T, R> field) {
        return jsonUnquote(LambdaUtils.getLambdaColumnName(field));
    }

    public static <T, R> FuncExpr jsonContains(TypeFunction<T, R> field, String value) {
        return jsonContains(LambdaUtils.getLambdaColumnName(field), value);
    }

    public static <T, R> FuncExpr jsonSet(TypeFunction<T, R> field, String path, Object value) {
        return jsonSet(LambdaUtils.getLambdaColumnName(field), path, value);
    }

    public static <T, R> FuncExpr jsonRemove(TypeFunction<T, R> field, String... paths) {
        return jsonRemove(LambdaUtils.getLambdaColumnName(field), paths);
    }

    public static <T, R> FuncExpr fromUnixTime(TypeFunction<T, R> field, String format) {
        return fromUnixTime(LambdaUtils.getLambdaColumnName(field), format);
    }

    public static <T, R> FuncExpr concat_ws(String separator, TypeFunction<T, R>... functions) {
        return concat_ws(separator, toColumnArray(functions));
    }

    // ------ 辅助 ------

    private static FuncExpr toExpr(Object value) {
        if (value instanceof FuncExpr) {
            return (FuncExpr) value;
        }
        if (value instanceof TypeFunction) {
            return FuncExpr.raw(LambdaUtils.getLambdaColumnName((TypeFunction<?, ?>) value));
        }
        if (value instanceof String) {
            return FuncExpr.raw((String) value);
        }
        return FuncExpr.value(value);
    }

    private static FuncExpr[] toRawArray(String... fields) {
        FuncExpr[] arr = new FuncExpr[fields.length];
        for (int i = 0; i < fields.length; i++) {
            arr[i] = FuncExpr.raw(fields[i]);
        }
        return arr;
    }

    @SafeVarargs
    private static <T, R> FuncExpr[] toRawArray(TypeFunction<T, R>... functions) {
        FuncExpr[] arr = new FuncExpr[functions.length];
        for (int i = 0; i < functions.length; i++) {
            arr[i] = FuncExpr.raw(LambdaUtils.getLambdaColumnName(functions[i]));
        }
        return arr;
    }

    @SafeVarargs
    private static <T, R> String[] toColumnArray(TypeFunction<T, R>... functions) {
        String[] arr = new String[functions.length];
        for (int i = 0; i < functions.length; i++) {
            arr[i] = LambdaUtils.getLambdaColumnName(functions[i]);
        }
        return arr;
    }

    private static FuncExpr[] merge(FuncExpr[] first, FuncExpr[] second) {
        FuncExpr[] arr = new FuncExpr[first.length + second.length];
        System.arraycopy(first, 0, arr, 0, first.length);
        System.arraycopy(second, 0, arr, first.length, second.length);
        return arr;
    }
}
