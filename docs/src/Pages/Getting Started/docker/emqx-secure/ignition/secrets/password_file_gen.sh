#!/bin/bash

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
cd "${SCRIPT_DIR}"

cat "password_file_input.txt" | base64 > password_file
if [ $? -ne 0 ]; then
	echo "Error: could not generate password_file"
	exit 1
fi

