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

.IFNDEF PIN_IS_VALID
.IF OS_FT_GPIO_PIN_NOCHECK==0
	.include "io/_port_offset.asm"
.ENDIF
;-----------------------------------------------------------
PIN_IS_VALID:
;-----------------------------------------------------------
;Проверяем поддерживает ли чип этот пин
;IN: ACCUM_L-пин
;OUT: ACCUM_L-результат(0-not valid, 1-valid)
;MOD:ACCUM_H/EL
;-----------------------------------------------------------
.IF OS_FT_GPIO_PIN_NOCHECK==0
	MOV ACCUM_H,ACCUM_L
	ANDI ACCUM_H,0xf0
	SWAP ACCUM_H
	CPI ACCUM_H,OS_PORTS_LAST+0x01
	BRCC _PIN_IS_VALID__NOT

	PUSH_Z
	LDI_Z PORTS_MASK*2
	ADD ZL,ACCUM_H
	ADC ZH,C0x00
	LPM ACCUM_EL,Z
	POP_Z
	MCALL _PORT_OFFSET
	AND ACCUM_L,ACCUM_EL
	LDI ACCUM_L,0x01
	BRNE _PIN_IS_VALID__END
_PIN_IS_VALID__NOT:
	LDI ACCUM_L,0x00
_PIN_IS_VALID__END:
	RET
.ELSE
	LDI ACCUM_L,0x01
	RET
.ENDIF
.ENDIF
