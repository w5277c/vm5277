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
.IFNDEF OS_MUL32_NR
;-----------------------------------------------------------
OS_MUL32_NR:												;NR-NO_RESTORE - не восстанавливаю регистры TEMP_*
;-----------------------------------------------------------
;Умножение 32бит числа на 32бит число
;IN: ACCUM_L/H/EL/EH-32b число, TEMP_L/H/EL/EH-32b число
;OUT: ACCUM_L/H/EL/EH-32b результат
;-----------------------------------------------------------
	PUSH XL
	PUSH XH
	PUSH YL
	PUSH YH

	MOVW XL,ACCUM_L
	MOVW YL,ACCUM_EL

	CLR ACCUM_L
	CLR ACCUM_H
	CLR ACCUM_EL
	CLR ACCUM_EH

	LDI FLAGS,0x20
_OS_MUL32_NR__LOOP:
	LSR TEMP_EH
	ROR TEMP_EL
	ROR TEMP_H
	ROR TEMP_L
	BRCC _OS_MUL32_NR__NO_ADD
	ADD ACCUM_L,XL
	ADC ACCUM_H,XH
	ADC ACCUM_EL,YL
	ADC ACCUM_EH,YH
_OS_MUL32_NR__NO_ADD:
	LSL YH
	ROL YL
	ROL XH
	ROL XL

	DEC FLAGS
	BRNE _OS_MUL32_NR__LOOP

	POP YH
	POP YL
	POP XH
	POP XL
	RET
.ENDIF
