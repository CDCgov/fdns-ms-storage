package gov.cdc.foundation.helper;

import java.util.HashMap;
import java.util.Map;

import gov.cdc.helper.AbstractMessageHelper;

public class MessageHelper extends AbstractMessageHelper {

	public static final String CONST_DRAWER = "drawer";

	public static final String METHOD_INDEX = "index";
	public static final String METHOD_GETDRAWERS = "getDrawers";
	public static final String METHOD_GETDRAWER = "getDrawer";
	public static final String METHOD_CREATEDRAWER = "createDrawer";
	public static final String METHOD_DELETEDRAWER = "deleteDrawer";
	public static final String METHOD_LISTNODES = "listNodes";
	public static final String METHOD_GETNODE = "getNode";
	public static final String METHOD_DOWNLOADNODE = "downloadNode";
	public static final String METHOD_DELETENODE = "deleteNode";
	public static final String METHOD_CREATENODE = "createNode";
	public static final String METHOD_COPYNODE = "createNode";
	public static final String METHOD_UPDATENODE = "updateNode";
	
	public static final String ERROR_DRAWERNAME_REQUIRED = "You must provide a drawer name.";
	public static final String ERROR_ID_REQUIRED = "You must provide a node id, or to ask to generate one.";
	public static final String ERROR_DRAWER_DONT_EXIST = "The following drawer doesn't exist: %s";
	public static final String ERROR_OBJECT_DONT_EXIST = "The following object doesn't exist `%s` in the drawer `%s`";
	public static final String ERROR_DRAWER_ALREADY_EXISTS = "The following drawer already exists: %s";
	public static final String ERROR_OBJECT_ALREADY_EXISTS_IMMUTABLE = "The following object already exists `%s` in the immutable drawer `%s`.";
	public static final String ERROR_OBJECT_ALREADY_EXISTS = "The following object already exists `%s` in the drawer `%s`.";
	public static final String ERROR_IMMUTABLE_DRAWER = "The following drawer is immutable: %s";
	public static final String ERROR_SOURCE_DRAWERNAME_REQUIRED = "You must provide a source drawer name.";
	public static final String ERROR_TARGET_DRAWERNAME_REQUIRED = "You must provide a target drawer name.";

	private MessageHelper() {
		throw new IllegalAccessError("Helper class");
	}

	public static Map<String, Object> initializeLog(String method, String drawer) {
		Map<String, Object> log = new HashMap<>();
		log.put(MessageHelper.CONST_METHOD, method);
		if (drawer != null)
			log.put(MessageHelper.CONST_DRAWER, drawer);
		return log;
	}

}
