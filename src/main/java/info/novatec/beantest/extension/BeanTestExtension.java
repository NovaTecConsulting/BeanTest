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
package info.novatec.beantest.extension;

import info.novatec.beantest.transactions.Transactional;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.Singleton;
import javax.ejb.Stateless;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.inject.Inject;
import javax.interceptor.Interceptor;
import org.apache.deltaspike.core.util.metadata.builder.AnnotatedTypeBuilder;

/**
 * Extension to modify bean meta data.
 * <p>
 * This extension adds and changes the bean meta data in order to convert EJB injection points into CDI injection points.
 * Therefore the extension changes the meta data of Beans annotated with {@link EJB}<br>
 * It also changes injection points in interceptors.
 *
 * @author Carlos Barragan (carlos.barragan@novatec-gmbh.de)
 */
public class BeanTestExtension implements Extension {
     
     

    /**
     * Replaces the meta data of the {@link ProcessAnnotatedType}.
     * 
     * <p>
     * The ProcessAnnotatedType's meta data will be replaced, if the annotated type has one of the following annotations:
     * <ul>
     * <li> {@link Stateless}
     * <li> {@link MessageDriven}
     * <li> {@link Interceptor}
     * <li> {@link Singleton}
     * </ul>
     *
     * @param <X> the type of the ProcessAnnotatedType
     * @param pat the annotated type representing the class being processed
     */
    public <X> void processInjectionTarget(@Observes @WithAnnotations({Stateless.class, MessageDriven.class, Interceptor.class, Singleton.class}) ProcessAnnotatedType<X> pat) {
        if (pat.getAnnotatedType().isAnnotationPresent(Stateless.class) || pat.getAnnotatedType().isAnnotationPresent(MessageDriven.class)) {
            modifiyAnnotatedTypeMetadata(pat);
        } else if (pat.getAnnotatedType().isAnnotationPresent(Interceptor.class)) {
            processInterceptorDependencies(pat);
        } else if(pat.getAnnotatedType().isAnnotationPresent(Singleton.class)) {
            addApplicationScopedAndTransactionalToSingleton(pat);
        }
    }
    
    /**
     * Adds {@link Transactional} and {@link ApplicationScoped} to the given annotated type and converts
     * its EJB injection points into CDI injection points (i.e. it adds the {@link Inject})
     * @param <X> the type of the annotated type.
     * @param pat the process annotated type.
     */
    private <X> void addApplicationScopedAndTransactionalToSingleton(ProcessAnnotatedType<X> pat) {
        AnnotatedType at = pat.getAnnotatedType();
        
        AnnotatedTypeBuilder<X> builder = new AnnotatedTypeBuilder<X>().readFromType(at);
        
        builder.addToClass(AnnotationInstances.APPLICATION_SCOPED).addToClass(AnnotationInstances.TRANSACTIONAL);
        
        InjectionHelper.addInjectAnnotation(at, builder);
        
        pat.setAnnotatedType(builder.create());
    }

    /**
     * Adds {@link Transactional} and {@link RequestScoped} to the given annotated type and converts
     * its EJB injection points into CDI injection points (i.e. it adds the {@link Inject})
     * @param <X> the type of the annotated type
     * @param pat the process annotated type.
     */
    private <X> void modifiyAnnotatedTypeMetadata(ProcessAnnotatedType<X> pat) {
        AnnotatedType at = pat.getAnnotatedType();
        
        AnnotatedTypeBuilder<X> builder = new AnnotatedTypeBuilder<X>().readFromType(at);
        builder.addToClass(AnnotationInstances.TRANSACTIONAL).addToClass(AnnotationInstances.REQUEST_SCOPED);

        InjectionHelper.addInjectAnnotation(at, builder);
        //Set the wrapper instead the actual annotated type
        pat.setAnnotatedType(builder.create());

    }
    
    /**
     * Adds {@link Inject} annotation to all the dependencies of the interceptor.
     * 
     * @param <X>
     *            the type of the annotated type
     * @param pat
     *            the process annotated type.
     */
    private <X> void processInterceptorDependencies(ProcessAnnotatedType<X> pat) {
        AnnotatedTypeBuilder<X> builder = new AnnotatedTypeBuilder<X>().readFromType(pat.getAnnotatedType());
        InjectionHelper.addInjectAnnotation(pat.getAnnotatedType(), builder);
        pat.setAnnotatedType(builder.create());
    }
    

}
