package feedzupzup.backend.feedback.infrastructure.pdf;

import feedzupzup.backend.feedback.domain.Feedback;
import feedzupzup.backend.feedback.domain.FeedbackDownloadJobStore;
import feedzupzup.backend.feedback.domain.FeedbackPdfDownloader;
import feedzupzup.backend.feedback.infrastructure.download.FeedbackImageProducer;
import feedzupzup.backend.feedback.infrastructure.download.FeedbackWithImage;
import feedzupzup.backend.global.exception.InfrastructureException.PdfExportException;
import feedzupzup.backend.organization.domain.Organization;
import feedzupzup.backend.s3.service.S3DownloadService;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FeedbackPdfBoxDownloader implements FeedbackPdfDownloader {

    private static final int QUEUE_CAPACITY = 15;
    private static final int PRODUCER_THREAD = 1;
    private static final int DOWNLOAD_THREADS = 10;

    private final S3DownloadService s3DownloadService;
    private final FeedbackDownloadJobStore feedbackDownloadJobStore;

    @Override
    public void download(
            final Organization organization,
            final List<Feedback> feedbacks,
            final OutputStream outputStream,
            final String jobId
    ) {
        log.info("피드백 PDF 다운로드 시작: 조직={}, 피드백 개수={}", organization.getName().getValue(), feedbacks.size());

        final ExecutorService executor = Executors.newFixedThreadPool(PRODUCER_THREAD + DOWNLOAD_THREADS);

        // 모든 스트림(이미지 포함)이 디스크 임시 파일로 자동 저장
        try (final PDDocument document = new PDDocument(IOUtils.createTempFileOnlyStreamCache())) {
            // 한글 폰트 로드
            final PDType0Font font = PDType0Font.load(
                    document,
                    getClass().getResourceAsStream("/fonts/NanumGothic.ttf")
            );

            createPdfPages(document, font, feedbacks, executor, jobId);

            document.save(outputStream);
            outputStream.flush();

            log.info("피드백 PDF 다운로드 완료");
        } catch (IOException e) {
            throw new PdfExportException("PDF 파일 생성 중 오류가 발생했습니다.", e);
        } finally {
            shutdownExecutor(executor);
        }
    }

    private void createPdfPages(
            final PDDocument document,
            final PDType0Font font,
            final List<Feedback> feedbacks,
            final ExecutorService executor,
            final String jobId
    ) {
        final BlockingQueue<FeedbackWithImage> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        final FeedbackImageProducer producer = new FeedbackImageProducer(s3DownloadService, queue, executor);
        final CompletableFuture<Void> produceJob = producer.produceImages(feedbacks);

        final PdfLayoutConfig layout = PdfLayoutConfig.createDefault();
        final FeedbackPageWriter consumer = new FeedbackPageWriter(
                document,
                font,
                queue,
                layout,
                feedbackDownloadJobStore,
                jobId
        );
        consumer.consumeToPdf(feedbacks.size());

        try {
            produceJob.join();
        } catch (CompletionException e) {
            log.error("이미지 다운로드 작업 중 오류 발생", e);
            throw new PdfExportException("이미지 다운로드 중 오류가 발생했습니다.", e);
        }
    }

    private void shutdownExecutor(final ExecutorService executor) {
        try {
            executor.shutdown();
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.error("Executor 강제 종료 실패");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
