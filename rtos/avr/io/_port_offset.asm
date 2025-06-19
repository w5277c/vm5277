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
.include "conv/_bit_to_mask_table.asm"
.IFNDEF _PORT_OFFSET
;-----------------------------------------------------------
_PORT_OFFSET:
;-----------------------------------------------------------
;Возвращаем смещение на первый адрес регистров порта
;IN: ACCUM_L-сдвоенный порт и пин (PA0, PC7 и т.п.)
;OUT: Z-адрес регистра PINx, ACCUM_L-пин в виде числа
;-----------------------------------------------------------
	PUSH TEMP_L

	MOV TEMP_L,ACCUM_L
	ANDI ACCUM_L,0x0f

	LDI_Z _BIT_TO_MASK_TABLE*2
	ADD ZL,ACCUM_L
	ADC ZH,C0x00
	LPM ACCUM_L,Z

	SWAP TEMP_L
	ANDI TEMP_L,0x0f

	LDI_Z PORTS_TABLE*2
	ADD ZL,TEMP_L
	ADC ZH,C0x00
	LPM ZL,Z
	LDI ZH,0x00

	POP TEMP_L
	RET
.ENDIF
