#!/bin/bash

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

keystore="${secrets_dir}/${keystore_name}.jks"
keystore_password_file="${secrets_dir}/keystore_password.txt"
alias_name_file="${secrets_dir}/alias_name.txt"
alias_password_file="${secrets_dir}/alias_password.txt"
sign_crt="${secrets_dir}/sign.crt"
chain_p7b="${secrets_dir}/${chain_name}.p7b"

echo "keystore_file=\"${keystore}\"" >> "${env_file}"
echo "keystore_password_file=\"${keystore_password_file}\"" >> "${env_file}"
echo "alias_name_file=\"${alias_name_file}\"" >> "${env_file}"
echo "alias_password_file=\"${alias_password_file}\"" >> "${env_file}"
echo "chain_file=\"${chain_p7b}\"" >> "${env_file}"

echo "${alias_name}" > "${alias_name_file}"
echo "${keystore_password}" > "${keystore_password_file}"
echo "${alias_password}" > "${alias_password_file}"


ca_srl=$(echo "${ca_key}" | sed 's/.key$/.srl/g')

# Create a keystore and key for signing
echo ""
echo "Generating a Keystore and Key..."
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
echo "Generating Secrets Completed"

