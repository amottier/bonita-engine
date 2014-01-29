/**
 * Copyright (C) 2012-2013 BonitaSoft S.A.
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
package org.bonitasoft.engine.operation;

import java.io.Serializable;

/**
 * @author Zhang Bole
 * @author Matthieu Chaffotte
 * @author Emmanuel Duchastenier
 * @author Baptiste Mesta
 */
public interface LeftOperand extends Serializable {

    String getName();

    /**
     * @deprecated As of 6.0 replaced by {@link #getName()}
     */
    @Deprecated
    String getDataName();

    /**
     * The type of the left operand
     * 
     * It define what kind of resource the operation will update
     * 
     * @return
     *         the type of the left operand
     */
    LeftOperandType getType();

    boolean isExternal();
}
