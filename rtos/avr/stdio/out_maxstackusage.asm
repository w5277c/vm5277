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

.IFNDEF OS_OUT_MAX_STACK_USAGE
	.include "stdio/out_hex16.asm"
	.include "stdio/out_char.asm"

_OS_OUT_MAX_STACK_USAGE__TEXT:
	.db "MSTCKUSG:",0x00
;-----------------------------------------------------------
OS_OUT_MAX_STACK_USAGE:
;-----------------------------------------------------------
;Выводим размер используемого стека и его полный размер
;MID: ACCUM_L/H
;-----------------------------------------------------------
	PUSH_Y

	LDI ACCUM_L,0x00
	MCALL OS_OUT_CHAR
	LDI ACCUM_H,high(_OS_OUT_MAX_STACK_USAGE__TEXT*2)
	LDI ACCUM_L,low(_OS_OUT_MAX_STACK_USAGE__TEXT*2)
	MCALL OS_OUT_CSTR

	LDS ACCUM_L,STACK_BORDER
	CPI ACCUM_L,STACK_GUARD_VALUE
	BREQ _OS_OUT_MAX_STACK_USAGE__NO_OVERFLOW
	LDI_Y SRAM_START+SRAM_SIZE+1							;Должно получиться 0xffff после вычитания этого значения из SRAM_START+SRAM_SIZE
	RJMP _OS_OUT_MAX_STACK_USAGE__OUT
_OS_OUT_MAX_STACK_USAGE__NO_OVERFLOW:

	LDI_Y SRAM_START+SRAM_SIZE-OS_STACK_SIZE
_OS_OUT_MAX_STACK_USAGE__LOOP:
	LD ACCUM_L,Y
	CPI ACCUM_L,STACK_GUARD_VALUE
	BRNE _OS_OUT_MAX_STACK_USAGE__OUT
	LDI ACCUM_L,high(SRAM_START+SRAM_SIZE-1)
	CPI YL,low(SRAM_START+SRAM_SIZE-1)
	CPC YH,ACCUM_L
	BREQ _OS_OUT_MAX_STACK_USAGE__OUT
	ADIW YL,0x01
	RJMP _OS_OUT_MAX_STACK_USAGE__LOOP
_OS_OUT_MAX_STACK_USAGE__OUT:

	LDI ACCUM_L,low(SRAM_START+SRAM_SIZE)
	LDI ACCUM_H,high(SRAM_START+SRAM_SIZE)
	SUB ACCUM_L,YL
	SBC ACCUM_H,YH
	MCALL OS_OUT_HEX16
	LDI ACCUM_L,'/'
	MCALL OS_OUT_CHAR
	LDI ACCUM_L,low(OS_STACK_SIZE)
	LDI ACCUM_H,high(OS_STACK_SIZE)
	MCALL OS_OUT_HEX16

	LDI ACCUM_L,'\n'
	MCALL OS_OUT_CHAR

	POP_Y
	RET
.ENDIF
