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
package org.sonatype.guice.bean.containers;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.eclipse.sisu.inject.BeanLocator;
import org.eclipse.sisu.inject.Logs;
import org.eclipse.sisu.wire.WireModule;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.util.Providers;

@Deprecated
public final class SisuGuice
{
    private static final ThreadLocal<BeanLocator> LOCATOR = new InheritableThreadLocal<BeanLocator>();

    private static volatile BeanLocator latest;

    private SisuGuice()
    {
    }

    @Inject
    public static void setBeanLocator( final BeanLocator locator )
    {
        if ( null != locator )
        {
            LOCATOR.set( locator );
        }
        else
        {
            LOCATOR.remove();
        }
        latest = locator;
    }

    public static BeanLocator getBeanLocator()
    {
        final BeanLocator locator = LOCATOR.get();
        return null != locator ? locator : latest;
    }

    public static <T> T lookup( final Key<T> key )
    {
        final BeanLocator locator = getBeanLocator();
        if ( null != locator )
        {
            final Iterator<? extends Entry<?, T>> i = locator.locate( key ).iterator();
            if ( i.hasNext() )
            {
                return i.next().getValue();
            }
        }
        else
        {
            Logs.trace( "No BeanLocator found for thread {}", Thread.currentThread(), null );
        }
        return null;
    }

    public static void inject( final Object that )
    {
        final BeanLocator locator = getBeanLocator();
        if ( null != locator )
        {
            Guice.createInjector( new WireModule( new Module()
            {
                public void configure( final Binder binder )
                {
                    binder.bind( BeanLocator.class ).toProvider( Providers.of( locator ) );
                    binder.requestInjection( that );
                }
            } ) );
        }
        else
        {
            Logs.trace( "No BeanLocator found for thread {}", Thread.currentThread(), null );
        }
    }

    public static Injector enhance( final Injector injector )
    {
        final Class<?>[] api = { Injector.class };
        return (Injector) Proxy.newProxyInstance( api[0].getClassLoader(), api, new InvocationHandler()
        {
            @SuppressWarnings( { "rawtypes", "unchecked" } )
            public Object invoke( final Object proxy, final Method method, final Object[] args )
                throws Throwable
            {
                final String methodName = method.getName();
                if ( "getInstance".equals( methodName ) )
                {
                    final Key key = args[0] instanceof Key ? (Key) args[0] : Key.get( (Class) args[0] );
                    final Iterator<Entry> i = injector.getInstance( BeanLocator.class ).locate( key ).iterator();
                    return i.hasNext() ? i.next().getValue() : null;
                }
                if ( "injectMembers".equals( methodName ) )
                {
                    Guice.createInjector( new WireModule( new Module()
                    {
                        public void configure( final Binder binder )
                        {
                            binder.bind( BeanLocator.class ).toProvider( injector.getProvider( BeanLocator.class ) );
                            binder.requestInjection( args[0] );
                        }
                    } ) );
                    return null;
                }
                return method.invoke( injector, args );
            }
        } );
    }
}
