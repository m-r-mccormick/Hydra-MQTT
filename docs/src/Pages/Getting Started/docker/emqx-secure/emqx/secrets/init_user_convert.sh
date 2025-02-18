#!/bin/bash

SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
cd "${SCRIPT_DIR}"

# Input file
input_file="init_user_plain.json"
output_file="init_user_hash.json"

# Function to generate a salt
generate_salt() {
  openssl rand -hex 16
}

# Function to generate a SHA-256 password hash with a salt
generate_password_hash() {
  local password=$1
  local salt=$2
  echo -n "${password}${salt}" | openssl dgst -sha256 | awk '{print $2}'
}

# Read and process the JSON file
jq -c '.[]' "$input_file" | while read -r user; do
  # Extract fields from JSON
  user_id=$(echo "$user" | jq -r '.user_id')
  password=$(echo "$user" | jq -r '.password')
  is_superuser=$(echo "$user" | jq '.is_superuser')

  # Generate salt and hash
  salt=$(generate_salt)
  password_hash=$(generate_password_hash "$password" "$salt")

  # Create new JSON object
  jq -n \
    --arg user_id "$user_id" \
    --arg password_hash "$password_hash" \
    --arg salt "$salt" \
    --argjson is_superuser "$is_superuser" \
    '{
      user_id: $user_id,
      password_hash: $password_hash,
      salt: $salt,
      is_superuser: $is_superuser
    }'
done | jq -s '.' > "$output_file"

echo "Hashed user data saved to $output_file"

