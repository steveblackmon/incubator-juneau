/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp. 
 *******************************************************************************/
package com.ibm.juno.core.test;

import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.json.*;

public class CT_BeanFilter {
	
	//====================================================================================================
	// Test sub types
	//====================================================================================================
	@Test
	public void testSubTypes() throws Exception {
		JsonSerializer s = JsonSerializer.DEFAULT_LAX;
		JsonParser p = JsonParser.DEFAULT;

		A1 a1 = new A1();
		a1.f1 = "f1";
		a1.fb = new B2();
		((B2)a1.fb).f2 = "f2";
		String r = s.serialize(a1);  
		assertEquals("{subType:'A1',f0:'f0',fb:{subType:'B2',f0b:'f0b',f2:'f2'},f1:'f1'}", r);
		
		A a = p.parse(r, A.class);
		assertTrue(a instanceof A1);
		assertTrue(a.fb instanceof B2);
		assertEquals("f1", ((A1)a).f1);
		assertEquals("f2", ((B2)a.fb).f2);
		
		// Try out-of-order creation.
		r = "{f0:'f0',f1:'f1',subType:'A1',fb:{f0b:'f0b',f2:'f2',subType:'B2'}}";
		a = p.parse(r, A.class);
		assertTrue(a instanceof A1);
		assertTrue(a.fb instanceof B2);
		assertEquals("f1", ((A1)a).f1);
		assertEquals("f2", ((B2)a.fb).f2);
	}
	
	@Bean(
		subTypeProperty="subType",
		subTypes={
			@BeanSubType(type=A1.class, id="A1"),
			@BeanSubType(type=A2.class, id="A2")
		}
	)
	public static abstract class A {
		public String f0 = "f0";
		public B fb;
	}
	
	public static class A1 extends A {
		public String f1;
	}

	public static class A2 extends A {
		public String f2;
	}
	
	@Bean(
		subTypeProperty="subType",
		subTypes={
			@BeanSubType(type=B1.class, id="B1"),
			@BeanSubType(type=B2.class, id="B2")
		}
	)
	public static abstract class B {
		public String f0b = "f0b";
	}
	
	public static class B1 extends B {
		public String f1;
	}

	public static class B2 extends B {
		public String f2;
	}
	
	//====================================================================================================
	// Test parent class used as filter
	//====================================================================================================
	@Test
	public void testParentClassFilter() throws Exception {
		JsonSerializer s = new JsonSerializer.Simple().addFilters(C1.class);

		C1 c1 = new C2();
		String r = s.serialize(c1);
		assertEquals("{f0:'f0'}", r);

		List<C1> l = new LinkedList<C1>();
		l.add(new C2());
		r = s.serialize(l);
		assertEquals("[{f0:'f0'}]", r);
	}

	public static class C1 {
		public String f0 = "f0";
	}

	public static class C2 extends C1 {
		public String f1 = "f1";
	}

	//====================================================================================================
	// Test non-static parent class used as filter
	//====================================================================================================
	@Test
	public void testParentClassFilter2() throws Exception {
		JsonSerializer s = new JsonSerializer.Simple().addFilters(D1.class);

		D1 d1 = new D2();
		String r = s.serialize(d1);
		assertEquals("{f0:'f0'}", r);

		List<D1> l = new LinkedList<D1>();
		l.add(new D2());
		r = s.serialize(l);
		assertEquals("[{f0:'f0'}]", r);
	}

	public class D1 {
		public String f0 = "f0";
	}

	public class D2 extends D1 {
		public String f1 = "f1";
	}

}
