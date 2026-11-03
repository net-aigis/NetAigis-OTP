#!/usr/bin/bash
#
# rebuild project, un-/install and start app
#
# (c) 2026+ by Christian Wittmer, Deidesheim, DE
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License along
# with this program; if not, write to the Free Software Foundation, Inc.,
# 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
#
##############################################################################
#

scEdition='free'
scPackage='de.wittmer.app.otp.debug'
scActivity='de.wittmer.app.otp.ui.MainActivity'
scApp='app/build/outputs/apk/free/debug/app-free-debug.apk'
scSeed='false'
scWipeBackup='false'
scWipeBackupDevice=''
scSampleTokens='testdata/sample-tokens.txt'

usage() {
  printf "Verwendung: %s [--edition free|pro] [--seed] [--wipe-backup SERIAL]\n" "$0"
}

while [ "$#" -gt 0 ]; do
  case "$1" in
    --edition)
      shift
      if [ "$#" -eq 0 ]; then
        printf "Fehlender Wert für --edition\n"
        usage
        exit 2
      fi
      scEdition="$1"
      ;;
    --seed)
      scSeed='true'
      ;;
    --wipe-backup)
      shift
      if [ "$#" -eq 0 ]; then
        printf "Fehlender Wert für --wipe-backup\n"
        usage
        exit 2
      fi
      scWipeBackup='true'
      scWipeBackupDevice="$1"
      ;;
    *)
      printf "Unbekannte Option: %s\n" "$1"
      usage
      exit 2
      ;;
  esac
  shift
done

case "${scEdition}" in
  free)
    scPackage='de.wittmer.app.otp.debug'
    scApp='app/build/outputs/apk/free/debug/app-free-debug.apk'
    scGradleTask=':app:assembleFreeDebug'
    ;;
  pro)
    scPackage='de.wittmer.app.otp.pro.debug'
    scApp='app/build/outputs/apk/pro/debug/app-pro-debug.apk'
    scGradleTask=':app:assembleProDebug'
    ;;
  *)
    printf "Ungültige Edition: %s\n" "${scEdition}"
    usage
    exit 2
    ;;
esac

### clear cache on installed package
# adb shell pm clear "${scPackage}"

if ./gradlew "${scGradleTask}" ; then
  adb wait-for-device
  if [ "${scWipeBackup}" = "true" ]; then
    scDevice="$(adb get-serialno | tr -d '\r')"
    if [ "${scDevice}" = "${scWipeBackupDevice}" ]; then
      scTransport="$(adb shell bmgr list transports | sed -n 's/^  \* //p' | tr -d '\r')"
      if [ -n "${scTransport}" ]; then
        printf "Wipe Android backup for %s using transport %s\n" "${scPackage}" "${scTransport}"
        adb shell bmgr wipe "${scTransport}" "${scPackage}" || true
      else
        printf "Kein aktiver Android Backup-Transport gefunden. Backup-Wipe übersprungen.\n"
      fi
    else
      printf "Backup-Wipe übersprungen. Verbundenes Device: %s, erlaubt: %s\n" "${scDevice}" "${scWipeBackupDevice}"
    fi
  fi
  adb uninstall "${scPackage}" || true
  adb install -r "${scApp}"
  if [ "${scSeed}" = "true" ]; then
    if [ ! -f "${scSampleTokens}" ]; then
      printf "Seed-Datei fehlt: %s\n" "${scSampleTokens}"
      exit 1
    fi
    adb shell monkey -p "${scPackage}" 1 >/dev/null
    sleep 2
    scComponent="${scPackage}/${scActivity}"
    scCount=0
    while IFS= read -r scUri || [ -n "${scUri}" ]; do
      scUri="$(printf '%s' "${scUri}" | tr -d '\r')"
      [ -z "${scUri}" ] && continue
      printf "Seed: %s\n" "${scUri%%\?*}"
      adb shell am start \
        -a android.intent.action.VIEW \
        -d "${scUri}" \
        -n "${scComponent}" >/dev/null || true
      scCount=$((scCount + 1))
      sleep 0.6
    done < "${scSampleTokens}"
    adb shell am start -n "${scComponent}" >/dev/null || true
    printf "Seed abgeschlossen: %s Tokens aus %s\n" "${scCount}" "${scSampleTokens}"
  else
    adb shell monkey -p "${scPackage}" 1
  fi
else
  printf "Der Build ist fehlgeschlagen. Un-/Install and Start useless\n"
  exit 1
fi
