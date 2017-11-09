/*
 * Copyright (c) 2007, Eclipse Foundation, Inc. and its licensors.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * Neither the name of the Eclipse Foundation, Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.eclipse.microprofile.sra.client;

import java.net.URL;

public class SRAInfo {
    private String sraId;
    private String clientId ;
    private boolean isRecovering;
    private boolean isActive;
    private boolean isTopLevel;

    public SRAInfo(String sraId) {
        this.sraId = sraId;
    }
    public SRAInfo(URL sraId) {
        this.sraId = sraId.toString();
    }

    public SRAInfo(String sraId, String clientId, boolean isRecovering, boolean isActive, boolean isTopLevel) {
        this.sraId = sraId;
        this.clientId = clientId;
        this.isRecovering = isRecovering;
        this.isActive = isActive;
        this.isTopLevel = isTopLevel;
    }

    public String getSraId() {
        return sraId;
    }

    public String getClientId() {
        return clientId;
    }

    public boolean isRecovering() {
        return isRecovering;
    }

    public boolean isActive() {
        return isActive;
    }

    public boolean isTopLevel() {
        return isTopLevel;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SRAInfo)) return false;

        SRAInfo sraStatus = (SRAInfo) o;

        return getSraId().equals(sraStatus.getSraId());
    }

    @Override
    public int hashCode() {
        return getSraId().hashCode();
    }
}
