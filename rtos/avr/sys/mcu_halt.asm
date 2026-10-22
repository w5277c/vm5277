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

.IFNDEF MCU_HALT
.IF OS_FT_WELCOME == 0x01
.include "stdio/out_cstr.asm"
.include "resources/txt_mcu_done.asm"
.ENDIF
;-----------------------------------------------------------
MCU_HALT:
;-----------------------------------------------------------
;Останов микроконтроллера с переходом в энергосберегающий
;режим
;-----------------------------------------------------------
.IF OS_FT_SOFT_RESET == 0x01								;Остаемся слушать прерывания
.IF OS_FT_WELCOME == 0x01
	LDI ACCUM_L,low(_OS_MCU_DONE_TXT*2)
	LDI ACCUM_H,high(_OS_MCU_DONE_TXT*2)
	MCALL OS_OUT_CSTR
.ENDIF
	; TODO написать и использовать макрос для перехода в энергосберегающий режим?
	RJMP PC
.ENDIF
	CLI
.IF OS_FT_WELCOME == 0x01
	LDI ACCUM_L,low(_OS_MCU_DONE_TXT*2)
	LDI ACCUM_H,high(_OS_MCU_DONE_TXT*2)
	MCALL OS_OUT_CSTR
.ENDIF
	; TODO написать и использовать макрос для перехода в энергосберегающий режим
	RJMP PC
.ENDIF

