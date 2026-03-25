package feedzupzup.backend.feedback.infrastructure.excel;

import feedzupzup.backend.feedback.domain.Feedback;
import feedzupzup.backend.s3.service.S3DownloadService;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class FeedbackImageProducer {

    private final S3DownloadService s3DownloadService;
    private final BlockingQueue<FeedbackWithImage> queue;

    public void produceImages(final List<Feedback> feedbacks) {
        try (final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (final Feedback feedback : feedbacks) {
                executor.submit(() -> {
                    final ImageDownloadResult result = downloadImage(feedback);
                    enqueue(feedback, result);
                });
            }
        }
        notifyFinished();
    }

    private void enqueue(final Feedback feedback, final ImageDownloadResult imageResult) {
        try {
            queue.put(new FeedbackWithImage(feedback, imageResult));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException("Queue 추가 실패", e);
        }
    }

    private ImageDownloadResult downloadImage(final Feedback feedback) {
        if (feedback.getImageUrl() == null) {
            return ImageDownloadResult.noImage();
        }

        try {
            final byte[] imageData = s3DownloadService.downloadFile(feedback.getImageUrl().getValue());
            return ImageDownloadResult.success(imageData);
        } catch (Exception e) {
            log.error("이미지 다운로드 실패: {}", feedback.getImageUrl(), e);
            return ImageDownloadResult.failed();
        }
    }

    private void notifyFinished() {
        try {
            queue.put(FeedbackWithImage.POISON_PILL);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException("종료 신호 전송 실패", e);
        }
    }
}
