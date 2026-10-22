#!/usr/bin/env bash

LOCAL_JRE_DIR="$vm5277/tools/jre"
JRE_TMP_FILE="$vm5277/tools/jre.tar.gz"

error_exit() {
    echo -e "[ERROR] $1" >&2
    [ -f "$JRE_TMP_FILE" ] && rm "$JRE_TMP_FILE"
    exit 2
}

if [ -f "$LOCAL_JRE_DIR/bin/java" ]; then
    exit 1
elif command -v java >/dev/null 2>&1; then
    exit 0
else
    OS="$(uname -s | tr '[:upper:]' '[:lower:]')"
    ARCH="$(uname -m)"
    if [ "$ARCH" = "x86_64" ]; then ARCH="x64"; fi
    if [ "$ARCH" = "aarch64" ]; then ARCH="aarch64"; fi

    JRE_URL="https://vm5277.ru/tools/jre11_"$OS-$ARCH".tar.gz"
    echo "Downloading JRE for '$OS-$ARCH ...'"
    mkdir -p "$LOCAL_JRE_DIR"
    if ! curl -f -L "$JRE_URL" -o "$JRE_TMP_FILE"; then
        error_exit "Failed to download the file. Please check your internet connection and link availability."
    fi
    if [ ! -s "$JRE_TMP_FILE" ]; then
        error_exit "The downloaded file is empty or missing."
    fi
    if ! tar -xzf "$JRE_TMP_FILE" -C "$LOCAL_JRE_DIR" --strip-components=1; then
        error_exit "Error extracting the archive. The file might be corrupted."
    fi

    rm "$JRE_TMP_FILE"
    if [ -f "$LOCAL_JRE_DIR/bin/java" ]; then
        chmod +x "$LOCAL_JRE_DIR/bin/java"
        echo "Portable JRE has been successfully deployed."
        echo
        exit 1
    else
        error_exit "The archive was extracted, but the java executable was not found."
    fi
fi
