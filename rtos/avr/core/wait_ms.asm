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

.IFNDEF OS_WAIT_MS
OS_WAIT_MS:
;--------------------------------------------------------
;Ждем истечения времени с момента прошлого сна или
;с момента вызова C5_TIMER_MARK
;Или выжидаем считая такты при OS_FT_TIMER1==0 (без
;мультипоточности)
;Или переводим нить в режим сна в многопоточном режиме
;IN ACCUM_H,ACCUM_L - время в 0.001s
;--------------------------------------------------------
	CP ACCUM_L,C0x00
	CPC ACCUM_H,C0x00
	BREQ _OS_WAIT_MS__RET

.IF OS_FT_MULTITHREADING == 0x01
	BRTS _OS_WAIT_MS__BY_UPTIME								;Если диспетчер заблокирован - используем отсет паузы на базе uptime

	PUSH TEMP_L												;Аналогичная запись в стек как и для обработчика основного таймера
	LDS TEMP_L,SREG
	PUSH TEMP_L
	PUSH_Z

	MOVW ZL,PID_L											;Усыпляем задачу на определенное время
	LDD TEMP_L,Z+_OS_DISPATCHER__TASK_STATEBITS_OFFSET		;Состояние потока RUNNING->WAIT
	ANDI TEMP_L,low(~(1<<_OS_DISPATCHER__TASK_RUNNING))
	ORI TEMP_L,(1<<_OS_DISPATCHER__TASK_WAIT)
	STD Z+_OS_DISPATCHER__TASK_STATEBITS_OFFSET,TEMP_L		;Состояние потока WAIT
	STD Z+_OS_DISPATCHER__TASK_TIMESTAMP_OFFSET+0x00,ACCUM_L;Время сна в мс
	STD Z+_OS_DISPATCHER__TASK_TIMESTAMP_OFFSET+0x01,ACCUM_H
	JMP OS_DISPATCHER_EVENT__DO_SUSPEND						;Переходим в диспетчер усыпляя задачу
.ENDIF
_OS_WAIT_MS__BY_UPTIME:
	PUSH_A32
.IF OS_FT_TIMER == 0x01
	PUSH FLAGS
_OS_WAIT_MS__LOOP1:
	LDS FLAGS,_OS_UPTIME+0x00
	LDS ACCUM_EH,_OS_TIMER_CNTR
_OS_WAIT_MS__LOOP2:
	SLEEP
	LDS ACCUM_EL,_OS_TIMER_CNTR
	CP ACCUM_EH,ACCUM_EL
	BREQ _OS_WAIT_MS__LOOP2
_OS_WAIT_MS__LOOP3:
	SLEEP
	LDS ACCUM_EL,_OS_TIMER_CNTR
	CP ACCUM_EH,ACCUM_EL
	BRNE _OS_WAIT_MS__LOOP3
	LDS ACCUM_EL,_OS_UPTIME+0x00
	SUB ACCUM_EL,FLAGS
	SUB ACCUM_L,ACCUM_EL
	SBC ACCUM_H,C0x00
	BRNE _OS_WAIT_MS__LOOP1
	POP FLAGS
.ELSE
_OS_WAIT_MS__LOOP1:
	LDI ACCUM_EH,(29*CORE_FREQ)/10000
_OS_WAIT_MS__LOOP2:
	LDI ACCUM_EL,0x56
_OS_WAIT_MS__LOOP3:
	NOP
	DEC ACCUM_EL
	BRNE _OS_WAIT_MS__LOOP3
	DEC ACCUM_EH
	BRNE _OS_WAIT_MS__LOOP2

	SUBI ACCUM_L,0x01
	SBCI ACCUM_H,0x00
	BRCC _OS_WAIT_MS__LOOP1
.ENDIF
_OS_WAIT_MS__END:
	POP_A32

_OS_WAIT_MS__RET:
	RET
.ENDIF