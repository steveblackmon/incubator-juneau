/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2011, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.core.xml;

import static com.ibm.juno.core.xml.annotation.XmlFormat.*;

import java.io.*;
import java.lang.reflect.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;

import javax.xml.*;
import javax.xml.transform.stream.*;
import javax.xml.validation.*;

import org.w3c.dom.bootstrap.*;
import org.w3c.dom.ls.*;
import org.xml.sax.*;

import com.ibm.juno.core.*;
import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.serializer.*;
import com.ibm.juno.core.utils.*;
import com.ibm.juno.core.xml.annotation.*;

/**
 * Serializes POJO metadata to HTTP responses as XML.
 *
 *
 * <h6 class='topic'>Media types</h6>
 * <p>
 * 	Handles <code>Accept</code> types: <code>text/xml+schema</code>
 * <p>
 * 	Produces <code>Content-Type</code> types: <code>text/xml</code>
 *
 *
 * <h6 class='topic'>Description</h6>
 * <p>
 * 	Produces the XML-schema representation of the XML produced by the {@link XmlSerializer} class with the same properties.
 *
 *
 * <h6 class='topic'>Configurable properties</h6>
 * <p>
 * 	This class has the following properties associated with it:
 * <ul>
 * 	<li>{@link XmlSerializerProperties}
 * 	<li>{@link SerializerProperties}
 * 	<li>{@link BeanContextProperties}
 * </ul>
 *
 * @author James Bognar (jbognar@us.ibm.com)
 */
@Produces(value="text/xml+schema",contentType="text/xml")
public class XmlSchemaSerializer extends XmlSerializer {

	@Override /* XmlSerializer */
	protected void doSerialize(Object o, Writer out, SerializerContext ctx) throws IOException, SerializeException {
		XmlSerializerContext xctx = (XmlSerializerContext)ctx;

		if (xctx.isEnableNamespaces() && xctx.isAutoDetectNamespaces())
			findNsfMappings(o, xctx);

		Namespace xs = xctx.getXsNamespace();
		Namespace[] allNs = ArrayUtils.append(new Namespace[]{xctx.getDefaultNamespace()}, xctx.getNamespaces());

		Schemas s = new Schemas(xs, xctx.getDefaultNamespace(), allNs, xctx);
		s.process(o, xctx);
		s.serializeTo(out);
	}

	/**
	 * Returns an XML-Schema validator based on the output returned by {@link #doSerialize(Object, Writer, SerializerContext)};
	 *
	 * @param o The object to serialize.
	 * @param ctx The serializer context object return by {@link #createContext(ObjectMap, Method)}.<br>
	 * 	Can be <jk>null</jk>.
	 * @return The new validator.
	 * @throws SAXException If a problem was detected in the XML-Schema output produced by this serializer.
	 * @throws SerializeException If a problem occurred trying to convert the output.
	 */
	public Validator getValidator(Object o, SerializerContext ctx) throws SerializeException, SAXException {
		String xmlSchema = serialize(o, ctx);

		// create a SchemaFactory capable of understanding WXS schemas
		SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

		if (xmlSchema.indexOf('\u0000') != -1) {

			// Break it up into a map of namespaceURI->schema document
			final Map<String,String> schemas = new HashMap<String,String>();
			String[] ss = xmlSchema.split("\u0000");
			xmlSchema = ss[0];
			for (String s : ss) {
				Matcher m = pTargetNs.matcher(s);
				if (m.find())
					schemas.put(m.group(1), s);
			}

			// Create a custom resolver
			factory.setResourceResolver(
				new LSResourceResolver() {

					@Override /* LSResourceResolver */
					public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {

						String schema = schemas.get(namespaceURI);
						if (schema == null)
							throw new RuntimeException(MessageFormat.format("No schema found for namespaceURI ''{0}''", namespaceURI));

						try {
							DOMImplementationRegistry registry = DOMImplementationRegistry.newInstance();
							DOMImplementationLS domImplementationLS = (DOMImplementationLS)registry.getDOMImplementation("LS 3.0");
							LSInput in = domImplementationLS.createLSInput();
							in.setCharacterStream(new StringReader(schema));
							in.setSystemId(systemId);
							return in;

						} catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
				}
			);
		}
		return factory.newSchema(new StreamSource(new StringReader(xmlSchema))).newValidator();
	}

	private static Pattern pTargetNs = Pattern.compile("targetNamespace=['\"]([^'\"]+)['\"]");


	/* An instance of a global element, global attribute, or XML type to be serialized. */
	private static class QueueEntry {
		Namespace ns;
		String name;
		ClassMeta<?> cm;
		QueueEntry(Namespace ns, String name, ClassMeta<?> cm) {
			this.ns = ns;
			this.name = name;
			this.cm = cm;
		}
	}

	/* An encapsulation of all schemas present in the metamodel of the serialized object. */
	private class Schemas extends LinkedHashMap<Namespace,Schema> {

		private static final long serialVersionUID = 1L;

		private Namespace defaultNs;
		private LinkedList<QueueEntry>
			elementQueue = new LinkedList<QueueEntry>(),
			attributeQueue = new LinkedList<QueueEntry>(),
			typeQueue = new LinkedList<QueueEntry>();

		private Schemas(Namespace xs, Namespace defaultNs, Namespace[] allNs, XmlSerializerContext ctx) throws IOException {
			this.defaultNs = defaultNs;
			for (Namespace ns : allNs)
				put(ns, new Schema(this, xs, ns, defaultNs, allNs, ctx));
		}

		private Schema getSchema(Namespace ns) {
			if (ns == null)
				ns = defaultNs;
			Schema s = get(ns);
			if (s == null)
				throw new RuntimeException("No schema defined for namespace '"+ns+"'");
			return s;
		}

		private void process(Object o, SerializerContext ctx) throws IOException {
			ClassMeta<?> cm = ctx.getBeanContext().getClassMetaForObject(o);
			Namespace ns = defaultNs;
			if (cm == null)
				queueElement(ns, "null", object());
			else {
				XmlClassMeta xmlMeta = cm.getXmlMeta();
				if (xmlMeta.getElementName() != null && xmlMeta.getNamespace() != null)
					ns = xmlMeta.getNamespace();
				queueElement(ns, xmlMeta.getElementName(), cm);
			}
			processQueue();
		}


		private void processQueue() throws IOException {
			boolean b;
			do {
				b = false;
				while (! elementQueue.isEmpty()) {
					QueueEntry q = elementQueue.removeFirst();
					b |= getSchema(q.ns).processElement(q.name, q.cm);
				}
				while (! typeQueue.isEmpty()) {
					QueueEntry q = typeQueue.removeFirst();
					b |= getSchema(q.ns).processType(q.name, q.cm);
				}
				while (! attributeQueue.isEmpty()) {
					QueueEntry q = attributeQueue.removeFirst();
					b |= getSchema(q.ns).processAttribute(q.name, q.cm);
				}
			} while (b);
		}

		private void queueElement(Namespace ns, String name, ClassMeta<?> cm) {
			elementQueue.add(new QueueEntry(ns, name, cm));
		}

		private void queueType(Namespace ns, String name, ClassMeta<?> cm) {
			if (name == null)
				name = XmlUtils.encodeElementName(cm.toString());
			typeQueue.add(new QueueEntry(ns, name, cm));
		}

		private void queueAttribute(Namespace ns, String name, ClassMeta<?> cm) {
			attributeQueue.add(new QueueEntry(ns, name, cm));
		}

		private void serializeTo(Writer w) throws IOException {
			boolean b = false;
			for (Schema s : values()) {
				if (b)
					w.append('\u0000');
				w.append(s.toString());
				b = true;
			}
		}
	}

	/* An encapsulation of a single schema. */
	private class Schema {
		private StringWriter sw = new StringWriter();
		private XmlSerializerWriter w;
		private XmlSerializerContext ctx;
		private Namespace defaultNs, targetNs;
		private Schemas schemas;
		private Set<String>
			processedTypes = new HashSet<String>(),
			processedAttributes = new HashSet<String>(),
			processedElements = new HashSet<String>();

		public Schema(Schemas schemas, Namespace xs, Namespace targetNs, Namespace defaultNs, Namespace[] allNs, XmlSerializerContext ctx) throws IOException {
			this.schemas = schemas;
			this.defaultNs = defaultNs;
			this.targetNs = targetNs;
			this.ctx = ctx;
			w = new XmlSerializerWriter(sw, ctx.isUseIndentation(), ctx.getQuoteChar(), null, null, true, null);
			int i = ctx.getIndent();
			w.oTag(i, "schema");
			w.attr("xmlns", xs.getUri());
			w.attr("targetNamespace", targetNs.getUri());
			w.attr("elementFormDefault", "qualified");
			if (targetNs != defaultNs)
				w.attr("attributeFormDefault", "qualified");
			for (Namespace ns2 : allNs)
				w.attr("xmlns", ns2.name, ns2.uri);
			w.append('>').nl();
			for (Namespace ns : allNs) {
				if (ns != targetNs) {
					w.oTag(i+1, "import")
						.attr("namespace", ns.getUri())
						.attr("schemaLocation", ns.getName()+".xsd")
						.append("/>").nl();
				}
			}
		}

		private boolean processElement(String name, ClassMeta<?> cm) throws IOException {
			if (processedElements.contains(name))
				return false;
			processedElements.add(name);

			ClassMeta<?> ft = cm.getFilteredClassMeta();
			int i = ctx.getIndent() + 1;
			if (name == null)
				name = getElementName(ft);
			Namespace ns = first(ft.getXmlMeta().getNamespace(), defaultNs);
			String type = getXmlType(ns, ft);

			w.oTag(i, "element")
				.attr("name", XmlUtils.encodeElementName(name))
				.attr("type", type)
				.append('/').append('>').nl();

			schemas.queueType(ns, null, ft);
			schemas.processQueue();
			return true;
		}

		private boolean processAttribute(String name, ClassMeta<?> cm) throws IOException {
			if (processedAttributes.contains(name))
				return false;
			processedAttributes.add(name);

			int i = ctx.getIndent() + 1;
			String type = getXmlAttrType(cm);

			w.oTag(i, "attribute")
				.attr("name", name)
				.attr("type", type)
				.append('/').append('>').nl();

			return true;
		}

		private boolean processType(String name, ClassMeta<?> cm) throws IOException {
			if (processedTypes.contains(name))
				return false;
			processedTypes.add(name);

			int i = ctx.getIndent() + 1;

			cm = cm.getFilteredClassMeta();

			w.oTag(i, "complexType")
				.attr("name", name);

			// This element can have mixed content if:
			// 	1) It's a generic Object (so it can theoretically be anything)
			//		2) The bean has a property defined with @XmlFormat.CONTENT.
			if ((cm.isBean() && cm.getBeanMeta().getXmlMeta().getXmlContentProperty() != null) || cm.isObject())
				w.attr("mixed", "true");

			w.cTag().nl();

			if (! (cm.isMap() || cm.isBean() || cm.hasToObjectMapMethod() || cm.isCollection() || cm.isArray() || (cm.isAbstract() && ! cm.isNumber()) || cm.isObject())) {
				String base = getXmlAttrType(cm);
				w.sTag(i+1, "simpleContent").nl();
				w.oTag(i+2, "extension")
					.attr("base", base);
				if (ctx.isAddJsonTypeAttrs() || (ctx.isAddJsonStringTypeAttrs() && base.equals("string"))) {
					w.cTag().nl();
					w.oTag(i+3, "attribute")
						.attr("name", "type")
						.attr("type", "string")
						.ceTag().nl();
					w.eTag(i+2, "extension").nl();
				} else {
					w.ceTag().nl();
				}
				w.eTag(i+1, "simpleContent").nl();

			} else {

				//----- Bean -----
				if (cm.isBean()) {
					BeanMeta<?> bm = cm.getBeanMeta();

					boolean hasChildElements = false;

					for (BeanPropertyMeta<?> pMeta : bm.getPropertyMetas())
						if (pMeta.getXmlMeta().getXmlFormat() != XmlFormat.ATTR && pMeta.getXmlMeta().getXmlFormat() != XmlFormat.CONTENT)
							hasChildElements = true;

					if (bm.getXmlMeta().getXmlContentProperty() != null) {
						w.sTag(i+1, "sequence").nl();
						w.oTag(i+2, "any")
							.attr("processContents", "skip")
							.attr("minOccurs", 0)
							.ceTag().nl();
						w.eTag(i+1, "sequence").nl();

					} else if (hasChildElements) {
						w.sTag(i+1, "sequence").nl();

						boolean hasOtherNsElement = false;

						for (BeanPropertyMeta<?> pMeta : bm.getPropertyMetas()) {
							XmlBeanPropertyMeta<?> xmlMeta = pMeta.getXmlMeta();
							if (xmlMeta.getXmlFormat() != XmlFormat.ATTR) {
								boolean isCollapsed = xmlMeta.getXmlFormat() == COLLAPSED;
								ClassMeta<?> ct2 = pMeta.getClassMeta();
								String childName = pMeta.getName();
								if (isCollapsed) {
									if (xmlMeta.getChildName() != null)
										childName = xmlMeta.getChildName();
									ct2 = pMeta.getClassMeta().getElementType();
								}
								Namespace cNs = first(xmlMeta.getNamespace(), ct2.getXmlMeta().getNamespace(), cm.getXmlMeta().getNamespace(), defaultNs);
								if (xmlMeta.getNamespace() == null) {
									w.oTag(i+2, "element")
										.attr("name", XmlUtils.encodeElementName(childName), true)
										.attr("type", getXmlType(cNs, ct2));
									if (isCollapsed) {
										w.attr("minOccurs", 0);
										w.attr("maxOccurs", "unbounded");
									} else {
										if (! ctx.isTrimNulls())
											w.attr("nillable", true);
										else
											w.attr("minOccurs", 0);
									}

									w.ceTag().nl();
								} else {
									// Child element is in another namespace.
									schemas.queueElement(cNs, pMeta.getName(), ct2);
									hasOtherNsElement = true;
								}

							}
						}

						// If this bean has any child elements in another namespace,
						// we need to add an <any> element.
						if (hasOtherNsElement)
							w.oTag(i+2, "any")
								.attr("minOccurs", 0)
								.attr("maxOccurs", "unbounded")
								.ceTag().nl();
						w.eTag(i+1, "sequence").nl();
					}

					for (BeanPropertyMeta<?> pMeta : bm.getXmlMeta().getXmlAttrProperties().values()) {
						Namespace pNs = pMeta.getXmlMeta().getNamespace();
						if (pNs == null)
							pNs = defaultNs;

						// If the bean attribute has a different namespace than the bean, then it needs to
						// be added as a top-level entry in the appropriate schema file.
						if (pNs != targetNs) {
							schemas.queueAttribute(pNs, pMeta.getName(), pMeta.getClassMeta());
							w.oTag(i+1, "attribute")
							//.attr("name", pMeta.getName(), true)
							.attr("ref", pNs.getName() + ':' + pMeta.getName())
							.ceTag().nl();
						}

						// Otherwise, it's just a plain attribute of this bean.
						else {
							w.oTag(i+1, "attribute")
								.attr("name", pMeta.getName(), true)
								.attr("type", getXmlAttrType(pMeta.getClassMeta()))
								.ceTag().nl();
						}
					}

				//----- Collection -----
				} else if (cm.isCollection() || cm.isArray()) {
					ClassMeta<?> elementType = cm.getElementType();
					if (elementType.isObject()) {
						w.sTag(i+1, "sequence").nl();
						w.oTag(i+2, "any")
							.attr("processContents", "skip")
							.attr("maxOccurs", "unbounded")
							.attr("minOccurs", "0")
							.ceTag().nl();
						w.eTag(i+1, "sequence").nl();
					} else {
						Namespace cNs = first(elementType.getXmlMeta().getNamespace(), cm.getXmlMeta().getNamespace(), defaultNs);
						schemas.queueType(cNs, null, elementType);
						w.sTag(i+1, "sequence").nl();
						w.oTag(i+2, "choice")
							.attr("minOccurs", 0)
							.attr("maxOccurs", "unbounded")
							.cTag().nl();
						w.oTag(i+3, "element")
							.attr("name", XmlUtils.encodeElementName(getElementName(elementType)))
							.attr("type", getXmlType(cNs, elementType))
							.ceTag().nl();
						w.oTag(i+3, "element")
							.attr("name", "null")
							.attr("type", "string")
							.ceTag().nl();
						w.eTag(i+2, "choice").nl();
						w.eTag(i+1, "sequence").nl();
					}

				//----- Map -----
				} else if (cm.isMap() || cm.hasToObjectMapMethod() || cm.isAbstract() || cm.isObject()) {
					w.sTag(i+1, "sequence").nl();
					w.oTag(i+2, "any")
						.attr("processContents", "skip")
						.attr("maxOccurs", "unbounded")
						.attr("minOccurs", "0")
						.ceTag().nl();
					w.eTag(i+1, "sequence").nl();
				}

				if (ctx.isAddClassAttrs()) {
					w.oTag(i+1, "attribute")
						.attr("name", "_class")
						.attr("type", "string")
						.ceTag().nl();
				}
				if (ctx.isAddJsonTypeAttrs()) {
					w.oTag(i+1, "attribute")
						.attr("name", "type")
						.attr("type", "string")
						.ceTag().nl();
				}
			}

			w.eTag(i, "complexType").nl();
			schemas.processQueue();

			return true;
		}

		private String getElementName(ClassMeta<?> cm) {
			cm = cm.getFilteredClassMeta();
			String name = cm.getXmlMeta().getElementName();

			if (name == null) {
				if (cm.isBoolean())
					name = "boolean";
				else if (cm.isNumber())
					name = "number";
				else if (cm.isArray() || cm.isCollection())
					name = "array";
				else if (! (cm.isMap() || cm.hasToObjectMapMethod() || cm.isBean() || cm.isCollection() || cm.isArray() || cm.isObject() || cm.isAbstract()))
					name = "string";
				else
					name = "object";
			}
			return name;
		}

		@Override /* Object */
		public String toString() {
			try {
				w.eTag(ctx.getIndent(), "schema").nl();
			} catch (IOException e) {
				throw new RuntimeException(e); // Shouldn't happen.
			}
			return sw.toString();
		}

		private String getXmlType(Namespace currentNs, ClassMeta<?> cm) {
			String name = null;
			cm = cm.getFilteredClassMeta();
			if (currentNs == targetNs && ! ctx.isAddJsonTypeAttrs()) {
				if (cm.isBoolean())
					name = "boolean";
				else if (cm.isNumber()) {
					if (cm.isDecimal())
						name = "decimal";
					else
						name = "integer";
				}
				if (name == null && ! ctx.isAddJsonStringTypeAttrs()) {
					if (! (cm.isMap() || cm.hasToObjectMapMethod() || cm.isBean() || cm.isCollection() || cm.isArray() || cm.isObject() || cm.isAbstract()))
						name = "string";
				}
			}
			if (name == null) {
				name = XmlUtils.encodeElementName(cm.toString());
				schemas.queueType(currentNs, name, cm);
				return currentNs.getName() + ":" + name;
			}

			return name;
		}
	}

	private <T> T first(T...tt) {
		for (T t : tt)
			if (t != null)
				return t;
		return null;
	}


	private static String getXmlAttrType(ClassMeta<?> cm) {
		if (cm.isBoolean())
			return "boolean";
		if (cm.isNumber()) {
			if (cm.isDecimal())
				return "decimal";
			return "integer";
		}
		return "string";
	}

	@Override /* Serializer */
	public XmlSerializerContext createContext(ObjectMap properties, Method javaMethod) {
		// This serializer must always have namespaces enabled.
		if (properties == null)
			properties = new ObjectMap();
		properties.put(XmlSerializerProperties.XML_enableNamespaces, true);
		return new XmlSerializerContext(getBeanContext(), sp, xsp, properties, javaMethod);
	}
}
