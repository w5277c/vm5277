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
.include "stdio/out_num8.asm"
.include "math/mul32.asm"
.include "math/div32.asm"

.IFNDEF OS_OUT_Q7N8
;-----------------------------------------------------------
OS_OUT_Q7N8:
;-----------------------------------------------------------
;Вывод в десятеричной форме FIXED(Q7.8) числа(16 бит)
;IN: ACCUM_L/H-16b число
;-----------------------------------------------------------
	PUSH TEMP_EH
	PUSH TEMP_EL
	PUSH TEMP_H
	PUSH TEMP_L
	PUSH XL

	MOVW TEMP_L,ACCUM_L
	SBRS TEMP_H,0x07
	RJMP _OS_OUT_Q7N8__POSITIVE
	LDI ACCUM_L,'-'
	MCALL OS_OUT_CHAR
    COM TEMP_L
    COM TEMP_H
    ADIW TEMP_L,1

_OS_OUT_Q7N8__POSITIVE:
	MOV ACCUM_L,TEMP_H
	MCALL OS_OUT_NUM8
	LDI ACCUM_L,'.'
	MCALL OS_OUT_CHAR

	LDI ACCUM_EH,0x00
	LDI ACCUM_EL,0x05
	LDI ACCUM_H,0xf5
	LDI ACCUM_L,0xe1
	LDI TEMP_EH,0x00
	LDI TEMP_EL,0x00
	LDI TEMP_H,0x00
	MCALL OS_MUL32_NR

	LDI XL,0x01
_OS_OUT_Q7N8__DIV_LOOP:
	LDI TEMP_L,0x0a
	MCALL OS_DIV32
	PUSH TEMP_L
	INC XL
	TST ACCUM_L
	BRNE _OS_OUT_Q7N8__DIV_LOOP
	TST ACCUM_H
	BRNE _OS_OUT_Q7N8__DIV_LOOP
	TST ACCUM_EL
	BRNE _OS_OUT_Q7N8__DIV_LOOP
	TST ACCUM_EH
	BRNE _OS_OUT_Q7N8__DIV_LOOP

_OS_OUT_Q7N8__PRINT_LOOP:
	POP ACCUM_L
	SUBI ACCUM_L,low(0x100-0x30)
	MCALL OS_OUT_CHAR
	DEC XL
	BRNE _OS_OUT_Q7N8__PRINT_LOOP

_OS_OUT_Q7N8__END:
	POP XL
	POP TEMP_L
	POP TEMP_H
	POP TEMP_EL
	POP TEMP_EH
	RET
.ENDIF
