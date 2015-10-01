/*
 * Bean Testing.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package info.novatec.beantest.demo.ejb;

import info.novatec.beantest.demo.entities.MyEntity;
import info.novatec.beantest.demo.exceptions.MyException;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Rewrote adjacent tests from TestEJBInjection with DeltaSpike´s "test control" module in order to compare the
 * configurations needs, code style and importantly to check if the module provides an already solution for deploying
 * the concepts and ideas of CDI BeanTest.
 */
@RunWith(CdiTestRunner.class)
public class DeltaSpikeEjbJpaTest {

    @Inject
    private MyEJBService myService;
    @Inject
    private MyEJBServiceWithEntityManagerSetter myEJBServiceWithEntityManagerSetter;

    @Test
    public void shouldInjectEJBAsCDIBean() {
        myService.callOtherServiceAndPersistAnEntity();
        assertThat(myService.getOtherService2().getAllEntities(), hasSize(1));
    }

    @Test
    public void shouldPersistEntityInSpiteOfException() {
        MyEntity myEntity = new MyEntity();
        myEntity.setName("Foo");
        //An exception is thrown within the following method call, but because it is caught, the entity should have benn saved.
        myService.saveEntityAndHandleException(myEntity);

        assertThat(myService.getOtherService2().getAllEntities(), hasSize(1));

    }

    /**
     * Verifies that the transaction is rolled back properly when an Exception is thrown and not handled.
     */
    @Test
    public void shouldNotPersistEntityBecauseOfException() {
        MyEntity myEntity = new MyEntity();
        myEntity.setName("Foo");
        try {
            myService.attemptToSaveEntityAndThrowException(myEntity);
            fail("Should have thrown an exception");
        } catch (MyException e) {
            assertThat(myService.getOtherService2().getAllEntities(), is(empty()));
        }
    }

    @Test
    public void shouldInjectEJBAsCDIBeanUsingSetter() {
        assertNotNull(myService.getOtherService2());
    }

    @Test
    public void shouldInjectPersistenceContextUsingSetter() {
        assertNotNull(myEJBServiceWithEntityManagerSetter.getEm());
    }
}
