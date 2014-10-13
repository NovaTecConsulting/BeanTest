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

import info.novatec.beantest.demo.ejb.InvalidInjectionPointConfigurationEJB;
import info.novatec.beantest.transactions.Transactional;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.ejb.Stateless;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.inject.Inject;
import javax.interceptor.Interceptor;
import javax.persistence.PersistenceContext;

import org.apache.deltaspike.core.util.metadata.AnnotationInstanceProvider;
import org.apache.deltaspike.core.util.metadata.builder.AnnotatedTypeBuilder;
import org.jboss.weld.exceptions.DefinitionException;

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
     * <li> {@link Interceptor}
     * </ul>
     *
     * @param <X> the type of the ProcessAnnotatedType
     * @param pat the annotated type representing the class being processed
     */
    public <X> void processInterceptorBeans(@Observes @WithAnnotations(Interceptor.class) ProcessAnnotatedType<X> pat) {
        processInterceptorDependencies(pat);
    }
    
    /**
     * Replaces the meta data of the {@link ProcessAnnotatedType}.
     * 
     * <p>
     * The ProcessAnnotatedType's meta data will be replaced, if the annotated type has one of the following annotations:
     * <ul>
     * <li> {@link Stateless}
     * <li> {@link MessageDriven}
     * </ul>
     *
     * @param <X> the type of the ProcessAnnotatedType
     * @param pat the annotated type representing the class being processed
     */
    public <X> void processStatelessOrMessagedrivenBeans(@Observes @WithAnnotations({Stateless.class, MessageDriven.class}) ProcessAnnotatedType<X> pat) {
    	modifyAnnotatedTypeMetaData(pat);
    }

    /**
     * Adds {@link Transactional} and {@link RequestScoped} to the given annotated type and converts
     * its EJB injection points into CDI injection points (i.e. it adds the {@link Inject})
     * @param <X> the type of the annotated type
     * @param pat the process annotated type.
     */
    private <X> void modifyAnnotatedTypeMetaData(ProcessAnnotatedType<X> pat) {
        Transactional transactionalAnnotation = AnnotationInstanceProvider.of(Transactional.class);
        RequestScoped requestScopedAnnotation = AnnotationInstanceProvider.of(RequestScoped.class);

        AnnotatedType<X> at = pat.getAnnotatedType();
        
        AnnotatedTypeBuilder<X> builder = new AnnotatedTypeBuilder<X>().readFromType(at);
        builder.addToClass(transactionalAnnotation).addToClass(requestScopedAnnotation);

    	addInjectAnnotation(at, builder);

        /* Replaces the actual annotated type in the processed bean or interceptor with the wrapper. */
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
        addInjectAnnotation(pat.getAnnotatedType(), builder);
        pat.setAnnotatedType(builder.create());
    }
    
     /**
     * Adds the {@link Inject} annotation to the fields and setters of the annotated type if required.
     * 
     * @param <X>
     *            the type of the annotated type
     * @param annotatedType
     *            the annotated type whose fields and setters the inject annotation should be added to
     * @param builder
     *            the builder that should be used to add the annotation.
     * @see #shouldInjectionAnnotationBeAddedToMember(AnnotatedField) and #shouldInjectionAnnotationBeAddedToMethod(AnnotatedMethod)
     */
    private <X> void addInjectAnnotation(final AnnotatedType<X> annotatedType, AnnotatedTypeBuilder<X> builder) {
    	new InjectionPointReplacement<X>(annotatedType, builder).performReplacements();
    }
   
    /**
     * This class is responsible for transforming {@link EJB}, {@link PersistenceContext} or {@link Resource} injection points into correlating {@link Inject} dependency definitions.
     * <p>
     * Furthermore this class ensures that the processed bean holds valid dependency injection points for its member i.e. the processed bean may hold exclusively field injection points
     * or setter injection points for a particular member.
     * <p>
     * By way of example the {@link InvalidInjectionPointConfigurationEJB} holds an invalid dependency configuration.  
     * @author Qaiser Abbasi (qaiser.abbasi@novatec-gmbh.de)
     *
     */
    private static class InjectionPointReplacement<X> {
    	
    	private static final String SETTER_METHOD_PREFIX = "set";

    	private static final int FIELD_NAME_INDEX = 1;
    	
		private static final Inject INJECT_ANNOTATION = AnnotationInstanceProvider.of(Inject.class);

    	private List<String> processedFieldInjections = new ArrayList<String>();
    	
    	private final AnnotatedType<X> annotatedType;
    	
    	private final AnnotatedTypeBuilder<X> builder;
    	
    	public InjectionPointReplacement(AnnotatedType<X> annotatedType, AnnotatedTypeBuilder<X> builder) {
    		this.annotatedType = annotatedType;
    		this.builder = builder;
    	}
    
    	/**
         * Returns <code>true</code> if the member is NOT annotated with {@link Inject} and is annotated with one of the following annotations:
         * <ul>
         * <li> {@link EJB}
         * <li> {@link PersistenceContext}
         * <li> {@link Resource}
         * </ul>
         * Otherwise, it returns <code>false</code>.
         * 
         * @param <X>
         *            the type of the annotated member
         * @param member
         *            the annotated member whose annotations should be verified
         * @return <code>true</code> if the member is NOT annotated with {@link Inject} and is annotated with {@link EJB},
         *         {@link PersistenceContext} or {@link Resource}
         */
        private boolean shouldInjectionAnnotationBeAddedToMember(AnnotatedMember<? super X> member) {
            return ! member.isAnnotationPresent(Inject.class) && (member.isAnnotationPresent(Resource.class)
            		|| member.isAnnotationPresent(EJB.class) || member.isAnnotationPresent(PersistenceContext.class));
        }
    	
        /**
         * This method performs the actual injection point transformation while ensuring that the processed bean comprise a valid dependency configuration.  
         */
    	public void performReplacements() {
		   for (AnnotatedField<? super X> field : annotatedType.getFields()) {
	             if (shouldInjectionAnnotationBeAddedToMember(field)) {
	                 builder.addToField(field, INJECT_ANNOTATION);
	                 processedFieldInjections.add(field.getJavaMember().getName());
	             }
	         }
	      
	         for (AnnotatedMethod<? super X> method : annotatedType.getMethods()) {
	            if (shouldInjectionAnnotationBeAddedToMember(method)) {
	            	validateDependencyConfiguration(method);
	            	builder.addToMethod(method, INJECT_ANNOTATION);
	            }
	        }
    	}

    	/**
    	 * Check if there is already a processed field injection point for the corresponding member. If so, the setter injection method for the given member is not legal.
    	 * The invalid dependency configuration lead towards to a deployment exception. 
    	 * @param method Represent the suffix-name of the corresponding setter injection method in the processed bean.
    	 * @throws DefinitionException Occurs if there is already a processed field for the given member.
    	 */
		private void validateDependencyConfiguration(AnnotatedMethod<? super X> method) {
			if(method.getJavaMember().getName().startsWith(SETTER_METHOD_PREFIX)){
				String methodSuffixName = method.getJavaMember().getName().split(SETTER_METHOD_PREFIX)[FIELD_NAME_INDEX];
				if(isInjectionPointAlreadyProcessed(methodSuffixName)){
					throw new DefinitionException(String.format("Invalid dependency definition in declaring class: %s."
							+ " Found duplicate injection points for method %s and corresponding field", annotatedType, method.getJavaMember().getName()));
				}
			}
		}

		private boolean isInjectionPointAlreadyProcessed(String methodSuffixName) {
			for (String processedField : processedFieldInjections) {
				if (processedField.equalsIgnoreCase(methodSuffixName)) {
					return true;
				}
			}
			
			return false;
		}
    	
    }

}
