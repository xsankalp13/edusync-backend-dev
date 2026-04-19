package com.project.edusync.ams.config;

import com.project.edusync.ams.model.entity.AttendanceType;
import com.project.edusync.ams.model.repository.AttendanceTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AmsDataSeeder implements ApplicationRunner {

    private static final List<AttendanceTypeSeed> DEFAULT_TYPES = List.of(
            new AttendanceTypeSeed("Present",            "P",  true,  false, false, "#10b981"),
            new AttendanceTypeSeed("Absent",             "A",  false, true,  false, "#ef4444"),
            new AttendanceTypeSeed("Late",               "L",  false, false, true,  "#f59e0b"),
            new AttendanceTypeSeed("Half Day",           "HD", true,  false, false, "#f97316"),
            new AttendanceTypeSeed("Unexcused Absence",  "UA", false, true,  false, "#dc2626"),
            new AttendanceTypeSeed("Excused",            "E",  false, false, false, "#6366f1")
    );

    private final AttendanceTypeRepository attendanceTypeRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seedDefaultAttendanceTypes();
    }

    void seedDefaultAttendanceTypes() {
        for (AttendanceTypeSeed seed : DEFAULT_TYPES) {
            Optional<AttendanceType> existingOpt = attendanceTypeRepository.findByShortCodeIgnoreCase(seed.shortCode());
            if (existingOpt.isPresent()) {
                log.info("AMS seeder skipped attendance type {} ({}): already exists.", seed.typeName(), seed.shortCode());
                continue;
            }

            AttendanceType entity = new AttendanceType();
            entity.setTypeName(seed.typeName());
            entity.setShortCode(seed.shortCode());
            entity.setPresentMark(seed.presentMark());
            entity.setAbsenceMark(seed.absenceMark());
            entity.setLateMark(seed.lateMark());
            entity.setColorCode(seed.colorCode());
            entity.setActive(true);

            AttendanceType saved = attendanceTypeRepository.save(entity);
            log.info("AMS seeder inserted attendance type {} ({}).", saved.getTypeName(), saved.getShortCode());
        }
    }

    private record AttendanceTypeSeed(
            String typeName,
            String shortCode,
            boolean presentMark,
            boolean absenceMark,
            boolean lateMark,
            String colorCode
    ) {
    }
}


