/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015, 2016. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.microservice;

import static com.ibm.juno.core.html.HtmlDocSerializerProperties.*;

import com.ibm.juno.core.utils.*;
import com.ibm.juno.server.*;
import com.ibm.juno.server.annotation.*;

/**
 * Superclass for all REST resource groups.
 * <p>
 * In additional to the functionality of the {@link RestServletGroupDefault} group, 
 * augments the {@link #createVarResolver()} method with the following additional variable types:
 * <ul>
 * 	<li><jk>$ARG{...}</jk> - Command line arguments.<br>
 * 		Resolves values from {@link Microservice#getArgs()}.<br>
 * 		<h6>Example:</h6>
 * 		<p class='bcode'>
 * 			String firstArg = request.getVarResolver().resolve(<js>"$ARG{0}"</js>);  <jc>// First argument.</jc> 
 * 			String namedArg = request.getVarResolver().resolve(<js>"$ARG{myarg}"</js>);  <jc>// Named argument (e.g. "myarg=foo"). </jc>
 * 		</p>
 * 	<li><jk>$MF{...}</jk> - Manifest file entries.
 * 		<h6>Example:</h6>
 * 		<p class='bcode'>
 * 			String mainClass = request.getVarResolver().resolve(<js>"$MF{Main-Class}"</js>);  <jc>// Main class. </jc>
 * 		</p>
 * </ul>
 * 
 * @author James Bognar (jbognar@us.ibm.com)
 */
@SuppressWarnings("serial")
@RestResource(
	properties={
		@Property(name=HTMLDOC_links, value="{up:'$R{requestParentURI}',options:'$R{servletURI}?method=OPTIONS'}"),
	},
	config="$S{juno.configFile}",
	stylesheet="$C{REST/stylesheet,styles/juno.css}"
)
public abstract class ResourceGroup extends RestServletGroupDefault {

	@Override
	protected StringVarResolver createVarResolver() {
		StringVarResolver r = super.createVarResolver();

		// Command-line arguments.
		r.addVar("ARG", new StringVarWithDefault() {
			@Override /* StringVar */
			public String resolve(String varVal) {
				return Microservice.getArgs().getArg(varVal);
			}
		});

		// Manifest file entries.
		r.addVar("MF", new StringMapVar(Microservice.getManifest()));

		return r;
	}
}
