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

//TODO при работе с массивами большая часть кода дублируется, нужно вынести в отдельную функцию

.include "dmem/dram.asm"
.include "math/mul16.asm"

.IFNDEF J8BPROC_ARRVIEW_MAKE
;-----------------------------------------------------------
J8BPROC_ARRVIEW_MAKE:
;-----------------------------------------------------------
;Создаем заголовок для VIEW
;IN: X-адрес заголовка массива, ACCUM_EH-глубина VIEW
;OUT: X-адрес на заголовок VIEW
;-----------------------------------------------------------
	PUSH YL
	PUSH YH
	LDS YL,SPL
	LDS YH,SPH

	PUSH ZL
	PUSH ZH
	PUSH ACCUM_L
	PUSH ACCUM_H
	PUSH ACCUM_EL
	PUSH ACCUM_EH
	PUSH TEMP_L
	PUSH FLAGS
	PUSH XL
	PUSH XH

	LD TEMP_L,X												;Глубина и флаги массива
	MOV FLAGS,TEMP_L										;Для определения размера ячейки
	ANDI TEMP_L,0x03
	PUSH TEMP_L
	LDI ACCUM_H,0x00
	LDI ACCUM_L,0x05
	MOVW ZL,XL
	MCALL OS_DRAM_ALLOC

	ST Z+,ACCUM_EH											;Глубина и флаг VIEW
	ST Z+,XL												;Адрес на массив
	ST Z+,XH

	;Вычисляем адрес ячейки с учетом глубин, их размерности и индексов
	ADIW YL,0x02+0x02										;индекс 0(из 0-2, 0x0c-количество PUSH)
	ADIW XL,0x05											;размерность 1(из 0-2)

	ANDI ACCUM_EH,0x03
	SUB TEMP_L,ACCUM_EH										;Кол-во отрезаемых уровней

	LD ACCUM_L,Y+											;index0
	LD ACCUM_H,Y+
	LD ACCUM_EL,X+											;dim1
	LD ACCUM_EH,X+
	MCALL OS_MUL16
	LD ACCUM_EL,X+											;dim2
	LD ACCUM_EH,X+
	MCALL OS_MUL16

	CPI TEMP_L,0xFF											;0xff-срезаем 1 уровень
	BREQ _J8BPROC_ARRVIEW_MAKE__2DSKIP
	SUBI XL,0x02
	PUSH ACCUM_L											;Сохраняем смещение
	PUSH ACCUM_H
	LD ACCUM_L,Y+											;index1
	LD ACCUM_H,Y+
	LD ACCUM_EL,X+											;dim2
	LD ACCUM_EH,X+
	MCALL OS_MUL16
	POP ACCUM_EL											;Складываем результаты
	ADD ACCUM_L,ACCUM_EL
	POP ACCUM_EL
	ADC ACCUM_H,ACCUM_EL
_J8BPROC_ARRVIEW_MAKE__2DSKIP:

	SWAP FLAGS												;Умножаю на размер ячейки(если не byte)
	ANDI FLAGS,0x03
	BREQ _J8BPROC_ARRVIEW_MAKE__COMPTOTAL_END
_J8BPROC_ARRVIEW_MAKE__COMPTOTAL_LOOP:
	LSL ACCUM_L
	ROL ACCUM_H
	DEC FLAGS
	BRNE _J8BPROC_ARRVIEW_MAKE__COMPTOTAL_LOOP
_J8BPROC_ARRVIEW_MAKE__COMPTOTAL_END:

	POP TEMP_L												;Учитываем динамический размер заголовка(dim0:5байт,dim1:7байт,dim2:9байт)
	LSL TEMP_L
	SUBI TEMP_L,low(0x100-0x03)								;Добавляю размер заголовка
	ADD ACCUM_L,TEMP_L
	ADC ACCUM_H,C0x00

	POP XL
	ADD ACCUM_L,XL
	POP XH
	ADC ACCUM_H,XH
	ST Z+,ACCUM_L
	ST Z,ACCUM_H

	MOVW XL,ZL
	POP TEMP_L
	POP FLAGS
	POP ACCUM_EH
	POP ACCUM_EL
	POP ACCUM_H
	POP ACCUM_L
	POP ZH
	POP ZL
	POP YH
	POP YL
	RET
.ENDIF
