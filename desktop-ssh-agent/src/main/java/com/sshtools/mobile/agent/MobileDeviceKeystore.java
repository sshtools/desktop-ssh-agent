package com.sshtools.mobile.agent;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.hypersocket.json.JsonClient;
import com.hypersocket.json.JsonPrivateKey;
import com.hypersocket.json.JsonPrivateKeyList;
import com.hypersocket.json.JsonResourceStatus;
import com.hypersocket.json.JsonStatusException;
import com.hypersocket.json.RequestParameter;
import com.sshtools.agent.ForwardingNotice;
import com.sshtools.agent.KeyConstraints;
import com.sshtools.agent.KeyStore;
import com.sshtools.agent.exceptions.KeyTimeoutException;
import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.publickey.SshPublicKeyFile;
import com.sshtools.common.publickey.SshPublicKeyFileFactory;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPrivateKey;
import com.sshtools.common.ssh.components.SshPublicKey;

public class MobileDeviceKeystore implements KeyStore {

	
	
	JsonClient client;
	String username;
	String authorization;
	String remoteName;
	String hostname;
	int port;
	boolean strictSSL;
	
	MobileDeviceKeystoreListener listener;
	KeyStore localKeystore;
	
	
	public MobileDeviceKeystore(String hostname, int port, boolean strictSSL, String username, String remoteName, String authorization,
			KeyStore localKeystore) throws IOException {
		this.username = username;
		this.authorization = authorization;
		this.remoteName = remoteName;
		this.hostname = hostname;
		this.port = port;
		this.strictSSL = strictSSL;
		this.localKeystore = localKeystore;
	}
	
	public void setListener(MobileDeviceKeystoreListener listener) {
		this.listener = listener;
	}
	
	public boolean ping() {
		
		try {
			verifyClient();
			/**
			 * We don't care if its not logged on, just that it can be contacted.
			 */
			client.isLoggedOn();
			return true;
		} catch (Throwable e) {
			return false;
		}
	}
	
	private void verifyClient() throws IOException {
		if(Objects.isNull(client)) {
			this.client = new JsonClient(hostname, port, !strictSSL);
		}
	}
	
	@Override
	public Map<SshPublicKey, String> getPublicKeys() {
		
		Map<SshPublicKey, String> results = getDeviceKeys();
		results.putAll(localKeystore.getPublicKeys());
		return results;
	}
	
	public Map<SshPublicKey, String> getLocalKeys() {
		return localKeystore.getPublicKeys();
	}

	protected JsonClient getClient() throws IOException {
		verifyClient();
		return client;
	}
	
	public Map<SshPublicKey, String> getDeviceKeys() {
		
		Map<SshPublicKey, String> results = new HashMap<>();
		
		try(InputStream in = IOUtils.toInputStream(
				getClient().doPost("/authorizedKeys/" + username, 
						new RequestParameter("token", authorization)), "UTF-8")) {
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String key;
			while((key = reader.readLine())!=null) {
				SshPublicKeyFile kf = SshPublicKeyFileFactory.parse(key.getBytes("UTF-8"));
				SshPublicKey pub = kf.toPublicKey();
				results.put(pub, kf.getComment());
			}
			
		} catch(JsonStatusException e) { 
			if(e.getStatusCode()==403) {
				throw new IllegalStateException("This device has not been authorized to access the users account.");
			} else {
				throw new IllegalStateException(e.getMessage(), e);
			}
		} catch(IOException e ) {
			Log.error("Failed to list authorized keys", e);
			throw new IllegalStateException(e.getMessage(), e);
		} 
		
		return results;
	}

	public  List<JsonConnection> getConnections() {
		
		try {
		
			JsonConnectionList connections = getClient().doPost(
					"api/serverConnections/myServerConnections", 
						JsonConnectionList.class,
						new RequestParameter("username", username),
						new RequestParameter("token", authorization));
			
			if(!connections.isSuccess()) {
				throw new IllegalStateException(connections.getError());
			}
			
			List<JsonConnection> cons = new ArrayList<JsonConnection>();
			cons.addAll(Arrays.asList(connections.getResources()));
			
			return cons;
			
		} catch(JsonStatusException e) { 
			if(e.getStatusCode()==403) {
				throw new IllegalStateException("This device has not been authorized to access the users account.");
			} else {
				throw new IllegalStateException(e.getMessage(), e);
			}
		} catch(IOException e ) {
			Log.error("Failed to list connections", e);
			throw new IllegalStateException(e.getMessage(), e);
		} 
	}
	
	@Override
	public KeyConstraints getKeyConstraints(SshPublicKey key) {
		KeyConstraints ks = localKeystore.getKeyConstraints(key);
		if(!Objects.isNull(ks)) {
			return ks;
		}
		return new KeyConstraints();
	}

	@Override
	public int size() {
		return getPublicKeys().size();
	}

	@Override
	public boolean addKey(SshPrivateKey prvkey, SshPublicKey pubkey, String description, KeyConstraints cs) {
		
		try {
			
			Map<SshPublicKey, String> deviceKeys = getDeviceKeys();
			if(deviceKeys.containsKey(pubkey)) {
				Log.error(String.format("The key %s is already installed as a device key", pubkey.getFingerprint()));
				return false;
			}
			if(listener!=null) {
				return listener.addKey(prvkey, pubkey, description, cs);
			} else {
				return false;
			}
		} catch (IOException | SshException e) {
			Log.error("Failed to process addKey", e);
			return false;
		}
	}

	@Override
	public boolean addKey(SshKeyPair pair, String description, KeyConstraints cs) {
		
		try {
			Map<SshPublicKey, String> deviceKeys = getDeviceKeys();
			if(deviceKeys.containsKey(pair.getPublicKey())) {
				Log.error(String.format("The key %s is already installed as a device key", pair.getPublicKey().getFingerprint()));
				return false;
			}
			
			if(listener!=null) {
				return listener.addKey(pair.getPrivateKey(), pair.getPublicKey(), description, cs);
			} else {
				return false;
			}
		} catch (IOException | SshException e) {
			Log.error("Failed to process addKey", e);
			return false;
		}
	}

	public void addTemporaryKey(SshKeyPair pair,  String description, KeyConstraints cs) throws IOException {
		localKeystore.addKey(pair, description, cs);
		
		if(listener!=null) {
			listener.onKeysChanged();
		}
	}
	
	public void removeTemporaryKey(SshPublicKey key) throws IOException {
		
		localKeystore.deleteKey(key);
		
		if(listener!=null) {
			listener.onKeysChanged();
		}
	}
	
	@Override
	public boolean deleteAllKeys() {
		
		if(listener!=null) {
			return listener.deleteAllKeys();
		}
		
		return false;
	}

	@Override
	public byte[] performHashAndSign(SshPublicKey pubkey, List<ForwardingNotice> forwardingNodes, byte[] data, int flags)
			throws KeyTimeoutException, SshException {
		
		if(!ping()) {
			throw new SshException("Authentication gateway is not available!", SshException.AGENT_ERROR);
		}
		
		KeyConstraints kc = getKeyConstraints(pubkey);
		
		if(Objects.isNull(kc)) {
			throw new SshException("Key not in store", SshException.AGENT_ERROR);
		}
		
		if(!kc.canUse()) {
			throw new SshException("Key cannot be used", SshException.AGENT_ERROR);
		}
		
		if(kc.hasTimedOut()) {
			throw new KeyTimeoutException();
		}
		
		if(kc.requiresUserVerification()) {
			//  TODO  prompt user
		}
		
		kc.use();
		
		Map<SshPublicKey, String> temporaryKeys = localKeystore.getPublicKeys();
		if(temporaryKeys.containsKey(pubkey)) {
			return localKeystore.performHashAndSign(pubkey, forwardingNodes, data, flags);
		} else {
			return performDeviceHashAndSign(pubkey, forwardingNodes, data, flags);
		}
	}
	
	public byte[] performDeviceHashAndSign(SshPublicKey pubkey, List<ForwardingNotice> forwardingNodes, byte[] data, int flags)
			throws KeyTimeoutException, SshException {
		
		String payload = Base64.getUrlEncoder().encodeToString(data);
		
		if(Log.isInfoEnabled()) {
			Log.info(String.format("Performing sign operation for %s with payload %s", pubkey.getFingerprint(), payload));
		}
		
		try {
			JsonSignRequestStatus request = getClient().doPost("api/agent/signPayload", JsonSignRequestStatus.class,
					new RequestParameter("username", username),
					new RequestParameter("token", authorization),
					new RequestParameter("flags", String.valueOf(flags)),
					new RequestParameter("fingerprint", pubkey.getFingerprint()),
					new RequestParameter("remoteName", remoteName),
					new RequestParameter("payload", payload));
			
			if(Log.isInfoEnabled()) {
				Log.info("Received response from %s", pubkey.getFingerprint());
			}
			
			if(request.isSuccess()) {
				if(Log.isInfoEnabled()) {
					Log.info(String.format("Received sign operation for %s with response %s", pubkey.getFingerprint(), request.getSignature()));
				}
				return Base64.getUrlDecoder().decode(request.getSignature());
			}
			
			if(Log.isInfoEnabled()) {
				Log.info("Received  failed response from %s", pubkey.getFingerprint());
			}
			
			throw new SshException("Remote response returned unknown failure",
					SshException.AGENT_ERROR);
			
		} catch (IOException | JsonStatusException e) {
			System.err.println(e.getMessage());
			throw new SshException(e);
		}
		
		
	}

	@Override
	public boolean deleteKey(SshPublicKey pubkey) throws IOException {
		
		
		if(listener!=null) {
			return listener.deleteKey(pubkey);
		}
		
		return false;
	}

	@Override
	public boolean lock(String password) throws IOException {
		return false;
	}

	@Override
	public boolean unlock(String password) throws IOException {
		return false;
	}

	@Override
	public boolean isLocked() {
		return false;
	}
	
	public JsonClient getLoggedOnClient(PasswordPrompt prompt, int maxAttempts) throws IOException, JsonStatusException {
		
		verifyClient();
		
		if(client.isLoggedOn()) {
	        return client;
	    } else {
	    	IOException lastError = null;
	    	while(maxAttempts > 0) {
	    		try {
					client.logon(username, prompt.getPassword(username));
					return client;
				} catch (IOException e) {
					lastError = e;
				}
	    	}
	    	throw lastError;
	    	
	    }
	}

	public void deleteTemporaryKeys() {
		
		localKeystore.deleteAllKeys();
		
		if(listener!=null) {
			listener.onKeysChanged();
		}
	}

	public void deleteDeviceKey(SshPublicKey key, PasswordPrompt passwordPrompt, int maxAttempts) throws IOException, JsonStatusException {
		doDeleteDeviceKeys(passwordPrompt, maxAttempts, key);
	}
	
	public void deleteDeviceKeys(PasswordPrompt passwordPrompt, int maxAttempts) throws IOException, JsonStatusException {
		doDeleteDeviceKeys(passwordPrompt, maxAttempts, getDeviceKeys().keySet().toArray(new SshPublicKey[0]));
	}
	
	private void doDeleteDeviceKeys(PasswordPrompt prompt, int maxAttempts, SshPublicKey... keys) throws IOException, JsonStatusException {
		
		verifyClient();
		
		IOException lastError = null;
		
		if(!client.isLoggedOn()) {
	    	
	    	while(maxAttempts > 0) {
	    		try {
					client.logon(username, prompt.getPassword(username));
					break;
				} catch (IOException e) {
					lastError = e;
				}
	    		}
		}
	    	
	    	if(client.isLoggedOn()) {
	    		JsonPrivateKeyList results = client.doGet("api/userPrivateKeys/personal", JsonPrivateKeyList.class);
	
	        if(!results.isSuccess()) {
	            throw new IOException(results.getError());
	        }
	        
	        Map<SshPublicKey, Long> ids = new HashMap<SshPublicKey, Long>();
	        
        		for(SshPublicKey key : keys) {
        			for(JsonPrivateKey jsonKey : results.getResources()) {
	        			if(jsonKey.getFingerprint().equals(SshKeyUtils.getFingerprint(key))) {
	        				ids.put(key, jsonKey.getId());
	        				break;
	        			}
        			}
        		}
	        
        		for(Map.Entry<SshPublicKey,Long> entry : ids.entrySet()) {
        			try {
            			client.doDelete("api/userPrivateKeys/key/" + entry.getValue().toString() + "?fromDevice=false", JsonResourceStatus.class);
                } catch(JsonStatusException e) {
                    if(e.getStatusCode()==404) {
                        continue;
                    }
                    throw e;
                }
        		}
	        
	        if(listener!=null) {
	        		listener.onKeysChanged();
	        }
	    	} else {
	    		throw lastError;
	    	}
	}

	public boolean isDeviceKey(SshPublicKey key) {
		return getDeviceKeys().containsKey(key);
	}

	public String getKeyName(SshPublicKey key) {
		Map<SshPublicKey, String> tmp = getDeviceKeys();
		if(tmp.containsKey(key)) {
			return tmp.get(key);
		} 
		else {
			tmp = localKeystore.getPublicKeys();
			if(tmp.containsKey(key)) {
				return tmp.get(key);
			}
		}
		throw new IllegalStateException(String.format("No key name for ", SshKeyUtils.getFingerprint(key)));
	}

	public JsonConnection createConnection(String name, String hostname, Integer port, String remoteUsername, Set<String> aliases, Set<SshPublicKey> hostKeys) {
		
		try {
			
			Set<String> keys = new TreeSet<String>();
			for(SshPublicKey key : hostKeys) {
				keys.add(SshKeyUtils.getOpenSSHFormattedKey(key));
			}
			
			JsonConnectionResourceStatus result = getClient().doPost(
					"api/serverConnections/create", 
					JsonConnectionResourceStatus.class,
						new RequestParameter("name", name),
						new RequestParameter("hostname", hostname),
						new RequestParameter("port", String.valueOf(port)),
						new RequestParameter("remoteUsername", remoteUsername),
						new RequestParameter("username", username),
						new RequestParameter("aliases", StringUtils.join(aliases, ",")),
						new RequestParameter("hostKeys", StringUtils.join(keys, ",")),
						new RequestParameter("token", authorization));
			
			if(!result.isSuccess()) {
				throw new IllegalStateException(result.getMessage());
			}
			
			return result.getResource();
			
		} catch(JsonStatusException e) { 
			if(e.getStatusCode()==403) {
				throw new IllegalStateException("This device has not been authorized to access the users account.");
			} else {
				throw new IllegalStateException(e.getMessage(), e);
			}
		} catch(IOException e ) {
			Log.error("Failed to list connections", e);
			throw new IllegalStateException(e.getMessage(), e);
		} 
	}

	public void deleteConnection(Long id) throws IOException {
		
		verifyClient();
		
		try {
			client.doDelete("api/serverConnections/delete/" + id, JsonResourceStatus.class,
					new RequestParameter("username", username),
					new RequestParameter("token", authorization));
		} catch(JsonStatusException e) {
	        throw new IOException(e.getMessage(), e);
		}
		
	}

	public JsonConnection updateConnection(Long id, String name, String hostname, Integer port, String remoteUsername, Set<String> aliases, Set<SshPublicKey> hostKeys) {

		try {
			
			Set<String> keys = new TreeSet<String>();
			for(SshPublicKey key : hostKeys) {
				keys.add(SshKeyUtils.getOpenSSHFormattedKey(key));
			}
			
			JsonConnectionResourceStatus result = getClient().doPost(
					"api/serverConnections/update", 
					JsonConnectionResourceStatus.class,
					    new RequestParameter("id", String.valueOf(id)),
						new RequestParameter("name", name),
						new RequestParameter("hostname", hostname),
						new RequestParameter("port", String.valueOf(port)),
						new RequestParameter("remoteUsername", remoteUsername),
						new RequestParameter("username", username),
						new RequestParameter("aliases", StringUtils.join(aliases, ",")),
						new RequestParameter("hostKeys", StringUtils.join(keys, ",")),
						new RequestParameter("token", authorization));
			
			if(!result.isSuccess()) {
				throw new IllegalStateException(result.getError());
			}
			
			return result.getResource();
			
		} catch(JsonStatusException e) { 
			if(e.getStatusCode()==403) {
				throw new IllegalStateException("This device has not been authorized to access the users account.");
			} else {
				throw new IllegalStateException(e.getMessage(), e);
			}
		} catch(IOException e ) {
			Log.error("Failed to list connections", e);
			throw new IllegalStateException(e.getMessage(), e);
		} 
	}

	
}
