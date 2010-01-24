/**
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */
package org.codehaus.plexus.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultPlexusConfiguration
    implements PlexusConfiguration
{
    // ----------------------------------------------------------------------
    // Constants
    // ----------------------------------------------------------------------

    private static final PlexusConfiguration[] NO_CHILDREN = new PlexusConfiguration[0];

    // ----------------------------------------------------------------------
    // Implementation fields
    // ----------------------------------------------------------------------

    private final String name;

    private final String value;

    private Map<String, String> attributeMap;

    private Map<String, List<PlexusConfiguration>> childMap;

    // ----------------------------------------------------------------------
    // Constructors
    // ----------------------------------------------------------------------

    public DefaultPlexusConfiguration( final String name )
    {
        this( name, null );
    }

    public DefaultPlexusConfiguration( final String name, final String value )
    {
        this.name = name;
        this.value = value;
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    public final String getName()
    {
        return name;
    }

    public final String getValue()
    {
        return value;
    }

    public final String getValue( final String defaultValue )
    {
        if ( null == value )
        {
            return defaultValue;
        }
        return value;
    }

    public final String getAttribute( final String attributeName, final String defaultValue )
    {
        if ( null == attributeMap )
        {
            return defaultValue;
        }
        final String attribute = attributeMap.get( attributeName );
        if ( null == attribute )
        {
            return defaultValue;
        }
        return attribute;
    }

    public final PlexusConfiguration getChild( final String childName )
    {
        final List<PlexusConfiguration> children = createChildren( childName );
        if ( children.isEmpty() )
        {
            children.add( createChild( childName ) );
        }
        return children.get( 0 );
    }

    public final PlexusConfiguration getChild( final String childName, final boolean create )
    {
        if ( create )
        {
            return getChild( childName );
        }
        if ( null == childMap )
        {
            return null;
        }
        final List<PlexusConfiguration> children = childMap.get( childName );
        if ( null == children )
        {
            return null;
        }
        return children.get( 0 );
    }

    public final PlexusConfiguration[] getChildren()
    {
        if ( null == childMap )
        {
            return NO_CHILDREN;
        }
        final List<PlexusConfiguration> children = new ArrayList<PlexusConfiguration>();
        for ( final List<PlexusConfiguration> siblings : childMap.values() )
        {
            children.addAll( siblings );
        }
        return children.toArray( new PlexusConfiguration[children.size()] );
    }

    public final PlexusConfiguration[] getChildren( final String childName )
    {
        if ( null == childMap )
        {
            return NO_CHILDREN;
        }
        final List<PlexusConfiguration> children = childMap.get( childName );
        if ( null == children )
        {
            return NO_CHILDREN;
        }
        return children.toArray( new PlexusConfiguration[children.size()] );
    }

    // ----------------------------------------------------------------------
    // Customizable methods
    // ----------------------------------------------------------------------

    protected PlexusConfiguration createChild( final String childName )
    {
        return new DefaultPlexusConfiguration( childName );
    }

    // ----------------------------------------------------------------------
    // Shared methods
    // ----------------------------------------------------------------------

    protected final void setAttribute( final String attributeName, final String attributeValue )
    {
        if ( null == attributeMap )
        {
            attributeMap = new HashMap<String, String>();
        }
        attributeMap.put( attributeName, attributeValue );
    }

    protected final void addChild( final PlexusConfiguration child )
    {
        createChildren( child.getName() ).add( child );
    }

    // ----------------------------------------------------------------------
    // Implementation methods
    // ----------------------------------------------------------------------

    private final List<PlexusConfiguration> createChildren( final String childName )
    {
        if ( null == childMap )
        {
            childMap = new LinkedHashMap<String, List<PlexusConfiguration>>();
        }
        List<PlexusConfiguration> children = childMap.get( childName );
        if ( null == children )
        {
            children = new ArrayList<PlexusConfiguration>();
            childMap.put( childName, children );
        }
        return children;
    }
}