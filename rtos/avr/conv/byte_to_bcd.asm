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

.IFNDEF BYTE_TO_BCD
.include "math/div8.asm"
;-----------------------------------------------------------
BYTE_TO_BCD:
;-----------------------------------------------------------
;Конвертируем 2 значное число в BCD(два ниббла)
;IN: ACCUM_L-значение(0-99)
;OUT: ACCUM_L-ниблы(0-9,0-9)
;MOD: ACCUM_H
;-----------------------------------------------------------
	PUSH TEMP_L

	LDI ACCUM_H,10
	MCALL OS_DIV8
	SWAP ACCUM_L
	ANDI ACCUM_L,0xf0
	OR ACCUM_L,TEMP_L

	POP TEMP_L
	RET
.ENDIF

