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

package org.codehaus.plexus.personality.plexus.lifecycle.phase;

public final class StoppingException
    extends Exception
{
    private static final long serialVersionUID = 1L;

    public StoppingException( final String message )
    {
        super( message );
    }

    public StoppingException( final String message, final Throwable cause )
    {
        super( message, cause );
    }
}
