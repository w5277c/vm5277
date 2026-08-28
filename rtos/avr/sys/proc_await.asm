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

// Если таймер не используется (OS_FT_TIMER==0) то выполняется задержка в 1 мс с помощью циклов
// И мы не знаем сколько тактов потребляет вызываемая функция проверки, поэтому процедура может немного ошибаться
// Для снижения ошибки функция вызывается во втором цикле

//TODO Скорректировать циклы ожидания 1 мс для OS_FT_TIMER==0

.IFNDEF OS_PROC_AWAIT
.IF OS_FT_TIMER==1
	.include "core/uptime16.asm"
.ENDIF

;--------------------------------------------------------
OS_PROC_AWAIT:
;--------------------------------------------------------
;Периодический вызываем функцию пока не наступит
;таймаут или не получим включенный флаг Z от функции
;IN:Z-адрес на функцию, TEMP_EL/EH-таймаут в мс
;OUT: флаг Z-true-успех,false-таймаут
;--------------------------------------------------------
	PUSH_X
	RCALL OS_PROC_AWAIT__INIT
	RCALL OS_PROC_AWAIT__CHECK
	POP_X
	RET

;--------------------------------------------------------
OS_PROC_AWAIT__INIT:
;--------------------------------------------------------
;Фиксируем начальное время в X
;OUT: X-текущий UPTIME
;--------------------------------------------------------
.IF OS_FT_TIMER==1
	PUSH TEMP_L
	LDS TEMP_L,SREG
    CLI
    LDS XL,_OS_UPTIME+0x00
    LDS XH,_OS_UPTIME+0x01
	STS SREG,TEMP_L
	POP TEMP_L
.ELSE
    LDI_X 0x0000
.ENDIF
    RET

;--------------------------------------------------------
OS_PROC_AWAIT__CHECK:
;--------------------------------------------------------
;Выполняем очновной код ожидания
;IN: X-текущий UPTIME, Z-адрес на функцию,
;TEMP_EL/EH-таймаут в мс
;OUT: флаг Z-true-успех,false-таймаут
;--------------------------------------------------------
.IF OS_FT_TIMER==1
_OS_PROC_AWAIT__LOOP:
	PUSH_X
	ICALL													;Вызываемая функция не должна менять Z
	POP_X
	BREQ _OS_PROC_AWAIT__END

	PUSH_A16												;Текущий UPTIME для подсчета таймаута
	MCALL OS_UPTIME16
	SUB ACCUM_L,XL											;Считаем дельту времени
	SBC ACCUM_H,XH
	CP ACCUM_L,TEMP_EL										;Проверяем на таймаут
	CPC ACCUM_H,TEMP_EH
	POP_A16
	BRCS _OS_PROC_AWAIT__LOOP
	CLZ														;Таймаут
.ELSE
	PUSH_A16
	PUSH TEMP_EL
	PUSH TEMP_EH
_OS_PROC_AWAIT__WO_UPTIME__LOOP1:
	LDI ACCUM_H,(29*CORE_FREQ)/10000
_OS_PROC_AWAIT__WO_UPTIME__LOOP2:
	PUSH TEMP_EL
	PUSH TEMP_EH
	PUSH_A16
	ICALL													;Вызываемая функция не должна менять Z
	POP_A16
	POP TEMP_EH
	POP TEMP_EL
	BREQ _OS_PROC_AWAIT__WO_UPTIME__END

	LDI ACCUM_L,0x40
_OS_PROC_AWAIT__WO_UPTIME__LOOP3:
	DEC ACCUM_L
	BRNE _OS_PROC_AWAIT__WO_UPTIME__LOOP3
	DEC ACCUM_H
	BRNE _OS_PROC_AWAIT__WO_UPTIME__LOOP2
	SUBI TEMP_EL,0x01
	SBCI TEMP_EH,0x00
	BRNE _OS_PROC_AWAIT__WO_UPTIME__LOOP1
	CLZ														;Таймаут
_OS_PROC_AWAIT__WO_UPTIME__END:
	POP TEMP_EH
	POP TEMP_EL
	POP_A16
.ENDIF
_OS_PROC_AWAIT__END:
	RET
.ENDIF
