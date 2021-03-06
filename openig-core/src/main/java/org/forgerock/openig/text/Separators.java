/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Copyright 2010-2011 ApexIdentity Inc.
 * Portions Copyright 2011-2014 ForgeRock AS.
 */

package org.forgerock.openig.text;

/**
 * Commonly used field separator specifications.
 */
public enum Separators {

    /** Unix-style colon separated values, with backslash escape character. */
    COLON(new Separator(':', -1, '\\')),

    /** Comma separated values, with support for quoted literal strings. */
    COMMA(new Separator(',', '"', -1)),

    /** Tab separated values, with support for quoted literal strings. */
    TAB(new Separator('\t', '"', -1));

    /** The separator specification that the enum constant represents. */
    private final Separator separator;

    /**
     * Constructs a new separator constant with the specified separator specification.
     *
     * @param separator
     *         specified separator specification
     */
    Separators(Separator separator) {
        this.separator = separator;
    }

    /**
     * Returns the {@link Separator} specification that the enum constant represents.
     * @return the {@link Separator} specification that the enum constant represents.
     */
    public Separator getSeparator() {
        return separator;
    }
}
