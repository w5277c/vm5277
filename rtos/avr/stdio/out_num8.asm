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
 
 ;TODO проверить на предмет оптимизации

.include "stdio/stdout.asm"
.include "math/diva8by10.asm"
.include "math/mula8by10.asm"

.IFNDEF OS_OUT_NUM8
;-----------------------------------------------------------
OS_OUT_NUM8:
;-----------------------------------------------------------
;Вывод в десятеричной форме числа(8 бит)
;IN: ACCUM_L-8b число
;-----------------------------------------------------------
.IF OS_FT_STDOUT == 0x01
.IFDEF STDIO_PORT_REGID
.IF OS_FT_STDIN == 0x01
	MCALL OS_STDIO_SEND_MODE_NR
.ENDIF
.ENDIF
	RCALL _OS_OUT_NUM8__SUB
.IFDEF STDIO_PORT_REGID
.IF OS_FT_STDIN == 0x01
	MCALL OS_STDIO_RECV_MODE_NR
.ENDIF
.ENDIF
	RET
	
_OS_OUT_NUM8__SUB:
	PUSH TEMP_EH
	PUSH_T16
	PUSH_A16
	CLR TEMP_H
	LDI TEMP_L,0x03
_OS_OUT_NUM8__LOOP:
	MOV ACCUM_H,ACCUM_L
	MCALL OS_DIVA8BY10
	PUSH ACCUM_L
	MCALL OS_MULA8BY10
	SUB ACCUM_H,ACCUM_L
	POP ACCUM_L
	BRNE PC+0x03
	CPI ACCUM_L,0x00
	BREQ OS_OUT_NUM8__PRINT
	PUSH ACCUM_H
	DEC TEMP_L
	BRNE _OS_OUT_NUM8__LOOP

OS_OUT_NUM8__PRINT:
	CPI TEMP_L,0x03
	BRNE _OS_OUT_NUM8__NOT_ZERO
	LDI TEMP_H,0x01
	PUSH C0x00
	DEC TEMP_L
_OS_OUT_NUM8__NOT_ZERO:
	LDI TEMP_EH,0x03
	SUB TEMP_EH,TEMP_L
_OS_OUT_NUM8__PRINT_LOOP:
	POP ACCUM_L
	SUBI ACCUM_L,(0x100-0x30)
.IF OS_FT_DEV_MODE == 0x01
	MCALL PROC__BLDR_UART_SEND_BYTE_NR
.ELSE
	MCALL OS_STDOUT_SEND_BYTE
.ENDIF
	DEC TEMP_EH
	BRNE _OS_OUT_NUM8__PRINT_LOOP
	POP_A16
	POP_T16
	POP TEMP_EH
	RET
	
.ENDIF
.ENDIF
