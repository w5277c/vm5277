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

.IFNDEF _OS_INIT

.IF OS_FT_DEV_MODE
	.EQU PROC__BLDR_INIT							= BOOT_512W_ADDR
	.EQU PROC__BLDR_UART_RECV_BYTE_NR				= BOOT_512W_ADDR+0x02
	.EQU PROC__BLDR_UART_SEND_BYTE_NR				= BOOT_512W_ADDR+0x03
	.EQU PROC__BLDR_WATCHDOG_STOP					= BOOT_512W_ADDR+0x04
.ENDIF

.IF OS_FT_IR_TABLE == 0x01 || OS_FT_DIAG == 0x01 || OS_FT_STDIN == 0x01
	.include "mem/ram_fill16.asm"
.ENDIF
.IF OS_FT_TIMER == 0x01 || OS_STAT_POOL_SIZE != 0x0000
	.include "mem/ram_clear16.asm"
.ENDIF

.IF OS_FT_STDOUT == 0x01 || OS_FT_STDIN == 0x01
	.include "stdio/stdio.asm"
.ENDIF
.IF OS_FT_STDIN == 0x01
	.include "stdio/stdin.asm"
	.include "mem/circ_buffer.asm"
.ENDIF

.IF OS_FT_WELCOME == 0x01 || OS_FT_DIAG == 0x01
	.include "stdio/out_cstr.asm"
.ENDIF
.IF OS_FT_ETRACE == 0x01
	.include "j8b/etrace_clear.asm"
.ENDIF
.IF OS_FT_PCINT == 0x01
	.include "core/pcint.asm"
.ENDIF

;---CONSTANTS-----------------------------------------------
	.EQU	_OS_TIMER_TICK_PERIOD				= 0x14		;20, т.е. 0.000050*20=0.001=1мс

;---TEXT-CONSTANTS------------------------------------------
.IF OS_FT_WELCOME == 0x01
CSTR_OSNAME:
	.db	"vm5277.avr v0.1",0x0d,0x0a,0x00
CSTR_OSMSG1:
	.db	0x0d,0x0a,"Powered by ",0x00
.ENDIF
.IF OS_FT_DIAG == 0x01
CSTR_OSMSG2:
	.db	" [",0x00,0x00
CSTR_MCUSR_NO:
	.db	"NO ",0x00
CSTR_MCUSR_WD:
	.db	"WD ",0x00
CSTR_MCUSR_BO:
	.db	"BO ",0x00
CSTR_MCUSR_EX:
	.db	"EX ",0x00
CSTR_MCUSR_PO:
	.db	"PO ",0x00
CSTR_MCUSR_RESET:
	.db	"reset]",0x0d,0x0a,0x00,0x00
.ENDIF

;---RAM-OFFSETS---------------------------------------------
	.SET	_OS_ROTO					= SRAM_START					;RAM OFFSETS TABLE OFFSET - временная переменная для отслеживания смещения в таблице
.IF OS_FT_TIMER == 0x01
	.EQU	_OS_UPTIME					= _OS_ROTO						;5B,LE-отсчет времени каждую в тиках(0.001с), ~34 года
	.EQU	_OS_TIMER_CNTR				= _OS_UPTIME+0x05				;2B-16 бит счтетчик таймера
	.EQU	_OS_TIMER_TICK_THRESHOLD	= _OS_TIMER_CNTR+0x02			;1B-порог срабатывания таймера(для отсчета 1 тика)
	.SET	_OS_ROTO					= _OS_TIMER_TICK_THRESHOLD+0x01
.ENDIF
.IF OS_FT_STDIN == 0x01
	.EQU	_OS_STDIN_CBUFFER			= _OS_ROTO						;Кольцевой буффер для STDIN
	.SET	_OS_ROTO					= _OS_STDIN_CBUFFER+OS_STDIN_CBUFFER_SIZE
.ENDIF
.IF OS_FT_PCINT
	.EQU	_OS_PCINT_PCICR				= _OS_ROTO						;PCICR – Pin Change Interrupt Control Register
	.SET	_OS_ROTO					= _OS_PCINT_PCICR+0x01
.ENDIF
.IF OS_FT_IR_TABLE == 0x01
	.EQU	_OS_IR_VECTORS_TABLE		= _OS_ROTO						;Таблица прерываний (OS_IR_QNT*3 для каждого прерывания, исключая RESET)
	.SET	_OS_ROTO					= _OS_IR_VECTORS_TABLE+OS_IR_QNT*3
.ENDIF

.IF OS_STAT_POOL_SIZE != 0x0000
	.EQU	_OS_STAT_POOL				= _OS_ROTO
	.SET	_OS_ROTO					= _OS_STAT_POOL + OS_STAT_POOL_SIZE
.ENDIF

.IF OS_FT_ETRACE == 0x01
	.EQU	_OS_ETRACE_BUFFER			= _OS_ROTO
	.SET	_OS_ROTO					= _OS_ETRACE_BUFFER + OS_ETRACE_BUFFER_SIZE
.ENDIF

;TODO Нужен динамический размир для блоков динамической памяти
.IF OS_FT_DRAM == 0x01
	.SET	OS_DRAM_BMASK_1B			= _OS_ROTO
	.SET	OS_DRAM_BMASK_2B			= OS_DRAM_BMASK_1B + OS_DRAM_BMASK_1B_SIZE
	.SET	OS_DRAM_BMASK_8B			= OS_DRAM_BMASK_2B + OS_DRAM_BMASK_2B_SIZE
	.SET	OS_DRAM_1B 					= OS_DRAM_BMASK_8B + OS_DRAM_BMASK_8B_SIZE
	.SET	OS_DRAM_2B					= OS_DRAM_1B + (OS_DRAM_BMASK_1B_SIZE*8)
	.SET	OS_DRAM_8B					= OS_DRAM_2B + (OS_DRAM_BMASK_2B_SIZE*8)
	.SET	_OS_ROTO 					= OS_DRAM_8B + (OS_DRAM_BMASK_8B_SIZE*8)
.ENDIF

	.SET	STACK_TOP					= _OS_ROTO


/* TODO оставил для других типов MCU
.IF SRAM_SIZE == 2048
	.EQU	OS_DRAM_BMASK_1B_SIZE		= 16
	.EQU	OS_DRAM_BMASK_2B_SIZE		= 16
	.EQU	OS_DRAM_BMASK_8B_SIZE		= 20
.IF SRAM_SIZE == 1024
	.EQU	OS_DRAM_BMASK_1B_SIZE		= 8
	.EQU	OS_DRAM_BMASK_2B_SIZE		= 8
	.EQU	OS_DRAM_BMASK_8B_SIZE		= 9
.IF SRAM_SIZE == 512
	.EQU	OS_DRAM_BMASK_1B_SIZE		= 4
	.EQU	OS_DRAM_BMASK_2B_SIZE		= 3
	.EQU	OS_DRAM_BMASK_8B_SIZE		= 4
.IF SRAM_SIZE == 256
	.EQU	OS_DRAM_BMASK_1B_SIZE		= 5
	.EQU	OS_DRAM_BMASK_2B_SIZE		= 5
	.EQU	OS_DRAM_BMASK_8B_SIZE		= 0
.ENDIF*/

_OS_INIT:
	CLI
	;Инициализация стека
.IFDEF SPH
	LDI TEMP_L,high(SRAM_START+SRAM_SIZE-1)
	STS SPH,TEMP_L
.ENDIF
	LDI TEMP_L,low(SRAM_START+SRAM_SIZE-1)
	STS SPL,TEMP_L
	EOR C0x00,C0x00
	LDI TEMP_L,0x01
	MOV C0x01,TEMP_L
	LDI TEMP_L,0xff
	MOV C0xff,TEMP_L

.IF OS_FT_DIAG == 0x01
	LDS FLAGS,MCUSR
.ENDIF
.IF OS_FT_DEV_MODE == 0x01
	MCALL PROC__BLDR_WATCHDOG_STOP
.ELSE
	;WATCHDOG STOP
	WDR
	LDS TEMP_L,MCUSR
	ANDI TEMP_L,low(~(1<<WDRF))
	STS MCUSR,TEMP_L
	LDS TEMP_L,WDTCR
	ORI TEMP_L,(1<<WDCE)|(1<<WDE)
	STS WDTCR,TEMP_L
	LDI TEMP_L,(0<<WDE)|(1<<WDIF)
	STS WDTCR,TEMP_L
.ENDIF

.IF OS_FT_DIAG == 0x01
	;Заполнение всей памяти 0x52 значением (для диагностики)
	LDI ACCUM_L,0x52
	LDI_Y SRAM_START
	LDI TEMP_L,low(SRAM_SIZE-0x0f)
	LDI TEMP_H,high(SRAM_SIZE-0x0f)
	MCALL OS_RAM_FILL16_NR
.ENDIF

.IF OS_FT_IR_TABLE == 0x01
	;Чистка таблицы прерываний
	LDI ACCUM_L,0xff
	LDI_Y _OS_IR_VECTORS_TABLE
	LDI TEMP_L,low(OS_IR_QNT*3)
	LDI TEMP_H,high(OS_IR_QNT*3)
	MCALL OS_RAM_FILL16_NR
.ENDIF

.IF OS_FT_STDOUT == 0x01 || OS_FT_STDIN == 0x01
	MCALL OS_STDIO_INIT_NR
.ENDIF

.IF OS_FT_WELCOME == 0x01
	LDI_Z CSTR_OSMSG1*2
	MCALL OS_OUT_CSTR
	LDI_Z CSTR_OSNAME*2
	MCALL OS_OUT_CSTR
.ENDIF
.IF OS_FT_DIAG == 0x01
	LDI_Z CSTR_OSMSG2*2
	MCALL OS_OUT_CSTR
	SBRS FLAGS,WDRF
	RJMP PC+0x03+_MCALL_SIZE
	LDI_Z CSTR_MCUSR_WD*2
	MCALL OS_OUT_CSTR
	SBRS FLAGS,BORF
	RJMP PC+0x03+_MCALL_SIZE
	LDI_Z CSTR_MCUSR_BO*2
	MCALL OS_OUT_CSTR
	SBRS FLAGS,EXTRF
	RJMP PC+0x03+_MCALL_SIZE
	LDI_Z CSTR_MCUSR_EX*2
	MCALL OS_OUT_CSTR
	SBRS FLAGS,PORF
	RJMP PC+0x03+_MCALL_SIZE
	LDI_Z CSTR_MCUSR_PO*2
	MCALL OS_OUT_CSTR
	MOV TEMP_L,FLAGS
	ANDI TEMP_L,(1<<WDRF)|(1<<BORF)|(1<<EXTRF)|(1<<PORF)
	BRNE PC+0x03+_MCALL_SIZE
	LDI_Z CSTR_MCUSR_NO*2
	MCALL OS_OUT_CSTR
	LDI_Z CSTR_MCUSR_RESET*2
	MCALL OS_OUT_CSTR

	;Сбрасываем флаги причины сброса
	STS MCUSR,C0x00
.ENDIF

.IF OS_STAT_POOL_SIZE != 0x0000
	;Очистка статического блока верхнего уровня
	LDI_Y _OS_STAT_POOL
	LDI TEMP_L,low(OS_STAT_POOL_SIZE)
	LDI TEMP_H,high(OS_STAT_POOL_SIZE)
	MCALL OS_RAM_CLEAR16_NR
.ENDIF

.IF OS_FT_ETRACE == 0x01
	MCALL J8BPROC_ETRACE_CLEAR
.ENDIF

.IF OS_FT_TIMER == 0x01
	;Сброс счетчика времени
	LDI_Y _OS_UPTIME
	LDI TEMP_L,0x05
	LDI TEMP_H,0x00
	MCALL OS_RAM_CLEAR16_NR

	;Задаем счетчик основному таймеру
	STS _OS_TIMER_CNTR+0x00,C0x00
	STS _OS_TIMER_CNTR+0x01,C0x00
	LDI TEMP_L,_OS_TIMER_TICK_PERIOD
	STS _OS_TIMER_TICK_THRESHOLD,TEMP_L

	;Запуск таймеров
	_OS_TIMERS_RESTART
.ENDIF
	;Разрешаем прерывания и переходим на основную программу
.IF OS_FT_IR_TABLE
	SEI
.ENDIF
	JMP MAIN

.IF OS_FT_IR_TABLE
;-----------------------------------------------------------
_OS_IR_HANDLER:
;-----------------------------------------------------------
;Обработчик всех прерываний
;-----------------------------------------------------------
	CLI														;Запрещаем прерывания глобально
	;Получаем адрес по которому определяем тип прерывания
	POP ATOM_L
	POP ATOM_L

	PUSH TEMP_L
	LDS TEMP_L,SREG
	PUSH TEMP_L

	MOV TEMP_L,ATOM_L
	SUBI TEMP_L,_MCALL_SIZE*0x02
	LSR TEMP_L
.IF _MCALL_SIZE == 0x02
	LSR TEMP_L												;Делим еще на 2, если переходы размерностью в 2 слова (JMP/CALL)
.ENDIF

	;TODO

	POP TEMP_L
	STS SREG,TEMP_L
	POP TEMP_L
	RETI
.ENDIF

.IF OS_FT_TIMER == 0x01
;-----------------------------------------------------------
_OS_TIMER1_HANDLER:
;-----------------------------------------------------------
;Обработчик прерывания основного таймера1
;-----------------------------------------------------------
	PUSH_T16
	PUSH_Z

	LDS TEMP_L,_OS_TIMER_CNTR+0x00
	LDS TEMP_H,_OS_TIMER_CNTR+0x01
	ADIW TEMP_L,0x01
	STS _OS_TIMER_CNTR+0x00,TEMP_L
	STS _OS_TIMER_CNTR+0x01,TEMP_H

	LDS TEMP_L,_OS_TIMER_TICK_THRESHOLD
	CP TEMP_L,TEMP_EL
	BRNE _OS_TIMER_HANDLER__END
	SUBI TEMP_L,(0x100-_OS_TIMER_TICK_PERIOD)
	STS _OS_TIMER_TICK_THRESHOLD,TEMP_L

	LDI_Z _OS_UPTIME
	LD TEMP_L,Z
	ADD TEMP_L,C0x01
	ST Z+,TEMP_L
	LD TEMP_L,Z
	ADC TEMP_L,C0x00
	ST Z+,TEMP_L
	LD TEMP_L,Z
	ADC TEMP_L,C0x00
	ST Z+,TEMP_L
	LD TEMP_L,Z
	ADC TEMP_L,C0x00
	ST Z+,TEMP_L
	LD TEMP_L,Z
	ADC TEMP_L,C0x00
	ST Z,TEMP_L

_OS_TIMER1_HANDLER__END:
	POP_Z
	POP_T16
	RETI
.ENDIF

.ENDIF
