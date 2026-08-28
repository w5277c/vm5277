/*
 * Copyright 2026 konstantin@5277.ru
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

;VERSION 0.1 - требует адаптации на другие частоты
;Блокирующая прерывания передача 3 байтного значения GRB LED чипу WS2812B (для 16Мгц)
;Блокирует прерывания на ~30мкс

;TODO Адаптировать под разные частоты (8, 9.6 и 20)

.IFNDEF OS_DRV_WS2812B_SET_NR

.EQU _OS_DRV_WS2812B__HEAP_IO_REG					= 0x05	;2B-адрес IO регистра PORTx

;-----------------------------------------------------------
OS_DRV_WS2812B_SET_NR:
;-----------------------------------------------------------
;Передача 3 байтного значения GRB
;IN:Y-адрес HEAP, ACCUM_L/H/EL-RGB значение
;MOD: ACCUM_L/H/EL/EH
;-----------------------------------------------------------
	PUSH_Z

	LDD ZL,Y+_OS_DRV_WS2812B__HEAP_IO_REG+0x00				;Адрес IO порта
	LDI ZH,0x00
	LDD ACCUM_EH,Y+_OS_DRV_WS2812B__HEAP_IO_REG+0x01		;Пин

	CLI														;Запрет прерываний
	LD ATOM_L,Z
	MOV ATOM_H,ATOM_L
	OR ATOM_H,ACCUM_EH										;HI состояние
	COM ACCUM_EH
	AND ATOM_L,ACCUM_EH										;LO состояние

	LDI ACCUM_EH,8
OS_DRV_WS2812B_SET__BITSLOOP1:
	ST Z,ATOM_H
	LSL ACCUM_H
	NOP
	NOP
	NOP
	BRCS PC+0x02
	ST Z,ATOM_L
	NOP
	NOP
	NOP
	NOP
	BRCC PC+0x02
	ST Z,ATOM_L
	NOP
	NOP
	DEC ACCUM_EH
	BRNE OS_DRV_WS2812B_SET__BITSLOOP1

	LDI ACCUM_EH,8
OS_DRV_WS2812B_SET__BITSLOOP2:
	ST Z,ATOM_H
	LSL ACCUM_L
	NOP
	NOP
	NOP
	BRCS PC+0x02
	ST Z,ATOM_L
	NOP
	NOP
	NOP
	NOP
	BRCC PC+0x02
	ST Z,ATOM_L
	NOP
	NOP
	DEC ACCUM_EH
	BRNE OS_DRV_WS2812B_SET__BITSLOOP2

	LDI ACCUM_EH,8
OS_DRV_WS2812B_SET__BITSLOOP3:
	ST Z,ATOM_H
	LSL ACCUM_EL
	NOP
	NOP
	NOP
	BRCS PC+0x02
	ST Z,ATOM_L
	NOP
	NOP
	NOP
	NOP
	BRCC PC+0x02
	ST Z,ATOM_L
	NOP
	NOP
	DEC ACCUM_EH
	BRNE OS_DRV_WS2812B_SET__BITSLOOP3

	SEI
	POP_Z
	RET
.ENDIF
