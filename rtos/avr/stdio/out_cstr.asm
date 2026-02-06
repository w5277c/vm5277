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

.IFNDEF OS_OUT_CSTR
;--------------------------------------------------------
OS_OUT_CSTR:
;--------------------------------------------------------
;Логирование строки, конец определяется по 0x00
;IN: Z-адрес FLASH на строку
;--------------------------------------------------------
.IF OS_FT_STDOUT == 0x01
	PUSH_Z
	PUSH ACCUM_L

.IFDEF STDIO_PORT_REGID
.IF OS_FT_STDIN == 0x01
	MCALL OS_STDIO_SEND_MODE_NR
.ENDIF
.ENDIF

_OS_OUT_CSTR__LOOP:
	;Считываем байт
	LPM ACCUM_L,Z+
	CPI ACCUM_L,0x00
	BREQ _OS_OUT_CSTR__END
.IF OS_FT_DEV_MODE
	MCALL PROC__BLDR_UART_SEND_BYTE_NR
.ELSE
	MCALL OS_STDOUT_SEND_BYTE
.ENDIF
	RJMP _OS_OUT_CSTR__LOOP

_OS_OUT_CSTR__END:

.IFDEF STDIO_PORT_REGID
.IF OS_FT_STDIN == 0x01
	MCALL OS_STDIO_RECV_MODE_NR
.ENDIF
.ENDIF

	POP ACCUM_L
	POP_Z
.ENDIF
	RET
.ENDIF
