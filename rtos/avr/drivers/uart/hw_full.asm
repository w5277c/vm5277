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
;Аппаратная полноудуплексная асинхронная реализация для байта(8 бит) с использованием кольцевых буферов

;TODO пока поддержка только одного UART

.include "drivers/uart/common.asm"
.include "core/irq_set.asm"
.include "math/div32.asm"
.include "drivers/mem/circ_buff16.asm"

.IFNDEF OS_DRV_UART_HWF__INIT

;Флаги состояния драйвера
.EQU OS_DRV_UART_HWF_FL_OPEN						= 0x00
.EQU OS_DRV_UART_HWF_FL_SENDING						= 0x01

;Константы смещений в HEAP
.EQU _OS_DRV_UART_HWF__HEAP_SENDBUFF_HEAPADDR		= 0x05	;2B
.EQU _OS_DRV_UART_HWF__HEAP_RECVBUFF_HEAPADDR		= 0x07	;2B
.EQU _OS_DRV_UART_HWF__HEAP_IFACE_SN				= 0x09	;1B
.EQU _OS_DRV_UART_HWF__HEAP_BAUDRATE				= 0x0a	;4B
.EQU _OS_DRV_UART_HWF__HEAP_PARITY					= 0x0e	;1B
.EQU _OS_DRV_UART_HWF__HEAP_STOP_BITS				= 0x0f	;1B
.EQU _OS_DRV_UART_HWF__HEAP_FLAGS					= 0x10	;1B
.EQU _OS_DRV_UART_HWF__HEAP_ERRCODE					= 0x11	;1B
.EQU _OS_DRV_UART_HWF__HEAP_TOTAL_SIZE				= 0x12

;-----------------------------------------------------------
OS_DRV_UART_HWF__INIT_NR:
;-----------------------------------------------------------
;Инициализация аппаратоного UART
;IN:Y-адрес HEAP
;MOD: ACCUM_L/H/EL/EH
;-----------------------------------------------------------
	PUSH_Z
	PUSH_T32

	_OS_POWER_ON PRUSART0									;Подаем питание на UART0

	LDI ACCUM_L,byte1(CORE_FREQ*125)						;Вычисляем значение для UBRR с включенным U2X
	LDI ACCUM_H,byte2(CORE_FREQ*125)
	LDI ACCUM_EL,byte3(CORE_FREQ*125)
	LDI ACCUM_EH,byte4(CORE_FREQ*125)
	LDD TEMP_L,Y+_OS_DRV_UART_HWF__HEAP_BAUDRATE+0x00
	LDD TEMP_H,Y+_OS_DRV_UART_HWF__HEAP_BAUDRATE+0x01
	LDD TEMP_EL,Y+_OS_DRV_UART_HWF__HEAP_BAUDRATE+0x02
	LDD TEMP_EH,Y+_OS_DRV_UART_HWF__HEAP_BAUDRATE+0x03
	MCALL OS_DIV32
	LSL TEMP_L												;Округляем
	ROL TEMP_H
	ROL TEMP_EL
	ROL TEMP_EH
	LDD ZL,Y+_OS_DRV_UART_HWF__HEAP_BAUDRATE+0x00
	CP TEMP_L,ZL
	LDD ZL,Y+_OS_DRV_UART_HWF__HEAP_BAUDRATE+0x01
	CPC TEMP_H,ZL
	LDD ZL,Y+_OS_DRV_UART_HWF__HEAP_BAUDRATE+0x02
	CPC TEMP_EL,ZL
	LDD ZL,Y+_OS_DRV_UART_HWF__HEAP_BAUDRATE+0x03
	CPC TEMP_EH,ZL
	BRCC _OS_DRV_UART_HWF__INIT_NR__SKIP_UBRR_DEC
	SUBI ACCUM_L,0x01
	SBCI ACCUM_H,0x00
	SBCI ACCUM_EL,0x00
	SBCI ACCUM_EH,0x00
_OS_DRV_UART_HWF__INIT_NR__SKIP_UBRR_DEC:
	LDI ACCUM_EL,(1<<RXC0)|(1<<TXC0)|(1<<U2X0)				;Устанавливаем значения для UCSR0A (U2X включен)
	CPI ACCUM_L,low(4095)
	LDI ZL,high(4095)
	CPC ACCUM_H,ZL
	BRCS _OS_DRV_UART_HWF__INIT_NR__U2X_TRUE
_OS_DRV_UART_HWF__INIT_NR__U2X_FALSE:
	LDI ACCUM_EL,(1<<RXC0)|(1<<TXC0)|(0<<U2X0)				;Устанавливаем значения для UCSR0A (U2X выключен)
	SBRS ACCUM_L,0x00										;Делим значение UBRR на 2 с корректным округлением для U2X OFF
	RJMP PC+0x03
	ADD ACCUM_L,C0x01
	ADC ACCUM_H,C0x00
	LSR ACCUM_H
	ROR ACCUM_L
_OS_DRV_UART_HWF__INIT_NR__U2X_TRUE:
	STS UCSR0A,ACCUM_EL
	STS UBRR0L,ACCUM_L
	STS UBRR0H,ACCUM_H
	STS UCSR0B,C0x00										;Отключаем прием и передачу
	LDI ACCUM_EL,low((1<<URSEL0)|(1<<UCSZ01)|(1<<UCSZ00))	;Async, 8 data bits
	LDD ACCUM_L,Y+_OS_DRV_UART_HWF__HEAP_PARITY				;Устанавливаем parity
	SWAP ACCUM_L
	OR ACCUM_EL,ACCUM_L
	LDD ACCUM_L,Y+_OS_DRV_UART_HWF__HEAP_STOP_BITS			;Устанавливаем кол-во стоп бит
	SWAP ACCUM_L
	LSR ACCUM_L
	OR ACCUM_EL,ACCUM_L
	STS UCSR0C,ACCUM_EL										;URSEL необходим для ATmega16, в остальных должен быть равен 0

	LDI ACCUM_L,OS_IRQ_UART0_RX								;Устанавливаем обработчик для прерывания UART RX
	LDI ACCUM_EL,low(_OS_DRV_UART_HWF__IRQ_RX_HANDLER)
	LDI ACCUM_EH,high(_OS_DRV_UART_HWF__IRQ_RX_HANDLER)
	MCALL OS_IRQ_SET
	LDI ACCUM_L,OS_IRQ_UART0_UDRE							;Устанавливаем обработчик для прерывания UART TX
	LDI ACCUM_EL,low(_OS_DRV_UART_HWF__IRQ_TX_HANDLER)
	LDI ACCUM_EH,high(_OS_DRV_UART_HWF__IRQ_TX_HANDLER)
	MCALL OS_IRQ_SET

	POP_T32
	POP_Z
	RET

;-----------------------------------------------------------
OS_DRV_UART_HWF__OPEN_NR:
;-----------------------------------------------------------
;Разрешаем аппаратный UART
;IN:Y-адрес HEAP
;MOD: ACCUM_L/H/EL/EH
;-----------------------------------------------------------
	LDD ACCUM_L,Y+_OS_DRV_UART_HWF__HEAP_FLAGS
	SBRC ACCUM_L,OS_DRV_UART_HWF_FL_OPEN
	RJMP OS_DRV_UART_HWF__OPEN_END

	MCALL OS_DISPATCHER_LOCK
	LDD ACCUM_L,Y+_OS_DRV_UART_HWF__HEAP_SENDBUFF_HEAPADDR+0x00
	LDD ACCUM_H,Y+_OS_DRV_UART_HWF__HEAP_SENDBUFF_HEAPADDR+0x01
	LDD ACCUM_EL,Y+_OS_DRV_UART_HWF__HEAP_RECVBUFF_HEAPADDR+0x00
	LDD ACCUM_EH,Y+_OS_DRV_UART_HWF__HEAP_RECVBUFF_HEAPADDR+0x01
	PUSH_Y
	MOVW YL,ACCUM_L
	MCALL OS_DRV_CBUFF16__INIT
	MOVW YL,ACCUM_EL
	MCALL OS_DRV_CBUFF16__INIT
	POP_Y

	LDS ACCUM_L,UCSR0B										;Разрешаем UART(прием)
	ORI ACCUM_L,(0<<UDRIE0)|(1<<TXEN0)|(1<<RXCIE0)|(1<<RXEN0)
	STS UCSR0B,ACCUM_L

	LDI ACCUM_L,(1<<OS_DRV_UART_HWF_FL_OPEN)
	STD Y+_OS_DRV_UART_HWF__HEAP_FLAGS,ACCUM_L
	STD Y+_OS_DRV_UART_HWF__HEAP_ERRCODE,C0x00
	MCALL OS_DISPATCHER_UNLOCK
OS_DRV_UART_HWF__OPEN_END:
	RET

;-----------------------------------------------------------
OS_DRV_UART_HWF__CLOSE_NR:
;-----------------------------------------------------------
;Запрещаем аппаратный UART
;IN:Y-адрес HEAP
;MOD: ACCUM_L
;-----------------------------------------------------------
	LDS ACCUM_H,SREG
	CLI
	LDS ACCUM_L,UCSR0B										;Запрещаем UART
	ANDI ACCUM_L,low(~((1<<UDRIE0)|(1<<TXEN0)|(1<<RXCIE0)|(1<<RXEN0)))
	STS UCSR0B,ACCUM_L
	STD Y+_OS_DRV_UART_HWF__HEAP_FLAGS,C0x00
	STS SREG,ACCUM_H
	RET

;-----------------------------------------------------------
OS_DRV_UART_HWF__TX_EVENT_NR:
;-----------------------------------------------------------
;Запускаем передачу, если не активна
;IN:Y-адрес HEAP
;MOD: ACCUM_L/H
;-----------------------------------------------------------
	LDS ACCUM_H,SREG
	CLI
	LDD ACCUM_L,Y+_OS_DRV_UART_HWF__HEAP_FLAGS
	SBRC ACCUM_L,OS_DRV_UART_HWF_FL_SENDING
	RJMP OS_DRV_UART_HWF__TX_EVENT_NR__END
	ORI ACCUM_L,(1<<OS_DRV_UART_HWF_FL_SENDING)
	STD Y+_OS_DRV_UART_HWF__HEAP_FLAGS,ACCUM_L
    LDS ACCUM_L,UCSR0B
    ORI ACCUM_L,(1<<UDRIE0)
    STS UCSR0B,ACCUM_L
OS_DRV_UART_HWF__TX_EVENT_NR__END:
	STS SREG,ACCUM_H
	RET


;-----------------------------------------------------------
_OS_DRV_UART_HWF__IRQ_TX_HANDLER:
;-----------------------------------------------------------
;Событие от шины (байт передан)
;IN:Y-адрес HEAP
;-----------------------------------------------------------
	PUSH_A32
	PUSH_X

	LDD ACCUM_L,Y+_OS_DRV_UART_HWF__HEAP_FLAGS				;Проверка на работающий драйвер
	SBRS ACCUM_L,OS_DRV_UART_HWF_FL_OPEN
	RJMP _OS_DRV_UART_HWF__IRQ_TX_HANDLER__END

	LDD ACCUM_L,Y+_OS_DRV_UART_HWF__HEAP_SENDBUFF_HEAPADDR+0x00
	LDD ACCUM_H,Y+_OS_DRV_UART_HWF__HEAP_SENDBUFF_HEAPADDR+0x01
	PUSH_Y
	MOVW YL,ACCUM_L
	MCALL OS_DRV_CBUFF16__GET_AVAILABLE
	CP ACCUM_L,C0x00
	CPC ACCUM_H,C0x00
	BREQ _OS_DRV_UART_HWF__IRQ_TX_HANDLER__EMPTY
	MCALL OS_DRV_CBUFF16__READ
	STS UDR0,ACCUM_L
	POP_Y
	RJMP _OS_DRV_UART_HWF__IRQ_TX_HANDLER__END
_OS_DRV_UART_HWF__IRQ_TX_HANDLER__EMPTY:
	POP_Y
	LDD ACCUM_L,Y+_OS_DRV_UART_HWF__HEAP_FLAGS
	ANDI ACCUM_L,low(~(1<<OS_DRV_UART_HWF_FL_SENDING))
	STD Y+_OS_DRV_UART_HWF__HEAP_FLAGS,ACCUM_L
    LDS ACCUM_L,UCSR0B
    ANDI ACCUM_L,low(~(1<<UDRIE0))
    STS UCSR0B,ACCUM_L

_OS_DRV_UART_HWF__IRQ_TX_HANDLER__END:
	POP_X
	POP_A32
	RET

;-----------------------------------------------------------
_OS_DRV_UART_HWF__IRQ_RX_HANDLER:
;-----------------------------------------------------------
;Событие от шины (байт принят)
;IN:Y-адрес HEAP
;-----------------------------------------------------------
	PUSH_A32
	PUSH_X

	LDS ACCUM_EL,UCSR0A
	LDS ACCUM_EH,UDR0										;Считываем принятый байт

	LDD ACCUM_L,Y+_OS_DRV_UART_HWF__HEAP_FLAGS				;Проверка на работающий драйвер
	SBRS ACCUM_L,OS_DRV_UART_HWF_FL_OPEN
	RJMP _OS_DRV_UART_HWF__IRQ_RX_HANDLER__END

	ANDI ACCUM_EL,(1<<DOR0)|(1<<UPE0)|(1<<FE0)
	BREQ _OS_DRV_UART_HWF__IRQ_RX_HANDLER__NO_ERROR
	LDI ACCUM_L,OSR_HARDWARE_FAIL
	STD Y+_OS_DRV_UART_HWF__HEAP_ERRCODE,ACCUM_L
	RJMP _OS_DRV_UART_HWF__IRQ_RX_HANDLER__END

_OS_DRV_UART_HWF__IRQ_RX_HANDLER__NO_ERROR:
	LDD ACCUM_L,Y+_OS_DRV_UART_HWF__HEAP_RECVBUFF_HEAPADDR+0x00
	LDD ACCUM_H,Y+_OS_DRV_UART_HWF__HEAP_RECVBUFF_HEAPADDR+0x01
	PUSH_Y
	MOVW YL,ACCUM_L
	MCALL OS_DRV_CBUFF16__GET_FREE
	CP ACCUM_L,C0x00
	CPC ACCUM_H,C0x00
	BREQ _OS_DRV_UART_HWF__IRQ_RX_HANDLER__OVERFLOW
	MOV ACCUM_L,ACCUM_EH
	MCALL OS_DRV_CBUFF16__WRITE
	POP_Y
	RJMP _OS_DRV_UART_HWF__IRQ_RX_HANDLER__END
_OS_DRV_UART_HWF__IRQ_RX_HANDLER__OVERFLOW:
	POP_Y
	LDI ACCUM_L,OSR_OVERFLOW
	STD Y+_OS_DRV_UART_HWF__HEAP_ERRCODE,ACCUM_L

_OS_DRV_UART_HWF__IRQ_RX_HANDLER__END:
	POP_X
	POP_A32
	RET
.ENDIF