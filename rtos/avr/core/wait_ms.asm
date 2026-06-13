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
	PUSH ACCUM_H
	PUSH ACCUM_L
	PUSH_T16
.IF OS_FT_TIMER == 0x01
;	SUBI ACCUM_L,0x01										;TODO ранее была бага - опаздывал на 1 мс (в тесте Thread не выявлена)
;	SBCI ACCUM_H,0x00
;	BRCS _OS_WAIT_MS__END

	LDS TEMP_H,_OS_UPTIME+0x00
_OS_WAIT_MS__LOOP:
	LDS TEMP_L,_OS_UPTIME+0x00
	CP TEMP_H,TEMP_L
	BREQ _OS_WAIT_MS__LOOP
	PUSH TEMP_L
	SUB TEMP_L,TEMP_H
	SUB ACCUM_L,TEMP_L
	SBC ACCUM_H,C0x00
	POP TEMP_H
	MOV TEMP_L,ACCUM_L
	OR TEMP_L,ACCUM_H
	BRNE _OS_WAIT_MS__LOOP
.ELSE
_OS_WAIT_MS__LOOP1:
	LDI TEMP_H,(29*CORE_FREQ)/10000
_OS_WAIT_MS__LOOP2:
	LDI TEMP_L,0x56
_OS_WAIT_MS__LOOP3:
	NOP
	DEC TEMP_L
	BRNE _OS_WAIT_MS__LOOP3
	DEC TEMP_H
	BRNE _OS_WAIT_MS__LOOP2

	SUBI ACCUM_L,0x01
	SBCI ACCUM_H,0x00
	BRCC _OS_WAIT_MS__LOOP1
.ENDIF
_OS_WAIT_MS__END:
	POP_T16
	POP ACCUM_L
	POP ACCUM_H

_OS_WAIT_MS__RET:
	RET
.ENDIF