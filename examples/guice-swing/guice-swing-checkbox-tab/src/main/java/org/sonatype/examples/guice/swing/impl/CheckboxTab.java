/*******************************************************************************
 * Copyright (c) 2010-present Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Stuart McCulloch (Sonatype, Inc.) - initial API and implementation
 *******************************************************************************/
package org.sonatype.examples.guice.swing.impl;

import javax.inject.Named;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

@Named( "Checkbox" )
final class CheckboxTab
    extends JPanel
{
    static int instanceCount;

    CheckboxTab()
    {
        add( new JCheckBox( "Checkbox #" + ++instanceCount ) );
    }
}
