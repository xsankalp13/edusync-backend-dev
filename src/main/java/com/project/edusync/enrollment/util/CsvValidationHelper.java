package com.project.edusync.enrollment.util;

import com.project.edusync.common.exception.enrollment.DataParsingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import java.math.BigDecimal;

/**
 * A stateless Spring component providing utility methods for validating and
 * parsing data from CSV rows.
 *
 * Each method now throws a specific {@link DataParsingException} upon failure,
 * which is caught by the import service to generate a per-row error report.
 */
@Component
@Slf4j
public class CsvValidationHelper {

    /**
     * A simple, common email validation regex.
     * While not 100% RFC 5322 compliant, it's sufficient for 99.9%
     * of practical use cases and prevents malformed data.
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$"
    );

    /**
     * A common date formatter. Adjust this if your CSVs use a different format
     * (e.g., "dd/MM/yyyy"). "yyyy-MM-dd" is the ISO standard.
     */
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // e.g., "2025-11-11"

    /**
     * Validates that a required string field is not null or blank.
     *
     * @param value The string value from the CSV cell.
     * @param fieldName The name of the field (e.g., "firstName") for error messages.
     * @return The original, trimmed value if valid.
     * @throws IllegalArgumentException if the string is blank or null.
     */
    public String validateString(String value, String fieldName) {
        if (StringUtils.isBlank(value)) {
            // USE: DataParsingException
            throw new DataParsingException(fieldName + " cannot be null or empty.");
        }
        return value.trim();
    }

    /**
     * Validates a string as a proper email address.
     * @throws DataParsingException if the email is blank or has an invalid format.
     */
    public String validateEmail(String email) {
        String validatedEmail = validateString(email, "email");
        if (!EMAIL_PATTERN.matcher(validatedEmail).matches()) {
            // USE: DataParsingException
            throw new DataParsingException("Invalid email format: '" + validatedEmail + "'.");
        }
        return validatedEmail;
    }

    /**
     * Validates and normalizes phone numbers.
     * Handles Scientific notation losslessly if possible (e.g. 9.19123E+11 -> 919123000000).
     * @throws DataParsingException if the string is empty or invalid format.
     */
    public String validatePhoneNumber(String value, String fieldName) {
        String validatedValue = validateString(value, fieldName);

        // Handle scientific notation gracefully (e.g. "9.19123E+11" -> "919123000000")
        if (validatedValue.toUpperCase().contains("E")) {
            try {
                BigDecimal bd = new BigDecimal(validatedValue);
                validatedValue = bd.toPlainString();
                // If it parses to something with decimals (like 919.5), strip decimal point
                if (validatedValue.contains(".")) {
                    validatedValue = validatedValue.substring(0, validatedValue.indexOf('.'));
                }
            } catch (NumberFormatException e) {
                throw new DataParsingException("Invalid scientific notation for phone number: '" + value + "'.");
            }
        }

        // Remove all non-numeric characters except leading '+'
        String normalized = validatedValue.replaceAll("[^0-9+]", "");
        
        // Basic length validation
        int digitCount = normalized.replaceAll("[^0-9]", "").length();
        if (digitCount < 10 || digitCount > 15) {
            throw new DataParsingException(
                "Phone number '" + value + "' for " + fieldName + " is invalid. Must contain 10-15 digits."
            );
        }

        return normalized;
    }

    /**
     * Parses a string into a {@link LocalDate}.
     * @throws DataParsingException if the string is blank or not a valid date.
     */
    public LocalDate parseDate(String dateStr, String fieldName) {
        String trimmedDate = validateString(dateStr, fieldName);
        try {
            return LocalDate.parse(trimmedDate, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            log.warn("Failed to parse date string '{}' for field '{}'", trimmedDate, fieldName);
            // USE: DataParsingException
            throw new DataParsingException(
                    "Invalid date format for " + fieldName + ". Expected format: yyyy-MM-dd."
            );
        }
    }

    /**
     * Parses a string into an {@link Integer}.
     * @throws DataParsingException if the string is blank or not a valid integer.
     */
    public Integer parseInt(String intStr, String fieldName) {
        String trimmedInt = validateString(intStr, fieldName);
        try {
            return Integer.parseInt(trimmedInt);
        } catch (NumberFormatException e) {
            log.warn("Failed to parse integer string '{}' for field '{}'", trimmedInt, fieldName);
            // USE: DataParsingException
            throw new DataParsingException(
                    fieldName + " is not a valid number: '" + trimmedInt + "'."
            );
        }
    }

    /**
     * Parses a string into a {@link Boolean}.
     * @throws DataParsingException if the string is blank or not a valid boolean.
     */
    public Boolean parseBoolean(String boolStr, String fieldName) {
        String validatedStr = validateString(boolStr, fieldName).toLowerCase();

        if ("true".equals(validatedStr) || "1".equals(validatedStr) || "yes".equals(validatedStr)) {
            return true;
        }
        if ("false".equals(validatedStr) || "0".equals(validatedStr) || "no".equals(validatedStr)) {
            return false;
        }

        // USE: DataParsingException
        throw new DataParsingException(
                "Invalid boolean value for " + fieldName + ": '" + boolStr + "'. Expected true/false, 1/0, or yes/no."
        );
    }

    /**
     * A generic, case-insensitive parser for any Enum class.
     * @throws DataParsingException if the string is blank or no matching enum constant
     * is found.
     */
    public <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, String fieldName) {
        String validatedValue = validateString(value, fieldName);
        try {
            return E.valueOf(enumClass, validatedValue.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Failed to parse enum {} for field '{}' with value '{}'",
                    enumClass.getSimpleName(), fieldName, validatedValue);
            // USE: DataParsingException
            throw new DataParsingException(
                    "Invalid value for " + fieldName + ": '" + validatedValue + "'."
            );
        }
    }
}