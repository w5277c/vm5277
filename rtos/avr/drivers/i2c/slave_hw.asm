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

.include "core/irq_set.asm"

.IFNDEF OS_DRV_I2C_SLAVE_H__INIT
;-----------------------------------------------------------
OS_DRV_I2C_SLAVE_H__INIT:
;-----------------------------------------------------------
;Инициализация аппаратоного I2C в режиме ведомого
;IN: ACCUM_L-адрес устройства								;TODO Добавить параметр скорости передачи данных
;-----------------------------------------------------------
	_OS_POWER_ON PRTWI										;Подаем питание на TWI

	LSL ACCUM_L												;Записываем в TWAR I2C адрес (в режиме ведомого !=0)
	STS TWAR,ACCUM_L

	LDI ACCUM_L,OS_IR_TWI									;Устанавливаем обработчик для прерывания I2C
	LDI_Z _OS_DRV_I2C_SLAVE_H__IRQ_HANDLER
	MCALL OS_IRQ_SET
	RET

;-----------------------------------------------------------
OS_DRV_I2C_SLAVE_H__COMM:
;-----------------------------------------------------------
;Прием/передача данных
;IN: X-SRC buff, ACCUM_L/H-SRC длина, TEMP_L/H-DST buff,
;ACCUM_EL/EH-DST длина, TEMP_EL/EH-таймацут в мс
;-----------------------------------------------------------

	RET

;--------------------------------------------------------
_OS_DRV_I2C_SLAVE_H__IRQ_HANDLER:
;--------------------------------------------------------
;Событие от шины
;--------------------------------------------------------
	PUSH TEMP
	PUSH_Y
	PUSH_Z
	PUSH TEMP_H
	PUSH TEMP_L

	MCALL C5_RAM_OFFSET

	;Читаем состояние
	LDD TEMP,Y+_DRV_I2C_H_STATE
	SBRS TEMP,0x07
	RJMP PC+0x05
	LDI TEMP,(0<<TWEN)
	STS TWCR,TEMP
	RJMP _DRV_I2C_H_EVENT__END

	;Получаем состояние TWI
	LDS TEMP,TWSR
	ANDI TEMP,0xf8

	;TWI проверка на старт передачи или приема
	CPI TEMP,TWI_MTR_START
	BREQ _DRV_I2C_H_EVENT__START
	CPI TEMP,TWI_MTR_NEXT_START
	BREQ _DRV_I2C_H_EVENT__START
	RJMP _DRV_I2C_H_EVENT__NO_START
_DRV_I2C_H_EVENT__START:
	LDD ACCUM,Y+_DRV_I2C_H_ADDR
	LDD TEMP,Y+_DRV_I2C_H_STATE
	CPI TEMP,DRV_I2C_STATE_RC
	BRNE PC+0x02
	ORI ACCUM,0x01
	STS TWDR,ACCUM
	LDI TEMP,(1<<TWINT)|(1<<TWEN)|(1<<TWIE)
	STS TWCR,TEMP
	RJMP _DRV_I2C_H_EVENT__END
_DRV_I2C_H_EVENT__NO_START:
	;TWI проверка на передачу байта
	CPI TEMP,TWI_MT_SLA_W_ACK
	BREQ _DRV_I2C_H_EVENT__TR_NEXT
	CPI TEMP,TWI_MT_DATA_ACK
	BREQ _DRV_I2C_H_EVENT__TR_NEXT
	RJMP _DRV_I2C_H_EVENT__NO_TR_NEXT
_DRV_I2C_H_EVENT__TR_NEXT:
	;Считываем позицию в блоке данных
	LDD TEMP_L,Y+_DRV_I2C_H_POSITION
	;Считываем адрес данных для передачи
	LDD ZH,Y+_DRV_I2C_H_SRC_ADDR+0x00
	LDD ZL,Y+_DRV_I2C_H_SRC_ADDR+0x01
	MOV ACCUM,ZH
	ANDI ZH,0x7f
	;Добавляем смещение
	ADD ZL,TEMP_L
	ADC ZH,C0x00
	;Читаем байт с памяти и передаем
	SBRC ACCUM,0x07
	RJMP PC+0x03
	LD TEMP,Z
	RJMP PC+0x02
	LPM TEMP,Z
	STS TWDR,TEMP
	;Проверяем на конец передачи
	LDD TEMP,Y+_DRV_I2C_H_TX_LEN
	CP TEMP,TEMP_L
	BREQ _DRV_I2C_H_EVENT__RC
	;Инкрементируем и записываем новую позицию в блоке данных
	INC TEMP_L
	STD Y+_DRV_I2C_H_POSITION,TEMP_L
	LDI TEMP,(1<<TWINT)|(1<<TWEN)|(1<<TWIE)
	STS TWCR,TEMP
	RJMP _DRV_I2C_H_EVENT__END
_DRV_I2C_H_EVENT__RC:
	;Все данные переданы, обнуляем смещение
	STD Y+_DRV_I2C_H_POSITION,C0x00
	;Проверяем на необходимость принять данные
	LDD TEMP,Y+_DRV_I2C_H_RX_LEN
	CPI TEMP,0x00
	BREQ _DRV_I2C_H_EVENT__SKIP_RC
	;Переходим в режим приема
	LDI TEMP,DRV_I2C_STATE_RC
	STD Y+_DRV_I2C_H_STATE,TEMP
	LDI TEMP,(1<<TWINT)|(1<<TWEN)|(1<<TWIE)|(1<<TWSTA)		;START
	STS TWCR,TEMP
	RJMP _DRV_I2C_H_EVENT__END
_DRV_I2C_H_EVENT__SKIP_RC:
	;Все данные отправлены, прием не нужен
	LDI TEMP,DRV_RESULT_OK
	STD Y+_DRV_I2C_H_STATE,TEMP
	LDI TEMP,(1<<TWINT)|(1<<TWEN)|(1<<TWIE)|(1<<TWSTO)		;STOP
	STS TWCR,TEMP
	RJMP _DRV_I2C_H_EVENT__END
_DRV_I2C_H_EVENT__NO_TR_NEXT:
	;TWI проверка на прием байта
	CPI TEMP,TWI_MR_SLA_R_ACK
	BREQ _DRV_I2C_H_EVENT__RC_ACK
	CPI TEMP,TWI_MR_ACK
	BREQ _DRV_I2C_H_EVENT__RC_NEXT
	RJMP _DRV_I2C_H_EVENT__NO_RC_NEXT
_DRV_I2C_H_EVENT__RC_NEXT:
	;Считываем позицию в блоке данных
	LDD TEMP_L,Y+_DRV_I2C_H_POSITION
	;Считываем адрес данных для приема
	LDD ZH,Y+_DRV_I2C_H_DST_ADDR+0x00
	LDD ZL,Y+_DRV_I2C_H_DST_ADDR+0x01
	;Добавляем смещение
	ADD ZL,TEMP_L
	ADC YH,C0x00
	LDS TEMP,TWDR
	ST Z,TEMP
	;Инкрементируем позицию
	INC TEMP_L
	;Проверяем на конец приема
	LDD TEMP,Y+_DRV_I2C_H_RX_LEN
	CP TEMP,TEMP_L
	BREQ _DRV_I2C_H_EVENT__RC_NEXT_END
	;Записываем новую позицию в блоке данных
	STD Y+_DRV_I2C_H_POSITION,TEMP_L
_DRV_I2C_H_EVENT__RC_ACK:
	LDI TEMP,(1<<TWINT)|(1<<TWEN)|(1<<TWIE)|(1<<TWEA)		;ACK
	STS TWCR,TEMP
	RJMP _DRV_I2C_H_EVENT__END
_DRV_I2C_H_EVENT__RC_NEXT_END:
	;Все данные получены
	LDI TEMP,DRV_RESULT_OK
	STD Y+_DRV_I2C_H_STATE,TEMP
	LDI TEMP,(1<<TWINT)|(1<<TWEN)|(1<<TWIE)					;NACK
	STS TWCR,TEMP
	RJMP _DRV_I2C_H_EVENT__END
_DRV_I2C_H_EVENT__NO_RC_NEXT:
	;Ошибка
	LDI TEMP,DRV_RESULT_ERROR
	STD Y+_DRV_I2C_H_STATE,TEMP
	LDI TEMP,(1<<TWSTO)
	STS TWCR,TEMP
_DRV_I2C_H_EVENT__END:
	POP TEMP_L
	POP TEMP_H
	POP_Z
	POP_Y
	POP TEMP
	RET


.ENDIF