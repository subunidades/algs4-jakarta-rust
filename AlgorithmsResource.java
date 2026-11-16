package com.example;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Arrays;

/** REST front for ALGS4Middleware.Middleware. engine=java runs in-process, engine=rust calls the Rust service. */
@Path("/algorithms")
public class AlgorithmsResource {

    /** GET /api/algorithms/sort?values=10,3,5,1&algo=quick|merge|heap&order=asc|desc&engine=java|rust */
    @GET
    @Path("/sort")
    @Produces(MediaType.APPLICATION_JSON)
    public Response sort(@QueryParam("values") String values,
                         @QueryParam("algo") @DefaultValue("quick") String algo,
                         @QueryParam("order") @DefaultValue("asc") String order,
                         @QueryParam("engine") @DefaultValue("java") String engine) {
        try {
            Integer[] arr = parse(values);
            ALGS4Middleware.Middleware.sort(arr,
                    ALGS4Middleware.Middleware.SortType.valueOf(algo.toUpperCase()),
                    "desc".equalsIgnoreCase(order),
                    ALGS4Middleware.Middleware.Engine.valueOf(engine.toUpperCase()));
            return Response.ok(Arrays.toString(arr)).build();
        } catch (IllegalArgumentException e) {
            return error(Response.Status.BAD_REQUEST, e);
        } catch (RuntimeException e) {
            return error(Response.Status.BAD_GATEWAY, e);
        }
    }

    /** GET /api/algorithms/select?values=10,3,5,1,20,8&k=2&engine=java|rust   (k is 0-based) */
    @GET
    @Path("/select")
    @Produces(MediaType.APPLICATION_JSON)
    public Response select(@QueryParam("values") String values,
                           @QueryParam("k") int k,
                           @QueryParam("engine") @DefaultValue("java") String engine) {
        try {
            Integer kth = ALGS4Middleware.Middleware.select(parse(values), k,
                    ALGS4Middleware.Middleware.Engine.valueOf(engine.toUpperCase()));
            return Response.ok(String.valueOf(kth)).build();
        } catch (IllegalArgumentException e) {
            return error(Response.Status.BAD_REQUEST, e);
        } catch (RuntimeException e) {
            return error(Response.Status.BAD_GATEWAY, e);
        }
    }

    private static Integer[] parse(String values) {
        if (values == null || values.isBlank())
            throw new IllegalArgumentException("values is required, e.g. values=10,3,5");
        return Arrays.stream(values.split(","))
                     .map(s -> Integer.valueOf(s.trim()))
                     .toArray(Integer[]::new);
    }

    private static Response error(Response.Status status, RuntimeException e) {
        return Response.status(status).type(MediaType.TEXT_PLAIN).entity("Error: " + e.getMessage()).build();
    }
}
