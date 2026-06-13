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

.IF OS_FT_TIMER == 0x01
.IFNDEF J8BPROC_UPTIME_S

.include "math/div32.asm"

;-----------------------------------------------------------
J8BPROC_UPTIME_S:
;-----------------------------------------------------------
;Загружаем в аккумулятор (4 байта) UPTIME в секундах (/1000)
;OUT: ACCUM_EH/EL/H/L-uptime в секундах (макс. 1год)
;-----------------------------------------------------------
	PUSH TEMP_L

	PUSH_Z
	LDI_Z _OS_UPTIME
	CLI
	LD ACCUM_L,Z+
	LD ACCUM_H,Z+
	LD ACCUM_EL,Z+
	LD ACCUM_EH,Z+
	LD TEMP_L,Z
	SEI
	POP_Z

	PUSH TEMP_EH
	LDI TEMP_EH,0x03
_J8BPROC_UPTIME_S__LOOP:
	LSR TEMP_L
	ROR ACCUM_EH
	ROR ACCUM_EL
	ROR ACCUM_H
	ROR ACCUM_L
	DEC TEMP_EH
	BRNE _J8BPROC_UPTIME_S__LOOP

	PUSH TEMP_H
	PUSH TEMP_EL
	LDI TEMP_EL,0x00
	LDI TEMP_H,0x00
	LDI TEMP_L,125

	MCALL OS_DIV32
	POP TEMP_EL
	POP TEMP_H
	POP TEMP_EH
	POP TEMP_L
	POP_Z
	RET
.ENDIF
.ENDIF