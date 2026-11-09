#!/usr/bin/bash
#
# Build and/or upload NetAigis OTP release artifacts (Pro only for now).
#
# Usage:
#   ./helper/release.sh 4.0.3 --build
#   FTOKEN_RELEASE=... ./helper/release.sh 4.0.3 --upload
#   ./helper/release.sh 4.0.3 --upload-github
#   FTOKEN_RELEASE=... ./helper/release.sh 4.0.3 --build --upload --upload-github
#   FTOKEN_RELEASE=... ./helper/release.sh 4.0.3 --upload --force-upload
#
# Release notes come from app/src/main/res/raw/changelog.md (section ## VERSION).
# Free edition builds/uploads are paused; Pro is the default release target.
#
##############################################################################

set -euo pipefail

usage() {
  printf "Verwendung: %s VERSION [--build] [--upload] [--upload-github] [--force-upload]\n" "$0"
}

if [ "$#" -lt 2 ]; then
  usage
  exit 2
fi

scVersion="$1"
shift
scDoBuild='false'
scDoUpload='false'
scDoUploadGithub='false'
scForceUpload='false'

case "${scVersion}" in
  *[!A-Za-z0-9._-]*|'')
    printf "Ungueltige Version: %s\n" "${scVersion}"
    usage
    exit 2
    ;;
esac

while [ "$#" -gt 0 ]; do
  case "$1" in
    --build)
      scDoBuild='true'
      ;;
    --upload)
      scDoUpload='true'
      ;;
    --upload-github)
      scDoUploadGithub='true'
      ;;
    --force-upload)
      scForceUpload='true'
      ;;
    *)
      printf "Unbekannte Option: %s\n" "$1"
      usage
      exit 2
      ;;
  esac
  shift
done

if [ "${scDoBuild}" = "false" ] && [ "${scDoUpload}" = "false" ] && [ "${scDoUploadGithub}" = "false" ]; then
  printf "Keine Aktion angegeben.\n"
  usage
  exit 2
fi

if [ "${scDoUpload}" = "true" ] && ! command -v jq >/dev/null 2>&1; then
  printf "jq wird fuer --upload benoetigt, ist aber nicht installiert.\n"
  exit 2
fi

if [ "${scDoUploadGithub}" = "true" ] && ! command -v gh >/dev/null 2>&1; then
  printf "gh wird fuer --upload-github benoetigt, ist aber nicht installiert.\n"
  exit 2
fi

scApiBase="${FORGEJO_API_BASE:-https://git.wittmer.software/api/v1}"
scOwner="${FORGEJO_OWNER:-WITTMER}"
scRepo="${FORGEJO_REPO:-otp-android}"
scGhRepo="${GITHUB_REPO:-net-aigis/NetAigis-OTP}"
scChangelog="${CHANGELOG_FILE:-app/src/main/res/raw/changelog.md}"
scOutDir="release/${scVersion}"
scProApk="app/build/outputs/apk/pro/debug/otp-debug.apk"
scProReleaseApk="app/build/outputs/apk/pro/release/otp-release.apk"
scProOut="netaigis-otp-pro-debug-${scVersion}.apk"
scProReleaseOut="netaigis-otp-pro-${scVersion}.apk"
scReleaseId=''
scReleaseNotes=''
scNotesFile=''

scUploadFiles=(
  "${scOutDir}/${scProReleaseOut}"
  "${scOutDir}/${scProReleaseOut}.sha256"
)

required_file() {
  if [ ! -f "$1" ]; then
    printf "Artefakt fehlt: %s\n" "$1"
    printf "Bitte zuerst bauen: %s %s --build\n" "$0" "${scVersion}"
    exit 1
  fi

  if [ ! -r "$1" ]; then
    printf "Artefakt ist nicht lesbar: %s\n" "$1"
    exit 1
  fi

  if [ ! -s "$1" ]; then
    printf "Artefakt ist leer: %s\n" "$1"
    exit 1
  fi
}

json_release_id() {
  jq -r '.id // empty' "$1"
}

extract_release_notes() {
  local notes

  if [ ! -f "${scChangelog}" ]; then
    printf "Changelog fehlt: %s — verwende Fallback-Text.\n" "${scChangelog}" >&2
    printf 'Release %s\n' "${scVersion}"
    return
  fi

  notes="$(
    awk -v ver="${scVersion}" '
      /^## / {
        if (printing) exit
        if ($2 == ver) printing = 1
      }
      printing { print }
    ' "${scChangelog}"
  )"
  notes="$(printf '%s\n' "${notes}" | sed -e :a -e '/^\n*$/{$d;N;ba' -e '}')"

  if [ -z "${notes}" ]; then
    printf "Kein Changelog-Abschnitt fuer %s in %s — verwende Fallback-Text.\n" "${scVersion}" "${scChangelog}" >&2
    printf 'Release %s\n' "${scVersion}"
    return
  fi

  printf '%s\n' "${notes}"
}

prepare_release_notes() {
  scReleaseNotes="$(extract_release_notes)"
  scNotesFile="$(mktemp)"
  printf '%s\n' "${scReleaseNotes}" > "${scNotesFile}"
  printf "Release notes aus %s:\n%s\n" "${scChangelog}" "${scReleaseNotes}"
}

cleanup_release_notes() {
  if [ -n "${scNotesFile}" ]; then
    rm -f "${scNotesFile}"
  fi
}

trap cleanup_release_notes EXIT

build_artifacts() {
  printf "Build Pro Debug and Release APKs for release %s\n" "${scVersion}"
  ./gradlew :app:assembleProDebug :app:assembleProRelease

  required_source_apk "${scProApk}" "Pro"
  required_source_apk "${scProReleaseApk}" "Pro release"

  mkdir -p "${scOutDir}"
  cp "${scProApk}" "${scOutDir}/${scProOut}"
  cp "${scProReleaseApk}" "${scOutDir}/${scProReleaseOut}"

  (
    cd "${scOutDir}"
    sha256sum "${scProOut}" > "${scProOut}.sha256"
    sha256sum "${scProReleaseOut}" > "${scProReleaseOut}.sha256"
  )

  printf "Release artifacts written to %s\n" "${scOutDir}"
}

required_source_apk() {
  local file="$1"
  local label="$2"

  if [ ! -f "${file}" ]; then
    printf "%s APK nicht gefunden: %s\n" "${label}" "${file}"
    exit 1
  fi
}

api_get_release_by_tag() {
  local response_file="$1"
  curl -sS \
    -H "Authorization: token ${FTOKEN_RELEASE}" \
    -H "Accept: application/json" \
    -o "${response_file}" \
    -w '%{http_code}' \
    "${scApiBase}/repos/${scOwner}/${scRepo}/releases/tags/${scVersion}"
}

api_create_release() {
  local response_file="$1"
  local payload

  payload="$(
    jq -n \
      --arg tag "${scVersion}" \
      --arg name "${scVersion}" \
      --arg body "${scReleaseNotes}" \
      '{tag_name:$tag,name:$name,body:$body,draft:false,prerelease:false}'
  )"

  curl -sS \
    -X POST \
    -H "Authorization: token ${FTOKEN_RELEASE}" \
    -H "Accept: application/json" \
    -H "Content-Type: application/json" \
    -o "${response_file}" \
    -w '%{http_code}' \
    -d "${payload}" \
    "${scApiBase}/repos/${scOwner}/${scRepo}/releases"
}

api_edit_release() {
  local release_id="$1"
  local response_file="$2"
  local payload

  payload="$(
    jq -n \
      --arg body "${scReleaseNotes}" \
      '{body:$body}'
  )"

  curl -sS \
    -X PATCH \
    -H "Authorization: token ${FTOKEN_RELEASE}" \
    -H "Accept: application/json" \
    -H "Content-Type: application/json" \
    -o "${response_file}" \
    -w '%{http_code}' \
    -d "${payload}" \
    "${scApiBase}/repos/${scOwner}/${scRepo}/releases/${release_id}"
}

api_list_release_assets() {
  local release_id="$1"
  local response_file="$2"
  curl -sS \
    -H "Authorization: token ${FTOKEN_RELEASE}" \
    -H "Accept: application/json" \
    -o "${response_file}" \
    -w '%{http_code}' \
    "${scApiBase}/repos/${scOwner}/${scRepo}/releases/${release_id}/assets"
}

api_delete_release_asset() {
  local release_id="$1"
  local asset_id="$2"
  local response_file="$3"
  curl -sS \
    -X DELETE \
    -H "Authorization: token ${FTOKEN_RELEASE}" \
    -H "Accept: application/json" \
    -o "${response_file}" \
    -w '%{http_code}' \
    "${scApiBase}/repos/${scOwner}/${scRepo}/releases/${release_id}/assets/${asset_id}"
}

find_uploaded_asset_id() {
  local assets_file="$1"
  local asset_name="$2"
  jq -r --arg name "${asset_name}" '.[] | select(.name == $name) | .id' "${assets_file}" | head -n 1
}

upload_asset() {
  local release_id="$1"
  local file="$2"
  local asset_name
  local upload_file
  local response_file
  local http_code

  asset_name="$(basename "${file}")"
  upload_file="$(realpath "${file}")"
  response_file="$(mktemp)"
  http_code="$(
    UPLOAD_FILE="${upload_file}" \
    ASSET_NAME="${asset_name}" \
    RESPONSE_FILE="${response_file}" \
    RELEASE_ASSET_URL="${scApiBase}/repos/${scOwner}/${scRepo}/releases/${release_id}/assets?name=${asset_name}" \
    python3 - <<'PY'
import os
import sys
import uuid
import http.client
import urllib.error
import urllib.parse
import urllib.request

upload_file = os.environ["UPLOAD_FILE"]
asset_name = os.environ["ASSET_NAME"]
response_file = os.environ["RESPONSE_FILE"]
url = os.environ["RELEASE_ASSET_URL"]
token = os.environ["FTOKEN_RELEASE"]
boundary = f"----netaigis-otp-{uuid.uuid4().hex}"
chunk_size = 1024 * 1024
file_size = os.path.getsize(upload_file)

head = (
    f"--{boundary}\r\n"
    f'Content-Disposition: form-data; name="attachment"; filename="{asset_name}"\r\n'
    "Content-Type: application/octet-stream\r\n"
    "\r\n"
).encode("utf-8")
tail = f"\r\n--{boundary}--\r\n".encode("utf-8")
content_length = len(head) + file_size + len(tail)

def print_progress(sent_file_bytes):
    percent = 100 if file_size == 0 else int(sent_file_bytes * 100 / file_size)
    sent_mib = sent_file_bytes / 1024 / 1024
    total_mib = file_size / 1024 / 1024
    print(f"\rUpload {asset_name}: {sent_mib:.1f}/{total_mib:.1f} MiB ({percent}%)", end="", file=sys.stderr, flush=True)

try:
    parsed_url = urllib.parse.urlsplit(url)
    if parsed_url.scheme == "https":
        connection = http.client.HTTPSConnection(parsed_url.netloc)
    elif parsed_url.scheme == "http":
        connection = http.client.HTTPConnection(parsed_url.netloc)
    else:
        raise RuntimeError(f"Unsupported URL scheme: {parsed_url.scheme}")

    path = urllib.parse.urlunsplit(("", "", parsed_url.path, parsed_url.query, ""))
    connection.putrequest("POST", path)
    connection.putheader("Authorization", f"token {token}")
    connection.putheader("Accept", "application/json")
    connection.putheader("Content-Type", f"multipart/form-data; boundary={boundary}")
    connection.putheader("Content-Length", str(content_length))
    connection.endheaders()

    connection.send(head)
    sent_file_bytes = 0
    print_progress(sent_file_bytes)

    with open(upload_file, "rb") as file_handle:
        while True:
            chunk = file_handle.read(chunk_size)
            if not chunk:
                break
            connection.send(chunk)
            sent_file_bytes += len(chunk)
            print_progress(sent_file_bytes)

    connection.send(tail)
    print(file=sys.stderr)

    response = connection.getresponse()
    status = response.status
    response_body = response.read()
    connection.close()
except Exception as error:
    print(file=sys.stderr)
    print(f"Upload request failed: {error}", file=sys.stderr)
    sys.exit(1)

with open(response_file, "wb") as output:
    output.write(response_body)

print(status)
PY
  )"

  if [ "${http_code}" != "201" ]; then
    printf "Upload fehlgeschlagen (%s): %s\n" "${http_code}" "${file}"
    cat "${response_file}"
    printf "\n"
    rm -f "${response_file}"
    exit 1
  fi

  rm -f "${response_file}"
  printf "Uploaded: %s\n" "${file}"
}

upload_artifacts() {
  local response
  local assets_response
  local asset_delete_response
  local http_code
  local file
  local asset_name
  local existing_asset_id

  if [ -z "${FTOKEN_RELEASE:-}" ]; then
    printf "FTOKEN_RELEASE ist nicht gesetzt.\n"
    usage
    exit 2
  fi

  for file in "${scUploadFiles[@]}"; do
    required_file "${file}"
  done

  response="$(mktemp)"
  http_code="$(api_get_release_by_tag "${response}")"

  case "${http_code}" in
    200)
      scReleaseId="$(json_release_id "${response}")"
      printf "Release %s existiert bereits. ID: %s\n" "${scVersion}" "${scReleaseId}"
      http_code="$(api_edit_release "${scReleaseId}" "${response}")"
      if [ "${http_code}" != "200" ]; then
        printf "Release-Beschreibung konnte nicht aktualisiert werden (%s):\n" "${http_code}"
        cat "${response}"
        printf "\n"
        rm -f "${response}"
        exit 1
      fi
      printf "Release-Beschreibung aktualisiert.\n"
      ;;
    404)
      printf "Release %s existiert nicht. Lege Release an.\n" "${scVersion}"
      http_code="$(api_create_release "${response}")"
      if [ "${http_code}" != "201" ]; then
        printf "Release konnte nicht angelegt werden (%s):\n" "${http_code}"
        cat "${response}"
        printf "\n"
        rm -f "${response}"
        exit 1
      fi
      scReleaseId="$(json_release_id "${response}")"
      printf "Release %s angelegt. ID: %s\n" "${scVersion}" "${scReleaseId}"
      ;;
    *)
      printf "Release-Abfrage fehlgeschlagen (%s):\n" "${http_code}"
      cat "${response}"
      printf "\n"
      rm -f "${response}"
      exit 1
      ;;
  esac

  rm -f "${response}"

  if [ -z "${scReleaseId}" ]; then
    printf "Release-ID konnte nicht ermittelt werden.\n"
    exit 1
  fi

  assets_response="$(mktemp)"
  http_code="$(api_list_release_assets "${scReleaseId}" "${assets_response}")"
  if [ "${http_code}" != "200" ]; then
    printf "Release-Assets konnten nicht abgefragt werden (%s):\n" "${http_code}"
    cat "${assets_response}"
    printf "\n"
    rm -f "${assets_response}"
    exit 1
  fi

  for file in "${scUploadFiles[@]}"; do
    asset_name="$(basename "${file}")"
    existing_asset_id="$(find_uploaded_asset_id "${assets_response}" "${asset_name}")"

    if [ -n "${existing_asset_id}" ]; then
      if [ "${scForceUpload}" = "false" ]; then
        printf "Already uploaded: %s\n" "${file}"
        continue
      fi

      asset_delete_response="$(mktemp)"
      http_code="$(api_delete_release_asset "${scReleaseId}" "${existing_asset_id}" "${asset_delete_response}")"
      if [ "${http_code}" != "204" ]; then
        printf "Vorhandenes Artefakt konnte nicht geloescht werden (%s): %s\n" "${http_code}" "${file}"
        cat "${asset_delete_response}"
        printf "\n"
        rm -f "${asset_delete_response}" "${assets_response}"
        exit 1
      fi
      rm -f "${asset_delete_response}"
      printf "Deleted existing: %s\n" "${file}"
    fi

    upload_asset "${scReleaseId}" "${file}"
  done

  rm -f "${assets_response}"
  printf "Release upload complete: %s/repos/%s/%s/releases/tag/%s\n" "${scApiBase%/api/v1}" "${scOwner}" "${scRepo}" "${scVersion}"
}

github_asset_exists() {
  local asset_name="$1"
  local names

  names="$(gh release view "${scVersion}" --repo "${scGhRepo}" --json assets -q '.assets[].name' 2>/dev/null || true)"
  printf '%s\n' "${names}" | grep -Fxq "${asset_name}"
}

upload_github_artifacts() {
  local file
  local asset_name
  local upload_args=()

  for file in "${scUploadFiles[@]}"; do
    required_file "${file}"
  done

  if gh release view "${scVersion}" --repo "${scGhRepo}" >/dev/null 2>&1; then
    printf "GitHub-Release %s existiert bereits.\n" "${scVersion}"
    gh release edit "${scVersion}" --repo "${scGhRepo}" --notes-file "${scNotesFile}"
    printf "GitHub Release-Beschreibung aktualisiert.\n"

    for file in "${scUploadFiles[@]}"; do
      asset_name="$(basename "${file}")"
      if github_asset_exists "${asset_name}"; then
        if [ "${scForceUpload}" = "false" ]; then
          printf "Already uploaded (GitHub): %s\n" "${file}"
          continue
        fi
        gh release upload "${scVersion}" "${file}" --repo "${scGhRepo}" --clobber
        printf "Re-uploaded (GitHub): %s\n" "${file}"
      else
        gh release upload "${scVersion}" "${file}" --repo "${scGhRepo}"
        printf "Uploaded (GitHub): %s\n" "${file}"
      fi
    done
  else
    printf "GitHub-Release %s existiert nicht. Lege Release an.\n" "${scVersion}"
    for file in "${scUploadFiles[@]}"; do
      upload_args+=("${file}")
    done
    gh release create "${scVersion}" \
      --repo "${scGhRepo}" \
      --title "${scVersion}" \
      --notes-file "${scNotesFile}" \
      "${upload_args[@]}"
    printf "GitHub-Release angelegt und Assets hochgeladen.\n"
  fi

  printf "GitHub release upload complete: https://github.com/%s/releases/tag/%s\n" "${scGhRepo}" "${scVersion}"
}

if [ "${scDoUpload}" = "true" ] || [ "${scDoUploadGithub}" = "true" ]; then
  prepare_release_notes
fi

if [ "${scDoBuild}" = "true" ]; then
  build_artifacts
fi

if [ "${scDoUpload}" = "true" ]; then
  upload_artifacts
fi

if [ "${scDoUploadGithub}" = "true" ]; then
  upload_github_artifacts
fi
