#!/usr/bin/env bash

VM_DIR="$(cd "$(dirname "${BASH_SOURCE}")/.." && pwd)"

if [ -n "$ZSH_VERSION" ] || [ "${SHELL##*/}" = "zsh" ]; then
    RC_FILE="$HOME/.zshrc"
else
    RC_FILE="$HOME/.bashrc"
fi


for rc in "$RC_FILE" "$HOME/.xsessionrc"; do
    if [ "$rc" = "$RC_FILE" ] || [ -f "$rc" ]; then
        [ -f "$rc" ] || touch "$rc"
        if ! grep -q "export vm5277=" "$rc"; then
            printf "\nexport vm5277=\"%s\"\nexport PATH=\"\$PATH:\$vm5277/bin\"\n" "$VM_DIR" >> "$rc"
        fi
    fi
done

export vm5277="$VM_DIR"
export PATH="$PATH:$vm5277/bin"

echo "[OK] VM5277 Toolkit initialized successfully."

