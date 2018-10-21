	package com.bitVaultAttachment.apiMethods;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Level;

import org.bouncycastle.util.encoders.Hex;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.bitVaultAttachment.constant.BitVaultConstants;
import com.bitVaultAttachment.constant.GlobalCalls;
import com.bitVaultAttachment.controller.MainPage;
import com.bitVaultAttachment.database.DbConnection;

import javafx.stage.WindowEvent;

public class MqttClientAsync implements MqttCallbackExtended {

	private static final int WAIT_TIME = 10000;

	private static final int BEGIN = 0;
	private static final int CONNECTED = 1;
	private static final int PUBLISHED = 2;
	private static final int SUBSCRIBED = 3;
	private static final int UNSUBSCRIBED = 4;
	private static final int DISCONNECTED = 5;
	private static final int FINISH = 6;
	private static final int ERROR = 7;
	private static final int DISCONNECT = 8;

	private static int state;
	private String tmpDir = BitVaultConstants.PATH_FOR_MQTT_FILES;

	// Default settings:
	private String deviceToken = null;
	private String desktopToken = null;
	private boolean registrationStatus;

	private int qos = 1;
	private String broker = "34.209.234.181";
	private int port = 1883;
	private String clientId = null;
	private String subRespTopic = deviceToken;
	private String subNotTopic = null;
	private String pubTopic = "bitvault/desktop-register";
	private boolean cleanSession = true;
	private boolean ssl = false;
	private String password = null;
	private String userName = null;
	private String message = null;

	private String brokerUrl = null;
	private Throwable ex = null;
	private Object waiter = new Object();
	private Object waiterResponse = new Object();
	private boolean donext = false;

	private MqttConnectOptions conOpt;
	private MqttConnector con = new MqttConnector();;

	public MqttAsyncClient client;
	public static String KEY_DEVICE_TOKEN = "device_token";
	public static String KEY_STATUS = "status";
	public static String KEY_DATA = "data";
	public static String KEY_DESKTOP_TOKEN = "desktop_token";
	public static String KEY_HASH_TXID = "data";
	public static String KEY_TAG = "tag";
	public static String KEY_REC_ADDRESS = "receiver_address";
	public static String KEY_SND_ADDRESS = "sender_address";

	/**
	 * Constructor
	 * 
	 * @param deviceToken
	 */
	public MqttClientAsync(String deviceToken) {
		
		this.deviceToken = deviceToken;
		// set window close handler
		MainPage.stage.setOnCloseRequest(e -> windowCloseHandler(e) );
	}

	/**
	 * check device registration
	 * 
	 * @return success/failure
	 * @throws Throwable 
	 */
	@SuppressWarnings("unchecked")
	public boolean mqttCheckRegistration() throws Throwable {

		clientId = "mqtt_" + deviceToken;
		subRespTopic = deviceToken;

		// convert to JSON object
		JSONObject sendObj = new JSONObject();
		sendObj.put( KEY_DEVICE_TOKEN, deviceToken);
		StringWriter out = new StringWriter();
		String jsonText = null;
		try {
			sendObj.writeJSONString(out);
			jsonText = out.toString();
			Utils.getLogger().log(Level.FINEST, jsonText);
		} catch (IOException e) {
			Utils.getLogger().log(Level.SEVERE,"json parsing error", e);
			return false;
		}

		// set the protocol and url
		String protocol = "tcp://";
		if (ssl) {
			protocol = "ssl://";
		}
		brokerUrl = protocol + broker + ":" + port;

		try {
			// Initialize the client
			conOpt = new MqttConnectOptions();
			cleanSession = true; 
			clientId = "mqtt_device_" + deviceToken;
			initClientAsync( clientId, cleanSession);
			// connect client
			connectClient();
			// subscribe to device token for response
			this.subscribe(subRespTopic, qos);
			// publish for verifying desktop
			message = jsonText;
			this.publish(pubTopic, qos, message.getBytes());
			// wait for response
			waitForResponse(10000);
			
			// if success, subscribe to desktop token for notifications
			if (registrationStatus == true) {
				// disconnect previous
				disconnectClient();
				// create new session
				cleanSession = false; 
				clientId = "mqtt_desktop_" + getDesktopClientID(deviceToken);
				initClientAsync( clientId, cleanSession);
				// reconnect
				connectClient();
				// subscribe to notifications
				this.subscribe(subNotTopic, qos);
				
				return true;
			// disconnect if failed
			} else {
				disconnectClient();
			}
			return false;

		} catch (MqttException me) {
			// Display full details of any exception that occurs
			Utils.getLogger().log(Level.SEVERE,"reason " + me.getReasonCode() +
					", msg " + me.getMessage() + ", loc " + me.getLocalizedMessage() + 
					", cause " + me.getCause() + 
					", mqtt registration connection error : " + me);
			return false;
		} catch (Throwable th) {
			Utils.getLogger().log(Level.SEVERE,"Error connecting to notifications ", th);
			return false;
		} 
	}

	/**
	 * Initializes the client
	 * 
	 * @throws MqttException
	 */
	public void initClientAsync( String clId, boolean cleanSess ) throws MqttException {

		// This sample stores in a temporary directory... where messages
		// temporarily
		// stored until the message has been delivered to the server.
		// ..a real application ought to store them somewhere
		// where they are not likely to get deleted or tampered with
		MqttDefaultFilePersistence dataStore = new MqttDefaultFilePersistence(tmpDir);
		
		// set connection options
		conOpt.setCleanSession(cleanSess);
		conOpt.setKeepAliveInterval(3);
		conOpt.setConnectionTimeout(5);
		conOpt.setAutomaticReconnect(true);

		try {
			// Construct the object that contains connection parameters
			if (password != null) {
				conOpt.setPassword(this.password.toCharArray());
			}
			if (userName != null) {
				conOpt.setUserName(this.userName);
			}

			// Construct the MqttClient instance
			client = new MqttAsyncClient(this.brokerUrl, clId, dataStore);

			// Set this wrapper as the callback handler
			client.setCallback(this);

		} catch (MqttException e) {
			Utils.getLogger().log(Level.SEVERE,"Unable to set up client: ", e);
			System.exit(1);
		}
	}
	
	/**
	 * window close handler
	 * @param mqttClientPass
	 */
	public void windowCloseHandler(WindowEvent e){
		
		if ( (client!=null) && client.isConnected() ) {
			
			// disconnect
			try {
				disconnectClient();
			} catch (Exception e1) {
				//
			}
		}
		Utils.getLogger().log(Level.INFO,"window closed");
	}

	/**
	 * Connect mqtt client
	 */
	public void connectClient() {

		con.doConnect();
		try {
			waitForStateChange(WAIT_TIME);
		} catch (MqttException e) {
			Utils.getLogger().log(Level.SEVERE,"connect error", e);
		}
	}

	/**
	 * disconnect mqtt client
	 */
	public void disconnectClient() {

		Disconnector disc = new Disconnector();
		disc.doDisconnect();
		try {
			waitForStateChange(WAIT_TIME);
		} catch (MqttException e) {
			Utils.getLogger().log(Level.SEVERE,"disconnect error", e);
		}
	}

	/**
	 * Publish / send a message to an MQTT server
	 * 
	 * @param topicName
	 *            the name of the topic to publish to
	 * @param qos
	 *            the quality of service to delivery the message at (0,1,2)
	 * @param payload
	 *            the set of bytes to send to the MQTT server
	 * @throws MqttException
	 */
	public void publish(String topicName, int qos, byte[] payload) throws Throwable {
		// Use a state machine to decide which step to do next. State change
		// occurs
		// when a notification is received that an MQTT action has completed
		state = CONNECTED;

		while (state != FINISH) {
			switch (state) {
			case BEGIN:
				// Connect using a non-blocking connect
				break;
			case CONNECTED:
				// Publish using a non-blocking publisher
				Publisher pub = new Publisher();
				pub.doPublish(topicName, qos, payload);
				break;
			case PUBLISHED:
				state = FINISH; // exit after publishing, but not disconnect
				donext = true;
				break;
			case DISCONNECT:
				Disconnector disc = new Disconnector();
				disc.doDisconnect();
				break;
			case ERROR:
				throw ex;
			case DISCONNECTED:
				state = FINISH;
				donext = true;
				break;
			}

			waitForStateChange(WAIT_TIME);
		}
	}

	/**
	 * Wait for a maximum amount of time for a state change event to occur
	 * 
	 * @param maxTTW
	 *            maximum time to wait in milliseconds
	 * @throws MqttException
	 */
	private void waitForStateChange(int maxTTW) throws MqttException {
		synchronized (waiter) {
			if (!donext) {
				try {
					waiter.wait(maxTTW);
				} catch (InterruptedException e) {
					Utils.getLogger().log(Level.SEVERE,"timed out");
				}

				if (ex != null) {
					throw (MqttException) ex;
				}
			}
			donext = false;
		}
	}
	
	/**
	 * Wait for a maximum amount of time for a response to occur
	 * 
	 * @param maxTTW
	 *            maximum time to wait in milliseconds
	 * @throws MqttException
	 */
	private void waitForResponse(int maxTTW) throws MqttException {
		synchronized (waiterResponse) {
			
			try {
				waiterResponse.wait(maxTTW);
			} catch (InterruptedException e) {
				Utils.getLogger().log(Level.SEVERE,"response timed out", e);
			}

			if (ex != null) {
				throw (MqttException) ex;
			}
		}
	}

	/**
	 * Subscribe to a topic on an MQTT server
	 * 
	 * @param topicName
	 *            to subscribe to (can be wild carded)
	 * @param qos
	 *            the maximum quality of service to receive messages at for this
	 *            subscription
	 * @throws MqttException
	 */
	public void subscribe(String topicName, int qos) throws Throwable {
		// Use a state machine to decide which step to do next. State change
		// occurs
		// when a notification is received that an MQTT action has completed
		state = CONNECTED;

		while (state != FINISH) {
			switch (state) {
			case BEGIN:
				break;
			case CONNECTED:
				// Subscribe using a non-blocking subscribe
				Subscriber sub = new Subscriber();
				sub.doSubscribe(topicName, qos);
				break;
			case SUBSCRIBED:
				state = FINISH;
				donext = true;
				break;
			case DISCONNECT:
				Disconnector disc = new Disconnector();
				disc.doDisconnect();
				break;
			case ERROR:
				throw ex;
			case DISCONNECTED:
				state = FINISH;
				donext = true;
				break;
			}
			waitForStateChange(WAIT_TIME);
		}
	}

	/**
	 * Unsubscribe to a topic on an MQTT server
	 * 
	 * @param topicName
	 *            to subscribe to (can be wild carded)
	 * @param qos
	 *            the maximum quality of service to receive messages at for this
	 *            subscription
	 * @throws MqttException
	 */
	public void unsubscribe(String topicName) throws Throwable {
		// a state machine to decide which step to do next. State change
		// occurs
		// when a notification is received that an MQTT action has completed
		state = CONNECTED;
	
		while (state != FINISH) {
			switch (state) {
			case BEGIN:
				break;
			case CONNECTED:
				// Subscribe using a non-blocking subscribe
				Unsubscriber unsub = new Unsubscriber();
				unsub.doUnsubscribe(topicName);
				state = FINISH;
				donext= true;
				break;
			case UNSUBSCRIBED:
				state = FINISH;
				donext = true;
				break;
			case DISCONNECT:
				Disconnector disc = new Disconnector();
				disc.doDisconnect();
				break;
			case ERROR:
				throw ex;
			case DISCONNECTED:
				state = FINISH;
				donext = true;
				break;
			}
			waitForStateChange(WAIT_TIME);
		}
	}

	/****************************************************************/
	/* Methods to implement the MqttCallback interface */
	/****************************************************************/
	
	/**
	 * Called when connection is completed
	 */
	@Override
	public void connectComplete(boolean arg0, String arg1) {
		GlobalCalls.isConnectedNotification.set(true);
		Utils.getLogger().log(Level.FINEST,"connectCompleted");
	}
	
	/**
	 * @see MqttCallback#connectionLost(Throwable)
	 */
	public void connectionLost(Throwable cause) {
		// Called when the connection to the server has been lost.
		// An application may choose to implement reconnection
		// Utils.logic at this point. This sample simply exits.
		Utils.getLogger().log(Level.SEVERE,"Connection lost! " + cause);
		GlobalCalls.isConnectedNotification.set(false);
	}

	/**
	 * @see MqttCallback#deliveryComplete(IMqttDeliveryToken)
	 */
	public void deliveryComplete(IMqttDeliveryToken token) {
		
		// note that token.getTopics() returns an array so we convert to a
		// string
		// before printing it on the console
		Utils.getLogger().log(Level.FINEST,"Delivery complete callback: Publish Completed " + Arrays.toString(token.getTopics()));
	}

	/**
	 * @see MqttCallback#messageArrived(String, MqttMessage)
	 */
	public void messageArrived(String topic, MqttMessage message) throws MqttException  {
		// Called when a message arrives from the server that matches any
		// subscription made by the client
		String time = new Timestamp(System.currentTimeMillis()).toString();
		String msg = new String(message.getPayload());
		Utils.getLogger().log(Level.FINEST,"Time:\t" + time + "  Topic:\t" + topic + "  Message:\t" + msg + "  QoS:\t" + message.getQos());
 
		// get the status and desktop token
		if (topic.compareTo(subRespTopic) == 0) {

			// parse the JSON object received
			JSONParser parser = new JSONParser();

			Object obj = null;
			try {
				obj = parser.parse(msg);
			} catch (ParseException e) {
				Utils.getLogger().log(Level.SEVERE,"desktop token: parse error", e);
			}

			try {
				JSONObject messageObject = (JSONObject) obj;

				// get the status
				registrationStatus = (boolean) messageObject.get(KEY_STATUS);
				JSONObject dataObject = (JSONObject) messageObject.get(KEY_DATA);

				// set the notification topic to subscribe.
				if (registrationStatus) {
					desktopToken = (String) dataObject.get(KEY_DESKTOP_TOKEN);
					subNotTopic = desktopToken;
				}
				Utils.getLogger().log(Level.FINEST, KEY_STATUS + ": " + registrationStatus);
				Utils.getLogger().log(Level.FINEST, KEY_DESKTOP_TOKEN + " : " + desktopToken);
				
				// stop waiting for response functions
				synchronized (waiterResponse){
					waiterResponse.notifyAll();
				}
				
			} catch (Exception e) {
				Utils.getLogger().log(Level.SEVERE,"json parse exception", e);
			}
		}

		// if subscribed to notifications
		if ( (subNotTopic != null) && (topic.compareTo(subNotTopic) == 0) ) {
			
			Utils.getLogger().log(Level.INFO,"Received new notification...");
			// parse the JSON object received
			JSONParser parser = new JSONParser();

			Object obj = null;
			try {
				obj = parser.parse(msg);
			} catch (ParseException e) {
				Utils.getLogger().log(Level.SEVERE,"notification: parse error", e);
			}

			try {
				JSONObject messageObject = (JSONObject) obj;

				// get the notification parameters
				String rcvHashOfTxid 	= (String) messageObject.get(KEY_HASH_TXID);
				String tag 				= (String) messageObject.get(KEY_TAG);
				String receiverAddress 	= (String) messageObject.get(KEY_REC_ADDRESS);
				String senderAddress 	= (String) messageObject.get(KEY_SND_ADDRESS);
				
				// add to database
				DbConnection connection=new DbConnection();
				if(!GlobalCalls.isNullOrEmptyStringCheck(deviceToken) && !GlobalCalls.isNullOrEmptyStringCheck(rcvHashOfTxid)
						&& !GlobalCalls.isNullOrEmptyStringCheck(senderAddress)){
					connection.addNotificationDta(deviceToken.trim(), senderAddress.trim(), rcvHashOfTxid.trim(),tag,receiverAddress.trim());
				}
				// add items to menu
				GlobalCalls.list.add(senderAddress);
				
			} catch (Exception e) {
				Utils.getLogger().log(Level.SEVERE,"json parse exception", e);
			}
		}
	}

	/**
	 * Connect in a non-blocking way and then sit back and wait to be notified
	 * that the action has completed.
	 */
	public class MqttConnector {

		public MqttConnector() {
		}

		public void doConnect() {
			// Connect to the server
			// Get a token and setup an asynchronous listener on the token which
			// will be notified once the connect completes
			Utils.getLogger().log(Level.FINEST,"Connecting to " + brokerUrl + " with client ID " + client.getClientId());

			IMqttActionListener conListener = new IMqttActionListener() {
				public void onSuccess(IMqttToken asyncActionToken) {
					Utils.getLogger().log(Level.FINEST,"Connected");
					state = CONNECTED;
					carryOn();
				}

				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					ex = exception;
					state = ERROR;
					Utils.getLogger().log(Level.SEVERE,"connect failed" + exception);
					carryOn();
				}

				public void carryOn() {
					synchronized (waiter) {
						donext = true;
						waiter.notifyAll();
					}
				}
			};

			try {
				// Connect using a non-blocking connect
				client.connect(conOpt, null, conListener);
			} catch (MqttException e) {
				// If though it is a non-blocking connect an exception can be
				// thrown if validation of parms fails or other checks such
				// as already connected fail.
				state = ERROR;
				donext = true;
				ex = e;
			}
		}
	}

	/**
	 * Publish in a non-blocking way and then sit back and wait to be notified
	 * that the action has completed.
	 */
	public class Publisher {
		public void doPublish(String topicName, int qos, byte[] payload) {
			// Send / publish a message to the server
			// Get a token and setup an asynchronous listener on the token which
			// will be notified once the message has been delivered
			MqttMessage message = new MqttMessage(payload);
			message.setQos(qos);

			String time = new Timestamp(System.currentTimeMillis()).toString();
			Utils.getLogger().log(Level.FINEST,"Publishing at: " + time + " to topic \"" + topicName + "\" qos " + qos);

			// Setup a listener object to be notified when the publish
			// completes.
			//
			IMqttActionListener pubListener = new IMqttActionListener() {
				public void onSuccess(IMqttToken asyncActionToken) {
					Utils.getLogger().log(Level.FINEST,"Publish Completed");
					state = PUBLISHED;
					carryOn();
				}

				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					ex = exception;
					state = ERROR;
					Utils.getLogger().log(Level.SEVERE,"Publish failed" + exception);
					carryOn();
				}

				public void carryOn() {
					synchronized (waiter) {
						donext = true;
						waiter.notifyAll();
					}
				}
			};

			try {
				// Publish the message
				client.publish(topicName, message, null, pubListener);
			} catch (MqttException e) {
				state = ERROR;
				donext = true;
				ex = e;
			}
		}
	}

	/**
	 * Subscribe in a non-blocking way and then sit back and wait to be notified
	 * that the action has completed.
	 */
	public class Subscriber {
		public void doSubscribe(String topicName, int qos) {
			// Make a subscription
			// Get a token and setup an asynchronous listener on the token which
			// will be notified once the subscription is in place.
			Utils.getLogger().log(Level.FINEST,"Subscribing to topic \"" + topicName + "\" qos " + qos);

			IMqttActionListener subListener = new IMqttActionListener() {
				public void onSuccess(IMqttToken asyncActionToken) {
					Utils.getLogger().log(Level.FINEST,"Subscribe Completed");
					state = SUBSCRIBED;
					carryOn();
				}

				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					ex = exception;
					state = ERROR;
					Utils.getLogger().log(Level.SEVERE,"Subscribe failed" + exception);
					carryOn();
				}

				public void carryOn() {
					synchronized (waiter) {
						donext = true;
						waiter.notifyAll();
					}
				}
			};

			try {
				client.subscribe(topicName, qos, null, subListener);
			} catch (MqttException e) {
				state = ERROR;
				donext = true;
				ex = e;
			}
		}
	}

	/**
	 * Unsubscribe in a non-blocking way and then sit back and wait to be
	 * notified that the action has completed.
	 */
	public class Unsubscriber {
		public void doUnsubscribe(String topicName) {
			// Make a unsubscription
			// Get a token and setup an asynchronous listener on the token which
			// will be notified once the subscription is in place.
			Utils.getLogger().log(Level.FINEST,"Unsubscribing to topic \"" + topicName + "\"");

			IMqttActionListener unsubListener = new IMqttActionListener() {
				public void onSuccess(IMqttToken asyncActionToken) {
					Utils.getLogger().log(Level.FINEST,"Unsubscribe Completed");
					state = UNSUBSCRIBED;
					carryOn();
				}

				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					ex = exception;
					state = ERROR;
					Utils.getLogger().log(Level.SEVERE,"Unsubscribe failed" + exception);
					carryOn();
				}

				public void carryOn() {
					synchronized (waiter) {
						donext = true;
						waiter.notifyAll();
					}
				}
			};

			try {
				client.unsubscribe(topicName, null, unsubListener);
			} catch (MqttException e) {
				state = ERROR;
				donext = true;
				ex = e;
			}
		}
	}

	/**
	 * Disconnect in a non-blocking way and then sit back and wait to be
	 * notified that the action has completed.
	 */
	public class Disconnector {
		public void doDisconnect() {
			// Disconnect the client
			Utils.getLogger().log(Level.FINEST,"Disconnecting");

			IMqttActionListener discListener = new IMqttActionListener() {
				public void onSuccess(IMqttToken asyncActionToken) {
					Utils.getLogger().log(Level.FINEST,"Disconnect Completed");
					state = DISCONNECTED;
					carryOn();
				}

				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					ex = exception;
					state = ERROR;
					Utils.getLogger().log(Level.SEVERE,"Disconnect failed" + exception);
					carryOn();
				}

				public void carryOn() {
					synchronized (waiter) {
						donext = true;
						waiter.notifyAll();
					}
				}
			};

			try {
				client.disconnect(null, discListener);
			} catch (MqttException e) {
				state = ERROR;
				donext = true;
				ex = e;
				Utils.getLogger().log(Level.SEVERE,"error in disconnect", e);
			}
		}
	}
	
	/**
	 * Gets the desktop client id from registration token
	 * @param registrationToken
	 * @return
	 * @throws NoSuchAlgorithmException 
	 */
	String getDesktopClientID(String registrationToken) throws NoSuchAlgorithmException{
		// convert UUID String to bytes
		UUID uuidOfToken = null;
		try {
			uuidOfToken = UUID.fromString(registrationToken);
		} catch (Exception e1) {
			Utils.getLogger().log(Level.SEVERE,"failed to parse bitvault-token", e1);
		}
		long hi = uuidOfToken.getMostSignificantBits();
		long lo = uuidOfToken.getLeastSignificantBits();
		byte[] uuidInBytes = ByteBuffer.allocate(16).putLong(hi).putLong(lo).array();
		// get x2 hash for clientID
		String HashOfBitvaultToken = null;
		try {
			HashOfBitvaultToken = Hex.toHexString(doubleHash(uuidInBytes));
			Utils.getLogger().log(Level.FINEST,"hash of token : " + HashOfBitvaultToken);
		} catch (NoSuchAlgorithmException e) {
			throw e;
		}
		return HashOfBitvaultToken;
	}
	/**
	 * Hashes the input twice
	 * 
	 * @param txId
	 * @return
	 */
	public byte[] doubleHash(byte[] hexValue) throws NoSuchAlgorithmException {

		MessageDigest digest = null;
		byte[] hash = null;

		try {
			digest = MessageDigest.getInstance("SHA-256");
			hash = digest.digest(digest.digest(hexValue));
		} catch (NoSuchAlgorithmException e) {
			throw e;
		}
		return hash;
	}
}
