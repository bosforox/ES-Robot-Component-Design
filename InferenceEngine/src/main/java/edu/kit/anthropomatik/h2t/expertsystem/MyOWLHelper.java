/*
 * Copyright 2018 Oliver Karrenbauer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation * files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, * * * copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package edu.kit.anthropomatik.h2t.expertsystem;

import edu.kit.anthropomatik.h2t.expertsystem.generated.Vocabulary;
import openllet.owlapi.OWLGenericTools;
import openllet.owlapi.OWLHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MyOWLHelper {

    public static final String INDIVIDUAL_ENDING = "Ind";

    private static final Logger logger = LogManager.getLogger(MyOWLHelper.class);

    private OWLHelper genericTool;

    private Set<OWLAxiom> generatedAxioms = new HashSet<>();
    private Set<OWLAxiom> removedAxioms = new HashSet<>();

    MyOWLHelper(OWLGenericTools genericTool) {
        this.genericTool = genericTool;
    }

    public IRI create(String name) {
        return IRI.create("#" + name);
    }

    public boolean addAxiom(OWLAxiom axiomToAdd) {
        if (removedAxioms.contains(axiomToAdd) || !generatedAxioms.add(axiomToAdd)) {
            return false;
        }
        genericTool.getOntology().addAxiom(axiomToAdd);
        return true;
    }

    public void removeAxioms(Collection<OWLAxiom> axioms) {
        genericTool.getOntology().removeAxioms(axioms.stream());
        removedAxioms.addAll(axioms);
        generatedAxioms.removeAll(axioms);

        //TODO test
        //        if (!axioms.isEmpty()) {
        //            PelletReasoner reasoner = (PelletReasoner) genericTool.getReasoner();
        //            reasoner.refresh();
        //        }
    }

    public void flush() {
        genericTool.getReasoner().flush();
    }

    /**
     * Remove INDIVIDUAL_ENDING
     */
    public String getNameOfOWLNamedIndividual(OWLNamedIndividual ind) {
        return ind.getIRI().getShortForm().substring(0, ind.getIRI().getShortForm().length() - INDIVIDUAL_ENDING
                .length());
    }

    /**
     * Remove "has"
     */
    public String getNameOfOWLObjectProperty(OWLObjectProperty obj) {
        return obj.getIRI().getShortForm().substring(3, obj.getIRI().getShortForm().length());
    }

    /**
     * Remove "isComposedOf"
     */
    public String getNameOfComponent(OWLObjectProperty prop) {
        return prop.getIRI().getShortForm().substring(12, prop.getIRI().getShortForm().length());
    }

    public double parseValueToDouble(OWLLiteral obProp) {
        if (obProp.isInteger()) {
            return obProp.parseInteger();
        }
        return obProp.parseDouble();
    }

    public int parseValueToInteger(OWLLiteral obProp) {
        if (obProp.isInteger()) {
            return obProp.parseInteger();
        }
        return Math.round(obProp.parseFloat());
    }

    public void clearGeneratedAxioms() {
        genericTool.getManager().removeAxioms(genericTool.getOntology(), generatedAxioms.stream());
        generatedAxioms.clear();
        removedAxioms.clear();
    }

    public Set<OWLAxiom> getGeneratedAxioms() {
        return generatedAxioms;
    }

    public int getOrderPositionForClass(OWLClass clas) {
        AtomicInteger value = new AtomicInteger(0);
        genericTool.getOntology().subClassAxiomsForSubClass(clas).forEach(axiom -> axiom.componentsWithoutAnnotations
                ().filter(comp -> comp instanceof OWLDataHasValue && Vocabulary.DATA_PROPERTY_HASORDERPOSITION.equals
                (((OWLDataHasValue) comp).getProperty())).findAny().ifPresent(comp -> value.set(parseValueToInteger((
                        (OWLDataHasValue) comp).getFiller()))));
        return value.get();
    }

    public boolean getShowDefaultInResultsForClass(OWLClass clas) {
        AtomicBoolean value = new AtomicBoolean(true);
        genericTool.getOntology().subClassAxiomsForSubClass(clas).forEach(axiom -> axiom.componentsWithoutAnnotations
                ().filter(comp -> comp instanceof OWLDataHasValue && Vocabulary.DATA_PROPERTY_SHOWDEFAULTINRESULTS
                .equals(((OWLDataHasValue) comp).getProperty())).findAny().ifPresent(comp -> value.set((
                        (OWLDataHasValue) comp).getFiller().parseBoolean())));
        return value.get();
    }

    public boolean checkConsistency() {
        boolean isConsitent = genericTool.getReasoner().isConsistent();
        if (isConsitent) {
            logger.info("Ontology is consistent");
        } else {
            logger.warn("Ontology is not consistent!");
        }
        return isConsitent;
    }
}
