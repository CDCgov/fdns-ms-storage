package gov.cdc.foundation.helper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.Owner;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

@Component
public class TemplatingHelper {

	private static final Logger logger = Logger.getLogger(TemplatingHelper.class);

	private static DateFormat df = null;

	private static String dateFormat;

	private TemplatingHelper(@Value("${date.format}") String dateFormat) {
		logger.debug("Creating templating helper...");
		TemplatingHelper.dateFormat = dateFormat;
	}

	public static JSONObject process(Bucket bucket) throws JSONException {
		JSONObject obj = new JSONObject();
		if (bucket.getCreationDate() != null)
			obj.put("created", getDateFormatter().format(bucket.getCreationDate()));
		if (bucket.getOwner() != null)
			obj.put("owner", process(bucket.getOwner()));
		obj.put("name", bucket.getName());
		return obj;
	}

	public static JSONObject process(Owner owner) throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("id", owner.getId());
		obj.put("displayName", owner.getDisplayName());
		return obj;
	}

	public static JSONObject process(S3ObjectSummary summary) throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("size", summary.getSize());
		obj.put("drawer", summary.getBucketName());
		obj.put("etag", summary.getETag());
		obj.put("id", summary.getKey());
		obj.put("modified", getDateFormatter().format(summary.getLastModified()));
		if (summary.getOwner() != null)
			obj.put("owner", process(summary.getOwner()));
		obj.put("class", summary.getStorageClass());
		return obj;
	}

	public static Object process(S3Object object) throws JSONException {
		JSONObject obj = new JSONObject();
		obj.put("size", object.getObjectMetadata().getContentLength());
		obj.put("disposition", object.getObjectMetadata().getContentDisposition());
		obj.put("encoding", object.getObjectMetadata().getContentEncoding());
		obj.put("md5", object.getObjectMetadata().getContentMD5());
		obj.put("drawer", object.getBucketName());
		obj.put("etag", object.getObjectMetadata().getETag());
		obj.put("id", object.getKey());
		obj.put("modified", getDateFormatter().format(object.getObjectMetadata().getLastModified()));
		obj.put("class", object.getObjectMetadata().getStorageClass());
		return obj;
	}

	private static DateFormat getDateFormatter() {
		if (df == null)
			df = new SimpleDateFormat(dateFormat);
		return df;
	}

}
