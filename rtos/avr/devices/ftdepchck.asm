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

;Подключение родительских фич

.IF OS_FT_WELCOME==0x01
	.SET	OS_FT_STDOUT 							= 0x01	;WELCOME требует STDOUT
.ENDIF
.IF OS_FT_SOFT_RESET==0x01
	.SET	OS_FT_STDIN 							= 0x01	;Использует STDIN
.ENDIF
.IF OS_FT_STDIN==0x01
	.SET	OS_FT_IRQ_PCINT_BLOCK					= 0x01	;Режим STDIN использует внешние прерывания
.ENDIF
.IF	OS_FT_TIMERTASK==0x01
	.SET	OS_FT_MULTITHREADING					= 0x01	;Поддержка задачи-таймера требует многозадачность
.ENDIF
.IF OS_FT_MULTITHREADING==0x01 || OS_FT_STIMER==0x01
	.SET	OS_FT_TIMER 							= 0x01	;Многопоточность и программные таймеры используют таймер
.ENDIF

.IF OS_FT_IRQ_INT_BLOCK==0x01 || OS_FT_IRQ_PCINT_BLOCK==0x01 || OS_FT_IRQ_WD_BLOCK==0x01 || OS_FT_IRQ_TIMER_CAPTURE_BLOCK==0x01
    .SET OS_FT_IR_TABLE                             = 0x01
.ENDIF
.IF OS_FT_IRQ_TIMER_BLOCK==0x01 || OS_FT_IRQ_UART_START_BLOCK==0x01 || OS_FT_IRQ_SPI_BLOCK==0x01 || OS_FT_IRQ_UART_BLOCK==0x01
    .SET OS_FT_IR_TABLE                             = 0x01
.ENDIF
.IF OS_FT_IRQ_EEPROM_BLOCK==0x01 || OS_FT_IRQ_ANALOG_CMP_BLOCK==0x01 || OS_FT_IRQ_TWI_BLOCK==0x01 ||OS_FT_IRQ_SPM_BLOCK==0x01
    .SET OS_FT_IR_TABLE                             = 0x01
.ENDIF
.IF OS_FT_IRQ_PTC_BLOCK==0x01 || OS_FT_IRQ_USI_BLOCK==0x01 || OS_FT_IRQ_ADC_BLOCK==0x01 || OS_FT_TIMER==0x01
    .SET OS_FT_IR_TABLE                             = 0x01
.ENDIF
