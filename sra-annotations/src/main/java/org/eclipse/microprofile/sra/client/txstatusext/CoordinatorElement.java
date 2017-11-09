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
/*
 * JBoss, Home of Professional Open Source
 * Copyright 2012, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.eclipse.microprofile.sra.client.txstatusext;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@XmlRootElement(name = "coordinator")
@XmlType(propOrder = { "status", "created", "timeout", "txnURI", "terminatorURI",
        "durableParticipantEnlistmentURI", "volatileParticipantEnlistmentURI", "twoPhaseAware", "twoPhaseUnaware",
        "volatileParticipants"})
public class CoordinatorElement {
    private TransactionStatusElement status;
    private Date created;
    private long timeout;
    private String txnURI;
    private String terminatorURI;
    private String durableParticipantEnlistmentURI;
    private String volatileParticipantEnlistmentURI;
    private List<TwoPhaseAwareParticipantElement> twoPhaseAware = new ArrayList<TwoPhaseAwareParticipantElement>();
    private List<TwoPhaseUnawareParticipantElement> twoPhaseUnaware = new ArrayList<TwoPhaseUnawareParticipantElement>();
    private List<String> volatileParticipants = new ArrayList<String>();

    @XmlElement
    public Date getCreated() {
        return new Date(created.getTime());
    }
    @XmlElement
    public long getTimeout() {
        return timeout;
    }
    @XmlElement
    public String getTxnURI() {
        return txnURI;
    }
    @XmlElement
    public TransactionStatusElement getStatus() {
        return status;
    }
    @XmlElement
    public String getTerminatorURI() {
        return terminatorURI;
    }
    @XmlElement
    public String getDurableParticipantEnlistmentURI() {
        return durableParticipantEnlistmentURI;
    }
    @XmlElement
    public String getVolatileParticipantEnlistmentURI() {
        return volatileParticipantEnlistmentURI;
    }
    @XmlElement
    public List<TwoPhaseAwareParticipantElement> getTwoPhaseAware() {
        return twoPhaseAware;
    }
    @XmlElement
    public List<TwoPhaseUnawareParticipantElement> getTwoPhaseUnaware() {
        return twoPhaseUnaware;
    }
    @XmlElement
    public List<String> getVolatileParticipants() {
        return volatileParticipants;
    }

    public void setCreated(Date created) {
        this.created = new Date(created.getTime());
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void setTxnURI(String txnURI) {
        this.txnURI = txnURI;
    }

    public void setStatus(TransactionStatusElement status) {
        this.status = status;
    }

    public void setTerminateURI(String terminatorURI) {
        this.terminatorURI = terminatorURI;
    }

    public void setDurableParticipantEnlistmentURI(String durableParticipantEnlistmentURI) {
        this.durableParticipantEnlistmentURI = durableParticipantEnlistmentURI;
    }

    public void setVolatileParticipantEnlistmentURI(String volatileParticipantEnlistmentURI) {
        this.volatileParticipantEnlistmentURI = volatileParticipantEnlistmentURI;
    }

    public void addTwoPhaseAware(TwoPhaseAwareParticipantElement participantElement) {
        twoPhaseAware.add(participantElement);
    }

    public void addTwoPhaseUnaware(TwoPhaseUnawareParticipantElement participantElement) {
        twoPhaseUnaware.add(participantElement);
    }

    public void addVolatileParticipant(String volatileParticipant) {
        volatileParticipants.add(volatileParticipant);
    }
}
