package feedzupzup.backend.feedback.infrastructure.excel;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lambda")
public record LambdaProperties(
        String feedbackExcelFunctionName
) {
}
