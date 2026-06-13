package ru.vm5277.plugin.executing;

public enum RunMode {
	BUILD_J8B("Build J8B", "j8b:build", "Compile J8B project to ASM and generate HEX firmware file"),
	FLASH_J8B("Build J8B & Flash", "j8b:build j8b:flash", "Compile J8B project to ASM and HEX firmware and flash target device"),
	RUN_J8B("Build J8B & Run", "j8b:build j8b:iflash", "Compile J8B project to ASM and HEX firmware and flash target device with interactive mode"),
	BUILD_ASM("Build ASM", "j8b:assemble", "Assemble ASM file to HEX firmware file"),
	FLASH_ASM("Build ASM & Flash", "j8b:assemble j8b:flash", "Assemble ASM file, generate HEX firmware and flash target device"),
	RUN_ASM("Build ASM & Run", "j8b:assemble j8b:iflash","Assemble ASM file and HEX firmware and flash target device with interactive mode");

	private final String name;
	private final String mvnCommand;
	private final String description;

	RunMode(String name, String mvnCommand, String description) {
		this.name = name;
		this.mvnCommand = mvnCommand;
		this.description = description;
	}

	public String getName() {
		return name;
	}
	public String getMvnCommand() { return mvnCommand; }
	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return name;
	}
}