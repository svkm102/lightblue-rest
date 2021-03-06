/*
 Copyright 2013 Red Hat, Inc. and/or its affiliates.

 This file is part of lightblue.

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.redhat.lightblue.rest.metadata;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.redhat.lightblue.metadata.Metadata;
import com.redhat.lightblue.metadata.MetadataConstants;
import com.redhat.lightblue.metadata.MetadataRole;
import com.redhat.lightblue.query.QueryExpression;
import com.redhat.lightblue.rest.RestConfiguration;
import com.redhat.lightblue.rest.metadata.cmd.CreateEntityMetadataCommand;
import com.redhat.lightblue.rest.metadata.cmd.CreateEntitySchemaCommand;
import com.redhat.lightblue.rest.metadata.cmd.GetDependenciesCommand;
import com.redhat.lightblue.rest.metadata.cmd.GetEntityMetadataCommand;
import com.redhat.lightblue.rest.metadata.cmd.GetEntityNamesCommand;
import com.redhat.lightblue.rest.metadata.cmd.GetEntityRolesCommand;
import com.redhat.lightblue.rest.metadata.cmd.DiffCommand;
import com.redhat.lightblue.rest.metadata.cmd.GetEntityVersionsCommand;
import com.redhat.lightblue.rest.metadata.cmd.ReIndexCommand;
import com.redhat.lightblue.rest.metadata.cmd.RemoveEntityCommand;
import com.redhat.lightblue.rest.metadata.cmd.SetDefaultVersionCommand;
import com.redhat.lightblue.rest.metadata.cmd.UpdateEntityInfoCommand;
import com.redhat.lightblue.rest.metadata.cmd.UpdateEntitySchemaStatusCommand;
import com.redhat.lightblue.rest.util.QueryTemplateUtils;
import com.redhat.lightblue.util.Error;
import com.redhat.lightblue.util.JsonUtils;
import com.restcompress.provider.LZF;

/**
 * @author nmalik
 * @author bserdar
 */
@Produces(MediaType.APPLICATION_JSON)
public abstract class AbstractMetadataResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMetadataResource.class);

    private static final String PARAM_ENTITY = "entity";
    private static final String PARAM_VERSION = "version";

    private static final Metadata metadata;

    static {
        try {
            metadata = RestConfiguration.getFactory().getMetadata();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw Error.get(RestMetadataConstants.ERR_CANT_GET_METADATA, e.getMessage());
        }

        // by default JVM caches DNS forever.  hard code an override to refresh DNS cache every 30 seconds
        java.security.Security.setProperty("networkaddress.cache.ttl", "30");
    }

    @GET
    @LZF
    @Path("/dependencies")
    public String getDepGraph(@Context SecurityContext sc) {
        return getDepGraph(sc, null, null);
    }

    @GET
    @LZF
    @Path("/{entity}/dependencies")
    public String getDepGraph(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity) {
        return getDepGraph(sc, entity, null);
    }

    @GET
    @LZF
    @Path("/{entity}/{version}/dependencies")
    public String getDepGraph(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity, @PathParam(PARAM_VERSION) String version) {
        Error.reset();

        checkPermission(sc, MetadataRole.FIND_DEPENDENCIES);
        return new GetDependenciesCommand(entity, version).run();
    }

    @GET
    @LZF
    @Path("/{entity}/{version}/diff/{version2}")
    public String getDiff(@Context SecurityContext sc,
                          @PathParam(PARAM_ENTITY) String entity,
                          @PathParam(PARAM_VERSION) String version1,
                          @PathParam("version2") String version2) {
        Error.reset();
        checkPermission(sc, MetadataRole.FIND_ENTITY_METADATA);
        return new DiffCommand(entity,version1,version2).run();

    }

    @GET
    @LZF
    @Path("/roles")
    public String getEntityRoles(@Context SecurityContext sc) {
        return getEntityRoles(sc, null, null);
    }

    @GET
    @LZF
    @Path("/{entity}/roles")
    public String getEntityRoles(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity) {
        return getEntityRoles(sc, entity, null);
    }

    @GET
    @LZF
    @Path("/{entity}/{version}/roles")
    public String getEntityRoles(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity, @PathParam(PARAM_VERSION) String version) {
        Error.reset();

        checkPermission(sc, MetadataRole.FIND_ROLES);
        return new GetEntityRolesCommand(entity, version).run();
    }

    @GET
    @LZF
    @Path("/")
    public String getEntityNames(@Context SecurityContext sc) {
        Error.reset();

        checkPermission(sc, MetadataRole.FIND_ENTITY_NAMES);
        return new GetEntityNamesCommand(new String[0]).run();
    }

    @GET
    @LZF
    @Path("/s={statuses}")
    public String getEntityNames(@Context SecurityContext sc, @PathParam("statuses") String statuses) {
        StringTokenizer tok = new StringTokenizer(" ,;:.");
        String[] s = new String[tok.countTokens()];
        int i = 0;
        while (tok.hasMoreTokens()) {
            s[i++] = tok.nextToken();
        }
        Error.reset();

        checkPermission(sc, MetadataRole.FIND_ENTITY_NAMES);
        return new GetEntityNamesCommand(s).run();
    }

    @GET
    @LZF
    @Path("/{entity}")
    public String getEntityVersions(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity) {
        Error.reset();

        checkPermission(sc, MetadataRole.FIND_ENTITY_VERSIONS);
        return new GetEntityVersionsCommand(entity).run();
    }

    @GET
    @LZF
    @Path("/{entity}/{version}")
    public String getMetadata(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity, @PathParam(PARAM_VERSION) String version) {
        Error.reset();

        checkPermission(sc, MetadataRole.FIND_ENTITY_METADATA);
        return new GetEntityMetadataCommand(entity, version).run();
    }

    @PUT
    @LZF
    @Path("/{entity}/{version}")
    @Consumes(MediaType.APPLICATION_JSON)
    public String createMetadata(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity, @PathParam(PARAM_VERSION) String version, String data) {
        Error.reset();

        checkPermission(sc, MetadataRole.INSERT);
        return new CreateEntityMetadataCommand(entity, version, data).run();
    }

    @PUT
    @LZF
    @Path("/{entity}/schema={version}")
    @Consumes(MediaType.APPLICATION_JSON)
    public String createSchema(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity, @PathParam(PARAM_VERSION) String version, String schema) {
        Error.reset();

        checkPermission(sc, MetadataRole.INSERT_SCHEMA);
        return new CreateEntitySchemaCommand(entity, version, schema).run();
    }

    @PUT
    @LZF
    @Path("/{entity}")
    @Consumes(MediaType.APPLICATION_JSON)
    public String updateEntityInfo(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity, String info) {
        Error.reset();

        checkPermission(sc, MetadataRole.UPDATE_ENTITYINFO);
        return new UpdateEntityInfoCommand(entity, info).run();
    }

    @PUT
    @LZF
    @Path("/{entity}/{version}/{status}")
    public String updateSchemaStatus(@Context SecurityContext sc,
                                     @PathParam(PARAM_ENTITY) String entity,
                                     @PathParam(PARAM_VERSION) String version,
                                     @PathParam("status") String status,
                                     @QueryParam("comment") String comment) {
        Error.reset();

        checkPermission(sc, MetadataRole.UPDATE_ENTITY_SCHEMASTATUS);
        return new UpdateEntitySchemaStatusCommand(entity, version, status, comment).run();
    }

    @POST
    @LZF
    @Path("/{entity}/{version}/default")
    public String setDefaultVersion(@Context SecurityContext sc,
                                    @PathParam(PARAM_ENTITY) String entity,
                                    @PathParam(PARAM_VERSION) String version) {
        Error.reset();

        checkPermission(sc, MetadataRole.UPDATE_DEFAULTVERSION);
        return new SetDefaultVersionCommand(entity, version).run();
    }

    @DELETE
    @LZF
    @Path("/{entity}")
    public String removeEntity(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity) {
        Error.reset();

        checkPermission(sc, MetadataRole.DELETE_ENTITY);
        return new RemoveEntityCommand(entity).run();
    }

    @DELETE
    @LZF
    @Path("/{entity}/default")
    public String clearDefaultVersion(@Context SecurityContext sc, @PathParam(PARAM_ENTITY) String entity) {
        Error.reset();

        checkPermission(sc, MetadataRole.UPDATE_DEFAULTVERSION);
        return new SetDefaultVersionCommand(entity, null).run();
    }

    private void checkPermission(SecurityContext sc, MetadataRole roleAllowed) {
        final Map<MetadataRole, List<String>> mappedRoles = metadata.getMappedRoles();
        if (mappedRoles == null || mappedRoles.size() == 0) {
            // No authorization was configured
            return;
        }

        List<String> roles = mappedRoles.get(roleAllowed);

        if (roles.contains(MetadataConstants.ROLE_NOONE)) {
            throw new SecurityException("Unauthorized Request");
        } else if (roles.contains(MetadataConstants.ROLE_ANYONE)) {
            return;
        }

        for (String role : roles) {
            if (sc.isUserInRole(role)) {
                return;
            }
        }

        throw new SecurityException("Unauthorized Request. One of the following roles is required: " + roles);
    }

    @POST
    @LZF
    @Path("/{entity}/reindex")
    public String reindex(@PathParam(PARAM_ENTITY) String entity) throws IOException {
        return new ReIndexCommand(null, metadata, entity).run();
    }

    @POST
    @LZF
    @Path("/{entity}/{version}/reindex")
    public String reindex(@PathParam(PARAM_ENTITY) String entity, @PathParam(PARAM_VERSION) String version, @QueryParam("Q") String query) throws IOException {
        String sq = QueryTemplateUtils.buildQueryFieldsTemplate(query);
        QueryExpression qe = QueryExpression.fromJson(JsonUtils.json(sq));
        return new ReIndexCommand(null, metadata, entity, version, qe).run();
    }
}
