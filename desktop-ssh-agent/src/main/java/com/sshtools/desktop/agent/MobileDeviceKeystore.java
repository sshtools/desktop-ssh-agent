/**
 * (c) 2002-2023 JADAPTIVE Limited. All Rights Reserved.
 *
 * This file is part of the Desktop SSH Agent.
 *
 * Desktop SSH Agent is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Desktop SSH Agent is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Desktop SSH Agent.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.sshtools.desktop.agent;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.hypersocket.json.JsonClient;
import com.hypersocket.json.JsonResponse;
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
import com.sshtools.common.util.IOUtils;

public class MobileDeviceKeystore implements KeyStore {

	JsonClient client;
	DesktopAgent agent;
	MobileDeviceKeystoreListener listener;
	KeyStore localKeystore;

	public MobileDeviceKeystore(DesktopAgent agent,
			KeyStore localKeystore) throws IOException {
		this.agent = agent;
		this.localKeystore = localKeystore;
	}
	
	public void setListener(MobileDeviceKeystoreListener listener) {
		this.listener = listener;
	}
	
	public boolean ping() {
		
		try {
			verifyClient();
			
			client.doGet("api/server/ping");
			return true;
		} catch (Throwable e) {
			return false;
		}
	}
	
	public boolean verify() {
		
		try {
			verifyClient();
			
			JsonResponse response = client.doPost("api/server/ping", JsonResponse.class);
					
			return response.isSuccess();
		} catch (Throwable e) {
			return false;
		}
	}
	
	private void verifyClient() throws IOException {
		if(Objects.isNull(client)) {
			this.client = new JsonClient(Settings.getInstance().getLogonboxDomain(), 
					Settings.getInstance().getLogonboxPort(), !Settings.getInstance().isStrictSSL(), false);
			this.client.setPath("/app");
		}
	}
	
	@Override
	public Map<SshPublicKey, String> getPublicKeys() {
		
		Map<SshPublicKey, String> results = new HashMap<>();
		if(ping()) {
			results.putAll(getDeviceKeys(false));	
		}
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
	
	public Map<SshPublicKey, String> getDeviceKeys(boolean reconnect) {
		
		if(reconnect) {
			client = null;
		}
		
		if(StringUtils.isAnyBlank(Settings.getInstance().getLogonboxUsername(),
				Settings.getInstance().getLogonboxDomain())) {
			return Collections.emptyMap();
		}
		
 		Map<SshPublicKey, String> results = new HashMap<>();
		
		try(InputStream in = IOUtils.toInputStream(
				getClient().doGet("api/authenticator/authorizedKeys/" + Settings.getInstance().getLogonboxUsername()), "UTF-8")) {
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			String key;
			while((key = reader.readLine())!=null) {
				if(key.trim().startsWith("#")) {
					continue;
				}
				SshPublicKeyFile kf = SshPublicKeyFileFactory.parse(key.getBytes("UTF-8"));
				SshPublicKey pub = kf.toPublicKey();
				results.put(pub, StringUtils.defaultIfBlank(kf.getComment(), "LogonBox Key"));
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
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean addKey(SshKeyPair pair, String description, KeyConstraints cs) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean deleteAllKeys() {
		throw new UnsupportedOperationException();
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
			Log.info("Performing sign operation for {} with payload {}", pubkey.getFingerprint(), payload);
		}
		
		try {
			JsonSignRequestStatus request = getClient().doPost("api/authenticator/signPayload", JsonSignRequestStatus.class,
					new RequestParameter("username", Settings.getInstance().getLogonboxUsername()),
					new RequestParameter("remoteName", "Desktop Agent"),
					new RequestParameter("authorizeText", "Login"),
					new RequestParameter("flags", String.valueOf(flags)),
					new RequestParameter("fingerprint", pubkey.getFingerprint()),
					new RequestParameter("payload", payload));
				
			if(Log.isInfoEnabled()) {
				Log.info("Received response from {}", pubkey.getFingerprint());
			}
			
			if(request.isSuccess()) {
				if(Log.isInfoEnabled()) {
					Log.info("Received sign operation for {} with response {}", pubkey.getFingerprint(), request.getSignature());
				}
				return Base64.getUrlDecoder().decode(request.getSignature());
			}
			
			if(Log.isInfoEnabled()) {
				Log.info("Received  failed response from {}", pubkey.getFingerprint());
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

	public void deleteTemporaryKeys() {
		
		localKeystore.deleteAllKeys();
		
		if(listener!=null) {
			listener.onKeysChanged();
		}
	}

	public boolean isDeviceKey(SshPublicKey key) {
		return getDeviceKeys(false).containsKey(key);
	}

	public String getKeyName(SshPublicKey key) {
		Map<SshPublicKey, String> tmp = getDeviceKeys(false);
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
}
