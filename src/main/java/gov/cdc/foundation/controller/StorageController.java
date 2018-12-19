package gov.cdc.foundation.controller;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.cdc.foundation.helper.LoggerHelper;
import gov.cdc.foundation.helper.MessageHelper;
import gov.cdc.foundation.helper.MimeTypes;
import gov.cdc.foundation.helper.ResourceHelper;
import gov.cdc.foundation.helper.TemplatingHelper;
import gov.cdc.helper.ErrorHandler;
import gov.cdc.helper.common.ServiceException;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Controller
@RequestMapping("/api/1.0/")
public class StorageController {

	private static final Logger logger = Logger.getLogger(StorageController.class);
	
	@Value("${version}")
	private String version;
	
	@Value("${immutable}")
	private String immutableDrawers;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<?> index() throws IOException {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> log = new HashMap<>();
		
		try {
			JSONObject json = new JSONObject();
			json.put("version", version);
			return new ResponseEntity<>(mapper.readTree(json.toString()), HttpStatus.OK);
		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.log(MessageHelper.METHOD_INDEX, log);
			
			return ErrorHandler.getInstance().handle(e, log);
		}
	}

	@PreAuthorize(
		"!@authz.isSecured()"
		+ " or #oauth2.hasScope('fdns.storage.*.read')"
		+ " or #oauth2.hasScope('fdns.storage.*.*')"
	)
	@RequestMapping(
		method = RequestMethod.GET,
		value = "/drawer",
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ApiOperation(value = "Get all drawers", notes = "Get all drawers.")
	@ResponseBody
	public ResponseEntity<?> getDrawers() {
		JSONArray data = new JSONArray();
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> log = MessageHelper.initializeLog(MessageHelper.METHOD_GETDRAWERS, null);

		try {
			List<Bucket> buckets = ResourceHelper.getS3Client().listBuckets();
			for (Bucket bucket : buckets)
				data.put(TemplatingHelper.process(bucket));
			return new ResponseEntity<>(mapper.readTree(data.toString()), HttpStatus.OK);
		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.log(MessageHelper.METHOD_GETDRAWERS, log);
			
			return ErrorHandler.getInstance().handle(e, log);
		}
	}

	@PreAuthorize(
		"!@authz.isSecured()"
		+ " or (#name == 'public')"
		+ " or #oauth2.hasScope('fdns.storage.'.concat(#name).concat('.read'))"
		+ " or #oauth2.hasScope('fdns.storage.'.concat(#name).concat('.*'))"
		+ " or #oauth2.hasScope('fdns.storage.*.read')"
		+ " or #oauth2.hasScope('fdns.storage.*.*')"
	)
	@RequestMapping(
		method = RequestMethod.GET,
		value = "/drawer/{name}",
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ApiOperation(value = "Get drawer", notes = "Get drawer")
	@ResponseBody
	public ResponseEntity<?> getDrawer(
		@ApiParam(value = "Drawer name") @PathVariable(value = "name") String name
	) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> log = MessageHelper.initializeLog(MessageHelper.METHOD_GETDRAWER, name);

		try {
			if (name == null || name.length() == 0)
				throw new ServiceException(MessageHelper.ERROR_DRAWERNAME_REQUIRED);

			AmazonS3 client = ResourceHelper.getS3Client();
			if (!client.doesBucketExistV2(name))
				throw new ServiceException(String.format(MessageHelper.ERROR_DRAWER_DONT_EXIST, name));
			List<Bucket> buckets = ResourceHelper.getS3Client().listBuckets();
			Bucket theBucket = null;
			for (Bucket bucket : buckets) {
				if (bucket.getName().equals(name))
					theBucket = bucket;
			}
			if (theBucket != null)
				return new ResponseEntity<>(mapper.readTree(TemplatingHelper.process(theBucket).toString()), HttpStatus.OK);
			else
				throw new ServiceException(String.format(MessageHelper.ERROR_DRAWER_DONT_EXIST, name));
		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.log(MessageHelper.METHOD_GETDRAWER, log);

			return ErrorHandler.getInstance().handle(e, log);
		}
	}

	@PreAuthorize(
		"!@authz.isSecured()"
		+ " or (#name == 'public')"
		+ " or #oauth2.hasScope('fdns.storage.'.concat(#name).concat('.create'))"
		+ " or #oauth2.hasScope('fdns.storage.'.concat(#name).concat('.*'))"
		+ " or #oauth2.hasScope('fdns.storage.*.create')"
		+ " or #oauth2.hasScope('fdns.storage.*.*')"
	)
	@RequestMapping(
		method = RequestMethod.PUT,
		value = "/drawer/{name}",
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ApiOperation(value = "Create drawer", notes = "Create drawer")
	@ResponseBody
	public ResponseEntity<?> createDrawer(
		@ApiParam(value = "Drawer name") @PathVariable(value = "name") String name
	) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> log = MessageHelper.initializeLog(MessageHelper.METHOD_CREATEDRAWER, name);

		try {
			if (name == null || name.length() == 0)
				throw new ServiceException(MessageHelper.ERROR_DRAWERNAME_REQUIRED);

			AmazonS3 client = ResourceHelper.getS3Client();
			if (client.doesBucketExistV2(name))
				throw new ServiceException(String.format(MessageHelper.ERROR_DRAWER_ALREADY_EXISTS, name));
			else {
				return new ResponseEntity<>(mapper.readTree(TemplatingHelper.process(client.createBucket(name)).toString()), HttpStatus.CREATED);
			}
		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.log(MessageHelper.METHOD_CREATEDRAWER, log);
			
			return ErrorHandler.getInstance().handle(e, log);
		}
	}

	@PreAuthorize(
		"!@authz.isSecured()"
		+ " or #oauth2.hasScope('fdns.storage.'.concat(#name).concat('.delete'))"
		+ " or #oauth2.hasScope('fdns.storage.'.concat(#name).concat('.*'))"
		+ " or #oauth2.hasScope('fdns.storage.*.delete')"
		+ " or #oauth2.hasScope('fdns.storage.*.*')"
	)
	@RequestMapping(
		method = RequestMethod.DELETE,
		value = "/drawer/{name}",
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ApiOperation(value = "Delete drawer", notes = "Delete drawer")
	@ResponseBody
	public ResponseEntity<?> deleteDrawer(
		@ApiParam(value = "Drawer name") @PathVariable(value = "name") String name
	) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> log = MessageHelper.initializeLog(MessageHelper.METHOD_DELETEDRAWER, name);

		try {

			if (name == null || name.length() == 0)
				throw new ServiceException(MessageHelper.ERROR_DRAWERNAME_REQUIRED);

			AmazonS3 client = ResourceHelper.getS3Client();
			if (!client.doesBucketExistV2(name))
				throw new ServiceException(String.format(MessageHelper.ERROR_DRAWER_DONT_EXIST, name));
			else {
				client.deleteBucket(name);

				JSONObject json = new JSONObject();
				json.put(MessageHelper.CONST_SUCCESS, true);

				return new ResponseEntity<>(mapper.readTree(json.toString()), HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.log(MessageHelper.METHOD_DELETEDRAWER, log);
			
			return ErrorHandler.getInstance().handle(e, log);
		}
	}

	@PreAuthorize(
		"!@authz.isSecured()"
		+ " or (#name == 'public')"
		+ " or #oauth2.hasScope('fdns.storage.'.concat(#name).concat('.read'))"
		+ " or #oauth2.hasScope('fdns.storage.'.concat(#name).concat('.*'))"
		+ " or #oauth2.hasScope('fdns.storage.*.read')"
		+ " or #oauth2.hasScope('fdns.storage.*.*')"
	)
	@RequestMapping(method = RequestMethod.GET, value = "/drawer/nodes/{name}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ApiOperation(value = "List all nodes in a drawer", notes = "List all nodes in a drawer")
	@ResponseBody
	public ResponseEntity<?> listNodes(
		@ApiParam(value = "Drawer name") @PathVariable(value = "name") String name,
		@ApiParam(value = "Prefix") @RequestParam(required = false) String prefix
	) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> log = MessageHelper.initializeLog(MessageHelper.METHOD_LISTNODES, name);

		try {
			if (name == null || name.length() == 0)
				throw new ServiceException(MessageHelper.ERROR_DRAWERNAME_REQUIRED);

			AmazonS3 client = ResourceHelper.getS3Client();
			if (!client.doesBucketExistV2(name))
				throw new ServiceException(String.format(MessageHelper.ERROR_DRAWER_DONT_EXIST, name));
			else {
				JSONArray data = new JSONArray();
				ListObjectsV2Result result;
				if (prefix == null || prefix.length() == 0)
					result = client.listObjectsV2(name);
				else
					result = client.listObjectsV2(name, prefix);
				List<S3ObjectSummary> summaries = result.getObjectSummaries();
				for (S3ObjectSummary summary : summaries) {
					data.put(TemplatingHelper.process(summary));
				}

				return new ResponseEntity<>(mapper.readTree(data.toString()), HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.log(MessageHelper.METHOD_LISTNODES, log);
			
			return ErrorHandler.getInstance().handle(e, log);
		}
	}

	@PreAuthorize(
		"!@authz.isSecured()"
		+ " or (#name == 'public')"
		+ " or #oauth2.hasScope('fdns.storage.'.concat(#name).concat('.read'))"
		+ " or #oauth2.hasScope('fdns.storage.'.concat(#name).concat('.*'))"
		+ " or #oauth2.hasScope('fdns.storage.*.read')"
		+ " or #oauth2.hasScope('fdns.storage.*.*')"
	)
	@RequestMapping(
		method = RequestMethod.GET,
		value = "/node/{name}",
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ApiOperation(value = "Get node", notes = "Get node")
	@ResponseBody
	public ResponseEntity<?> getNode(
		@ApiParam(value = "Drawer name") @PathVariable(value = "name") String name,
		@ApiParam(value = "Node id") @RequestParam(value = "id", required = true) String id
	) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> log = MessageHelper.initializeLog(MessageHelper.METHOD_GETNODE, name);

		try {
			if (name == null || name.length() == 0)
				throw new ServiceException(MessageHelper.ERROR_DRAWERNAME_REQUIRED);

			AmazonS3 client = ResourceHelper.getS3Client();
			if (!client.doesBucketExistV2(name))
				throw new ServiceException(String.format(MessageHelper.ERROR_DRAWER_DONT_EXIST, name));
			else {
				if (!client.doesObjectExist(name, id))
					throw new ServiceException(String.format(MessageHelper.ERROR_OBJECT_DONT_EXIST, id, name));

				return new ResponseEntity<>(mapper.readTree(TemplatingHelper.process(client.getObject(name, id)).toString()), HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.log(MessageHelper.METHOD_GETNODE, log);

			return ErrorHandler.getInstance().handle(e, log);
		}
	}

	@PreAuthorize(
		"!@authz.isSecured()"
		+ " or (#name == 'public')"
		+ " or #oauth2.hasScope('fdns.storage.'.concat(#name).concat('.read'))"
		+ " or #oauth2.hasScope('fdns.storage.'.concat(#name).concat('.*'))"
		+ " or #oauth2.hasScope('fdns.storage.*.read')"
		+ " or #oauth2.hasScope('fdns.storage.*.*')"
	)
	@RequestMapping(
		method = RequestMethod.GET,
		value = "/node/{name}/dl"
	)
	@ApiOperation(
		value = "Download node",
		notes = "Download node"
	)
	@ResponseBody
	public ResponseEntity<?> downloadNode(
		@ApiParam(value = "Drawer name") @PathVariable(value = "name") String name,
		@ApiParam(value = "Node id") @RequestParam(value = "id", required = true) String id
	) {
		Map<String, Object> log = MessageHelper.initializeLog(MessageHelper.METHOD_DOWNLOADNODE, name);

		try {
			if (name == null || name.length() == 0)
				throw new ServiceException(MessageHelper.ERROR_DRAWERNAME_REQUIRED);

			AmazonS3 client = ResourceHelper.getS3Client();
			if (!client.doesBucketExistV2(name))
				throw new ServiceException(String.format(MessageHelper.ERROR_DRAWER_DONT_EXIST, name));
			else {
				if (!client.doesObjectExist(name, id))
					throw new ServiceException(String.format(MessageHelper.ERROR_OBJECT_DONT_EXIST, id, name));

				// Get object
				S3Object object = client.getObject(name, id);

				// Prepare headers
				final HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.valueOf(MimeTypes.getMimeType(FilenameUtils.getExtension(id))));
				headers.setContentDispositionFormData(id, id);

				return new ResponseEntity<>(IOUtils.toByteArray(object.getObjectContent()), headers, HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.log(MessageHelper.METHOD_DOWNLOADNODE, log);
			
			return ErrorHandler.getInstance().handle(e, log);
		}
	}

	@PreAuthorize(
		"!@authz.isSecured()"
		+ " or #oauth2.hasScope('fdns.storage.'.concat(#name).concat('.delete'))"
		+ " or #oauth2.hasScope('fdns.storage.'.concat(#name).concat('.*'))"
		+ " or #oauth2.hasScope('fdns.storage.*.delete')"
		+ " or #oauth2.hasScope('fdns.storage.*.*')"
	)
	@RequestMapping(
		method = RequestMethod.DELETE,
		value = "/node/{name}",
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ApiOperation(value = "Delete node", notes = "Delete node")
	@ResponseBody
	public ResponseEntity<?> deleteNode(
		@ApiParam(value = "Drawer name") @PathVariable(value = "name") String name,
		@ApiParam(value = "Node id") @RequestParam(value = "id", required = true) String id
	) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> log = MessageHelper.initializeLog(MessageHelper.METHOD_DELETENODE, name);

		try {
			if (name == null || name.length() == 0)
				throw new ServiceException(MessageHelper.ERROR_DRAWERNAME_REQUIRED);

			AmazonS3 client = ResourceHelper.getS3Client();
			if (!client.doesBucketExistV2(name))
				throw new ServiceException(String.format(MessageHelper.ERROR_DRAWER_DONT_EXIST, name));
			else {
				if (!client.doesObjectExist(name, id))
					throw new ServiceException(String.format(MessageHelper.ERROR_OBJECT_DONT_EXIST, id, name));

				client.deleteObject(name, id);

				JSONObject json = new JSONObject();
				json.put(MessageHelper.CONST_SUCCESS, true);

				return new ResponseEntity<>(mapper.readTree(json.toString()), HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.log(MessageHelper.METHOD_DELETENODE, log);
			
			return ErrorHandler.getInstance().handle(e, log);
		}
	}

	@PreAuthorize(
		"!@authz.isSecured()"
		+ " or (#name == 'public')"
		+ " or #oauth2.hasScope('fdns.storage.'.concat(#name).concat('.create'))"
		+ " or #oauth2.hasScope('fdns.storage.'.concat(#name).concat('.*'))"
		+ " or #oauth2.hasScope('fdns.storage.*.create')"
		+ " or #oauth2.hasScope('fdns.storage.*.*')"
	)
	@RequestMapping(
		method = RequestMethod.POST,
		value = "/node/{name}",
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ApiOperation(
		value = "Create node",
		notes = "Create node"
	)
	@ResponseBody
	public ResponseEntity<?> createNode(
		@RequestParam("file") MultipartFile file,
		@ApiParam(value = "Drawer name") @PathVariable(value = "name", required = true) String name,
		@ApiParam(value = "Node id") @RequestParam(value = "id", required = false) String id,
		@ApiParam(value = "Generate date/time structure if asked") @RequestParam(value = "generateStruct", defaultValue = "false", required = false) boolean generateStruct,
		@ApiParam(value = "Generate node id if asked") @RequestParam(value = "generateId", defaultValue = "false", required = false) boolean generateId,
		@ApiParam(value = "Replace if existing") @RequestParam(value = "replace", defaultValue = "false", required = false) boolean replace
	) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> log = MessageHelper.initializeLog(MessageHelper.METHOD_CREATENODE, name);

		try {
			if (name == null || name.length() == 0)
				throw new ServiceException(MessageHelper.ERROR_DRAWERNAME_REQUIRED);

			AmazonS3 client = ResourceHelper.getS3Client();
			if (!client.doesBucketExistV2(name))
				throw new ServiceException(String.format(MessageHelper.ERROR_DRAWER_DONT_EXIST, name));
			else {
				String objectId = getId(id, generateId, generateStruct);
				canCreateNode(client, name, objectId, replace);

				ObjectMetadata metadata = new ObjectMetadata();
				metadata.setContentLength(file.getSize());
				metadata.setContentType(file.getContentType());

				client.putObject(name, objectId, file.getInputStream(), metadata);

				return new ResponseEntity<>(mapper.readTree(TemplatingHelper.process(client.getObject(name, objectId)).toString()), HttpStatus.CREATED);

			}
		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.log(MessageHelper.METHOD_CREATENODE, log);

			return ErrorHandler.getInstance().handle(e, log);
		}
	}

	private String getId(String id, boolean generateId, boolean generateStruct) throws ServiceException {
		String uniqueId = id;
		if (generateId)
			uniqueId = UUID.randomUUID().toString();

		if (uniqueId == null || uniqueId.length() == 0)
			throw new ServiceException(MessageHelper.ERROR_ID_REQUIRED);

		String objectId = uniqueId;
		if (generateStruct) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd/HH/mm/ss/");
			objectId = dateFormat.format(new Date()) + uniqueId;
		}
		return objectId;
	}

	@PreAuthorize(
		"!@authz.isSecured()"
		+ " or (#name == 'public')"
		+ " or (#oauth2.hasScope('fdns.storage.'.concat(#sourceDrawerName).concat('.read')) and #oauth2.hasScope('fdns.storage.'.concat(#targetDrawerName).concat('.create')))"
		+ " or (#oauth2.hasScope('fdns.storage.'.concat(#sourceDrawerName).concat('.*')) and #oauth2.hasScope('fdns.storage.'.concat(#targetDrawerName).concat('.create')))"
		+ " or (#oauth2.hasScope('fdns.storage.'.concat(#sourceDrawerName).concat('.read')) and #oauth2.hasScope('fdns.storage.'.concat(#targetDrawerName).concat('.*')))"
		+ " or (#oauth2.hasScope('fdns.storage.'.concat(#sourceDrawerName).concat('.*')) and #oauth2.hasScope('fdns.storage.*.create'))"
		+ " or (#oauth2.hasScope('fdns.storage.'.concat(#sourceDrawerName).concat('.read')) and #oauth2.hasScope('fdns.storage.*.create'))"
		+ " or (#oauth2.hasScope('fdns.storage.*.read') and #oauth2.hasScope('fdns.storage.'.concat(#targetDrawerName).concat('.create')))"
		+ " or (#oauth2.hasScope('fdns.storage.*.read') and #oauth2.hasScope('fdns.storage.'.concat(#targetDrawerName).concat('.*')))"
		+ " or #oauth2.hasScope('fdns.storage.*.read')"
		+ " or #oauth2.hasScope('fdns.storage.*.*')"
	)
	@RequestMapping(
		method = RequestMethod.PUT,
		value = "/node/copy/{source}/{target}",
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ApiOperation(value = "Copy node", notes = "Copy node")
	@ResponseBody
	public ResponseEntity<?> copyNode(
		@ApiParam(value = "Source drawer name") @PathVariable(value = "source", required = true) String sourceDrawerName,
		@ApiParam(value = "Target drawer name") @PathVariable(value = "target", required = true) String targetDrawerName,
		@ApiParam(value = "Source node id") @RequestParam(value = "sourceId", required = true) String sourceId,
		@ApiParam(value = "Target node id") @RequestParam(value = "targetId", required = false) String targetId,
		@ApiParam(value = "Generate date/time structure if asked") @RequestParam(value = "generateStruct", defaultValue = "false", required = false) boolean generateStruct,
		@ApiParam(value = "Generate node id if asked") @RequestParam(value = "generateId", defaultValue = "false", required = false) boolean generateId,
		@ApiParam(value = "Delete original") @RequestParam(value = "deleteOriginal", defaultValue = "false", required = false) boolean deleteOriginal
	) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> log = MessageHelper.initializeLog(MessageHelper.METHOD_COPYNODE, sourceDrawerName);

		try {
			if (sourceDrawerName == null || sourceDrawerName.length() == 0)
				throw new ServiceException(MessageHelper.ERROR_SOURCE_DRAWERNAME_REQUIRED);
			if (targetDrawerName == null || targetDrawerName.length() == 0)
				throw new ServiceException(MessageHelper.ERROR_TARGET_DRAWERNAME_REQUIRED);

			AmazonS3 client = ResourceHelper.getS3Client();
			if (!client.doesBucketExistV2(sourceDrawerName))
				throw new ServiceException(String.format(MessageHelper.ERROR_DRAWER_DONT_EXIST, sourceDrawerName));
			if (!client.doesBucketExistV2(targetDrawerName))
				throw new ServiceException(String.format(MessageHelper.ERROR_DRAWER_DONT_EXIST, targetDrawerName));
			if (!client.doesObjectExist(sourceDrawerName, sourceId))
				throw new ServiceException(String.format(MessageHelper.ERROR_OBJECT_DONT_EXIST, sourceId, sourceDrawerName));

			String objectId = getId(targetId, generateId, generateStruct);
			canCreateNode(client, targetDrawerName, objectId, false);

			client.copyObject(sourceDrawerName, sourceId, targetDrawerName, objectId);

			if (deleteOriginal)
				client.deleteObject(sourceDrawerName, sourceId);

			JSONObject json = new JSONObject();
			json.put(MessageHelper.CONST_SUCCESS, true);
			json.put("sourceDrawerName", sourceDrawerName);
			json.put("targetDrawerName", targetDrawerName);
			json.put("sourceId", sourceId);
			json.put("targetId", objectId);

			return new ResponseEntity<>(mapper.readTree(json.toString()), HttpStatus.CREATED);
		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.log(MessageHelper.METHOD_COPYNODE, log);

			return ErrorHandler.getInstance().handle(e, log);
		}
	}

	private void canCreateNode(AmazonS3 client, String name, String objectId, boolean replace) throws ServiceException {
		boolean objectExists = client.doesObjectExist(name, objectId);
		boolean drawerImmutable = isImmutableDrawer(name);
		if (drawerImmutable)
			throw new ServiceException(String.format(MessageHelper.ERROR_OBJECT_ALREADY_EXISTS_IMMUTABLE, objectId, name));
		if (objectExists && !replace)
			throw new ServiceException(String.format(MessageHelper.ERROR_OBJECT_ALREADY_EXISTS, objectId, name));
	}

	@PreAuthorize(
		"!@authz.isSecured()"
		+ " or #oauth2.hasScope('fdns.storage.'.concat(#name).concat('.update'))"
		+ " or #oauth2.hasScope('fdns.storage.'.concat(#name).concat('.*'))"
		+ " or #oauth2.hasScope('fdns.storage.*.update')"
		+ " or #oauth2.hasScope('fdns.storage.*.*')"
	)
	@RequestMapping(
		method = RequestMethod.PUT,
		value = "/node/{name}",
		produces = MediaType.APPLICATION_JSON_VALUE
	)
	@ApiOperation(value = "Update node", notes = "Update node")
	@ResponseBody
	public ResponseEntity<?> updateNode(
		@RequestParam("file") MultipartFile file,
		@ApiParam(value = "Drawer name") @PathVariable(value = "name") String name,
		@ApiParam(value = "Node id") @RequestParam(value = "id", required = true) String id
	) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> log = MessageHelper.initializeLog(MessageHelper.METHOD_UPDATENODE, name);

		try {
			if (name == null || name.length() == 0)
				throw new ServiceException(MessageHelper.ERROR_DRAWERNAME_REQUIRED);

			AmazonS3 client = ResourceHelper.getS3Client();
			if (!client.doesBucketExistV2(name))
				throw new ServiceException(String.format(MessageHelper.ERROR_DRAWER_DONT_EXIST, name));
			else {
				if (isImmutableDrawer(name))
					throw new ServiceException(String.format(MessageHelper.ERROR_IMMUTABLE_DRAWER, name));

				if (!client.doesObjectExist(name, id))
					throw new ServiceException(String.format(MessageHelper.ERROR_OBJECT_DONT_EXIST, id, name));

				ObjectMetadata metadata = new ObjectMetadata();
				metadata.setContentLength(file.getSize());
				metadata.setContentType(file.getContentType());

				client.putObject(name, id, file.getInputStream(), metadata);

				JSONObject json = new JSONObject();
				json.put(MessageHelper.CONST_SUCCESS, true);
				json.put("id", id);

				return new ResponseEntity<>(mapper.readTree(json.toString()), HttpStatus.OK);
			}
		} catch (Exception e) {
			logger.error(e);
			LoggerHelper.log(MessageHelper.METHOD_UPDATENODE, log);

			return ErrorHandler.getInstance().handle(e, log);
		}
	}

	private boolean isImmutableDrawer(String drawerName) {
		return Arrays.asList(immutableDrawers.split(";")).contains(drawerName);
	}
}