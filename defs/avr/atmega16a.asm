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

;TODO привести именования регистров IO и их бит в стандарт Atmega328 вязтый за базу

.include "common.asm"

;---DESCRIPTION---------------------------------------------
;Header for ATmega16A (16KB ROM, 512B EEPROM, 1KB SRAM, LE)

	.DEVICE ATmega16A
	.EQU DEVICE_SIGNATURE							= 0x001E9403
	.SET CHIP_FAMILY 								= FAMILY_ATMEGA16
	
	.EQU FLASH_SIZE									= 0x4000; 16KB(8K words)
	.EQU FLASH_PAGESIZE								= 0x80	; 128B
	.EQU EEPROM_SIZE								= 0x0200; 512B 
	.EQU EEPROM_PAGESIZE							= 0x04	; 4B
	.EQU SRAM_START									= 0x0060; 96B
	.EQU SRAM_SIZE									= 0x0400; 1KB 

	.EQU BOOT_256W_ADDR								= 0x1f00; Начальный адрес полукилобайтного бутлоадера В СЛОВАХ
	.EQU BOOT_512W_ADDR								= 0x1e00; Начальный адрес килобайтного бутлоадера В СЛОВАХ

	;---SBI/CBI/SBIS/SBIC-port-defines----------------------
	.EQU	PORTA									= 0x1b
	.EQU	DDRA									= 0x1a
	.EQU	PINA									= 0x19
	.EQU	PORTB									= 0x18
	.EQU	DDRB									= 0x17
	.EQU	PINB									= 0x16
	.EQU	PORTC									= 0x15
	.EQU	DDRC									= 0x14
	.EQU	PINC									= 0x13
	.EQU	PORTD									= 0x12
	.EQU	DDRD									= 0x11
	.EQU	PIND									= 0x10
	.EQU	PORTE									= 0xff
	.EQU	DDRE									= 0xff
	.EQU	PINE									= 0xff
	.EQU	PORTF									= 0xff
	.EQU	DDRF									= 0xff
	.EQU	PINF									= 0xff
	.EQU	PORTG									= 0xff
	.EQU	DDRG									= 0xff
	.EQU	PING									= 0xff

	;---IO-REGISTERS----------------------------------------
	.EQU	SREG									= 0x5f
	.EQU	SPH										= 0x5e
	.EQU	SPL										= 0x5d
	.EQU	MCUCSR									= 0x54
	.EQU	MCUSR									= MCUCSR
	.EQU		JTD									= 0x07
	.EQU		ISC2								= 0x06
	.EQU		JTRF								= 0x04
	.EQU		WDRF								= 0x03
	.EQU		BORF								= 0x02
	.EQU		EXTRF								= 0x01
	.EQU		PORF								= 0x00
;	.EQU	PRR										= 0x64 				;не поддерживается
	;---TWI---
	.EQU	TWCR									= 0x56
	.EQU		TWINT								= 0x07
	.EQU		TWEA								= 0x06
	.EQU		TWSTA								= 0x05
	.EQU		TWSTO								= 0x04
	.EQU		TWWC								= 0x03
	.EQU		TWEN								= 0x02
	.EQU		TWIE								= 0x00
	.EQU	TWDR									= 0x23
	.EQU	TWAR									= 0x22
	.EQU		TWGCE								= 0x00
	.EQU	TWSR									= 0x21
	.EQU		TWPS1								= 0x01
	.EQU		TWPS0								= 0x00
	.EQU	TWBR									= 0x20
	;---TIMER0---
	.EQU	TIMSK0									= 0x59				;TIMSK
	.EQU		OCIE0								= 0x01
	.EQU		TOIE0								= 0x00
	.EQU	TCCR0									= 0x53
	.EQU		FOC0								= 0x07
	.EQU		WGM00								= 0x06
	.EQU		COM01								= 0x05
	.EQU		COM00								= 0x04
	.EQU		WGM01								= 0x03
	.EQU		CS02								= 0x02
	.EQU		CS01								= 0x01
	.EQU		CS00								= 0x00
	.EQU	TCNT0									= 0x52
	.EQU	OCR0									= 0x5c
	.EQU	TIFR0									= 0x58				;TIFR
	.EQU		OCF0								= 0x01
	.EQU		TOV0								= 0x00
	;---TIMER1---
	.EQU	TIMSK1									= 0x59				;TIMSK
	.EQU		OCIE1A								= 0x04
	.EQU		OCIE1B								= 0x03
	.EQU		TOIE1								= 0x02
	.EQU	TCCR1A									= 0x4f
	.EQU		COM1A1								= 0x07
	.EQU		COM1A0								= 0x06
	.EQU		COM1B1								= 0x05
	.EQU		COM1B0								= 0x04
	.EQU		FOC1A								= 0x03
	.EQU		FOC1B								= 0x02
	.EQU		WGM11								= 0x01
	.EQU		WGM10								= 0x00
	.EQU	TCCR1B									= 0x4e
	.EQU		ICNC1								= 0x07
	.EQU		ICES1								= 0x06
	.EQU		WGM13								= 0x04
	.EQU		WGM12								= 0x03
	.EQU		CS12								= 0x02
	.EQU		CS11								= 0x01
	.EQU		CS10								= 0x00
	.EQU	TCNT1H									= 0x4d
	.EQU	TCNT1L									= 0x4c
	.EQU	OCR1AH									= 0x4b
	.EQU	OCR1AL									= 0x4a
	.EQU	OCR1BH									= 0x49
	.EQU	OCR1BL									= 0x48
	.EQU	TIFR1									= 0x36				;TIFR
	.EQU		ICF1								= 0x05
	.EQU		OCF1A								= 0x04
	.EQU		OCF1B								= 0x03
	.EQU		TOV1								= 0x02
	;---TIMER2---
	.EQU	TIMSK2									= 0x59				;TIMSK
	.EQU		OCIE2								= 0x07
	.EQU		TOIE2								= 0x06
	.EQU	TCCR2									= 0x45
	.EQU		FOC2								= 0x07
	.EQU		WGM20								= 0x06
	.EQU		COM21								= 0x05
	.EQU		COM20								= 0x04
	.EQU		WGM21								= 0x03
	.EQU		CS22								= 0x02
	.EQU		CS21								= 0x01
	.EQU		CS20								= 0x00
	.EQU	TCNT2									= 0x44
	.EQU	OCR2									= 0x43
	.EQU	TIFR2									= 0x58				;TIFR
	.EQU		OCF2								= 0x07
	.EQU		TOV2								= 0x06
	;---UART---
	.EQU	UDR0									= 0x2c				;UDR
	.EQU	UCSR0A									= 0x2b				;UCSRA
	.EQU		RXC0								= 0x07
	.EQU		TXC0								= 0x06
	.EQU		UDRE0								= 0x05
	.EQU		FE0									= 0x04
	.EQU		DOR0								= 0x03
	.EQU		UPE0								= 0x02
	.EQU		U2X0								= 0x01
	.EQU		MPCM0								= 0x00
	.EQU	UCSR0B									= 0x2a				;UCSRB
	.EQU		RXCIE0								= 0x07
	.EQU		TXCIE0								= 0x06
	.EQU		UDRIE0								= 0x05
	.EQU		RXEN0								= 0x04
	.EQU		TXEN0								= 0x03
	.EQU		UCSZ02								= 0x02
	.EQU		RXB80								= 0x01
	.EQU		TXB80								= 0x00
	.EQU	UCSR0C									= 0x40				;UCSRC*-один адрес с UBRRH
	.EQU		URSEL0								= 0x07
	.EQU		UMSEL0								= 0x06
	.EQU		UPM01								= 0x05
	.EQU		UPM00								= 0x04
	.EQU		USBS0								= 0x03
	.EQU		UCSZ01								= 0x02
	.EQU		UCSZ00								= 0x01
	.EQU		UCPOL0								= 0x00
	.EQU	UBRR0H									= 0x40				;UBRRH*-один адрес с UCSRC
	.EQU	UBRR0L									= 0x29
	;---PCINT---														;Не поддерживается
	;---INT---
	.EQU	MCUCR									= 0x55
	.EQU		SM2									= 0x07
	.EQU		SE									= 0x06
	.EQU		SM1									= 0x05
	.EQU		SM0									= 0x04
	.EQU		ISC11								= 0x03
	.EQU		ISC10								= 0x02
	.EQU		ISC01								= 0x01
	.EQU		ISC00								= 0x00
	.EQU	GICR									= 0x5b
	.EQU		INT1								= 0x07
	.EQU		INT0								= 0x06
	.EQU		INT2								= 0x05
	.EQU		IVSEL								= 0x01
	.EQU		IVCE								= 0x00
	.EQU	GIFR									= 0x5a
	.EQU		INTF1								= 0x07
	.EQU		INTF0								= 0x06
	.EQU		INTF2								= 0x05
	;---EEPROM---
	.EQU	EEARH									= 0x3f
	.EQU	EEARL									= 0x3e
	.EQU	EEDR									= 0x3d
	.EQU	EECR									= 0x3c
	.EQU		EERIE								= 0x03
	.EQU		EEMPE								= 0x02
	.EQU		EEMWE								= EEMPE
	.EQU		EEPE								= 0x01
	.EQU		EEWE								= EEPE
	.EQU		EERE								= 0x00
	;---ADC---
	.EQU	ADMUX									= 0x27
	.EQU		REFS1								= 0x07
	.EQU		REFS0								= 0x06
	.EQU		ADLAR								= 0x05
	.EQU		MUX4								= 0x04
	.EQU		MUX3								= 0x03
	.EQU		MUX2								= 0x02
	.EQU		MUX1								= 0x01
	.EQU		MUX0								= 0x00
	.EQU	ADCSRA									= 0x26
	.EQU		ADEN								= 0x07
	.EQU		ADSC								= 0x06
	.EQU		ADATE								= 0x05
	.EQU		ADIF								= 0x04
	.EQU		ADIE								= 0x03
	.EQU		ADPS2								= 0x02
	.EQU		ADPS1								= 0x01
	.EQU		ADPS0								= 0x00
	.EQU	ADCSRB									= 0x50				;SFIOR
	.EQU		ACME								= 0x03
	.EQU		ADTS2								= 0x07
	.EQU		ADTS1								= 0x06
	.EQU		ADTS0								= 0x05
	.EQU	ADCL									= 0x24
	.EQU	ADCH									= 0x25
	.EQU	ADC0									= 0x00
	.EQU	ADC1									= 0x01
	.EQU	ADC2									= 0x02
	.EQU	ADC3									= 0x03
	.EQU	ADC4									= 0x04
	.EQU	ADC5									= 0x05
	.EQU	ADC6									= 0x06
	.EQU	ADC7									= 0x07
	;--SPI---
	.EQU	SPCR									= 0x2d
	.EQU		SPIE								= 0x07
	.EQU		SPE									= 0x06
	.EQU		DORD								= 0x05
	.EQU		MSTR								= 0x04
	.EQU		CPOL								= 0x03
	.EQU		CPHA								= 0x02
	.EQU		SPR1								= 0x01
	.EQU		SPR0								= 0x00
	.EQU	SPSR									= 0x2e
	.EQU		SPIF								= 0x07
	.EQU		WCOL								= 0x06
	.EQU		SPI2X								= 0x00
	.EQU	SPDR									= 0x2f
	;--SPM---
	.EQU	SPMCSR									= 0x57
	.EQU	SPMCR									= SPMCSR
	.EQU		SPMIE								= 0x07
	.EQU		RWWSB								= 0x06
	.EQU		RWWSRE								= 0x04
	.EQU		BLBSET								= 0x03
	.EQU		PGWRT								= 0x02
	.EQU		PGERS								= 0x01
	.EQU		SPMEN								= 0x00
	;---WATCHDOG---
	.EQU	WDTCR									= 0x41
	.EQU		WDCE								= 0x04	;=WDTOE
	.EQU		WDE									= 0x03
	.EQU		WDP2								= 0x02
	.EQU		WDP1								= 0x01
	.EQU		WDP0								= 0x00

	;---ADC-VOLTAGE-REFERENCE---
	.EQU	ADC_VREF_AREF							= (0<<REFS1)|(0<<REFS0)
	.EQU	ADC_VREF_AVCC							= (0<<REFS1)|(1<<REFS0)
	.EQU	ADC_VREF_1_1_CAP						= (1<<REFS1)|(1<<REFS0)
	;---ADC-PRESCALLER---
	.EQU	ADC_PRESC_X2							= 0x01
	.EQU	ADC_PRESC_X4							= 0x02
	.EQU	ADC_PRESC_X8							= 0x03
	.EQU	ADC_PRESC_X16							= 0x04
	.EQU	ADC_PRESC_X32							= 0x05
	.EQU	ADC_PRESC_X64							= 0x06
	.EQU	ADC_PRESC_X128							= 0x07

	;---OSCILLATOR-CALIBRATION-REGISTER---
	.EQU	OSCCAL									= 0x51
