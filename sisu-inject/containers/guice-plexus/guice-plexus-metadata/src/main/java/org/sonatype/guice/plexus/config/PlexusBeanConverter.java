/*******************************************************************************
 * Copyright (c) 2010-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 * The Eclipse Public License is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 * The Apache License v2.0 is available at
 *   http://www.apache.org/licenses/LICENSE-2.0.html
 * You may elect to redistribute this code under either of these licenses.
 *******************************************************************************/
package org.sonatype.guice.plexus.config;

import com.google.inject.TypeLiteral;

/**
 * Service that converts values into various beans by following Plexus configuration rules.
 */
public interface PlexusBeanConverter
{
    /**
     * Converts the given constant value to a bean of the given type.
     * 
     * @param role The expected bean type
     * @param value The constant value
     * @return Bean of the given type, based on the given constant value
     */
    <T> T convert( TypeLiteral<T> role, String value );
}