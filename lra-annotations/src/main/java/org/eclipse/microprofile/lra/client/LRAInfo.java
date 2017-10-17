/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.eclipse.microprofile.lra.client;

/**
 * Data object carrying information about instance
 * of LRA (specified by lra id) and it's status.
 */
public interface LRAInfo {

    /**
     * @return  lra id that lra instance is identified by
     */
    String getLraId();

    /**
     * @return  lra client id, TODO: what is the purpose of this id?
     */
    String getClientId();

    /**
     * @return  true if lra was succesfully completed, false otherwise
     */
    boolean isComplete();

    /**
     * @return  true if lra was compensated, false otherwise
     */
    boolean isCompensated();

    /**
     * @return  true if recovery is in progress on the lra, false otherwise
     */
    boolean isRecovering();

    /**
     * @return  true if lra is in active state right now, false otherwise
     */
    boolean isActive();

    /**
     * @return  true if lra is top level (not nested), false otherwise
     */
    boolean isTopLevel();
}
