#!/bin/bash

#ca_key="ca.key"
#ca_crt="ca.crt"
#keystore="keystore.jks"
#sign_config="sign_cert.conf"
#alias_name="alias"
#sign_csr="sign.csr"
#sign_crt="sign.crt"
#chain_p7b="chain.p7b"

secrets_dir="secrets"
if [ ! -d "${secrets_dir}" ]; then
	mkdir "${secrets_dir}"
	if [ $? -ne 0 ]; then
		echo "Error: could not create keys directory"
		exit 1
	fi
fi

env_file="${secrets_dir}/env"
if [ -f ${env_file} ]; then
	echo "Error: ${env_file} already exists"
	exit 1
fi

#if [ ! -f "${env_file}" ]; then
#
#	env_ca_name="YourCommonName"
#	env_ca_password="password"
#	env_keystore_name="keystore"
#	env_keystore_password="password"
#	env_keystore_password_file="${secrets_dir}/keystore_password.txt"
#	env_alias_name="alias"
#	env_alias_password="password"
#	env_alias_password_file="${secrets_dir}/alias_password.txt"
#	env_chain_name="chain"
#	
#	
#	echo -e "\n\n\n\n\n\n\n"
#	echo "-- This Requires User Input --"
#	echo ""
#	clear
#	echo "Enter Certificate Signer Name:"
#	read env_ca_name
#	
#	# This doesn't show any text when running in makefile in intellij
#	#read -e -p "Certificate Authority Name: " -i "YourCommonName" env_ca_name
#	#read -e -p "Certificate Authority Password: " -i "password" env_ca_password
#	#read -e -p "Keystore Name: " -i "keystore" env_keystore_name
#	#read -e -p "Keystore Password: " -i "password" env_keystore_password
#	#read -e -p "Alias Name: " -i "alias" env_alias_name
#	#read -e -p "Alias Password: " -i "password" env_alias_password
#	#read -e -p "Certificate Chain Name: " -i "chain" env_chain_name
#	
#	echo "ca_name=\"${env_ca_name}\"" >> "${env_file}"
#	echo "ca_password=\"${env_ca_password}\"" >> "${env_file}"
#	echo "keystore_name=\"${env_keystore_name}\"" >> "${env_file}"
#	#echo "keystore_password=\"${env_keystore_password}\"" >> "${env_file}"
#	echo "keystore_password_file=\"${env_keystore_password_file}\"" >> "${env_file}"
#	echo "${env_keystore_password}" >> "${env_keystore_password_file}"
#	echo "alias_name=\"${env_alias_name}\"" >> "${env_file}"
#	#echo "alias_password=\"${env_alias_password}\"" >> "${env_file}"
#	echo "alias_password_file=\"${env_alias_password_file}\"" >> "${env_file}"
#	echo "${env_alias_password}" >> "${env_alias_password_file}"
#	echo "chain_name=\"${env_chain_name}\"" >> "${env_file}"
#fi
#if [ ! -f "${env_file}" ]; then
#	echo "Error: ${env_file} does not exist"
#	exit 1
#fi
#source "${env_file}"
#if [ $? -ne 0 ]; then
#	echo "Error: Could not source ${env_file}."
#	exit 1
#fi
#
#if [ -z "${ca_name}" ]; then
#	echo "Error: ca_name not defined in env"
#	exit 1
#fi
##if [ -z "${ca_password}" ]; then
##	echo "Error: ca_password not defined in env"
##	exit 1
##fi
#if [ -z "${keystore_name}" ]; then
#	echo "Error: keystore_name not defined in env"
#	exit 1
#fi
##if [ -z "${keystore_password}" ]; then
##	echo "Error: keystore_password not defined in env"
##	exit 1
##fi
#if [ -z "${keystore_password_file}" ]; then
#	echo "Error: keystore_password_file not defined in env"
#	exit 1
#fi
#if [ -z "${alias_name}" ]; then
#	echo "Error: alias_name not defined in env"
#	exit 1
#fi
##if [ -z "${alias_password}" ]; then
##	echo "Error: alias_password not defined in env"
##	exit 1
##fi
#if [ -z "${alias_password_file}" ]; then
#	echo "Error: alias_password_file not defined in env"
#	exit 1
#fi
#if [ -z "${chain_name}" ]; then
#	echo "Error: chain_name not defined in env"
#	exit 1
#fi






echo -e "\n\n\n\n\n\n\n"
echo "-- This Requires User Input --"
echo ""
clear
echo "Enter Certificate Signer Name:"
read ca_name

	
ca_password="password"
keystore_name="keystore"
keystore_password="password"
alias_name="alias"
alias_password="password"
chain_name="chain"





#ca_key="${secrets_dir}/${ca_name}.key"
#ca_crt="${secrets_dir}/${ca_name}.crt"
keystore="${secrets_dir}/${keystore_name}.jks"
keystore_password_file="${secrets_dir}/keystore_password.txt"
#sign_config="${secrets_dir}/sign_cert.conf"
#alias_name="${alias_name}"
alias_name_file="${secrets_dir}/alias_name.txt"
alias_password_file="${secrets_dir}/alias_password.txt"
#sign_csr="${secrets_dir}/sign.csr"
sign_crt="${secrets_dir}/sign.crt"
chain_p7b="${secrets_dir}/${chain_name}.p7b"


#	echo "ca_name=\"${env_ca_name}\"" >> "${env_file}"
#	echo "ca_password=\"${env_ca_password}\"" >> "${env_file}"
echo "keystore_file=\"${keystore}\"" >> "${env_file}"
#	#echo "keystore_password=\"${env_keystore_password}\"" >> "${env_file}"
echo "keystore_password_file=\"${keystore_password_file}\"" >> "${env_file}"
#	echo "${env_keystore_password}" >> "${env_keystore_password_file}"
#echo "alias_name=\"${alias_name}\"" >> "${env_file}"
echo "alias_name_file=\"${alias_name_file}\"" >> "${env_file}"
#	#echo "alias_password=\"${env_alias_password}\"" >> "${env_file}"
echo "alias_password_file=\"${alias_password_file}\"" >> "${env_file}"
#	echo "${env_alias_password}" >> "${env_alias_password_file}"
echo "chain_file=\"${chain_p7b}\"" >> "${env_file}"

echo "${alias_name}" > "${alias_name_file}"
echo "${keystore_password}" > "${keystore_password_file}"
echo "${alias_password}" > "${alias_password_file}"


ca_srl=$(echo "${ca_key}" | sed 's/.key$/.srl/g')











# Create a keystore and key for signing
echo ""
echo "Generating a Keystore and Key..."
#keytool -genkey -alias "${alias_name}" -validity 3652 -keyalg RSA -keysize 4096 -keystore "${keystore}" --storepass "${keystore_password}"
keytool -genkeypair -alias "${alias_name}" -keyalg RSA -keysize 2048 -keystore "${keystore}" -validity 3650 -storepass "${keystore_password}" -keypass "${alias_password}" -dname "CN=${ca_name}, OU=YourOrganizationalUnit, O=YourOrganization, L=YourCity, S=YourState, C=YourCountry"
if [ $? -ne 0 ]; then
	echo "Could not Generate Keystore and Key"
	exit 1
fi



echo ""
echo "Exporting Certificate..."
keytool -exportcert -alias "${alias_name}" -keystore "${keystore}" -storepass "${keystore_password}" -file "${sign_crt}" -rfc


echo ""
echo "Converting Certificate to pkcs7 format..."
openssl crl2pkcs7 -nocrl -certfile "${sign_crt}" -out "${chain_p7b}"
if [ $? -ne 0 ]; then
	echo "Could not Convert to pkcs7 format."
	exit 1
fi

#echo ""
#echo "Importing the signed certificate into the keystore..."
#keytool -import -trustcacerts -alias "${alias_name}" -file "${chain_p7b}" -keystore "${keystore}" --storepass "${keystore_password}"
#if [ $? -ne 0 ]; then
#	echo "Could not import signed certificate into the keystore."
#	exit 1
#fi

echo ""
echo "Removing temporary files..."
if [ -f "${ca_srl}" ]; then
	rm "${ca_srl}"
fi
if [ -f "${sign_csr}" ]; then
	rm "${sign_csr}"
fi
if [ -f "${sign_crt}" ]; then
	rm "${sign_crt}"
fi

echo ""
echo "Completed"













exit 0







echo ""
echo "Generating Certificate Authority (CA)..."
if [ -f "${ca_key}" ]; then
	echo "${ca_key} already exists."
	exit 1
fi
openssl genrsa -des3 -out "${ca_key}" 4096
#openssl genrsa -des3 -passout "${ca_password}" -out "${ca_key}" 4096
if [ $? -ne 0 ]; then
	echo "Could not Generate Certificate Authority (CA)"
	exit 1
fi

echo ""
echo "Generating CA certificate..."
openssl req -x509 -new -nodes -key "${ca_key}" -sha256 -days 3652 -out "${ca_crt}"
if [ $? -ne 0 ]; then
	echo "Could not Generate CA Certificate"
	exit 1
fi

# Create a keystore and key for signing
echo ""
echo "Generating a Keystore and Key for Signing..."
#keytool -genkey -alias "${alias_name}" -validity 3652 -keyalg RSA -keysize 4096 -keystore "${keystore}"
keytool -genkey -alias "${alias_name}" -validity 3652 -keyalg RSA -keysize 4096 -keystore "${keystore}" --storepass "${keystore_password}"
if [ $? -ne 0 ]; then
	echo "Could not Generate Keystore and Key for Signing"
	exit 1
fi

echo ""
echo "Generating a Certificate Signing Request (CSR) using the Keystore..."
keytool -certreq -alias "${alias_name}" -file "${sign_csr}" -keystore "${keystore}" --storepass "${keystore_password}"
if [ $? -ne 0 ]; then
	echo "Could not Generate CSR"
	exit 1
fi

echo ""
echo "Generating a Signing cert and key pair by signing the CSR with the CA Certificate..."
#cat <<< "
#authorityKeyIdentifier=keyid,issuer
#basicConstraints=CA:FALSE
#subjectAltName = @alt_names
#[alt_names]
#DNS.1 = example.com
#" > "${sign_config}"
#openssl x509 -req -CA "${ca_crt}" -CAkey "${ca_key}" -in "${sign_csr}" -out "${sign_crt}" -days 3652 -CAcreateserial -extfile "${sign_config}"
#rm "${sign_config}"
openssl x509 -req -CA "${ca_crt}" -CAkey "${ca_key}" -in "${sign_csr}" -out "${sign_crt}" -days 3652 -CAcreateserial
if [ $? -ne 0 ]; then
	echo "Could not Generate Signing cert and key pair."
	exit 1
fi

echo ""
echo "Converting the signed cert to pkcs7 format..."
openssl crl2pkcs7 -nocrl -certfile "${sign_crt}" -out "${chain_p7b}" -certfile "${ca_crt}"
if [ $? -ne 0 ]; then
	echo "Could not Convert to pkcs7 format."
	exit 1
fi

echo ""
echo "Importing the signed certificate into the keystore..."
keytool -import -trustcacerts -alias "${alias_name}" -file "${chain_p7b}" -keystore "${keystore}" --storepass "${keystore_password}"
if [ $? -ne 0 ]; then
	echo "Could not import signed certificate into the keystore."
	exit 1
fi

echo ""
echo "Removing temporary files..."
if [ -f "${ca_srl}" ]; then
	rm "${ca_srl}"
fi
if [ -f "${sign_csr}" ]; then
	rm "${sign_csr}"
fi
if [ -f "${sign_crt}" ]; then
	rm "${sign_crt}"
fi

echo ""
echo "Completed"
