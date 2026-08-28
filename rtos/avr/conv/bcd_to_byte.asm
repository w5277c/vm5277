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

.IFNDEF BCD_TO_BYTE
.include "math/mul8.asm"
;-----------------------------------------------------------
BCD_TO_BYTE:
;-----------------------------------------------------------
;Конвертируем BCD(два ниббла) в число в виде байта
;IN: ACCUM_L-ниблы(0-9,0-9)
;OUT: ACCUM_L-значение в виде байта (0-99)
;MOD: ACCUM_H/EL
;-----------------------------------------------------------
	MOV ACCUM_EL,ACCUM_L
	SWAP ACCUM_L
	ANDI ACCUM_L,0x0f
	ANDI ACCUM_EL,0x0f
	LDI ACCUM_H,10
	MCALL OS_MUL8
	ADD ACCUM_L,ACCUM_EL
	RET
.ENDIF

