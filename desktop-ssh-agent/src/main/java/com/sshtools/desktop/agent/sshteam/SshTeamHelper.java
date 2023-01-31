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
package com.sshtools.desktop.agent.sshteam;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hypersocket.json.JsonRequestStatus;
import com.sshtools.agent.KeyStore;
import com.sshtools.agent.exceptions.KeyTimeoutException;
import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshKeyPair;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.ByteArrayWriter;
import com.sshtools.desktop.agent.ExtendedKeyInfo;
import com.sshtools.synergy.ssh.SshContext;

public class SshTeamHelper {

		
	public static String getAuthorizedKeys(String username, String hostname, int port, SshPublicKey publicKey, KeyStore sign) throws NoSuchAlgorithmException, IOException, InterruptedException, URISyntaxException, SshException, KeyTimeoutException {
		
		long nonce = SecureRandom.getInstanceStrong().nextLong();
		
		String key = SshKeyUtils.getOpenSSHFormattedKey(publicKey);
		Map<String,String> params = new HashMap<>();
		params.put("username", username);
		params.put("nonce", String.valueOf(nonce));
		params.put("authorizationKey", key);
		params.put("authorization", generateAuthorization(username, sign, publicKey, nonce, key));
		
		return doRequestString(String.format("https://%s:%d/app/api/authorizedKeys/list", hostname, port), params);
	}

	public static SshTeamPolicy getPolicy(String username, String hostname, int port, SshPublicKey publicKey, KeyStore sign) throws NoSuchAlgorithmException, IOException, InterruptedException, URISyntaxException, SshException, KeyTimeoutException {
		
		long nonce = SecureRandom.getInstanceStrong().nextLong();
		
		String key = SshKeyUtils.getOpenSSHFormattedKey(publicKey);
		Map<String,String> params = new HashMap<>();
		params.put("username", username);
		params.put("nonce", String.valueOf(nonce));
		params.put("authorizationKey", key);
		params.put("authorization", generateAuthorization(username, sign, publicKey, nonce, key));
		
		String result = doRequestString(String.format("https://%s:%d/app/api/authorizedKeys/policy", hostname, port), params);
		
		ObjectMapper mapper = new ObjectMapper();
		SshTeamPolicyStatus policy = mapper.readValue(result, SshTeamPolicyStatus.class);
		
		if(!policy.isSuccess()) {
			throw new IOException(policy.getMessage());
		}
		return policy.getResource();
	}
	
	public static boolean checkKey(String username, String hostname, int port, SshKeyPair pair) {

		try {
			long nonce = SecureRandom.getInstanceStrong().nextLong();
			
			String key = SshKeyUtils.getOpenSSHFormattedKey(pair.getPublicKey());
			Map<String,String> params = new HashMap<>();
			params.put("username", username);
			params.put("nonce", String.valueOf(nonce));
			params.put("authorizationKey", key);
			params.put("authorization", generateAuthorization(username, pair, nonce, key));
			
			String result = doRequestString(String.format("https://%s:%d/app/api/authorizedKeys/policy", hostname, port), params);
			
			ObjectMapper mapper = new ObjectMapper();
			SshTeamPolicyStatus policy = mapper.readValue(result, SshTeamPolicyStatus.class);
			
			if(!policy.isSuccess()) {
				throw new IOException(policy.getMessage());
			}
			return true;
		} catch(Throwable e) {
			Log.error("Check for ssh.team synchronization failed", e);
			return false;
		}
	}
	
	
	public static void addKey(String username, String hostname, int port, SshPublicKey publicKey, KeyStore sign, String name, SshPublicKey newKey) throws NoSuchAlgorithmException, IOException, InterruptedException, URISyntaxException, SshException, KeyTimeoutException {
		
		long nonce = SecureRandom.getInstanceStrong().nextLong();
		
		String key = SshKeyUtils.getOpenSSHFormattedKey(publicKey);
		String pk =  SshKeyUtils.getOpenSSHFormattedKey(newKey);
		Map<String,String> params = new HashMap<>();
		params.put("username", username);
		params.put("nonce", String.valueOf(nonce));
		params.put("authorizationKey", key);
		params.put("authorization", generateAuthorization(username, sign, publicKey, nonce, name, pk));
		params.put("name", name);
		params.put("publicKey", pk);
		
		doRequest(String.format("https://%s:%d/app/api/authorizedKeys/add", hostname, port), params);
	}
	
	public static void removeKey(String username, String hostname, int port, SshPublicKey publicKey, KeyStore sign, String name, SshPublicKey newKey) throws NoSuchAlgorithmException, IOException, InterruptedException, URISyntaxException, SshException, KeyTimeoutException {
		
		long nonce = SecureRandom.getInstanceStrong().nextLong();
		
		String key = SshKeyUtils.getOpenSSHFormattedKey(publicKey);
		String pk =  SshKeyUtils.getOpenSSHFormattedKey(newKey);
		Map<String,String> params = new HashMap<>();
		params.put("username", username);
		params.put("nonce", String.valueOf(nonce));
		params.put("authorizationKey", key);
		params.put("authorization", generateAuthorization(username, sign, publicKey, nonce, name, pk));
		params.put("name", name);
		params.put("publicKey", pk);
		
		doRequest(String.format("https://%s:%d/app/api/authorizedKeys/remove", hostname, port), params);
	}


	private static String generateAuthorization(String username, KeyStore keystore, SshPublicKey publicKey, long nonce, String... other) throws IOException, SshException, KeyTimeoutException {
		
		if(Log.isInfoEnabled()) {
			Log.info("Generating authorization for {}", username);
			Log.info("Nonce {}", String.valueOf(nonce));
		}
		try(ByteArrayWriter baw = new ByteArrayWriter()) {
			baw.writeString(username);
			baw.writeUINT64(nonce);
			
			for(String o : other) {
				if(Log.isInfoEnabled()) {
					Log.info("And {}", o);
				}
				baw.writeString(o);
			}
			
			if(Log.isInfoEnabled()) {
				Log.info("Signing data {}", Base64.getUrlEncoder().encodeToString(baw.toByteArray()));
				Log.info("With key {}", SshKeyUtils.getFingerprint(publicKey));
			}
			int flags = 0;
			switch(publicKey.getSigningAlgorithm()) {
			case SshContext.PUBLIC_KEY_RSA_SHA256:
				flags = 1;
				break;
			case SshContext.PUBLIC_KEY_RSA_SHA512:
				flags = 2;
				break;
			default:
				break;
			}
			byte[] sig = keystore.performHashAndSign(publicKey, Collections.emptyList(), baw.toByteArray(), flags);
			return Base64.getUrlEncoder().encodeToString(sig);
		}
	}
	
	private static String generateAuthorization(String username, SshKeyPair pair, long nonce, String... other) throws IOException, SshException, KeyTimeoutException {
		
		try(ByteArrayWriter baw = new ByteArrayWriter()) {
			baw.writeString(username);
			baw.writeUINT64(nonce);
			
			for(String o : other) {
				baw.writeString(o);
			}
			
			byte[] sig;
			switch(pair.getPublicKey().getSigningAlgorithm()) {
			case SshContext.PUBLIC_KEY_RSA_SHA256:
				sig = pair.getPrivateKey().sign(baw.toByteArray(), SshContext.PUBLIC_KEY_RSA_SHA256);
				break;
			case SshContext.PUBLIC_KEY_RSA_SHA512:
				sig = pair.getPrivateKey().sign(baw.toByteArray(), SshContext.PUBLIC_KEY_RSA_SHA512);
				break;
			default:
				sig = pair.getPrivateKey().sign(baw.toByteArray(), pair.getPublicKey().getSigningAlgorithm());
				break;
			}
			
			return Base64.getUrlEncoder().encodeToString(sig);
		}
	}


	private static String doRequestString(String url, Map<String,String> params) throws IOException, InterruptedException, URISyntaxException {
		String form = params.entrySet()
			    .stream()
			    .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
			    .collect(Collectors.joining("&"));
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(new URI(url))
			    .headers("Content-Type", "application/x-www-form-urlencoded")
			    .POST(HttpRequest.BodyPublishers.ofString(form))
			    .build();

		HttpResponse<?> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		if(response.statusCode()!=200) {
			throw new IOException(url + " returned " + response.statusCode());
		}
		return response.body().toString();
	}
	
	private static void doRequest(String url, Map<String,String> params) throws IOException, InterruptedException, URISyntaxException {
		
		ObjectMapper mapper = new ObjectMapper();
		JsonRequestStatus status = mapper.readValue(doRequestString(url, params), JsonRequestStatus.class);
	
		if(!status.isSuccess()) {
			throw new IOException(status.getMessage());
		} 
	}



	public static Collection<SshPublicKey> verifyAccess(String username, String hostname, int port, KeyStore keystore) {
		
		List<SshPublicKey> results = new ArrayList<>();
		
		for(SshPublicKey key : keystore.getPublicKeys().keySet()) {
			ExtendedKeyInfo c = (ExtendedKeyInfo) keystore.getKeyConstraints(key);
			c.setName(keystore.getPublicKeys().get(key));
			try {
				getPolicy(username, hostname, port, key, keystore);
				Log.info("Found existing key {} on ssh.team domain", SshKeyUtils.getFingerprint(key));
				results.add(key);
				
				c.setTeamKey(true);
				
			} catch(Throwable e) {
				Log.info("Key {} is not present on ssh.team domain", SshKeyUtils.getFingerprint(key));
				c.setTeamKey(false);
			}
		}
		
		return results;
	}
}
