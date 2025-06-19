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
.IFNDEF BIT_TO_MASK
.include "conv/_bit_to_mask_table.asm"
;-----------------------------------------------------------
BIT_TO_MASK:
;-----------------------------------------------------------
;Номер бита в число(2^n)
;IN: TEMP-номер бита(0-7)
;OUT: TEMP-число(1,2,4,8,16,32,64,128)
;-----------------------------------------------------------
	PUSH_Z													;4
	LDI_Z _BIT_TO_MASK_TABLE*2								;2
	ADD ZL,TEMP												;1
	ADC ZH,C0x00											;1
	LPM TEMP,Z												;3
	POP_Z													;4,total:15t
	RET
.ENDIF

