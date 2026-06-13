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

;Две процедуры выделения и освобождения динамической памяти, основаны на фиксированных битных картах 8 байтных блоков.

.include "math/divz16by8.asm"
.include "math/mulz16by8.asm"
.include "math/div16p2.asm"
.include "conv/bit_to_mask.asm"

.IFNDEF OS_DRAM_ALLOC
.IF OS_DRAM_BMASK_SIZE > 0
;-----------------------------------------------------------;
OS_DRAM_ALLOC:
;-----------------------------------------------------------;
;Выделение памяти в динамическом блоке
;IN: ACCUM_H/L-размер блока
;OUT: Z-адрес на выделенный блок, флаг C true=OutOfMemory
;-----------------------------------------------------------;
	CP ACCUM_L,C0x00										;Проверка на нулевую длину (стоит убрать?)
	CPC ACCUM_H,C0x00
	BRNE PC+0x03
	SEC
	RET

	PUSH_Y
	PUSH_A16

	MOV YL,ACCUM_L											;Размер данных делим на 8
	MCALL OS_DIV16P2_X8
	ANDI YL,0b00000111										;Пропускаем инкремент если число было кратно 8
	BREQ PC+0x03
	ADD ACCUM_L,C0x01										;Иначе прибавляем единицу реализуя деление в большую сторону
	ADC ACCUM_H,C0x00
	LDI_Y OS_DRAM_BMASK_ADDR								;Загружаем адрес битовой маски
	MCALL _OS_DRAM_FIND_FREE_CELLS							;Вызываем функцию поиска свободной ячейки в блоке
	BRCS _OS_DRAM_ALLOC__END								;Флаг C включен - не нашли, выходим с ошибкой
	MCALL OS_MULZ16BY8										;Преобразуем смещение в ячейках в смещение в байтах (умножаем на 8)
	LDI ACCUM_L,low(OS_DRAM_ADDR)							;В Z смещение в блоке (в байтах) на свободную ячейку, добавляем адрес блока
	ADD ZL,ACCUM_L
	LDI ACCUM_L,high(OS_DRAM_ADDR)
	ADC ZH,ACCUM_L
	CLC														;Успешно, выходим

_OS_DRAM_ALLOC__END:
	POP_A16
	POP_Y
	RET

;-----------------------------------------------------------;
OS_DRAM_FREE:												;
;-----------------------------------------------------------;
;Освобождение памяти в динамическом блоке
;IN: Z-адрес на выделенный блок, ACCUM_H/L-размер блока
;-----------------------------------------------------------;
	PUSH_Y
	PUSH_Z
	PUSH_A32

	SUBI ZL,low(OS_DRAM_ADDR)								;Вычисляем смещение внутри блока
	SBCI ZH,high(OS_DRAM_ADDR)
	MCALL OS_DIVZ16BY8										;Преобразуем смещение в байтах в смещение в ячейках
	MOV YL,ACCUM_L
	MCALL OS_DIV16P2_X8										;Преобразуем размер данных в байтах в размер в ячейках (деление на 8 с округлением в большую сторону)
	ANDI YL,0b00000111
	BREQ PC+0x03
	ADD ACCUM_L,C0x01
	ADC ACCUM_H,C0x00
	;Процедура освобождения памяти
	MOV ACCUM_EH,ZL											;Вычисляю смещение в байте - номер бита с котрого нужно освобождать
	ANDI ACCUM_EH,0x07
	MCALL OS_DIVZ16BY8										;Преобразуем смещение в ячейках в смещение в битах
	LDI_Y OS_DRAM_BMASK_ADDR								;Адрес битовой маски
	ADD YL,ZL												;Прибавляем смещение к адресу начала блока битовой маски
	ADC YH,ZH
	LDI ACCUM_EL,0x00										;Сбрасываем биты (освобождение)
	MCALL _OS_DRAM_FILL_CELLS_NR							;Вызываем процедуру записи бит

	POP_A32
	POP_Z
	POP_Y
	RET

;TODO - проверить работу
;-----------------------------------------------------------;
_OS_DRAM_FIND_FREE_CELLS:
;-----------------------------------------------------------;
;Поиск блока свободных ячеек
;IN: ACCUM_H/L-размер блока в ячейках,
;ACCUM_EL-размер блока(байты) бит.маски,
;Y-адрес на бит.маску
;OUT: Z-адрес на выделенный блок, флаг C true=OutOfMemory
;-----------------------------------------------------------;
	PUSH RESULT
	PUSH_T32
	PUSH_A32
	PUSH FLAGS
	PUSH_X

	LDI FLAGS,0x00											;Нулевой бит не найден
	LDI_X 0x0000											;Начальное смещение в блоке памяти

_OS_DRAM_FIND_FREE_CELLS__BYTELOOP:
	LD RESULT,Y												;Получаем байт карты
	CPI RESULT,0xff											;Пропускаем проверку бит, если все биты включены
	BREQ _OS_DRAM_FIND_FREE_CELLS__BYTELOOP_IS_HI

	LDI TEMP_EL,0x08										;Цикл из 8 бит
_OS_DRAM_FIND_FREE_CELLS__BITLOOP:
	LSL RESULT												;Сдвигаем байт карты для поиска нулевого бита(младший бит слева)
	BRCS _OS_DRAM_FIND_FREE_CELLS__BITLOOP_IS_HI
	;Нашли нулевой бит
	CPI FLAGS,0x00
	BRNE _OS_DRAM_FIND_FREE_CELLS__BITLOOP_NOT_FIRST
	;Это первый нулевой бит, запоминаем адрес памяти, инициализируем размер необходимых данных и включаем флаг
	MOVW ZL,XL												;Скопировали смещение в блоке памяти во временный регистр
	MOV J8B_ATOM,YL											;Скопировали адрес первого байта маски с нулевым битом во временные ячейки
	MOV ACCUM_EH,YH
	LDI FLAGS,0x01											;Установили флаг режима анализа свободного места
	MOV TEMP_EH,TEMP_EL										;Скопировали позицию в битах во временную ячейку
	MOVW TEMP_L,ACCUM_L										;Скопировали длину во временные регистры
_OS_DRAM_FIND_FREE_CELLS__BITLOOP_NOT_FIRST:
	;Общая логика для первого и последующего нулевых бит
	SBIW TEMP_L,0x01										;Вычитаем 1 ячейку из необходимого размера
	BREQ _OS_DRAM_FIND_FREE_CELLS__OK						;Если размер стал нулевым, то блок найден
	RJMP _OS_DRAM_FIND_FREE_CELLS__BITLOOP_IS_NEXT
_OS_DRAM_FIND_FREE_CELLS__BITLOOP_IS_HI:
	LDI FLAGS,0x00											;Обрываем цепочку проверки длины блока нулевых битов
_OS_DRAM_FIND_FREE_CELLS__BITLOOP_IS_NEXT:
	ADIW XL,0x01
	DEC TEMP_EL
	BRNE _OS_DRAM_FIND_FREE_CELLS__BITLOOP
	RJMP _OS_DRAM_FIND_FREE_CELLS__BYTELOOP_NEXT

_OS_DRAM_FIND_FREE_CELLS__BYTELOOP_IS_HI:
	ADIW XL,0x08											;Смещаемся на 8 байт, так как цикл проверки бит был пропущен
	LDI FLAGS,0x00											;Обрываем цепочку проверки длины блока нулевых битов
_OS_DRAM_FIND_FREE_CELLS__BYTELOOP_NEXT:
;	INC TEMP_H
	ADIW YL,0x01
	DEC ACCUM_EL
	BRNE _OS_DRAM_FIND_FREE_CELLS__BYTELOOP
	SEC
	RJMP _OS_DRAM_FIND_FREE_CELLS__END

_OS_DRAM_FIND_FREE_CELLS__OK:
	;Блок найден, подготавливаем данные для заполнения блока бит.маски
	MOV YL,J8B_ATOM											;Адрес байта маски с ервым свободным битом
	MOV YH,ACCUM_EH
	LDI ACCUM_EH,0x08
	SUB ACCUM_EH,TEMP_EH
	LDI ACCUM_EL,0x01										;Устанавливаем биты в 1
	;Заполняем блок бит.маски
	MCALL _OS_DRAM_FILL_CELLS_NR
	CLC
_OS_DRAM_FIND_FREE_CELLS__END:
	POP_X
	POP FLAGS
	POP_A32
	POP_T32
	POP RESULT
	RET

;-----------------------------------------------------------;
_OS_DRAM_FILL_CELLS_NR:
;-----------------------------------------------------------;
;Заполняем блок бит.маски
;IN: Y-адрес(байты) со смещением на бит.маску
;ACCUM_EH-смещение(биты) на бит.маску, ACCUM_H/L-размер
;блока в ячейках, ACCUM_EL-установка бита(1) или сброс(0)
;MOD:ACCUM_L/H
;-----------------------------------------------------------;
	PUSH ACCUM_EH
	PUSH_T16

	;Подготавливаю маску для первого байта(размер блока может быть меньше 8 бит)
	CPI ACCUM_H,0x00
	BRNE _OS_DRAM_FILL_CELLS__SIZE_GT1B
	CPI ACCUM_L,0x09
	BRCC _OS_DRAM_FILL_CELLS__SIZE_GT1B
	LDI TEMP_L,0x80											;Чаще всего размер блока будет = 1 ячейка
	CPI ACCUM_L,0x01
	BREQ _OS_DRAM_FILL_CELLS__SIZE_DONE
	;Включаю биты слева(размер=6->0b11111100)
	LDI TEMP_L,0x08
	SUB TEMP_L,ACCUM_L
	MCALL BIT_TO_MASK										;Получаем число (2^TEMP_L)
	DEC TEMP_L
	COM TEMP_L
	RJMP PC+0x02
_OS_DRAM_FILL_CELLS__SIZE_GT1B:
	;Размер блока больше 7, включаю все биты
	LDI TEMP_L,0xff
_OS_DRAM_FILL_CELLS__SIZE_DONE:

	;Вычитаю из размера количество записываемых бит в первом байте(зависит от размера маски и смещения в байте)
	PUSH RESULT
	LDI RESULT,0x08
	SUB RESULT,ACCUM_EH
	SUB ACCUM_L,RESULT
	POP RESULT
	SBCI ACCUM_H,0x00
	BRCC PC+0x03											;Если рамер маски больше размера блока, то обнуляем размер
	CLR ACCUM_L
	CLR ACCUM_H

	;Смещаю маску на смещение в битах(маска в TEMP_L)
	CPI ACCUM_EH,0x00										;Нет смещения
	BREQ _OS_DRAM_FILL_CELLS__BITOFFSET_LOOP_DONE
	CPI ACCUM_EH,0x04										;Смещение>=4, смещаю на 4 через SWAP
	BRCS _OS_DRAM_FILL_CELLS__BITOFFSET_LOOP
	SWAP TEMP_L
	ANDI TEMP_L,0x0f
	SUBI ACCUM_EH,0x04
	BREQ _OS_DRAM_FILL_CELLS__BITOFFSET_LOOP_DONE			;Сдвиг ровно на 4 бита,завершено
_OS_DRAM_FILL_CELLS__BITOFFSET_LOOP:
	LSR TEMP_L												;Сдвигаю на бит
	DEC ACCUM_EH
	BRNE _OS_DRAM_FILL_CELLS__BITOFFSET_LOOP
_OS_DRAM_FILL_CELLS__BITOFFSET_LOOP_DONE:
	MCALL _OS_DRAM_BYTE_UPDATE								;Обновляем первый байт

	;Теперь смещение в битах всегда 0
	MOV TEMP_H,ACCUM_L										;Сохраняю малдший байт для определения кол-ва бит в последнем байте
	MCALL OS_DIV16P2_X8										;Получаю количество полностью заполненных байт
	BREQ _OS_DRAM_FILL_CELLS__FILLBYTES_DONE
_OS_DRAM_FILL_CELLS__FILLBYTES_LOOP:
	MCALL _OS_DRAM_BYTE_UPDATE								;Обновляем последующий байт
	DEC ACCUM_L												;Старший байт ожидается нулевым, не проверяем
	BRNE _OS_DRAM_FILL_CELLS__FILLBYTES_LOOP
_OS_DRAM_FILL_CELLS__FILLBYTES_DONE:

	;Последний байт, если заполняется не полностью
	ANDI TEMP_H,0x07
	BREQ _OS_DRAM_FILL_CELLS__LASTBYTE_DONE					;Заполнен полностью
	LDI TEMP_L,0x08
	SUB TEMP_L,TEMP_H
	MCALL BIT_TO_MASK										;Получаем число (2^TEMP_L)
	DEC TEMP_L												;Получаю заполненные биты слева
	COM TEMP_L
	MCALL _OS_DRAM_BYTE_UPDATE								;Обновляем последний байт

_OS_DRAM_FILL_CELLS__LASTBYTE_DONE:
	POP_T16
	POP ACCUM_EH
	RET

;-----------------------------------------------------------;
_OS_DRAM_BYTE_UPDATE:
;-----------------------------------------------------------;
;Обновляет байт значением
;IN: Y-адрес, TEMP_L-битовая маска,
;ACCUM_EL-установка бита(1) или сброс(0)
;-----------------------------------------------------------;
	PUSH RESULT
	
	LD RESULT,Y
	CPI ACCUM_EL,0x00
	BRNE PC+0x04
	COM TEMP_L
	AND RESULT,TEMP_L
	RJMP PC+0x02
	OR RESULT,TEMP_L
	ST Y+,RESULT

	POP RESULT
	RET
.ELSE
;-----------------------------------------------------------;
OS_DRAM_ALLOC:
;-----------------------------------------------------------;
;Заглушка, памяти нет, возвращаем OutOfMemory
;IN: ACCUM_H/L-размер блока
;OUT: Z-адрес на выделенный блок, флаг C true=OutOfMemory
;-----------------------------------------------------------;
	SEC
;-----------------------------------------------------------;
OS_DRAM_FREE:												;
;-----------------------------------------------------------;
;Заглушка, памяти нет
;IN: Z-адрес на выделенный блок, ACCUM_H/L-размер блока
;-----------------------------------------------------------;
	RET
.ENDIF
.ENDIF
