package com.project.edusync.iam.service;

import com.project.edusync.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;

/**
 * Generates unique, slug-safe usernames from a staff member's name.
 *
 * <p>Strategy:
 * <ol>
 *   <li>Base: {@code firstname.lastname} (all lowercase, diacritics stripped)</li>
 *   <li>If taken: {@code firstname.lastname2}, {@code firstname.lastname3} … up to 99</li>
 *   <li>Falls back to a UUID-suffix if all numeric suffixes are taken</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class UsernameGeneratorService {

    private final UserRepository userRepository;

    /**
     * Generates a unique, available username for a staff member.
     *
     * @param firstName The staff member's first name (required).
     * @param lastName  The staff member's last name (required).
     * @return A unique username that is not yet registered in the system.
     */
    @Transactional(readOnly = true)
    public String generate(String firstName, String lastName) {
        String base = buildBase(firstName, lastName);
        if (!userRepository.existsByUsername(base)) {
            return base;
        }
        // Try numeric suffixes 2..99
        for (int i = 2; i <= 99; i++) {
            String candidate = base + i;
            if (!userRepository.existsByUsername(candidate)) {
                return candidate;
            }
        }
        // Final fallback — UUID suffix
        return base + "_" + java.util.UUID.randomUUID().toString().substring(0, 6);
    }

    /**
     * Quick availability check — can be called before commit.
     *
     * @param username The username to test.
     * @return {@code true} if the username is available.
     */
    @Transactional(readOnly = true)
    public boolean isAvailable(String username) {
        return !userRepository.existsByUsername(username.toLowerCase().trim());
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private String buildBase(String firstName, String lastName) {
        String fn = slugify(firstName);
        String ln = slugify(lastName);
        if (fn.isEmpty() && ln.isEmpty()) return "staff";
        if (fn.isEmpty()) return ln;
        if (ln.isEmpty()) return fn;
        return fn + "." + ln;
    }

    /**
     * Converts a name segment into a URL+username safe lowercase slug:
     * strips diacritics, removes non-alphanumeric characters, lowercases, truncates.
     */
    private static String slugify(String input) {
        if (input == null || input.isBlank()) return "";
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD);
        // Remove combining diacritical marks
        String stripped = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        // Lowercase, keep only a-z0-9, truncate to 25 chars
        String slug = stripped.toLowerCase().replaceAll("[^a-z0-9]", "");
        return slug.length() > 25 ? slug.substring(0, 25) : slug;
    }
}
