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
package org.apache.juneau.dto.atom;

import static org.apache.juneau.dto.atom.Utils.*;

import java.net.URI;

import org.apache.juneau.annotation.*;

/**
 * Represents an <code>atomPersonConstruct</code> construct in the RFC4287 specification.
 * <p>
 * <h6 class='figure'>Schema</h6>
 * <p class='bcode'>
 * 	atomPersonConstruct =
 * 		atomCommonAttributes,
 * 		(element atom:name { text }
 * 		&amp; element atom:uri { atomUri }?
 * 		&amp; element atom:email { atomEmailAddress }?
 * 		&amp; extensionElement*)
 * </p>
 * <p>
 * Refer to <a class='doclink' href='package-summary.html#TOC'>org.apache.juneau.dto.atom</a> for further information about ATOM support.
 */
@SuppressWarnings("hiding")
public class Person extends Common {

	private String name;
	private URI uri;
	private String email;


	/**
	 * Normal constructor.
	 *
	 * @param name The name of the person.
	 */
	public Person(String name) {
		name(name);
	}

	/** Bean constructor. */
	public Person() {}


	//--------------------------------------------------------------------------------
	// Bean properties
	//--------------------------------------------------------------------------------

	/**
	 * Returns the name of the person.
	 *
	 * @return The name of the person.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of the person.
	 *
	 * @param name The name of the person.
	 * @return This object (for method chaining).
	 */
	@BeanProperty(name="name")
	public Person name(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Returns the URI of the person.
	 *
	 * @return The URI of the person.
	 */
	public URI getUri() {
		return uri;
	}

	/**
	 * Sets the URI of the person.
	 *
	 * @param uri The URI of the person.
	 * @return This object (for method chaining).
	 */
	@BeanProperty(name="uri")
	public Person uri(URI uri) {
		this.uri = uri;
		return this;
	}

	/**
	 * Sets the URI of the person.
	 *
	 * @param uri The URI of the person.
	 * @return This object (for method chaining).
	 */
	@BeanProperty(name="uri")
	public Person uri(String uri) {
		this.uri = toURI(uri);
		return this;
	}

	/**
	 * Returns the email address of the person.
	 *
	 * @return The email address of the person.
	 */
	public String getEmail() {
		return email;
	}

	/**
	 * Sets the email address of the person.
	 *
	 * @param email The email address of the person.
	 * @return This object (for method chaining).
	 */
	@BeanProperty(name="email")
	public Person email(String email) {
		this.email = email;
		return this;
	}


	//--------------------------------------------------------------------------------
	// Overridden setters (to simplify method chaining)
	//--------------------------------------------------------------------------------

	@Override /* Common */
	public Person base(URI base) {
		super.base(base);
		return this;
	}

	@Override /* Common */
	public Person lang(String lang) {
		super.lang(lang);
		return this;
	}
}
