package feedzupzup.backend.feedback.application;

import feedzupzup.backend.feedback.domain.Feedback;
import feedzupzup.backend.feedback.domain.FeedbackDownloadJobStore;
import feedzupzup.backend.feedback.domain.FeedbackPdfDownloader;
import feedzupzup.backend.feedback.domain.FeedbackRepository;
import feedzupzup.backend.feedback.domain.vo.FeedbackDownloadJob;
import feedzupzup.backend.global.exception.ResourceException.ResourceNotFoundException;
import feedzupzup.backend.organization.domain.Organization;
import feedzupzup.backend.organization.domain.OrganizationRepository;
import feedzupzup.backend.s3.service.S3UploadService;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FeedbackFileDownloadService {

    private final FeedbackDownloadJobStore feedbackDownloadJobStore;
    private final OrganizationRepository organizationRepository;
    private final FeedbackRepository feedBackRepository;
    private final FeedbackPdfDownloader feedbackPdfDownloader;
    private final S3UploadService s3UploadService;

    @Async
    public void createAndUploadFileAsync(final String jobId, final UUID organizationUuid) {
        final FeedbackDownloadJob job = feedbackDownloadJobStore.getById(jobId);
        if (job == null) {
            log.error("작업을 찾을 수 없습니다. jobId={}", jobId);
            return;
        }

        Path tempFile = null;
        try {
            final Organization organization = organizationRepository.findByUuid(organizationUuid)
                    .orElseThrow(() -> new ResourceNotFoundException("해당 ID(id = " + organizationUuid + ")인 단체를 찾을 수 없습니다."));

            final List<Feedback> feedbacks = feedBackRepository.findByOrganization(organization);

            tempFile = Files.createTempFile("feedback_", ".pdf");
            try (final FileOutputStream fileOutputStream = new FileOutputStream(tempFile.toFile())) {
                feedbackPdfDownloader.download(organization, feedbacks, fileOutputStream, jobId);
            }

            try (final FileInputStream fileInputStream = new FileInputStream(tempFile.toFile())) {
                final String s3Url = s3UploadService.uploadFileStream(
                        "pdf",
                        "feedback_file",
                        jobId,
                        fileInputStream,
                        Files.size(tempFile)
                );
                job.completeWithUrl(s3Url);
            }

        } catch (Exception e) {
            log.error("피드백 PDF 파일 생성 중 오류 발생. jobId={}", jobId, e);
            job.fail("파일 생성 중 오류가 발생했습니다: " + e.getMessage());
        } finally {
            deleteTempFile(tempFile);
        }
    }

    private void deleteTempFile(final Path tempFile) {
        if (tempFile != null) {
            try {
                Files.deleteIfExists(tempFile);
                log.debug("임시 파일 삭제 완료: {}", tempFile);
            } catch (IOException e) {
                log.warn("임시 파일 삭제 실패: {}", tempFile, e);
            }
        }
    }
}
