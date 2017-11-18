package edu.kit.expertsystem;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import edu.kit.expertsystem.generated.Vocabulary;
import openllet.owlapi.OWLGenericTools;
import openllet.owlapi.OWLHelper;

public class MyOWLHelper {

    // private static final Logger logger = LogManager.getLogger(MyOWLHelper.class);

    private OWLHelper genericTool;

    private Set<OWLAxiom> generatedAxioms = new HashSet<>();

    public MyOWLHelper(OWLGenericTools genericTool) {
        this.genericTool = genericTool;
    }

    public IRI create(String name) {
        return IRI.create("#" + name);
    }

    public void addAxiom(OWLAxiom axiomToAdd) {
        generatedAxioms.add(axiomToAdd);
        genericTool.getManager().addAxiom(genericTool.getOntology(), axiomToAdd);
    }

    public void flush() {
        genericTool.getReasoner().flush();
    }

    /**
     * Remove "Ind"
     * 
     * @param ind
     * @return
     */
    public String getNameOfOWLNamedIndividual(OWLNamedIndividual ind) {
        return ind.getIRI().getShortForm().substring(0, ind.getIRI().getShortForm().length() - 3);
    }

    /**
     * Remove "isComposedOf"
     * 
     * @param namedProperty
     * @return
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
    }

    public boolean deleteInstance(Stream<OWLClass> subClassesOfUnsatisfied) {
        List<InformationToDelete> informationToDelete = new ArrayList<>();
        Set<OWLAxiom> axiomsToDelete = new HashSet<>();

        subClassesOfUnsatisfied.forEach(subClas -> {
            int oldSize = axiomsToDelete.size();
            genericTool.getReasoner().instances(subClas)
                    .forEach(indiToDelete -> generatedAxioms.stream()
                            .filter(axiom -> axiom.individualsInSignature()
                                    .anyMatch(indi -> indi.equals(indiToDelete)))
                            .forEach(axiom -> axiomsToDelete.add(axiom)));

            if (oldSize != axiomsToDelete.size()) {
                InformationToDelete infToDelete = new InformationToDelete();
                genericTool.getOntology().subClassAxiomsForSubClass(subClas).forEach(topAxiom -> {
                    if (topAxiom.getSuperClass().objectPropertiesInSignature()
                            .anyMatch(ob -> Vocabulary.OBJECT_PROPERTY_HASCOUNTERSATISFIEDPART.equals(ob))) {
                        topAxiom.getSuperClass().classesInSignature()
                                .collect(Collectors.toCollection(() -> infToDelete.counterSatisfiedPart));
                    } else if (topAxiom.getSuperClass().objectPropertiesInSignature()
                            .anyMatch(ob -> Vocabulary.OBJECT_PROPERTY_HASNEWSATISFIEDPART.equals(ob))) {
                        topAxiom.getSuperClass().classesInSignature()
                                .collect(Collectors.toCollection(() -> infToDelete.newSatisfiedPart));
                    }
                });
                informationToDelete.add(infToDelete);
            }
        });

        if (!axiomsToDelete.isEmpty()) {
            genericTool.getManager().removeAxioms(genericTool.getOntology(), axiomsToDelete.stream());
            generatedAxioms.removeAll(axiomsToDelete);
            flush();

            for (InformationToDelete infoDelete : informationToDelete) {
                infoDelete.counterSatisfiedPart
                        .forEach(counter -> infoDelete.newSatisfiedPart.forEach(newPart -> genericTool
                                .getReasoner().instances(counter).forEach(counterInst -> addAxiom(genericTool
                                        .getFactory().getOWLClassAssertionAxiom(newPart, counterInst)))));
            }
            flush();
            return true;
        }
        return false;
    }

    private class InformationToDelete {
        public List<OWLClass> counterSatisfiedPart = new ArrayList<>();
        public List<OWLClass> newSatisfiedPart = new ArrayList<>();
    }

    public int getOrderPositionForClass(OWLClass clas) {
        AtomicInteger value = new AtomicInteger(0);
        genericTool.getOntology().subClassAxiomsForSubClass(clas).forEach(axiom -> axiom
                .componentsWithoutAnnotations()
                .filter(comp -> comp instanceof OWLDataHasValue && Vocabulary.DATA_PROPERTY_HASORDERPOSITION
                        .equals(((OWLDataHasValue) comp).getProperty()))
                .findAny()
                .ifPresent(comp -> value.set(parseValueToInteger(((OWLDataHasValue) comp).getFiller()))));
        return value.get();
    }

}
