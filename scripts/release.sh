#!/bin/bash

VERSION=$(cat ../VERSION.txt | tr -d '[:space:]')

ARCHIVE_NAME="vm5277-release-${VERSION}.zip"

(
cd ..

zip -r "$ARCHIVE_NAME" \
    bin/ \
    defs/ \
	docs/devel_intoduction/ \
    examples/ \
    rtos/ \
    runtime/ \
	scripts/init.cmd \
    scripts/init.ps1 \
    scripts/init.sh \
	tools/ \
	var/info.txt \
    FAQ.md \
	LICENSE-2.0.txt \
	NOTICE \
    instruction.pdf \
    README.md \
	README.en.md \
	README.ru.md \
    VERSION.txt
)