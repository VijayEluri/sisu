/*******************************************************************************
 * Copyright (c) 2009-2011 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 * The Eclipse Public License is available at
 *   http://www.eclipse.org/legal/epl-v10.html
 * The Apache License v2.0 is available at
 *   http://www.apache.org/licenses/LICENSE-2.0.html
 * You may elect to redistribute this code under either of these licenses.
 *******************************************************************************/
package org.sonatype.guice.bean.reflect;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

import com.google.inject.TypeLiteral;

/**
 * Utility methods for dealing with generic type parameters.
 */
public final class TypeParameters
{
    // ----------------------------------------------------------------------
    // Constants
    // ----------------------------------------------------------------------

    private static final TypeLiteral<?>[] NO_TYPE_LITERALS = {};

    private static final TypeLiteral<?> OBJECT_TYPE_LITERAL = TypeLiteral.get( Object.class );

    // ----------------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------------

    private TypeParameters()
    {
        // static utility class, not allowed to create instances
    }

    // ----------------------------------------------------------------------
    // Utility methods
    // ----------------------------------------------------------------------

    /**
     * Get all type parameters from a generic type, for example {@code [Foo,Bar]} from {@code Map<Foo,Bar>}.
     * 
     * @param genericType The generic type
     * @return Array of type parameters
     */
    public static TypeLiteral<?>[] get( final TypeLiteral<?> genericType )
    {
        final Type type = genericType.getType();
        if ( type instanceof ParameterizedType )
        {
            final Type[] arguments = ( (ParameterizedType) type ).getActualTypeArguments();
            final TypeLiteral<?>[] parameters = new TypeLiteral[arguments.length];
            for ( int i = 0; i < arguments.length; i++ )
            {
                parameters[i] = expand( arguments[i] );
            }
            return parameters;
        }
        if ( type instanceof GenericArrayType )
        {
            return new TypeLiteral[] { expand( ( (GenericArrayType) type ).getGenericComponentType() ) };
        }
        return NO_TYPE_LITERALS;
    }

    /**
     * Get an indexed type parameter from a generic type, for example {@code Bar} from {@code Map<Foo,Bar>}.
     * 
     * @param genericType The generic type
     * @param index The parameter index
     * @return Indexed type parameter; {@code TypeLiteral<Object>} if the given type is a raw class
     */
    public static TypeLiteral<?> get( final TypeLiteral<?> genericType, final int index )
    {
        final Type type = genericType.getType();
        if ( type instanceof ParameterizedType )
        {
            return expand( ( (ParameterizedType) type ).getActualTypeArguments()[index] );
        }
        if ( type instanceof GenericArrayType )
        {
            if ( 0 == index )
            {
                return expand( ( (GenericArrayType) type ).getGenericComponentType() );
            }
            throw new ArrayIndexOutOfBoundsException( index );
        }
        return OBJECT_TYPE_LITERAL;
    }

    /**
     * Determines if the subType can be converted to the generic superType via an identity or widening conversion.
     * 
     * @param superType The superType
     * @param subType The subType
     * @return {@code 1} if it is strongly assignable; {@code -1} if it is weakly assignable; otherwise {@code 0}
     * @see Class#isAssignableFrom(Class)
     */
    public static int isAssignableFrom( final TypeLiteral<?> superType, final TypeLiteral<?> subType )
    {
        final Class<?> superClazz = superType.getRawType();
        final Class<?> subClazz = subType.getRawType();
        if ( !superClazz.isAssignableFrom( subClazz ) )
        {
            return Object.class == subClazz ? -1 : 0;
        }
        int result = 1;
        final TypeLiteral<?>[] superParams = TypeParameters.get( superType );
        final TypeLiteral<?>[] subParams = TypeParameters.get( subType.getSupertype( superClazz ) );
        for ( int i = 0, len = Math.min( superParams.length, subParams.length ); i < len; i++ )
        {
            final int paramResult = isAssignableFrom( superParams[i], subParams[i] );
            if ( paramResult == 0 )
            {
                return 0;
            }
            if ( paramResult < 0 )
            {
                result = -1;
            }
        }
        return result;
    }

    // ----------------------------------------------------------------------
    // Implementation methods
    // ----------------------------------------------------------------------

    /**
     * Expands wild-card types where possible, for example {@code Bar} from {@code ? extends Bar}.
     * 
     * @param type The generic type
     * @return Widened type that is still assignment-compatible with the original.
     */
    private static TypeLiteral<?> expand( final Type type )
    {
        return TypeLiteral.get( type instanceof WildcardType ? ( (WildcardType) type ).getUpperBounds()[0] : type );
    }
}
