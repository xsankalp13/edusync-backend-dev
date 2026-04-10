package com.project.edusync.common.config;

import com.project.edusync.adm.model.entity.AcademicClass;
import com.project.edusync.adm.model.entity.Section;
import com.project.edusync.adm.repository.AcademicClassRepository;
import com.project.edusync.common.settings.model.entity.AppSetting;
import com.project.edusync.common.settings.model.enums.SettingGroup;
import com.project.edusync.common.settings.model.enums.SettingType;
import com.project.edusync.common.settings.repository.AppSettingRepository;
import com.project.edusync.common.settings.security.AppSettingCryptoService;
import com.project.edusync.iam.model.entity.Permission;
import com.project.edusync.iam.model.entity.Role;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.iam.repository.PermissionRepository;
import com.project.edusync.iam.repository.RoleRepository;
import com.project.edusync.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment; // Import the Environment class
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This component runs once on application startup.
 * It checks if the spring.jpa.hibernate.ddl-auto property is set to 'create'.
 * If it is, it populates the database with foundational data (Roles, Classes).
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final AcademicClassRepository classRepository;
    private final AppSettingRepository appSettingRepository;
    private final AppSettingCryptoService appSettingCryptoService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment; // 1. Inject the Spring Environment

    private static final Map<String, List<String>> ROLE_PERMISSION_BLUEPRINT = buildRolePermissionBlueprint();

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {

        // 2. Get the ddl-auto property value from the environment
        String ddlAuto = environment.getProperty("spring.jpa.hibernate.ddl-auto");

        log.info("Running RBAC role/permission bootstrap in idempotent mode...");
        Map<String, Role> rolesByName = seedRoles();
        seedPermissionsAndRoleMappings(rolesByName);
        seedSuperAdminUser(rolesByName);
        seedAppSettings();

        // Class/section fixture data should be inserted only on schema-create mode.
        if ("create".equalsIgnoreCase(ddlAuto)) {
            log.info("DDL-AUTO mode is 'create'. Running class/section seeder...");
            seedClassesAndSections();
        } else {
            log.info("DDL-AUTO mode is '{}' - skipping class/section fixture seeding.", ddlAuto);
        }

        log.info("Data seeding complete.");
    }

    private Map<String, Role> seedRoles() {
        log.info("Seeding foundational Roles (idempotent)...");

        List<String> roleNames = Arrays.asList(
                "ROLE_STUDENT",
                "ROLE_TEACHER",
                "ROLE_PRINCIPAL",
                "ROLE_LIBRARIAN",
                "ROLE_GUARDIAN",
                "ROLE_ADMIN",
                "ROLE_SUPER_ADMIN",
                "ROLE_SCHOOL_ADMIN"
        );

        Map<String, Role> rolesByName = new LinkedHashMap<>();
        for (String roleName : roleNames) {
            Role role = roleRepository.findByName(roleName).orElse(null);
            if (role == null) {
                Role created = createRole(roleName);
                Role saved = roleRepository.save(created);
                rolesByName.put(saved.getName(), saved);
                continue;
            }

            if (!role.isActive()) {
                role.setActive(true);
                role = roleRepository.save(role);
            }
            rolesByName.put(role.getName(), role);
        }

        log.info("Ensured {} foundational roles.", rolesByName.size());
        return rolesByName;
    }

    private Role createRole(String name) {
        Role role = new Role();
        role.setName(name);
        role.setActive(true);
        // Note: uuid, createdAt, etc., are set automatically
        // by AuditableEntity's @PrePersist and @CreatedDate
        return role;
    }

    private void seedPermissionsAndRoleMappings(Map<String, Role> rolesByName) {
        log.info("Seeding RBAC permissions and role-permission mappings...");

        List<String> permissionNames = ROLE_PERMISSION_BLUEPRINT.values().stream()
                .flatMap(List::stream)
                .distinct()
                .toList();

        Map<String, Permission> permissionByName = new LinkedHashMap<>();
        for (String permissionName : permissionNames) {
            Permission permission = permissionRepository.findByName(permissionName).orElse(null);
            if (permission == null) {
                Permission created = createPermission(permissionName);
                Permission saved = permissionRepository.save(created);
                permissionByName.put(saved.getName(), saved);
                continue;
            }

            if (!permission.isActive()) {
                permission.setActive(true);
                permission = permissionRepository.save(permission);
            }
            permissionByName.put(permission.getName(), permission);
        }

        for (Map.Entry<String, List<String>> entry : ROLE_PERMISSION_BLUEPRINT.entrySet()) {
            String roleName = entry.getKey();
            Role role = rolesByName.get(roleName);
            if (role == null) {
                log.warn("Role '{}' was not found while assigning permissions. Skipping mapping.", roleName);
                continue;
            }

            Set<Permission> mappedPermissions = entry.getValue().stream()
                    .map(permissionByName::get)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toCollection(HashSet::new));

            Set<Permission> existingPermissions = role.getPermissions();
            if (existingPermissions == null) {
                existingPermissions = new HashSet<>();
                role.setPermissions(existingPermissions);
            }

            int beforeCount = existingPermissions.size();
            boolean changed = existingPermissions.addAll(mappedPermissions);
            int afterCount = existingPermissions.size();

            if (changed) {
                roleRepository.save(role);
            }
            log.info("Role {} now has {} permissions (added {})", roleName, afterCount, (afterCount - beforeCount));
        }

        log.info("Ensured {} permissions and completed role-permission mapping.", permissionByName.size());
    }

    private Permission createPermission(String name) {
        Permission permission = new Permission();
        permission.setName(name);
        permission.setActive(true);
        return permission;
    }

    private static Map<String, List<String>> buildRolePermissionBlueprint() {
        Map<String, List<String>> blueprint = new LinkedHashMap<>();

        blueprint.put("ROLE_STUDENT", List.of(
                "profile:read:own",
                "profile:update:own",
                "dashboard:read:own",
                "attendance:read:own",
                "finance:read:own",
                "timetable:read:own",
                "exam:read:own"
        ));

        blueprint.put("ROLE_GUARDIAN", List.of(
                "profile:read:own",
                "profile:update:own",
                "dashboard:read:linked",
                "attendance:read:linked",
                "finance:read:linked",
                "timetable:read:linked",
                "exam:read:linked",
                "student:read:linked"
        ));

        blueprint.put("ROLE_TEACHER", List.of(
                "profile:read:own",
                "profile:update:own",
                "dashboard:read:own",
                "attendance:mark:section",
                "attendance:update:section",
                "attendance:read:section",
                "marks:read:section",
                "marks:update:section",
                "timetable:read:own",
                "student:read:section",
                "evaluation:assignment:read:own",
                "evaluation:answer-sheet:upload",
                "evaluation:marks:draft",
                "evaluation:marks:publish",
                "evaluation:annotation:write"
        ));

        blueprint.put("ROLE_LIBRARIAN", List.of(
                "profile:read:own",
                "profile:update:own",
                "library:book:issue",
                "library:book:return",
                "library:inventory:read",
                "library:inventory:update",
                "library:member:read"
        ));

        blueprint.put("ROLE_PRINCIPAL", List.of(
                "profile:read:own",
                "dashboard:read:school",
                "users:read:all",
                "attendance:read:all",
                "finance:read:all",
                "timetable:read:all",
                "exam:read:all",
                "reports:read:school"
        ));

        blueprint.put("ROLE_SCHOOL_ADMIN", List.of(
                "profile:read:own",
                "profile:update:own",
                "profile:read:all",
                "profile:update:all",
                "dashboard:read:school",
                "users:create",
                "users:read:all",
                "users:update",
                "users:deactivate",
                "adm:class:manage",
                "adm:section:manage",
                "adm:schedule:manage",
                "attendance:config:manage",
                "finance:manage",
                "reports:read:school",
                "rbac:permission:create",
                "rbac:permission:read",
                "rbac:role-permission:assign",
                "rbac:role-permission:revoke",
                "rbac:role-permission:read",
                "evaluation:assignment:manage",
                "evaluation:assignment:read:all",
                "evaluation:answer-sheet:read:all"
        ));

        blueprint.put("ROLE_ADMIN", List.of(
                "profile:read:all",
                "profile:update:all",
                "dashboard:read:school",
                "users:create",
                "users:read:all",
                "users:update",
                "users:deactivate",
                "attendance:config:manage",
                "finance:manage",
                "reports:read:school",
                "rbac:permission:create",
                "rbac:permission:read",
                "rbac:role-permission:assign",
                "rbac:role-permission:revoke",
                "rbac:role-permission:read",
                "evaluation:assignment:manage",
                "evaluation:assignment:read:all",
                "evaluation:answer-sheet:read:all"
        ));

        blueprint.put("ROLE_SUPER_ADMIN", List.of(
                "profile:read:all",
                "profile:update:all",
                "dashboard:read:school",
                "users:create",
                "users:read:all",
                "users:update",
                "users:deactivate",
                "adm:class:manage",
                "adm:section:manage",
                "adm:schedule:manage",
                "attendance:config:manage",
                "finance:manage",
                "reports:read:school",
                "rbac:permission:create",
                "rbac:permission:read",
                "rbac:role-permission:assign",
                "rbac:role-permission:revoke",
                "rbac:role-permission:read",
                "system:settings:manage",
                "evaluation:assignment:manage",
                "evaluation:assignment:read:all",
                "evaluation:answer-sheet:read:all"
        ));

        return blueprint;
    }

    private void seedClassesAndSections() {
        // No .count() check needed.
        log.info("Seeding foundational Academic Classes and Sections...");

        // Use the helper to create classes and their sections in one go
        createClassWithSections("Nursery", Arrays.asList("A", "B"));
        createClassWithSections("LKG", Arrays.asList("A", "B"));
        createClassWithSections("UKG", Arrays.asList("A", "B"));

        List<String> standardSections = Arrays.asList("A", "B", "C");
        for (int i = 1; i <= 12; i++) {
            createClassWithSections("Class " + i, standardSections);
        }

        log.info("Saved {} classes and their corresponding sections.", classRepository.count());
    }

    /**
     * Helper method to create an AcademicClass and its child Sections.
     * This leverages CascadeType.ALL on the 'sections' relationship
     * in the AcademicClass entity.
     */
    private void createClassWithSections(String className, List<String> sectionNames) {
        // 1. Create the parent (AcademicClass)
        AcademicClass ac = new AcademicClass();
        ac.setName(className);
        ac.setIsActive(true);

        // 2. Create the children (Sections) and link them to the parent
        Set<Section> sections = new HashSet<>();
        for (String sectionName : sectionNames) {
            Section section = new Section();
            section.setSectionName(sectionName);
            section.setAcademicClass(ac); // Link child to parent
            sections.add(section);
        }

        // 3. Set the relationship on the parent side
        ac.setSections(sections);

        // 4. Save the parent.
        // Because of @OneToMany(mappedBy = "academicClass", cascade = CascadeType.ALL...)
        // saving the parent (AcademicClass) will automatically save all the
        // linked Section entities in the same transaction.
        classRepository.save(ac);
        log.debug("Created class: {} with sections: {}", className, sectionNames);
    }

    private void seedAppSettings() {
        log.info("Seeding default app settings (idempotent)...");

        List<SettingSeed> seeds = List.of(
                // SMTP
                seed("smtp.host", "smtp.gmail.com", SettingType.STRING, SettingGroup.SMTP, "SMTP server hostname", true, false),
                seed("smtp.port", "587", SettingType.INTEGER, SettingGroup.SMTP, "SMTP server port", true, false),
                seed("smtp.username", "", SettingType.STRING, SettingGroup.SMTP, "SMTP username", true, false),
                seed("smtp.password", "", SettingType.ENCRYPTED, SettingGroup.SMTP, "SMTP authentication password", true, true),
                seed("smtp.from.email", "", SettingType.STRING, SettingGroup.SMTP, "From email address", false, false),
                seed("smtp.from.name", "Shiksha Intelligence", SettingType.STRING, SettingGroup.SMTP, "From display name", false, false),
                seed("smtp.tls.enabled", "true", SettingType.BOOLEAN, SettingGroup.SMTP, "Enable STARTTLS", true, false),
                // Storage
                seed("storage.provider", "CLOUDINARY", SettingType.STRING, SettingGroup.STORAGE, "Active storage backend: CLOUDINARY or AWS_S3", true, false),
                seed("cloudinary.cloud_name", "", SettingType.STRING, SettingGroup.STORAGE, "Cloudinary cloud name", true, false),
                seed("cloudinary.api_key", "", SettingType.STRING, SettingGroup.STORAGE, "Cloudinary API key", true, false),
                seed("cloudinary.api_secret", "", SettingType.ENCRYPTED, SettingGroup.STORAGE, "Cloudinary API secret", true, true),
                seed("aws.s3.region", "ap-south-1", SettingType.STRING, SettingGroup.STORAGE, "AWS region", true, false),
                seed("aws.s3.bucket", "", SettingType.STRING, SettingGroup.STORAGE, "AWS S3 bucket name", true, false),
                seed("aws.access_key_id", "", SettingType.STRING, SettingGroup.STORAGE, "AWS access key id", true, false),
                seed("aws.secret_access_key", "", SettingType.ENCRYPTED, SettingGroup.STORAGE, "AWS secret access key", true, true),
                // Security
                seed("auth.default_password", "School@1234", SettingType.ENCRYPTED, SettingGroup.SECURITY, "Default password used by admin reset", false, true),
                seed("auth.password.min_length", "8", SettingType.INTEGER, SettingGroup.SECURITY, "Minimum password length", false, false),
                seed("auth.session.timeout_minutes", "60", SettingType.INTEGER, SettingGroup.SECURITY, "Session timeout in minutes", false, false),
                seed("auth.max_login_attempts", "5", SettingType.INTEGER, SettingGroup.SECURITY, "Maximum login attempts before lock", false, false),
                seed("jwt.access_expiry_minutes", "30", SettingType.INTEGER, SettingGroup.SECURITY, "JWT access token expiry in minutes", true, false),
                seed("jwt.refresh_expiry_days", "7", SettingType.INTEGER, SettingGroup.SECURITY, "JWT refresh token expiry in days", true, false),
                // White-label
                seed("school.name", "My School", SettingType.STRING, SettingGroup.WHITELABEL, "School name", false, false),
                seed("school.logo_url", "", SettingType.STRING, SettingGroup.WHITELABEL, "School logo URL", false, false),
                seed("school.primary_color", "#6366f1", SettingType.STRING, SettingGroup.WHITELABEL, "Primary brand color", false, false),
                seed("school.accent_color", "#8b5cf6", SettingType.STRING, SettingGroup.WHITELABEL, "Accent brand color", false, false),
                seed("school.id_card_template", "classic", SettingType.STRING, SettingGroup.WHITELABEL, "Master designated template for system ID Cards (classic, modern, minimal)", false, false),
                seed("school.timezone", "Asia/Kolkata", SettingType.STRING, SettingGroup.WHITELABEL, "Default timezone", false, false),
                seed("school.currency", "INR", SettingType.STRING, SettingGroup.WHITELABEL, "Currency code", false, false),
                seed("school.date_format", "DD/MM/YYYY", SettingType.STRING, SettingGroup.WHITELABEL, "Default date format", false, false),
                seed("school.academic_year_start", "APRIL", SettingType.STRING, SettingGroup.WHITELABEL, "Academic year start month", false, false),
                seed("school.address", "", SettingType.STRING, SettingGroup.WHITELABEL, "School address for ID cards and documents", false, false),
                seed("school.phone", "", SettingType.STRING, SettingGroup.WHITELABEL, "School contact phone number", false, false),
                seed("school.email", "", SettingType.STRING, SettingGroup.WHITELABEL, "School contact email address", false, false),
                seed("school.website", "", SettingType.STRING, SettingGroup.WHITELABEL, "School website URL", false, false),
                seed("school.signature_url", "", SettingType.STRING, SettingGroup.WHITELABEL, "Principal/Director signature image URL", false, false),
                seed("school.short_name", "", SettingType.STRING, SettingGroup.WHITELABEL, "School abbreviation/short name (e.g. IIMT)", false, false),
                seed("school.tagline", "", SettingType.STRING, SettingGroup.WHITELABEL, "School tagline or subtitle", false, false),
                seed("school.id_card_header_mode", "TEXT", SettingType.STRING, SettingGroup.WHITELABEL, "ID card header content mode: TEXT or IMAGE", false, false),
                seed("school.id_card_header_image_url", "", SettingType.STRING, SettingGroup.WHITELABEL, "ID card header PNG/JPG URL used when header mode is IMAGE", false, false),
                // Features
                seed("feature.finance", "true", SettingType.BOOLEAN, SettingGroup.FEATURES, "Enable Finance module", false, false),
                seed("feature.examination", "true", SettingType.BOOLEAN, SettingGroup.FEATURES, "Enable Examination module", false, false),
                seed("feature.attendance", "true", SettingType.BOOLEAN, SettingGroup.FEATURES, "Enable Attendance module", false, false),
                seed("feature.timetable_ai", "true", SettingType.BOOLEAN, SettingGroup.FEATURES, "Enable AI timetable module", false, false),
                seed("feature.bulk_import", "true", SettingType.BOOLEAN, SettingGroup.FEATURES, "Enable bulk import module", false, false),
                seed("feature.parent_portal", "false", SettingType.BOOLEAN, SettingGroup.FEATURES, "Enable parent portal", false, false),
                seed("feature.sms_notifications", "false", SettingType.BOOLEAN, SettingGroup.FEATURES, "Enable SMS notifications", false, false),
                // Attendance controls
                // Stored under FEATURES to remain compatible with existing DB check constraint.
                // AppSettingService remaps attendance.* keys to ATTENDANCE group in responses.
                seed("attendance.edit.window.enabled", "true", SettingType.BOOLEAN, SettingGroup.FEATURES, "Enable/disable edit window enforcement", false, false),
                seed("attendance.edit.window.teacher.hours", "48", SettingType.INTEGER, SettingGroup.FEATURES, "Hours within which teachers can edit attendance", false, false),
                seed("attendance.edit.window.school_admin.hours", "0", SettingType.INTEGER, SettingGroup.FEATURES, "Hours within which school admins can edit attendance. 0 = unlimited", false, false),
                seed("attendance.geofence.enabled", "true", SettingType.BOOLEAN, SettingGroup.FEATURES, "Enable geo-fence validation for staff self check-in", false, false),
                seed("attendance.geofence.latitude", "", SettingType.STRING, SettingGroup.FEATURES, "School latitude coordinate (decimal degrees)", false, false),
                seed("attendance.geofence.longitude", "", SettingType.STRING, SettingGroup.FEATURES, "School longitude coordinate (decimal degrees)", false, false),
                seed("attendance.geofence.radius.meters", "200", SettingType.INTEGER, SettingGroup.FEATURES, "Maximum distance in meters from school for valid check-in", false, false)
        );

        int inserted = 0;
        for (SettingSeed seed : seeds) {
            if (appSettingRepository.existsById(seed.key())) {
                continue;
            }

            AppSetting setting = new AppSetting();
            setting.setKey(seed.key());
            setting.setType(seed.type());
            setting.setSettingGroup(seed.group());
            setting.setDescription(seed.description());
            setting.setRequiresRestart(seed.requiresRestart());
            setting.setSensitive(seed.sensitive());
            setting.setUpdatedAt(java.time.Instant.now());
            setting.setUpdatedBy("system-seeder");
            setting.setValue(seed.type() == SettingType.ENCRYPTED
                    ? appSettingCryptoService.encryptForStorage(seed.defaultValue())
                    : seed.defaultValue());

            appSettingRepository.save(setting);
            inserted++;
        }

        log.info("Default app settings seeding complete. Inserted {} new keys.", inserted);
    }

    private void seedSuperAdminUser(Map<String, Role> rolesByName) {
        if (userRepository.existsByRoles_Name("ROLE_SUPER_ADMIN")) {
            log.info("SUPER_ADMIN user already exists. Skipping SUPER_ADMIN bootstrap.");
            return;
        }

        String username = environment.getProperty("SUPER_ADMIN_USERNAME");
        String rawPassword = environment.getProperty("SUPER_ADMIN_PASSWORD");
        boolean failOnMissingCredentials = environment.getProperty(
                "app.bootstrap.super-admin.fail-on-missing-credentials",
                Boolean.class,
                false
        );

        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()) {
            if (failOnMissingCredentials) {
                throw new IllegalStateException(
                        "SUPER_ADMIN bootstrap requires SUPER_ADMIN_USERNAME and SUPER_ADMIN_PASSWORD on first deployment."
                );
            }
            log.warn("SUPER_ADMIN bootstrap skipped: SUPER_ADMIN_USERNAME/SUPER_ADMIN_PASSWORD not provided and strict mode is disabled.");
            return;
        }

        Role superAdminRole = rolesByName.get("ROLE_SUPER_ADMIN");
        if (superAdminRole == null) {
            throw new IllegalStateException("ROLE_SUPER_ADMIN was not found during bootstrap.");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            log.warn("Username '{}' already exists and no SUPER_ADMIN role holder was found. SUPER_ADMIN bootstrap skipped.", username);
            return;
        }

        User superAdmin = new User();
        superAdmin.setUsername(username.trim());
        superAdmin.setPassword(passwordEncoder.encode(rawPassword));
        superAdmin.setActive(true);
        // Null lastLoginTimestamp is used by existing login flow to enforce password-change UX.
        superAdmin.setLastLoginTimestamp(null);
        superAdmin.setRoles(Set.of(superAdminRole));

        userRepository.save(superAdmin);
        log.info("Bootstrapped initial SUPER_ADMIN user '{}'.", username);
    }

    private SettingSeed seed(String key,
                             String defaultValue,
                             SettingType type,
                             SettingGroup group,
                             String description,
                             boolean requiresRestart,
                             boolean sensitive) {
        return new SettingSeed(key, defaultValue, type, group, description, requiresRestart, sensitive);
    }

    private record SettingSeed(String key,
                               String defaultValue,
                               SettingType type,
                               SettingGroup group,
                               String description,
                               boolean requiresRestart,
                               boolean sensitive) {
    }
}

