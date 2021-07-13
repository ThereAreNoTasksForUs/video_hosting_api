package com.therearenotasksforus.videohostingapi.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class AmazonConfig {

    @Bean
    public AmazonS3 s3() {
        Map<String, String> env = System.getenv();
        AWSCredentials awsCredentials = new BasicAWSCredentials(
                env.get("AMAZON_ACCESS_KEY"),
                env.get("AMAZON_SECRET_KEY")
        );
        return AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion("eu-central-1")
                .build();
    }
}
