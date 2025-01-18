#!/bin/bash

# Use this file to generate a build config, or test build config generation using a mock env file
# Generate a config
#	make
#	make config
#	./generate-config.sh
# Generate a config using a mock file
#	make mock=main-push.env
#	make mock=main-push.env config
#	./generate-config.sh main-push.env

# exit on error
set -e
function err {
  if [ -f "${CONFIG_LOG}" ]; then
      cat "${CONFIG_LOG}"
  fi
  echo "[${FUNCNAME[1]}:${BASH_LINENO[0]}] Error: ${1}"
  exit 1
}
trap 'err' ERR

CONFIG_FILE="config"
CONFIG_LOG="config.log"
if [ -f "${CONFIG_LOG}" ]; then
	rm "${CONFIG_LOG}"
fi

# Load arguments and mock file if specified
if [ ! -z "${1}" ]; then
	MOCK_CI_ENV_FILE="${1}"
	echo "Using Mock CI Environment Variable File: ${MOCK_CI_ENV_FILE}"
	# Export all mock variables into environment variables
	source "${MOCK_CI_ENV_FILE}"
	while IFS= read -r line; do
	  export ${line}
	done <<< $(cat "${MOCK_CI_ENV_FILE}")
fi

# Get all raw ci variable names and values and dump them to disk so they can be used for mock testing
CI_ENV_FILE="mock/ci.env" # variable names with values
CI_LIST_FILE="mock/ci.list" # list of known ci variable names
CI_LIST_NEW_FILE="mock/ci.list.new" # list of new variable names not in CI_LIST_FILE
# Combine mock ci environment variables with current environment variables
ci_variables=$(echo "${ci_variables}\n`env`")
# Remove variable values to get only the variable names
ci_variable_names=$(echo "${ci_variables}" | sed 's/=.*//g')
# If a list of ci variable names already exists, append it
if [ -f "${CI_LIST_FILE}" ]; then
  ci_variable_names=$(echo "${ci_variable_names}\n`cat ${CI_LIST_FILE}`")
fi
# Get a unique sorted list of variable names
ci_variable_names=$(echo "${ci_variable_names}" | sed 's/=.*//g' | sort | uniq)
# Filter to only include pertinent variable names
ci_variable_names=$(echo "${ci_variable_names}" | grep "^ACT.*\|^CI$\|^GIT.*\|^RUN.*")
# Create a list of variables which are not listed in CI_LIST_FILE
rm -f "${CI_LIST_NEW_FILE}"
while IFS= read -r name; do
	in_ci_list=$(cat "${CI_LIST_FILE}" | grep "^${name}$" || true)
	if [ -z "${in_ci_list}" ]; then
		echo "${name}" >> "${CI_LIST_NEW_FILE}"
	fi
done <<< "${ci_variable_names}"
if [ -f "${CI_LIST_NEW_FILE}" ]; then
  	echo ""
	echo "New CI Variables Identified:"
	echo ""
	cat "${CI_LIST_NEW_FILE}"
	echo ""
fi
# Dump all ci environment variables so they can be used as a template for mocking
rm -f "${CI_ENV_FILE}"
while IFS= read -r name; do
  echo "${name}=${!name}" >> "${CI_ENV_FILE}"
done <<< "${ci_variable_names}"

# If not mocking ci environment variables
#if [ ! -z "${GITHUB_ACTIONS}" ]; then
#  # Dump all ci environment variables so they can be used as a template for mocking
#  echo ""
#  echo "CI Variables:"
#  echo ""
#  cat "${CI_ENV_FILE}"
#  echo ""
#fi

# Dump ci vars into config log
cat "${CI_ENV_FILE}" >> "${CONFIG_LOG}"
echo "" >> "${CONFIG_LOG}"

# Derivative Gitea Environment Variables \
GIT_SERVER_URL="https://github.com"
if [ ! -z "${GIT_SERVER_IP}" ]; then
	GIT_SERVER_URL="https://${GIT_SERVER_IP}"
	if [ ! -z "${GIT_SERVER_PORT}" ]; then
		GIT_SERVER_URL="https://${GIT_SERVER_IP}:${GIT_SERVER_PORT}"
	fi
elif [ ! -z "${GITHUB_SERVER_URL}" ]; then
	GIT_SERVER_URL="${GITHUB_SERVER_URL}"
fi
echo "GIT_SERVER_URL: ${GIT_SERVER_URL}" >> "${CONFIG_LOG}"

# Derivative Github Environment Variables
GIT_REPOSITORY_URL="${GIT_SERVER_URL}/${GITHUB_REPOSITORY}"
echo "GIT_REPOSITORY_URL: ${GIT_REPOSITORY_URL}" >> "${CONFIG_LOG}"
GIT_RELEASE_URL="${GIT_SERVER}/${GIT_REPOSITORY}/releases/tag/${GIT_RELEASE_TAG}"
echo "GIT_RELEASE_URL: ${GIT_RELEASE_URL}" >> "${CONFIG_LOG}"

# Hydra-MQTT Version Identification
VERSION="$(cat version)"
echo "VERSION=${VERSION}" >> "${CONFIG_FILE}"
echo "VERSION: ${VERSION}" >> "${CONFIG_LOG}"
EXISTING_VERSIONS="$( \
				git ls-remote --tags origin | \
				grep "refs/tags/v${RELEASE_VERSION}" | \
				awk '{ print $2 }' | \
				sed 's:^refs/tags/v::g' | \
				sort \
				)"
echo -e "EXISTING_VERSIONS:\n${EXISTING_VERSIONS}" >> "${CONFIG_LOG}"
EXISTING_RELEASE_VERSIONS=$(echo "${EXISTING_VERSIONS}" | grep -v ".*-rc.*" | sort)
echo -e "EXISTING_RELEASE_VERSIONS:\n${EXISTING_RELEASE_VERSIONS}" >> "${CONFIG_LOG}"
EXISTING_PRERELEASE_VERSIONS=$(echo "${EXISTING_VERSIONS}" | grep ".*-rc.*" | sort)
echo -e "EXISTING_PRERELEASE_VERSIONS:\n${EXISTING_PRERELEASE_VERSIONS}" >> "${CONFIG_LOG}"
EXISTING_CURRENT_RELEASE_VERSIONS=$(echo "${EXISTING_VERSIONS}" | grep "^${VERSION}$" | sort)
echo -e "EXISTING_CURRENT_RELEASE_VERSIONS:\n${EXISTING_CURRENT_RELEASE_VERSIONS}" >> "${CONFIG_LOG}"
if [ ! -z "${EXISTING_CURRENT_RELEASE_VERSIONS}" ]; then
	echo "Error: Version ${VERSION} has already been released."
	exit 1
fi
EXISTING_CURRENT_PRERELEASE_VERSIONS=$(echo "${EXISTING_VERSIONS}" | grep "^${VERSION}-rc" | sort)
echo -e "EXISTING_CURRENT_PRERELEASE_VERSIONS:\n${EXISTING_CURRENT_PRERELEASE_VERSIONS}" >> "${CONFIG_LOG}"
EXISTING_CURRENT_RCS="$(echo "${EXISTING_CURRENT_PRERELEASE_VERSIONS}" | sed 's/.*-rc//g' | sort)"
echo -e "EXISTING_CURRENT_RCS:\n${EXISTING_CURRENT_RCS}" >> "${CONFIG_LOG}"
LAST_RC=0
if [ ! -z "${EXISTING_CURRENT_RCS}" ]; then
	LAST_RC="$(echo "${EXISTING_CURRENT_RCS}" | sort | tail -n1)"
fi
echo "LAST_RC: ${LAST_RC}" >> "${CONFIG_LOG}"
NEW_RC="$((${LAST_RC}+1))"
echo "NEW_RC: ${NEW_RC}" >> "${CONFIG_LOG}"
if [ "${NEW_RC}" -gt 9 ]; then
	echo "RC Limit Exceeded (9 max)"
	exit 1
fi
if [ "${GITHUB_EVENT_NAME}" = "push" ]; then
	if [ "${GITHUB_REF_NAME}" = "main" ]; then
		BUILD_CONFIG="RELEASE"
	else
		BUILD_CONFIG="PRERELEASE"
	fi
elif [ "${GITHUB_EVENT_NAME}" = "pull_request" ]; then
	# Pull Request Target Branch
	if [ "${GITHUB_BASE_REF}" = "main" ]; then
		BUILD_CONFIG="RELEASE"
	else
		BUILD_CONFIG="PRERELEASE"
	fi
else
	if [ ! -z "${GITHUB_ACTIONS}" ]; then
		echo "GIT_EVENT_NAME '${GIT_EVENT_NAME}' is not 'push' or 'pull_request'"
		exit 1
	fi
	BUILD_CONFIG="DEVELOPMENT"
fi
echo "BUILD_CONFIG=${BUILD_CONFIG}" >> "${CONFIG_FILE}"
echo "BUILD_CONFIG: ${BUILD_CONFIG}" >> "${CONFIG_LOG}"
BUILD_NUMBER="${GITHUB_RUN_NUMBER}"
if [ -z "${BUILD_NUMBER}" ]; then
	BUILD_NUMBER="$(date +%Y%m%d)"
fi
echo "BUILD_NUMBER=${BUILD_NUMBER}" >> "${CONFIG_FILE}"
echo "BUILD_NUMBER: ${BUILD_NUMBER}" >> "${CONFIG_LOG}"
if [ "${BUILD_CONFIG}" = "RELEASE" ]; then
	BUILD_VERSION="${VERSION}"
    BUILD_MODULE="${VERSION}.${BUILD_NUMBER}"
elif [ "${BUILD_CONFIG}" = "PRERELEASE" ]; then
	BUILD_VERSION="${VERSION}-rc${NEW_RC}"
	BUILD_MODULE="${VERSION}.${BUILD_NUMBER}-rc${NEW_RC}"
else
	BUILD_VERSION="${VERSION}-dev"
	BUILD_MODULE="${VERSION}.${BUILD_NUMBER}-SNAPSHOT"
fi
echo "BUILD_VERSION=${BUILD_VERSION}" >> "${CONFIG_FILE}"
echo "BUILD_VERSION: ${BUILD_VERSION}" >> "${CONFIG_LOG}"
echo "BUILD_MODULE=${BUILD_MODULE}" >> "${CONFIG_FILE}"
echo "BUILD_MODULE: ${BUILD_MODULE}" >> "${CONFIG_LOG}"
RELEASE_VERSION="${BUILD_VERSION}"
echo "RELEASE_VERSION=${RELEASE_VERSION}" >> "${CONFIG_FILE}"
echo "RELEASE_VERSION: ${RELEASE_VERSION}" >> "${CONFIG_LOG}"
RELEASE_DATE="$(date "+%Y-%m-%d")"
echo "RELEASE_DATE=${RELEASE_DATE}" >> "${CONFIG_FILE}"
echo "RELEASE_DATE: ${RELEASE_DATE}" >> "${CONFIG_LOG}"
RELEASE_TAG="v${BUILD_VERSION}"
echo "RELEASE_TAG=${RELEASE_TAG}" >> "${CONFIG_FILE}"
echo "RELEASE_TAG: ${RELEASE_TAG}" >> "${CONFIG_LOG}"
RELEASE_NAME="v${BUILD_VERSION} (${RELEASE_DATE})"
echo "RELEASE_NAME=${RELEASE_NAME}" >> "${CONFIG_FILE}"
echo "RELEASE_NAME: ${RELEASE_NAME}" >> "${CONFIG_LOG}"
RELEASE_IS_PRERELEASE=false
if [ "${BUILD_CONFIG}" != "RELEASE" ]; then
	RELEASE_IS_PRERELEASE=true
fi
echo "RELEASE_IS_PRERELEASE=${RELEASE_IS_PRERELEASE}" >> "${CONFIG_FILE}"
echo "RELEASE_IS_PRERELEASE: ${RELEASE_IS_PRERELEASE}" >> "${CONFIG_LOG}"
RELEASE_URL="${GIT_REPOSITORY_URL}/releases/tag/${RELEASE_TAG}"
echo "RELEASE_URL=${RELEASE_URL}" >> "${CONFIG_FILE}"
echo "RELEASE_URL: ${RELEASE_URL}" >> "${CONFIG_LOG}"
