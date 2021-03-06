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
package org.apache.juneau.jena;

import static org.apache.juneau.jena.Constants.*;
import static org.apache.juneau.jena.RdfCommonContext.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.xml.*;

import com.hp.hpl.jena.rdf.model.*;

/**
 * Serializes POJOs to RDF.
 *
 * <h5 class='section'>Configurable properties:</h5>
 * <p>
 * Refer to <a class="doclink" href="package-summary.html#SerializerConfigurableProperties">Configurable Properties</a>
 * 	for the entire list of configurable properties.
 *
 * <h6 class='topic'>Behavior-specific subclasses</h6>
 * <p>
 * The following direct subclasses are provided for language-specific serializers:
 * <ul>
 * 	<li>{@link RdfSerializer.Xml} - RDF/XML.
 * 	<li>{@link RdfSerializer.XmlAbbrev} - RDF/XML-ABBREV.
 * 	<li>{@link RdfSerializer.NTriple} - N-TRIPLE.
 * 	<li>{@link RdfSerializer.Turtle} - TURTLE.
 * 	<li>{@link RdfSerializer.N3} - N3.
 * </ul>
 *
 * <h5 class='section'>Additional information:</h5>
 * <p>
 * See <a class="doclink" href="package-summary.html#TOC">RDF Overview</a> for an overview of RDF support in Juneau.
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
@Produces(value="text/xml+rdf+abbrev", contentType="text/xml+rdf")
public class RdfSerializer extends WriterSerializer {

	/** Default RDF/XML serializer, all default settings.*/
	public static final RdfSerializer DEFAULT_XML = new Xml(PropertyStore.create());

	/** Default Abbreviated RDF/XML serializer, all default settings.*/
	public static final RdfSerializer DEFAULT_XMLABBREV = new XmlAbbrev(PropertyStore.create());

	/** Default Turtle serializer, all default settings.*/
	public static final RdfSerializer DEFAULT_TURTLE = new Turtle(PropertyStore.create());

	/** Default N-Triple serializer, all default settings.*/
	public static final RdfSerializer DEFAULT_NTRIPLE = new NTriple(PropertyStore.create());

	/** Default N3 serializer, all default settings.*/
	public static final RdfSerializer DEFAULT_N3 = new N3(PropertyStore.create());


	/** Produces RDF/XML output */
	@Produces("text/xml+rdf")
	public static class Xml extends RdfSerializer {

		/**
		 * Constructor.
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public Xml(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override /* CoreObject */
		protected ObjectMap getOverrideProperties() {
			return super.getOverrideProperties().append(RDF_language, LANG_RDF_XML);
		}
	}

	/** Produces Abbreviated RDF/XML output */
	@Produces(value="text/xml+rdf+abbrev", contentType="text/xml+rdf")
	public static class XmlAbbrev extends RdfSerializer {

		/**
		 * Constructor.
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public XmlAbbrev(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override /* CoreObject */
		protected ObjectMap getOverrideProperties() {
			return super.getOverrideProperties().append(RDF_language, LANG_RDF_XML_ABBREV);
		}
	}

	/** Produces N-Triple output */
	@Produces("text/n-triple")
	public static class NTriple extends RdfSerializer {

		/**
		 * Constructor.
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public NTriple(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override /* CoreObject */
		protected ObjectMap getOverrideProperties() {
			return super.getOverrideProperties().append(RDF_language, LANG_NTRIPLE);
		}
	}

	/** Produces Turtle output */
	@Produces("text/turtle")
	public static class Turtle extends RdfSerializer {

		/**
		 * Constructor.
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public Turtle(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override /* CoreObject */
		protected ObjectMap getOverrideProperties() {
			return super.getOverrideProperties().append(RDF_language, LANG_TURTLE);
		}
	}

	/** Produces N3 output */
	@Produces("text/n3")
	public static class N3 extends RdfSerializer {

		/**
		 * Constructor.
		 * @param propertyStore The property store containing all the settings for this object.
		 */
		public N3(PropertyStore propertyStore) {
			super(propertyStore);
		}

		@Override /* CoreObject */
		protected ObjectMap getOverrideProperties() {
			return super.getOverrideProperties().append(RDF_language, LANG_N3);
		}
	}


	private final RdfSerializerContext ctx;
	
	/**
	 * Constructor.
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	public RdfSerializer(PropertyStore propertyStore) {
		super(propertyStore);
		this.ctx = createContext(RdfSerializerContext.class);
	}

	@Override /* CoreObject */
	public RdfSerializerBuilder builder() {
		return new RdfSerializerBuilder(propertyStore);
	}

	@Override /* Serializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {

		RdfSerializerSession s = (RdfSerializerSession)session;

		Model model = s.getModel();
		Resource r = null;

		ClassMeta<?> cm = session.getClassMetaForObject(o);
		if (s.isLooseCollections() && cm != null && cm.isCollectionOrArray()) {
			Collection c = s.sort(cm.isCollection() ? (Collection)o : toList(cm.getInnerClass(), o));
			for (Object o2 : c)
				serializeAnything(s, o2, false, object(), "root", null, null);
		} else {
			RDFNode n = serializeAnything(s, o, false, s.getExpectedRootType(o), "root", null, null);
			if (n.isLiteral()) {
				r = model.createResource();
				r.addProperty(s.getValueProperty(), n);
			} else {
				r = n.asResource();
			}

			if (s.isAddRootProp())
				r.addProperty(s.getRootProp(), "true");
		}

		s.getRdfWriter().write(model, session.getWriter(), "http://unknown/");
	}

	private RDFNode serializeAnything(RdfSerializerSession session, Object o, boolean isURI, ClassMeta<?> eType, String attrName, BeanPropertyMeta bpm, Resource parentResource) throws SerializeException {
		Model m = session.getModel();

		ClassMeta<?> aType = null;       // The actual type
		ClassMeta<?> wType = null;       // The wrapped type
		ClassMeta<?> sType = object();   // The serialized type

		aType = session.push(attrName, o, eType);

		if (eType == null)
			eType = object();

		// Handle recursion
		if (aType == null) {
			o = null;
			aType = object();
		}

		if (o != null) {

			if (aType.isDelegate()) {
				wType = aType;
				aType = ((Delegate)o).getClassMeta();
			}

			sType = aType.getSerializedClassMeta();

			// Swap if necessary
			PojoSwap swap = aType.getPojoSwap();
			if (swap != null) {
				o = swap.swap(session, o);

				// If the getSwapClass() method returns Object, we need to figure out
				// the actual type now.
				if (sType.isObject())
					sType = session.getClassMetaForObject(o);
			}
		} else {
			sType = eType.getSerializedClassMeta();
		}

		String typeName = session.getBeanTypeName(eType, aType, bpm);

		RDFNode n = null;

		if (o == null || sType.isChar() && ((Character)o).charValue() == 0) {
			if (bpm != null) {
				if (! session.isTrimNulls()) {
					n = m.createResource(RDF_NIL);
				}
			} else {
				n = m.createResource(RDF_NIL);
			}

		} else if (sType.isUri() || isURI) {
			n = m.createResource(getUri(session, o, null));

		} else if (sType.isCharSequence() || sType.isChar()) {
			n = m.createLiteral(session.encodeTextInvalidChars(o));

		} else if (sType.isNumber() || sType.isBoolean()) {
			if (! session.isAddLiteralTypes())
				n = m.createLiteral(o.toString());
			else
				n = m.createTypedLiteral(o);

		} else if (sType.isMap() || (wType != null && wType.isMap())) {
			if (o instanceof BeanMap) {
				BeanMap bm = (BeanMap)o;
				Object uri = null;
				RdfBeanMeta rbm = (RdfBeanMeta)bm.getMeta().getExtendedMeta(RdfBeanMeta.class);
				if (rbm.hasBeanUri())
					uri = rbm.getBeanUriProperty().get(bm, null);
				String uri2 = getUri(session, uri, null);
				n = m.createResource(uri2);
				serializeBeanMap(session, bm, (Resource)n, typeName);
			} else {
				Map m2 = (Map)o;
				n = m.createResource();
				serializeMap(session, m2, (Resource)n, sType);
			}

		} else if (sType.isBean()) {
			BeanMap bm = session.toBeanMap(o);
			Object uri = null;
			RdfBeanMeta rbm = (RdfBeanMeta)bm.getMeta().getExtendedMeta(RdfBeanMeta.class);
			if (rbm.hasBeanUri())
				uri = rbm.getBeanUriProperty().get(bm, null);
			String uri2 = getUri(session, uri, null);
			n = m.createResource(uri2);
			serializeBeanMap(session, bm, (Resource)n, typeName);

		} else if (sType.isCollectionOrArray() || (wType != null && wType.isCollection())) {
			Collection c = session.sort(sType.isCollection() ? (Collection)o : toList(sType.getInnerClass(), o));
			RdfCollectionFormat f = session.getCollectionFormat();
			RdfClassMeta rcm = sType.getExtendedMeta(RdfClassMeta.class);
			if (rcm.getCollectionFormat() != RdfCollectionFormat.DEFAULT)
				f = rcm.getCollectionFormat();
			if (bpm != null && bpm.getExtendedMeta(RdfBeanPropertyMeta.class).getCollectionFormat() != RdfCollectionFormat.DEFAULT)
				f = bpm.getExtendedMeta(RdfBeanPropertyMeta.class).getCollectionFormat();
			switch (f) {
				case BAG: n = serializeToContainer(session, c, eType, m.createBag()); break;
				case LIST: n = serializeToList(session, c, eType); break;
				case MULTI_VALUED: serializeToMultiProperties(session, c, eType, bpm, attrName, parentResource); break;
				default: n = serializeToContainer(session, c, eType, m.createSeq());
			}
		} else {
			n = m.createLiteral(session.encodeTextInvalidChars(session.toString(o)));
		}

		session.pop();

		return n;
	}

	private static String getUri(RdfSerializerSession session, Object uri, Object uri2) {
		String s = null;
		if (uri != null)
			s = uri.toString();
		if ((s == null || s.isEmpty()) && uri2 != null)
			s = uri2.toString();
		if (s == null)
			return null;
		return session.getUriResolver().resolve(s);
	}

	private void serializeMap(RdfSerializerSession session, Map m, Resource r, ClassMeta<?> type) throws SerializeException {

		m = session.sort(m);

		ClassMeta<?> keyType = type.getKeyType(), valueType = type.getValueType();

		ArrayList<Map.Entry<Object,Object>> l = new ArrayList<Map.Entry<Object,Object>>(m.entrySet());
		Collections.reverse(l);
		for (Map.Entry<Object,Object> me : l) {
			Object value = me.getValue();

			Object key = session.generalize(me.getKey(), keyType);

			Namespace ns = session.getJuneauBpNs();
			Model model = session.getModel();
			Property p = model.createProperty(ns.getUri(), session.encodeElementName(session.toString(key)));
			RDFNode n = serializeAnything(session, value, false, valueType, key == null ? null : session.toString(key), null, r);
			if (n != null)
				r.addProperty(p, n);
		}
	}

	private void serializeBeanMap(RdfSerializerSession session, BeanMap<?> m, Resource r, String typeName) throws SerializeException {
		List<BeanPropertyValue> l = m.getValues(session.isTrimNulls(), typeName != null ? session.createBeanTypeNameProperty(m, typeName) : null);
		Collections.reverse(l);
		for (BeanPropertyValue bpv : l) {
			BeanPropertyMeta pMeta = bpv.getMeta();
			ClassMeta<?> cMeta = pMeta.getClassMeta();

			if (pMeta.getExtendedMeta(RdfBeanPropertyMeta.class).isBeanUri())
				continue;

			String key = bpv.getName();
			Object value = bpv.getValue();
			Throwable t = bpv.getThrown();
			if (t != null)
				session.addBeanGetterWarning(pMeta, t);

			if (session.canIgnoreValue(cMeta, key, value))
				continue;

			BeanPropertyMeta bpm = bpv.getMeta();
			Namespace ns = bpm.getExtendedMeta(RdfBeanPropertyMeta.class).getNamespace();
			if (ns == null && session.isUseXmlNamespaces())
				ns = bpm.getExtendedMeta(XmlBeanPropertyMeta.class).getNamespace();
			if (ns == null)
				ns = session.getJuneauBpNs();
			else if (session.isAutoDetectNamespaces())
				session.addModelPrefix(ns);

			Property p = session.getModel().createProperty(ns.getUri(), session.encodeElementName(key));
			RDFNode n = serializeAnything(session, value, pMeta.isUri(), cMeta, key, pMeta, r);
			if (n != null)
				r.addProperty(p, n);
		}
	}


	private Container serializeToContainer(RdfSerializerSession session, Collection c, ClassMeta<?> type, Container list) throws SerializeException {

		ClassMeta<?> elementType = type.getElementType();
		for (Object e : c) {
			RDFNode n = serializeAnything(session, e, false, elementType, null, null, null);
			list = list.add(n);
		}
		return list;
	}

	private RDFList serializeToList(RdfSerializerSession session, Collection c, ClassMeta<?> type) throws SerializeException {
		ClassMeta<?> elementType = type.getElementType();
		List<RDFNode> l = new ArrayList<RDFNode>(c.size());
		for (Object e : c) {
			l.add(serializeAnything(session, e, false, elementType, null, null, null));
		}
		return session.getModel().createList(l.iterator());
	}

	private void serializeToMultiProperties(RdfSerializerSession session, Collection c, ClassMeta<?> sType, BeanPropertyMeta bpm, String attrName, Resource parentResource) throws SerializeException {
		ClassMeta<?> elementType = sType.getElementType();
		for (Object e : c) {
			Namespace ns = null;
			if (bpm != null) {
				ns = bpm.getExtendedMeta(RdfBeanPropertyMeta.class).getNamespace();
				if (ns == null && session.isUseXmlNamespaces())
					ns = bpm.getExtendedMeta(XmlBeanPropertyMeta.class).getNamespace();
			}
			if (ns == null)
				ns = session.getJuneauBpNs();
			else if (session.isAutoDetectNamespaces())
				session.addModelPrefix(ns);
			RDFNode n2 = serializeAnything(session, e, false, elementType, null, null, null);
			Property p = session.getModel().createProperty(ns.getUri(), session.encodeElementName(attrName));
			parentResource.addProperty(p, n2);
		}

	}

	
	//--------------------------------------------------------------------------------
	// Entry point methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	public RdfSerializerSession createSession(Object output, ObjectMap op, Method javaMethod, Locale locale, TimeZone timeZone, MediaType mediaType, UriContext uriContext) {
		return new RdfSerializerSession(ctx, op, output, javaMethod, locale, timeZone, mediaType, uriContext);
	}
}
