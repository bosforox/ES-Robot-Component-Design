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
package edu.kit.anthropomatik.h2t.expertsystem.model;

import edu.kit.anthropomatik.h2t.expertsystem.model.req.Requirement;

import java.util.List;
import java.util.Objects;

public class Result {

    public List<Component> components;
    public List<Requirement> requirements;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Result result = (Result) o;
        return Objects.equals(components, result.components) && Objects.equals(requirements, result.requirements);
    }

    @Override
    public int hashCode() {
        return Objects.hash(components, requirements);
    }

    @Override
    public String toString() {
        return "Result{" + "components=" + components + ", requirements=" + requirements + '}';
    }
}
