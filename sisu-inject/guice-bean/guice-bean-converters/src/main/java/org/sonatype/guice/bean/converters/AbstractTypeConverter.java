/**
 * Copyright (c) 2010-2011 Sonatype, Inc. All rights reserved.
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
package org.sonatype.guice.bean.converters;

import org.sonatype.guice.bean.reflect.TypeParameters;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.TypeConverter;

/**
 * Abstract {@link TypeConverter} {@link Module} that automatically registers the converter based on the type parameter.
 */
public abstract class AbstractTypeConverter<T>
    implements TypeConverter, Module
{
    public final void configure( final Binder binder )
    {
        // make sure we pick up the right super type parameter, i.e. Foo from AbstractTypeConverter<Foo>
        final TypeLiteral<?> superType = TypeLiteral.get( getClass() ).getSupertype( AbstractTypeConverter.class );
        binder.convertToTypes( Matchers.only( TypeParameters.get( superType, 0 ) ), this );
    }
}
