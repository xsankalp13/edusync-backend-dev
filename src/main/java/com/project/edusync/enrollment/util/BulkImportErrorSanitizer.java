package com.project.edusync.enrollment.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;

import com.project.edusync.common.exception.enrollment.BulkImportException;
import com.project.edusync.common.exception.enrollment.DataParsingException;
import com.project.edusync.common.exception.enrollment.InvalidCsvHeaderException;
import com.project.edusync.common.exception.enrollment.RelatedResourceNotFoundException;
import com.project.edusync.common.exception.enrollment.ResourceDuplicateException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Translates raw Java/Hibernate/JDBC exceptions into short, human-readable
 * messages that are safe to display in the bulk-import results UI.
 *
 * <h3>Design principles</h3>
 * <ul>
 *   <li>Domain exceptions ({@link DataParsingException}, {@link ResourceDuplicateException},
 *       {@link BulkImportException}, etc.) already carry clean messages — pass them through.</li>
 *   <li>Infrastructure exceptions ({@link DataIntegrityViolationException} and anything that
 *       contains raw SQL, Hibernate stack traces or DB column names) are <em>mapped</em> to
 *       a short, safe message. The original exception is always logged server-side.</li>
 *   <li>Unknown exceptions fall back to a generic "please try again" message.</li>
 * </ul>
 *
 * <p>The raw exception message is <strong>never</strong> sent to the frontend.</p>
 */
@UtilityClass
@Slf4j
public class BulkImportErrorSanitizer {

    // Regex patterns to detect specific DB-level constraint types
    private static final Pattern NOT_NULL_PATTERN =
            Pattern.compile("null value in column \"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNIQUE_PATTERN =
            Pattern.compile("(?:unique constraint|duplicate key|duplicate entry)[^\"]*\"?([^\",;]+)?",
                    Pattern.CASE_INSENSITIVE);
    private static final Pattern FK_PATTERN =
            Pattern.compile("(?:foreign key|not present in table|violates foreign)",
                    Pattern.CASE_INSENSITIVE);

    // SQL keywords that indicate a raw infrastructure message — never pass these through
    private static final Pattern SQL_KEYWORDS = Pattern.compile(
            "\\b(?:SELECT|INSERT|UPDATE|DELETE|FROM|WHERE|JOIN|INTO|VALUES|ALTER|TABLE|CONSTRAINT|"
                    + "Hibernate|org\\.postgresql|org\\.hibernate|com\\.zaxxer|"
                    + "could not execute|JDBC|SQL|HibernateJdbcException)\\b",
            Pattern.CASE_INSENSITIVE);

    /**
     * Converts any exception into a clean, user-safe message.
     *
     * @param e the exception thrown during row processing
     * @return a short, human-readable error message (≤ 250 chars)
     */
    public static String sanitize(Exception e) {
        if (e == null) return "Unknown error occurred.";

        // ── 1. Domain exceptions — always clean, pass through ─────────────────
        if (e instanceof DataParsingException
                || e instanceof ResourceDuplicateException
                || e instanceof RelatedResourceNotFoundException
                || e instanceof BulkImportException
                || e instanceof InvalidCsvHeaderException) {
            return truncate(e.getMessage());
        }

        // ── 2. Plain RuntimeException with a clean, short message ─────────────
        if (e instanceof RuntimeException && isCleanMessage(e.getMessage())) {
            return truncate(e.getMessage());
        }

        // ── 3. DataIntegrityViolationException — map by constraint type ────────
        if (e instanceof DataIntegrityViolationException dive) {
            return sanitizeDataIntegrityViolation(dive);
        }

        // ── 4. IllegalArgumentException with clean message ────────────────────
        if (e instanceof IllegalArgumentException && isCleanMessage(e.getMessage())) {
            return truncate(e.getMessage());
        }

        // ── 5. Anything else — log privately, return generic message ──────────
        log.warn("[BulkImportErrorSanitizer] Unmapped exception type '{}': {}",
                e.getClass().getSimpleName(), e.getMessage());
        return "Unexpected error processing this row. Please check the data and try again.";
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static String sanitizeDataIntegrityViolation(DataIntegrityViolationException dive) {
        // Drill down to the most specific cause for best pattern matching
        String raw = drill(dive);

        // NOT NULL constraint
        Matcher notNull = NOT_NULL_PATTERN.matcher(raw);
        if (notNull.find()) {
            String col = notNull.group(1); // e.g. "job_title"
            String friendly = camelify(col); // "job_title" → "job title"
            return "Missing required field: '" + friendly + "'. Please provide a value and try again.";
        }

        // UNIQUE / duplicate key
        if (UNIQUE_PATTERN.matcher(raw).find()) {
            return "Duplicate entry: a record with this value already exists. "
                    + "Check for duplicate emails or employee IDs.";
        }

        // FOREIGN KEY constraint
        if (FK_PATTERN.matcher(raw).find()) {
            return "Invalid reference: a linked record does not exist. "
                    + "Ensure all related records (e.g. designation, section) are created first.";
        }

        // Fell through — still a DB error, don't expose raw SQL
        log.debug("[BulkImportErrorSanitizer] Unrecognised DataIntegrityViolation: {}", raw);
        return "Data constraint violation. Please check the row values and try again.";
    }

    /**
     * Drills into the exception chain to find the most specific cause message.
     * Falls back to the top-level message if no deeper cause exists.
     */
    private static String drill(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        String msg = cause.getMessage();
        return (msg != null) ? msg : (t.getMessage() != null ? t.getMessage() : "");
    }

    /**
     * Returns true if the message looks safe to show the user directly —
     * i.e. it does not contain SQL, Hibernate class names, or stack trace noise.
     */
    private static boolean isCleanMessage(String message) {
        if (message == null || message.isBlank()) return false;
        if (message.length() > 300) return false; // suspiciously long
        return !SQL_KEYWORDS.matcher(message).find();
    }

    /** Truncates a message to 250 chars with ellipsis if needed. */
    private static String truncate(String msg) {
        if (msg == null) return "Unknown error.";
        return msg.length() > 250 ? msg.substring(0, 247) + "..." : msg;
    }

    /** Converts snake_case DB column names to a human-friendly form. */
    private static String camelify(String colName) {
        return colName.replace('_', ' ');
    }
}
