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

.IFNDEF OS_OUT_BYTES
.include "stdio/out_hex8.asm"
;-----------------------------------------------------------
OS_OUT_BYTES:
;-----------------------------------------------------------
;Вывод блока данных в шестнадцатеричной форме
;IN: X-адрес, ACCUM_L/H-длина
;-----------------------------------------------------------
	PUSH_X
	PUSH_A16

	CP ACCUM_L,C0x00
	CPC ACCUM_H,C0x00
	BREQ _OS_OUT_BYTES__END
_OS_OUT_BYTES__LOOP:
	PUSH ACCUM_L
	LD ACCUM_L,X+
	MCALL OS_OUT_HEX8
	POP ACCUM_L
	SUBI ACCUM_L,0x01
	SBCI ACCUM_H,0x00
	BRNE _OS_OUT_BYTES__LOOP

_OS_OUT_BYTES__END:
	POP_A16
	POP_X
	RET
.ENDIF
