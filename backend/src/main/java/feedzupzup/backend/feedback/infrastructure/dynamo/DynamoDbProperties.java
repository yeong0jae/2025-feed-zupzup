package feedzupzup.backend.feedback.infrastructure.dynamo;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "dynamodb")
public record DynamoDbProperties(
        String feedbackDownloadJobTableName
) {
}
