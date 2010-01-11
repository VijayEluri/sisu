/**
 * Copyright (c) 2009 Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package org.sonatype.guice.bean.reflect;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

import junit.framework.TestCase;

public class URLClassSpaceTest
    extends TestCase
{
    private static final URL COMMONS_LOGGING_JAR =
        ZipEntryIteratorTest.class.getClassLoader().getResource( "commons-logging-1.1.1.jar" );

    private static final URL SIMPLE_JAR = ZipEntryIteratorTest.class.getClassLoader().getResource( "simple.jar" );

    private static final URL CLASS_PATH_JAR = ZipEntryIteratorTest.class.getClassLoader().getResource( "classpath.jar" );

    private static final URL BROKEN_JAR = ZipEntryIteratorTest.class.getClassLoader().getResource( "broken.jar" );

    private static final URL CORRUPT_MANIFEST =
        ZipEntryIteratorTest.class.getClassLoader().getResource( "corrupt.manifest/" );

    public void testClassSpaceResources()
        throws IOException
    {
        final ClassSpace space = new URLClassSpace( URLClassLoader.newInstance( new URL[] { COMMONS_LOGGING_JAR } ) );
        Enumeration<URL> e;

        int n = 0;
        e = space.getResources( "META-INF/MANIFEST.MF" );
        while ( true )
        {
            n++;

            // should have several matches from parent loader, local match should be last
            if ( e.nextElement().getPath().startsWith( COMMONS_LOGGING_JAR.toString() ) )
            {
                assertFalse( e.hasMoreElements() );
                break;
            }
        }
        assertTrue( n > 1 );

        e = space.findEntries( "META-INF", "*.MF", false );

        // only expect to see single result
        assertTrue( e.hasMoreElements() );
        assertTrue( e.nextElement().getPath().startsWith( COMMONS_LOGGING_JAR.toString() ) );
        assertFalse( e.hasMoreElements() );
    }

    public void testClassPathExpansion()
        throws IOException
    {
        System.setProperty( "java.protocol.handler.pkgs", getClass().getPackage().getName() );

        final ClassSpace space =
            new URLClassSpace( URLClassLoader.newInstance( new URL[] { CLASS_PATH_JAR, null, new URL( "barf:up/" ),
                CORRUPT_MANIFEST } ) );

        final Enumeration<URL> e = space.findEntries( "META-INF", "*.MF", false );

        // expect to see three results
        assertTrue( e.hasMoreElements() );
        assertTrue( e.nextElement().getPath().startsWith( CLASS_PATH_JAR.toString() ) );
        assertTrue( e.hasMoreElements() );
        assertTrue( e.nextElement().getPath().startsWith( BROKEN_JAR.toString() ) );
        assertTrue( e.hasMoreElements() );
        assertTrue( e.nextElement().getPath().startsWith( COMMONS_LOGGING_JAR.toString() ) );
        assertTrue( e.hasMoreElements() );
        assertTrue( e.nextElement().getPath().startsWith( SIMPLE_JAR.toString() ) );
        assertFalse( e.hasMoreElements() );
    }
}
