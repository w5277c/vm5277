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
.include "stdio/out_cstr.asm"
.include "stdio/out_hex8.asm"

.IFNDEF J8BPROC_ETRACE_OUT
_J8BPROC_ETRACE_OUT_STR:
	.db "ETRC:",0x00
;-----------------------------------------------------------
J8BPROC_ETRACE_OUT:
;-----------------------------------------------------------
;Вывод исключения и его точек прохода
;-----------------------------------------------------------
	PUSH_X
	PUSH ACCUM_L

	LDI ACCUM_L,'\n'
	MCALL OS_OUT_CHAR
	LDI ACCUM_L,0x00
	MCALL OS_OUT_CHAR
	PUSH_Z
	LDI_Z _J8BPROC_ETRACE_OUT_STR*2
	MCALL OS_OUT_CSTR
	POP_Z
	
	LDI_X _OS_ETRACE_BUFFER
	LD ACCUM_L,X+
	MCALL OS_OUT_HEX8
	LDI ACCUM_L,'.'
	MCALL OS_OUT_CHAR
	LD ACCUM_L,X+
	MCALL OS_OUT_HEX8
	LDI ACCUM_L,':'
	MCALL OS_OUT_CHAR
.IF OS_ETRACE_POINT_BITSIZE==0x07
	LD ACCUM_L,X+											;Выводим первую точку
	MCALL OS_OUT_HEX8

	PUSH TEMP_L
	PUSH ACCUM_EL
	LDI TEMP_L,OS_ETRACE_BUFFER_SIZE-0x03
_J8BPROC_ETRACE_OUT__LOOP:
	LD ACCUM_EL,X+											;Считываем первую точку
	CPI ACCUM_EL,0x00
	BREQ _J8BPROC_ETRACE_OUT__DONE
	LDI ACCUM_L,'>'
	MCALL OS_OUT_CHAR
	SBRS ACCUM_EL,0x07
	RJMP PC+0x02 + _MCALL_SIZE*2
	LDI ACCUM_L,'.'
	MCALL OS_OUT_CHAR
	MCALL OS_OUT_CHAR
	MOV ACCUM_L,ACCUM_EL
	MCALL OS_OUT_HEX8
	DEC TEMP_L
	BRNE _J8BPROC_ETRACE_OUT__LOOP
_J8BPROC_ETRACE_OUT__DONE:
	POP ACCUM_EL
	POP TEMP_L
	
.ELSE
	LD ACCUM_L,X+											;Выводим первую точку (старший байт)
	MCALL OS_OUT_HEX8
	LD ACCUM_L,X+											;Выводим первую точку (младший байт)
	MCALL OS_OUT_HEX8

	PUSH TEMP_L
	PUSH ACCUM_EL
	PUSH ACCUM_EH
	LDI TEMP_L,(OS_ETRACE_BUFFER_SIZE-0x02)/2-1
_J8BPROC_ETRACE_OUT__LOOP:
	LD ACCUM_EH,X+
	LD ACCUM_EL,X+
	CPI ACCUM_EH,0x00
	BRNE PC+0x03
	CPI ACCUM_EL,0x00
	BREQ _J8BPROC_ETRACE_OUT__DONE
	LDI ACCUM_L,'>'
	MCALL OS_OUT_CHAR
	SBRS ACCUM_EH,0x07
	RJMP PC+0x02+_MCALL_SIZe*2
	LDI ACCUM_L,'.'
	MCALL OS_OUT_CHAR
	MCALL OS_OUT_CHAR
	MOV ACCUM_L,ACCUM_EH
	MCALL OS_OUT_HEX8
	MOV ACCUM_L,ACCUM_EL
	MCALL OS_OUT_HEX8
	DEC TEMP_L
	BRNE _J8BPROC_ETRACE_OUT__LOOP
_J8BPROC_ETRACE_OUT__DONE:
	POP ACCUM_EH
	POP ACCUM_EL
	POP TEMP_L
.ENDIF

	LDI ACCUM_L,'\n'
	MCALL OS_OUT_CHAR

	POP ACCUM_L
	POP_X
	RET
.ENDIF
