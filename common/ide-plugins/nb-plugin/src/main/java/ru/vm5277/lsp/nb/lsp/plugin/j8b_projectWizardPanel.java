/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/NetBeansModuleDevelopment-files/templateWizardPanel.java to edit this template
 */
package ru.vm5277.lsp.nb.lsp.plugin;

import java.awt.Component;
import java.util.HashSet;
import java.util.Set;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;
import org.openide.WizardValidationException;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

/**
 * Panel just asking for basic info.
 */
public class j8b_projectWizardPanel implements WizardDescriptor.Panel,
		WizardDescriptor.ValidatingPanel, WizardDescriptor.FinishablePanel {

	private WizardDescriptor wizardDescriptor;
	private j8b_projectPanelVisual component;

	public j8b_projectWizardPanel() {
	}

	public Component getComponent() {
		if (component == null) {
			component = new j8b_projectPanelVisual(this);
			component.setName(NbBundle.getMessage(j8b_projectWizardPanel.class, "LBL_CreateProjectStep"));
		}
		return component;
	}

	public HelpCtx getHelp() {
		return new HelpCtx(j8b_projectWizardPanel.class);
	}

	public boolean isValid() {
		getComponent();
		return component.valid(wizardDescriptor);
	}

	private final Set<ChangeListener> listeners = new HashSet<ChangeListener>(1); // or can use ChangeSupport in NB 6.0

	public final void addChangeListener(ChangeListener l) {
		synchronized (listeners) {
			listeners.add(l);
		}
	}

	public final void removeChangeListener(ChangeListener l) {
		synchronized (listeners) {
			listeners.remove(l);
		}
	}

	protected final void fireChangeEvent() {
		Set<ChangeListener> ls;
		synchronized (listeners) {
			ls = new HashSet<ChangeListener>(listeners);
		}
		ChangeEvent ev = new ChangeEvent(this);
		for (ChangeListener l : ls) {
			l.stateChanged(ev);
		}
	}

	public void readSettings(Object settings) {
		wizardDescriptor = (WizardDescriptor) settings;
		component.read(wizardDescriptor);
	}

	public void storeSettings(Object settings) {
		WizardDescriptor d = (WizardDescriptor) settings;
		component.store(d);
	}

	public boolean isFinishPanel() {
		return true;
	}

	public void validate() throws WizardValidationException {
		getComponent();
		component.validate(wizardDescriptor);
	}

}
