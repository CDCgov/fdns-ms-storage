package gov.cdc.foundation.helper;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

@Component
public class ResourceHelper {

	private static AmazonS3 client = null;

	private static final Logger logger = Logger.getLogger(ResourceHelper.class);

	private static String accessKey;
	private static String secretKey;
	private static String host;

	private ResourceHelper(@Value("${repo.accessKey}") String accessKey, @Value("${repo.secretKey}") String secretKey, @Value("${repo.host}") String host) {
		logger.debug("Creating resource helper...");
		ResourceHelper.host = host;
		ResourceHelper.accessKey = accessKey;
		ResourceHelper.secretKey = secretKey;
	}

	public static AmazonS3 getS3Client() {
		if (client == null) {
			logger.debug("Connecting to S3...");

			BasicAWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
			ClientConfiguration clientConfiguration = new ClientConfiguration();
			clientConfiguration.setSignerOverride("AWSS3V4SignerType");
			
			client = AmazonS3ClientBuilder
				.standard()
				.withClientConfiguration(clientConfiguration)
				.withCredentials(new AWSStaticCredentialsProvider(credentials))
				.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(host, Regions.US_EAST_1.toString()))
				.enablePathStyleAccess()
			    .disableChunkedEncoding()
				.build();
		}
		return client;
	}

}
