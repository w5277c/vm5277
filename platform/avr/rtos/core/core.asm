;-----------------------------------------------------------------------------------------------------------------------
;Файл распространяется под лицензией GPL-3.0-or-later, https://www.gnu.org/licenses/gpl-3.0.txt
;-----------------------------------------------------------------------------------------------------------------------
;30.05.2025	konstantin@5277.ru		Взят из проекта core5277
;-----------------------------------------------------------------------------------------------------------------------

;---RAM-OFFSETS---------------------------------------------
	;Таблица прерываний (_CORE_IR_QNT*3 для каждого прерывания, исключая RESET)
	.EQU	_CORE_IR_VECTORS_TABLE	= RAMSTART

CORE_INIT:
	CLI
	;Инициализация стека
.IFDEF SPH
	LDI TEMP,high(RAMEND)
	STS SPH,TEMP
.ENDIF
	LDI TEMP,low(RAMEND)
	STS SPL,TEMP
	EOR C0x00,C0x00
	LDI TEMP,0xff
	MOV C0xff,TEMP
	JMP MAIN

;-----------------------------------------------------------
_CORE_IR_HANDLER:
;-----------------------------------------------------------
;Обработчик всех прерываний
;-----------------------------------------------------------
	;Получаем адрес по которому определяем тип прерывания
	POP _CORE_REG_H
	POP _CORE_REG_L

	;Сохраняем регистры
	PUSH TEMP_L
	LDS TEMP_L,SREG
	CLI
	PUSH TEMP_L
	PUSH_Z
	;PUSH PID
	PUSH ACCUM_L
	;Находим смещение в таблице прерываний
	LDI_Z _CORE_IR_VECTORS_TABLE

.if _MCALL_SIZE == 2
	LSR _CORE_REG_L
.endif
	DEC _CORE_REG_L											;Получено на базе адреса выхода из процедуры(не входа), т.е. увеличен на 1

	MOV ACCUM_L,_CORE_REG_L									;Номер строки в таблице перываний
	DEC _CORE_REG_L											;Первая запись в таблице прерываний не используется
	BREQ PC+0x04
	MOV TEMP_L,_CORE_REG_L									;Умножаем на 3(1B-PID, 2B-адрес перехода)
	LSL _CORE_REG_L
	ADD _CORE_REG_L,TEMP

	ADD ZL,_CORE_REG_L
NOP
	LD PID,Z+
	CPI PID,0xff
	BREQ _CORE_IR_HANDLER__END

	LD TEMP_L,Z+
	LD ZL,Z
	MOV ZH,TEMP_L
	;Корневой процесс - текущий и единственный
;	PUSH _PID
;	MOV _PID,PID
	;Переходим, если адрес указан(в ACCUM_L номер строки таблицы прерываний)
	ICALL													;Возвращаться надо по RET(не RETI), TEMP и SREG сохранять не нужно
;	POP _PID
_CORE_IR_HANDLER__END:
	;Восстанавливаем регистры и выходим
	POP ACCUM_L
;	POP PID
	POP_Z
	POP TEMP_L
	STS SREG,TEMP_L
	POP TEMP_L
	RETI

;-----------------------------------------------------------
_CORE_TIMER_A_HANDLER:
;-----------------------------------------------------------
;Обработчик прерывания основного таймера
;-----------------------------------------------------------
	PUSH TEMP_L
	LDS TEMP_L,SREG
	PUSH TEMP_L

	LDI TEMP_L,TIMERS_SPEED-0x02
	SBRC _C5_COREFLAGS,_CFL_TIMER_UART_CORRECTION
	LDI TEMP_L,TIMERS_SPEED+0x02
	_CORRE5277_TIMERA_CORRECTOR TEMP_L

	;TODO

	POP TEMP_L
	STS SREG,TEMP_L
	POP TEMP_L
	RETI
