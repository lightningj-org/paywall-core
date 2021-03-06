/*
 * ***********************************************************************
 *                                                                       *
 *  LightningJ                                                           *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public License   *
 *  (LGPL-3.0-or-later)                                                  *
 *  License as published by the Free Software Foundation; either         *
 *  version 3 of the License, or any later version.                      *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.lightningj.paywall.keymgmt;

/**
 * Extendable class specifying context of crytographic operations.
 *
 *  Created by Philip Vendil on 2018-10-06.
 */
public abstract class Context {


    public enum KeyUsage{
        /**
         * Used to request an authentication key.
         */
        AUTH,
        /**
         * Used to request an signature key.
         */
        SIGN,
        /**
         * Used to request an encryption key.
         */
        ENC
    }
}
