// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.dto.html5;

import java.net.*;

import org.apache.juneau.annotation.*;

/**
 * DTO for an HTML <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#the-area-element">&lt;area&gt;</a> element.
 * <p>
 */
@Bean(typeName="area")
public class Area extends HtmlElementVoid {

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-area-alt">alt</a> attribute.
	 * Replacement text for use when images are not available.
	 * @param alt The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Area alt(String alt) {
		attr("alt", alt);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-area-coords">coords</a> attribute.
	 * Coordinates for the shape to be created in an image map.
	 * @param coords The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Area coords(String coords) {
		attr("coords", coords);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/links.html#attr-hyperlink-download">download</a> attribute.
	 * Whether to download the resource instead of navigating to it, and its file name if so.
	 * @param download The new value for this attribute.
	 * Typically a {@link Boolean} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Area download(Object download) {
		attr("download", download);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/links.html#attr-hyperlink-href">href</a> attribute.
	 * Address of the hyperlink.
	 * @param href The new value for this attribute.
	 * Typically a {@link URL} or {@link String}.
	 * @return This object (for method chaining).
	 */
	public final Area href(Object href) {
		attr("href", href);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/links.html#attr-hyperlink-hreflang">hreflang</a> attribute.
	 * Language of the linked resource.
	 * @param hreflang The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Area hreflang(String hreflang) {
		attr("hreflang", hreflang);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/links.html#attr-hyperlink-rel">rel</a> attribute.
	 * Relationship between the document containing the hyperlink and the destination resource.
	 * @param rel The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Area rel(String rel) {
		attr("rel", rel);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/embedded-content-0.html#attr-area-shape">shape</a> attribute.
	 * The kind of shape to be created in an image map.
	 * @param shape The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Area shape(String shape) {
		attr("shape", shape);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/links.html#attr-hyperlink-target">target</a> attribute.
	 * Browsing context for hyperlink navigation.
	 * @param target The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Area target(String target) {
		attr("target", target);
		return this;
	}

	/**
	 * <a class="doclink" href="https://www.w3.org/TR/html5/links.html#attr-hyperlink-type">type</a> attribute.
	 * Hint for the type of the referenced resource.
	 * @param type The new value for this attribute.
	 * @return This object (for method chaining).
	 */
	public final Area type(String type) {
		attr("type", type);
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden methods
	//--------------------------------------------------------------------------------

	@Override /* HtmlElement */
	public final Area _class(String _class) {
		super._class(_class);
		return this;
	}

	@Override /* HtmlElement */
	public final Area id(String id) {
		super.id(id);
		return this;
	}

	@Override /* HtmlElement */
	public final Area style(String style) {
		super.style(style);
		return this;
	}
}
