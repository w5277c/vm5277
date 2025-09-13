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

;Черновой вариант
.IFNDEF OS_MUL8
;-----------------------------------------------------------
OS_MUL8:
;-----------------------------------------------------------
;Умножение 8бит числа на 8бит число
;IN: ACCUM_L-8b число, ACCUM_H-8b число
;OUT: ACCUM_L/H-16b результат
;-----------------------------------------------------------
.IF MUL_SUPPORT != 0x01
	PUSH TEMP_L
	PUSH TEMP_H
	PUSH TEMP_EL

	MOV TEMP_L,ACCUM_L
	MOV TEMP_H,ACCUM_H
	CLR ACCUM_L
	CLR ACCUM_H
	CLR FLAGS
	CLR TEMP_EL

_OS_MUL8_LOOP:
	LSR TEMP_H
	BRCC PC+0x03
	ADD ACCUM_L,TEMP_L
	ADC ACCUM_H,TEMP_EL
	LSL TEMP_L
	ROL TEMP_EL
	CPI TEMP_H,0x00
	BRNE _OS_MUL8_LOOP

	POP TEMP_EL
	POP TEMP_L
	POP TEMP_H
.ELSE
	PUSH TEMP
	LDS TEMP,SREG
	CLI

	MUL ACCUM_L,ACCUM_H
	MOV ACCUM_L,_RESULT_L											;Не сохраняем, используются только в блоке без прерываний
	MOV ACCUM_H,_RESULT_H

	STS SREG,TEMP
	POP TEMP
.ENDIF
	RET
.ENDIF
