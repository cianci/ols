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
 * Copyright (C) 2010 J.W. Janssen, www.lxtreme.nl
 */
package nl.lxtreme.ols.tool.spi;


import java.util.logging.*;

import nl.lxtreme.ols.api.data.*;
import nl.lxtreme.ols.api.tools.*;
import nl.lxtreme.ols.tool.base.*;
import nl.lxtreme.ols.util.*;
import nl.lxtreme.ols.util.NumberUtils.BitOrder;


/**
 * @author jajans
 */
public class SPIAnalyserWorker extends BaseAsyncToolWorker<SPIDataSet>
{
  // CONSTANTS

  private static final Logger LOG = Logger.getLogger( SPIAnalyserWorker.class.getName() );

  // VARIABLES

  private int csIdx;
  private int sckIdx;
  private SPIMode mode;
  private int bitCount;
  private BitOrder bitOrder;
  private boolean reportCS;
  private boolean honourCS;
  private int mosiIdx;
  private int misoIdx;

  private String mosiLabel;

  private String misoLabel;

  // CONSTRUCTORS

  /**
   * @param aData
   */
  public SPIAnalyserWorker( final DataContainer aData, final ToolContext aContext )
  {
    super( aData, aContext );
  }

  // METHODS

  /**
   * Sets the number of bits an SPI datagram should consist of.
   * 
   * @param aBitCount
   *          the number of bits in a SPI datagram, >= 8.
   */
  public void setBitCount( final int aBitCount )
  {
    this.bitCount = aBitCount;
  }

  /**
   * Sets the chip-select channel index.
   * 
   * @param aCsMask
   *          the index of the chip-select channel.
   */
  public void setCSIndex( final int aCsIndex )
  {
    this.csIdx = aCsIndex;
  }

  /**
   * Sets whether or not chip-select should be honoured in the analysis.
   * 
   * @param aHonourCS
   *          <code>true</code> to only decode data when the chip-select line is
   *          low, <code>false</code> to decode all data.
   */
  public void setHonourCS( final boolean aHonourCS )
  {
    this.honourCS = aHonourCS;
  }

  /**
   * Sets the "master-in slave-out" channel index.
   * 
   * @param aMisoMask
   *          the index of the "master-in slave-out" channel.
   */
  public void setMisoIndex( final int aMisoIndex )
  {
    this.misoIdx = aMisoIndex;
  }

  /**
   * Sets which SPI mode should be used for the analysis process.
   * 
   * @param aMode
   *          the SPI mode to set, cannot be <code>null</code>.
   */
  public void setMode( final SPIMode aMode )
  {
    this.mode = aMode;
  }

  /**
   * Sets the "master-out slave-in" channel index.
   * 
   * @param aMosiMask
   *          the index of the "master-out slave-in" channel.
   */
  public void setMosiIndex( final int aMosiIndex )
  {
    this.mosiIdx = aMosiIndex;
  }

  /**
   * Sets the order in which bits in a SPI datagram are transmitted.
   * 
   * @param aOrder
   *          the bit order to use, cannot be <code>null</code>.
   */
  public void setOrder( final BitOrder aOrder )
  {
    this.bitOrder = aOrder;
  }

  /**
   * Sets whether or not chip-select events should be reported.
   * 
   * @param aReportCS
   *          <code>true</code> to include chip-select events in the analysis
   *          result, <code>false</code> to exclude them.
   */
  public void setReportCS( final boolean aReportCS )
  {
    this.reportCS = aReportCS;

  }

  /**
   * Sets the serial-clock channel index.
   * 
   * @param aSckIndex
   *          the index of the "serial-clock" channel.
   */
  public void setSCKIndex( final int aSckIndex )
  {
    this.sckIdx = aSckIndex;
  }

  /**
   * This is the SPI protocol decoder core The decoder scans for a decode start
   * event like CS high to low edge or the trigger of the captured data. After
   * this the decoder starts to decode the data by the selected mode, number of
   * bits and bit order. The decoded data are put to a JTable object directly.
   * 
   * @see javax.swing.SwingWorker#doInBackground()
   */
  @Override
  protected SPIDataSet doInBackground() throws Exception
  {
    if ( LOG.isLoggable( Level.FINE ) )
    {
      LOG.fine( "csmask   = 0x" + Integer.toHexString( 1 << this.csIdx ) );
      LOG.fine( "sckmask  = 0x" + Integer.toHexString( 1 << this.sckIdx ) );
      LOG.fine( "misomask = 0x" + Integer.toHexString( 1 << this.misoIdx ) );
      LOG.fine( "mosimask = 0x" + Integer.toHexString( 1 << this.mosiIdx ) );
    }

    final int[] values = getValues();

    this.mosiLabel = getChannelLabel( this.mosiIdx, "MOSI" );
    setChannelLabel( this.mosiIdx, this.mosiLabel );

    this.misoLabel = getChannelLabel( this.misoIdx, "MISO" );
    setChannelLabel( this.misoIdx, this.misoLabel );

    int startOfDecode;
    int endOfDecode;
    boolean slaveSelected = false;

    if ( isCursorsEnabled() && isCursorPositionSet( 0 ) && isCursorPositionSet( 1 ) )
    {
      startOfDecode = getSampleIndex( getCursorPosition( 0 ) );
      endOfDecode = getSampleIndex( getCursorPosition( 1 ) + 1 );

      // Search for a CS-low backwards from the first cursor...
      slaveSelected = searchSlaveSelected( startOfDecode, 0 ) >= 0;
      if ( !slaveSelected )
      {
        // Search for a CS-low forwards from the first cursor...
        slaveSelected = searchSlaveSelected( startOfDecode, endOfDecode ) >= 0;
      }
    }
    else if ( hasTriggerData() )
    {
      startOfDecode = getTriggerIndex();
      endOfDecode = values.length;

      // Search for a CS-low backwards from the trigger position...
      slaveSelected = searchSlaveSelected( startOfDecode, 0 ) >= 0;
      if ( !slaveSelected )
      {
        // Search for a CS-low forwards from the first cursor...
        slaveSelected = searchSlaveSelected( startOfDecode, endOfDecode ) >= 0;
      }
    }
    else
    {
      // Search for a CS-low forwards until the end...
      endOfDecode = values.length;
      startOfDecode = searchSlaveSelected( 0, endOfDecode );

      slaveSelected = ( startOfDecode > 0 );
    }

    if ( !slaveSelected || ( startOfDecode >= endOfDecode ) )
    {
      // no CS edge found, look for trigger
      LOG.log( Level.WARNING, "No CS start-condition found! Analysis aborted..." );
      throw new IllegalStateException( "No CS start-condition found!" );
    }

    final SPIDataSet decodedData = new SPIDataSet( startOfDecode, endOfDecode, this );
    if ( slaveSelected )
    {
      // now the trigger is in b, add trigger event to table
      reportCsLow( decodedData, startOfDecode );
    }

    // clear any existing annotations
    clearChannelAnnotations( this.misoIdx );
    clearChannelAnnotations( this.mosiIdx );

    /*
     * Use the mode parameter to determine which edges are to detect. Mode 0 and
     * mode 3 are sampling on the rising clk edge, mode 1 and 2 are sampling on
     * the falling edge. a is used for start of value, c is register for detect
     * line changes.
     */
    clockDataOnEdge( decodedData, this.mode );

    return decodedData;
  }

  /**
   * Decodes the SPI-data on a given clock edge.
   * 
   * @param aDataSet
   *          the decoded data to fill;
   * @param aMode
   *          the SPI mode defining the edges on which data can be sampled and
   *          on which edges data can change.
   */
  private void clockDataOnEdge( final SPIDataSet aDataSet, final SPIMode aMode )
  {
    final int[] values = getValues();

    final int startOfDecode = aDataSet.getStartOfDecode();
    final int endOfDecode = aDataSet.getEndOfDecode();

    final Edge sampleEdge = aMode.getSampleEdge();
    final Edge dataChangeEdge = aMode.getDataChangeEdge();

    final int misoMask = 1 << this.misoIdx;
    final int mosiMask = 1 << this.mosiIdx;
    final int sckMask = 1 << this.sckIdx;
    final int csMask = 1 << this.csIdx;

    // scanning for falling/rising clk edges
    int oldSckValue = ( values[startOfDecode] & sckMask );
    int oldCsValue = ( values[startOfDecode] & csMask );

    // We've already found the
    boolean slaveSelected = true;
    boolean dataEdgeSeen = false;
    int lastIdx = startOfDecode;
    int bitIdx = this.bitCount;

    int misovalue = 0;
    int mosivalue = 0;

    final double length = endOfDecode - startOfDecode;
    for ( int idx = startOfDecode + 1; idx < endOfDecode; idx++ )
    {
      /* CLK edge detection */
      final int sckValue = values[idx] & sckMask;
      /* CS edge detection */
      final int csValue = values[idx] & csMask;

      final Edge slaveSelectEdge = Edge.toEdge( oldCsValue, csValue );
      oldCsValue = csValue;

      if ( slaveSelectEdge.isFalling() )
      {
        reportCsLow( aDataSet, idx );

        slaveSelected = true;
        dataEdgeSeen = false;
      }
      else if ( slaveSelectEdge.isRising() )
      {
        reportCsHigh( aDataSet, idx );

        slaveSelected = false;
      }

      if ( this.honourCS && !slaveSelected )
      {
        // We should honour the slave-select, but the slave isn't
        // currently selected...
        continue;
      }

      final Edge clockEdge = Edge.toEdge( oldSckValue, sckValue );
      oldSckValue = sckValue;

      // In case the clock is phase-shifted with respect to the data line,
      // we should wait until the first inverse edge is seen. To put it
      // otherwise, we should simply ignore the first seen clock edge and
      // wait for the second one...
      if ( dataChangeEdge == clockEdge )
      {
        dataEdgeSeen = true;
      }
      // Keep track where we saw the first clocked bit of a byte-value...
      if ( dataEdgeSeen )
      {
        if ( bitIdx == this.bitCount )
        {
          lastIdx = idx - 1;
        }
      }

      final boolean sampleEdgeSeen = dataEdgeSeen && ( sampleEdge == clockEdge );
      if ( sampleEdgeSeen )
      {
        // sample MiSo here; always MSB first, perform conversion later on...
        if ( ( values[idx] & misoMask ) == misoMask )
        {
          misovalue |= ( 1 << bitIdx );
        }
        // sample MoSi here; always MSB first, perform conversion later on...
        if ( ( values[idx] & mosiMask ) == mosiMask )
        {
          mosivalue |= ( 1 << bitIdx );
        }

        if ( bitIdx > 0 )
        {
          bitIdx--;
        }
        else if ( bitIdx == 0 )
        {
          // Full datagram decoded...
          reportData( aDataSet, lastIdx, idx, mosivalue, misovalue );

          bitIdx = this.bitCount;
          misovalue = 0;
          mosivalue = 0;
        }
      }

      setProgress( ( int )( ( idx - startOfDecode ) * 100.0 / length ) );
    }
  }

  /**
   * Reports a slave-select low->high transition, effectively causing the slave
   * to be no longer selected.
   * 
   * @param aDecodedData
   *          the data set to add the event to;
   * @param aIndex
   *          the sample index on which the event occurred.
   */
  private void reportCsHigh( final SPIDataSet aDecodedData, final int aIndex )
  {
    if ( this.reportCS )
    {
      aDecodedData.reportCSHigh( this.csIdx, aIndex );
    }
  }

  /**
   * Reports a slave-select high->low transition, effectively causing the slave
   * to be selected.
   * 
   * @param aDecodedData
   *          the data set to add the event to;
   * @param aIndex
   *          the sample index on which the event occurred.
   */
  private void reportCsLow( final SPIDataSet aDecodedData, final int aIndex )
  {
    if ( this.reportCS )
    {
      aDecodedData.reportCSLow( this.csIdx, aIndex );
    }
  }

  /**
   * Reports a set of data-bytes (both MISO and MOSI).
   * 
   * @param aDecodedData
   *          the data set to add the data event(s) to;
   * @param aStartIdx
   *          the starting sample index on which the data started;
   * @param aEndIdx
   *          the ending sample index on which the data ended;
   * @param aMosiValue
   *          the MOSI data value;
   * @param aMisoValue
   *          the MISO data value.
   */
  private void reportData( final SPIDataSet aDecodedData, final int aStartIdx, final int aEndIdx, final int aMosiValue,
      final int aMisoValue )
  {
    // Perform bit-order conversion on the full byte...
    final int mosivalue = NumberUtils.convertByteOrder( aMosiValue, this.bitOrder );
    final int misovalue = NumberUtils.convertByteOrder( aMisoValue, this.bitOrder );

    addChannelAnnotation( this.mosiIdx, aStartIdx, aEndIdx, String.format( "0x%X (%c)", mosivalue, mosivalue ) );
    aDecodedData.reportMosiData( this.mosiIdx, aStartIdx, aEndIdx, mosivalue );

    addChannelAnnotation( this.misoIdx, aStartIdx, aEndIdx, String.format( "0x%X (%c)", misovalue, misovalue ) );
    aDecodedData.reportMisoData( this.misoIdx, aStartIdx, aEndIdx, misovalue );
  }

  /**
   * @param aSampleIndex
   * @param aI
   * @return
   */
  private int searchSlaveSelected( final int aStartIndex, final int aEndIndex )
  {
    final int[] values = getValues();

    final int csMask = 1 << this.csIdx;

    /*
     * For analyze scan the CS line for a falling edge. If no edge could be
     * found, the position of the trigger is used for start of analysis. If no
     * trigger and no edge is found the analysis fails.
     */
    if ( aStartIndex > aEndIndex )
    {
      // Walk backwards...
      int oldCsValue = values[aStartIndex] & csMask;
      for ( int i = aStartIndex; i >= aEndIndex; i-- )
      {
        final int csValue = values[i] & csMask;
        if ( oldCsValue < csValue )
        {
          // found first falling edge; start decoding from here...
          if ( LOG.isLoggable( Level.FINE ) )
          {
            LOG.fine( "CS found at " + i );
          }

          return i;
        }
        oldCsValue = csValue;
      }
    }
    else
    {
      // Walk forwards...
      int oldCsValue = values[aStartIndex] & csMask;
      for ( int i = aStartIndex + 1; i < aEndIndex; i++ )
      {
        final int csValue = values[i] & csMask;
        if ( oldCsValue > csValue )
        {
          // found first falling edge; start decoding from here...
          if ( LOG.isLoggable( Level.FINE ) )
          {
            LOG.fine( "CS found at " + i );
          }

          return i;
        }
        oldCsValue = csValue;
      }
    }

    return -1;
  }
}
