package org.sonatype.guice.plexus.config;

/*******************************************************************************
 * Copyright (c) 2010 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

import java.util.Map.Entry;

/**
 * Plexus bean mapping; from hint->instance.
 */
public interface PlexusBean<T>
    extends Entry<String, T>
{
    /**
     * @return Human readable description
     */
    String getDescription();

    /**
     * @return Bean implementation class
     */
    Class<T> getImplementationClass();
}