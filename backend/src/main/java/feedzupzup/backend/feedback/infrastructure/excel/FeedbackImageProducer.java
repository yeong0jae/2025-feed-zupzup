package feedzupzup.backend.feedback.infrastructure.excel;

import feedzupzup.backend.feedback.domain.Feedback;
import feedzupzup.backend.s3.service.S3DownloadService;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
public class FeedbackImageProducer {

    private static final int CONCURRENCY = 15;

    private final S3DownloadService s3DownloadService;
    private final BlockingQueue<FeedbackWithImage> queue;

    public void produceImages(final List<Feedback> feedbacks) {
        try {
            Flux.fromIterable(feedbacks)
                    .flatMap(this::downloadJob, CONCURRENCY)
                    .doFinally(signal -> notifyFinished())
                    .blockLast();
        } catch (Exception e) {
            log.error("이미지 다운로드 작업 중 오류 발생", e);
            throw e;
        }
    }

    private Mono<Void> downloadJob(final Feedback feedback) {
        if (feedback.getImageUrl() == null) {
            enqueue(feedback, ImageDownloadResult.noImage());
            return Mono.empty();
        }

        return s3DownloadService.downloadFileReactive(feedback.getImageUrl().getValue())
                .map(ImageDownloadResult::success)
                .onErrorResume(ex -> {
                    log.error("이미지 다운로드 실패: {}", feedback.getImageUrl(), ex);
                    return Mono.just(ImageDownloadResult.failed());
                })
                .doOnNext(imageResult -> enqueue(feedback, imageResult))
                .then();
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
