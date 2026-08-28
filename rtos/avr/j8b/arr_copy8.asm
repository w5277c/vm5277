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

.IFNDEF J8BPROC_ARR_COPY8
.include "j8b/_arr_offset.asm"
.include "mem/ram_copy16.asm"

;-----------------------------------------------------------
J8BPROC_ARR_COPY8:
;-----------------------------------------------------------
;Проверка и копирвание содержимого одномерного 8b массива
;в другой одномерный 8b массив
;IN: X-адрес заголовка SRC массива, ACCUM_L/H-SRC смещение
;Y-адрес заголовка DST массива, TEMP_L/H-DST смещение
;ACCUM_EL/EH-размер копируемых данных
;OUT: BoundsException
;MOD: Y,X,ACCUM_L/H,TEMP_L/H
;-----------------------------------------------------------
	CP ACCUM_EL,C0x00
	CPC ACCUM_EH,C0x00
	BREQ J8BPROC_ARR_COPY8__END

	MCALL _J8BPROC_ARR_OFFSET								;Проверяем границы и вычисляем адрес ячейки для 1-го массива
.IF OS_FT_ARRAY_INDEX_NOCHECK==0
	BRTS J8BPROC_ARR_COPY8__END
.ENDIF

	PUSH_X													;Проверяем границы и вычисляем адрес ячейки для 2-го массива
	MOVW XL,YL
	MOVW ACCUM_L,TEMP_L
	MCALL _J8BPROC_ARR_OFFSET
	MOVW YL,XL
	POP_X
	BRTS J8BPROC_ARR_COPY8__END

	MOVW TEMP_L,ACCUM_EL									;Копируем данные
	MJMP OS_RAM_COPY16_NR

J8BPROC_ARR_COPY8__END:
	RET
.ENDIF
