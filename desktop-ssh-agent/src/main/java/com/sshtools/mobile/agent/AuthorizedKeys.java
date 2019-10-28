package com.sshtools.mobile.agent;

import java.io.IOException;

import com.sshtools.agent.InMemoryKeyStore;
import com.sshtools.common.publickey.SshKeyUtils;
import com.sshtools.common.ssh.components.SshPublicKey;

public class AuthorizedKeys extends AbstractAgentProcess {

	AuthorizedKeys() throws IOException {
		super();
		
		MobileDeviceKeystore store=  new  MobileDeviceKeystore(hostname, port, strictSSL, username, deviceName, authorization, new InMemoryKeyStore());
		for(SshPublicKey key : store.getPublicKeys().keySet()) {
			System.out.println(SshKeyUtils.getFormattedKey(key, ""));
		}
	}

	public static void main(String[] args) throws IOException {
		new AuthorizedKeys();
	}
}
