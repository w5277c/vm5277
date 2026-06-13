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

.IF OS_FT_USTIMER == 0x01
.IFNDEF OS_UPTIME_US

.IF CORE_FREQ==1024000 || CORE_FREQ==512000 || CORE_FREQ==256000 || CORE_FREQ==128000 || CORE_FREQ==64000 || CORE_FREQ==32000 || CORE_FREQ==16000
	.include "math/div32p2.asm"
.ELIF CORE_FREQ==8000
.ELIF CORE_FREQ==4000 || CORE_FREQ==2000 || CORE_FREQ==1000 || CORE_FREQ==500 || CORE_FREQ==250 || CORE_FREQ==125
	.include "math/mul32p2.asm"
.ELSE
	.include "math/div32.asm"
	.include "math/mul32.asm"
.ENDIF

;-----------------------------------------------------------
OS_UPTIME_US:
;-----------------------------------------------------------
;Загружаем в аккумулятор 2 байта UPTIME US
;OUT: ACCUM_H/L-uptime в микросекундах
;-----------------------------------------------------------
	PUSH ACCUM_EH
	PUSH ACCUM_EL

_OS_UPTIME_US__IR_WAIT_LOOP:
	LDS ACCUM_L,TIFR2
	SBRC ACCUM_L,TOV2
	RJMP _OS_UPTIME_US__IR_WAIT_LOOP

	LDS ACCUM_L,TCNT2
	MOV ACCUM_H,US_CNTR_H
.IF CORE_FREQ <= 8000
	MOV ACCUM_EL,C0x00
.ELSE
	MOV ACCUM_EL,US_CNTR_EL
.ENDIF
	LDI ACCUM_EH,0x00

.IF CORE_FREQ == 1024000
	MCALL OS_DIV32P2_X128
.ELIF CORE_FREQ == 512000
	MCALL OS_DIV32P2_X64
.ELIF CORE_FREQ == 256000
	MCALL OS_DIV32P2_X32
.ELIF CORE_FREQ == 128000
	MCALL OS_DIV32P2_X16
.ELIF CORE_FREQ == 64000
	MCALL OS_DIV32P2_X8
.ELIF CORE_FREQ == 32000
	MCALL OS_DIV32P2_X4
.ELIF CORE_FREQ == 16000
	MCALL OS_DIV32P2_X2
.ELIF CORE_FREQ == 8000
.ELIF CORE_FREQ == 4000
	MCALL OS_MUL32P2_X2
.ELIF CORE_FREQ == 2000
	MCALL OS_MUL32P2_X4
.ELIF CORE_FREQ == 1000
	MCALL OS_MUL32P2_X8
.ELIF CORE_FREQ == 500
	MCALL OS_MUL32P2_X16
.ELIF CORE_FREQ == 250
	MCALL OS_MUL32P2_X32
.ELIF CORE_FREQ == 125
	MCALL OS_MUL32P2_X64
.ELSE
	PUSH_T32
	LDI TEMP_EH,0x00
	LDI TEMP_EL,0x00
	LDI TEMP_H,0x00
	LDI TEMP_L,0x64
	MCALL OS_MUL32
	LDI TEMP_EH,BYTE4(CORE_FREQ/80)
	LDI TEMP_EL,BYTE3(CORE_FREQ/80)
	LDI TEMP_H,BYTE2(CORE_FREQ/80)
	LDI TEMP_L,BYTE1(CORE_FREQ/80)
	MCALL OS_DIV32
	POP_T32
.ENDIF

	POP ACCUM_EL
	POP ACCUM_EH
	RET
.ENDIF
.ENDIF
