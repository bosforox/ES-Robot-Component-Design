package edu.ontology.sac.generated.impl;

import java.util.Collection;

import org.protege.owl.codegeneration.WrappedIndividual;
import org.protege.owl.codegeneration.impl.WrappedIndividualImpl;
import org.protege.owl.codegeneration.inference.CodeGenerationInference;
import org.semanticweb.owlapi.model.IRI;

import edu.ontology.sac.generated.Length;
import edu.ontology.sac.generated.Vocabulary;

/**
 * Generated by Protege (http://protege.stanford.edu).<br>
 * Source Class: DefaultLength <br>
 * 
 * @version generated on Wed Sep 27 21:00:28 CEST 2017 by Oliver
 */
public class DefaultLength extends WrappedIndividualImpl implements Length {

    public DefaultLength(CodeGenerationInference inference, IRI iri) {
        super(inference, iri);
    }

    /*
     * *************************************************** Object Property
     * http://www.semanticweb.org/oliver/ontologies/sac/prototypeV3#isPropertyOf
     */

    public Collection<? extends WrappedIndividual> getIsPropertyOf() {
        return getDelegate().getPropertyValues(getOwlIndividual(), Vocabulary.OBJECT_PROPERTY_ISPROPERTYOF,
                WrappedIndividualImpl.class);
    }

    public boolean hasIsPropertyOf() {
        return !getIsPropertyOf().isEmpty();
    }

    public void addIsPropertyOf(WrappedIndividual newIsPropertyOf) {
        getDelegate().addPropertyValue(getOwlIndividual(), Vocabulary.OBJECT_PROPERTY_ISPROPERTYOF,
                newIsPropertyOf);
    }

    public void removeIsPropertyOf(WrappedIndividual oldIsPropertyOf) {
        getDelegate().removePropertyValue(getOwlIndividual(), Vocabulary.OBJECT_PROPERTY_ISPROPERTYOF,
                oldIsPropertyOf);
    }

}
