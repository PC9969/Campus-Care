package com.campuscare.backend.service;

import com.campuscare.backend.dto.EvidenceRequest;
import com.campuscare.backend.dto.IssueEvidenceResponse;
import com.campuscare.backend.entity.*;
import com.campuscare.backend.exception.BadRequestException;
import com.campuscare.backend.exception.StorageException;
import com.campuscare.backend.exception.UnauthorizedException;
import com.campuscare.backend.repository.IssueAssignmentRepository;
import com.campuscare.backend.repository.IssueEvidenceRepository;
import com.campuscare.backend.repository.IssueRepository;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IssueEvidenceServiceTest {

    @Mock
    private IssueEvidenceRepository issueEvidenceRepository;

    @Mock
    private IssueRepository issueRepository;

    @Mock
    private IssueAssignmentRepository issueAssignmentRepository;

    private IssueEvidenceService issueEvidenceService;
    private HttpServer mockStorageServer;
    private String mockStorageUrl;

    private UUID studentId;
    private UUID otherStudentId;
    private UUID staffId;
    private Issue testIssue;

    @BeforeEach
    void setUp() {
        issueEvidenceService = new IssueEvidenceService(
                issueEvidenceRepository,
                issueRepository,
                issueAssignmentRepository
        );

        studentId = UUID.randomUUID();
        otherStudentId = UUID.randomUUID();
        staffId = UUID.randomUUID();

        testIssue = new Issue();
        testIssue.setId(10L);
        testIssue.setTitle("Water leak in lab");
        testIssue.setReportedBy(studentId);
    }

    @AfterEach
    void tearDown() {
        if (mockStorageServer != null) {
            mockStorageServer.stop(0);
        }
    }

    @Test
    void testRecordEvidenceMetadataWithoutUploadIsRejected() {
        EvidenceRequest req = new EvidenceRequest();
        req.setName("injected.jpg");
        req.setUrl("https://attacker.com/malicious.jpg");

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                issueEvidenceService.recordEvidenceMetadata(10L, req, studentId, UserRole.STUDENT)
        );
        assertTrue(ex.getMessage().contains("not permitted"));
        verifyNoInteractions(issueEvidenceRepository);
    }

    @Test
    void testMissingStorageConfigurationThrowsStorageExceptionOnUpload() {
        issueEvidenceService.setSupabaseConfig("", "");
        when(issueRepository.findById(10L)).thenReturn(Optional.of(testIssue));

        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "test image bytes".getBytes());

        StorageException ex = assertThrows(StorageException.class, () ->
                issueEvidenceService.uploadEvidence(10L, file, studentId, UserRole.STUDENT)
        );
        assertTrue(ex.getMessage().contains("configured"));
        verifyNoInteractions(issueEvidenceRepository);
    }

    @Test
    void testMissingServiceRoleKeyThrowsStorageException() {
        issueEvidenceService.setSupabaseConfig("http://127.0.0.1:54321", "");

        StorageException ex = assertThrows(StorageException.class, () ->
                issueEvidenceService.generateSecureUrl("issue-10/photo.jpg")
        );
        assertTrue(ex.getMessage().contains("SUPABASE_SERVICE_ROLE_KEY"));
    }

    @Test
    void testUploadFailurePreventsMetadataCreation() throws Exception {
        startMockStorageServer(500, "{\"error\":\"Upload server error\"}");
        issueEvidenceService.setSupabaseConfig(mockStorageUrl, "service-role-key-123");
        when(issueRepository.findById(10L)).thenReturn(Optional.of(testIssue));

        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "test image bytes".getBytes());

        StorageException ex = assertThrows(StorageException.class, () ->
                issueEvidenceService.uploadEvidence(10L, file, studentId, UserRole.STUDENT)
        );
        assertTrue(ex.getMessage().contains("Supabase Storage error"));
        verify(issueEvidenceRepository, never()).save(any(IssueEvidence.class));
    }

    @Test
    void testSignedUrlFailsWhenStorageUnconfigured() {
        issueEvidenceService.setSupabaseConfig("", "");

        StorageException ex = assertThrows(StorageException.class, () ->
                issueEvidenceService.generateSecureUrl("issue-10/photo.jpg")
        );
        assertTrue(ex.getMessage().contains("configured"));
    }

    @Test
    void testSignedUrlFailsWhenSupabaseReturnsError() throws Exception {
        startMockStorageServer(500, "{\"error\":\"Internal Server Error\"}");
        issueEvidenceService.setSupabaseConfig(mockStorageUrl, "service-role-key-123");

        StorageException ex = assertThrows(StorageException.class, () ->
                issueEvidenceService.generateSecureUrl("issue-10/photo.jpg")
        );
        assertTrue(ex.getMessage().contains("Failed to generate secure signed URL"));
    }

    @Test
    void testGenerateSecureUrlRejectsArbitraryHttpUrls() {
        StorageException ex = assertThrows(StorageException.class, () ->
                issueEvidenceService.generateSecureUrl("https://malicious.com/injected-photo.jpg")
        );
        assertTrue(ex.getMessage().contains("Invalid evidence storage path"));
    }

    @Test
    void testGenerateSecureUrlRejectsPathTraversal() {
        StorageException ex = assertThrows(StorageException.class, () ->
                issueEvidenceService.generateSecureUrl("issue-10/../../etc/passwd")
        );
        assertTrue(ex.getMessage().contains("Invalid evidence storage path"));
    }

    @Test
    void testGenerateSecureUrlRejectsMalformedFormat() {
        StorageException ex = assertThrows(StorageException.class, () ->
                issueEvidenceService.generateSecureUrl("random_file_without_issue_prefix.jpg")
        );
        assertTrue(ex.getMessage().contains("Invalid evidence storage path format"));
    }

    @Test
    void testSignedUrlSuccess() throws Exception {
        startMockStorageServer(200, "{\"signedURL\":\"/storage/v1/object/sign/issue-evidence/issue-10/photo.jpg?token=abc123xyz\"}");
        issueEvidenceService.setSupabaseConfig(mockStorageUrl, "service-role-key-123");

        String signedUrl = issueEvidenceService.generateSecureUrl("issue-10/photo.jpg");
        assertNotNull(signedUrl);
        assertTrue(signedUrl.contains("/storage/v1/object/sign/issue-evidence/issue-10/photo.jpg?token=abc123xyz"));
    }

    @Test
    void testMaintenanceStaffCannotViewEvidenceForUnassignedIssue() {
        when(issueRepository.findById(10L)).thenReturn(Optional.of(testIssue));
        when(issueAssignmentRepository.findByStaffId(staffId)).thenReturn(Collections.emptyList());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () ->
                issueEvidenceService.getEvidenceByIssue(10L, staffId, UserRole.MAINTENANCE)
        );
        assertTrue(ex.getMessage().contains("assigned"));
    }

    @Test
    void testMaintenanceStaffCannotUploadEvidenceForUnassignedIssue() {
        when(issueRepository.findById(10L)).thenReturn(Optional.of(testIssue));
        when(issueAssignmentRepository.findByStaffId(staffId)).thenReturn(Collections.emptyList());

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "image-content".getBytes());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () ->
                issueEvidenceService.uploadEvidence(10L, file, staffId, UserRole.MAINTENANCE)
        );
        assertTrue(ex.getMessage().contains("assigned"));
    }

    @Test
    void testStudentCannotAccessOtherUsersEvidence() {
        when(issueRepository.findById(10L)).thenReturn(Optional.of(testIssue));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class, () ->
                issueEvidenceService.getEvidenceByIssue(10L, otherStudentId, UserRole.STUDENT)
        );
        assertTrue(ex.getMessage().contains("not authorized"));
    }

    @Test
    void testMaintenanceStaffCanAccessAssignedIssueEvidence() throws Exception {
        startMockStorageServer(200, "{\"signedURL\":\"/storage/v1/object/sign/issue-evidence/issue-10/leak.jpg?token=valid-sig\"}");
        issueEvidenceService.setSupabaseConfig(mockStorageUrl, "service-role-key-123");

        when(issueRepository.findById(10L)).thenReturn(Optional.of(testIssue));

        IssueAssignment assignment = new IssueAssignment();
        assignment.setIssue(testIssue);
        assignment.setStaffId(staffId);
        when(issueAssignmentRepository.findByStaffId(staffId)).thenReturn(List.of(assignment));

        IssueEvidence evidence = new IssueEvidence();
        evidence.setId(1001L);
        evidence.setIssue(testIssue);
        evidence.setFileName("leak.jpg");
        evidence.setFileType("image/jpeg");
        evidence.setFileSize(2048L);
        evidence.setFileUrl("issue-10/leak.jpg");
        evidence.setUploadedAt(OffsetDateTime.now());

        when(issueEvidenceRepository.findByIssueIdOrderByUploadedAtDesc(10L)).thenReturn(List.of(evidence));

        List<IssueEvidenceResponse> result = issueEvidenceService.getEvidenceByIssue(10L, staffId, UserRole.MAINTENANCE);
        assertEquals(1, result.size());
        assertEquals("leak.jpg", result.get(0).getFileName());
        assertTrue(result.get(0).getFileUrl().contains("token=valid-sig"));
    }

    private void startMockStorageServer(int statusCode, String responseJson) throws Exception {
        mockStorageServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        mockStorageServer.createContext("/storage/v1/object/sign/issue-evidence/", exchange -> {
            byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        mockStorageServer.createContext("/storage/v1/object/issue-evidence/", exchange -> {
            byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(statusCode, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        mockStorageServer.start();
        mockStorageUrl = "http://127.0.0.1:" + mockStorageServer.getAddress().getPort();
    }
}
