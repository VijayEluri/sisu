/**
 * Copyright (c) 2010 Sonatype, Inc. All rights reserved.
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
package org.sonatype.guice.bean.locators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.sonatype.guice.bean.locators.spi.BindingDistributor;
import org.sonatype.guice.bean.locators.spi.BindingPublisher;
import org.sonatype.inject.BeanEntry;
import org.sonatype.inject.Mediator;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

/**
 * Default {@link MutableBeanLocator} that finds qualified beans across a dynamic group of {@link BindingPublisher}s.
 */
@Singleton
@SuppressWarnings( { "rawtypes", "unchecked" } )
public final class DefaultBeanLocator
    implements MutableBeanLocator
{
    // ----------------------------------------------------------------------
    // Implementation fields
    // ----------------------------------------------------------------------

    private final RankedList<BindingPublisher> publishers = new RankedList<BindingPublisher>();

    private final Map<TypeLiteral, RankedBindings> bindingsCache = new HashMap<TypeLiteral, RankedBindings>();

    private final List<WatchedBeans> watchedBeans = new ArrayList<WatchedBeans>();

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    public synchronized Iterable<BeanEntry> locate( final Key key )
    {
        final RankedBindings bindings = bindingsForType( key.getTypeLiteral() );
        final QualifiedBeans beans = new QualifiedBeans( key, bindings );
        bindings.addBeanCache( beans );
        return beans;
    }

    public synchronized void watch( final Key key, final Mediator mediator, final Object watcher )
    {
        final WatchedBeans beans = new WatchedBeans( key, mediator, watcher );
        for ( final BindingPublisher publisher : publishers )
        {
            beans.add( publisher, 0 );
        }
        watchedBeans.add( beans );
    }

    public void add( final Injector injector, final int rank )
    {
        add( new InjectorPublisher( injector, new DefaultRankingFunction( rank ) ), rank );
    }

    public void remove( final Injector injector )
    {
        remove( new InjectorPublisher( injector, null ) );
    }

    public synchronized void add( final BindingPublisher publisher, final int rank )
    {
        if ( !publishers.contains( publisher ) )
        {
            publishers.insert( publisher, rank );
            distribute( BindingEvent.ADD, publisher, rank );
        }
    }

    public synchronized void remove( final BindingPublisher publisher )
    {
        if ( publishers.remove( publisher ) )
        {
            distribute( BindingEvent.REMOVE, publisher, 0 );
        }
    }

    public synchronized void clear()
    {
        publishers.clear();
        distribute( BindingEvent.CLEAR, null, 0 );
    }

    // ----------------------------------------------------------------------
    // Implementation methods
    // ----------------------------------------------------------------------

    @Inject
    @SuppressWarnings( "unused" )
    private void autoPublish( final Injector injector )
    {
        final RankingFunction function = injector.getInstance( RankingFunction.class );
        add( new InjectorPublisher( injector, function ), function.maxRank() );
    }

    /**
     * Returns the {@link RankedBindings} tracking the type; creates a new instance if one doesn't already exist.
     * 
     * @param type The required type
     * @return Sequence of ranked bindings
     */
    private <T> RankedBindings bindingsForType( final TypeLiteral<T> type )
    {
        RankedBindings bindings = bindingsCache.get( type );
        if ( null == bindings )
        {
            bindings = new RankedBindings( type, publishers );
            bindingsCache.put( type, bindings );
        }
        return bindings;
    }

    private void distribute( final BindingEvent event, final BindingPublisher publisher, final int rank )
    {
        for ( final Iterator<RankedBindings> itr = bindingsCache.values().iterator(); itr.hasNext(); )
        {
            final RankedBindings bindings = itr.next();
            if ( bindings.isActive() )
            {
                event.apply( bindings, publisher, rank );
            }
            else
            {
                itr.remove();
            }
        }

        for ( int i = 0; i < watchedBeans.size(); i++ )
        {
            final WatchedBeans beans = watchedBeans.get( i );
            if ( beans.isActive() )
            {
                event.apply( beans, publisher, rank );
            }
            else
            {
                watchedBeans.remove( i-- );
            }
        }
    }

    // ----------------------------------------------------------------------
    // Implementation types
    // ----------------------------------------------------------------------

    private static enum BindingEvent
    {
        ADD
        {
            @Override
            void apply( final BindingDistributor distributor, final BindingPublisher publisher, final int rank )
            {
                distributor.add( publisher, rank );
            }
        },
        REMOVE
        {
            @Override
            void apply( final BindingDistributor distributor, final BindingPublisher publisher, final int rank )
            {
                distributor.remove( publisher );
            }
        },
        CLEAR
        {
            @Override
            void apply( final BindingDistributor distributor, final BindingPublisher publisher, final int rank )
            {
                distributor.clear();
            }
        };

        abstract void apply( final BindingDistributor distributor, final BindingPublisher publisher, final int rank );
    }
}
