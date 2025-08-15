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

.IFNDEF OS_DIV16
;-----------------------------------------------------------
OS_DIV16:
;-----------------------------------------------------------
;Деление 16b числа на 16бит число
;IN: ACCUM_L/H-16b делимое, TEMP_L/H-16b делитель
;OUT: ACCUM_L/H-16b результат, TEMP_L/H-16b остаток
;-----------------------------------------------------------
	PUSH XL
	PUSH XH
	PUSH TEMP_EH

	LDI TEMP_EH,0x11
	SUB XH,XH
	CLR XL

_OS_DIV16__LOOP:
	ROL ACCUM_L
	ROL ACCUM_H
	DEC TEMP_EH
	BREQ _OS_DIV16__END
	ROL XL
	ROL XH
	SUB XL,TEMP_L
	SBC XH,TEMP_H
	BRCS PC+0x03
	SEC
	RJMP _OS_DIV16__LOOP
	ADD XL,TEMP_L
	ADC XH,TEMP_H
	CLC
	RJMP _OS_DIV16__LOOP
_OS_DIV16__END:
	MOVW TEMP_L,XL

	POP TEMP_EH
	POP XH
	POP XL
	RET
.ENDIF
