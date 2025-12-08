#/bin/bash

for file in *.j8b; do
    j8bc avr:atmega328p $file --cg-verbose 0 --dump-ir --opt size
done