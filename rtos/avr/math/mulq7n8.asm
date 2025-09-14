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

.include "math/mul16.asm"

.IFNDEF OS_MULQ7N8
;-----------------------------------------------------------
OS_MULQ7N8:
;-----------------------------------------------------------
;Умножение fixed Q7.8 числа на fixed Q7.8 число
;IN: ACCUM_L/H-16b число, ACCUM_EL/EH-16b число
;OUT: ACCUM_L/H-16b результат
;-----------------------------------------------------------
	PUSH FLAGS
	PUSH ACCUM_EL
	PUSH ACCUM_EH

	MOV FLAGS,ACCUM_H
	EOR FLAGS,ACCUM_EH
	ANDI FLAGS,0x80


	SBRS ACCUM_H,0x07
	RJMP _OS_MULQ7N8_OP1_INV_SKIP
	COM ACCUM_L
	COM ACCUM_H
	ADD ACCUM_L,C0x01
	ADC ACCUM_H,C0x00
_OS_MULQ7N8_OP1_INV_SKIP:

	SBRS ACCUM_EH,0x07
	RJMP _OS_MULQ7N8_OP2_INV_SKIP
	COM ACCUM_EL
	COM ACCUM_EH
	ADD ACCUM_L,C0x01
	ADC ACCUM_H,C0x00
_OS_MULQ7N8_OP2_INV_SKIP:

	MCALL OS_MUL16
	MOV ACCUM_L,ACCUM_H
	MOV ACCUM_H,ACCUM_EL

	SBRS FLAGS,0x07
	RJMP _OS_MULQ7N8_RESULT_INV_SKIP
	COM ACCUM_L
	COM ACCUM_H
	ADD ACCUM_L,C0x01
	ADC ACCUM_H,C0x00
_OS_MULQ7N8_RESULT_INV_SKIP:

	POP ACCUM_EH
	POP ACCUM_EL
	POP FLAGS
	RET
.ENDIF
