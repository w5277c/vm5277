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

.IFNDEF _J8BPROC_ARR_OFFSET
.IF OS_FT_ARRAY_INDEX_NOCHECK==0
	.include "j8b/arr_size.asm"
	.include "j8b/etrace_add.asm"
.ENDIF
;-----------------------------------------------------------
_J8BPROC_ARR_OFFSET:
;-----------------------------------------------------------
;Проверяем границы одномерного массива и вычисляем
;адрес ячейки
;IN: X-адрес заголовка массива, ACCUM_L/H-смещение
;ACCUM_EL/EH-не нулевая длина
;OUT: X-адрес ячейки или BoundsException
;MOD: ACCUM_L/H
;-----------------------------------------------------------
.IF OS_FT_ARRAY_INDEX_NOCHECK==0
	CP XL,C0x00
	CPC XH,C0x00
	BREQ _J8BPROC_ARR_OFFSET__NULL
.ENDIF
	ADD XL,ACCUM_L
	ADC XH,ACCUM_H
.IF OS_FT_ARRAY_INDEX_NOCHECK==0
	ADD ACCUM_L,ACCUM_EL
	ADC ACCUM_H,ACCUM_EH									; ;) *Z
	BRCS _J8BPROC_ARR_OFFSET__OVERFLOW
	CP ACCUM_L,C0x00
	CPC ACCUM_H,C0x00
	BREQ _J8BPROC_ARR_OFFSET__OVERFLOW
	PUSH ACCUM_H
	PUSH ACCUM_L
	MCALL J8BPROC_ARR_SIZE
	POP J8B_ATOM
	CP ACCUM_L,J8B_ATOM
	POP J8B_ATOM
	CPC ACCUM_H,J8B_ATOM
	BRCS _J8BPROC_ARR_OFFSET__OVERFLOW
.ENDIF
	ADIW XL,OS_J8B_1D_BYTE_ARRAY_HEADER_SIZE
	RET

_J8BPROC_ARR_OFFSET__OVERFLOW:
.IF OS_FT_ARRAY_INDEX_NOCHECK==0
	LDI ACCUM_L,OS_BOUNDS_EXCEPTION_ID
	LDI ACCUM_H,0x00
	MJMP _J8BPROC_ETRACE_ADDFIRST__SET
_J8BPROC_ARR_OFFSET__NULL:
	LDI ACCUM_L,OS_NULL_POINTER_EXCEPTION_ID
	LDI ACCUM_H,0x00
	MJMP _J8BPROC_ETRACE_ADDFIRST__SET
.ENDIF
.ENDIF