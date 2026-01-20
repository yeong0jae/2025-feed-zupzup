package feedzupzup.backend.feedback.infrastructure.excel;

import feedzupzup.backend.feedback.domain.Feedback;
import java.time.LocalDateTime;

public record FeedbackData(
        String content,
        String category,
        String imageUrl,
        int likeCount,
        boolean isSecret,
        String status,
        String comment,
        String userName,
        LocalDateTime postedAt
) {

    public static FeedbackData from(final Feedback feedback) {
        return new FeedbackData(
                feedback.getContent().getValue(),
                feedback.getOrganizationCategory().getCategory().getKoreanName(),
                feedback.getImageUrl() == null ? null : feedback.getImageUrl().getValue(),
                feedback.getLikeCountValue(),
                feedback.isSecret(),
                feedback.getStatus().name(),
                feedback.getComment() == null ? "" : feedback.getComment().getValue(),
                feedback.getUserName().getValue(),
                feedback.getPostedAt().getValue()
        );
    }
}
