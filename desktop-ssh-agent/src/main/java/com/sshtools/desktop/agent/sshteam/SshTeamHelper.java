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

import com.sshtools.agent.KeyConstraints;
import com.sshtools.agent.KeyStore;
import com.sshtools.agent.exceptions.KeyTimeoutException;
import com.sshtools.common.logger.Log;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.SshException;
import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.ByteArrayWriter;
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
		
		return doRequest(String.format("https://%s:%d/app/api/authorizedKeys/list", hostname, port), params);
	}



	public static String getPolicy(String username, String hostname, int port, SshPublicKey publicKey, KeyStore sign) throws NoSuchAlgorithmException, IOException, InterruptedException, URISyntaxException, SshException, KeyTimeoutException {
		
		long nonce = SecureRandom.getInstanceStrong().nextLong();
		
		String key = SshKeyUtils.getOpenSSHFormattedKey(publicKey);
		Map<String,String> params = new HashMap<>();
		params.put("username", username);
		params.put("nonce", String.valueOf(nonce));
		params.put("authorizationKey", key);
		params.put("authorization", generateAuthorization(username, sign, publicKey, nonce, key));
		
		return doRequest(String.format("https://%s:%d/app/api/authorizedKeys/policy", hostname, port), params);
		
	}
	
	
	public static String addKey(String username, String hostname, int port, SshPublicKey publicKey, KeyStore sign, String name, SshPublicKey newKey) throws NoSuchAlgorithmException, IOException, InterruptedException, URISyntaxException, SshException, KeyTimeoutException {
		
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
		
		return doRequest(String.format("https://%s:%d/app/api/authorizedKeys/add", hostname, port), params);
	}
	
	public static String removeKey(String username, String hostname, int port, SshPublicKey publicKey, KeyStore sign, String name, SshPublicKey newKey) throws NoSuchAlgorithmException, IOException, InterruptedException, URISyntaxException, SshException, KeyTimeoutException {
		
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
		
		return doRequest(String.format("https://%s:%d/app/api/authorizedKeys/remove", hostname, port), params);
	}


	private static String generateAuthorization(String username, KeyStore keystore, SshPublicKey publicKey, long nonce, String... other) throws IOException, SshException, KeyTimeoutException {
		
		try(ByteArrayWriter baw = new ByteArrayWriter()) {
			baw.writeString(username);
			baw.writeUINT64(nonce);
			
			for(String o : other) {
				baw.writeString(o);
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


	private static String doRequest(String url, Map<String,String> params) throws IOException, InterruptedException, URISyntaxException {
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
		return response.body().toString();
	}



	public static Collection<SshPublicKey> verifyAccess(String username, String hostname, int port, KeyStore keystore) {
		
		List<SshPublicKey> results = new ArrayList<>();
		
		for(SshPublicKey key : keystore.getPublicKeys().keySet()) {
			KeyConstraints c = keystore.getKeyConstraints(key);
			try {
				getPolicy(username, hostname, port, key, keystore);
				Log.info("Found existing key {} on ssh.team domain", SshKeyUtils.getFingerprint(key));
				results.add(key);
				
				c.setSSH1Compatible(true);
			} catch(Throwable e) {
				Log.info("Key {} is not present on ssh.team domain", SshKeyUtils.getFingerprint(key));
				c.setSSH1Compatible(false);
			}
		}
		
		return results;
	}
}
