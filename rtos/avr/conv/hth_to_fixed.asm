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

.IFNDEF HTH_TO_FIXED
.include "math/div32.asm"
;-----------------------------------------------------------
HTH_TO_FIXED:
;-----------------------------------------------------------
;Сотые в fixed (Q7.8)
;IN: ACCUM_L/H-сотые
;OUT: ACCUM_H/-целая знаковая часть, ACCUM_L-после запятой
;-----------------------------------------------------------
	PUSH_T32
	PUSH ACCUM_EH
	PUSH ACCUM_EL

	LDI ACCUM_EH,0
	MOV ACCUM_EL,ACCUM_H
	MOV ACCUM_H,ACCUM_L
	LDI ACCUM_L,0

	LDI TEMP_EH,0
	LDI TEMP_EL,0
	LDI TEMP_H,0
	LDI TEMP_L,100
	MCALL OS_DIV32

	POP ACCUM_EL
	POP ACCUM_EH
	POP_T32
	RET
.ENDIF

