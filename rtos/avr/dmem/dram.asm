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

;Две процедуры выделения и освобождения динамической памяти,
;основаны на фиксированных битных картах трех типов ячеек:
;1/2/8 байт. Блоки заданы в OS, параметры которых заданы в
;заголовочных файлах MCU.
;Функция выделения сначала определяет оптимальный блок,
;исходя из размера запрошенного блока. Но если блок занят,
;используется свободный.
;Функция освобождения определяет ячейки по адресу выделенной
;памяти.
;Обоим функциям передается размер блока в байтах

;Черновой набросок, выполнена базовая проверка
;TODO добавить директивы исключения кода блоков, если блок нулевой длины

.include "math/divz8.asm"
.include "math/mulz8.asm"
.include "math/divacc168.asm"
.include "conv/bit_to_mask.asm"

.IFNDEF OS_DRAM
;-----------------------------------------------------------;
OS_DRAM_ALLOC:
;-----------------------------------------------------------;
;Выделение памяти в динамическом блоке OS_DRAM_1B
;IN: ACCUM_H,ACCUM_L-размер блока
;OUT: Z-адрес на выделенный блок, RESULT-OSR коды
;-----------------------------------------------------------;
	MOV ZL,ACCUM_H
	OR ZL,ACCUM_L
	BRNE PC+0x03
	LDI RESULT,OSR_ALLOC_FAIL
	RET

	PUSH_Y
	PUSH ACCUM_L
	PUSH ACCUM_H
	PUSH ACCUM_EL
	PUSH TEMP_L
	PUSH TEMP_H
	PUSH FLAGS

	LDI FLAGS,0x01
	CPI ACCUM_H,0x00
	BRNE PC+0x03
	CPI ACCUM_L,0x05
	BRCS PC+0x01+_MCALL_SIZE+0x02
	MCALL _OS_DRAM_ALLOC_8B
	LDI FLAGS,0x08
	RJMP _OS_DRAM_ALLOC__CALLEND
	CPI ACCUM_L,0x01
	BREQ PC+0x01+_MCALL_SIZE+0x02
	MCALL _OS_DRAM_ALLOC_2B
	LDI FLAGS,0x02
	RJMP _OS_DRAM_ALLOC__CALLEND
	MCALL _OS_DRAM_ALLOC_1B
_OS_DRAM_ALLOC__CALLEND:
	CPI RESULT,OSR_OK
	BREQ _OS_DRAM_ALLOC__END

	SBRS FLAGS,0x00
	MCALL _OS_DRAM_ALLOC_1B
	CPI RESULT,OSR_OK
	BREQ _OS_DRAM_ALLOC__END
_OS_DRAM_ALLOC__CHECK1:
	SBRS FLAGS,0x01
	MCALL _OS_DRAM_ALLOC_2B
	CPI RESULT,OSR_OK
	BREQ _OS_DRAM_ALLOC__END
_OS_DRAM_ALLOC__CHECK2:
	SBRS FLAGS,0x03
	MCALL _OS_DRAM_ALLOC_8B

_OS_DRAM_ALLOC__END:
	POP FLAGS
	POP TEMP_H
	POP TEMP_L
	POP ACCUM_EL
	POP ACCUM_H
	POP ACCUM_L
	POP_Y
	RET


;-----------------------------------------------------------;
_OS_DRAM_ALLOC_1B:
;-----------------------------------------------------------;
;Выделение памяти в 1 байт динамическом блоке
;IN: ACCUM_H,ACCUM_L-размер блока
;OUT: Z-адрес на выделенный блок, RESULT-OSR коды
;-----------------------------------------------------------;
	LDI ACCUM_EL,OS_DRAM_BMASK_1B_SIZE
	CPI ACCUM_EL,0x00
	BRNE PC+0x03
	LDI RESULT,OSR_ALLOC_FAIL
	RET
	LDI_Y OS_DRAM_BMASK_1B
	MCALL _OS_DRAM_FIND_FREE_CELLS
	CPI RESULT,OSR_OK
	BREQ PC+0x02
	RET
	LDI TEMP_L,low(OS_DRAM_1B)
	ADD ZL,TEMP_L
	LDI TEMP_L,high(OS_DRAM_1B)
	ADC ZH,TEMP_L
_OS_DRAM_ALLOC_1B__END:
	RET
;-----------------------------------------------------------;
_OS_DRAM_ALLOC_2B:
;-----------------------------------------------------------;
;Выделение памяти в 2 байта динамическом блоке
;IN: ACCUM_H,ACCUM_L-размер блока
;OUT: Z-адрес на выделенный блок, RESULT-OSR коды
;-----------------------------------------------------------;
	LDI ACCUM_EL,OS_DRAM_BMASK_2B_SIZE
	CPI ACCUM_EL,0x00
	BRNE PC+0x03
	LDI RESULT,OSR_ALLOC_FAIL
	RET
	LSR ACCUM_H
	ROR ACCUM_L
	ADC ACCUM_L,C0x00
	ADC ACCUM_H,C0x00
	LDI_Y OS_DRAM_BMASK_2B
	MCALL _OS_DRAM_FIND_FREE_CELLS
	CPI RESULT,OSR_OK
	BREQ PC+0x02
	RET
	LSL ZL
	ROL ZH
	LDI TEMP_L,low(OS_DRAM_2B)
	ADD ZL,TEMP_L
	LDI TEMP_L,high(OS_DRAM_2B)
	ADC ZH,TEMP_L
	RET
;-----------------------------------------------------------;
_OS_DRAM_ALLOC_8B:
;-----------------------------------------------------------;
;Выделение памяти в 8 байт динамическом блоке
;IN: ACCUM_H,ACCUM_L-размер блока
;OUT: Z-адрес на выделенный блок, RESULT-OSR коды
;-----------------------------------------------------------;
	LDI ACCUM_EL,OS_DRAM_BMASK_8B_SIZE
	CPI ACCUM_EL,0x00
	BRNE PC+0x03
	LDI RESULT,OSR_ALLOC_FAIL
	RET
	MOV YL,ACCUM_L
	MCALL OS_ACC168
	ANDI YL,0b00000111
	BREQ PC+0x03
	ADD ACCUM_L,C0x01
	ADC ACCUM_H,C0x00
	LDI_Y OS_DRAM_BMASK_8B
	MCALL _OS_DRAM_FIND_FREE_CELLS
	CPI RESULT,OSR_OK
	BREQ PC+0x02
	RET
	MCALL OS_MULZ8
	LDI TEMP_L,low(OS_DRAM_8B)
	ADD ZL,TEMP_L
	LDI TEMP_L,high(OS_DRAM_8B)
	ADC ZH,TEMP_L
	RET

;-----------------------------------------------------------;
OS_DRAM_FREE:												;TODO
;-----------------------------------------------------------;
;Освобождение памяти в динамическом блоке OS_DRAM_xB
;IN: Z-адрес на выделенный блок, ACCUM_H/L-размер блока
;-----------------------------------------------------------;
	PUSH_Y
	PUSH_Z
	PUSH ACCUM_L
	PUSH ACCUM_H
	PUSH ACCUM_EL
	PUSH ACCUM_EH
	PUSH TEMP_L
	PUSH TEMP_H

	CPI ZH,high(OS_DRAM_2B)
	BRCS OS_DRAM_FREE__1B
	BRNE OS_DRAM_FREE__CHECK2B
	CPI ZL,low(OS_DRAM_2B)
	BRCC OS_DRAM_FREE__CHECK2B
OS_DRAM_FREE__1B:
	LDI_Y OS_DRAM_BMASK_1B
	SUBI ZL,low(OS_DRAM_1B)
	SBCI ZH,high(OS_DRAM_1B)
	RJMP _OS_DRAM_FREE_CELLS
OS_DRAM_FREE__CHECK2B:
	CPI ZH,high(OS_DRAM_8B)
	BRCS OS_DRAM_FREE__2B
	BRNE OS_DRAM_FREE__8B
	CPI ZL,low(OS_DRAM_8B)
	BRCC OS_DRAM_FREE__8B
OS_DRAM_FREE__2B:
	LDI_Y OS_DRAM_BMASK_2B
	SUBI ZL,low(OS_DRAM_2B)
	SBCI ZH,high(OS_DRAM_2B)
	LSR ZH
	ROR ZL
	ADC ZL,C0x00
	ADC ZH,C0x00
	LSR ACCUM_H
	ROR ACCUM_L
	ADC ACCUM_L,C0x00
	ADC ACCUM_H,C0x00
	RJMP _OS_DRAM_FREE_CELLS
OS_DRAM_FREE__8B:
	LDI_Y OS_DRAM_BMASK_8B
	SUBI ZL,low(OS_DRAM_8B)
	SBCI ZH,high(OS_DRAM_8B)
	MOV RESULT,ZL
	MCALL OS_DIVZ8
	ANDI RESULT,0x07
	BREQ PC+0x02
	ADIW Z,0x01
	MOV RESULT,ACCUM_L
	MCALL OS_ACC168
	ANDI RESULT,0x07
	SUBI RESULT,0x01
	ADC ACCUM_L,C0x00
	ADC ACCUM_H,C0x00
_OS_DRAM_FREE_CELLS:
	MOV ACCUM_EH,ZL
	ANDI ACCUM_EH,0x07
	MCALL OS_DIVZ8
	ADD YL,ZL
	ADC YH,ZH
	LDI ACCUM_EL,0x00
	MCALL _OS_DRAM_FILL_CELLS

	POP TEMP_H
	POP TEMP_L
	POP ACCUM_EH
	POP ACCUM_EL
	POP ACCUM_H
	POP ACCUM_L
	POP_Z
	POP_Y
	RET
;-----------------------------------------------------------;
_OS_DRAM_FIND_FREE_CELLS:
;-----------------------------------------------------------;
;Поиск блока свободных ячеек
;IN: ACCUM_H/L-размер блока в ячейках,
;ACCUM_EL-размер блока(байты) бит.маски,
;Y-адрес на бит.маску
;OUT: RESULT-OSR код, Z-адрес на выделенный блок
;-----------------------------------------------------------;
	LDI FLAGS,0x00											;Нулевой бит не найден
	LDI_X 0x0000											;Начальное смещение в блоке памяти

_OS_DRAM_FIND_FREE_CELLS__BYTELOOP:
	LD RESULT,Y												;Получаем байт карты
	CPI RESULT,0xff											;Пропускаем проверку бит, если все биты включены
	BREQ _OS_DRAM_FIND_FREE_CELLS__BYTELOOP_IS_HI

	LDI TEMP_L,0x08											;Цикл из 8 бит
_OS_DRAM_FIND_FREE_CELLS__BITLOOP:
	LSL RESULT												;Сдвигаем байт карты для поиска нулевого бита(младший бит слева)
	BRCS _OS_DRAM_FIND_FREE_CELLS__BITLOOP_IS_HI
	;Нашли нулевой бит
	CPI FLAGS,0x00
	BRNE PC+0x05
	;Это первый нулевой бит, запоминаем адрес памяти, инициализируем размер необходимых данных и включаем флаг
	MOVW ZL,XL
	LDI FLAGS,0x01
	MOV TEMP_H,TEMP_L
	MOVW TEMP_EL,ACCUM_L
	;Общая логика для первого и последующего нулевых бит
	SBIW TEMP_EL,0x01										;Вычитаем 1 ячейку из необходимого размера
	BREQ _OS_DRAM_FIND_FREE_CELLS__OK						;Если рвзмер стал нулевым, то блок найден
	RJMP _OS_DRAM_FIND_FREE_CELLS__BITLOOP_IS_NEXT
_OS_DRAM_FIND_FREE_CELLS__BITLOOP_IS_HI:
	LDI FLAGS,0x00											;Обрываем цепочку проверки длины блока нулевых битов
_OS_DRAM_FIND_FREE_CELLS__BITLOOP_IS_NEXT:
	ADIW XL,0x01
	DEC TEMP_L
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

	LDI RESULT,OSR_ALLOC_FAIL
	RJMP _OS_DRAM_FIND_FREE_CELLS__END

_OS_DRAM_FIND_FREE_CELLS__OK:
	;Блок найден, подготавливаем данные для заполнения блока бит.маски
	LDI ACCUM_EH,0x08
	SUB ACCUM_EH,TEMP_H
;	SUB YL,TEMP_H
;	SBC YH,C0x00
	LDI ACCUM_EL,0x01										;Устанавливаем биты в 1
	;Заполняем блок бит.маски
	MCALL _OS_DRAM_FILL_CELLS
	LDI RESULT,OSR_OK
_OS_DRAM_FIND_FREE_CELLS__END:
	RET

;-----------------------------------------------------------;
_OS_DRAM_FILL_CELLS:
;-----------------------------------------------------------;
;Заполняем блок бит.маски
;IN: Y-адрес(байты) со смещением на бит.маску
;ACCUM_EH-смещение(биты) на бит.маску, ACCUM_H/L-размер
;блока в ячейках, ACCUM_EL-установка бита(1) или сброс(0)
;-----------------------------------------------------------;
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
	LDI RESULT,0x08
	SUB RESULT,ACCUM_EH
	SUB ACCUM_L,RESULT
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
	MCALL OS_ACC168											;Получаю количество полностью заполненных байт
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

	RET

;-----------------------------------------------------------;
_OS_DRAM_BYTE_UPDATE:
;-----------------------------------------------------------;
;Обновляет байт значением
;IN: Y-адрес, TEMP_L-битовая маска,
;ACCUM_EL-установка бита(1) или сброс(0)
;-----------------------------------------------------------;
	LD RESULT,Y
	CPI ACCUM_EL,0x00
	BRNE PC+0x04
	COM TEMP_L
	AND RESULT,TEMP_L
	RJMP PC+0x02
	OR RESULT,TEMP_L
	ST Y+,RESULT
	RET
.ENDIF
