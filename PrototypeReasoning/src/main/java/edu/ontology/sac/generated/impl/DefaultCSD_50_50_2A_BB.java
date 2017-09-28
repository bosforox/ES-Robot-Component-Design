package edu.ontology.sac.generated.impl;

import java.util.Collection;

import org.protege.owl.codegeneration.impl.WrappedIndividualImpl;
import org.protege.owl.codegeneration.inference.CodeGenerationInference;
import org.semanticweb.owlapi.model.IRI;

import edu.ontology.sac.generated.CSD_50_50_2A_BB;
import edu.ontology.sac.generated.Device;
import edu.ontology.sac.generated.Vocabulary;

/**
 * Generated by Protege (http://protege.stanford.edu).<br>
 * Source Class: DefaultCSD_50_50_2A_BB <br>
 * 
 * @version generated on Wed Sep 27 21:00:27 CEST 2017 by Oliver
 */
public class DefaultCSD_50_50_2A_BB extends WrappedIndividualImpl implements CSD_50_50_2A_BB {

    public DefaultCSD_50_50_2A_BB(CodeGenerationInference inference, IRI iri) {
        super(inference, iri);
    }

    /*
     * *************************************************** Object Property
     * http://www.semanticweb.org/oliver/ontologies/sac/prototypeV3#isComposedOf
     */

    public Collection<? extends Device> getIsComposedOf() {
        return getDelegate().getPropertyValues(getOwlIndividual(), Vocabulary.OBJECT_PROPERTY_ISCOMPOSEDOF,
                DefaultDevice.class);
    }

    public boolean hasIsComposedOf() {
        return !getIsComposedOf().isEmpty();
    }

    public void addIsComposedOf(Device newIsComposedOf) {
        getDelegate().addPropertyValue(getOwlIndividual(), Vocabulary.OBJECT_PROPERTY_ISCOMPOSEDOF,
                newIsComposedOf);
    }

    public void removeIsComposedOf(Device oldIsComposedOf) {
        getDelegate().removePropertyValue(getOwlIndividual(), Vocabulary.OBJECT_PROPERTY_ISCOMPOSEDOF,
                oldIsComposedOf);
    }

}
