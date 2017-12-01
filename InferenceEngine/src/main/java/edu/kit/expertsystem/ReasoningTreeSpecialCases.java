package edu.kit.expertsystem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLIndividual;

import edu.kit.expertsystem.generated.Vocabulary;
import openllet.owlapi.OWLHelper;

public class ReasoningTreeSpecialCases {

    private static final Logger logger = LogManager.getLogger(ReasoningTreeSpecialCases.class);

    private OWLHelper genericTool;
    private MyOWLHelper helper;

    private Map<OWLClass, Set<OWLIndividual>> handledPossibleUnsatisfied = new HashMap<>();
    private Map<OWLClass, Set<OWLIndividual>> handledPossibleSatisfied = new HashMap<>();

    private boolean didBackupOnLastRun;

    public ReasoningTreeSpecialCases(OWLHelper genericTool, MyOWLHelper helper) {
        this.genericTool = genericTool;
        this.helper = helper;
    }

    public void reset() {
        handledPossibleUnsatisfied.clear();
        handledPossibleSatisfied.clear();
    }

    public boolean handleUnsatisfied(OWLClass subClassOfUnsatisfied) {
        Set<OWLAxiom> axiomsToDelete = new HashSet<>();
        genericTool.getReasoner().instances(subClassOfUnsatisfied)
                .forEach(indiToDelete -> helper.getGeneratedAxioms().stream().filter(
                        axiom -> axiom.individualsInSignature().anyMatch(indi -> indi.equals(indiToDelete)))
                        .forEach(axiom -> axiomsToDelete.add(axiom)));

        if (!axiomsToDelete.isEmpty()) {
            InformationToDelete infoToDelete = new InformationToDelete();
            genericTool.getOntology().subClassAxiomsForSubClass(subClassOfUnsatisfied).forEach(topAxiom -> {
                if (topAxiom.getSuperClass().objectPropertiesInSignature()
                        .anyMatch(ob -> Vocabulary.OBJECT_PROPERTY_HASCOUNTERSATISFIEDPART.equals(ob))) {
                    topAxiom.getSuperClass().classesInSignature()
                            .collect(Collectors.toCollection(() -> infoToDelete.counterSatisfiedPart));
                } else if (topAxiom.getSuperClass().objectPropertiesInSignature()
                        .anyMatch(ob -> Vocabulary.OBJECT_PROPERTY_HASNEWSATISFIEDPART.equals(ob))) {
                    topAxiom.getSuperClass().classesInSignature()
                            .collect(Collectors.toCollection(() -> infoToDelete.newSatisfiedPart));
                }
            });

            logger.info("Unsatisfied: Deleted number of axioms: " + axiomsToDelete.size() + " for: "
                    + subClassOfUnsatisfied.getIRI().getShortForm());
            genericTool.getOntology().removeAxioms(axiomsToDelete.stream());
            helper.getGeneratedAxioms().removeAll(axiomsToDelete);
            helper.flush();

            infoToDelete.counterSatisfiedPart.forEach(
                    counter -> infoToDelete.newSatisfiedPart.forEach(newPart -> genericTool.getReasoner()
                            .instances(counter).forEach(counterInst -> helper.addAxiom(genericTool
                                    .getFactory().getOWLClassAssertionAxiom(newPart, counterInst)))));
            helper.flush();
            return true;
        }
        return false;
    }

    private class InformationToDelete {
        public List<OWLClass> counterSatisfiedPart = new ArrayList<>();
        public List<OWLClass> newSatisfiedPart = new ArrayList<>();

        public List<OWLClass> backupSatisfiedPart = new ArrayList<>();
        public List<OWLClass> possibleSatisfiedPart = new ArrayList<>();
    }

    public boolean handlePossibleUnsatisfied(OWLClass subClassOfPossibleUnsatisfied) {
        didBackupOnLastRun = false;
        if (!handledPossibleUnsatisfied.containsKey(subClassOfPossibleUnsatisfied)) {
            handledPossibleUnsatisfied.put(subClassOfPossibleUnsatisfied, new HashSet<>());
        }
        List<OWLIndividual> possibleUnsatisfiedIndis = new ArrayList<>();
        genericTool.getReasoner().instances(subClassOfPossibleUnsatisfied)
                .filter(indi -> !handledPossibleUnsatisfied.get(subClassOfPossibleUnsatisfied).contains(indi))
                .collect(Collectors.toCollection(() -> possibleUnsatisfiedIndis));

        if (!possibleUnsatisfiedIndis.isEmpty()) {
            handledPossibleUnsatisfied.get(subClassOfPossibleUnsatisfied).addAll(possibleUnsatisfiedIndis);
            InformationToDelete infoToDelete = new InformationToDelete();
            genericTool.getOntology().subClassAxiomsForSubClass(subClassOfPossibleUnsatisfied)
                    .forEach(topAxiom -> {
                        if (topAxiom.getSuperClass().objectPropertiesInSignature().anyMatch(
                                ob -> Vocabulary.OBJECT_PROPERTY_HASCOUNTERSATISFIEDPART.equals(ob))) {
                            topAxiom.getSuperClass().classesInSignature().collect(
                                    Collectors.toCollection(() -> infoToDelete.counterSatisfiedPart));
                        } else if (topAxiom.getSuperClass().objectPropertiesInSignature()
                                .anyMatch(ob -> Vocabulary.OBJECT_PROPERTY_HASNEWSATISFIEDPART.equals(ob))) {
                            topAxiom.getSuperClass().classesInSignature()
                                    .collect(Collectors.toCollection(() -> infoToDelete.newSatisfiedPart));
                        } else if (topAxiom.getSuperClass().objectPropertiesInSignature().anyMatch(
                                ob -> Vocabulary.OBJECT_PROPERTY_HASBACKUPSATISFIEDPART.equals(ob))) {
                            topAxiom.getSuperClass().classesInSignature()
                                    .collect(Collectors.toCollection(() -> infoToDelete.backupSatisfiedPart));
                        } else if (topAxiom.getSuperClass().objectPropertiesInSignature().anyMatch(
                                ob -> Vocabulary.OBJECT_PROPERTY_HASPOSSIBLESATISFIEDPART.equals(ob))) {
                            topAxiom.getSuperClass().classesInSignature().collect(
                                    Collectors.toCollection(() -> infoToDelete.possibleSatisfiedPart));
                        }
                    });
            List<OWLIndividual> counterSatisfiedWithoutUnsatisfiedIndis = new ArrayList<>();
            infoToDelete.counterSatisfiedPart.forEach(counter -> genericTool.getReasoner().instances(counter)
                    .filter(indi -> !handledPossibleUnsatisfied.get(subClassOfPossibleUnsatisfied)
                            .contains(indi))
                    .collect(Collectors.toCollection(() -> counterSatisfiedWithoutUnsatisfiedIndis)));

            if (counterSatisfiedWithoutUnsatisfiedIndis.isEmpty()) {
                didBackupOnLastRun = true;
                logger.info("Possible unsatisfied: Move to backup: " + possibleUnsatisfiedIndis.size()
                        + " for: " + subClassOfPossibleUnsatisfied.getIRI().getShortForm());
                infoToDelete.backupSatisfiedPart.forEach(backup -> possibleUnsatisfiedIndis
                        .forEach(possibleUnsatisfiedInst -> helper.addAxiom(genericTool.getFactory()
                                .getOWLClassAssertionAxiom(backup, possibleUnsatisfiedInst))));
                helper.flush();

                if (!handledPossibleSatisfied.containsKey(subClassOfPossibleUnsatisfied)) {
                    handledPossibleSatisfied.put(subClassOfPossibleUnsatisfied, new HashSet<>());
                }
                infoToDelete.possibleSatisfiedPart
                        .forEach(possibleSat -> genericTool.getReasoner().instances(possibleSat).forEach(
                                ind -> handledPossibleSatisfied.get(subClassOfPossibleUnsatisfied).add(ind)));
            } else {
                Set<OWLAxiom> axiomsToDelete = new HashSet<>();
                possibleUnsatisfiedIndis.forEach(indiToDelete -> helper.getGeneratedAxioms().stream().filter(
                        axiom -> axiom.individualsInSignature().anyMatch(indi -> indi.equals(indiToDelete)))
                        .forEach(axiom -> axiomsToDelete.add(axiom)));
                logger.info("Possible unsatisfied: Deleted number of axioms: " + axiomsToDelete.size()
                        + " for: " + subClassOfPossibleUnsatisfied.getIRI().getShortForm());
                genericTool.getOntology().removeAxioms(axiomsToDelete.stream());
                helper.getGeneratedAxioms().removeAll(axiomsToDelete);
                helper.flush();

                infoToDelete.newSatisfiedPart.forEach(newPart -> counterSatisfiedWithoutUnsatisfiedIndis
                        .forEach(counterInst -> helper.addAxiom(
                                genericTool.getFactory().getOWLClassAssertionAxiom(newPart, counterInst))));
            }
            return true;
        }
        return false;
    }

    public boolean handlePossibleSatisfied(OWLClass subClassOfPossibleSatisfied) {
        if (!handledPossibleSatisfied.containsKey(subClassOfPossibleSatisfied)) {
            handledPossibleSatisfied.put(subClassOfPossibleSatisfied, new HashSet<>());
        }
        int oldSize = handledPossibleSatisfied.get(subClassOfPossibleSatisfied).size();
        genericTool.getReasoner().instances(subClassOfPossibleSatisfied)
                .filter(indi -> !handledPossibleSatisfied.get(subClassOfPossibleSatisfied).contains(indi))
                .forEach(indi -> genericTool.getOntology()
                        .subClassAxiomsForSubClass(subClassOfPossibleSatisfied)
                        .filter(topAxiom -> topAxiom.getSuperClass().objectPropertiesInSignature()
                                .anyMatch(ob -> Vocabulary.OBJECT_PROPERTY_HASNEWSATISFIEDPART.equals(ob)))
                        .forEach(
                                topAxiom -> topAxiom.getSuperClass().classesInSignature().forEach(newPart -> {
                                    helper.addAxiom(genericTool.getFactory()
                                            .getOWLClassAssertionAxiom(newPart, indi));
                                    handledPossibleSatisfied.get(subClassOfPossibleSatisfied).add(indi);
                                })));
        if (oldSize != handledPossibleSatisfied.get(subClassOfPossibleSatisfied).size()) {
            logger.info(
                    "Handled possible satisfied for: " + subClassOfPossibleSatisfied.getIRI().getShortForm());
            helper.flush();
            return true;
        }
        return false;
    }

    public boolean didBackupOnLastRun() {
        return didBackupOnLastRun;
    }

}