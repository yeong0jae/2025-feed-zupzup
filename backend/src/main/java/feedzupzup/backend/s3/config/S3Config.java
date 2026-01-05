package feedzupzup.backend.s3.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@EnableConfigurationProperties({S3Properties.class, AwsCredentialsProperties.class})
public class S3Config {

    @Bean
    public S3Presigner s3Presigner(final S3Properties s3Properties, final AwsCredentialsProperties awsCredentialsProperties) {
        final AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
                awsCredentialsProperties.accessKey(),
                awsCredentialsProperties.secretKey()
        );

        return S3Presigner.builder()
                .region(Region.of(s3Properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();
    }

    @Bean
    public S3Client s3Client(final S3Properties s3Properties, final AwsCredentialsProperties awsCredentialsProperties) {
        final AwsBasicCredentials awsCredentials = AwsBasicCredentials.create(
                awsCredentialsProperties.accessKey(),
                awsCredentialsProperties.secretKey()
        );

        return S3Client.builder()
                .region(Region.of(s3Properties.region()))
                .credentialsProvider(StaticCredentialsProvider.create(awsCredentials))
                .build();
    }
}
