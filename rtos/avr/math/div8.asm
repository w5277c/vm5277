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
.IFNDEF OS_DIV8
;-----------------------------------------------------------
OS_DIV8:
;-----------------------------------------------------------
;Деление 8b числа на 8бит число
;IN: ACCUM_L-8b делимое, ACCUM_H-8b делитель
;OUT: ACCUM_L-8b результат, TEMP_L-8b остаток
;-----------------------------------------------------------
	PUSH TEMP_H

	LDI TEMP_H,0x09        ; 8 бит + 1
	SUB TEMP_L,TEMP_L

_OS_DIV8__LOOP:
	ROL ACCUM_L
	DEC TEMP_H
	BREQ _OS_DIV8__END
	ROL TEMP_L
	SUB TEMP_L,ACCUM_H
	BRCS PC+0x03
	SEC
	RJMP _OS_DIV8__LOOP
	ADD TEMP_L,ACCUM_H
	CLC
	RJMP _OS_DIV8__LOOP
_OS_DIV8__END:

	POP TEMP_H
	RET
.ENDIF
