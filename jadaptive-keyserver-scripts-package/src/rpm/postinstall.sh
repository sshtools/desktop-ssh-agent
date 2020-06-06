if [ -w /etc/ssh/sshd_config -a -w /etc/ssh ] ; then

    echo "# DO NOT CHANGE\nMACHINE_ID=`mktemp -u XXXXXXXXXXXXXXXXXXXXXXXX`" >> /etc/default/jadaptive-keyserver
    
    version_raw=`ssh -V 2>&1`
    major_version=`echo "${version_raw}" | sed 's/^.*_\([0-9]\)\.[0-9].*$/\1/'`
    minor_version=`echo "${version_raw}" | sed 's/^.*_[0-9]\.\([0-9]\).*$/\1/'`
    
    if [ "$major_version" -le 6 ]; then
	 if [ "$major_version" -lt 8 ]; then
	    echo "WARNING"
	    echo "OpenSSH $major_version.$minor_version is not compatible with the AuthorizedKeysCommand scripts."
	    echo "For stronger security, and to support these scripts please upgrade to OpenSSH 6.8 or greater."
	    exit 0
	 fi
	fi

	sed -i 's_#AuthorizedKeysCommand none_AuthorizedKeysCommand /usr/bin/keyserver-authorized-keys %h %u_g' /etc/ssh/sshd_config 	
	sed -i 's_#AuthorizedKeysCommandUser nobody_AuthorizedKeysCommandUser %u_g' /etc/ssh/sshd_config
	service sshd reload
fi
