/*******************************************************************************
 * Copyright (c) 2010, 2013 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Stuart McCulloch (Sonatype, Inc.) - initial API and implementation
 *******************************************************************************/
package org.sonatype.guice.plexus.shim;

import org.sonatype.guice.bean.reflect.ClassSpace;

import com.google.inject.Binder;
import com.google.inject.Module;

@Deprecated
public final class PlexusSpaceModule
    implements Module
{
    private final ClassSpace space;

    public PlexusSpaceModule( final ClassSpace space )
    {
        this.space = space;
    }

    public void configure( final Binder binder )
    {
        binder.install( new org.eclipse.sisu.plexus.PlexusSpaceModule( space ) );
    }
}
