package feedzupzup.backend.feedback.infrastructure.excel;

import feedzupzup.backend.feedback.domain.Feedback;
import feedzupzup.backend.s3.service.S3DownloadService;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class FeedbackImageProducer {

    private final S3DownloadService s3DownloadService;
    private final BlockingQueue<FeedbackWithImage> queue;

    public CompletableFuture<Void> produceImages(final List<Feedback> feedbacks) {
        final List<CompletableFuture<Void>> downloadJobs = feedbacks.stream()
                .map(this::downloadJob)
                .toList();

        return CompletableFuture.allOf(downloadJobs.toArray(CompletableFuture[]::new))
                .whenComplete((result, ex) -> notifyFinished());
    }

    private CompletableFuture<Void> downloadJob(final Feedback feedback) {
        if (feedback.getImageUrl() == null) {
            enqueue(feedback, ImageDownloadResult.noImage());
            return CompletableFuture.completedFuture(null);
        }

        return s3DownloadService.downloadFileAsync(feedback.getImageUrl().getValue())
                .thenApply(ImageDownloadResult::success)
                .exceptionally(ex -> {
                    log.error("이미지 다운로드 실패: {}", feedback.getImageUrl(), ex);
                    return ImageDownloadResult.failed();
                })
                .thenAccept(imageResult -> enqueue(feedback, imageResult));
    }

    private void enqueue(final Feedback feedback, final ImageDownloadResult imageResult) {
        try {
            queue.put(new FeedbackWithImage(feedback, imageResult));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException("Queue 추가 실패", e);
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
