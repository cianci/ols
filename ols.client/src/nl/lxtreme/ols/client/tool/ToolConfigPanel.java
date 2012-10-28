/*
 * OpenBench LogicSniffer / SUMP project 
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin St, Fifth Floor, Boston, MA 02110, USA
 *
 * Copyright (C) 2006-2010 Michael Poppitz, www.sump.org
 * Copyright (C) 2010-2012 J.W. Janssen, www.lxtreme.nl
 */
package nl.lxtreme.ols.client.tool;


import static nl.lxtreme.ols.client.tool.EditorUtils.*;
import static nl.lxtreme.ols.util.swing.SwingComponentUtils.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.Map.Entry;

import javax.swing.*;
import javax.swing.text.*;

import nl.lxtreme.ols.tool.api.*;
import nl.lxtreme.ols.util.swing.*;

import org.osgi.service.metatype.*;


/**
 * Provides a panel for configuring tools.
 */
public class ToolConfigPanel extends JPanel
{
  // CONSTANTS

  private static final long serialVersionUID = 1L;

  // VARIABLES

  private final ObjectClassDefinition ocd;
  private final ToolContext context;
  private final Map<AttributeDefinition, JComponent> components;

  // CONSTRUCTORS

  /**
   * Creates a new {@link ToolConfigPanel} instance.
   * 
   * @param aSettings
   */
  public ToolConfigPanel( final ObjectClassDefinition aOCD, final ToolContext aContext,
      final Map<Object, Object> aSettings )
  {
    super( new SpringLayout() );

    this.ocd = aOCD;
    this.context = aContext;

    this.components = new HashMap<AttributeDefinition, JComponent>();

    initPanel( aSettings );
  }

  // METHODS

  /**
   * @return <code>true</code> if the settings are valid, <code>false</code>
   *         otherwise.
   */
  public boolean areSettingsValid()
  {
    for ( Entry<AttributeDefinition, JComponent> entry : this.components.entrySet() )
    {
      String value = String.valueOf( getComponentValue( entry.getValue() ) );

      String validationResult = entry.getKey().validate( value );
      if ( ( validationResult != null ) && !"".equals( validationResult ) )
      {
        return false;
      }
    }

    return true;
  }

  /**
   * @return the properties with the current values, never <code>null</code>.
   */
  public Dictionary<Object, Object> getProperties()
  {
    Hashtable<Object, Object> result = new Hashtable<Object, Object>();
    for ( Entry<AttributeDefinition, JComponent> entry : this.components.entrySet() )
    {
      Object value = getComponentValue( entry.getValue() );

      if ( value != null )
      {
        result.put( entry.getKey().getID(), value );
      }
    }
    return result;
  }

  /**
   * Initializes this panel.
   * 
   * @param aSettings
   */
  final void initPanel( final Map<Object, Object> aSettings )
  {
    SpringLayoutUtils.addSeparator( this, "Settings" );

    AttributeDefinition[] ads = this.ocd.getAttributeDefinitions( ObjectClassDefinition.ALL );
    for ( AttributeDefinition ad : ads )
    {
      Object initialValue = aSettings.get( ad.getID() );

      JComponent comp = createEditorComponent( ad, this.context, initialValue );
      if ( comp != null )
      {
        add( createRightAlignedLabel( ad.getName() ) );
        add( comp );

        this.components.put( ad, comp );
      }
    }

    applyComponentProperties( this.components.values() );

    SpringLayoutUtils.makeEditorGrid( this, 6, 4, 6, 6 );
  }

  /**
   * @param aIndex
   * @param aComponent
   * @param aDescriptor
   */
  private void addListener( final Map<String, JComponent> aIndex, final JComponent aComponent, final String aDescriptor )
  {
    if ( aComponent instanceof AbstractButton )
    {
      final AbstractButton button = ( AbstractButton )aComponent;
      final String[] parts = aDescriptor.split( "\\s*;\\s*" );

      button.addActionListener( new ActionListener()
      {
        @Override
        public void actionPerformed( final ActionEvent aEvent )
        {
          for ( String part : parts )
          {
            final boolean aInvert = part.startsWith( "!" );
            final JComponent target = aIndex.get( aInvert ? part.substring( 1 ) : part );
            if ( target != null )
            {
              boolean value = button.isSelected();
              if ( aInvert )
              {
                value = !value;
              }
              target.setEnabled( value );
            }
          }
        }
      } );

      for ( String part : parts )
      {
        final boolean aInvert = part.startsWith( "!" );
        final JComponent target = aIndex.get( aInvert ? part.substring( 1 ) : part );
        if ( target != null )
        {
          boolean value = button.isSelected();
          if ( aInvert )
          {
            value = !value;
          }
          target.setEnabled( value );
        }
      }
    }
    else if ( aComponent instanceof JComboBox )
    {
      final JComboBox combobox = ( JComboBox )aComponent;
      final String[] parts = aDescriptor.split( "\\s*:\\s*" );

      combobox.addActionListener( new ActionListener()
      {
        @Override
        public void actionPerformed( final ActionEvent aEvent )
        {
          final int index = combobox.getSelectedIndex();
          if ( ( index >= 0 ) && ( index < parts.length ) )
          {
            String part = parts[index];
            final boolean aInvert = part.startsWith( "!" );
            final JComponent target = aIndex.get( aInvert ? part.substring( 1 ) : part );
            if ( target != null )
            {
              boolean value = aInvert ? false : true;
              target.setEnabled( value );
            }
          }
        }
      } );

      final int index = combobox.getSelectedIndex();
      if ( ( index >= 0 ) && ( index < parts.length ) )
      {
        String part = parts[index];
        final boolean aInvert = part.startsWith( "!" );
        final JComponent target = aIndex.get( aInvert ? part.substring( 1 ) : part );
        if ( target != null )
        {
          boolean value = aInvert ? false : true;
          target.setEnabled( value );
        }
      }
    }
    else if ( aComponent instanceof JTextComponent )
    {
      // TODO
    }
    else if ( aComponent instanceof JList )
    {
      // TODO
    }
    else if ( aComponent instanceof JSlider )
    {
      // TODO
    }
    else if ( aComponent instanceof JSpinner )
    {
      // TODO
    }
  }

  /**
   * @param aComponents
   */
  private void applyComponentProperties( final Collection<JComponent> aComponents )
  {
    // Create an index on the component's name...
    Map<String, JComponent> nameIndex = new HashMap<String, JComponent>();
    for ( JComponent comp : aComponents )
    {
      nameIndex.put( comp.getName(), comp );
    }

    // Process the component's properties...
    for ( JComponent comp : aComponents )
    {
      Object value = comp.getClientProperty( PROPERTY_READONLY );
      if ( Boolean.TRUE.equals( value ) )
      {
        comp.setEnabled( false );
      }
      value = comp.getClientProperty( PROPERTY_EDITABLE );
      if ( Boolean.TRUE.equals( value ) )
      {
        if ( comp instanceof JComboBox )
        {
          ( ( JComboBox )comp ).setEditable( true );
        }
        else if ( comp instanceof JTextComponent )
        {
          ( ( JTextComponent )comp ).setEditable( true );
        }
      }
      value = comp.getClientProperty( PROPERTY_LISTENER );
      if ( value != null )
      {
        for ( String descriptor : ( String[] )value )
        {
          addListener( nameIndex, comp, descriptor );
        }
      }
    }
  }

  /**
   * @param aComponent
   * @return
   */
  @SuppressWarnings( "boxing" )
  private Object getComponentValue( final Component aComponent )
  {
    Object value = null;
    if ( aComponent instanceof AbstractButton )
    {
      value = ( ( AbstractButton )aComponent ).isSelected();
    }
    else if ( aComponent instanceof JComboBox )
    {
      value = ( ( JComboBox )aComponent ).getSelectedItem();
    }
    else if ( aComponent instanceof JTextComponent )
    {
      value = ( ( JTextComponent )aComponent ).getText();
    }
    else if ( aComponent instanceof JList )
    {
      value = ( ( JList )aComponent ).getSelectedIndex();
    }
    else if ( aComponent instanceof JSlider )
    {
      value = ( ( JSlider )aComponent ).getValue();
    }
    else if ( aComponent instanceof JSpinner )
    {
      value = ( ( JSpinner )aComponent ).getValue();
    }
    return value;
  }
}