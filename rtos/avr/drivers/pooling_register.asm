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

.IFNDEF OS_POOLING_REGISTER
;-----------------------------------------------------------
OS_POOLING_REGISTER:
/*;-----------------------------------------------------------
;Проверяем поддерживается PCINT для этотго пина
;IN: ACCUM_L-пин
;OUT: ACCUM_L-результат(0-not valid, 1-valid)
;MOD:ACCUM_H/EL
;-----------------------------------------------------------
	ANDI ACCUM_L,0xf0
	SWAP ACCUM_L
	CPI ACCUM_L,OS_PORTS_LAST+0x01
	BRCC OS_PCINT_SUPPORTED__NOT

	PUSH_Z
	LDI_Z PCINT_TABLE*2
	ADD ZL,ACCUM_L
	ADC ZH,C0x00
	LPM ACCUM_L,Z
	POP_Z
	CPI ACCUM_L,0x00
	LDI ACCUM_L,0x01
	BRNE OS_PCINT_SUPPORTED__END
OS_PCINT_SUPPORTED__NOT:
	LDI ACCUM_L,0x00
OS_PCINT_SUPPORTED__END:*/
	LDI ACCUM_L,0x00
	RET
.ENDIF
