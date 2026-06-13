/*
 * Copyright 2025 konstantin@5277.ru
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

;Выводим блок памяти на STDIO порт
.IFNDEF OS_OUT_RAMBYTES_NR
	.include "stdio/out_hex8.asm"
;-----------------------------------------------------------
OS_OUT_RAMBYTES_NR:
;-----------------------------------------------------------
;Выводим блок данных RAM
;IN: Z-адрес начала блока, TEMP_H/L-длина блока
;MID: ZL/H,TEMP_L/H
;-----------------------------------------------------------
	PUSH ACCUM_L

_OS_OUT_RAMBYTES_NR__LOOP:
	LD ACCUM_L,Z+
	MCALL OS_OUT_HEX8
	SBIW TEMP_L,0x01
	BRNE _OS_OUT_RAMBYTES_NR__LOOP

	POP ACCUM_L
	RET
.endif
