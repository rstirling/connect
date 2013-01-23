/*
 * Copyright (c) Mirth Corporation. All rights reserved.
 * http://www.mirthcorp.com
 * 
 * The software in this package is published under the terms of the MPL
 * license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package com.mirth.connect.plugins.datatypes.delimited;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import com.mirth.connect.util.StringUtil;


public class DelimitedXMLHandler extends DefaultHandler {

	private StringBuilder output = new StringBuilder();
	
	private DelimitedDeserializationProperties properties;
	private boolean inRow;
	private boolean inColumn;
	private int columnIndex;
	private String columnDelimiter = null;
	private String recordDelimiter = null;
	private String quoteChar = null;
	private String quoteEscapeChar = null;
	private String escapedQuote = null;
	private String escapedQuoteEscape = null;
	private StringBuilder columnValue = null;

	public DelimitedXMLHandler(DelimitedDeserializationProperties properties) {
		super();
		this.properties = properties;
		
		updateColumnDelimiter();
        updateRecordDelimiter();
        updateQuoteChar();
        updateQuoteEscapeChar();
        updateEscapedQuote();
        updateEscapedQuoteEscape();
	}

	// //////////////////////////////////////////////////////////////////
	// Event handlers.
	// //////////////////////////////////////////////////////////////////

	public void startDocument() {
		inRow = false;
		inColumn = false;
	}

	public void endDocument() {
	}

	public void startElement(String uri, String name, String qName, Attributes atts) {
		
		// If it's a row
		if (!inRow && name.length() >= 3 && name.substring(0, 3).equalsIgnoreCase("row")) {
			inRow = true;
			inColumn = false;
			columnIndex = 0;
		}
		else if (inRow && !inColumn) {

			// It's a column
			inColumn = true;

			// If not fixed width columns
			if (properties.getColumnWidths() == null) {

				// If this isn't the first column
				if (columnIndex > 0) {
					output.append(columnDelimiter);
				}
			}
			
			// Initialize the column value
			columnValue = new StringBuilder();
		}
	}

	public void endElement(String uri, String name, String qName) {
		
		// If in a column
		if (inColumn) {
			
			// If fixed width columns
			if (properties.getColumnWidths() != null) {
			
				if (columnIndex < properties.getColumnWidths().length) {

					output.append(columnValue);
					
					// Pad with trailing spaces to fixed column width
					int len = properties.getColumnWidths()[columnIndex] - columnValue.length();
					while (len > 0) {
						output.append(' ');
						len--;
					}
				}
			}
			else {

				// If the column value contains the column delimiter, or record delimiter
				String temp = columnValue.toString();
				if (temp.contains(columnDelimiter) || temp.contains(recordDelimiter)) {
					
					// Escape the escape characters and the quote characters
					temp = temp.replace(quoteEscapeChar, escapedQuoteEscape);
					temp = temp.replace(quoteChar, escapedQuote);
	
					output.append(quoteChar);
					output.append(temp);
					output.append(quoteChar);
				}
				else {
					output.append(columnValue);
				}
			}

			inColumn = false;
			columnIndex++;
		} else if (inRow) {
			inRow = false;
			
			// Append the record delimiter
			output.append(recordDelimiter);
		}
	}

	public void characters(char ch[], int start, int length) {

		if (inColumn) {
			
			if (properties.getColumnWidths() != null) {

				if (columnIndex < properties.getColumnWidths().length) {

					// Get the fixed width of this column
					int columnWidth = properties.getColumnWidths()[columnIndex];
		
					// Truncate if the size of the column value exceeds the fixed column width
					if (columnValue.length() + length > columnWidth) {
						length = columnWidth - columnValue.length();
					}
					
					columnValue.append(ch, start, length);
				}
			}
			else {
				columnValue.append(ch, start, length);
			}
		}
	}

	public StringBuilder getOutput() {
		return output;
	}

	public void setOutput(StringBuilder output) {
		this.output = output;
	}

	private void updateColumnDelimiter() {
		if (columnDelimiter == null) {
			
			if (StringUtils.isNotEmpty(properties.getColumnDelimiter())) {
				columnDelimiter = StringUtil.unescape(properties.getColumnDelimiter()).substring(0,1);
			}
		}
	}

	private void updateRecordDelimiter() {
		if (recordDelimiter == null) {
			
			if (StringUtils.isNotEmpty(properties.getRecordDelimiter())) {
				recordDelimiter = StringUtil.unescape(properties.getRecordDelimiter()).substring(0,1);
			}
		}
	}

	private void updateQuoteChar() {
		if (quoteChar == null) {
			
			if (StringUtils.isNotEmpty(properties.getQuoteChar())) {
				quoteChar = StringUtil.unescape(properties.getQuoteChar()).substring(0,1);
			}
		}
	}

	private void updateQuoteEscapeChar() {
		if (quoteEscapeChar == null) {
			
			if (StringUtils.isNotEmpty(properties.getQuoteEscapeChar())) {
				quoteEscapeChar = StringUtil.unescape(properties.getQuoteEscapeChar()).substring(0,1);
			}
		}
	}

	private void updateEscapedQuote() {
		
		if (escapedQuote == null) {
			
			if (properties.isEscapeWithDoubleQuote()) {
				escapedQuote = quoteChar + quoteChar;
			}
			else {
				escapedQuote = quoteEscapeChar + quoteChar;
			}
		}
	}

	private void updateEscapedQuoteEscape() {
		
		if (escapedQuoteEscape == null) {
			escapedQuoteEscape = quoteEscapeChar + quoteEscapeChar;
		}
	}
}
