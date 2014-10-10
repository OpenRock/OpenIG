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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openig.handler;

import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.builder.verify.VerifyHttp.verifyHttp;
import static com.xebialabs.restito.semantics.Action.status;
import static com.xebialabs.restito.semantics.Condition.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.forgerock.http.Request;
import org.forgerock.openig.http.Exchange;
import org.forgerock.openig.io.TemporaryStorage;
import org.forgerock.openig.util.HttpClient;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.testng.annotations.Test;

import com.xebialabs.restito.server.StubServer;

@SuppressWarnings("javadoc")
public class ClientHandlerTest {
    @Test(description = "test for OPENIG-315")
    public void checkRequestIsForwardedUnaltered() throws Exception {
        final StubServer server = new StubServer().run();
        final int port = server.getPort();
        whenHttp(server).match(alwaysTrue()).then(status(HttpStatus.OK_200));
        try {
            final Exchange exchange = new Exchange();
            exchange.request = new Request();
            exchange.request.setMethod("POST");
            exchange.request.setUri("http://0.0.0.0:" + port + "/example");

            final Map<String, Object> json = new LinkedHashMap<String, Object>();
            json.put("k1", "v1");
            json.put("k2", "v2");
            exchange.request.setEntity(json);

            final ClientHandler handler = new ClientHandler(new HttpClient(new TemporaryStorage()));
            handler.handle(exchange);

            assertThat(exchange.response.getStatus()).isEqualTo(200);
            verifyHttp(server).once(method(Method.POST), uri("/example"),
                    withPostBodyContaining("{\"k1\":\"v1\",\"k2\":\"v2\"}"));
        } finally {
            server.stop();
        }
    }
}