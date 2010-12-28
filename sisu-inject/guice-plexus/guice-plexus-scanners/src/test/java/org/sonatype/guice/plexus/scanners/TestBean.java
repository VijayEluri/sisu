package org.sonatype.guice.plexus.scanners;

/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import org.codehaus.plexus.component.annotations.Component;
import org.sonatype.guice.plexus.config.Strategies;

@Component( role = Runnable.class, hint = "test", instantiationStrategy = Strategies.PER_LOOKUP, description = "Some Test", isolatedRealm = true )
public class TestBean
    implements Runnable
{
    public void run()
    {
    }
}
