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
.IFNDEF OS_OUT_CHAR
;-----------------------------------------------------------
OS_OUT_CHAR:
;-----------------------------------------------------------
;Вывод символа
;IN: ACCUM_L-байт
;-----------------------------------------------------------
	PUSH_Z
	PUSH ACCUM_L
	PUSH LOOP_CNTR
	PUSH TEMP_L
	PUSH FLAGS

	LDI_Z PORTS_TABLE*2+(STDOUT_PORT>>4)					;PINx
	LPM ZL,Z
	ADIW ZL,0x02											;PORTx
	CLR ZH

	LD TEMP_L,Z
	ORI TEMP_L,(EXP2 (STDOUT_PORT & 0x0f))
	ST Z,TEMP_L
	LDI TEMP_L,0x14
	DEC TEMP_L
	BRNE PC-0x01

	LDS FLAGS,SREG
	CLI
	LD TEMP_L,Z
	ANDI TEMP_L,low(~(EXP2 (STDOUT_PORT & 0x0f)))
	ST Z,TEMP_L
	LDI TEMP_L,0x14;0x08
	DEC TEMP_L
	BRNE PC-0x01

	;DATA BITS
	LDI LOOP_CNTR,0x08
_OS_OUT_CHAR__LOOP:

	LD TEMP_L,Z
	SBRC ACCUM_L,0x00
	ORI TEMP_L,(EXP2 (STDOUT_PORT & 0x0f))
	SBRS ACCUM_L,0x00
	ANDI TEMP_L,low(~(EXP2 (STDOUT_PORT & 0x0f)))
	LSR ACCUM_L
	ST Z,TEMP_L

	NOP
	LDI TEMP_L,0x13
	DEC TEMP_L
	BRNE PC-0x01
	DEC LOOP_CNTR
	BRNE _OS_OUT_CHAR__LOOP

	;STOP
	LD TEMP_L,Z
	ORI TEMP_L,(EXP2 (STDOUT_PORT & 0x0f))
	ST Z,TEMP_L

	STS SREG,FLAGS
	POP FLAGS
	POP TEMP_L
	POP LOOP_CNTR
	POP ACCUM_L
	POP_Z
	RET
.ENDIF

