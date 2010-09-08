/*
 * Copyright (C) 2009 Emweb bvba, Leuven, Belgium.
 *
 * See the LICENSE file for terms of use.
 */
package eu.webtoolkit.jwt;


enum Property {
	PropertyInnerHTML, PropertyAddedInnerHTML, PropertyValue, PropertyDisabled, PropertyChecked, PropertySelected, PropertySelectedIndex, PropertyMultiple, PropertyTarget, PropertyIndeterminate, PropertySrc, PropertyText, PropertyScript, PropertyColSpan, PropertyRowSpan, PropertyReadOnly, PropertyTabIndex, PropertyClass, PropertyStyle, PropertyStyleWidthExpression, PropertyStylePosition, PropertyStyleZIndex, PropertyStyleFloat, PropertyStyleClear, PropertyStyleWidth, PropertyStyleHeight, PropertyStyleLineHeight, PropertyStyleMinWidth, PropertyStyleMinHeight, PropertyStyleMaxWidth, PropertyStyleMaxHeight, PropertyStyleLeft, PropertyStyleRight, PropertyStyleTop, PropertyStyleBottom, PropertyStyleVerticalAlign, PropertyStyleTextAlign, PropertyStylePadding, PropertyStylePaddingRight, PropertyStylePaddingLeft, PropertyStyleMarginTop, PropertyStyleMarginRight, PropertyStyleMarginBottom, PropertyStyleMarginLeft, PropertyStyleCursor, PropertyStyleBorderTop, PropertyStyleBorderRight, PropertyStyleBorderBottom, PropertyStyleBorderLeft, PropertyStyleColor, PropertyStyleOverflowX, PropertyStyleOverflowY, PropertyStyleOpacity, PropertyStyleFontFamily, PropertyStyleFontStyle, PropertyStyleFontVariant, PropertyStyleFontWeight, PropertyStyleFontSize, PropertyStyleBackgroundColor, PropertyStyleBackgroundImage, PropertyStyleBackgroundRepeat, PropertyStyleBackgroundAttachment, PropertyStyleBackgroundPosition, PropertyStyleTextDecoration, PropertyStyleWhiteSpace, PropertyStyleTableLayout, PropertyStyleBorderSpacing, PropertyStyleVisibility, PropertyStyleDisplay;

	/**
	 * Returns the numerical representation of this enum.
	 */
	public int getValue() {
		return ordinal();
	}
}
