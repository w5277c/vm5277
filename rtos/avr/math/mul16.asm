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

.include "math/mul8.asm"
.IFNDEF OS_MUL16
;-----------------------------------------------------------
OS_MUL16:
;-----------------------------------------------------------
;Умножение 16бит числа на 16бит число
;IN: ACCUM_L/H-16b число, ACCUM_EL/EH-16b число
;OUT: ACCUM_L/H/EL/EH-32b результат
;-----------------------------------------------------------
;TODO если не поддерживается аппаратное умножение, то лучше сразу здесь считать в цикле, чем вызывать mul8
	PUSH XL
	PUSH XH
	PUSH YL
	PUSH YH
	PUSH FLAGS

	CLR YL
	CLR YH

	MOV FLAGS,ACCUM_H
	PUSH ACCUM_L

	MOV ACCUM_H,ACCUM_EL
	RCALL OS_MUL8
	MOVW XL,ACCUM_L

	POP ACCUM_L
	MOV ACCUM_H,ACCUM_EH
	RCALL OS_MUL8
	ADD XH,ACCUM_L
	ADC YL,ACCUM_H
	ADC YH,C0x00

	MOV ACCUM_L,FLAGS
	MOV ACCUM_H,ACCUM_EL
	RCALL OS_MUL8
	ADD XH,ACCUM_L
	ADC YL,ACCUM_H
	ADC YH,C0x00

	MOV ACCUM_L,FLAGS
	MOV ACCUM_H,ACCUM_EH
	RCALL OS_MUL8
	ADD YL,ACCUM_L
	ADC YH,ACCUM_H

	MOVW ACCUM_L,XL
	MOVW ACCUM_EL,YL

	POP FLAGS
	POP YH
	POP YL
	POP XH
	POP XL
	RET
.ENDIF
