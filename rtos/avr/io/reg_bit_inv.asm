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
.IFNDEF REG_BIT_INV_NR
;-----------------------------------------------------------
REG_BIT_INV_NR:
;-----------------------------------------------------------
;Инвертируем бит регистра и возвращаем его новое значение
;IN: ACCUM_L-адрес,ACCUM_H-число бита
;OUT: flag C-состояние бита(true = HI)
;MOD: XH,ACCUM_L/EL
;-----------------------------------------------------------
	MOV XL,ACCUM_L
	LDI XH,0x00
	LDS ACCUM_EL,SREG
	CLI
	LD ACCUM_L,X
	EOR ACCUM_L,ACCUM_H
	ST X,ACCUM_L
	AND ACCUM_L,ACCUM_H
	STS SREG,ACCUM_EL
	ADD ACCUM_L,C0xff
	RET
.ENDIF
