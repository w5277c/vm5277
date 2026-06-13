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

.IF OS_FT_IR_TABLE == 0x01 || OS_FT_DIAG == 0x01 || OS_FT_STDIN == 0x01
	.include "mem/ram_fill16.asm"
.ENDIF
.IF OS_FT_TIMER == 0x01 || OS_STAT_POOL_SIZE != 0x0000 || OS_FT_DRAM == 0x01
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
.IF OS_FT_PCINT
	.EQU	_OS_PCINT_PCICR				= _OS_ROTO			;PCICR – Pin Change Interrupt Control Register
	.SET	_OS_ROTO					= _OS_PCINT_PCICR+0x01
.ENDIF
.IF OS_FT_IR_TABLE == 0x01
	.EQU	_OS_IR_VECTORS_TABLE		= _OS_ROTO			;Таблица прерываний (OS_IR_QNT*3 для каждого прерывания, исключая RESET)
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
.ENDIF

;TODO Нужен динамический размир для блоков динамической памяти
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
.ENDIF

.IF OS_FT_MULTITHREADING == 0x01
	.include "core/dispatcher.asm"
.ENDIF

	.SET	STACK_BORDER				= _OS_ROTO			;1B-байт изменение значения которого указывает на переполнение стека
	.SET	STACK_TOP					= STACK_BORDER+0x01

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
	LDI TEMP_L,0x02
	MOV C0x02,TEMP_L

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

	;Устаналиваем значение для определения переполнения стека
	LDI TEMP_L,STACK_GUARD_VALUE
	STS STACK_BORDER,TEMP_L

.IF OS_FT_DIAG == 0x01
	;Заполнение всей памяти 0x52 значением (для диагностики)
	LDI ACCUM_EL,0x52
	LDI_X SRAM_START
	LDI ACCUM_L,low(SRAM_SIZE-0x0f)
	LDI ACCUM_H,high(SRAM_SIZE-0x0f)
	MCALL OS_RAM_FILL16_NR
.ENDIF

.IF OS_FT_IR_TABLE == 0x01
	;Чистка таблицы прерываний
	LDI ACCUM_EL,0xff
	LDI_X _OS_IR_VECTORS_TABLE
	LDI ACCUM_L,low(OS_IR_QNT*3)
	LDI ACCUM_H,high(OS_IR_QNT*3)
	MCALL OS_RAM_FILL16_NR
.ENDIF

.IF OS_FT_STDOUT == 0x01 || OS_FT_STDIN == 0x01
	MCALL OS_STDIO_INIT_NR
.ENDIF

.IF OS_FT_WELCOME == 0x01
	LDI_A16 CSTR_OSMSG1*2
	MCALL OS_OUT_CSTR
	LDI_A16 CSTR_OSNAME*2
	MCALL OS_OUT_CSTR
.ENDIF
.IF OS_FT_DIAG == 0x01
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
.ENDIF

.IF OS_STAT_POOL_SIZE != 0x0000
	;Очистка статического блока верхнего уровня
	LDI_Y _OS_STAT_POOL
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
		LDI_Y OS_DRAM_BMASK_ADDR
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
	LDI_Y _OS_UPTIME
	LDI TEMP_L,0x05
	LDI TEMP_H,0x00
	MCALL OS_RAM_CLEAR16_NR

	;Задаем счетчик основному таймеру
	LDI TEMP_L,_OS_TIMER_1MS_PERIOD
	STS _OS_TIMER_CNTR,TEMP_L

	;Запуск таймеров
	_OS_TIMER_INIT_NR
.ENDIF

.IF OS_FT_USTIMER == 0x01
	CLR US_CNTR_H
	CLR US_CNTR_EL
	_OS_USTIMER_INIT_NR
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
.IF OS_FT_IR_TABLE == 0x01
	SEI
.ENDIF
	JMP MAIN

.IF OS_FT_IR_TABLE == 0x01
;-----------------------------------------------------------
_OS_IR_HANDLER:
;-----------------------------------------------------------
;Обработчик всех прерываний
;-----------------------------------------------------------
	;Прерывания и так запрещены CLI														;Запрещаем прерывания глобально
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
_OS_TIMER_HANDLER:
;-----------------------------------------------------------
;Обработчик прерывания основного таймера1
;-----------------------------------------------------------
	PUSH TEMP_L
	LDS TEMP_L,SREG
	PUSH TEMP_L

	LDS TEMP_L,_OS_TIMER_CNTR								;Декремент внутреннего счетчика таймера для отсчета 1мс
	DEC TEMP_L
	STS _OS_TIMER_CNTR,TEMP_L
	BRNE _OS_TIMER1_HANDLER__END
	LDI TEMP_L,_OS_TIMER_1MS_PERIOD
	STS _OS_TIMER_CNTR,TEMP_L

	PUSH_Z													;Инкремент uptime
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

.IF OS_FT_USTIMER == 0x01
;-----------------------------------------------------------
_OS_USTIMER_HANDLER:
;-----------------------------------------------------------
;Обработчик прерывания таймера для счета микросекунд
;Нет учета частоты CPU (вход через каждые 2048 тактов)
;-----------------------------------------------------------
;-----------------------------------------------------------
	PUSH TEMP_L
	LDS TEMP_L,SREG

.IF CORE_FREQ <= 8000										;На частотах не превышающих 8МГц достачточно 16бит
	INC US_CNTR_H
.ELSE
	ADD US_CNTR_H,C0x01
	ADC US_CNTR_EL,C0x00									;24 бита
.ENDIF

	STS SREG,TEMP_L
	POP TEMP_L
	RETI
.ENDIF

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
