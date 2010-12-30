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

import org.sonatype.guice.bean.locators.spi.BindingExporter;
import org.sonatype.guice.bean.locators.spi.BindingImporter;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;

/**
 * Exports {@link Binding}s from a single {@link Injector}; assigns each binding a rank up to a maximum value.
 */
final class InjectorBindingExporter
    implements BindingExporter
{
    // ----------------------------------------------------------------------
    // Implementation fields
    // ----------------------------------------------------------------------

    private final Injector injector;

    private final int rank;

    // ----------------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------------

    InjectorBindingExporter( final Injector injector, final int rank )
    {
        if ( rank < 0 )
        {
            throw new IllegalArgumentException( BeanLocator.INJECTOR_RANKING + " must be zero or more" );
        }

        this.injector = injector;
        this.rank = rank;
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    public <T> void publish( final TypeLiteral<T> type, final BindingImporter importer )
    {
        final int secondaryRank = rank + Integer.MIN_VALUE;
        for ( final Binding<T> binding : injector.findBindingsByType( type ) )
        {
            if ( false == binding.getSource() instanceof HiddenBinding )
            {
                importer.publish( binding, isDefault( binding ) ? rank : secondaryRank );
            }
        }
    }

    public <T> void remove( final TypeLiteral<T> type, final BindingImporter importer )
    {
        for ( final Binding<T> binding : injector.findBindingsByType( type ) )
        {
            importer.remove( binding );
        }
    }

    @Override
    public int hashCode()
    {
        return injector.hashCode();
    }

    @Override
    public boolean equals( final Object rhs )
    {
        if ( this == rhs )
        {
            return true;
        }
        if ( rhs instanceof InjectorBindingExporter )
        {
            return injector.equals( ( (InjectorBindingExporter) rhs ).injector );
        }
        return false;
    }

    // ----------------------------------------------------------------------
    // Implementation methods
    // ----------------------------------------------------------------------

    /**
     * @return {@code true} if this is a default (ie. un-annotated) binding; otherwise {@code false}
     */
    private boolean isDefault( final Binding<?> binding )
    {
        return null == binding.getKey().getAnnotationType();
    }
}
