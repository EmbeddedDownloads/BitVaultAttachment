	package com.bitVaultAttachment.apiMethods;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;

import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.bitVaultAttachment.models.DataFromPBCModel;

public class PBCRequest {
	
	public static final String A2A_TAG = "A2A_File";
	public static final String A2A_NOTIFICATION_TAG = "A2A_FileNotification";
	public static final String B2A_TAG = "B2A_FileNotification";
	public static final String B2A_NOTIFICATION_TAG = "B2A_FileNotification";
	
	public static final int NODES_PBC = 3;
	public static final int NODES_SENT_SUCCESS_THRES = NODES_PBC - 1;

	public static final String URL_HTTPS_EXT = "http://";

	// Live
	public static final String URL_1 = "35.161.247.187:8080";
	public static final String URL_2 = "34.212.202.45:8080";
	public static final String URL_3 = "52.37.159.5:8080";
	
	// urls for api
	public static final String URL_SEND = "/PrivateBlockChain/apis/sendMessage";
	public static final String URL_GET_ATTACHMENT_LINK = "/PrivateBlockChain/apis/block/getMessage";
	public static final String URL_GET_ATTACHMENT_STATUS = "/PrivateBlockChain/apis/block/messageStatus";
	public static final String URL_GET_BLOCKS_FOR_ADDRESS = "/PrivateBlockChain/apis/block/getBlocks";
	public static final String URL_GET_FILE = "/PrivateBlockChain/apis/block/getFile?fileId=";
	public static final String URL_SEND_ACK = "/PrivateBlockChain/apis/block/acknowledge/";

	// PBC model
	public static final String KEY_TRANSACTION_ID = "transactionId";
	public static final String KEY_CRC = "crc";
	public static final String KEY_DATA_TAG = "tag";
	public static final String KEY_RECEIVER = "receiverAddress";
	public static final String KEY_FILE = "file";
	public static final String KEY_APPID = "appId";
	public static final String KEY_TIMESTAMP = "timestamp";
	public static final String KEY_SESSION_KEY = "sessionKey";
	public static final String KEY_PBC_ID = "pbcId";
	public static final String KEY_DATA_HASH = "dataHash";
	public static final String KEY_SENDER = "senderAddress";
	public static final String KEY_WEB_SERVER_KEY = "webServerKey";

	public static final String WEB_SERVER_VALUE = "4370ca92-0004-421c-9195-6v07cf2ddgcf";
	
	public final static String BLOCK_CREATED = "BLOCK_TO_BE_CREATED";
	public final static String BLOCK_INPROCESS = "INPROCESS";
	public final static String BLOCK_SAVED = "SAVED";

	public static final int CONNECT_TIMEOUT = 25000;
	public static final int SOCKET_TIMEOUT = 25000;
	public static final int REQUEST_TIMEOUT = 25000;

	/**
	 * Get attachment link and data from PBC
	 * @param txid
	 * @param tag
	 * @param receiverAddress
	 * @param node
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static DataFromPBCModel getAttachmentLink(String txid, String tag, 
			String receiverAddress, int node) {
		
		DataFromPBCModel pbcModel = null;
		Utils.getLogger().log(Level.FINEST, "getAttachmentLink API");
		JSONObject sendObj = new JSONObject();
		sendObj.put("transactionId", txid);
		sendObj.put("tag", tag);
		sendObj.put("receiverAddress", receiverAddress);
		Utils.getLogger().log(Level.FINEST, sendObj.toJSONString());

		// get the URL for sending
		String URL = URL_HTTPS_EXT;
		URL += getUrlForNode(node);
		URL += URL_GET_ATTACHMENT_LINK;
		
		// Set configuration
		RequestConfig defaultRequestConfig = RequestConfig.custom()
				.setConnectTimeout(CONNECT_TIMEOUT)
				.setSocketTimeout(SOCKET_TIMEOUT)
				.setConnectionRequestTimeout(REQUEST_TIMEOUT).build();

		// create http client
		CloseableHttpClient httpClient = HttpClients.custom()
				.setDefaultRequestConfig(defaultRequestConfig)
				.build();

		try {
			HttpPost request = new HttpPost(URL);

			// add header and body
			StringEntity params = new StringEntity(sendObj.toJSONString());
			request.addHeader("content-type", "application/json");
			request.setEntity(params);

			// connect and post
			HttpResponse result = httpClient.execute(request);

			// handle response
			int status = result.getStatusLine().getStatusCode();
			if (!(status >= 200 && status < 300)) {
				throw new Exception("Unexpected response status: " + status);
			}

			// get result
			String jsonResult = EntityUtils.toString(result.getEntity(), "UTF-8");
			Utils.getLogger().log(Level.FINEST, "response : " + jsonResult);

			// parse JSON result
			try {
				JSONParser parser = new JSONParser();
				Object resultObject = parser.parse(jsonResult);
				JSONObject obj = (JSONObject) resultObject;
				JSONObject messageBlock = (JSONObject)obj.get("resultSet");
				
				if( !((String) obj.get("status")).equals("success")){
					return pbcModel;
				}
				
				// copy to data model
				pbcModel = new DataFromPBCModel((JSONObject)obj, (JSONObject)messageBlock);
				
				Utils.getLogger().log(Level.FINEST, "status : " + pbcModel.getStatus());
				Utils.getLogger().log(Level.FINEST, "message : " + pbcModel.getMessage());
				Utils.getLogger().log(Level.FINEST, "fileId : " + pbcModel.getResultSet().getFileId());
				Utils.getLogger().log(Level.FINEST, "crc : " + pbcModel.getResultSet().getCrc());
			} catch (Exception e) {
				Utils.getLogger().log(Level.SEVERE,"json parsing error", e);
			}
		} catch (Exception ex) {
			Utils.getLogger().log(Level.SEVERE,"http connection exception", ex);
		}
		return pbcModel;
	}

	/**
	 * gets the status of an attachment block on the PBC
	 * 
	 * @param tag
	 * @param txid
	 * @param node
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static String getAttachmentBlockStatus(String tag, String txid, int node) throws Exception {

		Utils.getLogger().log(Level.FINEST, "/n getAttachmentBlockStatus API");
		JSONObject sendObj = new JSONObject();
		sendObj.put("tag", tag);
		sendObj.put("transactionId", txid);
		Utils.getLogger().log(Level.FINEST, sendObj.toJSONString());

		// get the URL for sending
		String URL = URL_HTTPS_EXT;
		URL += getUrlForNode(node);
		URL += URL_GET_ATTACHMENT_STATUS;

		// Set configuration
		RequestConfig defaultRequestConfig = RequestConfig.custom().
				setConnectTimeout(CONNECT_TIMEOUT)
				.setSocketTimeout(SOCKET_TIMEOUT).setConnectionRequestTimeout(REQUEST_TIMEOUT).build();

		// create http client
		CloseableHttpClient httpClient = HttpClients.custom().
				setDefaultRequestConfig(defaultRequestConfig).build();
		JSONObject obj = null;

		try {
			// http post entity
			HttpPost request = new HttpPost(URL);

			// add header and body
			StringEntity params = new StringEntity(sendObj.toJSONString());
			request.addHeader("content-type", "application/json");
			request.setEntity(params);

			// connect and post, get result
			HttpResponse result = httpClient.execute(request);

			// handle response
			int status = result.getStatusLine().getStatusCode();
			if (!(status >= 200 && status < 300)) {
				throw new Exception("Unexpected response status: " + status);
			}

			// get result
			String jsonResult = EntityUtils.toString(result.getEntity(), "UTF-8");
			Utils.getLogger().log(Level.FINEST, "response : " + jsonResult);

			// parse JSON result
			try {
				JSONParser parser = new JSONParser();
				Object resultObject = parser.parse(jsonResult);
				obj = (JSONObject) resultObject;
				Utils.getLogger().log(Level.FINEST, "status : " + (String) obj.get("status"));
				Utils.getLogger().log(Level.FINEST, "message : " + (String) obj.get("message"));
				Utils.getLogger().log(Level.FINEST, "resulSet : " + (String) obj.get("resultSet"));

			} catch (Exception e) {
				Utils.getLogger().log(Level.SEVERE,"json parsing error", e);
				throw e;
			}

		} catch (Exception ex) {
			Utils.getLogger().log(Level.SEVERE,"http connection exception", ex);
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				Utils.getLogger().log(Level.SEVERE,"http client close exception", e);
			}
		}
		return (String) obj.get("resultSet");
	}

	/**
	 * Gets the blocks on PBC for the receiver Addresses Returns the array list
	 * of blocks in JSONObject with keys ->
	 * "webServerKey","receiver","pbcId","sessionKey","crc","sender","appId","tag",
	 * "transactionId","fileId", "timestamp"
	 * 
	 * @param receiverAddress
	 * @param node
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<JSONObject> getBlocksForAddress(ArrayList<String> receiverAddress, int node) {

		ArrayList<JSONObject> resultBlocks = new ArrayList<JSONObject>();
		Utils.getLogger().log(Level.FINEST, "getBlocksForAddress API");

		// add addresses
		JSONArray sendAddressArray = new JSONArray();
		sendAddressArray.addAll(receiverAddress);

		// arguments to send
		JSONObject sendObj = new JSONObject();
		sendObj.put("receiverAddress", sendAddressArray);
		Utils.getLogger().log(Level.FINEST, sendObj.toJSONString());

		// get the URL for sending
		String URL = URL_HTTPS_EXT;
		URL += getUrlForNode(node);
		URL += URL_GET_BLOCKS_FOR_ADDRESS;

		// Set configuration
		RequestConfig defaultRequestConfig = RequestConfig.custom()
				.setConnectTimeout(CONNECT_TIMEOUT)
				.setSocketTimeout(SOCKET_TIMEOUT)
				.setConnectionRequestTimeout(REQUEST_TIMEOUT).build();

		// create http client
		CloseableHttpClient httpClient = HttpClients.custom()
				.setDefaultRequestConfig(defaultRequestConfig)
				.build();

		try {
			// http post entity
			HttpPost request = new HttpPost(URL);

			// add header and body
			StringEntity params = new StringEntity(sendObj.toJSONString());
			request.addHeader("content-type", "application/json");
			request.setEntity(params);

			// connect and post, get result
			HttpResponse result = httpClient.execute(request);

			int status = result.getStatusLine().getStatusCode();
			if (!(status >= 200 && status < 300)) {
				throw new Exception("Unexpected response status: " + status);
			}

			// get result
			String jsonResult = EntityUtils.toString(result.getEntity(), "UTF-8");
			Utils.getLogger().log(Level.FINEST, "result: " + jsonResult);

			// parse JSON result
			try {
				JSONParser parser = new JSONParser();
				Object resultObject = parser.parse(jsonResult);
				JSONObject obj = (JSONObject) resultObject;
				Utils.getLogger().log(Level.FINEST, "status : " + (String) obj.get("status"));
				Utils.getLogger().log(Level.FINEST, "message : " + (String) obj.get("message"));

				// if blocks returned
				if (((String) obj.get("status")).equalsIgnoreCase("success")) {

					JSONArray blockList = new JSONArray();
					blockList = (JSONArray) obj.get("resultSet");
					for (int i = 0; i < blockList.size(); i++) {
						resultBlocks.add((JSONObject) blockList.get(i));
						Utils.getLogger().log(Level.FINEST, "result " + i + " : " + resultBlocks.get(i).toJSONString());
					}
				}

			} catch (Exception e) {
				Utils.getLogger().log(Level.SEVERE,"json parsing error", e);
			}

		} catch (Exception ex) {
			Utils.getLogger().log(Level.SEVERE,"http connection exception", ex);
		} finally {
			try {
				httpClient.close();
			} catch (IOException e) {
				Utils.getLogger().log(Level.SEVERE,"http client close exception", e);
			}
		}
		return resultBlocks;
	}

	/**
	 * returns the URL for the node
	 * 
	 * @param node
	 * @return
	 */
	public static String getUrlForNode(int node) {

		String URL = null;
		switch (node) {
		case 1:
			URL = URL_1;
			break;
		case 2:
			URL = URL_2;
			break;
		case 3:
			URL = URL_3;
			break;
		default:
			break;
		}
		return URL;
	}
}
