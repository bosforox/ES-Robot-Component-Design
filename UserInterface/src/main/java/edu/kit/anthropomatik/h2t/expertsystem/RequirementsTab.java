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

import edu.kit.anthropomatik.h2t.expertsystem.controller.wrapper.*;
import edu.kit.anthropomatik.h2t.expertsystem.model.req.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;

import java.util.List;

public class RequirementsTab {

    private static final int btnEnalbeFieldOffsetXEnd = 20;
    private static final int btnEnalbeFieldOffsetYEnd = 7;

    private final FormToolkit formToolkit;
    private final boolean isOptimization;

    private SashForm requirementsForm;
    private RequirementsHelper requirementsHelper;

    private ScrolledComposite leftScrolledComposite;
    private Composite leftComposite;
    private Composite rightComposite;
    private Button btnExpertMode;
    private DescriptionHelper descriptionHelper;

    RequirementsTab(Composite parent, FormToolkit formToolkit, Rectangle sizeOfForm, boolean isOptimization) {
        this.formToolkit = formToolkit;
        this.isOptimization = isOptimization;

        requirementsForm = new SashForm(parent, SWT.NONE);
        requirementsForm.setBounds(sizeOfForm);
        formToolkit.adapt(requirementsForm);
        formToolkit.paintBordersFor(requirementsForm);
    }

    public void createContents(Category category, List<RequirementWrapper> requirements,
                               List<RequirementDependencyCheckboxWrapper> requirementDependencyWrappers) {
        leftScrolledComposite = new ScrolledComposite(requirementsForm, SWT.V_SCROLL | SWT.H_SCROLL);
        leftScrolledComposite.setExpandVertical(true);
        leftScrolledComposite.setExpandHorizontal(true);
        formToolkit.adapt(leftScrolledComposite);

        leftComposite = new Composite(leftScrolledComposite, SWT.NONE);
        formToolkit.adapt(leftComposite);
        leftScrolledComposite.setContent(leftComposite);

        boolean isAnyFieldDisabled = false;
        requirementsHelper = new RequirementsHelper(formToolkit, leftComposite, category, isOptimization);
        int lastReqOrderPosition = -1;
        int rowNumber = 0;
        for (RequirementWrapper requirement1 : requirements) {
            if (requirement1 instanceof TextFieldMinMaxRequirementWrapper) {
                TextFieldMinMaxRequirement req = (TextFieldMinMaxRequirement) requirement1.requirement;
                isAnyFieldDisabled |= !req.enableMin || !req.enableMax;
            } else if (requirement1 instanceof TextFieldRequirementWrapper) {
                TextFieldRequirement req = (TextFieldRequirement) requirement1.requirement;
                isAnyFieldDisabled |= !req.enable;
            } else if (requirement1 instanceof CheckboxRequirementWrapper) {
                CheckboxRequirement req = (CheckboxRequirement) requirement1.requirement;
                isAnyFieldDisabled |= !req.enable;
            } else if (requirement1 instanceof DropdownRequirementWrapper) {
                DropdownRequirement req = (DropdownRequirement) requirement1.requirement;
                isAnyFieldDisabled |= !req.enable;
            } else if (requirement1 instanceof RequirementOnlyForSolutionWrapper) {
                // RequirementOnlyForSolution will not be displayed
            } else {
                throw new RuntimeException("Requirement class unknown: " + requirement1.getClass());
            }
            if (lastReqOrderPosition == requirement1.requirement.orderPosition && !isOptimization) {
                rowNumber--;
            }
            lastReqOrderPosition = requirement1.requirement.orderPosition;
            if (requirementsHelper.createRequirement(requirement1, requirementDependencyWrappers, rowNumber)) {
                ++rowNumber;
            }
        }

        if (isAnyFieldDisabled && !isOptimization) {
            btnExpertMode = new Button(leftComposite, SWT.CHECK);
            updateExpertMode();
            btnExpertMode.setText("Expert Mode");
            btnExpertMode.addSelectionListener(new SelectionAdapter() {

                @Override
                public void widgetSelected(SelectionEvent event) {
                    for (RequirementWrapper req : requirements) {
                        if (req instanceof TextFieldMinMaxRequirementWrapper) {
                            TextFieldMinMaxRequirementWrapper reqWrapper = (TextFieldMinMaxRequirementWrapper) req;
                            TextFieldMinMaxRequirement realReq = (TextFieldMinMaxRequirement) req.requirement;
                            reqWrapper.minValue.setEnabled(realReq.enableMin || !reqWrapper.minValue.isEnabled());
                            reqWrapper.maxValue.setEnabled(realReq.enableMax || !reqWrapper.maxValue.isEnabled());
                        } else if (req instanceof TextFieldRequirementWrapper) {
                            TextFieldRequirementWrapper reqWrapper = (TextFieldRequirementWrapper) req;
                            TextFieldRequirement realReq = (TextFieldRequirement) req.requirement;
                            reqWrapper.value.setEnabled(realReq.enable || !reqWrapper.value.isEnabled());
                        } else if (req instanceof CheckboxRequirementWrapper) {
                            CheckboxRequirementWrapper reqWrapper = (CheckboxRequirementWrapper) req;
                            CheckboxRequirement realReq = (CheckboxRequirement) req.requirement;
                            reqWrapper.value.setEnabled(realReq.enable || !reqWrapper.value.isEnabled());
                        } else if (req instanceof DropdownRequirementWrapper) {
                            DropdownRequirementWrapper reqWrapper = (DropdownRequirementWrapper) req;
                            DropdownRequirement realReq = (DropdownRequirement) req.requirement;
                            reqWrapper.values.setEnabled(realReq.enable || !reqWrapper.values.isEnabled());
                        } else if (req instanceof RequirementOnlyForSolutionWrapper) {
                            // RequirementOnlyForSolution will not be displayed
                        } else {
                            throw new RuntimeException("Requirement class unknown: " + req.getClass());
                        }
                    }
                }
            });
        }

        Label separator = new Label(requirementsForm, SWT.SEPARATOR | SWT.VERTICAL);
        formToolkit.adapt(separator, false, false);

        ScrolledComposite rightScrolledComposite = new ScrolledComposite(requirementsForm, SWT.V_SCROLL);
        rightScrolledComposite.setExpandVertical(true);
        rightScrolledComposite.setExpandHorizontal(true);
        formToolkit.adapt(rightScrolledComposite);

        rightComposite = new Composite(rightScrolledComposite, SWT.NONE);
        formToolkit.adapt(rightComposite);
        rightScrolledComposite.setContent(rightComposite);

        rowNumber = 0;
        descriptionHelper = new DescriptionHelper(formToolkit, rightComposite);
        if (isOptimization) {
            descriptionHelper.createDescription("Deviation", "The amount of deviation the result can differ from.",
                    rowNumber++);
            descriptionHelper.createDescription("Priority", "Influences the NRMSD and performance index (PX) " +
                    "depending on the actual deviation.\n0=no influence\n5=very important", rowNumber++);
        }
        for (RequirementWrapper requirement : requirements) {
            if (requirement.requirement.description != null && !(requirement instanceof
                    RequirementOnlyForSolutionWrapper)) {
                if (isOptimization) {
                    if (!(requirement instanceof TextFieldMinMaxRequirementWrapper)) {
                        continue;
                    }
                    if (!((TextFieldMinMaxRequirement) ((TextFieldMinMaxRequirementWrapper) requirement).requirement)
                            .allowOptimization) {
                        continue;
                    }
                }
                descriptionHelper.createDescription(requirement.requirement.displayName, requirement.requirement
                        .description, rowNumber++);
            }
        }
        rightScrolledComposite.setMinHeight(descriptionHelper.getMaxYEnd());
    }

    public void updateSize(Rectangle sizeOfForm) {
        if (btnExpertMode != null) {
            // set to o so it will not extend leftComposite.width
            // it will be set back later on accordingly to leftComposite
            btnExpertMode.setBounds(0, 0, 0, 0);
        }
        requirementsForm.setBounds(sizeOfForm);
        formToolkit.adapt(requirementsForm);
        formToolkit.paintBordersFor(requirementsForm);
        requirementsHelper.updateSize(leftComposite.getBounds());
        leftScrolledComposite.setMinWidth(requirementsHelper.getMaxXEnd());
        leftScrolledComposite.setMinHeight(requirementsHelper.getMaxYEnd());
        descriptionHelper.updateSize(rightComposite.getBounds());
        updateExpertMode();
    }

    private void updateExpertMode() {
        if (btnExpertMode != null) {
            Point size = GuiHelper.getSizeOfControl(btnExpertMode);
            // Offset for width, to include the checkbox size
            int realWidth = size.x + btnEnalbeFieldOffsetXEnd;
            int xCord = leftComposite.getBounds().width - realWidth;
            int yCord = leftComposite.getBounds().height - size.y - btnEnalbeFieldOffsetYEnd;
            btnExpertMode.setBounds(xCord, yCord, realWidth, size.y);
            formToolkit.adapt(btnExpertMode, true, true);
        }
    }

    public SashForm getRequirementsForm() {
        return requirementsForm;
    }

}
