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

.IFNDEF OS_STDIO_INIT_NR

.IF OS_FT_STDOUT == 0x01 || OS_FT_STDIN == 0x01
;-----------------------------------------------------------
OS_STDIO_INIT_NR:
;-----------------------------------------------------------
;Инициализация порта логирования
;-----------------------------------------------------------
.IF OS_FT_STDIN == 0x01
	LDI_Y _OS_STDIN_CBUFFER
	LDI TEMP_L,low(OS_STDIN_CBUFFER_SIZE)
	MCALL OS_CIRC_BUFFER_INIT_NR
.ENDIF
	RCALL OS_STDIO_INIT_SET_PORT_NR
.IFDEF STDIO_PORT_REGID
	.IF OS_FT_STDIN == 0x01
		RCALL OS_STDIO_RECV_MODE_NR
	.ELSE
		RCALL OS_STDIO_SEND_MODE_NR
	.ENDIF
.ELSE
	.IF OS_FT_STDIN == 0x01
		LDS TEMP_L,PCMSK0+(STDIN_PORTNUM-0x01)					;Разрешаем прерывание PCINT для STDIO пина (вызывается при инициализации ядра - прерывания выключены)
		ORI TEMP_L,(1<<STDIN_PINNUM)
		STS PCMSK0+(STDIN_PORTNUM-0x01),TEMP_L
	.ENDIF
.ENDIF
	RET

;-----------------------------------------------------------
;Процедура установки пина в HI
;-----------------------------------------------------------
OS_STDIO_INIT_SET_PORT_NR:
.IF OS_FT_STDIN == 0x01
	;TODO вынести в процедуру добавления порта для PCINT
	LDS TEMP_H,SREG
	CLI
	LDS TEMP_L,PCICR										;Разрешаем прерывание PCINT для STDIO порта
	.IFDEF STDIO_PORT_REGID
		ORI TEMP_L,(1<<(STDIO_PORTNUM-0x01))
	.ELSE
		ORI TEMP_L,(1<<(STDIN_PORTNUM-0x01))
	.ENDIF
	STS PCICR,TEMP_L
	STS _OS_PCINT_PCICR,TEMP_L
	;Вычислем 3-х байтную ячейку нашего прерывания(учитыаем отсутствие RESET IR) и записываем текущее состоянеи порта
	.IFDEF STDIO_PORT_REGID
		LDI_Y _OS_IR_VECTORS_TABLE+(C5_IR_PCINT0+STDIO_PORTNUM-1)*3-0x03
		LDI_Z PORTS_TABLE*2+STDIO_PORTNUM						;PINx (учитываем отсутствие порта A, PCINT0=PORTB)
	.ELSE
		LDI_Y _OS_IR_VECTORS_TABLE+(C5_IR_PCINT0+STDIN_PORTNUM-1)*3-0x03
		LDI_Z PORTS_TABLE*2+STDIN_PORTNUM
	.ENDIF

	LPM ZL,Z
	LD TEMP_L,Z												;Текущее значение
	ST Y,TEMP_L
	STS SREG,TEMP_H

	.IFDEF STDIO_PORT_REGID
		SBI STDIO_PORT_REGID,STDIO_PINNUM						;Высокий уровень
	.ENDIF
.ENDIF
.IFNDEF STDIO_PORT_REGID
	CBI STDIN_DDR_REGID,STDIN_PINNUM						;Вход
	SBI STDIN_PORT_REGID,STDIN_PINNUM						;С подтяжкой
	SBI STDOUT_DDR_REGID,STDOUT_PINNUM						;Выход
	SBI STDOUT_PORT_REGID,STDOUT_PINNUM						;Высокий уровень
.ENDIF
	RET

;-----------------------------------------------------------
;Процедура установки пина на вывод							;Не восстанавливает Z,TEMP_L
;-----------------------------------------------------------
.IFDEF STDIO_PORT_REGID
.IF OS_FT_STDOUT == 0x01
OS_STDIO_SEND_MODE_NR:
	PUSH TEMP_L
	.IF OS_FT_STDIN == 0x01
		LDS TEMP_L,SREG											;Запоминаем I флаг и блокирую прерывания
		CLI
		PUSH TEMP_L
		LDS TEMP_L,PCMSK0+(STDIO_PORTNUM-0x01)					;Запрещаем прерывание PCINT для STDIO пина
		ANDI TEMP_L,low(~(1<<STDIO_PINNUM))
		STS PCMSK0+(STDIO_PORTNUM-0x01),TEMP_L
		POP TEMP_L
		STS SREG,TEMP_L
	.ENDIF
	SBI STDIO_DDR_REGID,STDIO_PINNUM						;Режим выхода
	POP TEMP_L
	RET
.ENDIF
.ENDIF

;-----------------------------------------------------------
;Процедура установки пина на ввод							;Не восстанавливает Z,TEMP_L
;-----------------------------------------------------------
.IFDEF STDIO_PORT_REGID
.IF OS_FT_STDIN == 0x01
OS_STDIO_RECV_MODE_NR:
	PUSH TEMP_L
	.IF OS_FT_STDIN == 0x01
		LDS TEMP_L,SREG											;Запоминаем I флаг и блокирую прерывания
		CLI
		PUSH TEMP_L
		LDS TEMP_L,PCMSK0+(STDIO_PORTNUM-0x01)					;Разрешаем прерывание PCINT для STDIO пина
		ORI TEMP_L,(1<<STDIO_PINNUM)
		STS PCMSK0+(STDIO_PORTNUM-0x01),TEMP_L
		POP TEMP_L
		STS SREG,TEMP_L
	.ENDIF
	CBI STDIO_DDR_REGID,STDIO_PINNUM						;Режим ввода
	POP TEMP_L
	RET
.ENDIF
.ENDIF

.IF OS_FT_DEV_MODE == 0x00
OS_STDIO_1BIT_DELAY:
;-----------------------------------------------------------
;Пауза между битами
;IN: TEMP_H - значение паузы
;OUT: сбрасывает флаг C
;-----------------------------------------------------------
	SUBI TEMP_H,0x01
	BRNE PC-0x01
	RET
.ENDIF
.ENDIF
.ENDIF


