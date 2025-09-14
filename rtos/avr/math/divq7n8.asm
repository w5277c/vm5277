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

.IFNDEF OS_DIVQ7N8
;-----------------------------------------------------------
OS_DIVQ7N8:
;-----------------------------------------------------------
;Деление fixed Q7.8 числа на fixed Q7.8 число
;IN: ACCUM_L/H-16b делимое, ACCUM_EL/EH-16b делитель
;OUT: ACCUM_L/H-16b результат
;-----------------------------------------------------------
	PUSH FLAGS
	PUSH TEMP_L
	PUSH TEMP_H
	PUSH XL
	PUSH XH

	MOV FLAGS,ACCUM_H
	EOR FLAGS,ACCUM_EH
	ANDI FLAGS,0x80

	SBRS ACCUM_H,0x07
	RJMP _OS_DIVQ7N8_OP1_INV_SKIP
	COM ACCUM_L
	COM ACCUM_H
	ADD ACCUM_L,C0x01
	ADC ACCUM_H,C0x00
_OS_DIVQ7N8_OP1_INV_SKIP:

	SBRS ACCUM_EH,0x07
	RJMP _OS_DIVQ7N8_OP2_INV_SKIP
	COM ACCUM_EL
	COM ACCUM_EH
	ADD ACCUM_L,C0x01
	ADC ACCUM_H,C0x00
_OS_DIVQ7N8_OP2_INV_SKIP:

	CLR TEMP_L
	LDI TEMP_H,0x19											;TODO проверить на 17 циклах
	SUB XH,XH
	CLR XL

_OS_DIVQ7N8__LOOP:
	ROL TEMP_L
	ROL ACCUM_L
	ROL ACCUM_H
	DEC TEMP_H
	BREQ _OS_DIVQ7N8__LOOP_END
	ROL XL
	ROL XH
	SUB XL,ACCUM_EL
	SBC XH,ACCUM_EH
	BRCS PC+0x03
	SEC
	RJMP _OS_DIVQ7N8__LOOP
	ADD XL,ACCUM_EL
	ADC XH,ACCUM_EH
	CLC
	RJMP _OS_DIVQ7N8__LOOP
_OS_DIVQ7N8__LOOP_END:
	MOV ACCUM_H,ACCUM_L
	MOV ACCUM_L,TEMP_L

	SBRS FLAGS,0x07
	RJMP _OS_DIVQ7N8_RESULT_INV_SKIP
	COM ACCUM_L
	COM ACCUM_H
	ADD ACCUM_L,C0x01
	ADC ACCUM_H,C0x00
_OS_DIVQ7N8_RESULT_INV_SKIP:

	POP XH
	POP XL
	POP TEMP_H
	POP TEMP_L
	POP FLAGS
	RET
.ENDIF
