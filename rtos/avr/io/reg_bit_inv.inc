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
.IFNDEF REG_BIT_INV
;--------------------------------------------------------
REG_BIT_INV:
;--------------------------------------------------------
;Инвертируем бит регистра и возвращаем его новое значение
;IN: Z-адрес,ACCUM_L-число бита
;OUT: flag C-состояние бита(true = HI)
;--------------------------------------------------------
	PUSH_T16

	LDS TEMP_H,SREG
	CLI
	LD TEMP_L,Z
	EOR TEMP_L,ACCUM_L
	ST Z,TEMP_L
	AND TEMP_L,ACCUM_L
	STS SREG,TEMP_H
	ADD TEMP_L,C0xff

	POP_T16
	RET
.ENDIF
