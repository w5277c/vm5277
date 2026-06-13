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

.IFNDEF OS_RAM_COPY16
;-----------------------------------------------------------
OS_RAM_COPY16:
;-----------------------------------------------------------
;Заполнение блока памяти SRC->DST
;IN: Y-SRC адрес, Z-DST адрес ACCUM_L/H-длина
;MOD: YL/H,ZL/H,ACCUM_L/H/EL
;-----------------------------------------------------------
	CP ACCUM_L,C0x00
	CPC ACCUM_H,C0x00
	BREQ _OS_RAM_COPY16__RET
_OS_RAM_COPY16__LOOP:
	LD ACCUM_EL,Y+
	ST Z+,ACCUM_EL
	SUBI ACCUM_L,0x01
	SBCI ACCUM_H,0x00
	BRNE _OS_RAM_COPY16__LOOP
_OS_RAM_COPY16__RET:
	RET
.ENDIF
