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

.IFNDEF OS_DELAY_US_NR

.IF CORE_FREQ > 8000
	.include "math/mul16p2.asm"
.ELIF CORE_FREQ >= 4800
	; Частоты от 4.8 МГц до 8 МГц: коэффициенты 1:1, математика не требуется
.ELIF CORE_FREQ >= 125
	.include "math/div16p2.asm"
.ENDIF

;-----------------------------------------------------------
OS_DELAY_US_NR:
;-----------------------------------------------------------
;Ждем необходимое время в микросекундах
;IN: ACCUM_L-uptime в микросекундах
;MOD:ACCUM_EH/EL/H/L
;-----------------------------------------------------------
	PUSH TEMP_L

	CPI ACCUM_L,0
	BREQ _OS_DELAY_US__END

	LDI ACCUM_H,0x00

.IF CORE_FREQ > 512000
	MCALL OS_MUL16P2_X128
.ELIF CORE_FREQ > 256000
	MCALL OS_MUL16P2_X64
.ELIF CORE_FREQ > 128000
	MCALL OS_MUL16P2_X32
.ELIF CORE_FREQ > 64000
	MCALL OS_MUL16P2_X16
.ELIF CORE_FREQ > 32000
	MCALL OS_MUL16P2_X8
.ELIF CORE_FREQ > 16000
	MCALL OS_MUL16P2_X4
.ELIF CORE_FREQ > 8000
	MCALL OS_MUL16P2_X2
.ELIF CORE_FREQ >= 4800
	; Частоты от 4.8 МГц до 8 МГц: коэффициент 1:1
.ELIF CORE_FREQ >= 4000
	MCALL OS_DIV16P2_X2
.ELIF CORE_FREQ >= 2000
	MCALL OS_DIV16P2_X4
.ELIF CORE_FREQ >= 1000
	MCALL OS_DIV16P2_X8
.ELIF CORE_FREQ >= 500
	MCALL OS_DIV16P2_X16
.ELIF CORE_FREQ >= 250
	MCALL OS_DIV16P2_X32
.ELIF CORE_FREQ >= 125
	MCALL OS_DIV16P2_X64
.ELSE
	.ERROR "UNSUPPORTED OS_DELAY_US function"
.ENDIF

	CP ACCUM_L,C0x00
	CPC ACCUM_H,C0x00
	BREQ _OS_DELAY_US__END

	LDS TEMP_L,TCNT0
	LDI ACCUM_EH,0
_OS_DELAY_US__LOOP:
	LDS ACCUM_EL,TCNT0
	SUB ACCUM_EL,TEMP_L
	SBCI ACCUM_EH,0
	BRCC _OS_DELAY_US__LOOP_NO_OVERFLOW
	SUBI ACCUM_EL,low(-_OS_TIMER_FREQ)
	SBCI ACCUM_EH,high(-_OS_TIMER_FREQ)
_OS_DELAY_US__LOOP_NO_OVERFLOW:
	CP ACCUM_EL,ACCUM_L
	CPC ACCUM_EH,ACCUM_H
	BRLO _OS_DELAY_US__LOOP

_OS_DELAY_US__END:
	POP TEMP_L
	RET
.ENDIF