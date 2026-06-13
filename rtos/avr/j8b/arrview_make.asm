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

;TODO при работе с массивами большая часть кода дублируется, нужно вынести в отдельную функцию
;TODO добавить проверку на OUT_OF_MEMORY (C=true) после OS_DRAM_ALLOC, возвращать флаг C

; Структура данных
; 1B flags&depth
; 2B arr addr
; 2B index 0
; 0/2B index 1


.include "dmem/dram.asm"

.IFNDEF J8BPROC_ARRVIEW_MAKE
;-----------------------------------------------------------
J8BPROC_ARRVIEW_MAKE:
;-----------------------------------------------------------
;Создаем заголовок для VIEW
;IN: X-адрес заголовка массива,
;ACCUM_L/H-индекс 0, ACCUM_EL/EH-индекс 1
;TEMP_L-флаги view(7-is view, 0-глубина)
;OUT: ACCUM_H/L-адрес на заголовок VIEW
;-----------------------------------------------------------
	PUSH_Z

	PUSH_A16
	LDI ACCUM_L,0x05
	SBRC TEMP_L,0x00
	SUBI ACCUM_L,low(-2)
	LDI ACCUM_H,0x00
	MCALL OS_DRAM_ALLOC
	POP_A16
	BRCS _J8BPROC_ARRVIEW_MAKE__OVERFLOW
	STD Z+0x00,TEMP_L
	STD Z+0x01,XL
	STD Z+0x02,XH
	STD Z+0x03,ACCUM_L										;LE
	STD Z+0x04,ACCUM_H
	SBRC TEMP_L,0x00
	STD Z+0x05,ACCUM_EL
	SBRC TEMP_L,0x00
	STD Z+0x06,ACCUM_EH

	MOVW ACCUM_L,ZL

_J8BPROC_ARRVIEW_MAKE__OVERFLOW:
	POP_Z
	RET
.ENDIF
