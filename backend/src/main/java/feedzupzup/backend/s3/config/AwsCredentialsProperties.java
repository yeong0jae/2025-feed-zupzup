package feedzupzup.backend.s3.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws.credentials")
public record AwsCredentialsProperties(
        String accessKey,
        String secretKey
) {

}
