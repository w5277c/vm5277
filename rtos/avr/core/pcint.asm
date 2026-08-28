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
;2 - по Flash таблице прерываний определяем адрес записии RAM таблицы прерываний, текущий порт и номер PCINT
;3 - для поддержки STDIN выполняю проверки и чтение байта с порта STDIN
;4 - проверяем на принятое слово 0x12 и переходим на бутлоадер при совпадении
;5 - сбрасываю все биты PCIFR текущего порта
;6 - разрешаем прерывания PCINT
;7 - пеерходим на код _OS_IRQ_HANDLER__PINT_FOR_PCINT для дальнейшей обработки прерывания

;ВАЖНО! Включение/выключение PCINT выполняется через запись в ячейку памяти _OS_PCINT_PCICR. Если старший бит включен - означает, что мы в
;обработчике PCINT. Если выключен, то пишем напрямую в в PCICR
;Таким образом мы сможем управлять состоянием PCINT как внутри обработчика прерывания, так и в остальном коде

.IF OS_FT_IRQ_PCINT_BLOCK==0x01
.IFNDEF _OS_IRQ_PCINT_HANDLER
.INCLUDE "conv/bit_to_mask.asm"
.IF OS_FT_STDIN
	.INCLUDE "mem/circ_buffer.asm"
.ENDIF

_OS_IRQ_PCINT_HANDLER:
	;Получаем адрес по которому определяем тип прерывания
	POP ATOM_L
	POP ATOM_L

	;Сохраняем регистры
	PUSH ACCUM_L
	LDS ACCUM_L,SREG

.IF _MCALL_SIZE == 0x02
	LSR ACCUM_L												;Делим на 2, если переходы размерностью в 2 слова
.ENDIF

	PUSH_A32
	PUSH_Y
	PUSH_Z

	LDS ACCUM_L,PCICR										;Запоминаем в отдельной ячейке значение PCICR
	ORI ACCUM_L,0x80										;Старший бит определяет выполнение кода в обработчике PCINT
	STS _OS_PCINT_PCICR,ACCUM_L
	STS PCICR,C0x00											;Запрещаем все прерывания PCINT

	LDI ZH,0x00
	MOV ZL,ATOM_L
	LPM ACCUM_EH,Z+											;Загружаем следующий байт из Flash после команды перехода - адрес записи в RAM таблице прерываний
	LPM ACCUM_EL,Z											;Загружаем номер порта (0-A,1-B,...) и номер PCINT(0,1,2...)

	MOV ACCUM_H,ACCUM_EL
   	ANDI ACCUM_EL,0x0f										;номер PCINT(0,1,2...)
   	SWAP ACCUM_H
   	ANDI ACCUM_H,0x0f										;PORT номер (0-A,1-B,...)

	LDI_Z PORTS_TABLE*2										;Вычисляю IO reg адрес порта (PINx)
	ADD ZL,ACCUM_H
	LPM ZL,Z												;Адрес IO регистра PINx
   	LDI_Y _OS_PCINT_STATES									;Вычисляем адрес предыдущего значения
   	ADD YL,ACCUM_EL
   	ADC YH,C0x00

   	LD ACCUM_L,Y											;Предыдущее значение порта
   	LD ACCUM_H,Z											;Текущее значение порта
   	EOR ACCUM_L,ACCUM_H										;Измененные пины в ACCUM_L
   	ST Y,ACCUM_H											;Записали новое значение

	;ACCUM_EL-номер PCINT, ACCUM_H-текущее состояние порта, ACCUM_L-биты измененных пинов, ZL-адрес IO регистра порта (PINx)
	;Если OS_FT_STDIN включена, то проверяем событие для пина STDIO_PORT
.IF OS_FT_STDIN == 0x01
.IFDEF STDIO_PORT_REGID
	CPI ZL,STDIO_PORTNUM-1									;Проверяем порт на использование для STDIN (PORTx-1=PINx)
.ELSE
	CPI ZL,STDIN_PORTNUM-1
.ENDIF
	BRNE __OS_IRQ_PCINT_HANDLER__STDIN_SKIP
.IFDEF STDIO_PORT_REGID
	SBRS ACCUM_L,STDIO_PINNUM								;Проверяем менял ли пин STDIN свое состояние
.ELSE
	SBRS ACCUM_L,STDIN_PINNUM
.ENDIF
	RJMP __OS_IRQ_PCINT_HANDLER__STDIN_SKIP					;Пин состояние не менял
.IFDEF STDIO_PORT_REGID
	SBRC ACCUM_H,STDIO_PINNUM								;Проверяем текущее состояние пина STDIN
.ELSE
	SBRC ACCUM_H,STDIN_PINNUM
.ENDIF
	RJMP __OS_IRQ_PCINT_HANDLER__STDIN_SKIP					;Низкий уровень - обнаружен START
	LDI TEMP_L,0x06											;Выдерживаем паузу для чтения UART
	DEC TEMP_L
	BRNE PC-0x01
.IF OS_FT_BLDR_API_REUSE == 0x01
	MCALL PROC__BLDR_UART_RECV_BYTE_NR						;Читаем байт прямо в прерывании PCINT
.ELSE
	MCALL OS_STDIN_NR
.ENDIF
	CLI														;Процедура разрешит глобальные прерывания, поэтому ранее запретили прерывания PCINT, запрещаем снова
	BREQ __OS_IRQ_PCINT_HANDLER__STDIN_SKIP					;Z=1 - ошибка приема
.IF OS_FT_SOFT_RESET == 0x01
	CPI ACCUM_L,0x12										;Проверка на MAGIC как команду перехода в бутлоадер
	BRNE __OS_IRQ_PCINT_HANDLER__PUT_TO_BUFFER
	JMP PROC__BLDR_INIT										;Переход на бутлоадер
__OS_IRQ_PCINT_HANDLER__PUT_TO_BUFFER:						;Помещаем полученный байт с UART в кольцевой буфер
.ENDIF
	LDI_Y _OS_STDIN_CBUFFER
	LDI TEMP_L,OS_STDIN_CBUFFER_SIZE
	MCALL OS_CIRC_BUFFER_WRITE								;Запись полученного байта с STDIN в кольцевой буфер
__OS_IRQ_PCINT_HANDLER__STDIN_SKIP:
.ENDIF

	PUSH TEMP_L
	MOV TEMP_L,ACCUM_EL										;Сбрасываю флаги прерываний PCINT для текущего порта
	MCALL BIT_TO_MASK
	STS PCIFR,TEMP_L
	POP TEMP_L

	LDI YH,0x00
	MOV YL,ACCUM_EH
	MCALL _OS_IRQ_HANDLER__SUBROUTINE

	POP_Z
	POP_Y
	LDS ACCUM_L,_OS_PCINT_PCICR								;Восстанавливаю из выделенной ячейки PCICR (разрешаю PCINTx)
	ANDI ACCUM_L,0x7f
	STS PCICR,ACCUM_L
	POP_A32
	STS SREG,ACCUM_L
	POP ACCUM_L
	RETI
.ENDIF
.ENDIF