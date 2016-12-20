/*
 *  Copyright 2006-2016 WebPKI.org (http://webpki.org).
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.webpki.keygen2;

import java.io.IOException;

import java.util.Date;

import org.webpki.sks.SecureKeyStore;

import org.webpki.util.Base64URL;

import org.webpki.crypto.KeyContainerTypes;

import org.webpki.json.JSONObjectWriter;

import org.webpki.keygen2.ServerState.ProtocolPhase;

import static org.webpki.keygen2.KeyGen2Constants.*;

public class InvocationRequestEncoder extends ServerEncoder {

    private static final long serialVersionUID = 1L;

    Action action = Action.MANAGE;

    String serverSessionId;

    String submitUrl;

    String abortUrl; // Optional

    private ServerState serverState;

    // Constructors

    public InvocationRequestEncoder(ServerState serverState,
                                    String submitUrl,
                                    String serverSessionId) throws IOException {
        serverState.checkState(true, ProtocolPhase.INVOCATION);
        this.serverState = serverState;
        this.submitUrl = submitUrl;
        if (serverSessionId == null) {
            serverSessionId = Long.toHexString(new Date().getTime());
            serverSessionId += Base64URL.generateURLFriendlyRandom(SecureKeyStore.MAX_LENGTH_ID_TYPE - serverSessionId.length());
        }
        this.serverSessionId = serverState.serverSessionId = serverSessionId;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public void setAbortUrl(String abortUrl) {
        this.abortUrl = abortUrl;
    }


    @Override
    void writeServerRequest(JSONObjectWriter wr) throws IOException {
        //////////////////////////////////////////////////////////////////////////
        // Session properties
        //////////////////////////////////////////////////////////////////////////
        wr.setString(SERVER_SESSION_ID_JSON, serverSessionId);

        wr.setString(SUBMIT_URL_JSON, submitUrl);

        wr.setString(ACTION_JSON, action.getJSONName());

        setOptionalString(wr, ABORT_URL_JSON, abortUrl);

        if (serverState.privacyEnabledSet) {
            wr.setBoolean(PRIVACY_ENABLED_JSON, serverState.privacyEnabled);
        }

        setOptionalStringArray(wr, PREFERREDD_LANGUAGES_JSON, serverState.languageList);

        setOptionalStringArray(wr, KeyContainerTypes.KCT_TARGET_KEY_CONTAINERS, serverState.keyContainerList);

        setOptionalStringArray(wr,
                               CLIENT_CAPABILITY_QUERY_JSON,
                               serverState.queriedCapabilities.isEmpty() ?
                                   null : serverState.queriedCapabilities.keySet().toArray(new String[0]));
    }

    @Override
    public String getQualifier() {
        return KeyGen2Messages.INVOCATION_REQUEST.getName();
    }
}
