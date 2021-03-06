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

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.dto.atom.AtomBuilder.*;
import static org.junit.Assert.*;

import java.net.*;

import org.apache.juneau.xml.*;
import org.junit.*;

@SuppressWarnings("javadoc")
public class AtomTest {

	public Feed createFeed() throws Exception {
		return
			feed("tag:foo.org", "Title", "2016-12-31T01:02:03-04:00")
			.subtitle(text("html").text("Subtitle"))
			.links(
				link("alternate", "text/html", "http://foo.org/").hreflang("en"),
				link("self", "application/atom+xml", "http://foo.org/feed.atom")
			)
			.rights("Copyright (c) 2016, Apache Foundation")
			.generator(
				generator("Example Toolkit").uri("http://www.foo.org/").version("1.0")
			)
			.entries(
				entry("tag:foo.org", "Title", "2016-12-31T01:02:03-04:00")
				.links(
					link("alternate", "text/html", "http://foo.org/2005/04/02/atom"),
					link("enclosure", "audio/mpeg", "http://foo.org/audio/foobar.mp3").length(1337)
				)
				.published("2016-12-31T01:02:03-04:00")
				.authors(
					person("John Smith").uri(new URI("http://foo.org/")).email("foo@foo.org")
				)
				.contributors(
					person("John Smith"),
					person("Jane Smith")
				)
				.content(
					content("xhtml")
					.lang("en")
					.base("http://foo.org/")
					.text("<div><p><i>[Sample content]</i></p></div>")
				)
			);
	}

	@Test
	public void testNormal() throws Exception {
		XmlSerializer s;
		XmlParser p = XmlParser.DEFAULT;
		String r;
		Feed f = createFeed(), f2;

		String expected =
			"<feed>\n"
			+"	<entry>\n"
			+"		<author>\n"
			+"			<email>foo@foo.org</email>\n"
			+"			<name>John Smith</name>\n"
			+"			<uri>http://foo.org/</uri>\n"
			+"		</author>\n"
			+"		<content base='http://foo.org/' lang='en' type='xhtml'><div><p><i>[Sample content]</i></p></div></content>\n"
			+"		<contributor>\n"
			+"			<name>John Smith</name>\n"
			+"		</contributor>\n"
			+"		<contributor>\n"
			+"			<name>Jane Smith</name>\n"
			+"		</contributor>\n"
			+"		<id>tag:foo.org</id>\n"
			+"		<link href='http://foo.org/2005/04/02/atom' rel='alternate' type='text/html'/>\n"
			+"		<link href='http://foo.org/audio/foobar.mp3' length='1337' rel='enclosure' type='audio/mpeg'/>\n"
			+"		<published>2016-12-31T01:02:03-04:00</published>\n"
			+"		<title>Title</title>\n"
			+"		<updated>2016-12-31T01:02:03-04:00</updated>\n"
			+"	</entry>\n"
			+"	<generator uri='http://www.foo.org/' version='1.0'>Example Toolkit</generator>\n"
			+"	<id>tag:foo.org</id>\n"
			+"	<link href='http://foo.org/' hreflang='en' rel='alternate' type='text/html'/>\n"
			+"	<link href='http://foo.org/feed.atom' rel='self' type='application/atom+xml'/>\n"
			+"	<rights>Copyright (c) 2016, Apache Foundation</rights>\n"
			+"	<subtitle type='html'>Subtitle</subtitle>\n"
			+"	<title>Title</title>\n"
			+"	<updated>2016-12-31T01:02:03-04:00</updated>\n"
			+"</feed>\n";

		s = new XmlSerializerBuilder().sq().ws().enableNamespaces(false).sortProperties(true).build();
		r = s.serialize(f);
		assertEquals(expected, r);
		f2 = p.parse(r, Feed.class);
		assertEqualObjects(f, f2);
	}

	@Test
	public void testWithNamespaces() throws Exception {
		XmlSerializer s;
		XmlParser p = XmlParser.DEFAULT;
		String r;
		Feed f = createFeed(), f2;

		String expected =
			"<atom:feed xmlns='http://www.apache.org/2013/Juneau' xmlns:atom='http://www.w3.org/2005/Atom/' xmlns:xml='http://www.w3.org/XML/1998/namespace'>\n"
			+"	<atom:entry>\n"
			+"		<atom:author>\n"
			+"			<atom:email>foo@foo.org</atom:email>\n"
			+"			<atom:name>John Smith</atom:name>\n"
			+"			<atom:uri>http://foo.org/</atom:uri>\n"
			+"		</atom:author>\n"
			+"		<atom:content xml:base='http://foo.org/' xml:lang='en' type='xhtml'><div><p><i>[Sample content]</i></p></div></atom:content>\n"
			+"		<atom:contributor>\n"
			+"			<atom:name>John Smith</atom:name>\n"
			+"		</atom:contributor>\n"
			+"		<atom:contributor>\n"
			+"			<atom:name>Jane Smith</atom:name>\n"
			+"		</atom:contributor>\n"
			+"		<atom:id>tag:foo.org</atom:id>\n"
			+"		<atom:link href='http://foo.org/2005/04/02/atom' rel='alternate' type='text/html'/>\n"
			+"		<atom:link href='http://foo.org/audio/foobar.mp3' length='1337' rel='enclosure' type='audio/mpeg'/>\n"
			+"		<atom:published>2016-12-31T01:02:03-04:00</atom:published>\n"
			+"		<atom:title>Title</atom:title>\n"
			+"		<atom:updated>2016-12-31T01:02:03-04:00</atom:updated>\n"
			+"	</atom:entry>\n"
			+"	<atom:generator uri='http://www.foo.org/' version='1.0'>Example Toolkit</atom:generator>\n"
			+"	<atom:id>tag:foo.org</atom:id>\n"
			+"	<atom:link href='http://foo.org/' hreflang='en' rel='alternate' type='text/html'/>\n"
			+"	<atom:link href='http://foo.org/feed.atom' rel='self' type='application/atom+xml'/>\n"
			+"	<atom:rights>Copyright (c) 2016, Apache Foundation</atom:rights>\n"
			+"	<atom:subtitle type='html'>Subtitle</atom:subtitle>\n"
			+"	<atom:title>Title</atom:title>\n"
			+"	<atom:updated>2016-12-31T01:02:03-04:00</atom:updated>\n"
			+"</atom:feed>\n";

		s = new XmlSerializerBuilder().sq().ws().enableNamespaces(true).addNamespaceUrisToRoot(true).sortProperties(true).build();
		r = s.serialize(f);
		assertEquals(expected, r);
		f2 = p.parse(r, Feed.class);
		assertEqualObjects(f, f2);
	}

	@Test
	public void testWithNamespacesWithAtomAsDefault() throws Exception {
		XmlSerializer s;
		XmlParser p = XmlParser.DEFAULT;
		String r;
		Feed f = createFeed(), f2;

		String expected =
			"<feed xmlns='http://www.w3.org/2005/Atom/' xmlns:xml='http://www.w3.org/XML/1998/namespace'>\n"
			+"	<entry>\n"
			+"		<author>\n"
			+"			<email>foo@foo.org</email>\n"
			+"			<name>John Smith</name>\n"
			+"			<uri>http://foo.org/</uri>\n"
			+"		</author>\n"
			+"		<content xml:base='http://foo.org/' xml:lang='en' type='xhtml'><div><p><i>[Sample content]</i></p></div></content>\n"
			+"		<contributor>\n"
			+"			<name>John Smith</name>\n"
			+"		</contributor>\n"
			+"		<contributor>\n"
			+"			<name>Jane Smith</name>\n"
			+"		</contributor>\n"
			+"		<id>tag:foo.org</id>\n"
			+"		<link href='http://foo.org/2005/04/02/atom' rel='alternate' type='text/html'/>\n"
			+"		<link href='http://foo.org/audio/foobar.mp3' length='1337' rel='enclosure' type='audio/mpeg'/>\n"
			+"		<published>2016-12-31T01:02:03-04:00</published>\n"
			+"		<title>Title</title>\n"
			+"		<updated>2016-12-31T01:02:03-04:00</updated>\n"
			+"	</entry>\n"
			+"	<generator uri='http://www.foo.org/' version='1.0'>Example Toolkit</generator>\n"
			+"	<id>tag:foo.org</id>\n"
			+"	<link href='http://foo.org/' hreflang='en' rel='alternate' type='text/html'/>\n"
			+"	<link href='http://foo.org/feed.atom' rel='self' type='application/atom+xml'/>\n"
			+"	<rights>Copyright (c) 2016, Apache Foundation</rights>\n"
			+"	<subtitle type='html'>Subtitle</subtitle>\n"
			+"	<title>Title</title>\n"
			+"	<updated>2016-12-31T01:02:03-04:00</updated>\n"
			+"</feed>\n";

		s = new XmlSerializerBuilder().sq().ws().defaultNamespace("atom").enableNamespaces(true).addNamespaceUrisToRoot(true).sortProperties(true).build();
		r = s.serialize(f);
		assertEquals(expected, r);
		f2 = p.parse(r, Feed.class);
		assertEqualObjects(f, f2);
	}
}
