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

.IFNDEF OS_RAM_PUSHBLK16
;-----------------------------------------------------------
OS_RAM_PUSHBLK16:
;-----------------------------------------------------------
;Запись блока в стек
;IN: X-SRC адрес ACCUM_L/H-длина
;MOD: XL/H,ZL/H,ACCUM_L/H/EL
;-----------------------------------------------------------
	POP ZH
	POP ZL

	CP ACCUM_L,C0x00
	CPC ACCUM_H,C0x00
	BREQ OS_RAM_PUSHBLK16__RET
OS_RAM_PUSHBLK16__LOOP:
	LD ACCUM_EL,X+
	PUSH ACCUM_EL
	SUBI ACCUM_L,0x01
	SBCI ACCUM_H,0x00
	BRNE OS_RAM_PUSHBLK16__LOOP
OS_RAM_PUSHBLK16__RET:
	IJMP
.ENDIF
