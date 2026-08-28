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

;VERSION 0.1 - черновая версия, требует тщательного тестирования
;Аппаратная полудуплексная(запрос-ответ) асинхронная реализация для байта(8 бит) с поддержкой пина направления (для RS485 подобных)

;INFO Дравйвер блокирует диспетчер только если это запрос-ответ. Так как после передачи нужно моментально переключиться в режим приема.
;Диспетчер блокируется только на период передачи и настройки UART на прием. Ожидание данных выполняется с разблокированным диспетчеорм

;TODO пока поддержка только одного UART


.IFNDEF OS_DRV_UART_HWH__INIT
.include "j8b/_arr_offset.asm"
.include "drivers/uart/common.asm"
.include "core/irq_set.asm"

;Состояния драйвера
.EQU OS_DRV_UART_HWH_ST_READY						= OSR_OK
.EQU OS_DRV_UART_HWH_ST_FAIL						= OSR_HARDWARE_FAIL
.EQU OS_DRV_UART_HWH_ST_OVERFLOW					= OSR_OVERFLOW
.EQU OS_DRV_UART_HWH_ST_CANCELLED					= OSR_CANCELLED

.EQU OS_DRV_UART_HWH_ST_SENDING						= 0x41
.EQU OS_DRV_UART_HWH_ST_BREAK						= 0x42
.EQU OS_DRV_UART_HWH_ST_WAIT_DATA					= 0x81
.EQU OS_DRV_UART_HWH_ST_RECIEVING					= 0x82
.EQU OS_DRV_UART_HWH_ST_WAIT_END					= 0x83	;При OVERFLOW прием не обрывается, иначе последующий байт определим как новый пакет данных

;Фичи
.EQU OS_DRV_UART_HWH_FT_BREAK						= 0x00
.EQU OS_DRV_UART_HWH_FT_MAGIC						= 0x01
.EQU OS_DRV_UART_HWH_FT_U2X_OFF						= 0x02
.EQU OS_DRV_UART_HWH_FT_DIR_CONTROL					= 0x03

;Константы смещений в HEAP
.EQU _OS_DRV_UART_HWH__HEAP_IFACE_SN				= 0x05	;1B
.EQU _OS_DRV_UART_HWH__HEAP_BAUDRATE				= 0x06	;4B
.EQU _OS_DRV_UART_HWH__HEAP_PARITY					= 0x0a	;1B
.EQU _OS_DRV_UART_HWH__HEAP_STOP_BITS				= 0x0b	;1B
.EQU _OS_DRV_UART_HWH__HEAP_MAGIC					= 0x0c	;2B
.EQU _OS_DRV_UART_HWH__HEAP_MAGIC_CALLBACK			= 0x0e	;2B
.EQU _OS_DRV_UART_HWH__HEAP_DIR_PIN					= 0x11	;1B
.EQU _OS_DRV_UART_HWH__HEAP_FEATURES				= 0x12	;1B
.EQU _OS_DRV_UART_HWH__HEAP_STATE					= 0x13	;1B
;---данные драйвера (не являются полями класса)---
.EQU _OS_DRV_UART_HWH__HEAP_FRAME_GAP				= 0x14	;1B-время в мс для паузы BREAK и таймаута (диапазон 1-112)
.EQU _OS_DRV_UART_HWH__HEAP_BUFFER_ADDR				= 0x15	;2B-buffer addr
.EQU _OS_DRV_UART_HWH__HEAP_BUFFER_LEN				= 0x17	;2B-buffer length
.EQU _OS_DRV_UART_HWH__HEAP_TIMEOUT					= 0x19	;2B-receive timeout
.EQU _OS_DRV_UART_HWH__HEAP_BUFFER_OFFSET			= 0x1b	;2B-buffer offset
.EQU _OS_DRV_UART_HWH__HEAP_UPTIME					= 0x1d	;1B-Младший байт UPTIME для определения inter-character таймаута
.EQU _OS_DRV_UART_HWH__HEAP_TOTAL_SIZE				= 0x1e

;-----------------------------------------------------------
OS_DRV_UART_HWH__INIT_NR:
;-----------------------------------------------------------
;Инициализация аппаратоного UART
;IN:Y-адрес HEAP
;MOD: ACCUM_L
;-----------------------------------------------------------
	PUSH_Z
	PUSH_T32

	_OS_POWER_ON PRUSART0									;Подаем питание на UART0

	LDI ACCUM_L,byte1(CORE_FREQ*125)						;Вычисляем значение для UBRR с включенным U2X
	LDI ACCUM_H,byte2(CORE_FREQ*125)
	LDI ACCUM_EL,byte3(CORE_FREQ*125)
	LDI ACCUM_EH,byte4(CORE_FREQ*125)
	LDD TEMP_L,Y+_OS_DRV_UART_HWH__HEAP_BAUDRATE+0x00
	LDD TEMP_H,Y+_OS_DRV_UART_HWH__HEAP_BAUDRATE+0x01
	LDD TEMP_EL,Y+_OS_DRV_UART_HWH__HEAP_BAUDRATE+0x02
	LDD TEMP_EH,Y+_OS_DRV_UART_HWH__HEAP_BAUDRATE+0x03
	MCALL OS_DIV32
	LSL TEMP_L												;Округляем
	ROL TEMP_H
	ROL TEMP_EL
	ROL TEMP_EH
	LDD ZL,Y+_OS_DRV_UART_HWH__HEAP_BAUDRATE+0x00
	CP TEMP_L,ZL
	LDD ZL,Y+_OS_DRV_UART_HWH__HEAP_BAUDRATE+0x01
	CPC TEMP_H,ZL
	LDD ZL,Y+_OS_DRV_UART_HWH__HEAP_BAUDRATE+0x02
	CPC TEMP_EL,ZL
	LDD ZL,Y+_OS_DRV_UART_HWH__HEAP_BAUDRATE+0x03
	CPC TEMP_EH,ZL
	BRCC _OS_DRV_UART_HWH__INIT_NR__SKIP_UBRR_DEC
	SUBI ACCUM_L,0x01
	SBCI ACCUM_H,0x00
	SBCI ACCUM_EL,0x00
	SBCI ACCUM_EH,0x00
_OS_DRV_UART_HWH__INIT_NR__SKIP_UBRR_DEC:
	LDI ACCUM_EL,(1<<RXC0)|(1<<TXC0)|(1<<U2X0)				;Устанавливаем значения для UCSR0A (U2X включен)
	LDD ACCUM_EH,Y+_OS_DRV_UART_HWH__HEAP_FEATURES			;Проверяем фичу FT_U2X_OFF и переполнение UBRR
	SBRC ACCUM_EH,OS_DRV_UART_HWH_FT_U2X_OFF
	RJMP _OS_DRV_UART_HWH__INIT_NR__U2X_FALSE
	CPI ACCUM_L,low(4095)
	LDI ZL,high(4095)
	CPC ACCUM_H,ZL
	BRCS _OS_DRV_UART_HWH__INIT_NR__U2X_TRUE
_OS_DRV_UART_HWH__INIT_NR__U2X_FALSE:
	LDI ACCUM_EL,(1<<RXC0)|(1<<TXC0)|(0<<U2X0)				;Устанавливаем значения для UCSR0A (U2X выключен)
	SBRS ACCUM_L,0x00										;Делим значение UBRR на 2 с корректным округлением для U2X OFF
	RJMP PC+0x03
	ADD ACCUM_L,C0x01
	ADC ACCUM_H,C0x00
	LSR ACCUM_H
	ROR ACCUM_L
_OS_DRV_UART_HWH__INIT_NR__U2X_TRUE:
	STS UCSR0A,ACCUM_EL
	STS UBRR0L,ACCUM_L
	STS UBRR0H,ACCUM_H
	STS UCSR0B,C0x00										;Отключаем прием и передачу
	LDI ACCUM_EL,low((1<<URSEL0)|(1<<UCSZ01)|(1<<UCSZ00))	;Async, 8 data bits
	LDD ACCUM_L,Y+_OS_DRV_UART_HWH__HEAP_PARITY				;Устанавливаем parity
	SWAP ACCUM_L
	OR ACCUM_EL,ACCUM_L
	LDD ACCUM_L,Y+_OS_DRV_UART_HWH__HEAP_STOP_BITS			;Устанавливаем кол-во стоп бит
	SWAP ACCUM_L
	LSR ACCUM_L
	OR ACCUM_EL,ACCUM_L
	STS UCSR0C,ACCUM_EL										;URSEL необходим для ATmega16, в остальных должен быть равен 0

	SBRS ACCUM_EH,OS_DRV_UART_HWH_FT_DIR_CONTROL			;Проверяем фичу FT_DIR_CONTROL
	RJMP _OS_DRV_UART_HWH__INIT_NR__SET_DIR_PIN_SKIP
	LDD ACCUM_L,Y+_OS_DRV_UART_HWH__HEAP_DIR_PIN
	MCALL PIN_DIR_OUT
	MCALL PIN_SET_LO
_OS_DRV_UART_HWH__INIT_NR__SET_DIR_PIN_SKIP:

	LDI ACCUM_L,OS_IRQ_UART0_RX								;Устанавливаем обработчик для прерывания UART RX
	LDI_Z _OS_DRV_UART_HWH__IRQ_RX_HANDLER
	MCALL OS_IRQ_SET
	LDI ACCUM_L,OS_IRQ_UART0_TX								;Устанавливаем обработчик для прерывания UART TX
	LDI_Z _OS_DRV_UART_HWH__IRQ_TX_HANDLER
	MCALL OS_IRQ_SET

	;Вычисляем время задержки для BREAK и детектирования конца посылки.
	;Минимум 1мс, это вполне приемлимо даже для выских BAUDRATE
	LDD ACCUM_L,Y+_OS_DRV_UART_HWH__HEAP_BAUDRATE+0x01		;high(BAUDRATE)
	CP ACCUM_L,112											;1 мс достаточно, если BAUDRATE > 28672
	LDI ACCUM_L,0x01
	BRCC _OS_DRV_UART_HWH__COMM_NR__BREAK_DIV_SKIP
	LDI ACCUM_L,112
	LDD ACCUM_H,Y+_OS_DRV_UART_HWH__HEAP_BAUDRATE+0x01
	MCALL OS_DIV8
	INC ACCUM_L												;Берем на 1 больше (на случай округления, точность не нужна)
_OS_DRV_UART_HWH__COMM_NR__BREAK_DIV_SKIP:
	STD Y+_OS_DRV_UART_HWH__HEAP_FRAME_GAP,ACCUM_L

	LDI ACCUM_L,TXD
	MCALL PIN_DIR_OUT
	MCALL PIN_SET_LO
	LDI ACCUM_L,RXD
	MCALL PIN_DIR_IN
	MCALL PIN_SET_HI

	POP_T32
	POP_Z
	RET

;-----------------------------------------------------------
OS_DRV_UART_HWH__CLOSE_NR:
;-----------------------------------------------------------
;Запрещаем аппаратный UART
;IN:Y-адрес HEAP
;MOD: ACCUM_L
;-----------------------------------------------------------
	LDI ACCUM_L,OS_DRV_UART_HWH_ST_CANCELLED
	STD Y+_OS_DRV_UART_HWH__HEAP_STATE,ACCUM_L
	RET

;-----------------------------------------------------------
_OS_DRV_UART_HWH__COMM_NR:
;-----------------------------------------------------------
;Прием/передача данных
;IN:Y-адрес HEAP, X-SRC buff, ACCUM_EL/EH-SRC длина,
;TEMP_EL/EH-DST buff, TEMP_L/H-DST длина,
;ACCUM_L/H-таймаут в мс
;OUT: ACCUM_L/H-длина полученных данных или
;BoundsException
;MOD: X
;-----------------------------------------------------------
	PUSH_Z
	PUSH_T32

	STD Y+_OS_DRV_UART_HWH__HEAP_TIMEOUT+0x00,ACCUM_L
	STD Y+_OS_DRV_UART_HWH__HEAP_TIMEOUT+0x01,ACCUM_H
	;Блок передачи
	CP ACCUM_EL,C0x00
	CPC ACCUM_EH,C0x00
	BREQ _OS_DRV_UART_HWH__COMM_RECV_BLOCK
	PUSH_A16
	LDI ACCUM_L,0x00
	LDI ACCUM_H,0x00
	MCALL _J8BPROC_ARR_OFFSET
	POP_A16
	BRTS _OS_DRV_UART_HWH__COMM_END
	STD Y+_OS_DRV_UART_HWH__HEAP_BUFFER_ADDR+0x00,XL		;Инициализация данных
	STD Y+_OS_DRV_UART_HWH__HEAP_BUFFER_ADDR+0x01,XH
	STD Y+_OS_DRV_UART_HWH__HEAP_BUFFER_LEN+0x00,TEMP_L
	STD Y+_OS_DRV_UART_HWH__HEAP_BUFFER_LEN+0x01,TEMP_H
	STD Y+_OS_DRV_UART_HWH__HEAP_BUFFER_OFFSET+0x00,C0x00
	STD Y+_OS_DRV_UART_HWH__HEAP_BUFFER_OFFSET+0x01,C0x00

	MOVW XL,TEMP_EL											;Меняем регистры местами чтобы перенести таймаут в TEMP_EL/EH для OS_PROC_AWAIT
	MOVW TEMP_EL,ACCUM_L
	MOVW ACCUM_EL,TEMP_L

	CP ACCUM_EL,C0x00										;Необходимо блокировать диспетчер если ожидаем данные в ответ
	CPC ACCUM_EH,C0x00
	BREQ _OS_DRV_UART_HWH__COMM__SKIP_DISP_LOCK
	MCALL OS_DISPATCHER_LOCK								;Блокируем диспетчер, так как между отправкой и приемом должна быть минимальная задержка
_OS_DRV_UART_HWH__COMM__SKIP_DISP_LOCK:

	LDD ACCUM_H,Y+_OS_DRV_UART_HWH__HEAP_FEATURES			;Управление направлением(OUT)
	SBRS ACCUM_H,OS_DRV_UART_HWH_FT_DIR_CONTROL
	RJMP _OS_DRV_UART_HWH__COMM_NR__SKIP_DIR_CONTROL1
	LDI ACCUM_L,Y+_OS_DRV_UART_HWH__HEAP_DIR_PIN
	MCALL PIN_SET_HI
_OS_DRV_UART_HWH__COMM_NR__SKIP_DIR_CONTROL1:
	LDI ACCUM_L,OS_DRV_UART_HWH_ST_SENDING					;Инициализация UART для передачи
	STD Y+_OS_DRV_UART_HWH__HEAP_STATE,ACCUM_L
	LDS ACCUM_L,UCSR0B										;Разрешаем UART(передача)
	ANDI ACCUM_L,low(~((1<<RXCIE0)|(1<<RXEN0)))
	ORI ACCUM_L,(1<<TXCIE0)|(1<<TXEN0)
	STS UCSR0B,ACCUM_L
	MCALL _OS_DRV_UART_HWH__IRQ_TX_HANDLER					;Отправляю первый байт
_OS_DRV_UART_HWH__COMM_NR__SEND_WAIT_LOOP:
	SLEEP													;Диспетчер заблокирован, остаются только прерывания
	LDD ACCUM_L,Y+_OS_DRV_UART_HWH__HEAP_STATE				;Ожидание окончания передачи(с минимальной задержкой)
	SBRC ACCUM_L,0x06
	RJMP _OS_DRV_UART_HWH__COMM_NR__SEND_WAIT_LOOP
	SBRS ACCUM_H,OS_DRV_UART_HWH_FT_DIR_CONTROL				;Управление направлением(IN)
	RJMP _OS_DRV_UART_HWH__COMM_NR__SKIP_DIR_CONTROL2
	PUSH ACCUM_L
	LDI ACCUM_L,Y+_OS_DRV_UART_HWH__HEAP_DIR_PIN
	MCALL PIN_SET_LO
	POP ACCUM_L
_OS_DRV_UART_HWH__COMM_NR__SKIP_DIR_CONTROL2:
	CPI ACCUM_L,OS_DRV_UART_HWH_ST_READY
	BRNE _OS_DRV_UART_HWH__COMM_END							;Ошибка, выходим

	SBRS ACCUM_H,OS_DRV_UART_HWH_FT_BREAK					;Проверка на включенную фичу BREAK
	RJMP _OS_DRV_UART_HWH__COMM_NR__SKIP_BREAK
	LDI ACCUM_L,TXD											;Низкий продолжительный уровень TX - BREAK
	MCALL PIN_SET_LO
	LDD ACCUM_L,Y+_OS_DRV_UART_HWH__HEAP_FRAME_GAP
	LDI ACCUM_H,0x00
	MCALL OS_WAIT_MS
	LDI ACCUM_L,TXD											;Восстанавливаем уровень TX
	MCALL PIN_SET_HI
_OS_DRV_UART_HWH__COMM_NR__SKIP_BREAK:

_OS_DRV_UART_HWH__COMM_RECV_BLOCK:
	LDI ACCUM_L,OSR_OK
	STD Y+_OS_DRV_UART_HWH__HEAP_BUFFER_OFFSET+0x00,C0x00
	STD Y+_OS_DRV_UART_HWH__HEAP_BUFFER_OFFSET+0x01,C0x00
	;Блок приема
	CP ACCUM_EL,C0x00
	CPC ACCUM_EH,C0x00
	BREQ _OS_DRV_UART_HWH__COMM_END
	LDI ACCUM_L,0x00
	LDI ACCUM_H,0x00
	MCALL _J8BPROC_ARR_OFFSET
	BRTS _OS_DRV_UART_HWH__COMM_END

	STD Y+_OS_DRV_UART_HWH__HEAP_BUFFER_ADDR+0x00,XL
	STD Y+_OS_DRV_UART_HWH__HEAP_BUFFER_ADDR+0x01,XH
	STD Y+_OS_DRV_UART_HWH__HEAP_BUFFER_LEN+0x00,ACCUM_EL
	STD Y+_OS_DRV_UART_HWH__HEAP_BUFFER_LEN+0x01,ACCUM_EH
	LDI ACCUM_L,OS_DRV_UART_HWH_ST_WAIT_DATA				;Инициализация UART для приема (ждем данные)
	STD Y+_OS_DRV_UART_HWH__HEAP_STATE,ACCUM_L
	LDS ACCUM_L,UCSR0B										;Разрешаем UART(прием)
	ANDI ACCUM_L,~((1<<TXCIE0)|(1<<TXEN0))
	ORI ACCUM_L,(1<<RXCIE0)|(1<<RXEN0)
	STS UCSR0B,ACCUM_L

	MCALL OS_DISPATCHER_UNLOCK								;UART настроен на прием, можно снимать блокировку диспетчера
    CLI														;Метка с текущим uptime
    LDS XL,_OS_UPTIME+0x00
    LDS XH,_OS_UPTIME+0x01
	SEI
_OS_DRV_UART_HWH__COMM_NR__RECV_WAIT_LOOP:
	LDI ACCUM_L,0x01
	LDI ACCUM_H,0x00
	MCALL OS_WAIT_MS
	LDD ACCUM_L,Y+_OS_DRV_UART_HWH__HEAP_STATE				;Ожидание окончания приема
	SBRS ACCUM_L,0x07
	RJMP _OS_DRV_UART_HWH__COMM_RECV_DONE
	CLI
	LDD TEMP_L,Y+_OS_DRV_UART_HWH__HEAP_BUFFER_OFFSET+0x00
	LDD TEMP_H,Y+_OS_DRV_UART_HWH__HEAP_BUFFER_OFFSET+0x01
	SEI
	CP TEMP_L,C0x00
	CPC TEMP_H,C0x00
	BREQ _OS_DRV_UART_HWH__COMM_NR__RECV_CHECK_TIMEOUT
	LDD TEMP_L,Y+_OS_DRV_UART_HWH__HEAP_UPTIME
	LDS TEMP_H,UPTIME+0x00
	SUB TEMP_H,TEMP_L
	LDD TEMP_L,Y+_OS_DRV_UART_HWH__HEAP_FRAME_GAP
	CP TEMP_H,TEMP_L
	BRCC _OS_DRV_UART_HWH__COMM_DONE_BLOCK
	RJMP _OS_DRV_UART_HWH__COMM_NR__RECV_WAIT_LOOP
_OS_DRV_UART_HWH__COMM_NR__RECV_CHECK_TIMEOUT:
	MCALL OS_UPTIME16
	SUB ACCUM_L,XL											;Считаем дельту времени
	SBC ACCUM_H,XH
	CP ACCUM_L,TEMP_EL										;Проверяем на таймаут
	CPC ACCUM_H,TEMP_EH
	BRCS _OS_DRV_UART_HWH__COMM_NR__RECV_WAIT_LOOP
	RJMP _OS_DRV_UART_HWH__COMM_TIMEOUT
_OS_DRV_UART_HWH__COMM_RECV_DONE:
	CPI ACCUM_L,OSR_DRV_UART_HW_ST_READY
	BRNE _OS_DRV_UART_HWH__COMM_END							;Ошибка, выходим

_OS_DRV_UART_HWH__COMM_DONE_BLOCK:
	LDI ACCUM_L,OSR_OK
	RJMP _OS_DRV_UART_HWH__COMM_END
_OS_DRV_UART_HWH__COMM_TIMEOUT:
	LDI ACCUM_L,OSR_TIMEOUT
_OS_DRV_UART_HWH__COMM_END:
	STD Y+_OS_DRV_UART_HWH__HEAP_STATE,ACCUM_L
	LDS ACCUM_H,UCSR0B										;Запрещаем UART
	ANDI ACCUM_H,low(~((1<<TXCIE0)|(1<<TXEN0)|(1<<RXCIE0)|(1<<RXEN0)))
	STS UCSR0B,ACCUM_H

	LDD ACCUM_L,Y+_OS_DRV_UART_HWH__HEAP_BUFFER_OFFSET+0x00
	LDD ACCUM_H,Y+_OS_DRV_UART_HWH__HEAP_BUFFER_OFFSET+0x01
	POP_T32
	POP_Z
	RET

;-----------------------------------------------------------
_OS_DRV_UART_HWH__IRQ_TX_HANDLER:
;-----------------------------------------------------------
;Событие от шины (байт передан)
;IN:Y-адрес HEAP
;-----------------------------------------------------------
	PUSH_A32
	PUSH_Z

	LDD ACCUM_EL,Y+_OS_DRV_UART_HWH__HEAP_STATE				;Проверка на режим передачи
	SBRS ACCUM_EL,0x06
	RJMP _OS_DRV_UART_HWH__IRQ_TX_HANDLER__END

	LDD ACCUM_L,Y+_OS_DRV_UART_HWH__HEAP_BUFFER_OFFSET+0x00
	LDD ACCUM_H,Y+_OS_DRV_UART_HWH__HEAP_BUFFER_OFFSET+0x01
	LDD ACCUM_EL,Y+_OS_DRV_UART_HWH__HEAP_BUFFER_LEN+0x00
	LDD ACCUM_EH,Y+_OS_DRV_UART_HWH__HEAP_BUFFER_LEN+0x01
	CP ACCUM_L,ACCUM_EL
	CPC ACCUM_H,ACCUM_EH
	BRNE _OS_DRV_UART_HWH__IRQ_TX_HANDLER__HAVE_DATA
	LDI ACCUM_L,TXD											;Устанавливаем состояние пина TX
	MCALL PIN_DIR_OUT
	MCALL PIN_SET_HI
	LDS ACCUM_L,UCSR0B										;Отключаем передатчик
	ANDI ACCUM_L,~((1<<TXCIE0)|(1<<TXEN0))
	STS UCSR0B,ACCUM_L
	LDI ACCUM_L,OS_DRV_UART_HWH_ST_READY
	STD Y+_OS_DRV_UART_HWH__HEAP_STATE,ACCUM_L
	RJMP _OS_DRV_UART_HWH__IRQ_TX_HANDLER__END
_OS_DRV_UART_HWH__IRQ_TX_HANDLER__HAVE_DATA:
	LDD ZL,Y+_OS_DRV_UART_HWH__HEAP_BUFFER_ADDR+0x00
	LDD ZH,Y+_OS_DRV_UART_HWH__HEAP_BUFFER_ADDR+0x01
	ADD ZL,ACCUM_L
	ADC ZH,ACCUM_H
	ADD ACCUM_L,C0x01
	ADC ACCUM_H,C0x00
	STD Y+_OS_DRV_UART_HWH__HEAP_BUFFER_OFFSET+0x00,ACCUM_L
	STD Y+_OS_DRV_UART_HWH__HEAP_BUFFER_OFFSET+0x01,ACCUM_H
	LDD ACCUM_L,Z
	STS UDR0,ACCUM_L

_OS_DRV_UART_HWH__IRQ_TX_HANDLER__END:
	POP_Z
	POP_A32
	RET


;-----------------------------------------------------------
_OS_DRV_UART_HWH__IRQ_RX_HANDLER:
;-----------------------------------------------------------
;Событие от шины (принят байт)
;IN:Y-адрес HEAP
;-----------------------------------------------------------
	PUSH_A32
	PUSH_Z
	PUSH FLAGS

	LDS ACCUM_L,UCSR0A
	LDS FLAGS,UDR0											;Считываем принятый байт
	LDS ACCUM_H,UPTIME+0x00									;Фиксируем текущий UPTIME для обновления inter-character таймаута
	STD Y+_OS_DRV_UART_HWH__HEAP_UPTIME,ACCUM_H

	LDD ACCUM_EL,Y+_OS_DRV_UART_HWH__HEAP_STATE				;Проверка на режим приема
	SBRS ACCUM_EL,0x07
	RJMP _OS_DRV_UART_HWH__IRQ_RX_HANDLER__END
	SBRC ACCUM_L,DOR0										;Проверка на ошибку переполнения
	RJMP _OS_DRV_UART_HWH__IRQ_RX_HANDLER__ERROR
	SBRC ACCUM_L,UPE0										;Проверка на ошибку четности
	RJMP _OS_DRV_UART_HWH__IRQ_RX_HANDLER__ERROR
	;Блок проверки на BREAK
	SBRS ACCUM_L,FE0										;FE включен - не получили STOP (вместо HI->LO уровень)
	RJMP _OS_DRV_UART_HWH__IRQ_RX_HANDLER__NO_ERROR
	CPI FLAGS,0x00											;Проверяем на BREAK
	BRNE _OS_DRV_UART_HWH__IRQ_RX_HANDLER__ERROR			;Переходим если это сбой передачи (не BREAK)
	LDI ACCUM_L,OS_DRV_UART_HWH_ST_READY					;Получен BREAK, прием завершен
	RJMP _OS_DRV_UART_HWH__IRQ_RX_HANDLER__DONE
_OS_DRV_UART_HWH__IRQ_RX_HANDLER__NO_ERROR:
	;Подготовка данных
	LDD ACCUM_L,Y+_OS_DRV_UART_HWH__HEAP_BUFFER_OFFSET+0x00	;Считываем смещение в буфере
	LDD ACCUM_H,Y+_OS_DRV_UART_HWH__HEAP_BUFFER_OFFSET+0x01
	LDD ACCUM_EL,Y+_OS_DRV_UART_HWH__HEAP_BUFFER_LEN+0x00	;Считываем размер буфера
	LDD ACCUM_EH,Y+_OS_DRV_UART_HWH__HEAP_BUFFER_LEN+0x01
	CP ACCUM_L,ACCUM_EL										;Проверяем не заполнен ли буфер
	CPC ACCUM_H,ACCUM_EH
	BRNE _OS_DRV_UART_HWH__IRQ_RX_HANDLER__BUFF_NOT_FULL
	LDI ACCUM_L,OS_DRV_UART_HWH_ST_WAIT_END					;Если заполнен - переходим в режим ожидания конца приема
	STD Y+_OS_DRV_UART_HWH__HEAP_STATE,ACCUM_L
	RJMP _OS_DRV_UART_HWH__IRQ_RX_HANDLER__END
_OS_DRV_UART_HWH__IRQ_RX_HANDLER__BUFF_NOT_FULL:
	LDD ZL,Y+_OS_DRV_UART_HWH__HEAP_BUFFER_ADDR+0x00		;Загружаем адрес буфера
	LDD ZH,Y+_OS_DRV_UART_HWH__HEAP_BUFFER_ADDR+0x01
	ADD ZL,ACCUM_L											;Смещаемся на текущий байт
	ADC ZH,ACCUM_H
	;Блок проверки на MAGIC
	CP ACCUM_L,C0x00										;Проверяем на первый байт для пропуска проверки на MAGIC слово
	CPC ACCUM_H,C0x00
	BREQ _OS_DRV_UART_HWH__IRQ_RX_HANDLER__SKIP_MAGIC_CHECK
	LDD ACCUM_EL,Y+_OS_DRV_UART_HWH__HEAP_FEATURES			;Проверяем на включенную фичу FT_MAGIC
	SBRS ACCUM_EL,OS_DRV_UART_HWH_FT_MAGIC
	RJMP _OS_DRV_UART_HWH__IRQ_RX_HANDLER__SKIP_MAGIC_CHECK	;Пропускаем проверку MAGIC если фича выключена
	LDD ACCUM_EL,Y+_OS_DRV_UART_HWH__HEAP_MAGIC+0x01		;Проверяем на второй байт
	CP ACCUM_EL,FLAGS
	BRNE _OS_DRV_UART_HWH__IRQ_RX_HANDLER__SKIP_MAGIC_CHECK
	SBIW ZL,0x01
	LDD ACCUM_EH,Z											;Достаем из буфера предыдущий байт
	ADIW ZL,0x01
	LDD ACCUM_EL,Y+_OS_DRV_UART_HWH__HEAP_MAGIC+0x00		;И проверяем его на первый байт MAGIC
	CP ACCUM_EL,ACCUM_EH
	BRNE _OS_DRV_UART_HWH__IRQ_RX_HANDLER__SKIP_MAGIC_CHECK
	LDD ZL,Y+_OS_DRV_UART_HWH__HEAP_MAGIC_CALLBACK+0x00		;Загружаем адрес процедуры обработчика MAGIC
	LDD ZH,Y+_OS_DRV_UART_HWH__HEAP_MAGIC_CALLBACK+0x01
	ICALL
	STD Y+_OS_DRV_UART_HWH__HEAP_BUFFER_OFFSET+0x00,C0x00
	STD Y+_OS_DRV_UART_HWH__HEAP_BUFFER_OFFSET+0x01,C0x00
	LDI ACCUM_L,OS_DRV_UART_HWH_ST_READY
	RJMP _OS_DRV_UART_HWH__IRQ_RX_HANDLER__DONE
_OS_DRV_UART_HWH__IRQ_RX_HANDLER__SKIP_MAGIC_CHECK:
	STD Z,FLAGS
	ADD ACCUM_L,C0x01
	ADC ACCUM_H,C0x00
	STD Y+_OS_DRV_UART_HWH__HEAP_BUFFER_OFFSET+0x00,ACCUM_L
	STD Y+_OS_DRV_UART_HWH__HEAP_BUFFER_OFFSET+0x01,ACCUM_H
	RJMP _OS_DRV_UART_HWH__IRQ_RX_HANDLER__END

_OS_DRV_UART_HWH__IRQ_RX_HANDLER__ERROR:
	LDI ACCUM_L,OS_DRV_UART_HWH_ST_FAIL
_OS_DRV_UART_HWH__IRQ_RX_HANDLER__DONE:
	STD Y+_OS_DRV_UART_HWH__HEAP_STATE,ACCUM_L
	LDS ACCUM_L,UCSR0B										;Если и требовалась работа с UART, то обработчик ее выполнил, завершаемся.
	ANDI ACCUM_L,low(~((1<<RXCIE0)|(1<<RXEN0)))
	STS UCSR0B,ACCUM_L
_OS_DRV_UART_HWH__IRQ_RX_HANDLER__END:

	POP FLAGS
	POP_Z
	POP_A32
	RET
.ENDIF