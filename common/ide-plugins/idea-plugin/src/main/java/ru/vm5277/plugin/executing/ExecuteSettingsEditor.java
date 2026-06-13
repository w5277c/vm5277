package ru.vm5277.plugin.executing;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.ContextHelpLabel;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBRadioButton;
import com.intellij.ui.components.JBTextField;
import com.intellij.ui.components.panels.VerticalLayout;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import ru.vm5277.common.FSUtils;
import ru.vm5277.common.Platform;
import ru.vm5277.common.StrUtils;
import ru.vm5277.common.Toolkit;
import ru.vm5277.common.enums.OptimizationType;
import ru.vm5277.common.enums.PlatformType;
import ru.vm5277.common.enums.StrictLevel;
import ru.vm5277.common.flash.FlashTool;
import ru.vm5277.common.flash.FlashToolParam;
import ru.vm5277.common.flash.FlashToolProvider;
import ru.vm5277.common.flash.InternalFlashTool;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class ExecuteSettingsEditor extends SettingsEditor<ExecuteConfiguration> {
	public static final int LABEL_WIDTH = 150;
	public static final int FIELD_WIDTH = 340;

	public final static String DOWNLOADING_BUTTON_TEXT = "Downloading...";
	public final static String DOWNLOAD_BUTTON_TEXT = "Download";
	public final static String RETRY_DOWNLOAD_BUTTON_TEXT = "Retry download";
	private final static Logger LOG = Logger.getInstance(ExecuteSettingsEditor.class);
	private final Path toolkitPath = FSUtils.getToolkitPath();

	private final Project project;
	private volatile boolean disposed = false;

	// UI Components
	private final JBTextField toolkitPathField = new JBTextField();
	private final JButton toolkitDownloadButton = new JButton(DOWNLOAD_BUTTON_TEXT);
	private JPanel bodyPanel;
	private final ComboBox<RunMode> runModeCombo = new ComboBox<>();
	private final JBTextField cpuFreqFiled = new JBTextField();
	private final JBTextField stdioPortFiled = new JBTextField();
	private final JPanel targetTitleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
	private final JButton makeBootloaderButton = new JButton("Flash vm5277 bootloader");
	private final JBRadioButton autodetectCheckbox = new JBRadioButton("Autodetect target");
	private final JPanel autodetectRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
	private final JBRadioButton specifyTargetCheckbox = new JBRadioButton("Specify target");
	private final ButtonGroup targetGroup = new ButtonGroup();
	private final JPanel specifyTargetPanel = new JPanel(new VerticalLayout(5));
	private final ComboBox<PlatformType> platformCombo = new ComboBox<>();
	private final ComboBox<String> deviceCombo = new ComboBox<>();
	private final ComboBox<FlashTool> flashToolCombo = new ComboBox<>();
	private final JButton flashToolDownloadButton = new JButton(DOWNLOAD_BUTTON_TEXT);
	private final JPanel flashToolParamsPanel = new JPanel(new VerticalLayout(5));
	private final ComboBox<String> optimizationTypeCombo = new ComboBox<>();
	private final ComboBox<String> strictLevelCombo = new ComboBox<>();
	private final JBCheckBox softResetCheckbox = new JBCheckBox("Soft reset feature");
	private final JBCheckBox bldrApiReuseCheckbox = new JBCheckBox("Bootloader API reuse");
	private final JBCheckBox verboseOutputCheckbox = new JBCheckBox("Verbose output");
	private final JBCheckBox syncPomCheckbox = new JBCheckBox("Sync. POM ");

	private ItemListener platformItemListener;
	private ItemListener deviceItemListener;
	private ItemListener flashToolItemListener;

	public ExecuteSettingsEditor(Project project) {
		this.project = project;

		for (RunMode mode : RunMode.values()) {
			runModeCombo.addItem(mode);
		}

		for (OptimizationType optimizationType : OptimizationType.values()) {
			optimizationTypeCombo.addItem(optimizationType.name().toLowerCase());
		}

		for (StrictLevel strictLevel : StrictLevel.values()) {
			strictLevelCombo.addItem(strictLevel.name().toLowerCase());
		}
	}

	@Override
	@NotNull
	protected JComponent createEditor() {
		boolean toolkitExist = (null != toolkitPath && toolkitPath.toFile().exists() && toolkitPath.toFile().isDirectory());

		JPanel toolkitPathRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		toolkitPathField.setPreferredSize(new Dimension(!toolkitExist ? FIELD_WIDTH / 2 : FIELD_WIDTH, toolkitPathField.getPreferredSize().height));
		toolkitPathField.setEditable(false);
		toolkitPathField.setText(null == toolkitPath ? "" : toolkitPath.toString());
		JLabel label = new JLabel("Toolkit path:");
		label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
		toolkitPathRow.add(label);
		toolkitPathRow.add(toolkitPathField);
		toolkitPathRow.add(toolkitDownloadButton);

		toolkitDownloadButton.setVisible(!toolkitExist);
		toolkitDownloadButton.addActionListener(e -> {
			toolkitDownloadButton.setEnabled(false);
			toolkitDownloadButton.setText("Downloading...");
			new Thread(new Runnable() {
				@Override
				public void run() {
					ru.vm5277.common.Logger log = new ru.vm5277.common.Logger() {
						@Override
						public void info(String s) {
							LOG.info(s);
						}

						@Override
						public void warn(String s) {
							LOG.warn(s);
						}

						@Override
						public void error(String s) {
							LOG.error(s);
						}

						@Override
						public void debug(String s) {
							LOG.debug(s);
						}

						@Override
						public boolean progress(int percents, String status) {
							SwingUtilities.invokeLater(() -> {
								if (!disposed) {
									toolkitDownloadButton.setText(status + "... " + percents + "%");
								}
							});
							return !disposed;
						}
					};
					boolean success = Toolkit.checkToolkit(log, toolkitPath, Toolkit.DONWLOAD_URL, true);
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							toolkitDownloadButton.setEnabled(true);
							if (success) {
								toolkitDownloadButton.setVisible(false);
								toolkitDownloadButton.setText(DOWNLOAD_BUTTON_TEXT);
								bodyPanel.setVisible(true);
								toolkitPathField.setPreferredSize(new Dimension(FIELD_WIDTH, toolkitPathField.getPreferredSize().height));
							} else {
								toolkitDownloadButton.setText(RETRY_DOWNLOAD_BUTTON_TEXT);
							}
						}
					});
				}
			}).start();
		});

		JPanel runModeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		runModeCombo.setPreferredSize(new Dimension(FIELD_WIDTH, runModeCombo.getPreferredSize().height));
		label = new JLabel("Run mode:");
		label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
		runModeRow.add(label);
		runModeRow.add(runModeCombo);

		JPanel cpuFreqRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		cpuFreqFiled.setPreferredSize(new Dimension(FIELD_WIDTH, cpuFreqFiled.getPreferredSize().height));
		cpuFreqFiled.setDocument(new FloatDocumentFilter());
		cpuFreqFiled.getEmptyText().setText("E.g., 8 or 9.6 or 16 and other");
		label = new JLabel("CPU Frequency (MHz):");
		label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
		ContextHelpLabel helpLabel = ContextHelpLabel.create(
			"CPU frequency in MHz. " +
				"Used for UART baud rate calculation during autodetection and for RTOS timer configuration. " +
				"Accepts decimal values (e.g., 8.0, 9.6, 16.0)."
		);
		cpuFreqRow.add(label);
		cpuFreqRow.add(cpuFreqFiled);
		cpuFreqRow.add(Box.createRigidArea(new Dimension(5, 0)));
		cpuFreqRow.add(helpLabel);

		label = new JLabel("Target device");
		label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
		targetTitleRow.add(label);
		makeBootloaderButton.setForeground(JBColor.RED);
		makeBootloaderButton.setIcon(AllIcons.General.Gear);
		makeBootloaderButton.setHorizontalTextPosition(SwingConstants.LEFT);
		makeBootloaderButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (toolkitExist) {
					FlashBootloaderDialog dialog = new FlashBootloaderDialog(project, toolkitPath, stdioPortFiled.getText(),
						(PlatformType) platformCombo.getSelectedItem(), (String) deviceCombo.getSelectedItem(), (FlashTool) flashToolCombo.getSelectedItem());
					dialog.setResizable(false);
					dialog.show();
				}
			}
		});
		targetTitleRow.add(makeBootloaderButton);

		autodetectRow.add(autodetectCheckbox);
		autodetectRow.add(Box.createRigidArea(new Dimension(5, 0)));
		autodetectRow.add(ContextHelpLabel.create("Autodetect target modes required vm5277 bootloader"));
		targetGroup.add(autodetectCheckbox);
		targetGroup.add(specifyTargetCheckbox);

		JPanel stdioPortRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		stdioPortFiled.setPreferredSize(new Dimension(FIELD_WIDTH, stdioPortFiled.getPreferredSize().height));
		stdioPortFiled.setDocument(new PortDocumentFilter());
		stdioPortFiled.getEmptyText().setText("PA4/PC2 or RX or RX/TX or PB0 or RESET ...");
		label = new JLabel("STDIO Port(s):");
		label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
		helpLabel = ContextHelpLabel.create(
			"Specify device pins for internal flasher and communication or leave empty.\n" +
				"For two-wire mode use format: RX/TX. For single-wire mode: RX.\n" +
				"Most commonly used pins are RX/TX or RX.\n" +
				"Pin names can be found in the bootloader flashing utility report."
		);
		stdioPortRow.add(label);
		stdioPortRow.add(stdioPortFiled);
		stdioPortRow.add(Box.createRigidArea(new Dimension(5, 0)));
		stdioPortRow.add(helpLabel);

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

		JPanel flashToolRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		flashToolCombo.setPreferredSize(new Dimension(FIELD_WIDTH, flashToolCombo.getPreferredSize().height));
		label = new JLabel("Flash tool:");
		label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
		flashToolDownloadButton.setVisible(false);
		flashToolRow.add(label);
		flashToolRow.add(flashToolCombo);
		flashToolRow.add(flashToolDownloadButton);

		flashToolItemListener = new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					softResetCheckbox.setEnabled(false);
					bldrApiReuseCheckbox.setEnabled(false);

					FlashTool flashTool = (FlashTool) e.getItem();
					if(flashTool instanceof InternalFlashTool) {
						softResetCheckbox.setEnabled(true);
						bldrApiReuseCheckbox.setEnabled(true);
					}
					else {
						softResetCheckbox.setSelected(false);
						bldrApiReuseCheckbox.setSelected(false);
					}
					Path toolPath = null == toolkitPath ? null : flashTool.resolveTool(false);
					flashToolDownloadButton.setVisible(null == toolPath);
					flashToolCombo.setPreferredSize(new Dimension(null == toolPath ? FIELD_WIDTH / 2 : FIELD_WIDTH, flashToolCombo.getPreferredSize().height));
					flashToolParamsPanel.removeAll();
					for (FlashToolParam param : flashTool.getParams()) {
						flashToolParamsPanel.add(flashToolParamToUIComponent(param, LABEL_WIDTH, FIELD_WIDTH));
					}
					flashToolParamsPanel.revalidate();
					flashToolParamsPanel.repaint();
				}
			}
		};

		deviceItemListener = new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					FlashTool oldFlashTool = (FlashTool) flashToolCombo.getSelectedItem();
					flashToolCombo.removeItemListener(flashToolItemListener);
					flashToolCombo.removeAllItems();
					flashToolParamsPanel.removeAll();
					flashToolDownloadButton.setVisible(false);

					String deviceId = (String) e.getItem();
					if (toolkitExist) {
						for (FlashTool flashTool : FlashToolProvider.getTools(null, toolkitPath, (PlatformType) platformCombo.getSelectedItem(), deviceId)) {
							flashToolCombo.addItem(flashTool);
						}
					}

					flashToolCombo.setSelectedItem(null);
					flashToolCombo.addItemListener(flashToolItemListener);
					if (null != oldFlashTool) {
						for (int i = 0; i < flashToolCombo.getItemCount(); i++) {
							if (((FlashTool) flashToolCombo.getItemAt(i)).getName().equalsIgnoreCase(oldFlashTool.getName())) {
								flashToolCombo.setSelectedIndex(i);
							}
						}
					}
					if (null == flashToolCombo.getSelectedItem() && 0 != flashToolCombo.getItemCount()) {
						flashToolCombo.setSelectedIndex(0);
					}
				}
			}
		};

		platformItemListener = new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (e.getStateChange() == ItemEvent.SELECTED) {
					String oldDeviceId = (String) deviceCombo.getSelectedItem();

					deviceCombo.removeItemListener(deviceItemListener);
					flashToolCombo.removeItemListener(flashToolItemListener);
					deviceCombo.removeAllItems();
					flashToolCombo.removeAllItems();
					flashToolParamsPanel.removeAll();
					flashToolDownloadButton.setVisible(false);

					PlatformType platformType = (PlatformType) e.getItem();
					List<String> devices = getDevices(toolkitPath, platformType);
					if (null != devices) {
						for (String deviceId : devices) {
							deviceCombo.addItem(deviceId);
						}
					}

					deviceCombo.setSelectedItem(null);
					deviceCombo.addItemListener(deviceItemListener);
					if (null != oldDeviceId) {
						deviceCombo.setSelectedItem(oldDeviceId);
					}
					if (null == deviceCombo.getSelectedItem() && 0 != deviceCombo.getItemCount()) {
						deviceCombo.setSelectedIndex(0);
					}
				}
			}
		};

		autodetectCheckbox.setSelected(true);
		ItemListener specifyTargetCBListener = new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				boolean isSelected = specifyTargetCheckbox.isSelected();
				specifyTargetPanel.setVisible(isSelected);
				platformCombo.removeItemListener(platformItemListener);
				deviceCombo.removeItemListener(deviceItemListener);
				flashToolCombo.removeItemListener(flashToolItemListener);
				platformCombo.removeAllItems();
				deviceCombo.removeAllItems();
				flashToolCombo.removeAllItems();
				flashToolDownloadButton.setVisible(false);

				if (isSelected) {
					softResetCheckbox.setEnabled(false);
					bldrApiReuseCheckbox.setEnabled(false);

					PlatformType oldPaltdormType = (PlatformType) platformCombo.getSelectedItem();

					for (PlatformType platformType : PlatformType.values()) {
						if (PlatformType.STUB != platformType) {
							platformCombo.addItem(platformType);
						}
					}
					platformCombo.setSelectedItem(null);
					platformCombo.addItemListener(platformItemListener);
					if (null != oldPaltdormType) {
						platformCombo.setSelectedItem(oldPaltdormType);
					}
					if (null == platformCombo.getSelectedItem() && 0 != platformCombo.getItemCount()) {
						platformCombo.setSelectedIndex(0);
					}
				}
				else {
					softResetCheckbox.setEnabled(true);
					bldrApiReuseCheckbox.setEnabled(true);
				}
			}
		};
		specifyTargetCheckbox.addItemListener(specifyTargetCBListener);

		specifyTargetPanel.setVisible(false);
		specifyTargetPanel.add(stdioPortRow);
		specifyTargetPanel.add(platformRow);
		specifyTargetPanel.add(deviceRow);
		specifyTargetPanel.add(flashToolRow);
		specifyTargetPanel.add(flashToolParamsPanel);

		flashToolDownloadButton.addActionListener((ActionEvent e) -> {
			flashToolDownloadButton.setEnabled(false);
			flashToolDownloadButton.setText(DOWNLOADING_BUTTON_TEXT);
			new Thread(new Runnable() {
				@Override
				public void run() {
					ru.vm5277.common.Logger log = new ru.vm5277.common.Logger() {
						@Override
						public void info(String s) {
							LOG.info(s);
						}

						@Override
						public void warn(String s) {
							LOG.warn(s);
						}

						@Override
						public void error(String s) {
							LOG.error(s);
						}

						@Override
						public void debug(String s) {
							LOG.debug(s);
						}

						@Override
						public boolean progress(int percents, String status) {
							SwingUtilities.invokeLater(() -> {
								if (!disposed) {
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
							if (success) {
								flashToolDownloadButton.setVisible(false);
								flashToolDownloadButton.setText(ExecuteSettingsEditor.DOWNLOAD_BUTTON_TEXT);
								flashToolCombo.setPreferredSize(new Dimension(FIELD_WIDTH, flashToolCombo.getPreferredSize().height));
							} else {
								flashToolDownloadButton.setText(ExecuteSettingsEditor.RETRY_DOWNLOAD_BUTTON_TEXT);
							}
						}
					});
				}
			}).start();
		});

		JPanel optimizationTypeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		optimizationTypeCombo.setPreferredSize(new Dimension(FIELD_WIDTH, optimizationTypeCombo.getPreferredSize().height));
		label = new JLabel("Optimization type:");
		label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
		optimizationTypeRow.add(label);
		optimizationTypeRow.add(optimizationTypeCombo);

		JPanel strictLevelRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		strictLevelCombo.setPreferredSize(new Dimension(FIELD_WIDTH, strictLevelCombo.getPreferredSize().height));
		label = new JLabel("Strict level:");
		label.setPreferredSize(new Dimension(LABEL_WIDTH, label.getPreferredSize().height));
		strictLevelRow.add(label);
		strictLevelRow.add(strictLevelCombo);

		JPanel softResetRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		helpLabel = ContextHelpLabel.create("Enables UART-triggered reboot into bootloader mode (no physical reset required). Useful during " +
											"development for faster flash cycles. Slightly increases firmware size. Works with VM5277 bootloader only.");
		softResetCheckbox.setSelected(true);
		softResetRow.add(softResetCheckbox);
		softResetRow.add(Box.createRigidArea(new Dimension(5, 0)));
		softResetRow.add(helpLabel);

		JPanel bldrApiReuseRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		helpLabel = ContextHelpLabel.create("Reduces firmware size by reusing bootloader routines. Works with VM5277 bootloader only.");
		bldrApiReuseCheckbox.setSelected(true);
		bldrApiReuseRow.add(bldrApiReuseCheckbox);
		bldrApiReuseRow.add(Box.createRigidArea(new Dimension(5, 0)));
		bldrApiReuseRow.add(helpLabel);

		JPanel verboseOutputRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		helpLabel = ContextHelpLabel.create("Show detailed build and generates additional files useful for project debugging");
		verboseOutputRow.add(verboseOutputCheckbox);
		verboseOutputRow.add(Box.createRigidArea(new Dimension(5, 0)));
		verboseOutputRow.add(helpLabel);

		JPanel pomExportRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		helpLabel = ContextHelpLabel.create("Sync with Maven POM: pom → configuration on project open, configuration → pom on apply");
		pomExportRow.add(syncPomCheckbox);
		pomExportRow.add(Box.createRigidArea(new Dimension(5, 0)));
		pomExportRow.add(helpLabel);

		FormBuilder builder = FormBuilder.createFormBuilder()
			.setVerticalGap(5)
			.addComponent(runModeRow)
			.addSeparator()
			.addComponent(cpuFreqRow)
			.addSeparator()
			.addComponent(targetTitleRow)
			.addComponent(autodetectRow)
			.addComponent(specifyTargetCheckbox)
			.addComponent(specifyTargetPanel)
			.addSeparator()
			.addComponent(optimizationTypeRow)
			.addComponent(strictLevelRow)
			.addComponent(softResetRow)
			.addComponent(bldrApiReuseRow)
			.addComponent(verboseOutputRow)
			.addComponent(pomExportRow);
		bodyPanel = builder.getPanel();

		builder = FormBuilder.createFormBuilder();
		builder.setVerticalGap(5);
		builder.addComponent(toolkitPathRow);
		builder.addSeparator();
		builder.addComponent(bodyPanel);

		bodyPanel.setVisible(toolkitExist);
		return builder.getPanel();
	}

	@Override
	public void disposeEditor() {
		disposed = true;
		super.disposeEditor();
	}

	@Override
	protected void resetEditorFrom(@NotNull ExecuteConfiguration configuration) {
		runModeCombo.setSelectedItem(configuration.getRunMode());
		cpuFreqFiled.setText(Double.toString(configuration.getCpuFreq() / 1000d));
		stdioPortFiled.setText(configuration.getStdioPort());
		String targetMode = configuration.getTargetMode();

		if(!targetMode.equals("specify")) {
			autodetectCheckbox.setSelected(true);
		}
		else {
			specifyTargetCheckbox.setSelected(true);

			PlatformType platformType = configuration.getPlatformType();
			boolean exist = false;
			for (int i = 0; i < platformCombo.getItemCount(); i++) {
				if (platformCombo.getItemAt(i) == platformType) {
					exist = true;
					break;
				}
			}
			if (exist) {
				platformCombo.setSelectedItem(platformType);

				String deviceId = configuration.getDeviceId();
				exist = false;
				for (int i = 0; i < deviceCombo.getItemCount(); i++) {
					if (Objects.equals(deviceCombo.getItemAt(i), deviceId)) {
						exist = true;
						break;
					}
				}
				if (exist) {
					deviceCombo.setSelectedItem(deviceId);
					String flashToolName = configuration.getFlashToolName();
					exist = false;
					for (int i = 0; i < flashToolCombo.getItemCount(); i++) {
						if (((FlashTool) flashToolCombo.getItemAt(i)).getName().equalsIgnoreCase(flashToolName)) {
							flashToolCombo.setSelectedIndex(i);
							exist = true;
							break;
						}
					}
					if (exist) {
						//TODO
					}
				}
			}
		}

		optimizationTypeCombo.setSelectedItem(configuration.getOptimizationType().toLowerCase());
		strictLevelCombo.setSelectedItem(configuration.getStrictLevel().toLowerCase());

		softResetCheckbox.setSelected(configuration.isSoftReset());
		bldrApiReuseCheckbox.setSelected(configuration.isBldrApiReuse());
		verboseOutputCheckbox.setSelected(configuration.isVerboseOutput());
	}

	@Override
	protected void applyEditorTo(@NotNull ExecuteConfiguration configuration) throws ConfigurationException {
		configuration.setRunMode((RunMode) runModeCombo.getSelectedItem());

		String cpuFreq = cpuFreqFiled.getText();
		if(!FloatDocumentFilter.isValid(cpuFreq)) throw new ConfigurationException("Invalid CPU Freq: " + cpuFreq);
		configuration.setCpuFreq((int) (Double.parseDouble(cpuFreq) * 1000));

		String stdioPort = stdioPortFiled.getText().trim();
		if(!PortDocumentFilter.isValid(stdioPort)) throw new ConfigurationException("Invalid STDIO Port(s): " + stdioPort);
		configuration.setStdioPort(stdioPort);

		if(autodetectCheckbox.isSelected()) {
			configuration.setTargetMode("auto");
			configuration.setPlatformType(null);
			configuration.setDeviceId("");
			configuration.setFlashToolName("");
		}
		else {
			configuration.setTargetMode("specify");

			PlatformType platformType = (PlatformType) platformCombo.getSelectedItem();
			if(null==platformType) throw new ConfigurationException("Empty platform type");
			configuration.setPlatformType(platformType);

			String deviceId = (String) deviceCombo.getSelectedItem();
			if(null==deviceId || deviceId.trim().isEmpty()) throw new ConfigurationException("Empty device");
			configuration.setDeviceId(deviceId.trim());

			FlashTool flashTool = (FlashTool) flashToolCombo.getSelectedItem();
			if(null==flashTool) throw new ConfigurationException("Empty flash tool");
			configuration.setFlashToolName(flashTool.getName());

			StringBuilder sb = new StringBuilder();
			if(!flashTool.getParams().isEmpty()) {
				//Проще сконвертировать в Base64, чем разобраться в лютой логике IDEA
				for(FlashToolParam param : flashTool.getParams()) {
					String str = (null==param.getArgName() ? param.getName() : param.getArgName() + "=" + param.getValue());
					sb.append(StrUtils.printBase64Binary(str.getBytes(StandardCharsets.UTF_8))).append(",");
				}
				sb.deleteCharAt(sb.length()-0x01);
			}
			configuration.setFlashToolParams(sb.toString());
		}

		configuration.setOptimizationType(((String)optimizationTypeCombo.getSelectedItem()).toLowerCase());
		configuration.setStrictLevel(((String) strictLevelCombo.getSelectedItem()).toLowerCase());

		configuration.setSoftReset(softResetCheckbox.isEnabled() && softResetCheckbox.isSelected());
		configuration.setBldrApiReuse(bldrApiReuseCheckbox.isEnabled() && bldrApiReuseCheckbox.isSelected());
		configuration.setVerboseOutput(verboseOutputCheckbox.isSelected());
		configuration.setSyncPom(syncPomCheckbox.isSelected());
	}

	public static List<String> getDevices(Path toolkitPath, PlatformType platformType) {
		Path rtosPath = toolkitPath.resolve("rtos").normalize();
		switch (platformType) {
			case AVR:
				try {
					return new Platform(rtosPath.resolve(platformType.name().toLowerCase()).normalize(), platformType).getSupportedDeviceIds();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				break;
		}
		return null;
	}


	public static JComponent flashToolParamToUIComponent(FlashToolParam param, int labelWidth, int fieldWidth) {
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		row.putClientProperty("param", param);
		JLabel label = new JLabel(param.getName() + ":");
		label.setPreferredSize(new Dimension(labelWidth, label.getPreferredSize().height));
		row.add(label);
		if (null == param.getValues()) {
			JTextField textField = new JTextField();
			textField.setPreferredSize(new Dimension(fieldWidth, textField.getPreferredSize().height));
			textField.addFocusListener(new FocusAdapter() {
				@Override
				public void focusLost(FocusEvent e) {
					row.putClientProperty("value", textField.getText());
				}
			});
			row.add(textField);
		} else {
			JComboBox<String> combo = new JComboBox<String>();
			combo.setPreferredSize(new Dimension(fieldWidth, combo.getPreferredSize().height));
			for (String val : param.getValues()) {
				combo.addItem(val);
			}
			combo.setSelectedItem(param.getValue());
			row.putClientProperty("value", (String) param.getValue());

			combo.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(ItemEvent e) {
					if (e.getStateChange() == ItemEvent.SELECTED) {
						row.putClientProperty("value", (String) e.getItem());
					}
				}
			});
			row.add(combo);
		}
		return row;
	}
}
