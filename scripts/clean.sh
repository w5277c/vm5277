#!/bin/bash

find ../examples/j8b/ -type d -name "target" -exec rm -rf {} + 2>/dev/null
find ../examples/j8b/ -type d -name ".idea" -exec rm -rf {} + 2>/dev/null
find ../examples/j8b/ -type d -name "prebuild" -exec rm -rf {} + 2>/dev/null
rm -rf ../common/common-lib/target
rm -rf ../common/compiler/target
rm -rf ../common/mkboot/target
rm -rf ../common/flasher/target
rm -rf ../common/avr-codegen/target
rm -rf ../common/stub-codegen/target
rm -rf ../common/avr-assembler/target
rm -rf ../common/ide-plugins/idea-plugin/.gradle
rm -rf ../common/ide-plugins/idea-plugin/.idea
rm -rf ../common/ide-plugins/idea-plugin/.intellijPlatform
rm -rf ../common/ide-plugins/idea-plugin/build
rm -rf ../common/ide-plugins/nb-plugin/target
rm -rf ../common/lsp-server/target
rm -rf ../common/maven-tools/j8b-archetype/target
rm -rf ../common/maven-tools/j8b-packaging/target
rm -rf ../common/maven-tools/j8b-plugin/target
rm -rf ../common/runtime/.idea
rm -rf ../common/avr-rtos/.idea