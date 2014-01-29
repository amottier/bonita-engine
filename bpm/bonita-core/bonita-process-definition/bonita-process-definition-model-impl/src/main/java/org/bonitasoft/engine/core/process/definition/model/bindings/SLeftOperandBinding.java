/**
 * Copyright (C) 2012 BonitaSoft S.A.
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation
 * version 2.1 of the License.
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth
 * Floor, Boston, MA 02110-1301, USA.
 **/
package org.bonitasoft.engine.core.process.definition.model.bindings;

import java.util.Map;

import org.bonitasoft.engine.builder.BuilderFactory;
import org.bonitasoft.engine.core.operation.model.SLeftOperandType;
import org.bonitasoft.engine.core.operation.model.builder.SLeftOperandBuilderFactory;
import org.bonitasoft.engine.xml.ElementBinding;

/**
 * @author Baptiste Mesta
 */
public class SLeftOperandBinding extends ElementBinding {

    private String name;

    private SLeftOperandType type;

    @Override
    public String getElementTag() {
        return XMLSProcessDefinition.OPERATION_LEFT_OPERAND;
    }

    @Override
    public void setAttributes(final Map<String, String> attributes) {
        name = attributes.get(XMLSProcessDefinition.LEFT_OPERAND_NAME);
        type = SLeftOperandType.valueOf(attributes.get(XMLSProcessDefinition.LEFT_OPERAND_TYPE));
    }

    @Override
    public void setChildElement(final String name, final String value, final Map<String, String> attributes) {

    }

    @Override
    public void setChildObject(final String name, final Object value) {

    }

    @Override
    public Object getObject() {
        return BuilderFactory.get(SLeftOperandBuilderFactory.class).createNewInstance().setName(name).setType(type).done();
    }

}
