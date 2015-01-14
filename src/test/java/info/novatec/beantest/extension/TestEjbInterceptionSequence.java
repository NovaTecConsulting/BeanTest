/*
 *
 *  * Bean Testing.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package info.novatec.beantest.extension;

import info.novatec.beantest.api.BaseBeanTest;
import info.novatec.beantest.extension.resources.EjbInterceptionSequence;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

/**
 * This tests asserts that dynamically configured InterceptorBindings are invoked in the correct sequence.
 * @see info.novatec.beantest.extension.EjbInterceptorWrapperImpl
 * @author Qaiser Abbasi (qaiser.abbasi@novatec-gmbh.de)
 */
public class TestEjbInterceptionSequence  extends BaseBeanTest{

    public static final List<String> INTERCEPTOR_SEQUENCE = new ArrayList<String>();

    @Before
    public void setUp() throws Exception {
        INTERCEPTOR_SEQUENCE.clear();
    }

    @Test
    public void assertThatInterceptionOccursInCorrectSequence() throws Exception {
        EjbInterceptionSequence bean = getBean(EjbInterceptionSequence.class);
        bean.business();
        assertThat(INTERCEPTOR_SEQUENCE, contains("InterceptorSequence1", "InterceptorSequence2"));
    }
}
