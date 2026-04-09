package com.project.edusync.em.model.service.serviceImpl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.project.edusync.adm.model.entity.AcademicClass;
import com.project.edusync.adm.model.entity.Subject;
import com.project.edusync.adm.repository.AcademicClassRepository;
import com.project.edusync.adm.repository.SubjectRepository;
import com.project.edusync.common.exception.emException.EdusyncException;
import com.project.edusync.em.model.dto.RequestDTO.PastPaperRequestDTO;
import com.project.edusync.em.model.dto.ResponseDTO.PastPaperResponseDTO;
import com.project.edusync.em.model.entity.PastPaper;
import com.project.edusync.em.model.repository.PastPaperRepository;
import com.project.edusync.em.model.service.PastPaperService;
import com.project.edusync.uis.config.MediaUploadProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.project.edusync.common.security.AuthUtil;
import com.project.edusync.iam.model.entity.User;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.repository.StudentRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PastPaperServiceImpl implements PastPaperService {

    private final PastPaperRepository pastPaperRepository;
    private final AcademicClassRepository academicClassRepository;
    private final SubjectRepository subjectRepository;
    private final MediaUploadProperties mediaUploadProperties;
    private final AuthUtil authUtil;
    private final StudentRepository studentRepository;
    private Cloudinary cloudinary;

    @jakarta.annotation.PostConstruct
    private void initCloudinary() {
        MediaUploadProperties.Cloudinary cfg = mediaUploadProperties.getCloudinary();
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cfg.getCloudName(),
                "api_key", cfg.getApiKey(),
                "api_secret", cfg.getApiSecret()
        ));
    }

    @Override
    public PastPaperResponseDTO uploadPastPaper(PastPaperRequestDTO requestDTO, MultipartFile file) {
        log.info("Uploading past paper: {}", requestDTO.getTitle());

        if (file.isEmpty()) {
            throw new EdusyncException("EM-400", "File cannot be empty", HttpStatus.BAD_REQUEST);
        }

        // 1. Validate and Fetch related entities
        AcademicClass academicClass = academicClassRepository.findById(requestDTO.getClassId())
                .orElseThrow(() -> new EdusyncException("ADM-404", "Class not found", HttpStatus.NOT_FOUND));
        Subject subject = subjectRepository.findActiveById(requestDTO.getSubjectId())
                .orElseThrow(() -> new EdusyncException("ADM-404", "Subject not found", HttpStatus.NOT_FOUND));

        // 2. Upload file to Cloudinary
        String fileUrl;
        String mimeType = file.getContentType();
        int fileSizeKb = (int) (file.getSize() / 1024);
        try {
            MediaUploadProperties.Cloudinary cfg = mediaUploadProperties.getCloudinary();
            String folder = cfg.getFolder() != null ? cfg.getFolder() : "past-papers";
            
            // Clean filename by stripping extension to prevent double extensions in Cloudinary
            String originalName = file.getOriginalFilename();
            String nameWithoutExtension = originalName != null && originalName.contains(".") 
                ? originalName.substring(0, originalName.lastIndexOf('.')) 
                : (originalName != null ? originalName : "file");

            String publicId = folder + "/" + UUID.randomUUID() + "_" + nameWithoutExtension;
            
            var uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", "auto",
                    "flags", "attachment" // Suggest download instead of browser preview if supported
            ));
            fileUrl = (String) uploadResult.get("secure_url");
            mimeType = (String) uploadResult.getOrDefault("resource_type", mimeType);
            fileSizeKb = (int) (((Number) uploadResult.getOrDefault("bytes", file.getSize())).longValue() / 1024);
        } catch (Exception e) {
            log.error("Cloudinary upload failed", e);
            throw new EdusyncException("EM-500", "Failed to upload file to Cloudinary", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        // 3. Save metadata to DB
        PastPaper pastPaper = new PastPaper();
        pastPaper.setTitle(requestDTO.getTitle());
        pastPaper.setAcademicClass(academicClass);
        pastPaper.setSubject(subject);
        pastPaper.setExamYear(requestDTO.getExamYear());
        pastPaper.setExamType(requestDTO.getExamType());
        pastPaper.setFileUrl(fileUrl);
        pastPaper.setFileMimeType(mimeType);
        pastPaper.setFileSizeKb(fileSizeKb);

        PastPaper savedPaper = pastPaperRepository.save(pastPaper);
        log.info("Past paper uploaded successfully with UUID: {}", savedPaper.getUuid());

        return toResponseDTO(savedPaper);
    }

    @Override
    @Transactional(readOnly = true)
    public PastPaperResponseDTO getPastPaperByUuid(UUID uuid) {
        log.info("Fetching past paper UUID: {}", uuid);
        User currentUser = authUtil.getCurrentUser();
        PastPaper paper;

        if (isStudent(currentUser)) {
            UUID studentClassUuid = resolveStudentClassUuid(currentUser);
            paper = pastPaperRepository.findByUuidAndAcademicClass_Uuid(uuid, studentClassUuid)
                    .orElseThrow(() -> new EdusyncException("EM-404", "Past paper not found", HttpStatus.NOT_FOUND));
        } else {
            paper = pastPaperRepository.findByUuid(uuid)
                    .orElseThrow(() -> new EdusyncException("EM-404", "Past paper not found", HttpStatus.NOT_FOUND));
        }

        return toResponseDTO(paper);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PastPaperResponseDTO> getAllPastPapers(UUID classId, UUID subjectId, Integer year) {
        log.info("Fetching past papers with filters - Class: {}, Subject: {}, Year: {}", classId, subjectId, year);
        User currentUser = authUtil.getCurrentUser();
        if (isStudent(currentUser)) {
            UUID studentClassUuid = resolveStudentClassUuid(currentUser);
            if (classId != null && !classId.equals(studentClassUuid)) {
                log.info("Ignoring requested classId={} for student userId={} and enforcing classId={}",
                        classId, currentUser.getId(), studentClassUuid);
            }
            classId = studentClassUuid;
        }

        return pastPaperRepository.findAllByFilters(classId, subjectId, year)
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    private boolean isStudent(User currentUser) {
        if (currentUser.getRoles() == null) {
            return false;
        }
        return currentUser.getRoles().stream()
                .map(role -> role.getName())
                .filter(name -> name != null)
                .anyMatch("ROLE_STUDENT"::equals);
    }

    private UUID resolveStudentClassUuid(User currentUser) {
        Student student = studentRepository.findByUserProfile_User_Id(currentUser.getId())
                .orElseThrow(() -> new EdusyncException("EM-403", "Student record not found", HttpStatus.FORBIDDEN));

        if (student.getSection() == null || student.getSection().getAcademicClass() == null) {
            throw new EdusyncException("EM-403", "Student class mapping not found", HttpStatus.FORBIDDEN);
        }
        return student.getSection().getAcademicClass().getUuid();
    }

    @Override
    public void deletePastPaper(UUID uuid) {
        log.info("Deleting past paper UUID: {}", uuid);
        PastPaper paper = pastPaperRepository.findByUuid(uuid)
                .orElseThrow(() -> new EdusyncException("EM-404", "Past paper not found", HttpStatus.NOT_FOUND));
        try {
            String fileUrl = paper.getFileUrl();
            if (fileUrl != null && fileUrl.contains("cloudinary.com")) {
                String[] parts = fileUrl.split("/");
                String publicId = parts[parts.length - 2] + "/" + parts[parts.length - 1].split("\\.")[0];
                cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            }
        } catch (Exception e) {
            log.warn("Failed to delete file from Cloudinary: {}", e.getMessage());
        }
        pastPaperRepository.delete(paper);
    }

    // --- Helper Methods ---


    private PastPaperResponseDTO toResponseDTO(PastPaper entity) {
        return PastPaperResponseDTO.builder()
                .uuid(entity.getUuid())
                .classId(entity.getAcademicClass().getUuid())
                .className(entity.getAcademicClass().getName())
                .subjectId(entity.getSubject().getUuid())
                .subjectName(entity.getSubject().getName())
                .title(entity.getTitle())
                .examYear(entity.getExamYear())
                .examType(entity.getExamType())
                .fileUrl(entity.getFileUrl())
                .fileMimeType(entity.getFileMimeType())
                .fileSizeKb(entity.getFileSizeKb())
                .uploadedAt(entity.getCreatedAt()) // Mapped from remapped field in entity
                .uploadedBy(entity.getCreatedBy())
                .build();
    }
}