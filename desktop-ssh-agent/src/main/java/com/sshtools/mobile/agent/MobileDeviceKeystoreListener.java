package com.sshtools.mobile.agent;

import java.io.IOException;

import com.sshtools.agent.KeyConstraints;
import com.sshtools.common.ssh.components.SshPrivateKey;
import com.sshtools.common.ssh.components.SshPublicKey;

public interface MobileDeviceKeystoreListener {

	boolean addKey(SshPrivateKey prvkey, SshPublicKey pubkey, String description, KeyConstraints cs) throws IOException;

	boolean deleteAllKeys();

	void onKeysChanged();

	boolean deleteKey(SshPublicKey pubkey);

}
