package com.sshtools.desktop.agent.sshteam;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.sshtools.common.ssh.components.SshPublicKey;
import com.sshtools.common.util.ByteArrayWriter;

public class SshTeamHelper {

	public static boolean verifyAccess(String username, String domain, int port, Collection<SshPublicKey> keys) {
		return false;
	}
	
	public static SshTeamPolicy getPolicy(String username, String domain, int port) {
		return null;
	}
	
	public static Collection<SshPublicKey> listKeys(String username, String domain, int port, SshPublicKey key) throws IOException {
		
		try {
			long nonce = SecureRandom.getInstanceStrong().nextLong();
			
			Map<String, String> parameters = new HashMap<>();
			parameters.put("username", username);
			parameters.put("authorizationKey", username);
			parameters.put("authorization", generateAuthorization(username, key, nonce));
			parameters.put("nonce", String.valueOf(nonce));
			parameters.put("account", username);
			parameters.put("account", username);
			String form = parameters.entrySet()
			    .stream()
			    .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
			    .collect(Collectors.joining("&"));
	
			HttpClient client = HttpClient.newHttpClient();
	
			HttpRequest request = HttpRequest.newBuilder()
			    .uri(URI.create(String.format("https://%s:%d/app/api/authorizedKeys/list", domain, port)))
			    .headers("Content-Type", "application/x-www-form-urlencoded")
			    .POST(HttpRequest.BodyPublishers.ofString(form))
			    .build();
	
			HttpResponse<?> response = client.send(request, HttpResponse.BodyHandlers.ofString());
	
			return null;
		
		} catch(NoSuchAlgorithmException | InterruptedException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	private static String generateAuthorization(String username, SshPublicKey key, long nonce, SshPublicKey... keys) {
		
		try(ByteArrayWriter baw = new ByteArrayWriter()) {
			baw.writeString(username);
			baw.writeUINT64(nonce);
			
			
		}
		return null;
	}
}
