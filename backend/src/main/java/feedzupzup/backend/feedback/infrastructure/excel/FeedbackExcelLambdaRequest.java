package feedzupzup.backend.feedback.infrastructure.excel;

import java.util.List;

public record FeedbackExcelLambdaRequest(
        String jobId,
        String organizationUuid,
        List<FeedbackData> feedbackDataList
) {
}
