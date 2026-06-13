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

	ADIW XL,0x02											;Дополняем индексы массиву LE
	LD ACCUM_H,X											;H - перевернутый LE из-за стека
	PUSH ACCUM_H
	SBIW XL,0x01											;L - перевернутый LE из-за стека
	LD ACCUM_H,X+
	PUSH ACCUM_H
	ADIW XL,0x03											;H - перевернутый LE из-за стека
	SBRC ACCUM_L,0x00
    PUSH ACCUM_H
	SBIW XL,0x01											;L - перевернутый LE из-за стека
	LD ACCUM_H,X
	SBRC ACCUM_L,0x00
    PUSH ACCUM_H
    MOVW XL,ACCUM_EL										;Записываем адрес массива в X
_J8BPROC_ARRVIEW_CELLADDR__END:
	PUSH ZH													;Востанавливаем точку возврата
	PUSH ZL
	JMP J8BPROC_ARR_CELLADDR								;Переходим на вычисление адреса ячейки в массиве
.ENDIF
.ENDIF