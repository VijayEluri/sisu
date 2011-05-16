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
package org.sonatype.guice.bean.binders;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.sonatype.guice.bean.reflect.DeferredProvider;
import org.sonatype.guice.bean.reflect.Logs;
import org.sonatype.guice.bean.reflect.TypeParameters;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Binding;
import com.google.inject.ImplementedBy;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.MembersInjector;
import com.google.inject.Module;
import com.google.inject.ProvidedBy;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.BindingTargetVisitor;
import com.google.inject.spi.DefaultBindingTargetVisitor;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.HasDependencies;
import com.google.inject.spi.InjectionPoint;
import com.google.inject.spi.InjectionRequest;
import com.google.inject.spi.LinkedKeyBinding;
import com.google.inject.spi.ProviderInstanceBinding;
import com.google.inject.spi.ProviderKeyBinding;
import com.google.inject.spi.ProviderLookup;
import com.google.inject.spi.StaticInjectionRequest;
import com.google.inject.spi.UntargettedBinding;

/**
 * {@link BindingTargetVisitor} that collects the {@link Key}s of any injected dependencies.
 */
final class DependencyAnalyzer
    extends DefaultBindingTargetVisitor<Object, Boolean>
{
    // ----------------------------------------------------------------------
    // Static initialization
    // ----------------------------------------------------------------------

    static
    {
        RESTRICTED_CLASSES =
            new HashSet<Class<?>>( Arrays.<Class<?>> asList( AbstractModule.class, Binder.class, Binding.class,
                                                             Injector.class, Key.class, Logger.class,
                                                             MembersInjector.class, Module.class, Provider.class,
                                                             Scope.class, TypeLiteral.class ) );
    }

    // ----------------------------------------------------------------------
    // Constants
    // ----------------------------------------------------------------------

    private static final Set<Class<?>> RESTRICTED_CLASSES;

    // ----------------------------------------------------------------------
    // Implementation fields
    // ----------------------------------------------------------------------

    private final Map<TypeLiteral<?>, Boolean> analyzedTypes = new HashMap<TypeLiteral<?>, Boolean>();

    private final Set<Key<?>> requiredKeys = new HashSet<Key<?>>();

    // ----------------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------------

    DependencyAnalyzer()
    {
        // properties parameter is implicitly required
        requiredKeys.add( ParameterKeys.PROPERTIES );
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    public Set<Key<?>> findMissingKeys( final Set<Key<?>> localKeys )
    {
        final Set<Key<?>> missingKeys = new HashSet<Key<?>>();
        while ( requiredKeys.size() > 0 )
        {
            missingKeys.addAll( requiredKeys );
            missingKeys.removeAll( localKeys );
            requiredKeys.clear();

            for ( final Key<?> key : missingKeys )
            {
                analyzeImplicitBindings( key.getTypeLiteral() );
            }
        }
        return missingKeys;
    }

    @Override
    public Boolean visit( final UntargettedBinding<?> binding )
    {
        return analyzeImplementation( binding.getKey().getTypeLiteral() );
    }

    @Override
    public Boolean visit( final LinkedKeyBinding<?> binding )
    {
        final Key<?> linkedKey = binding.getLinkedKey();
        if ( linkedKey.getAnnotationType() == null )
        {
            return analyzeImplementation( linkedKey.getTypeLiteral() );
        }
        return Boolean.TRUE; // indirect binding, don't scan
    }

    @Override
    public Boolean visit( final ProviderKeyBinding<?> binding )
    {
        final Key<?> providerKey = binding.getProviderKey();
        if ( providerKey.getAnnotationType() == null )
        {
            return analyzeImplementation( providerKey.getTypeLiteral() );
        }
        return Boolean.TRUE; // indirect binding, don't scan
    }

    @Override
    public Boolean visit( final ProviderInstanceBinding<?> binding )
    {
        final javax.inject.Provider<?> provider = binding.getProviderInstance();
        if ( provider instanceof DeferredProvider<?> )
        {
            try
            {
                analyzeImplementation( TypeLiteral.get( ( (DeferredProvider<?>) provider ).getImplementationClass().load() ) );
            }
            catch ( final Throwable e ) // NOPMD
            {
                // deferred provider, so ignore errors for now
            }
            return Boolean.TRUE;
        }
        return Boolean.valueOf( analyzeDependencies( binding.getDependencies() ) );
    }

    @Override
    public Boolean visitOther( final Binding<?> binding )
    {
        if ( binding instanceof HasDependencies )
        {
            return Boolean.valueOf( analyzeDependencies( ( (HasDependencies) binding ).getDependencies() ) );
        }
        return Boolean.TRUE;
    }

    public <T> Boolean visit( final ProviderLookup<T> lookup )
    {
        requireKey( lookup.getKey() );
        return Boolean.TRUE;
    }

    public Boolean visit( final StaticInjectionRequest request )
    {
        return Boolean.valueOf( analyzeInjectionPoints( request.getInjectionPoints() ) );
    }

    public Boolean visit( final InjectionRequest<?> request )
    {
        return Boolean.valueOf( analyzeInjectionPoints( request.getInjectionPoints() ) );
    }

    // ----------------------------------------------------------------------
    // Implementation methods
    // ----------------------------------------------------------------------

    private void requireKey( final Key<?> key )
    {
        if ( !RESTRICTED_CLASSES.contains( key.getTypeLiteral().getRawType() ) )
        {
            requiredKeys.add( key );
        }
    }

    private Boolean analyzeImplementation( final TypeLiteral<?> type )
    {
        Boolean applyBinding = analyzedTypes.get( type );
        if ( null == applyBinding )
        {
            applyBinding = Boolean.TRUE;
            if ( TypeParameters.isConcrete( type ) )
            {
                try
                {
                    // check methods+fields first and avoid short-circuiting to maximize dependency analysis results
                    final boolean rhs = analyzeInjectionPoints( InjectionPoint.forInstanceMethodsAndFields( type ) );
                    if ( !analyzeDependencies( InjectionPoint.forConstructorOf( type ).getDependencies() ) || !rhs )
                    {
                        applyBinding = Boolean.FALSE;
                    }
                }
                catch ( final Throwable e )
                {
                    Logs.debug( "Potential problem: {}", type, e );
                    applyBinding = Boolean.FALSE;
                }
            }
            analyzedTypes.put( type, applyBinding );
        }
        return applyBinding;
    }

    private boolean analyzeInjectionPoints( final Set<InjectionPoint> points )
    {
        boolean applyBinding = true;
        for ( final InjectionPoint p : points )
        {
            applyBinding &= analyzeDependencies( p.getDependencies() );
        }
        return applyBinding;
    }

    private boolean analyzeDependencies( final Collection<Dependency<?>> dependencies )
    {
        boolean applyBinding = true;
        for ( final Dependency<?> d : dependencies )
        {
            Key<?> key = d.getKey();
            if ( key.hasAttributes() && "Assisted".equals( key.getAnnotationType().getSimpleName() ) )
            {
                applyBinding = false; // avoid directly binding AssistedInject based components
            }
            else
            {
                final Class<?> clazz = key.getTypeLiteral().getRawType();
                if ( javax.inject.Provider.class == clazz || com.google.inject.Provider.class == clazz )
                {
                    key = key.ofType( TypeParameters.get( key.getTypeLiteral(), 0 ) );
                }
                requireKey( key );
            }
        }
        return applyBinding;
    }

    private Boolean analyzeImplicitBindings( final TypeLiteral<?> type )
    {
        Class<?> clazz = type.getRawType();
        for ( TypeLiteral<?> t = type; !analyzedTypes.containsKey( t ); t = TypeLiteral.get( clazz ) )
        {
            if ( TypeParameters.isConcrete( clazz ) )
            {
                return analyzeImplementation( t );
            }
            analyzedTypes.put( t, Boolean.TRUE );
            final ImplementedBy implementedBy = clazz.getAnnotation( ImplementedBy.class );
            if ( null != implementedBy )
            {
                clazz = implementedBy.value();
            }
            else
            {
                final ProvidedBy providedBy = clazz.getAnnotation( ProvidedBy.class );
                if ( null != providedBy )
                {
                    clazz = providedBy.value();
                }
                else
                {
                    break; // reached end of the implicit chain
                }
            }
        }
        return Boolean.FALSE;
    }
}
