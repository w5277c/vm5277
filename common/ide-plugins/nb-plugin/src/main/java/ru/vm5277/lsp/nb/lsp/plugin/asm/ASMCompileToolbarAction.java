/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package ru.vm5277.lsp.nb.lsp.plugin.asm;

import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Arrays;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import javax.swing.AbstractAction;
import static javax.swing.Action.NAME;
import static javax.swing.Action.SMALL_ICON;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import org.netbeans.api.project.FileOwnerQuery;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.filesystems.FileObject;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.netbeans.api.project.Project;
import org.openide.loaders.DataObject;
import org.openide.util.Utilities;
import org.openide.windows.WindowManager;
import org.netbeans.spi.project.ActionProvider;
import org.openide.awt.DropDownButtonFactory;
import org.openide.awt.ToolbarPool;
import org.openide.util.NbPreferences;
import org.openide.util.actions.Presenter;
import org.openide.util.lookup.Lookups;

@ActionID(
    category = "Build",
    id = "ru.vm5277.lsp.nb.lsp.plugin.asm.CompileASMToolbarAction"
)
@ActionRegistration(
    displayName = "#CTL_CompileASMToolbarAction",
	lazy = false
)
@ActionReference(
    path = "Toolbars/Build",
    position = 1000
)

@NbBundle.Messages("CTL_CompileASMToolbarAction=Compile ASM")
public class ASMCompileToolbarAction extends ASMCompileAction implements Presenter.Toolbar {
	private	JMenuItem	compileItem;
	private	JMenuItem	runItem;
	private	boolean		isSmallIcons = false;
	
	public ASMCompileToolbarAction() {
		putValue(NAME, "ASM");
		putValue(SHORT_DESCRIPTION, "ASM Actions");
	
		WindowManager.getDefault().getRegistry().addPropertyChangeListener(evt -> {
            if ("activated".equals(evt.getPropertyName())) {
                updateState();
            }
        });
	}
	
	
	 @Override
    public Component getToolbarPresenter() {
        // Создаем иконку
        ImageIcon icon = ImageUtilities.loadImageIcon("ru/vm5277/lsp/nb/lsp/plugin/buildASM24.png", false);
        
        // Создаем меню
        JPopupMenu menu = createPopupMenu();
        
        // Используем фабрику NetBeans для создания стандартной кнопки с "стрелочкой"
        // Она сама откроет меню при нажатии на стрелку
        final JButton dropDownButton = DropDownButtonFactory.createDropDownButton(new ImageIcon(createCombinedIcon(icon.getImage(), false)), menu);
        
        // Привязываем основное действие (клик по самой кнопке)
        dropDownButton.addActionListener(e -> {
            // Логика по умолчанию (например, компиляция)
            runMvnTask("buildAsm");
        });

        // Синхронизация состояния (enable/disable)
        this.addPropertyChangeListener(evt -> {
            if ("enabled".equals(evt.getPropertyName())) {
                dropDownButton.setEnabled((boolean) evt.getNewValue());
            }
        });
        
		dropDownButton.setEnabled(false);
		
		dropDownButton.addPropertyChangeListener("PreferredIconSize", new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				boolean isNewSmallIcons = (null==evt.getNewValue() || 24!=(Integer)evt.getNewValue());
				if(isNewSmallIcons!=isSmallIcons) {
					isSmallIcons = isNewSmallIcons;
					ImageIcon buildIcon = ImageUtilities.loadImageIcon("ru/vm5277/lsp/nb/lsp/plugin/buildASM" + (isSmallIcons ? ".png" : "24.png"), false);
					if(null!=buildIcon) {
						if(!isEnabled()) { // Костыль. так и не нашел решения как заставить UI перерисовать disabled кнопку с новой иконкой
							dropDownButton.setDisabledIcon(null);
						}
						dropDownButton.setIcon(new ImageIcon(createCombinedIcon(buildIcon.getImage(), isSmallIcons)));

						compileItem.setIcon(ImageUtilities.loadImageIcon("ru/vm5277/lsp/nb/lsp/plugin/buildASM" + (isSmallIcons ? ".png" : "24.png"), false));
						runItem.setIcon(ImageUtilities.loadImageIcon("ru/vm5277/lsp/nb/lsp/plugin/runASM" + (isSmallIcons ? ".png" : "24.png"), false));
					}
				}
			}
		});
		
        return dropDownButton;
    }
	
	private JPopupMenu createPopupMenu() {
        JPopupMenu result = new JPopupMenu();
        
        // Пункт "Compile ASM"
        compileItem = new JMenuItem(new AbstractAction("Compile ASM") {
            @Override
            public void actionPerformed(ActionEvent e) {
                runMvnTask("buildAsm");
            }
        });
        compileItem.setIcon(ImageUtilities.loadImageIcon("ru/vm5277/lsp/nb/lsp/plugin/buildASM24.png", false));
        result.add(compileItem);
        
        // Пункт "Run ASM" (прошить)
        runItem = new JMenuItem(new AbstractAction("Run ASM") {
            @Override
            public void actionPerformed(ActionEvent e) {
                runMvnTask("runAsm");
            }
        });
        runItem.setIcon(ImageUtilities.loadImageIcon("ru/vm5277/lsp/nb/lsp/plugin/runASM24.png", false));
        result.add(runItem);
		
		return result;
	}
    
	@Override
public void actionPerformed(ActionEvent e) {
   /* if (popupMenu != null && isEnabled()) {
        java.awt.Component invoker = (java.awt.Component) e.getSource();
        if (invoker != null) {
            popupMenu.show(invoker, 0, invoker.getHeight());
        }
    }*/
}
	private boolean isAsmFileActive() {
        DataObject dataObject = Utilities.actionsGlobalContext().lookup(DataObject.class);
        if (dataObject != null) {
            FileObject file = dataObject.getPrimaryFile();
            return file != null && "text/x-vm5277-asm".equals(file.getMIMEType());
        }
        return false;
    }
    
    private FileObject getCurrentAsmFile() {
        DataObject dataObject = Utilities.actionsGlobalContext().lookup(DataObject.class);
        if (dataObject != null) {
            FileObject file = dataObject.getPrimaryFile();
            if (file != null && "text/x-vm5277-asm".equals(file.getMIMEType())) {
                return file;
            }
        }
        return null;
    }
    private void updateState() {
        setEnabled(isAsmFileActive());
    }
    
    private void runMvnTask(String action) {
		FileObject file = getCurrentAsmFile();
		if(null==file) return;
		
		Project project = FileOwnerQuery.getOwner(file);
		if(null==project) return;
	
		ActionProvider ap = project.getLookup().lookup(ActionProvider.class);
		if (ap != null && Arrays.asList(ap.getSupportedActions()).contains(ActionProvider.COMMAND_BUILD)) {
			ap.invokeAction(action, Lookups.fixed(project, "asm"));
		}
	}
}
