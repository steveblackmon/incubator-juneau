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
package org.apache.juneau.csv;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.serializer.*;

/**
 * TODO - Work in progress.  CSV serializer.
 */
@Produces("text/csv")
@SuppressWarnings({"rawtypes"})
public final class CsvSerializer extends WriterSerializer {

	/** Default serializer, all default settings.*/
	public static final CsvSerializer DEFAULT = new CsvSerializer(PropertyStore.create());


	/**
	 * Constructor.
	 * @param propertyStore The property store containing all the settings for this object.
	 */
	public CsvSerializer(PropertyStore propertyStore) {
		super(propertyStore);
	}

	@Override /* CoreObject */
	public CsvSerializerBuilder builder() {
		return new CsvSerializerBuilder(propertyStore);
	}

	//--------------------------------------------------------------------------------
	// Entry point methods
	//--------------------------------------------------------------------------------

	@Override /* Serializer */
	protected void doSerialize(SerializerSession session, Object o) throws Exception {
		Writer out = session.getWriter();
		ClassMeta cm = session.getClassMetaForObject(o);
		Collection l = null;
		if (cm.isArray()) {
			l = Arrays.asList((Object[])o);
		} else {
			l = (Collection)o;
		}
		// TODO - Doesn't support DynaBeans.
		if (l.size() > 0) {
			ClassMeta entryType = session.getClassMetaForObject(l.iterator().next());
			if (entryType.isBean()) {
				BeanMeta<?> bm = entryType.getBeanMeta();
				int i = 0;
				for (BeanPropertyMeta pm : bm.getPropertyMetas()) {
					if (i++ > 0)
						out.append(',');
					append(out, pm.getName());
				}
				out.append('\n');
				for (Object o2 : l) {
					i = 0;
					BeanMap bean = session.toBeanMap(o2);
					for (BeanPropertyMeta pm : bm.getPropertyMetas()) {
						if (i++ > 0)
							out.append(',');
						append(out, pm.get(bean, pm.getName()));
					}
					out.append('\n');
				}
			}
		}
	}

	private static void append(Writer w, Object o) throws IOException {
		if (o == null)
			w.append("null");
		else {
			String s = o.toString();
			boolean mustQuote = false;
			for (int i = 0; i < s.length() && ! mustQuote; i++) {
				char c = s.charAt(i);
				if (Character.isWhitespace(c) || c == ',')
					mustQuote = true;
			}
			if (mustQuote)
				w.append('"').append(s).append('"');
			else
				w.append(s);
		}
	}
}
