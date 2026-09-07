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

.IFNDEF OS_HAL_TIMER_SET_PERIOD
.include "hal/timer/_compute_ioregs.asm"
.include "hal/timer/_set_ioregs.asm"
;-----------------------------------------------------------
OS_HAL_TIMER_SET_PERIOD:
;-----------------------------------------------------------
;Устанавливаем новый еприод
;IN: ACCUM_H/L-период,ACCUM_EL-номер таймера
;MOD: ACCUM_EH/EL/H/L
;OUT: флаг C-true-успешно
;-----------------------------------------------------------
	PUSH FLAGS
	PUSH RESULT

	MOV FLAGS,ACCUM_EL
	MCALL _OS_HAL_TIMER_COMPUTE_IOREGS
	MCALL _OS_HAL_TIMER_SET_IOREGS

	POP RESULT
	POP FLAGS
	RET
.ENDIF