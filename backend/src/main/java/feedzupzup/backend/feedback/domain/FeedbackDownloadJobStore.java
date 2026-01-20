package feedzupzup.backend.feedback.domain;

import feedzupzup.backend.feedback.domain.vo.FeedbackDownloadJob;
import feedzupzup.backend.feedback.infrastructure.dynamo.DynamoDbProperties;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

@Component
public class FeedbackDownloadJobStore {

    private static final String ATTR_JOB_ID = "jobId";
    private static final String ATTR_ORGANIZATION_UUID = "organizationUuid";
    private static final String ATTR_STATUS = "status";
    private static final String ATTR_PROGRESS = "progress";
    private static final String ATTR_DOWNLOAD_URL = "downloadUrl";
    private static final String ATTR_ERROR_MESSAGE = "errorMessage";
    private static final String ATTR_CREATED_AT = "createdAt";
    private static final String ATTR_UPDATED_AT = "updatedAt";

    private final DynamoDbClient dynamoDbClient;
    private final DynamoDbProperties dynamoDbProperties;

    public FeedbackDownloadJobStore(
            final DynamoDbClient dynamoDbClient,
            final DynamoDbProperties dynamoDbProperties
    ) {
        this.dynamoDbClient = dynamoDbClient;
        this.dynamoDbProperties = dynamoDbProperties;
    }

    public FeedbackDownloadJob save(final FeedbackDownloadJob job) {
        final Map<String, AttributeValue> item = new HashMap<>();
        item.put(ATTR_JOB_ID, AttributeValue.builder().s(job.getJobId()).build());
        item.put(ATTR_ORGANIZATION_UUID, AttributeValue.builder().s(job.getOrganizationUuid()).build());
        item.put(ATTR_STATUS, AttributeValue.builder().s(job.getStatus().name()).build());
        item.put(ATTR_PROGRESS, AttributeValue.builder().n(String.valueOf(job.getProgress())).build());
        item.put(ATTR_CREATED_AT, AttributeValue.builder().s(job.getCreatedAt().toString()).build());
        item.put(ATTR_UPDATED_AT, AttributeValue.builder().s(job.getUpdatedAt().toString()).build());

        if (job.getDownloadUrl() != null) {
            item.put(ATTR_DOWNLOAD_URL, AttributeValue.builder().s(job.getDownloadUrl()).build());
        }

        if (job.getErrorMessage() != null) {
            item.put(ATTR_ERROR_MESSAGE, AttributeValue.builder().s(job.getErrorMessage()).build());
        }

        final PutItemRequest request = PutItemRequest.builder()
                .tableName(dynamoDbProperties.feedbackDownloadJobTableName())
                .item(item)
                .build();

        dynamoDbClient.putItem(request);
        return job;
    }

    public FeedbackDownloadJob getById(final String jobId) {
        final Map<String, AttributeValue> key = Map.of(
                ATTR_JOB_ID, AttributeValue.builder().s(jobId).build()
        );

        final GetItemRequest request = GetItemRequest.builder()
                .tableName(dynamoDbProperties.feedbackDownloadJobTableName())
                .key(key)
                .build();

        final GetItemResponse response = dynamoDbClient.getItem(request);
        if (!response.hasItem()) {
            return null;
        }

        return fromItem(response.item());
    }

    private FeedbackDownloadJob fromItem(final Map<String, AttributeValue> item) {
        final String downloadUrl = item.containsKey(ATTR_DOWNLOAD_URL) ? item.get(ATTR_DOWNLOAD_URL).s() : null;
        final String errorMessage = item.containsKey(ATTR_ERROR_MESSAGE) ? item.get(ATTR_ERROR_MESSAGE).s() : null;

        return FeedbackDownloadJob.of(
                item.get(ATTR_JOB_ID).s(),
                item.get(ATTR_ORGANIZATION_UUID).s(),
                FeedbackDownloadJob.DownloadStatus.valueOf(item.get(ATTR_STATUS).s()),
                Integer.parseInt(item.get(ATTR_PROGRESS).n()),
                downloadUrl,
                errorMessage,
                LocalDateTime.parse(item.get(ATTR_CREATED_AT).s()),
                LocalDateTime.parse(item.get(ATTR_UPDATED_AT).s())
        );
    }
}
