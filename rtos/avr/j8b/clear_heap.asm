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

.IFNDEF J8BPROC_CLEAR_HEAP_NR
;-----------------------------------------------------------
J8BPROC_CLEAR_HEAP_NR:										;NR-NO_RESTORE - не восстанавливает ACCUM_L/H
;-----------------------------------------------------------
;Заполнение нулями HEAP(часть переменных)
;IN: Z-адрес начала HEAP,ACCUM_L/H-размер HEAP
;-----------------------------------------------------------
	PUSH_Z
	ADIW ZL,0x05
	SUBI ACCUM_L,0x05
	SBCI ACCUM_H,0x00
_J8BPROC_CLEAR_HEAP__LOOP:
	ST Z+,C0x00
	SUBI ACCUM_L,0x01
	BRNE _J8BPROC_CLEAR_HEAP__LOOP
	SBCI ACCUM_H,0x00
	BRNE _J8BPROC_CLEAR_HEAP__LOOP
	POP_Z
	RET
.ENDIF

