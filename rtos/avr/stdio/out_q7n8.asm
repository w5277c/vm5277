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

.include "stdio/stdout.asm"
.include "stdio/out_num8.asm"
.include "math/mul8.asm"

.IFNDEF OS_OUT_Q7N8
;-----------------------------------------------------------
OS_OUT_Q7N8:
;-----------------------------------------------------------
;Вывод в десятеричной форме FIXED(Q7.8) числа(16 бит)
;IN: ACCUM_L/H-16b число
;-----------------------------------------------------------
.IF OS_FT_STDOUT == 0x01
	PUSH_T16
	PUSH_A16

.IFDEF STDIO_PORT_REGID
.IF OS_FT_STDIN == 0x01
	MCALL OS_STDIO_SEND_MODE_NR
.ENDIF
.ENDIF

	MOVW TEMP_L,ACCUM_L
	SBRS ACCUM_H,0x07
	RJMP _OS_OUT_Q7N8__POSITIVE
	LDI ACCUM_L,'-'
.IF OS_FT_DEV_MODE
	MCALL PROC__BLDR_UART_SEND_BYTE_NR
.ELSE
	MCALL OS_STDOUT_SEND_BYTE
.ENDIF
	COM TEMP_L
	COM TEMP_H
	ADIW TEMP_L,1

_OS_OUT_Q7N8__POSITIVE:
	MOV ACCUM_L,TEMP_H
	MCALL _OS_OUT_NUM8__SUB
	LDI ACCUM_L,'.'
.IF OS_FT_DEV_MODE == 0x01
	MCALL PROC__BLDR_UART_SEND_BYTE_NR
.ELSE
	MCALL OS_STDOUT_SEND_BYTE
.ENDIF

	MOV ACCUM_L,TEMP_L
	LDI ACCUM_H,0x64
	MCALL OS_MUL8
	CPI ACCUM_H,99
	BREQ PC+0x04
	LDI TEMP_L,0x80
	ADD ACCUM_L,TEMP_L
	ADC ACCUM_H,C0x00
	CPI ACCUM_H,0x0a
	BRCC _OS_OUT_Q7N8__SKIP_ZERO
	LDI ACCUM_L,'0'
.IF OS_FT_DEV_MODE
	MCALL PROC__BLDR_UART_SEND_BYTE_NR
.ELSE
	MCALL OS_STDOUT_SEND_BYTE
.ENDIF
_OS_OUT_Q7N8__SKIP_ZERO:
	MOV ACCUM_L,ACCUM_H
	MCALL _OS_OUT_NUM8__SUB

_OS_OUT_Q7N8__END:
.IFDEF STDIO_PORT_REGID
.IF OS_FT_STDIN == 0x01
	MCALL OS_STDIO_RECV_MODE_NR
.ENDIF
.ENDIF

	POP_A16
	POP_T16
.ENDIF
	RET
.ENDIF
