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
.include "math/div10.asm"
.include "math/mul10.asm"

.IFNDEF OS_OUT_NUM8
;-----------------------------------------------------------
OS_OUT_NUM8:
;-----------------------------------------------------------
;Вывод в десятеричной форме числа(8 бит)
;IN: ACCUM_L-байт
;-----------------------------------------------------------
	PUSH TEMP_L
	PUSH TEMP_H
	PUSH ACCUM_H
	PUSH ACCUM_L

	CLR TEMP_H
	LDI TEMP_L,0x03
_OS_OUT_NUM8__LOOP:
	MOV ACCUM_H,ACCUM_L
	MCALL OS_DIV10
	PUSH ACCUM_L
	MCALL OS_MUL10
	SUB ACCUM_H,ACCUM_L
	POP ACCUM_L

	BRNE PC+0x02
	INC TEMP_H
	CPSE TEMP_H,C0x00
	PUSH ACCUM_H
	DEC TEMP_L
	BRNE _OS_OUT_NUM8__LOOP

	CPI TEMP_H,0x00
	BRNE _OS_OUT_NUM8__NOT_ZERO
	LDI TEMP_H,0x01
	PUSH C0x00
_OS_OUT_NUM8__NOT_ZERO:
	POP ACCUM_L
	SUBI ACCUM_L,(0x100-0x30)
	MCALL OS_OUT_CHAR
	DEC TEMP_H
	BRNE _OS_OUT_NUM8__NOT_ZERO

_OS_OUT_NUM8__END:
	POP ACCUM_L
	POP ACCUM_H
	POP TEMP_L
	POP TEMP_H
	RET
.ENDIF
