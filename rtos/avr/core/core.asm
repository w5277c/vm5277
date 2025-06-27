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
.include "mem/ram_fill.asm"
.IF OS_FT_STDOUT == 0x01
.include "stdio/out_init.asm"
.ENDIF
.IF OS_FT_WELCOME == 0x01 || OS_FT_DIAG == 0x01
.include "stdio/out_cstr.asm"
.ENDIF

;---CONSTANTS-----------------------------------------------
	.EQU	_OS_TIMER_TICK_PERIOD				= 0x14		;20, т.е. 0.000050*20=0.001=1мс

;---TEXT-CONSTANTS------------------------------------------
	.MESSAGE "######## LOGGING ENABLED"
	.MESSAGE "######## IO BAUDRATE:";,14400*CORE_FREQ
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
.IF OS_FT_TIMER1 == 0x01
	.EQU	_OS_UPTIME					= _OS_ROTO						;5B,LE-отсчет времени каждую в тиках(0.001с), ~34 года
	.EQU	_OS_TIMER1_CNTR				= _OS_UPTIME+0x05				;2B-16 бит счтетчик таймера1
	.EQU	_OS_TIMER_TICK_THRESHOLD	= _OS_TIMER1_CNTR+0x02			;1B-порог срабатывания таймера1(для отсчета 1 тика)
	.SET	_OS_ROTO					= _OS_TIMER_TICK_THRESHOLD+0x01
.ENDIF
.IF OS_FT_IR_TABLE == 0x01
	.EQU	_OS_IR_VECTORS_TABLE		= _OS_ROTO						;Таблица прерываний (OS_IR_QNT*3 для каждого прерывания, исключая RESET)
	.SET	_OS_ROTO					= OS_IR_QNT*3
.ENDIF

	.SET	SRAM_FREE					= SRAM_SIZE-_OS_ROTO-SRAM_START
.IF OS_FT_DRAM == 0x01
.IF SRAM_FREE > 1024
	.EQU	OS_DRAM_1B_TABLE_SIZE		= SRAM_FREE/4*3/8/2
	.EQU	OS_DRAM_2B_TABLE_SIZE		= SRAM_FREE/4*3/8/2
.ELSE
	.EQU	OS_DRAM_1B_TABLE_SIZE		= SRAM_FREE/4*3/8
	.EQU	OS_DRAM_2B_TABLE_SIZE		= 0
.ENDIF
	.SET	SRAM_FREE					= SRAM_FREE-(OS_DRAM_1B_TABLE_SIZE + OS_DRAM_2B_TABLE_SIZE)
.ENDIF

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

	LDS TEMP_H,MCUSR

	;WATCHDOG STOP
	WDR
	LDS TEMP_L,MCUSR
	ANDI TEMP_L,low(~(1<<WDRF))
	STS MCUSR,TEMP_L
	LDS TEMP_L,WDTCR
	ORI TEMP_L,(1<<WDCE)|(1<<WDE)
	STS WDTCR,TEMP_L
	LDI TEMP_L,(0<<WDE)
	STS WDTCR,TEMP_L

.IF OS_FT_STDOUT == 0x01
	MCALL OS_OUT_INIT
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
.ENDIF

	;Сбрасываем флаги причины сброса
	STS MCUSR,C0x00

.IF OS_FT_DIAG == 0x01
	;Заполнение всей памяти 0x52 значением (для диагностики)
	LDI ACCUM_L,0x52
	LDI_X SRAM_START
	LDI_Y SRAM_SIZE-0x0f
	MCALL RAM_FILL_NR
.ENDIF

.IF OS_FT_TIMER1 == 0x01
	;Сброс счетчика времени
	LDI ACCUM_L,0x00
	LDI_X _OS_UPTIME
	LDI_Y 0x0005
	MCALL RAM_FILL_NR
.ENDIF

OS_SOFTRESET:
.IF OS_FT_IR_TABLE == 0x01
	;Чистка таблицы прерываний
	LDI ACCUM_L,0xff
	LDI_X _OS_IR_VECTORS_TABLE
	LDI_Y OS_IR_QNT*3
	MCALL RAM_FILL_NR
.ENDIF

.IF OS_FT_TIMER1 == 0x01
	;Задаем счетчик основному таймеру
	STS _OS_TIMER1_CNTR+0x00,C0x00
	STS _OS_TIMER1_CNTR+0x01,C0x00
	LDI TEMP_L,_OS_TIMER_TICK_PERIOD
	STS _OS_TIMER_TICK_THRESHOLD,TEMP_L

	;Запуск таймеров
	_OS_TIMERS_RESTART
.ENDIF
	;Разрешаем прерывания и переходим на основную программу
	;SEI
	JMP MAIN

;-----------------------------------------------------------
_OS_IR_HANDLER:
;-----------------------------------------------------------
;Обработчик всех прерываний
;-----------------------------------------------------------
	RETI

.IF OS_FT_TIMER1 == 0x01
;-----------------------------------------------------------
_OS_TIMER1_HANDLER:
;-----------------------------------------------------------
;Обработчик прерывания основного таймера1
;-----------------------------------------------------------
	PUSH_T32
	PUSH_Z

	LDS TEMP_EL,_OS_TIMER1_CNTR+0x00
	LDS TEMP_EH,_OS_TIMER1_CNTR+0x01
	ADIW TEMP_EL,0x01
	STS _OS_TIMER1_CNTR+0x00,TEMP_EL
	STS _OS_TIMER1_CNTR+0x01,TEMP_EH

	LDS TEMP_L,_OS_TIMER_TICK_THRESHOLD
	CP TEMP_L,TEMP_EL
	BRNE _OS_TIMER1_HANDLER__END
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
	POP_T32
	RETI
.ENDIF

.IF OS_FT_TIMER2 == 0x01
;-----------------------------------------------------------
_OS_TIMER2_HANDLER:
;-----------------------------------------------------------
;Обработчик прерывания основного таймера2
;-----------------------------------------------------------
	RETI
.ENDIF
.ENDIF
