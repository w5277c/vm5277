package ru.vm5277.plugin.executing;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ContextHelpLabel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.*;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nullable;
import ru.vm5277.common.Device;
import ru.vm5277.common.FSUtils;
import ru.vm5277.common.enums.PlatformType;
import ru.vm5277.common.flash.FlashTool;
import ru.vm5277.common.flash.FlashToolParam;
import ru.vm5277.common.flash.FlashToolProvider;
import ru.vm5277.common.flash.InternalFlashTool;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FlashBootloaderDialog extends DialogWrapper {
	public static final int LABEL_WIDTH = 120;
	public static final int FIELD_WIDTH = 350;
	private final static Logger LOG = Logger.getInstance(FlashBootloaderDialog.class);
	private final Project project;
	private final Path toolkitPath;
	private final Path localUidsPath;
	private final String origStdioPort;
	private final PlatformType origPlatformType;
	private final String origDeviceId;
	private final FlashTool origFlashTool;
	private Device device = null;
	private boolean flashing = false;
	private boolean disposed = false;

	private JBTabbedPane tabbedPane;

	private JBTextField stdioPortFiled = new JBTextField();
	private JComboBox<PlatformType> platformCombo = new JComboBox<>();
	private JComboBox<String> deviceCombo = new JComboBox<>();
	private JComboBox<FlashTool> flashToolCombo = new JComboBox<>();
	private JButton flashToolDownloadButton = new JButton(ExecuteSettingsEditor.DOWNLOAD_BUTTON_TEXT);;
	private JPanel flashToolParamsPanel = new JPanel(new VerticalLayout(5));
	private JBTextField customPortField;

	// UID Settings
	private JBRadioButton uidModeGlobal;
	private JBRadioButton uidModeLocal;
	private JBRadioButton uidModeParam;
	private ButtonGroup uidModeGroup;
	private TextFieldWithBrowseButton uidsFileField;
	private JBTextField uidField;
	private JBTextField vendorField;
	private JBTextField passwordField;
	private JBCheckBox overwriteUidCheckbox;

	// Security
	private JBTextField keyField;
	private TextFieldWithBrowseButton secureAsmFileField;

	// Advanced
	private JBCheckBox generateListingCheckbox;

	// Output
	private JPanel outputPanel;
	private JBTextField toolkitPathField;
	private JBTextField mkbootCmdField;
	private JBTextField flashCmdField;
	private JTextArea logArea;

	private JButton flashButton;
	private JButton closeButton;

	public FlashBootloaderDialog(Project project, Path toolkitPath, String origStdioPort, PlatformType origPlatformType, String origDeviceId, FlashTool origFlashTool) {
		super(project, false);
		this.project = project;
		this.toolkitPath = toolkitPath;
		this.localUidsPath =  toolkitPath.resolve("var").resolve("uids.db").normalize();
		this.origStdioPort = origStdioPort;
		this.origPlatformType = origPlatformType;
		this.origDeviceId = origDeviceId;
		this.origFlashTool = origFlashTool;
		setTitle("Flash vm5277 Bootloader");
		init();
	}

	@Override
	protected Action[] createActions() {
		return new Action[0]; // Убираем стандартные кнопки
	}

	@Override
	protected JComponent createSouthPanel() {
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		flashButton = new JButton("Flash device");
		flashButton.setForeground(JBColor.RED);
		closeButton = new JButton("Close");

		flashButton.addActionListener(e -> {
			if (!flashing) {
				startFlashing();
			}
		});

		closeButton.addActionListener(e -> {
			if (!flashing) {
				close(CANCEL_EXIT_CODE);
			}
		});

		buttonPanel.add(flashButton);
		buttonPanel.add(closeButton);

		return buttonPanel;
	}

	@Override
	protected @Nullable JComponent createCenterPanel() {
		tabbedPane = new JBTabbedPane();
		tabbedPane.addTab("Target", createTargetPanel());
		tabbedPane.addTab("UID Settings", createUidPanel());
		tabbedPane.addTab("Security", createSecurityPanel());
		tabbedPane.addTab("Advanced", createAdvancedPanel());
		outputPanel = createOutputPanel();
		tabbedPane.addTab("Output", outputPanel);

		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (tabbedPane.getSelectedComponent() == outputPanel) {
					FlashBootloaderDialog.this.generateCommands();
				}
			}
		});

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(tabbedPane, BorderLayout.CENTER);
		panel.setPreferredSize(new Dimension(550, 450));

		return panel;
	}

	private JPanel createTargetPanel() {
		JPanel stdioPortsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		stdioPortFiled.setPreferredSize(new Dimension(FIELD_WIDTH, stdioPortFiled.getPreferredSize().height));
		stdioPortFiled.getEmptyText().setText("PA4/PC2 or RX or RX/TX or PB0 or RESET ...");
		stdioPortFiled.setText(origStdioPort);
		JLabel label = new JLabel("STDIO Port(s):");
		label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
		ContextHelpLabel stdioPortHelpLabel = ContextHelpLabel.create(
				"Specify device pins for flashing and communication.\n" +
				"For two-wire mode use format: RX/TX. For single-wire mode: RX.\n" +
				"Most commonly used pins are RX/TX or RX.\n" +
				"Pin names can be found in device documentation (datasheet)."
		);
		stdioPortsRow.add(label);
		stdioPortsRow.add(stdioPortFiled);
		stdioPortsRow.add(Box.createRigidArea(new Dimension(5, 0)));
		stdioPortsRow.add(stdioPortHelpLabel);

		JPanel platformRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		platformCombo.setPreferredSize(new Dimension(FIELD_WIDTH, platformCombo.getPreferredSize().height));
		label = new JLabel("Platform:");
		label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
		platformRow.add(label);
		platformRow.add(platformCombo);

		JPanel deviceRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		deviceCombo.setPreferredSize(new Dimension(FIELD_WIDTH, deviceCombo.getPreferredSize().height));
		label = new JLabel("Device:");
		label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
		deviceRow.add(label);
		deviceRow.add(deviceCombo);

		final ItemListener flashToolItemListener = new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					FlashTool flashTool = (FlashTool) e.getItem();
					Path toolPath = null==toolkitPath ? null : flashTool.resolveTool(false);
					flashToolDownloadButton.setVisible(null==toolPath);
					flashToolCombo.setPreferredSize(new Dimension(null==toolPath ? FIELD_WIDTH/2 : FIELD_WIDTH, flashToolCombo.getPreferredSize().height));
					flashToolParamsPanel.removeAll();
					for(FlashToolParam param : flashTool.getParams()) {
						flashToolParamsPanel.add(ExecuteSettingsEditor.flashToolParamToUIComponent(param, LABEL_WIDTH, FIELD_WIDTH));
					}
					flashToolParamsPanel.revalidate();
					flashToolParamsPanel.repaint();
				}
			}
		};

		JPanel flashToolRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		flashToolCombo.setPreferredSize(new Dimension(FIELD_WIDTH, flashToolCombo.getPreferredSize().height));
		label = new JLabel("Flash Tool:");
		label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
		flashToolDownloadButton.setVisible(false);
		flashToolRow.add(label);
		flashToolRow.add(flashToolCombo);
		flashToolRow.add(flashToolDownloadButton);
		flashToolDownloadButton.addActionListener((ActionEvent e) -> {
			flashToolDownloadButton.setEnabled(false);
			flashToolDownloadButton.setText(ExecuteSettingsEditor.DOWNLOADING_BUTTON_TEXT);
			new Thread(new Runnable() {
				@Override
				public void run() {
					ru.vm5277.common.Logger log = new ru.vm5277.common.Logger() {
						@Override public void info(String s) {LOG.info(s);}
						@Override public void warn(String s) {LOG.warn(s);}
						@Override public void error(String s) {LOG.error(s);}
						@Override public void debug(String s) {LOG.debug(s);}
						@Override public boolean progress(int percents, String status) {
							SwingUtilities.invokeLater(() -> {
								if(!disposed) {
									flashToolDownloadButton.setText(status + "... " + percents + "%");
								}
							});
							return !disposed;
						}
					};
					boolean success = (null != ((FlashTool) flashToolCombo.getSelectedItem()).resolveTool(true));
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							flashToolDownloadButton.setEnabled(true);
							if(success) {
								flashToolDownloadButton.setVisible(false);
								flashToolDownloadButton.setText(ExecuteSettingsEditor.DOWNLOAD_BUTTON_TEXT);
								flashToolCombo.setPreferredSize(new Dimension(FIELD_WIDTH, flashToolCombo.getPreferredSize().height));

								flashToolCombo.removeItemListener(flashToolItemListener);
								flashToolCombo.removeAllItems();
								for (FlashTool flashTool : FlashToolProvider.getTools(null, toolkitPath, (PlatformType) platformCombo.getSelectedItem(), (String)deviceCombo.getSelectedItem())) {
									if(!(flashTool instanceof InternalFlashTool)) {
										flashToolCombo.addItem(flashTool);
									}
								}
								flashToolCombo.setSelectedItem(null);
								flashToolCombo.addItemListener(flashToolItemListener);
								if(null==flashToolCombo.getSelectedItem() && 0!=flashToolCombo.getItemCount()) {
									flashToolCombo.setSelectedIndex(0);
								}
							} else {
								flashToolDownloadButton.setText(ExecuteSettingsEditor.RETRY_DOWNLOAD_BUTTON_TEXT);
							}
						}
					});
				}
			}).start();
		});

		ItemListener deviceItemListener = new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					flashToolCombo.removeItemListener(flashToolItemListener);
					flashToolCombo.removeAllItems();
					flashToolParamsPanel.removeAll();
					flashToolDownloadButton.setVisible(false);

					device = null;
					String deviceId = (String) e.getItem();
					try {
						device = new Device(toolkitPath, (PlatformType) platformCombo.getSelectedItem(), deviceId);
						if(device.allowsBootloader()) {
							deviceCombo.setForeground(null);

							for (FlashTool flashTool : FlashToolProvider.getTools(null, toolkitPath, (PlatformType) platformCombo.getSelectedItem(), deviceId)) {
								if(!(flashTool instanceof InternalFlashTool)) {
									flashToolCombo.addItem(flashTool);
								}
							}
						}
						else {
							deviceCombo.setForeground(JBColor.RED);
						}
					}
					catch (Exception ex) {
						deviceCombo.setForeground(JBColor.RED);
						LOG.error(ex);
					}

					flashToolCombo.setSelectedItem(null);
					flashToolCombo.addItemListener(flashToolItemListener);
					if(null!=origFlashTool) {
						flashToolCombo.setSelectedItem(origFlashTool);
					}
					if(null==flashToolCombo.getSelectedItem() && 0!=flashToolCombo.getItemCount()) {
						flashToolCombo.setSelectedIndex(0);
					}
				}
			}
		};

		final ItemListener platformItemListener = new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if(e.getStateChange()==ItemEvent.SELECTED) {
					device = null;
					deviceCombo.removeItemListener(deviceItemListener);
					flashToolCombo.removeItemListener(flashToolItemListener);
					deviceCombo.removeAllItems();
					flashToolCombo.removeAllItems();
					flashToolParamsPanel.removeAll();
					flashToolDownloadButton.setVisible(false);

					PlatformType platformType = (PlatformType) e.getItem();
					List<String> devices = ExecuteSettingsEditor.getDevices(toolkitPath, platformType);
					if(null!=devices) {
						for (String deviceId : devices) {
							deviceCombo.addItem(deviceId);
						}
					}

					deviceCombo.setSelectedItem(null);
					deviceCombo.addItemListener(deviceItemListener);
					if(null!=origDeviceId) {
						deviceCombo.setSelectedItem(origDeviceId);
					}
					if(null==deviceCombo.getSelectedItem() && 0!=deviceCombo.getItemCount()) {
						deviceCombo.setSelectedIndex(0);
					}
				}
			}
		};

		for(PlatformType platformType : PlatformType.values()) {
			if(PlatformType.STUB!=platformType) {
				platformCombo.addItem(platformType);
			}
		}
		platformCombo.setSelectedItem(null);
		platformCombo.addItemListener(platformItemListener);
		if(null!=origPlatformType) {
			platformCombo.setSelectedItem(origPlatformType);
		}
		if(null==platformCombo.getSelectedItem() && 0!=platformCombo.getItemCount()) {
			platformCombo.setSelectedIndex(0);
		}

		return FormBuilder.createFormBuilder()
			.addComponent(stdioPortsRow)
			.addComponent(platformRow)
			.addComponent(deviceRow)
			.addComponent(flashToolRow)
			.addComponent(flashToolParamsPanel)
			.addComponentFillVertically(new JPanel(), 0)
			.getPanel();
	}

	private JPanel createUidPanel() {
		uidModeGlobal = new JBRadioButton("Global (request to server)");
		uidModeLocal = new JBRadioButton("Local (use uids.db file)", true);
		uidModeParam = new JBRadioButton("Parameter (set manually)");

		uidModeGroup = new ButtonGroup();
		uidModeGroup.add(uidModeGlobal);
		uidModeGroup.add(uidModeLocal);
		uidModeGroup.add(uidModeParam);

		JPanel vendorRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		vendorField = new JBTextField();
		vendorField.setDocument(new HexDocumentFilter(4));
		vendorField.setPreferredSize(new Dimension(FIELD_WIDTH, vendorField.getPreferredSize().height));
		vendorField.getEmptyText().setText("2 bytes hex");
		JLabel label = new JLabel("Vendor ID:");
		label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
		vendorRow.add(label);
		vendorRow.add(vendorField);

		JPanel passwordRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		passwordField = new JBTextField();
		passwordField.setPreferredSize(new Dimension(FIELD_WIDTH, passwordField.getPreferredSize().height));
		passwordField.getEmptyText().setText("Password");
		label = new JLabel("Password:");
		label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
		passwordRow.add(label);
		passwordRow.add(passwordField);

		JPanel uidsFileRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		uidsFileField = new TextFieldWithBrowseButton();
		uidsFileField.setPreferredSize(new Dimension(FIELD_WIDTH, uidsFileField.getPreferredSize().height));
		uidsFileField.setText(localUidsPath.toString());
		label = new JLabel("UIDs file:");
		label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
		uidsFileRow.add(label);
		uidsFileRow.add(uidsFileField);
		uidsFileField.addActionListener(e -> {
			FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
				.withTitle("Select UIDs Database File")
				.withFileFilter(file -> file.getName().endsWith(".db"));

			VirtualFile file = FileChooser.chooseFile(descriptor, project, null);
			if (file != null) {
				uidsFileField.setText(file.getPath());
			}
		});

		JPanel uidRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		uidField = new JBTextField();
		uidField.setDocument(new HexDocumentFilter(16));
		uidField.setPreferredSize(new Dimension(FIELD_WIDTH, uidField.getPreferredSize().height));
		uidField.getEmptyText().setText("8 bytes hex, e.g., 2d849f626c42ff33");
		label = new JLabel("UID:");
		label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
		uidRow.add(label);
		uidRow.add(uidField);

		ActionListener radioListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (uidModeGlobal.isSelected()) {
					vendorRow.setVisible(true);
					passwordRow.setVisible(true);
					uidsFileRow.setVisible(false);
					uidRow.setVisible(false);
				} else if (uidModeLocal.isSelected()) {
					vendorRow.setVisible(false);
					passwordRow.setVisible(false);
					uidsFileRow.setVisible(true);
					uidRow.setVisible(false);
				} else if (uidModeParam.isSelected()) {
					vendorRow.setVisible(false);
					passwordRow.setVisible(false);
					uidsFileRow.setVisible(false);
					uidRow.setVisible(true);
				}
			}
		};
		uidModeGlobal.addActionListener(radioListener);
		uidModeLocal.addActionListener(radioListener);
		uidModeParam.addActionListener(radioListener);
		radioListener.actionPerformed(null);

		overwriteUidCheckbox = new JBCheckBox("Overwrite existing UID");

		JPanel uidPanel = FormBuilder.createFormBuilder()
			.addComponent(uidModeGlobal)
			.addComponent(uidModeLocal)
			.addComponent(uidModeParam)
			.addSeparator()
			.addComponent(uidRow)
			.addComponent(vendorRow)
			.addComponent(passwordRow)
			.addComponent(uidsFileRow)
			.addComponent(overwriteUidCheckbox)
			.addComponentFillVertically(new JPanel(), 0)
			.getPanel();

		return uidPanel;
	}

	private JPanel createSecurityPanel() {
		JPanel keyRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		keyField = new JBTextField();
		keyField.setDocument(new HexDocumentFilter(64));
		keyField.setPreferredSize(new Dimension(FIELD_WIDTH, keyField.getPreferredSize().height));
		keyField.getEmptyText().setText("32 bytes hex (auto-generated if empty)");
		JLabel label = new JLabel("Encryption key:");
		label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
		keyRow.add(label);
		keyRow.add(keyField);

		JPanel asmFileRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		secureAsmFileField = new TextFieldWithBrowseButton();
		secureAsmFileField.setPreferredSize(new Dimension(FIELD_WIDTH, secureAsmFileField.getPreferredSize().height));
		label = new JLabel("Secure ASM file:");
		label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
		asmFileRow.add(label);
		asmFileRow.add(secureAsmFileField);
		secureAsmFileField.addActionListener(e -> {
			FileChooserDescriptor descriptor = new FileChooserDescriptor(true, false, false, false, false, false)
				.withTitle("Select Secure ASM File")
				.withFileFilter(file -> file.getName().endsWith(".asm"));

			VirtualFile file = FileChooser.chooseFile(descriptor, project, null);
			if (file != null) {
				secureAsmFileField.setText(file.getPath());
			}
		});
		ContextHelpLabel secureAsmHelpLabel = ContextHelpLabel.create(
				"Specify the assembly file (for building into the bootloader) corresponding to your device, containing the firmware decryption " +
				"implementation based on the provided key. See the bootloader implementation in RTOS for reference (~/vm5277/rtos/avr/boot/bldr.asm)."
		);
		asmFileRow.add(Box.createRigidArea(new Dimension(5, 0)));
		asmFileRow.add(secureAsmHelpLabel);

		return FormBuilder.createFormBuilder()
			.addComponent(keyRow)
			.addComponent(asmFileRow)
			.addComponentFillVertically(new JPanel(), 0)
			.getPanel();
	}

	private JPanel createAdvancedPanel() {
		generateListingCheckbox = new JBCheckBox("Generate listing file (bldr.lst)");

		return FormBuilder.createFormBuilder()
			.addComponent(generateListingCheckbox)
			.addComponentFillVertically(new JPanel(), 0)
			.getPanel();
	}

	private JPanel createOutputPanel() {
		JPanel toolkitRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		toolkitPathField = new JBTextField();
		toolkitPathField.setPreferredSize(new Dimension(FIELD_WIDTH, toolkitPathField.getPreferredSize().height));
		toolkitPathField.setEditable(false);
		toolkitPathField.setText(toolkitPath.toString());
		JLabel label = new JLabel("Toolkit path:");
		label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
		toolkitRow.add(label);
		toolkitRow.add(toolkitPathField);

		JPanel mkbootCmdRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		mkbootCmdField = new JBTextField();
		mkbootCmdField.setPreferredSize(new Dimension(FIELD_WIDTH, mkbootCmdField.getPreferredSize().height));
		mkbootCmdField.setEditable(false);
		label = new JLabel("MkBoot cmd:");
		label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
		mkbootCmdRow.add(label);
		mkbootCmdRow.add(mkbootCmdField);

		JPanel flashCmdRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		flashCmdField = new JBTextField();
		flashCmdField.setPreferredSize(new Dimension(FIELD_WIDTH, flashCmdField.getPreferredSize().height));
		flashCmdField.setEditable(false);
		label = new JLabel("Flash cmd:");
		label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
		flashCmdRow.add(label);
		flashCmdRow.add(flashCmdField);

		logArea = new JTextArea();
		logArea.setEditable(false);
		logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		JScrollPane scrollPane = new JBScrollPane(logArea);

		scrollPane.setPreferredSize(new Dimension(600, 200));

		return FormBuilder.createFormBuilder()
			.addComponent(toolkitRow)
			.addComponent(mkbootCmdRow)
			.addComponent(flashCmdRow)
			.addComponentFillVertically(scrollPane, 0)
			.getPanel();
	}

	private void startFlashing() {
		if(outputPanel==tabbedPane.getSelectedComponent()) {
			generateCommands();
		}
		else {
			tabbedPane.setSelectedComponent(outputPanel);
		}

		if(!validateInputs()) {
			return;
		}

		logArea.setText("");
		flashButton.setEnabled(false);
		closeButton.setEnabled(false);
		flashing = true;

		new Thread(new Runnable() {
			@Override
			public void run() {
				Path tmpDirPath = null;
				try {
					tmpDirPath = toolkitPath.resolve("tmp_" + System.currentTimeMillis()).normalize();
					tmpDirPath.toFile().mkdir();
					tmpDirPath.toFile().deleteOnExit();

					appendLog("Executing: " + mkbootCmdField.getText() + "\n");
					ProcessBuilder mkBootPb = new ProcessBuilder(parseCommand(mkbootCmdField.getText()));
					mkBootPb.directory(tmpDirPath.toFile());
					mkBootPb.redirectErrorStream(true);
					Process mkbootProcess = mkBootPb.start();
					int exitCode = readProcessOutput(mkbootProcess);
					if (0 != exitCode) {
						appendLog("\n=== j8bmb failed with exit code " + exitCode + " ===\n");
						return;
					}

					appendLog("\n\nExecuting: " + flashCmdField.getText() + "\n");
					ProcessBuilder flashPb = new ProcessBuilder(parseCommand(flashCmdField.getText()));
					flashPb.directory(tmpDirPath.toFile());
					flashPb.redirectErrorStream(true);

					Process flashProcess = flashPb.start();
					exitCode = readProcessOutput(flashProcess);
					if (0 != exitCode) {
						appendLog("\n=== Flash failed with exit code " + exitCode + " ===\n");
						return;
					}
					appendLog("\n=== Flash completed successfully ===\n");

				} catch (Exception ex) {
					LOG.error(ex);
					appendLog("\n=== Error: " + ex.getMessage() + " ===\n");
				} finally {
					if(null!=tmpDirPath) {
						try {
							FSUtils.deleteDirectory(null, tmpDirPath.toFile());
						} catch (Exception ignored) {
						}
					}
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							flashButton.setEnabled(true);
							closeButton.setEnabled(true);
							flashing = false;
						}
					});
				}
			}
		}, "FlashBootloader-Thread").start();
	}

	private int readProcessOutput(Process process) throws IOException, InterruptedException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		String line;
		while((line=reader.readLine())!=null) {
			appendLog(line + "\n");
		}
		return process.waitFor();
	}

	private void appendLog(final String text) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				logArea.append(text);
				logArea.setCaretPosition(logArea.getDocument().getLength());
			}
		});
	}
	private void generateCommands() {
		try {
			Path j8bmbPath = toolkitPath.resolve("bin").resolve("j8bmb" + (FSUtils.isWindows() ? ".exe" : ".sh")).normalize();

			StringBuilder mkbootCmdSB = new StringBuilder(j8bmbPath.toString());
			mkbootCmdSB.append(" ").append(((PlatformType)platformCombo.getSelectedItem()).name().toLowerCase());
			mkbootCmdSB.append(":").append(((String)deviceCombo.getSelectedItem()).toLowerCase());
			mkbootCmdSB.append(" ").append(stdioPortFiled.getText().toLowerCase()).append(" ");
			if(uidModeGlobal.isSelected()) {
				mkbootCmdSB.append("-m global -a ").append(vendorField.getText().toLowerCase()).append(" \"").append(passwordField.getText()).append("\" ");
			}
			else if(uidModeLocal.isSelected()) {
				mkbootCmdSB.append("-m local -i \"").append(uidsFileField.getTextField().getText()).append("\" ");
			}
			else if(uidModeParam.isSelected()) {
				mkbootCmdSB.append("-m param -u ").append(uidField.getText()).append(" ");
			}
			if(overwriteUidCheckbox.isSelected()) {
				mkbootCmdSB.append("-o ");
			}
			if(!keyField.getText().trim().isEmpty()) {
				mkbootCmdSB.append("-k ").append(keyField.getText().trim().toLowerCase()).append(" -s \"");
				mkbootCmdSB.append(secureAsmFileField.getTextField().getText()).append("\" ");
			}
			if(generateListingCheckbox.isSelected()) {
				mkbootCmdSB.append("-l ");
			}

			mkbootCmdField.setText(mkbootCmdSB.toString());

			FlashTool flashTool = (FlashTool) flashToolCombo.getSelectedItem();
			StringBuilder flashCmdSB = new StringBuilder(flashTool.resolveTool(true).toString());
			String deviceId = flashTool.convertDeviceId((String) deviceCombo.getSelectedItem());
			flashCmdSB.append(" ");
			for(Component component : flashToolParamsPanel.getComponents()) {
				if(component instanceof JPanel) {
					JPanel panel = (JPanel) component;
					FlashToolParam param = (FlashToolParam) panel.getClientProperty("param");
					String value = (String) panel.getClientProperty("value");
					if(null!=param && null!=value && !value.trim().isEmpty()) {
						flashCmdSB.append(param.getArgName()).append(" \"").append(value).append("\" ");
					}
				}
			}
			flashCmdSB.append("-p ").append(deviceId).append(" ");
			flashCmdSB.append("-U flash:w:bldr_cseg.hex:i");
			flashCmdField.setText(flashCmdSB.toString());
		}
		catch (Exception ex) {
			LOG.error(ex);
		}
	}

	private boolean validateInputs() {

		StringBuilder errors = new StringBuilder();

		if(stdioPortFiled.getText().trim().isEmpty()) {
			errors.append(" - STDIO Port(s) is required\n");
		}

		if(null==platformCombo.getSelectedItem()) {
			errors.append(" - platform is required\n");
		}

		if(null==deviceCombo.getSelectedItem()) {
			errors.append(" - device is required\n");
		}
		else if(null==device || !device.allowsBootloader()) {
			errors.append(" - selected device does not support bootloader\n");
		}

		if(null==flashToolCombo.getSelectedItem()) {
			errors.append(" - flash Tool is required\n");
		}
		else {
			FlashTool tool = (FlashTool) flashToolCombo.getSelectedItem();
			if (null==tool.resolveTool(false)) {
				errors.append(" - flash Tool is not available (download required)\n");
			}
		}

		if(uidModeParam.isSelected()) {
			String uid = uidField.getText().trim();
			if(uid.isEmpty()) {
				errors.append(" - UID is required\n");
			}
			else if(0x10!=uid.length()) {
				errors.append(" - UID must be exactly 8 bytes (16 hex chars)\n");
			}
		}
		else if(uidModeGlobal.isSelected()) {
			String vendorId = vendorField.getText().trim();
			if(vendorId.isEmpty()) {
				errors.append(" - vendor ID is required\n");
			}
			else if(0x04!=vendorId.length()) {
				errors.append(" - vendor ID must be exactly 2 bytes (4 hex chars)\n");
			}
			if(passwordField.getText().trim().isEmpty()) {
				errors.append(" - password is required\n");
			}
		}
		else if(uidModeLocal.isSelected()) {
			if(uidsFileField.getText().trim().isEmpty()) {
				errors.append(" - UIDs file is required\n");
			}
		}

		boolean valid = (0==errors.length());

		if(!valid) {
			logArea.setText("Bootloader flashing aborted, incorrect form data:\n" + errors.toString());
		}
		else {
			logArea.setText("");
		}

		return valid;
	}

	private String[] parseCommand(String command) {
		List<String> parts = new ArrayList<>();
		StringBuffer current = new StringBuffer();
		boolean inQuotes = false;

		for(int i=0; i<command.length(); i++) {
			char c = command.charAt(i);

			if('"'==c) {
				inQuotes = !inQuotes;
			}
			else if(' '==c && !inQuotes) {
				if(0<current.length()) {
					parts.add(current.toString());
					current = new StringBuffer();
				}
			}
			else {
				current.append(c);
			}
		}

		if(current.length() > 0) {
			parts.add(current.toString());
		}

		return parts.toArray(new String[0]);
	}

	@Override
	protected void dispose() {
		disposed=true;
		super.dispose();
	}
}