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
package edu.kit.anthropomatik.h2t.expertsystem.reasoning;

import edu.kit.anthropomatik.h2t.expertsystem.MyOWLHelper;
import edu.kit.anthropomatik.h2t.expertsystem.generated.Vocabulary;
import openllet.owlapi.OWLHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLIndividual;

import java.util.*;
import java.util.stream.Collectors;

public class ReasoningTreeSpecialCases {

    private static final Logger logger = LogManager.getLogger(ReasoningTreeSpecialCases.class);

    private OWLHelper genericTool;
    private MyOWLHelper helper;

    private Map<OWLClass, Set<OWLIndividual>> handledPossibleUnsatisfied = new HashMap<>();
    private Map<OWLClass, Set<OWLIndividual>> handledPossibleSatisfied = new HashMap<>();

    private boolean didBackupOnLastRun;

    ReasoningTreeSpecialCases(OWLHelper genericTool, MyOWLHelper helper) {
        this.genericTool = genericTool;
        this.helper = helper;
    }

    public void reset() {
        handledPossibleUnsatisfied.clear();
        handledPossibleSatisfied.clear();
    }

    public boolean handleUnsatisfied(OWLClass subClassOfUnsatisfied) {
        long startTime = System.currentTimeMillis();
        Set<OWLAxiom> axiomsToDelete = new HashSet<>();
        genericTool.getReasoner().instances(subClassOfUnsatisfied).forEach(indiToDelete -> helper.getGeneratedAxioms
                ().stream().filter(axiom -> axiom.individualsInSignature().anyMatch(indi -> indi.equals(indiToDelete)
        )).forEach(axiomsToDelete::add));

        if (!axiomsToDelete.isEmpty()) {
            InformationToDelete infoToDelete = new InformationToDelete();
            genericTool.getOntology().subClassAxiomsForSubClass(subClassOfUnsatisfied).forEach(topAxiom -> {
                if (topAxiom.getSuperClass().objectPropertiesInSignature().anyMatch(Vocabulary
                        .OBJECT_PROPERTY_HASCOUNTERSATISFIEDPART::equals)) {
                    topAxiom.getSuperClass().classesInSignature().collect(Collectors.toCollection(() -> infoToDelete
                            .counterSatisfiedPart));
                } else if (topAxiom.getSuperClass().objectPropertiesInSignature().anyMatch(Vocabulary
                        .OBJECT_PROPERTY_HASNEWSATISFIEDPART::equals)) {
                    topAxiom.getSuperClass().classesInSignature().collect(Collectors.toCollection(() -> infoToDelete
                            .newSatisfiedPart));
                }
            });

            logger.debug("SpecialCase: Unsatisfied: Deleted number of axioms: " + axiomsToDelete.size() + " for: " +
                    subClassOfUnsatisfied.getIRI().getShortForm());
            helper.removeAxioms(axiomsToDelete);
            helper.flush();

            infoToDelete.counterSatisfiedPart.forEach(counter -> infoToDelete.newSatisfiedPart.forEach(newPart ->
                    genericTool.getReasoner().instances(counter).forEach(counterInst -> helper.addAxiom(genericTool
                            .getFactory().getOWLClassAssertionAxiom(newPart, counterInst)))));
            helper.flush();
            double timeNeeded = (System.currentTimeMillis() - startTime) / 1000.0;
            if (timeNeeded >= ReasoningTree.TIME_NEEDED_THRESHOLD) {
                logger.debug("Time needed for handleUnsatisfied of " + subClassOfUnsatisfied.getIRI().getShortForm()
                        + " " + ": " + timeNeeded + "s");
            }
            return true;
        }
        return false;
    }

    private static class InformationToDelete {
        List<OWLClass> counterSatisfiedPart = new ArrayList<>();
        List<OWLClass> newSatisfiedPart = new ArrayList<>();

        List<OWLClass> backupSatisfiedPart = new ArrayList<>();
        List<OWLClass> possibleSatisfiedPart = new ArrayList<>();
    }

    public boolean handlePossibleUnsatisfied(OWLClass subClassOfPossibleUnsatisfied) {
        long startTime = System.currentTimeMillis();
        didBackupOnLastRun = false;
        if (!handledPossibleUnsatisfied.containsKey(subClassOfPossibleUnsatisfied)) {
            handledPossibleUnsatisfied.put(subClassOfPossibleUnsatisfied, new HashSet<>());
        }
        List<OWLIndividual> possibleUnsatisfiedIndis = new ArrayList<>();
        genericTool.getReasoner().instances(subClassOfPossibleUnsatisfied).filter(indi -> !handledPossibleUnsatisfied
                .get(subClassOfPossibleUnsatisfied).contains(indi)).collect(Collectors.toCollection(() ->
                possibleUnsatisfiedIndis));

        if (!possibleUnsatisfiedIndis.isEmpty()) {
            handledPossibleUnsatisfied.get(subClassOfPossibleUnsatisfied).addAll(possibleUnsatisfiedIndis);
            InformationToDelete infoToDelete = new InformationToDelete();
            genericTool.getOntology().subClassAxiomsForSubClass(subClassOfPossibleUnsatisfied).forEach(topAxiom -> {
                if (topAxiom.getSuperClass().objectPropertiesInSignature().anyMatch(Vocabulary
                        .OBJECT_PROPERTY_HASCOUNTERSATISFIEDPART::equals)) {
                    topAxiom.getSuperClass().classesInSignature().collect(Collectors.toCollection(() -> infoToDelete
                            .counterSatisfiedPart));
                } else if (topAxiom.getSuperClass().objectPropertiesInSignature().anyMatch(Vocabulary
                        .OBJECT_PROPERTY_HASNEWSATISFIEDPART::equals)) {
                    topAxiom.getSuperClass().classesInSignature().collect(Collectors.toCollection(() -> infoToDelete
                            .newSatisfiedPart));
                } else if (topAxiom.getSuperClass().objectPropertiesInSignature().anyMatch(Vocabulary
                        .OBJECT_PROPERTY_HASBACKUPSATISFIEDPART::equals)) {
                    topAxiom.getSuperClass().classesInSignature().collect(Collectors.toCollection(() -> infoToDelete
                            .backupSatisfiedPart));
                } else if (topAxiom.getSuperClass().objectPropertiesInSignature().anyMatch(Vocabulary
                        .OBJECT_PROPERTY_HASPOSSIBLESATISFIEDPART::equals)) {
                    topAxiom.getSuperClass().classesInSignature().collect(Collectors.toCollection(() -> infoToDelete
                            .possibleSatisfiedPart));
                }
            });
            List<OWLIndividual> counterSatisfiedWithoutUnsatisfiedIndis = new ArrayList<>();
            infoToDelete.counterSatisfiedPart.forEach(counter -> genericTool.getReasoner().instances(counter).filter
                    (indi -> !handledPossibleUnsatisfied.get(subClassOfPossibleUnsatisfied).contains(indi)).collect
                    (Collectors.toCollection(() -> counterSatisfiedWithoutUnsatisfiedIndis)));

            if (counterSatisfiedWithoutUnsatisfiedIndis.isEmpty()) {
                didBackupOnLastRun = true;
                logger.debug("SpecialCase: Possible unsatisfied: Move to backup: " + possibleUnsatisfiedIndis.size()
                        + " for: " + subClassOfPossibleUnsatisfied.getIRI().getShortForm());
                infoToDelete.backupSatisfiedPart.forEach(backup -> possibleUnsatisfiedIndis.forEach
                        (possibleUnsatisfiedInst -> helper.addAxiom(genericTool.getFactory()
                                .getOWLClassAssertionAxiom(backup, possibleUnsatisfiedInst))));
                helper.flush();

                if (!handledPossibleSatisfied.containsKey(subClassOfPossibleUnsatisfied)) {
                    handledPossibleSatisfied.put(subClassOfPossibleUnsatisfied, new HashSet<>());
                }
                infoToDelete.possibleSatisfiedPart.forEach(possibleSat -> genericTool.getReasoner().instances
                        (possibleSat).forEach(ind -> handledPossibleSatisfied.get(subClassOfPossibleUnsatisfied).add
                        (ind)));
            } else {
                Set<OWLAxiom> axiomsToDelete = new HashSet<>();
                possibleUnsatisfiedIndis.forEach(indiToDelete -> helper.getGeneratedAxioms().stream().filter(axiom ->
                        axiom.individualsInSignature().anyMatch(indi -> indi.equals(indiToDelete))).forEach
                        (axiomsToDelete::add));
                logger.debug("SpecialCase: Possible unsatisfied: Deleted number of axioms: " + axiomsToDelete.size()
                        + " for: " + subClassOfPossibleUnsatisfied.getIRI().getShortForm());
                helper.removeAxioms(axiomsToDelete);
                helper.flush();

                infoToDelete.newSatisfiedPart.forEach(newPart -> counterSatisfiedWithoutUnsatisfiedIndis.forEach
                        (counterInst -> helper.addAxiom(genericTool.getFactory().getOWLClassAssertionAxiom(newPart,
                                counterInst))));
            }
            double timeNeeded = (System.currentTimeMillis() - startTime) / 1000.0;
            if (timeNeeded >= ReasoningTree.TIME_NEEDED_THRESHOLD) {
                logger.debug("Time needed for handlePossibleUnsatisfied of " + subClassOfPossibleUnsatisfied.getIRI()
                        .getShortForm() + " " + ": " + timeNeeded + "s");
            }
            return true;
        }
        return false;
    }

    public boolean handlePossibleSatisfied(OWLClass subClassOfPossibleSatisfied) {
        long startTime = System.currentTimeMillis();
        if (!handledPossibleSatisfied.containsKey(subClassOfPossibleSatisfied)) {
            handledPossibleSatisfied.put(subClassOfPossibleSatisfied, new HashSet<>());
        }
        int oldSize = handledPossibleSatisfied.get(subClassOfPossibleSatisfied).size();
        genericTool.getReasoner().instances(subClassOfPossibleSatisfied).filter(indi -> !handledPossibleSatisfied.get
                (subClassOfPossibleSatisfied).contains(indi)).forEach(indi -> genericTool.getOntology()
                .subClassAxiomsForSubClass(subClassOfPossibleSatisfied).filter(topAxiom -> topAxiom.getSuperClass()
                        .objectPropertiesInSignature().anyMatch(Vocabulary
                                .OBJECT_PROPERTY_HASNEWSATISFIEDPART::equals)).forEach(topAxiom -> topAxiom
                        .getSuperClass().classesInSignature().forEach(newPart -> {
            helper.addAxiom(genericTool.getFactory().getOWLClassAssertionAxiom(newPart, indi));
            handledPossibleSatisfied.get(subClassOfPossibleSatisfied).add(indi);
        })));
        if (oldSize != handledPossibleSatisfied.get(subClassOfPossibleSatisfied).size()) {
            logger.debug("SpecialCase: Handled possible satisfied for: " + subClassOfPossibleSatisfied.getIRI()
                    .getShortForm());
            helper.flush();
            double timeNeeded = (System.currentTimeMillis() - startTime) / 1000.0;
            if (timeNeeded >= ReasoningTree.TIME_NEEDED_THRESHOLD) {
                logger.debug("Time needed for handlePossibleSatisfied of " + subClassOfPossibleSatisfied.getIRI()
                        .getShortForm() + " " + ": " + timeNeeded + "s");
            }
            return true;
        }
        return false;
    }

    public boolean didBackupOnLastRun() {
        return didBackupOnLastRun;
    }

}
