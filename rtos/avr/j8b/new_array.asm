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
 
 .include "dmem/dram.asm"
 
.IFNDEF J8BPROC_NEW_ARRAY
;-----------------------------------------------------------
J8BPROC_NEW_ARRAY:
;-----------------------------------------------------------
;Вынесен блок кода для оптимизации
;-----------------------------------------------------------
_J8BPROC_ARR_CLEAR__LOOP:
	PUSH ZL
	PUSH ZH
	MCALL OS_DRAM_ALLOC

	MOVW XL,ZL
	ST Z+,C0x00
	SUBI ACCUM_L,0x01
	SBCI ACCUM_H,0x00
	BRNE _J8BPROC_ARR_CLEAR__LOOP
	MOVW ACCUM_L,XL
	POP ZH
	POP ZL
	RET
.ENDIF
