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
.IFNDEF OS_DIV32
;-----------------------------------------------------------
OS_DIV32:
;-----------------------------------------------------------
;Деление 32b числа на 32b число
;IN: ACCUM_L/H/EL/EH-32b делимое,
;TEMP_L/H/EL/EH-16b делитель
;OUT: ACCUM_L/H/EL/EH-32b результат,
;TEMP_L/H/EL/EH-32b остаток
;-----------------------------------------------------------
	PUSH RESULT
	PUSH_Z
	PUSH_X

	LDI RESULT,0x21
	SUB XL,XL
	CLR XH
	CLR ZL
	CLR ZH

_OS_DIV32__LOOP:
	ROL ACCUM_L
	ROL ACCUM_H
	ROL ACCUM_EL
	ROL ACCUM_EH
	DEC RESULT
	BREQ _OS_DIV32__END
	ROL XL
	ROL XH
	ROL ZL
	ROL ZH
	SUB XL,TEMP_L
	SBC XH,TEMP_H
	SBC ZL,TEMP_EL
	SBC ZH,TEMP_EH
	BRCS PC+0x03
	SEC
	RJMP _OS_DIV32__LOOP
	ADD XL,TEMP_L
	ADC XH,TEMP_H
	ADC ZL,TEMP_EL
	ADC ZH,TEMP_EH
	CLC
	RJMP _OS_DIV32__LOOP
_OS_DIV32__END:
	MOVW TEMP_L,XL
	MOVW TEMP_EL,ZL

	POP_X
	POP_Z
	POP RESULT
	RET
.ENDIF
