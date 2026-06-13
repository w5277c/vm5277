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

//Любое! прерывание (кроме таймера на котором висит диспетчер) должно блокировать диспетчер
//Иначе процедура resume сломает стек.
//Т.е. на стеке в момент вызова диспетчера (и с остановленной текущей задачей) должны быть только вызов таймера и диспетчера

.IF OS_FT_MULTITHREADING == 0x01
.IFNDEF _OS_DISPATCHER_EVENT

.include "mem/ram_popblk16.asm"
.include "mem/ram_pushblk16.asm"
.include "j8b/class_refcount.asm"

;.include "stdio/out_stack.asm"
;.include "stdio/out_hex16.asm"

;---Структура заголовка класса Thread для refSize=0x02---
;---0x00-2B heap size (lh) - с учетом multithreading данных
;---0x02-1B link counter
;---0x03-2B metadata addr (lh)
;---данные-для-multithreading---
;---0x05-1B status bits
;---0x06-2B next thread addr (lh)
;---0x08-2B stack addr (lh)
;---0x0a-2B stack size (lh)
;---0x0c-16B task h-registers
;---0x1c-1B SREG
;---0x1d-2B ret point (lh)
;---0x1f-2B resume timestamp (wait state)
;---0x21-2B resource id
;---total:35 bytes

;---Структура заголовка класса TimerTask для refSize=0x02---
;---0x00-2B heap size (lh) - с учетом multithreading данных
;---0x02-1B link counter
;---0x03-2B metadata addr (lh)
;---данные-для-multithreading---
;---0x05-1B status bits
;---0x06-2B next thread addr (lh)
;---0x08-2B stack addr (lh)
;---0x0a-2B stack size (lh)
;---0x0c-16B task h-registers
;---0x1c-1B SREG
;---0x1d-2B ret point (lh)
;---0x1f-2B resume timestamp
;---0x21-2B period (x1ms)
;---total:35 bytes

	.EQU	_OS_DISPATCHER_PERIOD					= 10

;---Смещения заголовка класса
	.EQU	_OS_DISPATCHER__HEAP_SIZE_OFFSET		= 0x00
	.EQU	_OS_DISPATCHER__HEAP_LINKCNTR_OFFSET	= 0x02
	.EQU	_OS_DISPATCHER__HEAP_META_OFFSET		= 0x03
;---Смещения в заголовке heap'а
	.EQU	_OS_DISPATCHER__TASK_STATEBITS_OFFSET	= 0x05
	.EQU	_OS_DISPATCHER__TASK_NEXT_OFFSET		= 0x06
	.EQU	_OS_DISPATCHER__TASK_STACKADDR_OFFSET	= 0x08
	.EQU	_OS_DISPATCHER__TASK_STACKSIZE_OFFSET	= 0x0a
	.EQU	_OS_DISPATCHER__TASK_REGS_OFFSET		= 0x0c
	.EQU	_OS_DISPATCHER__TASK_SREG_OFFSET		= 0x1c
	.EQU	_OS_DISPATCHER__TASK_RETPOINT_OFFSET	= 0x1d
	.EQU	_OS_DISPATCHER__TASK_TIMESTAMP_OFFSET	= 0x1f
	.EQU	_OS_DISPATCHER__TASK_PERIOD_OFFSET		= 0x21
	;.EQU	_OS_DISPATCHER__TASK_RESOURCE_OFFSET	= 0x23
;---Биты статуса---
	.EQU	_OS_DISPATCHER__TASK_READY				= 0x00	;true-ждет(готова для) выполнения
	.EQU	_OS_DISPATCHER__TASK_RUNNING			= 0x01	;true-выполняется
	.EQU	_OS_DISPATCHER__TASK_WAIT				= 0x02	;true-ждет истечения таймера для перехода в READY
	.EQU	_OS_DISPATCHER__TASK_RESOURCE_WAIT		= 0x03	;true-ждет разблокировки ресурса или истечения таймера (0x0000 - без таймаута)
	.EQU	_OS_DISPATCHER__TASK_RESERVED			= 0x04
	.EQU	_OS_DISPATCHER__TASK_STOP_REQUEST		= 0x05	;true-включен флаг остановки
	.EQU	_OS_DISPATCHER__TASK_STOPPED			= 0x06	;true-завершен
	.EQU	_OS_DISPATCHER__TASK_TIMER				= 0x07	;тип класса - таймер
;-----------------------------------------------------------
_OS_DISPATCHER_EVENT:
;-----------------------------------------------------------
;Передаем управление на следующую задачу
;-----------------------------------------------------------
	PUSH_Z

	;Проверяем все нити со статусом WAIT и уменьшаем временную метку
.IF OS_FT_CPU_LOAD == 0x01
	PUSH FLAGS
	LDI FLAGS,0x00
.ENDIF

	MCALL _OS_DISPATCHER_CHECK_PID
   	CP PID_L,C0x00
   	CPC PID_H,C0x00
   	BREQ _OS_DISPATCHER_EVENT__WAIT_LOOP_DONE				;Потоков нет, завершаем цикл
   	MOVW ZL,PID_L

_OS_DISPATCHER_EVENT__WAIT_LOOP:
   	LDD TEMP_L,Z+_OS_DISPATCHER__TASK_STATEBITS_OFFSET		;Состояние потока
.IF OS_FT_CPU_LOAD == 0x01
   	SBRC TEMP_L,_OS_DISPATCHER__TASK_RUNNING
	LDI FLAGS,0x01
.ENDIF
   	SBRS TEMP_L,_OS_DISPATCHER__TASK_WAIT
   	RJMP _OS_DISPATCHER_EVENT__WAIT_LOOP_NEXT				;Нет состояния WAIT, берем следующую нить
	PUSH_A16
	LDD ACCUM_L,Z+_OS_DISPATCHER__TASK_TIMESTAMP_OFFSET+0x00
	LDD ACCUM_H,Z+_OS_DISPATCHER__TASK_TIMESTAMP_OFFSET+0x01
	SUBI ACCUM_L,_OS_DISPATCHER_PERIOD
	SBCI ACCUM_H,0x00
	STD Z+_OS_DISPATCHER__TASK_TIMESTAMP_OFFSET+0x00,ACCUM_L
	STD Z+_OS_DISPATCHER__TASK_TIMESTAMP_OFFSET+0x01,ACCUM_H
	POP_A16
	BRCC _OS_DISPATCHER_EVENT__WAIT_LOOP_NEXT
	ANDI TEMP_L,low(~(1<<_OS_DISPATCHER__TASK_WAIT))
	SBRS TEMP_L,_OS_DISPATCHER__TASK_STOP_REQUEST
	ORI TEMP_L,(1<<_OS_DISPATCHER__TASK_READY)
	STD Z+_OS_DISPATCHER__TASK_STATEBITS_OFFSET,TEMP_L		;Состояние потока WAIT->READY
	SBRS TEMP_L,_OS_DISPATCHER__TASK_TIMER
	RJMP _OS_DISPATCHER_EVENT__WAIT_LOOP_NEXT
	LDD TEMP_L,Z+_OS_DISPATCHER__TASK_PERIOD_OFFSET+0x00
	STD Z+_OS_DISPATCHER__TASK_TIMESTAMP_OFFSET+0x00,TEMP_L
	LDD TEMP_L,Z+_OS_DISPATCHER__TASK_PERIOD_OFFSET+0x01
	STD Z+_OS_DISPATCHER__TASK_TIMESTAMP_OFFSET+0x01,TEMP_L
_OS_DISPATCHER_EVENT__WAIT_LOOP_NEXT:
   	MCALL _OS_DISPATCHER_NEXT_TASK
   	CP ZL,PID_L
	CPC ZH,PID_H
	BRNE _OS_DISPATCHER_EVENT__WAIT_LOOP

_OS_DISPATCHER_EVENT__WAIT_LOOP_DONE:
.IF OS_FT_CPU_LOAD == 0x01
	SBRS FLAGS,0x00
	RJMP _OS_DISPATCHER_EVENT__CPU_LOAD_INC_SKIP
	LDS TEMP_L,_OS_CPU_LOAD_TMP
	INC TEMP_L
	STS _OS_CPU_LOAD_TMP,TEMP_L
_OS_DISPATCHER_EVENT__CPU_LOAD_INC_SKIP:
	POP FLAGS
.ENDIF
	BRTS _OS_DISPATCHER_EVENT__END							;Пропускаем переключение нитей, если диспетчер заблокирован

	;Если в диапазоне 1 кванта просыпается несколько задач, то их пробуждение дает ротацию - порядок очереди выполнения нарушается,
	;и это лучше чем одинаковый порядок - оставляем. Если нужно убрать - то вероятнее всего в этом месте достаточно просто сбрасывать PID в начальное положение
	;T=false-Работаем из прерывания,T=true-вызов из _OS_TASK_ENDPOINT
_OS_DISPATCHER_EVENT__MAINLOOP:
	MCALL _OS_DISPATCHER_CHECK_PID
	CP PID_L,C0x00
	CPC PID_H,C0x00
	BREQ _OS_DISPATCHER_EVENT__SLEEP						;Потоков нет - спим
	MOVW ZL,PID_L
_OS_DISPATCHER_EVENT__READY_SEARCH_LOOP:
	LDD TEMP_L,Z+_OS_DISPATCHER__TASK_STATEBITS_OFFSET		;Состояние потока
	SBRC TEMP_L,_OS_DISPATCHER__TASK_READY
	RJMP _OS_DISPATCHER_EVENT__SUSPEND_LOOP_INIT			;Переходим в цикл suspend (т.е. выполняем suspend только если есть поток готовый на выполнение)
	MCALL _OS_DISPATCHER_NEXT_TASK
	CP ZL,PID_L
	CPC ZH,PID_H
	BRNE _OS_DISPATCHER_EVENT__READY_SEARCH_LOOP
	BRTS _OS_DISPATCHER_EVENT__SLEEP						;Спим, если вызваны из _OS_TASK_ENDPOINT
	RJMP _OS_DISPATCHER_EVENT__END							;Все задачи проанализированы, нет с состоянием READY - выходим

_OS_DISPATCHER_EVENT__SUSPEND_LOOP_INIT:
	MOVW ZL,PID_L											;HEAP текущего потока в Z
_OS_DISPATCHER_EVENT__SUSPEND_LOOP:
	LDD TEMP_L,Z+_OS_DISPATCHER__TASK_STATEBITS_OFFSET		;Состояние потока
	SBRS TEMP_L,_OS_DISPATCHER__TASK_RUNNING
	RJMP _OS_DISPATCHER_EVENT__SUSPEND_LOOP__SKIP_SUSPEND
	ANDI TEMP_L,low(~(1<<_OS_DISPATCHER__TASK_RUNNING))		;Задача больше не запущена
	ORI TEMP_L,(1<<_OS_DISPATCHER__TASK_READY)				;Но готова для запуска
	STD Z+_OS_DISPATCHER__TASK_STATEBITS_OFFSET,TEMP_L
OS_DISPATCHER_EVENT__DO_SUSPEND:							;Точка входа для процедуры WAIT_MS
	MCALL _OS_DISPATCHER_TASK_SUSPEND						;Если задача выполняется - приостанавливаем и переходим к следующей
	MOVW PID_L,ZL											;Обеспечиваем проход по всем элементам заново
	RJMP _OS_DISPATCHER_EVENT__SUSPEND_LOOP__NEXT
_OS_DISPATCHER_EVENT__SUSPEND_LOOP__SKIP_SUSPEND:
	SBRC TEMP_L,_OS_DISPATCHER__TASK_READY
	RJMP _OS_DISPATCHER_TASK_RESUME							;Если задача готова к выполнению - переходим на возобновление
_OS_DISPATCHER_EVENT__SUSPEND_LOOP__NEXT:
	MCALL _OS_DISPATCHER_NEXT_TASK
	CP ZL,PID_L
	CPC ZH,PID_H
	BRNE _OS_DISPATCHER_EVENT__SUSPEND_LOOP

_OS_DISPATCHER_EVENT__SLEEP:
	; Здесь мы оказываемся если выгрузили задачу (и ее стек сохранен) и нет новой на выполнение, или вообще нет активных задач.
	; В любом случае в стеке ничего актуального нет (при работе прерываний диспетчер блокируется)
	; Смело очищаем стек и бесконечно спим, пока не придет прерывание или поменяется состояние задачи
.IFDEF SPH
	LDI TEMP_L,high(SRAM_START+SRAM_SIZE-3)					;-0x02 - Оставляем точку возврата
	STS SPH,TEMP_L
.ENDIF
	LDI TEMP_L,low(SRAM_START+SRAM_SIZE-3)
	STS SPL,TEMP_L
	CLT
	SEI
_OS_DISPATCHER_EVENT__SLEEP_LOOP:
	SLEEP													;Весь пул задач проверен, кандидат на выполнение не найден, спим
	RJMP _OS_DISPATCHER_EVENT__SLEEP_LOOP

_OS_DISPATCHER_EVENT__END:									;Выходим если не было переключения потоков
	POP_Z
	POP TEMP_L
    STS SREG,TEMP_L
    POP TEMP_L
   	RETI

;-----------------------------------------------------------
_OS_DISPATCHER_CHECK_PID:
;-----------------------------------------------------------
;Актуализируем PID
;-----------------------------------------------------------
;IN: PID_L/H-текущая задача
;MOD: TEMP_L
;OUT: PID_L/H-текущая задача
;-----------------------------------------------------------
	;TODO в PID должен быть задан в начале программы адрес первой нити - main, но почему получаем OUT_OF_MEMORY если убрать проверку на 0?
	CP PID_L,C0x00
	CPC PID_H,C0x00
	BRNE _OS_DISPATCHER_CHECK_PID__END
	LDS TEMP_L,OS_FIRSTTHREAD_ADDR+0x00
	MOV PID_L,TEMP_L
	LDS TEMP_L,OS_FIRSTTHREAD_ADDR+0x01
	MOV PID_H,TEMP_L
_OS_DISPATCHER_CHECK_PID__END:
	RET

;-----------------------------------------------------------
_OS_DISPATCHER_NEXT_TASK:
;-----------------------------------------------------------
;Загружаем в Z следующую задачу
;-----------------------------------------------------------
;IN: ZL/H-текущая задача
;MOD: TEMP_L
;OUT:ZL/H-следующая задача
;-----------------------------------------------------------
	LDD TEMP_L,Z+_OS_DISPATCHER__TASK_NEXT_OFFSET+0x00
	LDD ZH,Z+_OS_DISPATCHER__TASK_NEXT_OFFSET+0x01
	MOV ZL,TEMP_L
	CP ZL,C0x00
	CPC ZH,C0x00
	BRNE _OS_DISPATCHER_NEXT_TASK__END
	LDS ZL,OS_FIRSTTHREAD_ADDR+0x00
	LDS ZH,OS_FIRSTTHREAD_ADDR+0x01
_OS_DISPATCHER_NEXT_TASK__END:
	RET

;-----------------------------------------------------------
OS_TASK_START_NR:
;-----------------------------------------------------------
;Запускает поток
;-----------------------------------------------------------
;IN: ACCUM_L/H-адрес HEAP потока
;MOD:ACCUM_L/H/EL
;-----------------------------------------------------------
	BLD ACCUM_EL,0x00										;Блокируем диспетчер
	SET

	PUSH_Z
	MOVW ZL,ACCUM_L
	LDD ACCUM_L,Z+_OS_DISPATCHER__TASK_STATEBITS_OFFSET		;Состояние потока
	MOV ACCUM_H,ACCUM_L
	ANDI ACCUM_H,0x0f
	BRNE _OS_TASK_START__END								;Запуск уже выполнен

	LDD ACCUM_H,Z+_OS_DISPATCHER__HEAP_LINKCNTR_OFFSET
	INC ACCUM_H												;Инсремент ref count
	STD Z+_OS_DISPATCHER__HEAP_LINKCNTR_OFFSET,ACCUM_H

	ORI ACCUM_L,(1<<_OS_DISPATCHER__TASK_READY)
	STD Z+_OS_DISPATCHER__TASK_STATEBITS_OFFSET,ACCUM_L		;Состояние потока READY
	STD Z+_OS_DISPATCHER__TASK_STACKSIZE_OFFSET+0x00,C0x00	;Размер стека (стек пуст)
	STD Z+_OS_DISPATCHER__TASK_STACKSIZE_OFFSET+0x01,C0x00
	STD Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x0e,ZL			;Записываем в Z адрес HEAP
	STD Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x0f,ZH

_OS_TASK_START__END:
	POP_Z
	BST ACCUM_EL,0x00										;Снимаем блокировку диспетчера
	RET

;-----------------------------------------------------------
OS_TIMER_START_NR:
;-----------------------------------------------------------
;Запускает поток таймера
;-----------------------------------------------------------
;IN: ACCUM_L/H-адрес HEAP потока, ACCUM_EL/EH-период
;MOD:ACCUM_L/H
;-----------------------------------------------------------
	PUSH TEMP_L
	BLD TEMP_L,0x00											;Блокируем диспетчер
	SET

	PUSH_Z
	MOVW ZL,ACCUM_L

	STD Z+_OS_DISPATCHER__TASK_TIMESTAMP_OFFSET+0x00,ACCUM_EL;Таймаут
	STD Z+_OS_DISPATCHER__TASK_TIMESTAMP_OFFSET+0x01,ACCUM_EH
	STD Z+_OS_DISPATCHER__TASK_PERIOD_OFFSET+0x00,ACCUM_EL	;Период
	STD Z+_OS_DISPATCHER__TASK_PERIOD_OFFSET+0x01,ACCUM_EH

	LDD ACCUM_L,Z+_OS_DISPATCHER__TASK_STATEBITS_OFFSET		;Состояние потока
	SBRC ACCUM_L,_OS_DISPATCHER__TASK_RUNNING
	RJMP _OS_TIMER_START__END								;Нить выполняется
	LDD ACCUM_H,Z+_OS_DISPATCHER__HEAP_LINKCNTR_OFFSET
	INC ACCUM_H												;Инсремент ref count
	STD Z+_OS_DISPATCHER__HEAP_LINKCNTR_OFFSET,ACCUM_H

	LDI ACCUM_L,(1<<_OS_DISPATCHER__TASK_READY)|(1<<_OS_DISPATCHER__TASK_TIMER)
	STD Z+_OS_DISPATCHER__TASK_STATEBITS_OFFSET,ACCUM_L		;Состояние потока READY
	STD Z+_OS_DISPATCHER__TASK_STACKSIZE_OFFSET+0x00,C0x00	;Размер стека (стек пуст)
	STD Z+_OS_DISPATCHER__TASK_STACKSIZE_OFFSET+0x01,C0x00
	STD Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x0e,ZL			;Записываем в Z адрес HEAP
	STD Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x0f,ZH

_OS_TIMER_START__END:
	POP_Z
	BST TEMP_L,0x00											;Снимаем блокировку диспетчера
	POP TEMP_L
	RET

;-----------------------------------------------------------
OS_TASK_STOP_NR:
;-----------------------------------------------------------
;Устанавливаем запрос на останов потока
;-----------------------------------------------------------
;IN: ACCUM_L/H-адрес HEAP потока
;MOD: ACCUM_L/H/EL
;-----------------------------------------------------------
	BLD ACCUM_EL,0x00										;Блокируем диспетчер
	SET

	PUSH_Z
	MOVW ZL,ACCUM_L
	LDD ACCUM_L,Z+_OS_DISPATCHER__TASK_STATEBITS_OFFSET		;Состояние потока
	ORI ACCUM_L,(1<<_OS_DISPATCHER__TASK_STOP_REQUEST)
	STD Z+_OS_DISPATCHER__TASK_STATEBITS_OFFSET,ACCUM_L		;Состояние потока STOP_REQUEST

	POP_Z
	BST ACCUM_EL,0x00										;Снимаем блокировку диспетчера
	RET

;-----------------------------------------------------------
OS_TASK_IS_ALIVE:
;-----------------------------------------------------------
;Проверка на необходимость завершения выполнения кода потока
;-----------------------------------------------------------
;IN: ACCUM_L/H-адрес HEAP потока
;OUT: Флаг C=true-alive(нет запроса на останов)
;--------------------------------------------------------
	PUSH_Z
	MOVW ZL,ACCUM_L
	LDD ACCUM_L,Z+_OS_DISPATCHER__TASK_STATEBITS_OFFSET		;Состояние потока
	ANDI ACCUM_L,(1<<_OS_DISPATCHER__TASK_STOP_REQUEST)
	SUB ACCUM_L,C0x01
	POP_Z
	RET

;-----------------------------------------------------------
OS_TASK_IS_STOPPED:
;-----------------------------------------------------------
;Проверка на завершенный поток
;-----------------------------------------------------------
;IN: ACCUM_L/H-адрес HEAP потока
;OUT: Флаг C=true-stopped
;--------------------------------------------------------
	PUSH_Z
	MOVW ZL,ACCUM_L
	LDD ACCUM_L,Z+_OS_DISPATCHER__TASK_STATEBITS_OFFSET		;Состояние потока
	ANDI ACCUM_L,(1<<_OS_DISPATCHER__TASK_STOPPED)
	ADD ACCUM_L,C0xff
	POP_Z
	RET

;-----------------------------------------------------------
_OS_TASK_ENDPOINT:
;-----------------------------------------------------------
;Сюда передается управление когда код нити завершен
;-----------------------------------------------------------
;IN: Z-адрес HEAP нити
;--------------------------------------------------------
	SET

	LDD ACCUM_L,Z+_OS_DISPATCHER__TASK_STATEBITS_OFFSET
	SBRS ACCUM_L,_OS_DISPATCHER__TASK_TIMER
	RJMP _OS_TASK_ENDPOINT__IS_TASK

	ANDI ACCUM_L,low(~(1<<_OS_DISPATCHER__TASK_RUNNING));
	ORI ACCUM_L,(1<<_OS_DISPATCHER__TASK_WAIT)
	STD Z+_OS_DISPATCHER__TASK_STATEBITS_OFFSET,ACCUM_L		;Состояние потока WAIT

	MCALL _OS_DISPATCHER_FREE_STACK_NR
	RJMP _OS_TASK_ENDPOINT__END

_OS_TASK_ENDPOINT__IS_TASK:
	LDS YL,OS_FIRSTTHREAD_ADDR+0x00
	LDS YH,OS_FIRSTTHREAD_ADDR+0x01
	CP YL,ZL
	CPC YH,ZH
	BRNE _OS_TASK_ENDPOINT__PREV_THREAD_LOOP

	;Наша нить была первой
	LDD ACCUM_L,Z+_OS_DISPATCHER__TASK_NEXT_OFFSET+0x00
	LDD ACCUM_H,Z+_OS_DISPATCHER__TASK_NEXT_OFFSET+0x01
	CP ACCUM_L,C0x00
	CPC ACCUM_H,C0x00
	BREQ _OS_TASK_ENDPOINT__TERMINATE
	STS OS_FIRSTTHREAD_ADDR+0x00,ACCUM_L
	STS OS_FIRSTTHREAD_ADDR+0x01,ACCUM_H
	MOVW PID_L,ACCUM_L
	RJMP _OS_TASK_ENDPOINT__NOT_PREV_THREAD
	;Конец программы - переход к процедурам терминации (типа CLI+RJMP PC)
_OS_TASK_ENDPOINT__TERMINATE:
	MJMP MCU_HALT

_OS_TASK_ENDPOINT__PREV_THREAD_LOOP:
	LDD ACCUM_L,Y+_OS_DISPATCHER__TASK_NEXT_OFFSET+0x00
	LDD ACCUM_H,Y+_OS_DISPATCHER__TASK_NEXT_OFFSET+0x01
	CP ACCUM_L,ZL
	CPC ACCUM_H,ZH
	BREQ _OS_TASK_ENDPOINT__GOT_PREV_THREAD
	MOVW YL,ACCUM_L
	RJMP _OS_TASK_ENDPOINT__PREV_THREAD_LOOP

_OS_TASK_ENDPOINT__GOT_PREV_THREAD:
	LDD ACCUM_L,Z+_OS_DISPATCHER__TASK_NEXT_OFFSET+0x00
	LDD ACCUM_H,Z+_OS_DISPATCHER__TASK_NEXT_OFFSET+0x01
	STD Y+_OS_DISPATCHER__TASK_NEXT_OFFSET+0x00,ACCUM_L
	STD Y+_OS_DISPATCHER__TASK_NEXT_OFFSET+0x01,ACCUM_H
	MOVW PID_L,ACCUM_L

_OS_TASK_ENDPOINT__NOT_PREV_THREAD:
	LDI ACCUM_L,(1<<_OS_DISPATCHER__TASK_STOPPED)
	STD Z+_OS_DISPATCHER__TASK_STATEBITS_OFFSET,ACCUM_L		;Состояние потока STOPPED

	MCALL _OS_DISPATCHER_FREE_STACK_NR

	; Декремент счтечика ссылок с возможным освобожением HEAP
	MOVW ACCUM_L,ZL
	MCALL J8BPROC_CLASS_REFCOUNT_DEC_NR

_OS_TASK_ENDPOINT__END:
	;Передаем управление диспетчеру
	JMP _OS_DISPATCHER_EVENT__MAINLOOP

;-----------------------------------------------------------
_OS_DISPATCHER_FREE_STACK_NR:
;-----------------------------------------------------------
;Освобождаем стек нити
;IN: Z-адрес HEAP нити
;MOD:ACCUM_L/H
;-----------------------------------------------------------
	PUSH_Z

	LDD ACCUM_L,Z+_OS_DISPATCHER__TASK_STACKSIZE_OFFSET+0x00;Загружаем размер стека
	LDD ACCUM_H,Z+_OS_DISPATCHER__TASK_STACKSIZE_OFFSET+0x01
	CP ACCUM_L,C0x00
	CPC ACCUM_H,C0x00
	BREQ _OS_DISPATCHER_FREE_STACK__END
	PUSH ACCUM_EH
	LDD ACCUM_EH,Z+_OS_DISPATCHER__TASK_STACKADDR_OFFSET+0x00
	LDD ZH,Z+_OS_DISPATCHER__TASK_STACKADDR_OFFSET+0x01
	MOV ZL,ACCUM_EH
	POP ACCUM_EH
	MCALL OS_DRAM_FREE

_OS_DISPATCHER_FREE_STACK__END:
	POP_Z
	RET

;-----------------------------------------------------------
_OS_DISPATCHER_TASK_SUSPEND:
;-----------------------------------------------------------
;Приостанавливаем задачу
;IN: Z-адрес HEAP нити
;-----------------------------------------------------------
	STD Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x00,r16												;Сохраняем регистры задачи
	STD Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x01,r17
	STD Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x02,r18
	STD Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x03,r19
	STD Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x04,r20
	STD Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x05,r21
	STD Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x06,r22
	STD Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x07,r23
	STD Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x09,r25
	STD Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x0a,r26
	STD Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x0b,r27
	STD Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x0c,r28
	STD Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x0d,r29
/*
PUSH_A16
LDI ACCUM_L,'\n'
MCALL OS_OUT_CHAR
LDI ACCUM_L,'s'
MCALL OS_OUT_CHAR
MOVW ACCUM_L,ZL
MCALL OS_OUT_HEX16
LDI ACCUM_L,':'
MCALL OS_OUT_CHAR
POP_A16
MCALL OS_OUT_STACK_NR
*/
	LDS YL,SPL												;Далее сохраненные регистры можно переиспользовать
.IFDEF SPH
	LDS YH,SPH
.ELSE
	LDI YH,0x00
.ENDIF
	LDD ACCUM_L,Y+0x06										;Сохраненный TEMP_L
	STD Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x08,ACCUM_L
	LDD ACCUM_L,Y+0x04										;Сохраненный ZL
	STD Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x0e,ACCUM_L
	LDD ACCUM_L,Y+0x03										;Сохраненный ZH
	STD Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x0f,ACCUM_L
	LDD ACCUM_L,Y+0x05										;Сохраненный SREG
	STD Z+_OS_DISPATCHER__TASK_SREG_OFFSET,ACCUM_L
	LDD ACCUM_L,Y+0x08										;Адрес возврата L
	STD Z+_OS_DISPATCHER__TASK_RETPOINT_OFFSET+0x00,ACCUM_L
	LDD ACCUM_L,Y+0x07										;Адрес возврата H
	STD Z+_OS_DISPATCHER__TASK_RETPOINT_OFFSET+0x01,ACCUM_L
	LDD TEMP_EL,Y+0x02										;Адрес возврата из этой процедуры
	LDD TEMP_EH,Y+0x01

	ADIW YL,0x08											;Вычитаем из стека все что сформировано прерыванием таймера(0x02 - известный end point)

	LDI ACCUM_L,low(SRAM_START+SRAM_SIZE-3)					;2 байиа - end point
	SUB ACCUM_L,YL
.IFDEF SPH
	LDI ACCUM_H,high(SRAM_START+SRAM_SIZE-3)
	SBC ACCUM_H,YH
.ELSE
	LDI ACCUM_H,0x00
.ENDIF
	STD Z+_OS_DISPATCHER__TASK_STACKSIZE_OFFSET+0x00,ACCUM_L;Сохраняем в HEAP размер стека
	STD Z+_OS_DISPATCHER__TASK_STACKSIZE_OFFSET+0x01,ACCUM_H
	CP ACCUM_L,C0x00
	CPC ACCUM_H,C0x00
	BREQ _OS_DISPATCHER_TASK_SUSPEND__END
	CLI
.IFDEF SPH
	STS SPH,YH
.ENDIF
	STS SPL,YL
	SEI
	MOVW YL,ZL
	MCALL OS_DRAM_ALLOC										;Выделяем память под стек задачи
	BRCC _OS_DISPATCHER_TASK_SUSPEND__STACK_COPY_ALLOCATED
	RJMP _OS_COREFAULT_OUT_OF_MEMORY
_OS_DISPATCHER_TASK_SUSPEND__STACK_COPY_ALLOCATED:
	STD Y+_OS_DISPATCHER__TASK_STACKADDR_OFFSET+0x00,ZL		;Сохраняем в HEAP адрес стека
	STD Y+_OS_DISPATCHER__TASK_STACKADDR_OFFSET+0x01,ZH
	MOVW XL,ZL
	ADD XL,ACCUM_L
	ADC XH,ACCUM_H
	MCALL OS_RAM_POPBLK16
	MOVW ZL,YL
_OS_DISPATCHER_TASK_SUSPEND__END:
	LDI ACCUM_L,low(SRAM_START+SRAM_SIZE-1)
	STS SPL,ACCUM_L
.IFDEF SPH
	LDI ACCUM_L,high(SRAM_START+SRAM_SIZE-1)
	STS SPH,ACCUM_L
.ENDIF

	PUSH TEMP_EL
	PUSH TEMP_EH
	;Возвращаемся в диспетчер, текущий стек и регистры более не актуальны
	RET

;-----------------------------------------------------------
_OS_DISPATCHER_TASK_RESUME:
;-----------------------------------------------------------
;Возомновляем задачу
;IN: Z-адрес HEAP потока
;-----------------------------------------------------------
	ANDI TEMP_L,low(~(1<<_OS_DISPATCHER__TASK_READY))		;Состяние READY->RUNNING
	ORI TEMP_L,(1<<_OS_DISPATCHER__TASK_RUNNING)
	STD Z+_OS_DISPATCHER__TASK_STATEBITS_OFFSET,TEMP_L

	MOVW PID_L,ZL											;Обновляем текущий PID

	CLI
.IFDEF SPH													;Обнуляем стек
   	LDI ACCUM_L,high(SRAM_START+SRAM_SIZE-1)
   	STS SPH,ACCUM_L
.ENDIF
   	LDI ACCUM_L,low(SRAM_START+SRAM_SIZE-1)
   	STS SPL,ACCUM_L
	SEI

	LDI ACCUM_L,low(_OS_TASK_ENDPOINT)						;Помещаем в пустой стек адрес возврата с run() в обработчик end point
	PUSH ACCUM_L
	LDI ACCUM_L,high(_OS_TASK_ENDPOINT)
	PUSH ACCUM_L

	LDD ACCUM_L,Z+_OS_DISPATCHER__TASK_STACKSIZE_OFFSET+0x00;Загружаем размер стека
	LDD ACCUM_H,Z+_OS_DISPATCHER__TASK_STACKSIZE_OFFSET+0x01
	CP ACCUM_L,C0x00
	CPC ACCUM_H,C0x00
	BREQ _OS_DISPATCHER_TASK_RESUME__SKIP_STACK_LOAD
	LDD XL,Y+_OS_DISPATCHER__TASK_STACKADDR_OFFSET+0x00		;Загружаем адрес данных стека
	LDD XH,Y+_OS_DISPATCHER__TASK_STACKADDR_OFFSET+0x01
	MCALL OS_RAM_PUSHBLK16									;Заполняем стек
	MCALL _OS_DISPATCHER_FREE_STACK_NR

_OS_DISPATCHER_TASK_RESUME__SKIP_STACK_LOAD:
	LDD ACCUM_L,Z+_OS_DISPATCHER__TASK_RETPOINT_OFFSET+0x00	;Помещаем в стек адрес на котором было прервано выполнение
	PUSH ACCUM_L
	LDD ACCUM_L,Z+_OS_DISPATCHER__TASK_RETPOINT_OFFSET+0x01
	PUSH ACCUM_L

	LDD ACCUM_L,Z+_OS_DISPATCHER__TASK_SREG_OFFSET			;Восстанавливаем SREG
	STS SREG,ACCUM_L
	LDD r16,Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x00			;Восстанавливаем 16 регистров
	LDD r17,Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x01
	LDD r18,Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x02
	LDD r19,Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x03
	LDD r20,Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x04
	LDD r21,Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x05
	LDD r22,Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x06
	LDD r23,Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x07
	LDD r24,Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x08
	LDD r25,Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x09
	LDD r26,Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x0a
	LDD r27,Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x0b
	LDD r28,Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x0c
	LDD r29,Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x0d
	CLI
	LDD ATOM_L,Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x0e
	LDD r31,Z+_OS_DISPATCHER__TASK_REGS_OFFSET+0x0f
	MOV r30,ATOM_L
	SEI
	RET														;Возвращаемся на выполнение восстановленной программы
.ENDIF
.ENDIF
