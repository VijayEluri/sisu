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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.sonatype.guice.plexus.config.PlexusBeanLocator;

import com.google.inject.Injector;
import com.google.inject.TypeLiteral;

/**
 * Specialized {@link GuiceBeans} implementation that iterates over beans of a given type according to named hints.
 */
final class HintedGuiceBeans<T>
    extends AbstractGuiceBeans<T>
{
    // ----------------------------------------------------------------------
    // Implementation fields
    // ----------------------------------------------------------------------

    private final TypeLiteral<T> role;

    private final String[] hints;

    // ----------------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------------

    HintedGuiceBeans( final TypeLiteral<T> role, final String[] hints )
    {
        this.role = role;
        this.hints = hints;
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    @SuppressWarnings( "unchecked" )
    public synchronized Iterator<PlexusBeanLocator.Bean<T>> iterator()
    {
        // compile map of all known beans at this particular moment
        // can't build map ahead of time as contributions will vary
        final Map<String, Entry<String, T>> beanMap = new HashMap();
        if ( null != injectorBeans )
        {
            for ( int i = 0, size = injectorBeans.size(); i < size; i++ )
            {
                for ( final Entry<String, T> e : injectorBeans.get( i ) )
                {
                    final String key = e.getKey();
                    if ( !beanMap.containsKey( key ) )
                    {
                        beanMap.put( key, e );
                    }
                }
            }
        }

        // "copy-on-read" - select hinted beans from above map
        final List selectedBeans = new ArrayList( hints.length );
        for ( final String hint : hints )
        {
            final Entry bean = beanMap.get( hint );
            if ( null != bean )
            {
                selectedBeans.add( bean );
            }
            else
            {
                // no-one supplies this hint, so mark it as missing
                selectedBeans.add( new MissingBean( role, hint ) );
            }
        }
        return selectedBeans.iterator();
    }

    public boolean add( final Injector injector )
    {
        return addInjectorBeans( new InjectorBeans<T>( injector, role, hints ) );
    }
}