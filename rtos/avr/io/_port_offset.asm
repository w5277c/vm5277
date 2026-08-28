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
REG_IN:														;Не требует дополнительных действий
;-----------------------------------------------------------
;Возвращаем смещение на первый адрес регистров порта
;IN: ACCUM_L-сдвоенный порт и пин (PA0, PC7 и т.п.)
;OUT: ACCUM_L-адрес регистра PINx, ACCUM_H-пин в виде
;числа
;-----------------------------------------------------------
	PUSH_Z

	PUSH ACCUM_L
	ANDI ACCUM_L,0x0f
	LDI_Z _BIT_TO_MASK_TABLE*2
	ADD ZL,ACCUM_L
	ADC ZH,C0x00
	LPM ACCUM_H,Z

	POP ACCUM_L
	SWAP ACCUM_L
	ANDI ACCUM_L,0x0f
	LDI_Z PORTS_TABLE*2
	ADD ZL,ACCUM_L
	ADC ZH,C0x00
	LPM ACCUM_L,Z

	POP_Z
	RET
.ENDIF
