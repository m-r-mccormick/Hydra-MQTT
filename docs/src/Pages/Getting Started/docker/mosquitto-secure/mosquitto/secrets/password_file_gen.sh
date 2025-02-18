#!/bin/bash

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
cd "${SCRIPT_DIR}"

if [ -f password_file ]; then
	rm password_file
	if [ $? -ne 0 ]; then
		echo "Error: could not remove password_file"
		exit 1
	fi
fi

cp password_file_input.txt password_file
if [ $? -ne 0 ]; then
	echo "Error: could not copy password_file_input.txt"
	exit 1
fi

# Mosquitto requires mosquitto(1883):root ownership
sudo chown 1883:1883 password_file
if [ $? -ne 0 ]; then
	echo "Error: could not change ownership of password_file"
	exit 1
fi

# Mosquitto requires 700 permissions
sudo chmod 700 password_file
if [ $? -ne 0 ]; then
	echo "Error: could not change permissions of password_file"
	exit 1
fi

# Use the mosquitto_passwd utility in the container to convert the raw usernames and passwords to mosquitto passwords file
docker run --rm -it -v ./password_file:/run/secrets/password_file eclipse-mosquitto mosquitto_passwd -U /run/secrets/password_file
if [ $? -ne 0 ]; then
	echo "Error: could not generate password_file"
	exit 1
fi

