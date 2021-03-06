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
package org.lightningj.paywall.util;

import org.lightningj.paywall.InternalErrorException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * Digest helper is a utility class to create digests.
 *
 * Created by Philip Vendil on 2018-09-16.
 */
public class DigestUtils {

    public static byte[] sha256(byte[] data) throws InternalErrorException {
        try {
            MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256","BC");
            sha256Digest.reset();
            sha256Digest.update(data);
            return sha256Digest.digest();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new InternalErrorException("Internal error generating SHA256 digest: " + e.getMessage(),e);
        }
    }

    public static byte[] ripeMD160(byte[] data) throws InternalErrorException {
        try {
            MessageDigest ripeMD160Digest = MessageDigest.getInstance("RipeMD160","BC");
            ripeMD160Digest.reset();
            ripeMD160Digest.update(data);
            return ripeMD160Digest.digest();
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new InternalErrorException("Internal error generating RipeMD160 digest: " + e.getMessage(),e);
        }
    }
}
