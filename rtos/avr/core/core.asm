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

.IFDEF BOOT_512W_ADDR
	.EQU PROC__BLDR_INIT							= BOOT_512W_ADDR
.ENDIF
.IF OS_FT_BLDR_API_REUSE
	.EQU PROC__BLDR_UART_RECV_BYTE_NR				= BOOT_512W_ADDR+0x02
	.EQU PROC__BLDR_UART_SEND_BYTE_NR				= BOOT_512W_ADDR+0x03
	.EQU PROC__BLDR_WATCHDOG_STOP					= BOOT_512W_ADDR+0x04
	.EQU _OS_BLDR_UID_DATA							= BOOT_512W_ADDR+0x09
.ENDIF

.include "mem/ram_fill16.asm"
.IF OS_FT_TIMER == 0x01 || OS_STAT_POOL_SIZE != 0x0000 || OS_FT_DRAM == 0x01 || OS_FT_IR_TABLE == 0x01
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
.IF OS_FT_IRQ_PCINT_BLOCK == 0x01
	.include "core/pcint.asm"
.ENDIF

.IF OS_FT_DIAG == 0x01
	.include "stdio/out_stack.asm"
	.include "stdio/out_hex16.asm"
.ENDIF

;---TEXT-CONSTANTS------------------------------------------
.IF OS_FT_WELCOME == 0x01
CSTR_OSNAME:
	.db	"vm5277.avr v0.2",0x0a,0x00,0x00
CSTR_OSMSG1:
	.db	0x0a,"Powered by ",0x00,0x00
.ENDIF
.IF OS_FT_DIAG == 0x01
CSTR_OSMSG2:
	.db	"[",0x00
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
	.db	"reset]",0x0a,0x00
CSTR_INVALID_IRQ:
	.db	0x0a,"!Invalid IRQ!",0x0a,0x00
CSTR_SP:
	.db	"SP:",0x00
.ENDIF

.IF OS_FT_STIMER==0x01
	.IFNDEF OS_STIMER_QNT
		.ERROR "OS_STIMER_QNT is undefined"
	.ENDIF
 	.IF OS_STIMER_QNT==0
 		.ERROR "OS_FT_STIMER enabled, but OS_STIMER_QNT is 0"
 	.ENDIF
.ENDIF

;---RAM-OFFSETS---------------------------------------------
	.SET	_OS_ROTO					= SRAM_START		;RAM OFFSETS TABLE OFFSET - временная переменная для отслеживания смещения в таблице
.IF OS_FT_TIMER == 0x01
	.EQU	_OS_UPTIME					= _OS_ROTO			;5B,LE-отсчет времени каждую в тиках(0.001с), ~34 года
	.EQU	_OS_TIMER_CNTR				= _OS_UPTIME+0x05	;1B-счтетчик таймера
	.SET	_OS_ROTO					= _OS_TIMER_CNTR+0x01
.ENDIF
.IF OS_FT_PRND == 0x01
	.EQU	_OS_PRND_CNTR				= _OS_ROTO
	.SET 	_OS_ROTO					= _OS_PRND_CNTR+0x02
.ENDIF
.IF OS_FT_MULTITHREADING == 0x01
	.EQU	OS_FIRSTTHREAD_ADDR			= _OS_ROTO						;2B-адрес HEAP первого потока
	.EQU	_OS_DISPATCHER_TIMER_CNTR	= OS_FIRSTTHREAD_ADDR+0x02		;1B-счетчик мс для вызова диспетчера
	.SET	_OS_ROTO					= _OS_DISPATCHER_TIMER_CNTR+0x01
.IF OS_FT_CPU_LOAD == 0x01
	.EQU	_OS_CPU_LOAD_CNTR			= _OS_ROTO						;Счетчик таймера отмерающий 100 итераций
	.EQU	_OS_CPU_LOAD_TMP			= _OS_CPU_LOAD_CNTR+0x01		;Сумматор активностей
	.EQU	OS_CPU_LOAD					= _OS_CPU_LOAD_TMP+0x01			;Итоговое значение в %
	.SET	_OS_ROTO					= OS_CPU_LOAD+0x01
.ENDIF
.ENDIF
.IF OS_FT_STDIN == 0x01
	.EQU	_OS_STDIN_CBUFFER			= _OS_ROTO			;Кольцевой буффер для STDIN
	.SET	_OS_ROTO					= _OS_STDIN_CBUFFER+OS_STDIN_CBUFFER_SIZE
.ENDIF
.IF OS_FT_IRQ_PCINT_BLOCK==0x01
	.EQU	_OS_PCINT_PCICR				= _OS_ROTO			;PCICR – (старший бит определяет выполнение кода в обработчике PCINT)
	.EQU	_OS_PCINT_STATES			= _OS_PCINT_PCICR+1	;Послденее состояние портов
	.SET	_OS_ROTO					= _OS_PCINT_STATES+OS_PCINT_PORTS_QNT
.ENDIF
.IF _OS_IRQ_RAM_TABLE_SIZE > 0
	.EQU	_OS_IRQ_RAM_TABLE			= _OS_ROTO			;RAM таблица прерываний
	.SET	_OS_ROTO					= _OS_IRQ_RAM_TABLE+_OS_IRQ_RAM_TABLE_SIZE
.ENDIF
.IF OS_FT_STIMER==0x01
	.EQU	_OS_STIMER_TABLE			= _OS_ROTO			;Таблица программных таймеров (OS_STIMER_QNT*9)
	.SET	_OS_ROTO					= _OS_STIMER_TABLE+OS_STIMER_QNT*9
.ENDIF
.IF OS_STAT_POOL_SIZE != 0x0000
	.EQU	_OS_STAT_POOL				= _OS_ROTO
	.SET	_OS_ROTO					= _OS_STAT_POOL + OS_STAT_POOL_SIZE
.ENDIF

.IF OS_FT_ETRACE == 0x01
	.EQU	_OS_ETRACE_BUFFER			= _OS_ROTO
	.SET	_OS_ROTO					= _OS_ETRACE_BUFFER + OS_ETRACE_BUFFER_SIZE
.ENDIF

.IF OS_FT_DIAG == 0x01
	.EQU	OS_DIAG_BITS0				= _OS_ROTO;			;1B
;Не реально, так как обычно RESUME будет идти сразу после SUSPEND, а при SUSPEND стек использовался задачей.
;Возможно только если не было задач для возобновления после приостановления и возникла новая задача, но это редко и сложно отследить
;	.EQU	OS_DIAG_BITS0__DIRTY_STACK	= 0x00				;Грязный стек - при возобновлении задачи стек оказался чем-то занят
;															;вероятно обработчик прерывания(драйвер) разрешил прерывания не заблокировав диспетчер)
	;...
	.SET	_OS_ROTO					= OS_DIAG_BITS0+0x01;
.ENDIF


.IF OS_FT_DIAG == 0x01 && EXTERNAL_ASM_TOOL == 0x00
	.IF -1==OS_STACK_SIZE
		.MESSAGE "Stack size:", SRAM_START + SRAM_SIZE - 0x01 - _OS_ROTO	; 0x01-STACK BORDER
	.ELSE
		.MESSAGE "Stack size:", OS_STACK_SIZE
	.ENDIF

	.IF _OS_IRQ_RAM_TABLE_SIZE > 0
		.MESSAGE "IRQ RAM address:", _OS_IRQ_RAM_TABLE
		.MESSAGE "IRQ RAM table size:", _OS_IRQ_RAM_TABLE_SIZE
	.ENDIF

	.IF OS_FT_IRQ_PCINT_BLOCK==0x01
		.MESSAGE "PCINT states table size:", _OS_PCINT_STATES+OS_PCINT_PORTS_QNT
	.ENDIF
.ENDIF



.IF OS_FT_DRAM == 0x01
	;Вычисляю доступную память
	.IF -1==OS_STACK_SIZE
		.EQU _DRAM_AVAILABLE_SIZE = 0;
	.ELSE
		.EQU _DRAM_AVAILABLE_SIZE = SRAM_START + SRAM_SIZE - OS_STACK_SIZE - 0x01 - _OS_ROTO	;0x01-STACK BORDER
	.ENDIF
	.IF OS_FT_DIAG == 0x01 && EXTERNAL_ASM_TOOL == 0x00
		.MESSAGE "Available RAM for DRAM:", _DRAM_AVAILABLE_SIZE
	.ENDIF
	.IF _DRAM_AVAILABLE_SIZE > 0
		.EQU OS_DRAM_BMASK_SIZE = _DRAM_AVAILABLE_SIZE / 65
		.IF OS_FT_DIAG == 0x01 && EXTERNAL_ASM_TOOL == 0x00
			.MESSAGE "DRAM bitmask size:", OS_DRAM_BMASK_SIZE
		.ENDIF
		.EQU _DRAM_SIZE = OS_DRAM_BMASK_SIZE * 65
		.IF OS_FT_DIAG == 0x01 && EXTERNAL_ASM_TOOL == 0x00
			.MESSAGE "DRAM size:", _DRAM_SIZE
		.ENDIF

		.SET OS_DRAM_BMASK_ADDR = _OS_ROTO
		.IF OS_FT_DIAG == 0x01 && EXTERNAL_ASM_TOOL == 0x00
			.MESSAGE "DRAM bitmask addr:", OS_DRAM_BMASK_ADDR
		.ENDIF
		.SET OS_DRAM_ADDR = OS_DRAM_BMASK_ADDR + OS_DRAM_BMASK_SIZE
		.IF OS_FT_DIAG == 0x01 && EXTERNAL_ASM_TOOL == 0x00
			.MESSAGE "DRAM block addr:", OS_DRAM_ADDR
		.ENDIF
		.SET _OS_ROTO = OS_DRAM_ADDR + (OS_DRAM_BMASK_SIZE * 64)
		.IF OS_FT_DIAG == 0x01 && EXTERNAL_ASM_TOOL == 0x00
			.MESSAGE "DRAM end addr:", _OS_ROTO	
		.ENDIF
	.ELSE
		.WARNING "No available RAM for DRAM"
	.ENDIF
.ELSE
.IF OS_FT_DIAG == 0x01
	.EQU _RAM_USAGE = OS_STACK_SIZE + 0x01 + _OS_ROTO - SRAM_START
	.MESSAGE "RAM usage:", _RAM_USAGE
.ENDIF
.ENDIF

.IF OS_FT_MULTITHREADING == 0x01
	.include "core/dispatcher.asm"
	.IF OS_FT_DIAG == 0x01
		.MESSAGE "Multithreading activated"
	.ENDIF
.ENDIF

	.SET	STACK_TOP					= SRAM_START+SRAM_SIZE-OS_STACK_SIZE
	.SET	STACK_BORDER				= STACK_TOP-1		;1B-байт изменение значения которого указывает на переполнение стека

_OS_INIT:
	CLI
.IF OS_FT_DIAG == 0x01
	LDS _SPL,SPL
.IFDEF SPH
	LDS _SPH,SPH
.ENDIF
.ENDIF
	;Инициализация стека
.IFDEF SPH
	LDI TEMP_L,high(SRAM_START+SRAM_SIZE-1)
	STS SPH,TEMP_L
.ENDIF
	LDI TEMP_L,low(SRAM_START+SRAM_SIZE-1)
	STS SPL,TEMP_L
	EOR C0x00,C0x00
	MOV DISPATCHER_CNTR,C0x00
	LDI TEMP_L,0x01
	MOV C0x01,TEMP_L
	LDI TEMP_L,0xff
	MOV C0xff,TEMP_L
	LDI TEMP_L,0x02
	MOV C0x02,TEMP_L

.IF OS_FT_STDOUT==0x01 || OS_FT_STDIN==0x01
	MCALL OS_STDIO_INIT_NR
.ENDIF

.IF OS_FT_DIAG == 0x01
	LDS FLAGS,MCUSR
.ENDIF
.IF OS_FT_BLDR_API_REUSE == 0x01
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

.IF OS_FT_WELCOME == 0x01
	LDI_A16 CSTR_OSMSG1*2
	MCALL OS_OUT_CSTR
	LDI_A16 CSTR_OSNAME*2
	MCALL OS_OUT_CSTR
.ENDIF
.IF OS_FT_DIAG == 0x01
	LDI_A16 CSTR_SP*2
	MCALL OS_OUT_CSTR
	MOV ACCUM_L,_SPL
.IFDEF SPH
	MOV ACCUM_H,_SPH
.ELSE
	LDI ACCUM_H,0x00
.ENDIF
	MCALL OS_OUT_HEX16
	LDI_A16 CSTR_OSMSG2*2
	MCALL OS_OUT_CSTR
	SBRS FLAGS,WDRF
	RJMP PC+0x03+_MCALL_SIZE
	LDI_A16 CSTR_MCUSR_WD*2
	MCALL OS_OUT_CSTR
	SBRS FLAGS,BORF
	RJMP PC+0x03+_MCALL_SIZE
	LDI_A16 CSTR_MCUSR_BO*2
	MCALL OS_OUT_CSTR
	SBRS FLAGS,EXTRF
	RJMP PC+0x03+_MCALL_SIZE
	LDI_A16 CSTR_MCUSR_EX*2
	MCALL OS_OUT_CSTR
	SBRS FLAGS,PORF
	RJMP PC+0x03+_MCALL_SIZE
	LDI_A16 CSTR_MCUSR_PO*2
	MCALL OS_OUT_CSTR
	MOV TEMP_L,FLAGS
	ANDI TEMP_L,(1<<WDRF)|(1<<BORF)|(1<<EXTRF)|(1<<PORF)
	BRNE PC+0x03+_MCALL_SIZE
	LDI_A16 CSTR_MCUSR_NO*2
	MCALL OS_OUT_CSTR
	LDI_A16 CSTR_MCUSR_RESET*2
	MCALL OS_OUT_CSTR

	;Сбрасываем флаги причины сброса
	STS MCUSR,C0x00

	MCALL OS_OUT_STACK_NR
.ENDIF

	;Устаналиваем значение для определения переполнения стека
	LDI TEMP_L,STACK_GUARD_VALUE
	STS STACK_BORDER,TEMP_L

.IF OS_FT_DIAG == 0x01 || -1==OS_STACK_SIZE
	;Заполнение всей памяти STACK_GUARD_VALUE значением (для диагностики)
	LDI ACCUM_L,STACK_GUARD_VALUE
	LDI_X SRAM_START
	LDI TEMP_L,low(SRAM_SIZE-0x0f) ;TODO почему 0x0f?
	LDI TEMP_H,high(SRAM_SIZE-0x0f)
	MCALL OS_RAM_FILL16_NR
.ELSE
	;Заполнение всей памяти STACK_GUARD_VALUE значением (для определения глубины использования стека)
	LDI ACCUM_L,STACK_GUARD_VALUE
	LDI_X SRAM_START+SRAM_SIZE-OS_STACK_SIZE
	LDI TEMP_L,low(OS_STACK_SIZE-0x02)
	LDI TEMP_H,high(OS_STACK_SIZE-0x02)
	MCALL OS_RAM_FILL16_NR
.ENDIF

.IF _OS_IRQ_RAM_TABLE_SIZE > 0
	;Чистка RAM таблицы прерываний
	LDI_X _OS_IRQ_RAM_TABLE
	LDI TEMP_L,low(_OS_IRQ_RAM_TABLE_SIZE)
	LDI TEMP_H,high(_OS_IRQ_RAM_TABLE_SIZE)
	MCALL OS_RAM_CLEAR16_NR									;0x0000 в адрес обработчика = обработчик отсутствует
.ENDIF

.IF OS_FT_STIMER==0x01
	;Чистка таблицы прерываний
	LDI_X _OS_STIMER_TABLE
	LDI TEMP_L,low(OS_STIMER_QNT*8)
	LDI TEMP_H,high(OS_STIMER_QNT*8)
	MCALL OS_RAM_CLEAR16_NR									;0x0000 в адрес обработчика = обработчик отсутствует
.ENDIF

.IF OS_FT_IRQ_PCINT_BLOCK == 1
	;Устанавливаем PCINT состояния
	LDI ACCUM_L,0xff
	LDI_X _OS_PCINT_STATES
	LDI TEMP_L,low(OS_PCINT_PORTS_QNT)
	LDI TEMP_H,high(OS_PCINT_PORTS_QNT)
	MCALL OS_RAM_FILL16_NR
.ENDIF

.IF OS_STAT_POOL_SIZE != 0x0000
	;Очистка статического блока верхнего уровня
	LDI_X _OS_STAT_POOL
	LDI TEMP_L,low(OS_STAT_POOL_SIZE)
	LDI TEMP_H,high(OS_STAT_POOL_SIZE)
	MCALL OS_RAM_CLEAR16_NR
.ENDIF

.IF OS_FT_MULTITHREADING == 0x01
	;Устанавливаем ID первой задачи
;	MOV PID_L,C0x00 - должен быть задан верхним уровнем
;	MOV PID_H,C0x00
	STS OS_FIRSTTHREAD_ADDR+0x00,C0x00
	STS OS_FIRSTTHREAD_ADDR+0x01,C0x00

	.IF OS_FT_CPU_LOAD == 0x01
		;Инициализируем данные для оценки загрузки CPU
		STS OS_CPU_LOAD_CNTR,C0x00
		STS OS_CPU_LOAD_TMP,C0x00
		STS OS_CPU_LOAD,C0x00
	.ENDIF
	;Устанавливаем начальное значение для счетчика таймера диспетчера
	STS _OS_DISPATCHER_TIMER_CNTR,C0x00
.ENDIF

	;Подготавливаем битовую таблицу для DRAM
.IF OS_FT_DRAM == 0x01
	.IF OS_DRAM_BMASK_SIZE > 0
		LDI_X OS_DRAM_BMASK_ADDR
		LDI TEMP_H,high(OS_DRAM_BMASK_SIZE)
		LDI TEMP_L,low(OS_DRAM_BMASK_SIZE)
		MCALL OS_RAM_CLEAR16_NR
	.ENDIF
.ENDIF

.IF OS_FT_ETRACE == 0x01
	MCALL J8BPROC_ETRACE_CLEAR
.ENDIF

.IF OS_FT_TIMER == 0x01
	;Сброс счетчика времени
	LDI_X _OS_UPTIME
	LDI TEMP_L,0x05
	LDI TEMP_H,0x00
	MCALL OS_RAM_CLEAR16_NR

	;Задаем счетчик основному таймеру
	LDI TEMP_L,_OS_TIMER_1MS_PERIOD
	STS _OS_TIMER_CNTR,TEMP_L

	;Запуск таймеров
	_OS_SYSTIMER_INIT_NR
.ENDIF

	;Инициализируем начальное значение псевдослучайного числа
.IF OS_FT_PRND == 0x01
	LDS TEMP_L,TCNT0										;Значение счетчика таймера
	.IF PINA != 0xFF
		LDS TEMP_H,PINA										;Значение порта A
		EOR TEMP_L,TEMP_H
	.ENDIF
	.IF PINB != 0xFF
		LDS TEMP_H,PINB										;Значение порта B
		EOR TEMP_L,TEMP_H
	.ENDIF
	.IF PINC != 0xFF
		LDS TEMP_H,PINC										;Значение порта C
		EOR TEMP_L,TEMP_H
	.ENDIF
	.IF PIND != 0xFF
		LDS TEMP_H,PIND										;Значение порта D
		EOR TEMP_L,TEMP_H
	.ENDIF
	.IFDEF OSCCAL
		LDS TEMP_H,OSCCAL									;Значение калибровочного байта
		EOR TEMP_L,TEMP_H
	.ENDIF
	STS _OS_PRND_CNTR+0x01,TEMP_L
.ENDIF

	;Разрешаем прерывания и переходим на основную программу
.IF _OS_IRQ_RAM_TABLE_SIZE > 0
	SEI
.ENDIF
	JMP MAIN

;-----------------------------------------------------------
_OS_INVALID_IRQ_HANDLER:
;-----------------------------------------------------------
;Обработчик не поддерживаемого прерывания (случайный сбой)
;-----------------------------------------------------------
.IF OS_FT_DIAG == 0x01
	LDI_A16 CSTR_INVALID_IRQ*2
	MCALL OS_OUT_CSTR
.ENDIF
RJMP PC

.IF _OS_IRQ_RAM_TABLE_SIZE>0x00
;-----------------------------------------------------------
_OS_IRQ_HANDLER:
;-----------------------------------------------------------
;Общий обработчик прерываний
;-----------------------------------------------------------
;TODO нужно запретить прерывания INT но разрешить глобальные!
;TODO похоже нужно делать разные обработчики прерываний, так как есть необходимость разрешать глобальные прерывания,
;потому что многие из них будут обработаны на верхнем уровне.
;Но при этом нужно блокировать повторное срабатывание - либо вводить дополнительне флаги, либо реализовать отдельные обработчики.
;Похоже да, каждый тип блока должен иметь свой обработчик

	;Получаем адрес по которому определяем тип прерывания
	POP ATOM_L
	POP ATOM_L

	LDS ATOM_EH,SREG
	PUSH ATOM_EH
.IF _MCALL_SIZE == 0x02
	LSL ATOM_L												;Умножаем на 2, если переходы размерностью в 2 слова
.ENDIF

	PUSH_Y
	PUSH_Z
	LDI ZH,0x00
	MOV ZL,ATOM_L											;Загружаем следующий байт из Flash после команды перехода
	LDI YH,0x00
	LPM YL,Z+
	RCALL _OS_IRQ_HANDLER__SUBROUTINE

	POP_Z
	POP_Y
	POP ATOM_EH
	STS SREG,ATOM_EH
	RETI

_OS_IRQ_HANDLER__SUBROUTINE:
	SUBI YL,low(-_OS_IRQ_RAM_TABLE)
	SBCI YH,high(-_OS_IRQ_RAM_TABLE)
	LDD ATOM_L,Y+_OS_IRQ_RAM_TABLE__OFFSET_HANDLER_ADDR+0x00
	LDD ATOM_H,Y+_OS_IRQ_RAM_TABLE__OFFSET_HANDLER_ADDR+0x01
/*PUSH_A16
PUSH_T16
PUSH_Z
LDI ACCUM_L,'('
MCALL OS_OUT_CHAR
LDI TEMP_H,0x00
LDI TEMP_L,0x08
LDI_Z 0x0100
MCALL OS_OUT_RAMBYTES_NR
LDI ACCUM_L,')'
MCALL OS_OUT_CHAR
POP_Z
POP_T16
POP_A16
*/
	CP ATOM_L,C0x00
	CPC ATOM_H,C0x00
	BREQ _OS_IRQ_HANDLER__SKIP

	PUSH J8B_ATOM											;Верхний уровень может использовать ACC и J8B_ATOM - остальные регистры он должне сам восстановить
	PUSH_A32
	LDD ACCUM_L,Y+_OS_IRQ_RAM_TABLE__OFFSET_RES_ID
	LDD ZL,Y+_OS_IRQ_RAM_TABLE__OFFSET_HEAP_ADDR+0x00
	LDD ZH,Y+_OS_IRQ_RAM_TABLE__OFFSET_HEAP_ADDR+0x01
	CP ZL,C0xff
	CPC ZH,C0xff
	BREQ _OS_IRQ_HANDLER__SUBROUTINE__SKIP_STACKHEADER
	PUSH ZL													;Сохраняем в стек Z для совместимости с логикой j8b
	PUSH ZH
	PUSH ACCUM_L											;Помещаем в стек аргумент
_OS_IRQ_HANDLER__SUBROUTINE__SKIP_STACKHEADER:

	LDI ACCUM_EL,low(_OS_IRQ_HANDLER__RETPOINT)				;Сохраняем в стек адрес возврата
	PUSH ACCUM_EL
	LDI ACCUM_EL,high(_OS_IRQ_HANDLER__RETPOINT)
	PUSH ACCUM_EL
	PUSH ATOM_L												;Сохраняем в стек адрес для перехода
	PUSH ATOM_H
	RET
_OS_IRQ_HANDLER__RETPOINT:									;Аргумент и Z восстанаавливает процедура выхода из метода
	CLI														;Важно запрещать глобальные прерывания поскольку дальше в коде могут быть
	POP_A32													;разрешены INT, PCINT и прочие - при этом существенно раньше, чем произойдет освобождение стека и RETI
	POP J8B_ATOM
_OS_IRQ_HANDLER__SKIP:
	RET
.ENDIF

.IF OS_FT_TIMER == 0x01
;-----------------------------------------------------------
_OS_SYSTIMER_HANDLER:
;-----------------------------------------------------------
;Обработчик прерывания основного таймера1
;-----------------------------------------------------------
	PUSH TEMP_L
	LDS TEMP_L,SREG
	PUSH TEMP_L

.IF OS_FT_STIMER==0x01										;Блок обработки программных таймеров
	PUSH_Z
	PUSH_Y
	PUSH_A32

	LDI_Y _OS_STIMER_TABLE
	LDI ACCUM_EL,OS_STIMER_QNT								;Перебираем все записи таймеров
_OS_SYSTIMER_HANDLER__STIMER_LOOP:
	LDD ZL,Y+_OS_STIMER_TABLE__OFFSET_HEAP_ADDR+0x00		;Загружаем адрес кучи в Y
	LDD ZH,Y+_OS_STIMER_TABLE__OFFSET_HEAP_ADDR+0x01
	CP ZL,C0x00												;0x0000 - таймер выключен
	CPC ZH,C0x00
	BREQ _OS_SYSTIMER_HANDLER__STIMER_NEXT

	LDD ACCUM_L,Y+_OS_STIMER_TABLE__OFFSET_CNTR+0x00
	LDD ACCUM_H,Y+_OS_STIMER_TABLE__OFFSET_CNTR+0x01
.IF	CORE_FREQ<=8000
	SUBI ACCUM_L,0x02										;Выполняем декремент (для медленных CPU попадаем сюда в 2 раза реже, компенсируем)
.ELSE
	SUBI ACCUM_L,0x01
.ENDIF
	SBCI ACCUM_H,0x00
	STD Y+_OS_STIMER_TABLE__OFFSET_CNTR+0x00,ACCUM_L
	STD Y+_OS_STIMER_TABLE__OFFSET_CNTR+0x01,ACCUM_H
	CP ACCUM_L,C0x00
	CPC ACCUM_H,C0x00
	BRCS PC+0x02											;Пропускаем переход если было переполнение (проверка для шага -2)
	BRNE _OS_SYSTIMER_HANDLER__STIMER_NEXT					;Переходим к следующему если не 0

	;Сработка таймера
	STD Y+_OS_STIMER_TABLE__OFFSET_HEAP_ADDR+0x00,C0x00		;Блокируем таймер исключая повторное срабатывание
	STD Y+_OS_STIMER_TABLE__OFFSET_HEAP_ADDR+0x01,C0x00
	PUSH ACCUM_EL
	PUSH_Y
	PUSH ZL
	PUSH ZH
	LDD ACCUM_L,Y+_OS_STIMER_TABLE__OFFSET_RES_ID
	PUSH ACCUM_L
	LDI ACCUM_L,low(_OS_SYSTIMER_HANDLER__STIMER_RETPOINT)
	PUSH ACCUM_L
	LDI ACCUM_L,high(_OS_SYSTIMER_HANDLER__STIMER_RETPOINT)
	PUSH ACCUM_L
	LDD ACCUM_L,Y+_OS_STIMER_TABLE__OFFSET_HANDLER_ADDR+0x00	;Загружаем адрес обработчика
	PUSH ACCUM_L
	LDD ACCUM_L,Y+_OS_STIMER_TABLE__OFFSET_HANDLER_ADDR+0x01
	PUSH ACCUM_L
	RET														;Выполняем обработчик и запрещаем прерывания на случай если обработчик их разрешал
_OS_SYSTIMER_HANDLER__STIMER_RETPOINT:
	CLI
	POP_Y
	POP ACCUM_EL
	STD Y+_OS_STIMER_TABLE__OFFSET_HEAP_ADDR+0x00,ZL		;Восстанавливаем адрес HEAP
	STD Y+_OS_STIMER_TABLE__OFFSET_HEAP_ADDR+0x01,ZH
	LDD ACCUM_L,Y+_OS_STIMER_TABLE__OFFSET_PERIOD+0x00		;Инициализируем счетчик заново
	STD Y+_OS_STIMER_TABLE__OFFSET_CNTR+0x00,ACCUM_L
	LDD ACCUM_L,Y+_OS_STIMER_TABLE__OFFSET_PERIOD+0x01
	STD Y+_OS_STIMER_TABLE__OFFSET_CNTR+0x01,ACCUM_L

_OS_SYSTIMER_HANDLER__STIMER_NEXT:
	ADIW YL,0x09
	DEC ACCUM_EL
	BRNE _OS_SYSTIMER_HANDLER__STIMER_LOOP

	POP_A32
	POP_Y
	POP_Z
.ENDIF

	LDS TEMP_L,_OS_TIMER_CNTR								;Декремент внутреннего счетчика таймера для отсчета 1мс
	DEC TEMP_L
	STS _OS_TIMER_CNTR,TEMP_L
	BRNE _OS_TIMER1_HANDLER__END
	LDI TEMP_L,_OS_TIMER_1MS_PERIOD
	STS _OS_TIMER_CNTR,TEMP_L

	PUSH_Z
	LDI_Z _OS_UPTIME										;Инкремент uptime
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
	POP_Z

/*
.IFDEF SPH													;Проверка на переполнение стека (прямо сейчас)
	LDS TEMP_L,SPH
	CPI TEMP_L,high(STACK_BORDER)
	BRCS _OS_TIMER1_HANDLER__STACK_OVERFLOW
	BRNE _OS_TIMER1_HANDLER__CHECK_STACK_OVERFLOW
.ENDIF
	LDS TEMP_L,SPL
	CPI TEMP_L,low(STACK_BORDER)
	BRCS _OS_TIMER1_HANDLER__STACK_OVERFLOW
_OS_TIMER1_HANDLER__CHECK_STACK_OVERFLOW:
	LDS TEMP_L,STACK_BORDER									;Проверка на факт переполнения стека
	CPI TEMP_L,STACK_GUARD_VALUE
	BREQ _OS_TIMER1_HANDLER__NO_STACK_OVERFLOW
_OS_TIMER1_HANDLER__STACK_OVERFLOW:
	;TODO MJMP J8B_STACK_OVERFLOW_EXCEPTION
_OS_TIMER1_HANDLER__NO_STACK_OVERFLOW:
*/

.IF OS_FT_MULTITHREADING == 0x01
	LDS TEMP_L,_OS_DISPATCHER_TIMER_CNTR
	INC TEMP_L
	STS _OS_DISPATCHER_TIMER_CNTR,TEMP_L
	CPI TEMP_L,_OS_DISPATCHER_PERIOD						;Отработка диспетчера каждые 10мс
	BRCS _OS_TIMER1_HANDLER__END
	STS _OS_DISPATCHER_TIMER_CNTR,C0x00
.IF OS_FT_CPU_LOAD == 0x01
	LDS TEMP_L,_OS_CPU_LOAD_CNTR
	INC TEMP_L
	CPI TEMP_L,0x64
	BRNE _OS_TIMER1_HANDLER__CPU_LOAD_RESET_SKIP
	LDS TEMP_L,_OS_CPU_LOAD_TMP
	STS _OS_CPU_LOAD_TMP,C0x00
	STS OS_CPU_LOAD,TEMP_L
	CLR TEMP_L
_OS_TIMER1_HANDLER__CPU_LOAD_RESET_SKIP:
	STS _OS_CPU_LOAD_CNTR,TEMP_L
.ENDIF
	MJMP _OS_DISPATCHER_EVENT
.ENDIF

_OS_TIMER1_HANDLER__END:
	POP TEMP_L
	STS SREG,TEMP_L
	POP TEMP_L
_OS_TIMER1_HANDLER__RETI:
	RETI
.ENDIF

;-----------------------------------------------------------
OS_DISPATCHER_LOCK:
;-----------------------------------------------------------
;Блокируем диспетчер
;-----------------------------------------------------------
.IF OS_FT_MULTITHREADING==0x01
	CP DISPATCHER_CNTR,C0xFF
	BREQ PC+0x02
	INC DISPATCHER_CNTR
.ENDIF
	RET

;-----------------------------------------------------------
OS_DISPATCHER_UNLOCK:
;-----------------------------------------------------------
;Разблокируем диспетчер
;-----------------------------------------------------------
.IF OS_FT_MULTITHREADING==0x01
	CP DISPATCHER_CNTR,C0x00
	BREQ PC+0x02
	DEC DISPATCHER_CNTR
.ENDIF
	RET

;-----------------------------------------------------------
_OS_COREFAULT_OUT_OF_MEMORY:
;-----------------------------------------------------------
;Эндпоинт OUT OF MEMORY
;-----------------------------------------------------------
.IFDEF OS_OUT_CHAR
	LDI ACCUM_L,'^'
	MCALL OS_OUT_CHAR
.ENDIF
	CLI
	RJMP PC
.ENDIF
