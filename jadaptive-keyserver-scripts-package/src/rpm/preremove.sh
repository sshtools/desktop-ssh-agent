if [ -w /etc/ssh/sshd_config -a -w /etc/ssh ] ; then
	sed -i 's_AuthorizedKeysCommand /usr/bin/keyserver-authorized-keys %h %u_#AuthorizedKeysCommand none_g' /etc/ssh/sshd_config 	
	sed -i 's_AuthorizedKeysCommandUser %u_#AuthorizedKeysCommandUser nobody_g' /etc/ssh/sshd_config
	service sshd reload
fi