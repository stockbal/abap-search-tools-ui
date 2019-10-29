package com.devepos.adt.saat.internal.help;

import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;

import com.devepos.adt.saat.SearchAndAnalysisPlugin;

/**
 * Utility for easily setting help context to controls
 *
 * @author stockbal
 */
public class HelpUtil {

	/**
	 * Sets the help with the given context id to the given control
	 *
	 * @param control   a control
	 * @param contextId unique id of a help context
	 */
	public static void setHelp(final Control control, final HelpContexts context) {
		if (control == null || control.isDisposed()) {
			return;
		}
		PlatformUI.getWorkbench()
			.getHelpSystem()
			.setHelp(control, SearchAndAnalysisPlugin.PLUGIN_ID + "." + context.getHelpContextId());
	}
}