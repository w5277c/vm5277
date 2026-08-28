#!/bin/bash

find ../examples/j8b/ -type d -name "target" -exec rm -rf {} + 2>/dev/null
find ../examples/j8b/ -type d -name ".idea" -exec rm -rf {} + 2>/dev/null
find ../examples/j8b/ -type d -name "prebuild" -exec rm -rf {} + 2>/dev/null
rm -rf ../common/mkboot/target
rm -rf ../common/stub_codegen/target