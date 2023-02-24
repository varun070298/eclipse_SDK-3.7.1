/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui.properties;

import java.net.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.update.core.*;
import org.eclipse.update.internal.ui.model.*;
import org.eclipse.update.internal.ui.parts.*;
import org.eclipse.update.internal.ui.UpdateUI;
import org.eclipse.update.internal.ui.UpdateUIMessages;


public class FeatureLicensePropertyPage extends PropertyPage implements IWorkbenchPropertyPage {
	public FeatureLicensePropertyPage() {
		noDefaultAndApplyButton();
	}

	protected Control createContents(Composite parent)  {
		try {
			Composite composite = new Composite(parent, SWT.NULL);
			composite.setLayout(new GridLayout());

			IFeatureAdapter adapter = (IFeatureAdapter)getElement();
			IFeature feature = adapter.getFeature(null);			
			IURLEntry license = feature.getLicense();
			String annotation = (license != null) ? license.getAnnotation() : null;
			
			if (annotation != null && annotation.length() > 0) {
				Text text = new Text(composite, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.WRAP);
				GridData gd = new GridData(GridData.FILL_BOTH);
				gd.heightHint = 200;
				gd.widthHint = 350;
				text.setLayoutData(gd);
				text.setText(annotation);
				text.setEditable(false);
				final URL url = license.getURL();
				String filename = (url != null) ? url.getFile() : null;
				if (filename != null && (filename.endsWith(".htm") || url.getFile().endsWith(".html"))) { //$NON-NLS-1$ //$NON-NLS-2$
					Button button = new Button(composite, SWT.PUSH);
					button.setText(UpdateUIMessages.FeatureLicensePropertyPage_showInBrowser); 
					button.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
					SWTUtil.setButtonDimensionHint(button);
					button.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent e) {
							UpdateUI.showURL(url.toExternalForm());
						}
					});
				}
			} else {
				Label label = new Label(composite, SWT.NULL);
				label.setText(UpdateUIMessages.FeatureLicensePropertyPage_noLicense); 
			}
			
			Dialog.applyDialogFont(parent);
			
			return composite;
			
		} catch (CoreException e) {
		}
		return null;
	}
}
