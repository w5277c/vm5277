/*
 * Copyright 2026 konstantin@5277.ru
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
.IFNDEF OS_OUT_STACK_NR
	.include "stdio/out_hex8.asm"
	.include "stdio/out_cstr.asm"
	.include "stdio/out_char.asm"

_OS_OUT_STACK_NR__TEXT:
	.db "~STACK:",0x00
;-----------------------------------------------------------
OS_OUT_STACK_NR:
;-----------------------------------------------------------
;Выводим данные стека
;MID: YL/H
;-----------------------------------------------------------
	LDS YL,SPL
.IFDEF SPH
	LDS YH,SPH
.ELSE
	LDI YH,0x00
.ENDIF
	ADIW YL,0x03

	PUSH_A16
	LDI ACCUM_H,high(_OS_OUT_STACK_NR__TEXT*2)
	LDI ACCUM_L,low(_OS_OUT_STACK_NR__TEXT*2)
	MCALL OS_OUT_CSTR

	MOV ACCUM_L,YH
	MCALL OS_OUT_HEX8
	MOV ACCUM_L,YL
	MCALL OS_OUT_HEX8
	LDI ACCUM_L,':'
	MCALL OS_OUT_CHAR

_OS_OUT_STACK_NR__LOOP:
	LD ACCUM_L,Y+
	MCALL OS_OUT_HEX8
	CPI YL,low(SRAM_START+SRAM_SIZE)
	BRNE _OS_OUT_STACK_NR__LOOP
	CPI YH,high(SRAM_START+SRAM_SIZE)
	BRNE _OS_OUT_STACK_NR__LOOP

	LDI ACCUM_L,'\n'
	MCALL OS_OUT_CHAR

	POP_A16
	RET
.endif
