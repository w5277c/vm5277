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

 //TODO вероятно блокировка диспетчера будет уровнем выше

.IFNDEF J8BPROC_CLASS_REFCOUNT_INC_NR
;-----------------------------------------------------------
J8BPROC_CLASS_REFCOUNT_INC_NR:
;-----------------------------------------------------------
;Инкремент счетчика ссылок объекта
;IN: ACCUM_L/H-адрес HEAP
;MOD:ACCUM_EL
;-----------------------------------------------------------
	CP ACCUM_L,C0x00
	CPC ACCUM_H,C0x00
	BREQ J8BPROC_CLASS_REFCOUNT_INC__END

	MCALL OS_DISPATCHER_LOCK
	PUSH_Z

	MOVW ZL,ACCUM_L
	LDD ACCUM_L,Z+0x02
	CPI ACCUM_L,0x00
    BREQ J8BPROC_CLASS_REFCOUNT_INC__SKIP
	INC ACCUM_L
	STD Z+0x02,ACCUM_L
J8BPROC_CLASS_REFCOUNT_INC__SKIP:

	POP_Z
	MCALL OS_DISPATCHER_UNLOCK
J8BPROC_CLASS_REFCOUNT_INC__END:
	RET

;-----------------------------------------------------------
J8BPROC_CLASS_REFCOUNT_DEC_NR:
;-----------------------------------------------------------
;Декремент счетчика ссылок объекта
;IN: ACCUM_L/H-адрес HEAP
;MOD: ACCUM_L/H/EL/EH
;-----------------------------------------------------------
	CP ACCUM_L,C0x00
	CPC ACCUM_H,C0x00
	BREQ J8BPROC_CLASS_REFCOUNT_DEC__END

	MCALL OS_DISPATCHER_LOCK
	PUSH_Z

	MOVW ZL,ACCUM_L
	LDD ACCUM_EH,Z+0x02
	CPI ACCUM_EH,0x00
	BREQ J8BPROC_CLASS_REFCOUNT_DEC__SKIP2
	DEC ACCUM_EH
	BRNE J8BPROC_CLASS_REFCOUNT_DEC__SKIP1
	LDD ACCUM_L,Z+0x00
	LDD ACCUM_H,Z+0x01
	MCALL OS_DRAM_FREE
J8BPROC_CLASS_REFCOUNT_DEC__SKIP1:
	STD Z+0x02,ACCUM_EH

J8BPROC_CLASS_REFCOUNT_DEC__SKIP2:
	POP_Z
	MCALL OS_DISPATCHER_UNLOCK
J8BPROC_CLASS_REFCOUNT_DEC__END:
	RET
.ENDIF
