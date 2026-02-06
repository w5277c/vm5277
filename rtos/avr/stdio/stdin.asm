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

.IFNDEF OS_STDIN
OS_STDIN:
.IF OS_FT_STDIN == 0x01
;-----------------------------------------------------------;
OS_STDIN_IS_EMPTY:
;-----------------------------------------------------------;
;Буфер пуст?
;OUT: флаг Z-true - да
;-----------------------------------------------------------;
	PUSH_Y
	LDI_Y _OS_STDIN_CBUFFER
	MCALL OS_CIRC_BUFFER_IS_EMPTY
	POP_Y
	RET

;-----------------------------------------------------------;
OS_STDIN_GET:
;-----------------------------------------------------------;
;Читаем байт из STDIO
;OUT: ACCUM_L-значение, флаг Z-true - буфер пуст
;-----------------------------------------------------------;
	PUSH_Y
	PUSH TEMP_L
	LDI_Y _OS_STDIN_CBUFFER
	LDI TEMP_L,low(OS_STDIN_CBUFFER_SIZE)
	MCALL OS_CIRC_BUFFER_READ
	POP TEMP_L
	POP_Y
	RET

.IF OS_FT_DEV_MODE == 0x00
;-----------------------------------------------------------;
OS_STDIN_NR:												;Не восстанавливает регистр TEMP_L,TEMP_H
;-----------------------------------------------------------;
;Процедура считывающая байт с UART по событию от PCINT и помещающая его в буфер
;Прерывания должны быть запрещены в обработчике PCINT
;OUT: ACCUM_L-байт, Flag Z=1-ошибка приема
;-----------------------------------------------------------
	;Если мы здесь, значит получен START, паузу не выполняем, так как такты съела логика PCINT
	LDI TEMP_L,0x08											;Цикл в 8 бит (8n0)
_OS_STDIN__BITLOOP:
.IFDEF STDIO_PORT_REGID
	SBIC STDIO_PIN_REGID,STDIO_PINNUM						;Передаем бит с UART в флаг C
.ELSE
	SBIC STDIN_PIN_REGID,STDIN_PINNUM
.ENDIF
	SEC
	ROR ACCUM_L												;Записываем флаг в аккумулятор используя битовый сдвиг
	LDI TEMP_H,_OS_STDIO_BITDELAY_CONST
	RCALL OS_STDIO_1BIT_DELAY
	DEC TEMP_L												;Декрементируем счетчик бит
	BRNE _OS_STDIN__BITLOOP									;Повторяем для 8 итераций

	LDI TEMP_H,1
	MCALL OS_STDIO_1BIT_DELAY								;Пауза в 1 бит
	CLZ
.IFDEF STDIO_PORT_REGID
	SBIS STDIO_PIN_REGID,STDIO_PINNUM						;Проверяем STOP
.ELSE
	SBIS STDIN_PIN_REGID,STDIN_PINNUM						;Проверяем STOP
.ENDIF
	SEZ														;STOP не получен
	RET
.ENDIF
.ENDIF
.ENDIF
