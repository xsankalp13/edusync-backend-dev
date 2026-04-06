package com.project.edusync.teacher.service.impl;

import com.project.edusync.adm.model.entity.Section;
import com.project.edusync.adm.repository.SectionRepository;
import com.project.edusync.ams.model.entity.StudentDailyAttendance;
import com.project.edusync.ams.model.repository.StudentDailyAttendanceRepository;
import com.project.edusync.common.exception.EdusyncException;
import com.project.edusync.finance.service.PdfGenerationService;
import com.project.edusync.teacher.service.AttendanceExportService;
import com.project.edusync.uis.model.entity.Staff;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.repository.StaffRepository;
import com.project.edusync.uis.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceExportServiceImpl implements AttendanceExportService {

    private final StaffRepository staffRepository;
    private final SectionRepository sectionRepository;
    private final StudentRepository studentRepository;
    private final StudentDailyAttendanceRepository studentDailyAttendanceRepository;
    private final PdfGenerationService pdfGenerationService;

    @Override
    @Transactional(readOnly = true)
    public byte[] exportDailyAttendanceSheet(Long currentUserId, UUID sectionUuid, LocalDate date) {
        Staff staff = staffRepository.findByUserProfile_User_Id(currentUserId)
                .orElseThrow(() -> new EdusyncException("Authenticated user is not linked to a staff profile", HttpStatus.FORBIDDEN));

        Section section = sectionRepository.findByUuid(sectionUuid)
                .orElseThrow(() -> new EdusyncException("Section not found", HttpStatus.NOT_FOUND));

        if (section.getClassTeacher() == null || !staff.getId().equals(section.getClassTeacher().getId())) {
            throw new EdusyncException("You are not the class teacher for this section", HttpStatus.FORBIDDEN);
        }

        List<Student> students = studentRepository.findAllBySectionUuidWithDetails(sectionUuid);
        List<Long> studentIds = students.stream().map(Student::getId).toList();

        Map<Long, StudentDailyAttendance> attendanceByStudentId = new HashMap<>();
        if (!studentIds.isEmpty()) {
            studentDailyAttendanceRepository.findAttendanceByStudentIdsAndDate(studentIds, date)
                    .forEach(record -> attendanceByStudentId.put(record.getStudentId(), record));
        }

        long present = 0L;
        long absent = 0L;
        long late = 0L;
        List<Map<String, Object>> rows = students.stream().map(student -> {
            StudentDailyAttendance record = attendanceByStudentId.get(student.getId());
            String status = "NOT_MARKED";
            if (record != null && record.getAttendanceType() != null) {
                if (record.getAttendanceType().isPresentMark()) {
                    status = "PRESENT";
                } else if (record.getAttendanceType().isLateMark()) {
                    status = "LATE";
                } else if (record.getAttendanceType().isAbsenceMark()) {
                    status = "ABSENT";
                }
            }

            Map<String, Object> row = new HashMap<>();
            row.put("rollNumber", student.getRollNo());
            row.put("name", fullName(student));
            row.put("status", status);
            return row;
        }).toList();

        for (Map<String, Object> row : rows) {
            String status = (String) row.get("status");
            if ("PRESENT".equals(status)) {
                present++;
            } else if ("ABSENT".equals(status)) {
                absent++;
            } else if ("LATE".equals(status)) {
                late++;
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("date", date);
        data.put("teacherName", fullName(staff));
        data.put("className", section.getAcademicClass().getName());
        data.put("sectionName", section.getSectionName());
        data.put("rows", rows);
        data.put("presentCount", present);
        data.put("absentCount", absent);
        data.put("lateCount", late);
        data.put("totalCount", rows.size());

        return pdfGenerationService.generatePdfFromHtml("teacher/attendance-sheet", data);
    }

    private String fullName(Student student) {
        String first = student.getUserProfile() != null && student.getUserProfile().getFirstName() != null
                ? student.getUserProfile().getFirstName().trim()
                : "";
        String last = student.getUserProfile() != null && student.getUserProfile().getLastName() != null
                ? student.getUserProfile().getLastName().trim()
                : "";
        return (first + " " + last).trim();
    }

    private String fullName(Staff staff) {
        String first = staff.getUserProfile() != null && staff.getUserProfile().getFirstName() != null
                ? staff.getUserProfile().getFirstName().trim()
                : "";
        String last = staff.getUserProfile() != null && staff.getUserProfile().getLastName() != null
                ? staff.getUserProfile().getLastName().trim()
                : "";
        return (first + " " + last).trim();
    }
}

