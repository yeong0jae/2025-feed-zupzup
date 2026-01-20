package feedzupzup.backend.feedback.infrastructure.excel;

import feedzupzup.backend.s3.config.AwsCredentialsProperties;
import feedzupzup.backend.s3.config.S3Properties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;

@Configuration
@EnableConfigurationProperties(LambdaProperties.class)
public class LambdaConfig {

    @Bean
    public LambdaClient lambdaClient(
            final S3Properties s3Properties,
            final AwsCredentialsProperties awsCredentialsProperties
    ) {
        final AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
                awsCredentialsProperties.accessKey(),
                awsCredentialsProperties.secretKey()
        );

        return LambdaClient.builder()
                .region(Region.of(s3Properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();
    }
}
