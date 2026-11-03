package com.campuscare.backend.service;

import com.campuscare.backend.dto.EvidenceRequest;
import com.campuscare.backend.dto.IssueEvidenceResponse;
import com.campuscare.backend.entity.Issue;
import com.campuscare.backend.entity.IssueEvidence;
import com.campuscare.backend.entity.UserRole;
import com.campuscare.backend.exception.BadRequestException;
import com.campuscare.backend.exception.ResourceNotFoundException;
import com.campuscare.backend.exception.StorageException;
import com.campuscare.backend.exception.UnauthorizedException;
import com.campuscare.backend.repository.IssueAssignmentRepository;
import com.campuscare.backend.repository.IssueEvidenceRepository;
import com.campuscare.backend.repository.IssueRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@Transactional
public class IssueEvidenceService {

    private static final long MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024; // 10 MB

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/gif",
            "video/mp4",
            "video/quicktime",
            "video/webm",
            "video/x-msvideo",
            "video/3gpp"
    );

    private final IssueEvidenceRepository issueEvidenceRepository;
    private final IssueRepository issueRepository;
    private final IssueAssignmentRepository issueAssignmentRepository;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Value("${supabase.url:}")
    private String supabaseUrl;

    @Value("${supabase.service-role-key:}")
    private String supabaseServiceKey;

    @Value("${supabase.storage.bucket:issue-evidence}")
    private String storageBucket = "issue-evidence";

    public IssueEvidenceService(
            IssueEvidenceRepository issueEvidenceRepository,
            IssueRepository issueRepository,
            IssueAssignmentRepository issueAssignmentRepository
    ) {
        this.issueEvidenceRepository = issueEvidenceRepository;
        this.issueRepository = issueRepository;
        this.issueAssignmentRepository = issueAssignmentRepository;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public void setSupabaseConfig(String supabaseUrl, String supabaseServiceKey) {
        this.supabaseUrl = supabaseUrl;
        this.supabaseServiceKey = supabaseServiceKey;
    }

    public IssueEvidenceResponse uploadEvidence(Long issueId, MultipartFile file, UUID currentUserId, UserRole role) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded evidence file cannot be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("File size exceeds 10 MB limit (current: "
                    + (file.getSize() / (1024 * 1024)) + " MB)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Unsupported file type: " + contentType
                    + ". Allowed types: JPG, JPEG, PNG, WEBP, GIF, MP4, MOV, WEBM");
        }

        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with ID: " + issueId));

        validateIssueAccess(issue, currentUserId, role, true);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "evidence";
        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex);
        }

        String storagePath = "issue-" + issueId + "/" + UUID.randomUUID() + extension;

        // Upload to Supabase Storage
        uploadToSupabaseStorage(storagePath, file);

        // Record metadata in issue_evidence
        IssueEvidence evidence = new IssueEvidence();
        evidence.setIssue(issue);
        evidence.setFileUrl(storagePath);
        evidence.setFileName(originalFilename);
        evidence.setFileType(contentType);
        evidence.setFileSize(file.getSize());
        evidence.setUploadedAt(OffsetDateTime.now());

        IssueEvidence saved = issueEvidenceRepository.save(evidence);

        return mapToResponse(saved);
    }

    public IssueEvidenceResponse recordEvidenceMetadata(Long issueId, EvidenceRequest req, UUID currentUserId, UserRole role) {
        throw new BadRequestException("Metadata-only evidence creation is not permitted. Evidence files must be uploaded through the secure storage service.");
    }

    @Transactional(readOnly = true)
    public List<IssueEvidenceResponse> getEvidenceByIssue(Long issueId, UUID currentUserId, UserRole role) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new ResourceNotFoundException("Issue not found with ID: " + issueId));

        validateIssueAccess(issue, currentUserId, role, false);

        return issueEvidenceRepository.findByIssueIdOrderByUploadedAtDesc(issueId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public IssueEvidenceResponse getEvidenceById(Long id, UUID currentUserId, UserRole role) {
        IssueEvidence evidence = issueEvidenceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence not found with ID: " + id));

        validateIssueAccess(evidence.getIssue(), currentUserId, role, false);

        return mapToResponse(evidence);
    }

    private void validateIssueAccess(Issue issue, UUID currentUserId, UserRole role, boolean isWrite) {
        if (role == UserRole.ADMIN) {
            return;
        }

        if (role == UserRole.STUDENT || role == UserRole.PROFESSOR) {
            if (!issue.getReportedBy().equals(currentUserId)) {
                throw new UnauthorizedException("You are not authorized to access evidence for this issue");
            }
            return;
        }

        if (role == UserRole.MAINTENANCE) {
            boolean isAssigned = issueAssignmentRepository.findByStaffId(currentUserId).stream()
                    .anyMatch(a -> a.getIssue().getId().equals(issue.getId()));
            if (!isAssigned) {
                throw new UnauthorizedException("Maintenance staff can only access evidence for issues assigned to them");
            }
        }
    }

    private void uploadToSupabaseStorage(String path, MultipartFile file) {
        if (supabaseUrl == null || supabaseUrl.isBlank()) {
            throw new StorageException("Cannot upload evidence: Supabase Storage URL is not configured");
        }
        String token = getSupabaseServiceRoleKey();

        try {
            String uploadUrl = supabaseUrl.replaceAll("/+$", "")
                    + "/storage/v1/object/" + storageBucket + "/" + path;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl))
                    .header("Authorization", "Bearer " + token)
                    .header("apikey", token)
                    .header("Content-Type", file.getContentType())
                    .header("x-upsert", "true")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(file.getBytes()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new StorageException("Supabase Storage error (" + response.statusCode() + "): " + response.body());
            }
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StorageException("Failed to upload evidence to storage: " + e.getMessage(), e);
        }
    }

    public String generateSecureUrl(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return "";
        }

        // Validate that storagePath is a legitimate object path within the private issue-evidence bucket
        // Do not return arbitrary stored HTTP/HTTPS URLs directly; reject invalid historical URLs with a controlled error
        if (storagePath.startsWith("http://") || storagePath.startsWith("https://")
                || storagePath.contains("..") || storagePath.startsWith("/")) {
            throw new StorageException("Invalid evidence storage path: '" + storagePath + "'. Only secure relative storage object paths are permitted.");
        }

        // Expected format: issue-{issueId}/{filename}
        if (!storagePath.matches("^issue-\\d+/[a-zA-Z0-9._-]+$")) {
            throw new StorageException("Invalid evidence storage path format: '" + storagePath + "'. Expected format: issue-{id}/{filename}");
        }

        if (supabaseUrl == null || supabaseUrl.isBlank()) {
            throw new StorageException("Cannot generate signed URL: Supabase Storage URL is not configured");
        }

        String token = getSupabaseServiceRoleKey();

        try {
            String signEndpoint = supabaseUrl.replaceAll("/+$", "")
                    + "/storage/v1/object/sign/" + storageBucket + "/" + storagePath;

            String bodyJson = "{\"expiresIn\": 3600}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(signEndpoint))
                    .header("Authorization", "Bearer " + token)
                    .header("apikey", token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.has("signedURL")) {
                    String signedPath = root.get("signedURL").asText();
                    return supabaseUrl.replaceAll("/+$", "") + (signedPath.startsWith("/") ? signedPath : "/" + signedPath);
                }
            }
            throw new StorageException("Failed to generate secure signed URL for evidence (Supabase storage returned status " + response.statusCode() + ")");
        } catch (StorageException se) {
            throw se;
        } catch (Exception e) {
            throw new StorageException("Failed to generate secure signed URL for evidence: " + e.getMessage(), e);
        }
    }

    private String getSupabaseServiceRoleKey() {
        if (supabaseServiceKey != null && !supabaseServiceKey.isBlank()) {
            return supabaseServiceKey.trim();
        }
        throw new StorageException("Server-side storage operation requires SUPABASE_SERVICE_ROLE_KEY to be configured");
    }

    public IssueEvidenceResponse mapToResponse(IssueEvidence e) {
        IssueEvidenceResponse resp = new IssueEvidenceResponse();
        resp.setId(e.getId());
        resp.setIssueId(e.getIssue().getId());
        resp.setFileUrl(generateSecureUrl(e.getFileUrl()));
        resp.setFileName(e.getFileName());
        resp.setName(e.getFileName());
        resp.setFileType(e.getFileType());

        boolean isVideo = e.getFileType() != null && e.getFileType().toLowerCase().contains("video");
        resp.setType(isVideo ? "video" : "image");

        resp.setFileSize(e.getFileSize());
        resp.setSize(formatFileSize(e.getFileSize()));
        resp.setUploadedAt(e.getUploadedAt());
        return resp;
    }

    private String formatFileSize(Long bytes) {
        if (bytes == null || bytes <= 0) return "0 KB";
        if (bytes < 1024 * 1024) {
            return Math.max(1, Math.round(bytes / 1024.0)) + " KB";
        }
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private Long parseSizeStringToBytes(String sizeStr) {
        if (sizeStr == null || sizeStr.isBlank()) return 1024L * 1024L; // Default 1MB
        try {
            String clean = sizeStr.trim().toUpperCase();
            if (clean.endsWith("MB")) {
                double mb = Double.parseDouble(clean.replace("MB", "").trim());
                return (long) (mb * 1024 * 1024);
            }
            if (clean.endsWith("KB")) {
                double kb = Double.parseDouble(clean.replace("KB", "").trim());
                return (long) (kb * 1024);
            }
            return Long.parseLong(clean);
        } catch (Exception ex) {
            return 1024L * 1024L;
        }
    }
}
