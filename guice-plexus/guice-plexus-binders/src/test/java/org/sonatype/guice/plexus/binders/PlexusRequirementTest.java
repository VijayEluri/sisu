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
package org.sonatype.guice.plexus.binders;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import junit.framework.TestCase;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.guice.bean.reflect.ClassSpace;
import org.sonatype.guice.bean.reflect.DeferredClass;
import org.sonatype.guice.bean.reflect.URLClassSpace;
import org.sonatype.guice.plexus.config.Hints;
import org.sonatype.guice.plexus.config.PlexusBeanConverter;
import org.sonatype.guice.plexus.config.PlexusBeanLocator;
import org.sonatype.guice.plexus.config.PlexusBeanMetadata;
import org.sonatype.guice.plexus.config.PlexusBeanSource;
import org.sonatype.guice.plexus.config.Strategies;
import org.sonatype.guice.plexus.converters.PlexusDateTypeConverter;
import org.sonatype.guice.plexus.converters.PlexusXmlBeanConverter;
import org.sonatype.guice.plexus.locators.DefaultPlexusBeanLocator;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.ImplementedBy;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import com.google.inject.Scopes;
import com.google.inject.name.Names;

public class PlexusRequirementTest
    extends TestCase
{
    @Inject
    Component1 component;

    @Inject
    Injector injector;

    @Override
    protected void setUp()
    {
        Guice.createInjector( new AbstractModule()
        {
            @Override
            protected void configure()
            {
                final ClassSpace space = new URLClassSpace( TestCase.class.getClassLoader() );

                @SuppressWarnings( "unchecked" )
                final DeferredClass<A> deferA = (DeferredClass) space.deferLoadClass( BrokenAImpl.class.getName() );

                install( new PlexusDateTypeConverter() );

                bind( PlexusBeanLocator.class ).to( DefaultPlexusBeanLocator.class );
                bind( PlexusBeanConverter.class ).to( PlexusXmlBeanConverter.class );

                bind( A.class ).annotatedWith( Names.named( "AA" ) ).to( AAImpl.class );
                bind( A.class ).annotatedWith( Names.named( "broken" ) ).toProvider( deferA.asProvider() );
                bind( A.class ).annotatedWith( Names.named( "AB" ) ).to( ABImpl.class );
                bind( A.class ).to( AImpl.class ).in( Scopes.SINGLETON );
                bind( A.class ).annotatedWith( Names.named( "AC" ) ).to( ACImpl.class );

                bind( B.class ).annotatedWith( Names.named( "B" ) ).to( BImpl.class );

                bind( D.class ).annotatedWith( Names.named( "" ) ).to( DImpl.class );

                install( new PlexusBindingModule( null, new PlexusBeanSource()
                {
                    public void configure( final Binder binder )
                    {
                        binder.bind( Alpha.class ).to( AlphaImpl.class ).in( Scopes.SINGLETON );
                        binder.bind( Omega.class ).to( OmegaImpl.class ).in( Scopes.SINGLETON );

                        @SuppressWarnings( "unchecked" )
                        final DeferredClass<Gamma> gammaProvider =
                            (DeferredClass) space.deferLoadClass( "some-broken-class" ).asProvider();

                        binder.bind( Gamma.class ).toProvider( gammaProvider.asProvider() ).in( Scopes.SINGLETON );
                    }

                    public PlexusBeanMetadata getBeanMetadata( final Class<?> implementation )
                    {
                        return null;
                    }
                }, new PlexusAnnotatedBeanSource( null, null ) ) );

                requestInjection( PlexusRequirementTest.this );
            }
        } );
    }

    @ImplementedBy( AImpl.class )
    interface A
    {
    }

    interface B
    {
    }

    interface C
    {
    }

    interface D
    {
    }

    static class AImpl
        implements A
    {
    }

    static class BImpl
        implements B
    {
    }

    static class DImpl
        implements D
    {
    }

    static class AAImpl
        extends AImpl
    {
    }

    static class ABImpl
        extends AImpl
    {
    }

    static class ACImpl
        extends AImpl
    {
    }

    static class BrokenAImpl
        extends AImpl
    {
        public BrokenAImpl( final MissingClass missing )
        {
        }
    }

    static class MissingClass
    {
    }

    @Component( role = Component1.class, instantiationStrategy = Strategies.PER_LOOKUP )
    static class Component1
    {
        @Requirement
        A testField;

        A testSetter;

        @Requirement( hints = { "default" } )
        void setValue( final A a )
        {
            testSetter = a;
        }

        @Requirement( role = A.class )
        Object testRole;

        @Requirement( hint = "AB", optional = true )
        A testHint;

        @Requirement( hint = "MISSING", optional = true )
        A testOptional = new ACImpl();

        @Requirement( role = A.class )
        Map<String, ?> testMap;

        @Requirement( hints = { "AC", "AB" } )
        Map<String, A> testSubMap;

        @Requirement
        Map<String, C> testEmptyMap;

        @Requirement( role = A.class )
        List<?> testList;

        @Requirement( hints = { "AC", "AA" } )
        List<? extends A> testSubList;

        @Requirement
        List<C> testEmptyList;

        @Requirement
        B testWildcard;

        @Requirement( optional = true )
        C optionalRequirement;
    }

    @Component( role = Component2.class )
    static class Component2
    {
        @Requirement
        void testZeroArgSetter()
        {
            throw new RuntimeException();
        }
    }

    @Component( role = Component3.class )
    static class Component3
    {
        @Requirement
        void testMultiArgSetter( final A a1, final A a2 )
        {
            throw new RuntimeException();
        }
    }

    @Component( role = Component4.class )
    static class Component4
    {
        @Requirement
        C testMissingRequirement;
    }

    @Component( role = Component5.class )
    static class Component5
    {
        @Requirement( hint = "B!" )
        B testNoSuchHint;
    }

    @Component( role = Component6.class )
    static class Component6
    {
        @Requirement( hints = { "AA", "AZ", "A!" } )
        Map<String, B> testNoSuchHint;
    }

    @Component( role = Component7.class )
    static class Component7
    {
        @Requirement( hints = { "AA", "AZ", "A!" } )
        List<C> testNoSuchHint;
    }

    @Component( role = Component8.class )
    static class Component8
    {
        @Requirement( hints = { "" } )
        List<D> testBadName;
    }

    @Component( role = Component9.class )
    static class Component9
    {
        @Requirement( hint = "default" )
        B testNoDefault;
    }

    public void testRepeatInjection()
    {
        final Component1 duplicate = injector.getInstance( Component1.class );
        assertSame( component.testField, duplicate.testField );
        assertSame( component.testSetter, duplicate.testSetter );
        assertSame( component.testRole, duplicate.testRole );
    }

    public void testSingleRequirement()
    {
        assertEquals( AImpl.class, component.testField.getClass() );
        assertEquals( AImpl.class, component.testSetter.getClass() );
        assertEquals( AImpl.class, component.testRole.getClass() );
        assertEquals( ABImpl.class, component.testHint.getClass() );
        assertEquals( ACImpl.class, component.testOptional.getClass() );
        assertEquals( BImpl.class, component.testWildcard.getClass() );
    }

    public void testRequirementMap()
    {
        assertEquals( 5, component.testMap.size() );
        assertEquals( 0, component.testEmptyMap.size() );

        // check mapping
        assertEquals( AImpl.class, component.testMap.get( "default" ).getClass() );
        assertEquals( AAImpl.class, component.testMap.get( "AA" ).getClass() );
        assertEquals( ABImpl.class, component.testMap.get( "AB" ).getClass() );
        assertEquals( ACImpl.class, component.testMap.get( "AC" ).getClass() );

        // check key ordering is same as original map-binder
        final Iterator<String> keys = component.testMap.keySet().iterator();
        assertEquals( Hints.DEFAULT_HINT, keys.next() );
        assertEquals( "AA", keys.next() );
        assertEquals( "broken", keys.next() );
        assertEquals( "AB", keys.next() );
        assertEquals( "AC", keys.next() );
        assertFalse( keys.hasNext() );

        // check value ordering is same as original map-binder
        final Iterator<?> values = component.testMap.values().iterator();
        assertEquals( AImpl.class, values.next().getClass() );
        assertEquals( AAImpl.class, values.next().getClass() );
        try
        {
            values.next();
            fail( "Expected ProvisionException" );
        }
        catch ( final ProvisionException e )
        {
        }
        assertEquals( ABImpl.class, values.next().getClass() );
        assertEquals( ACImpl.class, values.next().getClass() );
        assertFalse( values.hasNext() );
    }

    public void testRequirementSubMap()
    {
        assertEquals( 2, component.testSubMap.size() );

        // check mapping
        assertEquals( ABImpl.class, component.testSubMap.get( "AB" ).getClass() );
        assertEquals( ACImpl.class, component.testSubMap.get( "AC" ).getClass() );

        // check key ordering is same as original map-binder
        final Iterator<String> keys = component.testSubMap.keySet().iterator();
        assertEquals( "AC", keys.next() );
        assertEquals( "AB", keys.next() );
        assertFalse( keys.hasNext() );

        // check value ordering is same as hints
        final Iterator<A> values = component.testSubMap.values().iterator();
        assertEquals( ACImpl.class, values.next().getClass() );
        assertEquals( ABImpl.class, values.next().getClass() );
        assertFalse( values.hasNext() );
    }

    public void testRequirementList()
    {
        assertEquals( 5, component.testList.size() );
        assertEquals( 0, component.testEmptyList.size() );

        // check ordering is same as original map-binder
        final Iterator<?> i = component.testList.iterator();
        assertEquals( AImpl.class, i.next().getClass() );
        assertEquals( AAImpl.class, i.next().getClass() );
        try
        {
            i.next();
            fail( "Expected ProvisionException" );
        }
        catch ( final ProvisionException e )
        {
        }
        assertEquals( ABImpl.class, i.next().getClass() );
        assertEquals( ACImpl.class, i.next().getClass() );
        assertFalse( i.hasNext() );
    }

    public void testRequirementSubList()
    {
        assertEquals( 2, component.testSubList.size() );

        // check ordering is same as hints
        final Iterator<? extends A> i = component.testSubList.iterator();
        assertEquals( ACImpl.class, i.next().getClass() );
        assertEquals( AAImpl.class, i.next().getClass() );
        assertFalse( i.hasNext() );
    }

    public void testZeroArgSetterError()
    {
        injector.getInstance( Component2.class );
    }

    public void testMultiArgSetterError()
    {
        injector.getInstance( Component3.class );
    }

    public void testMissingRequirement()
    {
        try
        {
            injector.getInstance( Component4.class );
            fail( "Expected error for missing requirement" );
        }
        catch ( final ProvisionException e )
        {
        }
    }

    public void testNoSuchHint()
    {
        try
        {
            injector.getInstance( Component5.class );
            fail( "Expected error for no such hint" );
        }
        catch ( final ProvisionException e )
        {
        }
    }

    public void testNoSuchMapHint()
    {
        try
        {
            injector.getInstance( Component6.class ).testNoSuchHint.toString();
            fail( "Expected error for no such hint" );
        }
        catch ( final ProvisionException e )
        {
        }
    }

    public void testNoSuchListHint()
    {
        try
        {
            injector.getInstance( Component7.class ).testNoSuchHint.toString();
            fail( "Expected error for no such hint" );
        }
        catch ( final ProvisionException e )
        {
        }
    }

    public void testBadName()
    {
        try
        {
            injector.getInstance( Component8.class ).testBadName.toString();
            fail( "Expected error for bad name" );
        }
        catch ( final ProvisionException e )
        {
        }
    }

    public void testNoDefault()
    {
        try
        {
            injector.getInstance( Component9.class );
            fail( "Expected error for missing default requirement" );
        }
        catch ( final ProvisionException e )
        {
        }
    }

    interface Alpha
    {
    }

    interface Omega
    {
    }

    interface Gamma
    {
    }

    @Component( role = Alpha.class )
    static class AlphaImpl
        implements Alpha
    {
        @Requirement
        Omega omega;
    }

    @Component( role = Omega.class )
    static class OmegaImpl
        implements Omega
    {
        @Requirement
        Alpha alpha;
    }

    @Inject
    Alpha alpha;

    @Inject
    Omega omega;

    public void testCircularity()
    {
        assertNotNull( ( (OmegaImpl) omega ).alpha );
        assertNotNull( ( (AlphaImpl) alpha ).omega );

        assertSame( alpha, ( (OmegaImpl) omega ).alpha );
        assertSame( omega, ( (AlphaImpl) alpha ).omega );
    }

    public void testBadDeferredRole()
    {
        try
        {
            injector.getInstance( Gamma.class );
            fail( "Expected ProvisionException" );
        }
        catch ( final ProvisionException e )
        {
        }
    }

    public void testPlexus121Compatibility()
        throws Exception
    {
        final List<URL> urls = new ArrayList<URL>();
        urls.add( new File( "target/dependency/plexus-component-annotations-1.2.1.jar" ).toURI().toURL() );
        Collections.addAll( urls, ( (URLClassLoader) getClass().getClassLoader() ).getURLs() );

        // check binding works with Plexus 1.2.1 annotations: @Requirement does not have optional setting
        final ClassLoader legacyLoader = new URLClassLoader( urls.toArray( new URL[urls.size()] ), null )
        {
            @Override
            protected synchronized Class<?> loadClass( final String name, final boolean resolve )
                throws ClassNotFoundException
            {
                if ( name.contains( "cobertura" ) )
                {
                    return PlexusRequirementTest.class.getClassLoader().loadClass( name );
                }
                return super.loadClass( name, resolve );
            }
        };
        legacyLoader.loadClass( SimpleRequirementExample.class.getName() ).newInstance();
    }

    @SuppressWarnings( "unchecked" )
    static <S, T extends S> DeferredClass<T> defer( final Class<S> clazz )
    {
        return (DeferredClass) new URLClassSpace( TestCase.class.getClassLoader() ).deferLoadClass( clazz.getName() );
    }
}
