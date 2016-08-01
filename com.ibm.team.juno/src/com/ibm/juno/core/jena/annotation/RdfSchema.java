/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.jena.annotation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.*;

/**
 * Identifies the default RDF namespaces at the package level.
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Documented
@Target(PACKAGE)
@Retention(RUNTIME)
@Inherited
public @interface RdfSchema {

	/**
	 * Sets the default RDF prefix for all classes in this and child packages.
	 * <p>
	 * 	Must either be matched with a {@link #namespace()} annotation, or an {@link #rdfNs()} mapping with the
	 * 	same {@link RdfNs#prefix} value.
	 * </p>
	 */
	public String prefix() default "";

	/**
	 * Sets the default RDF namespace URL for all classes in this and child packages.
	 * <p>
	 * 	Must either be matched with a {@link #prefix()} annotation, or an {@link #rdfNs()} mapping with the
	 * 	same {@link RdfNs#namespaceURI} value.
	 * </p>
	 */
	public String namespace() default "";

	/**
	 * Lists all namespace mappings to be used on all classes within this package.
	 * <p>
	 * 	The purpose of this annotation is to allow namespace mappings to be defined in a single location
	 * 	and referred to by name through just the {@link Rdf#prefix()} annotation.
	 * <p>
	 * 	Inherited by child packages.
	 *
	 * <dl>
	 * 	<dt>Example:</dt>
	 * 	<dd>
	 * <p>
	 * 	Contents of <code>package-info.java</code>...
	 * </p>
	 * <p class='bcode'>
	 * 	<jc>// XML namespaces used within this package.</jc>
	 * 	<ja>@RdfSchema</ja>(prefix=<js>"ab"</js>,
	 * 		namespaces={
	 * 			<ja>@RdfNs</ja>(prefix=<js>"ab"</js>, namespaceURI=<js>"http://www.ibm.com/addressBook/"</js>),
	 * 			<ja>@RdfNs</ja>(prefix=<js>"per"</js>, namespaceURI=<js>"http://www.ibm.com/person/"</js>),
	 * 			<ja>@RdfNs</ja>(prefix=<js>"addr"</js>, namespaceURI=<js>"http://www.ibm.com/address/"</js>),
	 * 			<ja>@RdfNs</ja>(prefix=<js>"mail"</js>, namespaceURI="<js>http://www.ibm.com/mail/"</js>)
	 * 		}
	 * 	)
	 * 	<jk>package</jk> com.ibm.sample.addressbook;
	 * 	<jk>import</jk> com.ibm.juno.core.rdf.annotation.*;
	 * </p>
	 * <p>
	 * 	Class in package using defined namespaces...
	 * </p>
	 * <p class='bcode'>
	 * 	<jk>package</jk> com.ibm.sample.addressbook;
	 *
	 * 	<jc>// Bean class, override "ab" namespace on package.</jc>
	 * 	<ja>@Rdf</ja>(prefix=<js>"addr"</js>)
	 * 	<jk>public class</jk> Address {
	 *
	 * 		<jc>// Bean property, use "addr" namespace on class.</jc>
	 * 		<jk>public int</jk> <jf>id</jf>;
	 *
	 * 		<jc>// Bean property, override with "mail" namespace.</jc>
	 * 		<ja>@Rdf</ja>(prefix=<js>"mail"</js>)
	 * 		<jk>public</jk> String <jf>street</jf>, <jf>city</jf>, <jf>state</jf>;
	 * 	}
	 * </p>
	 * 	</dd>
	 * </dl>
	 */
	public RdfNs[] rdfNs() default {};
}
