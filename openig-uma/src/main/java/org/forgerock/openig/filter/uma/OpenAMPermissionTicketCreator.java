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
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openig.filter.uma;

import static java.lang.String.format;
import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;
import static org.forgerock.util.Utils.closeSilently;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.openig.filter.oauth2.OAuth2TokenException;
import org.forgerock.openig.handler.Handler;
import org.forgerock.openig.handler.HandlerException;
import org.forgerock.openig.http.Entity;
import org.forgerock.openig.http.Exchange;
import org.forgerock.openig.http.Request;
import org.forgerock.openig.http.Response;

public class OpenAMPermissionTicketCreator implements PermissionTicketCreator {

    private final Handler client;
    private final String permissionRequestEndpoint;

    //For Demo
    private final DemoPatGetter patGetter;

    //For Test
    private final Timer timer;

    public OpenAMPermissionTicketCreator(Handler client, String permissionRequestEndpoint,
            DemoPatGetter patGetter, Timer timer) {
        this.client = client;
        this.permissionRequestEndpoint = permissionRequestEndpoint;
        this.patGetter = patGetter;
        this.timer = timer;
    }

    @Override
    public String create(String resourceSetId, Collection<String> requiredScopes) throws OAuth2TokenException {
        try {
            Exchange exchange = new Exchange();
            exchange.request = new Request();
            exchange.request.setMethod("POST");
            exchange.request.setUri(new URI(permissionRequestEndpoint));
            exchange.request.setEntity(json(object(
                    field("resource_set_id", resourceSetId),
                    field("scopes", requiredScopes))).asMap());

            timer.start("PermissionTicketCreator: get PAT");
            exchange.request.getHeaders().add("Authorization", "Bearer " + patGetter.getPAT());
            timer.stop("PermissionTicketCreator: get PAT");

            timer.start("PermissionTicketCreator: get permission ticket");
            client.handle(exchange);
            timer.stop("PermissionTicketCreator: get permission ticket");

            if (isResponseEmpty(exchange)) {
                throw new OAuth2TokenException("Authorization Server did not return any AccessToken");
            }

            JsonValue content = asJson(exchange.response.getEntity());
            if (isCreated(exchange.response)) {
                return content.get("ticket").asString();
            }

            if (content.isDefined("error")) {
                String error = content.get("error").asString();
                String description = content.get("error_description").asString();
                throw new OAuth2TokenException(format("Authorization Server returned an error "
                                + "(error: %s, description: %s)", error, description));
            }

            return null;

        } catch (URISyntaxException e) {
            throw new OAuth2TokenException(
                    format("The permission_request endpoint %s could not be accessed because it is a malformed URI",
                            permissionRequestEndpoint), e);
        } catch (IOException e) {
            throw new OAuth2TokenException(format("Cannot create Permission Ticket from %s",
                    permissionRequestEndpoint), e);
        } catch (HandlerException e) {
            throw new OAuth2TokenException(format("Could not handle call to permission_request endpoint %s",
                    permissionRequestEndpoint), e);
        }
    }

    private boolean isResponseEmpty(final Exchange exchange) {
        return (exchange.response == null) || (exchange.response.getEntity() == null);
    }

    private boolean isCreated(final Response response) {
        return response.getStatus() == 201;
    }

    /**
     * Parse the response's content as a JSON structure.
     * @param entity stream response's content
     * @return {@link JsonValue} representing the JSON content
     * @throws OAuth2TokenException if there was some errors during parsing
     */
    private JsonValue asJson(final Entity entity) throws OAuth2TokenException {
        try {
            return new JsonValue(entity.getJson());
        } catch (IOException e) {
            // Do not use Entity.toString(), we probably don't want to fully output the content here
            throw new OAuth2TokenException("Cannot read response content as JSON", e);
        } finally {
            closeSilently(entity);
        }
    }
}
