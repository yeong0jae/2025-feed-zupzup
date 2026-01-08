package feedzupzup.backend.feedback.infrastructure.download;

import feedzupzup.backend.feedback.domain.Feedback;

public record FeedbackWithImage(Feedback feedback, ImageDownloadResult imageResult, boolean isPoisonPill) {

    static final FeedbackWithImage POISON_PILL = new FeedbackWithImage(null, null, true);

    FeedbackWithImage(final Feedback feedback, final ImageDownloadResult imageResult) {
        this(feedback, imageResult, false);
    }
}
