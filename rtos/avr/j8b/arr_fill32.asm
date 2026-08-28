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

.IFNDEF J8BPROC_ARR_FILL32
.include "j8b/_arr_offset.asm"
.include "mem/ram_fill16.asm"

;-----------------------------------------------------------
J8BPROC_ARR_FILL32:
;-----------------------------------------------------------
;Заполнение части одномерного 32b массива указанным
;значением
;IN: X-адрес заголовка массива, ACCUM_L/H-смещение
;TEMP_L/H-длина, ACCUM_EL-значение
;OUT: BoundsException
;MOD: X,ACCUM_L/H, TEMP_L/H
;-----------------------------------------------------------
	CP TEMP_L,C0x00
	CPC TEMP_H,C0x00
	BREQ J8BPROC_ARR_FILL32__END

	MCALL _J8BPROC_ARR_OFFSET								;Проверяем границы и вычисляем адрес ячейки для массива
.IF OS_FT_ARRAY_INDEX_NOCHECK==0
	BRTS J8BPROC_ARR_FILL32__END
.ENDIF
	MOV ACCUM_L,ACCUM_EL
	LSL TEMP_L												;Кол-во ячеек в байты (x4)
	ROL TEMP_H
.IF OS_FT_ARRAY_INDEX_NOCHECK==0
	BRCS J8BPROC_ARR_FILL32__OVERFLOW						;Дополнительная проверка на переполнение
.ENDIF
	LSL TEMP_L
	ROL TEMP_H
.IF OS_FT_ARRAY_INDEX_NOCHECK==0
	BRCS J8BPROC_ARR_FILL32__OVERFLOW
.ENDIF
	MJMP OS_RAM_FILL16_NR
J8BPROC_ARR_FILL32__OVERFLOW:
	MJMP _J8BPROC_ARR_OFFSET__OVERFLOW
.ENDIF
J8BPROC_ARR_FILL32__END:
	RET
.ENDIF