#!/bin/bash

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
cd "${SCRIPT_DIR}"

for d in $(find . -mindepth 1 -maxdepth 1 -type d | sed 's:^\./::g'); do
	rm -f "${d}.zip"
	zip -r -q "${d}.zip" "${d}" -x '*.crt' -x '*.key' -x '*.der' -x '*.csr' -x '*.cnf' -x '*password_file' -x '*hash.json'
	if [ $? -ne 0 ]; then
		echo "Error zipping ${d}.zip"
		exit 1
	fi
done

