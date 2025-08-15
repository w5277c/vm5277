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

.include "stdio/out_char.asm"
.include "math/div32.asm"

.IFNDEF OS_OUT_NUM32
;-----------------------------------------------------------
OS_OUT_NUM32:
;-----------------------------------------------------------
;Вывод в десятеричной форме числа(32 бит)
;IN: ACCUM_L/H/EL/EH-32b число
;-----------------------------------------------------------
	PUSH ACCUM_L
	PUSH ACCUM_H
	PUSH ACCUM_EL
	PUSH ACCUM_EH
	PUSH TEMP_L
	PUSH TEMP_H
	PUSH TEMP_EL
	PUSH TEMP_EH
	PUSH XL

	CLR TEMP_H
	CLR TEMP_EL
	CLR TEMP_EH
	LDI XL,0x09
_OS_OUT_NUM32__LOOP1:
	LDI TEMP_L,0x0a
	MCALL OS_DIV32
	PUSH TEMP_L
	DEC XL
	BRNE _OS_OUT_NUM32__LOOP1
	PUSH ACCUM_L

	CLR TEMP_EL
	LDI TEMP_EH,0x0a
_OS_OUT_NUM32__LOOP2:
	POP ACCUM_L
	CPI TEMP_EH,0x01
	BREQ PC+0x03
	OR TEMP_EL,ACCUM_L
	BREQ PC+0x02+_MCALL_SIZE
	SUBI ACCUM_L,low(0x100-0x30)
	MCALL OS_OUT_CHAR
	DEC TEMP_EH
	BRNE _OS_OUT_NUM32__LOOP2

	POP XL
	POP TEMP_EH
	POP TEMP_EL
	POP TEMP_H
	POP TEMP_L
	POP ACCUM_EH
	POP ACCUM_EL
	POP ACCUM_H
	POP ACCUM_L
	RET
.ENDIF
