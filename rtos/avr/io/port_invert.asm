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
.IFNDEF PORT_INVERT
.include "io/_port_offset.inc"
.include "io/reg_bit_inv.inc"
;--------------------------------------------------------
PORT_INVERT:
;--------------------------------------------------------
;Инвертирование состояния порта
;IN: ACCUM_L адрес регистра PORTx
;OUT: flag C-состояние бита(true = HI)
;--------------------------------------------------------;
	CPI ACCUM_L,0xff
	BRNE PC+0x02
	RET

	PUSH_Z
	PUSH ACCUM_L

	MCALL _PORT_OFFSET
	MCALL REG_BIT_INV

	POP ACCUM_L
	POP_Z
	RET
.ENDIF
