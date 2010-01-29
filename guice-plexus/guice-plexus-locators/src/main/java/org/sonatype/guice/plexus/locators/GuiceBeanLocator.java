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
package org.sonatype.guice.plexus.locators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Map.Entry;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.sonatype.guice.plexus.config.PlexusBeanLocator;

import com.google.inject.Injector;
import com.google.inject.TypeLiteral;

@Singleton
public final class GuiceBeanLocator
    implements PlexusBeanLocator
{
    final List<PlexusBeanLocator> locators = new ArrayList<PlexusBeanLocator>();

    @Inject
    GuiceBeanLocator( final Injector rootInjector )
    {
        locators.add( new InjectorBeanLocator( rootInjector ) );
    }

    public void add( final PlexusBeanLocator locator )
    {
        locators.add( locator );
    }

    public void remove( final PlexusBeanLocator locator )
    {
        locators.remove( locator );
    }

    public <T> Iterable<Entry<String, T>> locate( final TypeLiteral<T> type, final String... hints )
    {
        return new Iterable<Entry<String, T>>()
        {
            final Map<PlexusBeanLocator, Iterable<Entry<String, T>>> iterableCache =
                new HashMap<PlexusBeanLocator, Iterable<Entry<String, T>>>();

            public Iterator<Entry<String, T>> iterator()
            {
                return new Iterator<Entry<String, T>>()
                {
                    private final Iterator<PlexusBeanLocator> l = locators.iterator();

                    private Iterator<Entry<String, T>> e = Collections.<Entry<String, T>> emptyList().iterator();

                    public boolean hasNext()
                    {
                        while ( true )
                        {
                            if ( e.hasNext() )
                            {
                                return true;
                            }
                            else if ( l.hasNext() )
                            {
                                final PlexusBeanLocator locator = l.next();
                                Iterable<Entry<String, T>> i = iterableCache.get( locator );
                                if ( null == i )
                                {
                                    i = locator.locate( type, hints );
                                    iterableCache.put( locator, i );
                                }
                                e = i.iterator();
                            }
                            else
                            {
                                return false;
                            }
                        }
                    }

                    public Entry<String, T> next()
                    {
                        while ( hasNext() )
                        {
                            final Entry<String, T> entry = e.next();
                            if ( l.hasNext() ) // TODO: round-robin checks for named lists/maps
                            {
                                try
                                {
                                    entry.getValue();
                                }
                                catch ( final RuntimeException re )
                                {
                                    continue; // TODO: is it always OK to skip broken entries?
                                }
                            }
                            return entry;
                        }
                        throw new NoSuchElementException();
                    }

                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }
}