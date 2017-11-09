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

import org.eclipse.microprofile.sra.client.txstatusext.TransactionManagerElement;
import org.eclipse.microprofile.sra.client.txstatusext.TransactionStatisticsElement;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.TimeUnit;

public interface SRAClientAPI {
    /**
     * List of all transaction URIs known to the coordinator (active and in recovery)
     */
    public List<SRAInfo> getAllTransactions();

    /**
     * Return extended information about the transaction-manager resource such as how long it has
     * been up and all transaction-coordinator URIs.
     */
    public TransactionManagerElement getTransactionManagerInfo();

    /**
     * Return statistics for the transaction manager:
     * number of active, prepared, committed, and aborted transactions are returned.
     *
     * @return TransactionStatisticsElement
     */
    public TransactionStatisticsElement getTransactionStatistics();

    /**
     * Obtain the transaction terminator and participant enlistment URIs for the
     * specified transaction id.
     *
     * @param id URL template parameter for the transaction id
     * @return
     */
    public SRAInfo getTransactionURIs(String id);

    /**
     * Get the current status for a transaction
     * @param id the id of the transaction
     * @return content representing the status of the transaction
     */
    public String getTransactionStatus(String id);

    /**
     * Performing a GET on the transaction URL with media type application/txstatusext+xml
     * returns extended information about the transaction, such as its status,
     * number of participants, and their individual URIs.
     *
     * @param id the id of the transaction
     * @return HTTP response representing extended transaction status information
     */
    public Response getTransactionExtStatus(String id);

    /**
     * Begin a new SRA.
     *
     * @param timeout the number of milliseconds after which the transaction is eligible for being timed out.
     * @return SRA info
     */
    public SRAInfo beginTransaction(Long timeout, TimeUnit unit) throws GenericSRAException;

    /**
     * Commit a SRA
     *
     * @param txId id of the SRA to commit
     * @return SRA status
     */
    public SRAInfo commitTransaction(String txId);

    /**
     * Cancel a SRA
     *
     * @param txId id of the SRA to cancel
     * @return SRA status
     */
    public SRAInfo cancelTransaction(String txId);

    /**
     * Register a participant in a tx
     * @param linkHeader link header
     * @param txId id of transaction
     * @param content body of request containing URI for driving the participant through completion
     *  (the URI should be unique within the scope of txId)
     * @return unique resource ref for the participant
     */
    public Response enlistParticipant(String linkHeader, String txId, String content);

    /**
     * Register a volatile participant in a tx
     *
     * @param linkHeader link header
     * @param txId id of transaction
     * @return HTTP status code
     */
    public Response enlistVolatileParticipant(String linkHeader, String txId);

    /**
     * Get the participant url (registered during enlistParticipant) corresponding to a resource reference
     * if the coordinator crashes - the participant list will be empty but this is ok if commit hasn't been
     * called since the TM uses presumed abort semantics.
     *
     * @param txId transaction id that this recovery url belongs to
     * @param enlistmentId the resource reference
     * @return the participant url
     */
    public Response lookupParticipant(String txId, String enlistmentId);

    /**
     * PUT /recovery-coordinator/_RecCoordId_/_new participant URL_ -
     *   overwrite the old participant URL with new participant URL
     *   (as with JTS, this will also trigger off a recovery attempt on the associated transaction)
     * A participant may use this url to notifiy the coordinator that he has moved to a new location.
     *
     * @param linkHeader link header containing participant links
     * @param txId transaction id that this recovery url belongs to
     * @param enlistmentId id by the participant is known
     * @return http status code
     */
    public Response replaceParticipant(String linkHeader, String txId, String enlistmentId);

    public Response postParticipant(String enlistmentId);

    /**
     * Performing DELETE on participant's recovery URL removes participant from the transaction.
     *
     * @param enlistmentId The resource reference
     * @return HTTP status code
     */
    public Response deleteParticipant(String enlistmentId);

}

