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

;Процедура для вывода символа через UART

.IFNDEF OS_STDOUT_SEND_BYTE
.IF OS_FT_STDOUT == 0x01
.IF OS_FT_DEV_MODE == 0x00
;-----------------------------------------------------------
OS_STDOUT_SEND_BYTE:
;-----------------------------------------------------------
;Вывод символа
;IN: Z-адрес на PINx, ACCUM_L-байт
;-----------------------------------------------------------
	PUSH_T16
	PUSH ACCUM_L

	LDS TEMP_L,SREG											;Запоминаем флаг I и запрещаем прерывания
	CLI
	PUSH TEMP_L

.IFDEF STDIO_PORT_REGID
	CBI STDIO_PORT_REGID,STDIO_PINNUM						;Выставляем START
.ENDIF
	LDI TEMP_H,_OS_STDIO_BITDELAY_CONST
	MCALL OS_STDIO_1BIT_DELAY

	;DATA BITS
	LDI TEMP_L,0x08
OS_STDOUT_SEND_BYTE__LOOP:
	SBRC ACCUM_L,0x00										;В зависимости от текущего бита задаем на пине высокий или низкий уровень
.IFDEF STDIO_PORT_REGID
	SBI STDIO_PORT_REGID,STDIO_PINNUM
.ELSE
	SBI STDOUT_PORT_REGID,STDOUT_PINNUM
.ENDIF
	SBRS ACCUM_L,0x00
.IFDEF STDIO_PORT_REGID
	CBI STDIO_PORT_REGID,STDIO_PINNUM
.ELSE
	CBI STDOUT_PORT_REGID,STDOUT_PINNUM
.ENDIF
	LSR ACCUM_L												;Сдвигаем биты в аккумуляторе
	LDI TEMP_H,_OS_STDIO_BITDELAY_CONST-1
	RCALL OS_STDIO_1BIT_DELAY
	DEC TEMP_L
	BRNE OS_STDOUT_SEND_BYTE__LOOP

	;STOP
.IFDEF STDIO_PORT_REGID
	SBI STDIO_PORT_REGID,STDIO_PINNUM						;Выставляем STOP
.ELSE
	SBI STDOUT_PORT_REGID,STDOUT_PINNUM
.ENDIF
	POP TEMP_L
	STS SREG,TEMP_L											;Восстанавливаем флаг I

	LDI TEMP_H,_OS_STDIO_BITDELAY_CONST
	RCALL OS_STDIO_1BIT_DELAY

	POP ACCUM_L
	POP_T16
	RET
.ENDIF
.ENDIF
.ENDIF

