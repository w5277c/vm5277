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

.include "j8b/etrace_clear.asm"

.IFNDEF J8BPROC_ETRACE_ADD
;-----------------------------------------------------------
J8BPROC_ETRACE_ADDFIRST:
;-----------------------------------------------------------
;Устанавливаем в буфер ид типа исключения и код
;-----------------------------------------------------------
	MCALL J8BPROC_ETRACE_CLEAR
	STS _OS_ETRACE_BUFFER+0x00,r16							;В начале буфера записываем ид типа исключений
	STS _OS_ETRACE_BUFFER+0x01,r17							;И код

;-----------------------------------------------------------
J8BPROC_ETRACE_ADD:
;-----------------------------------------------------------
;Добавляем в буфер точку прохода(или переписываем последнюю)
;-----------------------------------------------------------
	PUSH_X
	PUSH TEMP_L

	LDI_X _OS_ETRACE_BUFFER+0x02							;Перемещаемся на первую точку
.IF OS_ETRACE_POINT_BITSIZE==0x07
	PUSH ACCUM_L
	PUSH ACCUM_EL
	LDI TEMP_L,OS_ETRACE_BUFFER_SIZE						;Количество итераций без последнего элемента
_J8BPROC_ETRACE_ADD__LOOP:
	LD ACCUM_L,X+											;Считываем первую точку
	CPI ACCUM_L,0x00										;Проверяем, если свободна - переходим на запись
	BREQ PC+0x05
	DEC TEMP_L												;Иначе продолжаем итерации
	BRNE _J8BPROC_ETRACE_ADD__LOOP
	ORI ACCUM_EL,0x80										;Все элементы заполнены, пишем в последний включив признак переполнения
	SBIW XL,0x01
	ST X,ACCUM_EL
	POP ACCUM_EL
	POP ACCUM_L
.ELSE
	PUSH ACCUM_H
	PUSH ACCUM_L
	PUSH ACCUM_EH
	LDI TEMP_L,OS_ETRACE_BUFFER_SIZE/2						;Аналогичная логика только для 15 битных элементов
_J8BPROC_ETRACE_ADD__LOOP:
	LD ACCUM_H,X+
	LD ACCUM_L,X+
	CPI ACCUM_H,0x00
	BRNE PC+0x03
	CPI ACCUM_L,0x00
	BREQ PC+0x05
	DEC TEMP_L
	BRNE _J8BPROC_ETRACE_ADD__LOOP
	ORI ACCUM_EH,0x80
	SBIW XL,0x02
	ST X+,ACCUM_EH
	ST X,ACCUM_EL
	POP ACCUM_EH
	POP ACCUM_L
	POP ACCUM_H
.ENDIF

	POP TEMP_L
	POP_X
    RET
.ENDIF
