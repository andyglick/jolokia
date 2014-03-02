package org.jolokia.service.jmx.handler;

/*
 * Copyright 2009-2013 Roland Huss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import javax.management.*;

import org.jolokia.service.serializer.Converters;
import org.jolokia.server.core.request.JolokiaRequestBuilder;
import org.jolokia.server.core.request.JolokiaWriteRequest;
import org.jolokia.server.core.service.serializer.JmxSerializer;
import org.jolokia.server.core.util.TestJolokiaContext;
import org.testng.annotations.*;

import static org.jolokia.server.core.util.RequestType.WRITE;
import static org.testng.Assert.*;
import static org.testng.AssertJUnit.assertEquals;

/**
 * @author roland
 * @since 21.04.11
 */
public class WriteHandlerTest {

    private WriteHandler handler;

    private ObjectName oName;

    private TestJolokiaContext ctx;

    @BeforeClass
    public void setup() throws MalformedObjectNameException, MBeanException, InstanceAlreadyExistsException, IOException, NotCompliantMBeanException, ReflectionException {
        oName = new ObjectName("jolokia:test=write");
        MBeanServer server = getMBeanServer();
        server.createMBean(WriteData.class.getName(), oName);
    }

    @BeforeMethod
    public void createHandler() {
        ctx = new TestJolokiaContext.Builder().services(JmxSerializer.class,new Converters()).build();
        handler = new WriteHandler(ctx);
    }

    @AfterTest
    public void unregisterMBean() throws InstanceNotFoundException, MBeanRegistrationException, IOException {
        MBeanServerConnection conn = getMBeanServer();
        conn.unregisterMBean(oName);
    }

    private MBeanServer getMBeanServer() {
        return ManagementFactory.getPlatformMBeanServer();
    }


    @Test
    public void simple() throws MalformedObjectNameException, InstanceNotFoundException, IOException, ReflectionException, AttributeNotFoundException, MBeanException {
        JolokiaWriteRequest req = new JolokiaRequestBuilder(WRITE,oName).attribute("Simple").value("10").build();
        handler.doHandleRequest(getMBeanServer(),req);
        req = new JolokiaRequestBuilder(WRITE,oName).attribute("Simple").value("20").build();
        Integer ret = (Integer) handler.doHandleRequest(getMBeanServer(),req);
        assertEquals(ret,new Integer(10));
        assertEquals(handler.getType(),WRITE);
    }

    @Test
    public void map() throws Exception {
        Map map = new HashMap<String,Integer>();
        map.put("answer",42);
        JolokiaWriteRequest req = new JolokiaRequestBuilder(WRITE,oName).attribute("Map").value(map).build();
        handler.doHandleRequest(getMBeanServer(),req);
        req = new JolokiaRequestBuilder(WRITE,oName).attribute("Map").value(null).build();
        Map ret = (Map) handler.doHandleRequest(getMBeanServer(),req);
        assertTrue(ret instanceof Map);
        assertEquals(((Map) ret).get("answer"),42);

    }

    @Test(expectedExceptions = {AttributeNotFoundException.class})
    public void invalidAttribute() throws MalformedObjectNameException, InstanceNotFoundException, IOException, ReflectionException, AttributeNotFoundException, MBeanException {
        JolokiaWriteRequest req = new JolokiaRequestBuilder(WRITE,oName).attribute("ReadOnly").value("Sommer").build();
        handler.doHandleRequest(getMBeanServer(),req);
    }

    @Test
    public void invalidValue() throws Exception {
        JolokiaWriteRequest req = new JolokiaRequestBuilder(WRITE,oName).attribute("Boolean").value(10).build();
        handler.doHandleRequest(getMBeanServer(),req);
    }
}