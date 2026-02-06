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

;Обработчик внешних прерываний PIN Change interrupt
;1 - запрещаем все прерывания PCINT
;2 - определяю текущий порт PCINT
;3 - для поддержки STDIN выполняю проверки и чтение байта с порта STDIN
;4 - для поддержки DEV_MODE проверяю на принятое слово 0x7700 и перехожу на бутлоадер при совпадении
;5 - выполняю обработку таблицы драйверов с разрешенными глобально прерываниями
;6 - сбрасываю все биты PCIFR текущего порта
;7 - разрешаю прерывания PCINT

.IFNDEF _OS_PCINT_HANDLER

	.INCLUDE "conv/bit_to_mask.asm"
.IF OS_FT_STDIN
	.INCLUDE "mem/circ_buffer.asm"
.ENDIF

_OS_PCINT_HANDLER:
	CLI														;Запрещаем прерывания глобально
	STS PCICR,C0x00											;Запрещаем все прерывания PCINT
	;Получаем адрес по которому определяем тип прерывания
	POP ATOM_L
	POP ATOM_L

	;Сохраняем регистры
	PUSH_T32
	LDS TEMP_L,SREG
	PUSH TEMP_L

	MOV TEMP_H,ATOM_L
	SUBI TEMP_H,_OS_PCINT_0_VECTOR+_MCALL_SIZE				;Получаем номер PCINT
.IF _MCALL_SIZE == 0x02
	LSR TEMP_H												;Делим еще на 2, если переходы размерностью в 2 слова (JMP/CALL)
.ENDIF

	PUSH_Y
	PUSH_Z
	LDI_Y _OS_IR_VECTORS_TABLE+C5_IR_PCINT0*3-0x03			;Вычислем 3-х байтную ячейку нашего прерывания(учитыаем отсутствие RESET IR)
	MOV ZL,TEMP_H											;Умножаем на 3 (размер ячейки в программной таблице прерываний)
	LSL ZL
	ADD ZL,TEMP_H
	ADD YL,ZL
	ADC YH,C0x00
	LD TEMP_EH,Y											;Загружаем старое значение
	LDI_Z PORTS_TABLE*2+0x01								;PINx (учитываем отсутствие порта A, PCINT0=PORTB)
	ADD ZL,TEMP_H
	LPM ZL,Z
	LD TEMP_EL,Z											;Текущее значение
	EOR TEMP_EH,TEMP_EL
	ST Y,TEMP_EL											;Записали новое значение, измененные пины в TEMP_EH
	POP_Z

	;TEMP_H-номер PCINT или порта, где 0=PORTB, TEMP_EL-текущее состояние порта, TEMP_EH-биты измененных пинов
	;Если OS_FT_STDIN включена, то проверяем событие для пина STDIO_PORT
.IF OS_FT_STDIN
	PUSH ACCUM_L
.IFDEF STDIO_PORT_REGID
	CPI TEMP_H,STDIO_PORTNUM-1
.ELSE
	CPI TEMP_H,STDIN_PORTNUM-1
.ENDIF
	BRNE __OS_PCINT_HANDLER__STDIN_SKIP
.IFDEF STDIO_PORT_REGID
	SBRS TEMP_EH,STDIO_PINNUM
.ELSE
	SBRS TEMP_EH,STDIN_PINNUM
.ENDIF
	RJMP __OS_PCINT_HANDLER__STDIN_SKIP						;Пин состояние не менял
.IFDEF STDIO_PORT_REGID
	SBRC TEMP_EL,STDIO_PINNUM
.ELSE
	SBRC TEMP_EL,STDIN_PINNUM
.ENDIF
	RJMP __OS_PCINT_HANDLER__STDIN_SKIP						;Низкий уровень - START
	LDI TEMP_L,0x06											;Выдерживаем паузу для чтения UART
	DEC TEMP_L
	BRNE PC-0x01
.IF OS_FT_DEV_MODE
	MCALL PROC__BLDR_UART_RECV_BYTE_NR
.ELSE
	MCALL OS_STDIN_NR
.ENDIF
	BREQ __OS_PCINT_HANDLER__STDIN_SKIP						;Z=1 - ошибка приема
.IF OS_FT_DEV_MODE
	CPI ACCUM_L,0x12										;Проверка на слово 0x7700 как команду перехода в бутлоадер
	BRNE __OS_PCINT_HANDLER__PUT_TO_BUFFER
	JMP PROC__BLDR_INIT										;Переход на бутлоадер
__OS_PCINT_HANDLER__PUT_TO_BUFFER:
.ENDIF
	LDI_Y _OS_STDIN_CBUFFER
	LDI TEMP_L,OS_STDIN_CBUFFER_SIZE
	MCALL OS_CIRC_BUFFER_WRITE								;Запись полученного байта с STDIN в кольцевой буфер
__OS_PCINT_HANDLER__STDIN_SKIP:
	POP_Y
	POP ACCUM_L
.ENDIF

;	SEI														;Разрешаю прерывания глобально

	;TODO Будет таблица обработчиков драйверов, котрые мы должны вызвать в соответствии с привязкой к определенному пину и ISC

;	CLI
	MOV TEMP_L,TEMP_H										;Сбрасываю флаги прерываний PCINT для текущего порта
	MCALL BIT_TO_MASK
	STS PCIFR,TEMP_L
	LDS TEMP_L,_OS_PCINT_PCICR								;Разрешаю PCINT
	STS PCICR,TEMP_L
	POP TEMP_L
	STS SREG,TEMP_L
	POP_T32
	RETI													;Также разрешает прерывания
.ENDIF
