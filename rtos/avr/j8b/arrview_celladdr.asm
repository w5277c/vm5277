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

;Функционал работы с VIEW (также применим для массивов)
;VIEW содержит флаг VIEW, адрес массива и 1-2 индекса, которые нужно добавить к индексам массива
;0x00 1B FLAGS (7-is view, 6-extended(7bytes), 5-4-cell type size(0=1byte,...), 2-0-depth(0-1)
;0x01 2B ARRAY HEADER ADDR
;0x03 2B INDEX0
;0x05 0/2B INDEX1

;Необходимо загрузить в X адрес массива и обновить интексы в стек
;Затем вызвать J8BPROC_ARR_CELLADDR

.IF OS_FT_ARRVIEW == 0x01
.include "j8b/arr_celladdr.asm"

.IFNDEF J8BPROC_ARRVIEW_CELLADDR
;-----------------------------------------------------------
J8BPROC_ARRVIEW_CELLADDR:									;Портит аккумулятор 32бита
;-----------------------------------------------------------
;Вычисляет адрес ячейки массива
;IN: X-адрес заголовка VIEW, индексы расположены в стеке
;MOD: ACCUM_EH/EL/H/L,XL/H
;OUT: см. J8BPROC_ARR_CELLADDR
;-----------------------------------------------------------
	POP ZL													;Сохраняем точку возврата
	POP ZH

	LD ACCUM_L,X+
	SBRS ACCUM_L,0x07										;Проверка на флаг VIEW
	RJMP _J8BPROC_ARRVIEW_CELLADDR__END
	LD ACCUM_EL,X+											;Загружаем адрес массива
	LD ACCUM_EH,X+

	 ;TODO возможно сначала должен идти index1 - надо проверить
	LD ACCUM_H,X+											;index0 L
	PUSH ACCUM_H
	LD ACCUM_H,X+											;index0 H
	PUSH ACCUM_H
	
	LD ACCUM_H,X+											;index1 L
	SBRC ACCUM_L,0x00
    PUSH ACCUM_H
	LD ACCUM_H,X											;index1 H
	SBRC ACCUM_L,0x00
    PUSH ACCUM_H
    MOVW XL,ACCUM_EL										;Записываем адрес массива в X
_J8BPROC_ARRVIEW_CELLADDR__END:
	PUSH ZH													;Востанавливаем точку возврата
	PUSH ZL
	JMP J8BPROC_ARR_CELLADDR								;Переходим на вычисление адреса ячейки в массиве
.ENDIF
.ENDIF