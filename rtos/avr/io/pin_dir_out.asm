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
.IFNDEF PIN_MODE_OUT
.include "io/_port_offset.asm"
.include "io/reg_bit_hi.asm"
;-----------------------------------------------------------
PIN_DIR_OUT:
;-----------------------------------------------------------
;Устанвливаем пин на выход
;IN: ACCUM_L-пин
;-----------------------------------------------------------
	CPI ACCUM_L,0xff
	BREQ _PIN_DIR_OUT__END
	PUSH_A16
	MCALL _PORT_OFFSET
	ADD ACCUM_L,C0x01
	MCALL REG_BIT_HI_NR
	POP_A16
_PIN_DIR_OUT__END:
	RET
.ENDIF
