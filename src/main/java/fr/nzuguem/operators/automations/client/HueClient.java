package fr.nzuguem.operators.automations.client;

import com.fasterxml.jackson.databind.JsonNode;
import fr.nzuguem.operators.automations.client.models.Light;
import fr.nzuguem.operators.automations.client.models.LightState;
import fr.nzuguem.operators.automations.client.models.SwitchCommand;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;


@Path("/api")
@Produces(MediaType.APPLICATION_JSON)
@RegisterRestClient(configKey = "hue-api")
public interface HueClient {

    Logger LOGGER = LoggerFactory.getLogger(HueClient.class);

    @GET
    @Path("{token}/lights")
    JsonNode getLights(@PathParam("token") String token);

    default List<Light> getLights() {
        var token = ConfigProvider.getConfig().getValue("application.hue-api.token", String.class);
        var jsonResponse = this.getLights(token);

        if (jsonResponse.toString().contains("error")) {
            LOGGER.error("⚠️ - {}", jsonResponse);
            throw new RuntimeException(jsonResponse.toString());
        }

        Iterable<Map.Entry<String, JsonNode>> iterable = jsonResponse::fields;
        return StreamSupport.stream(iterable.spliterator(), false)
                .map(entry -> {
                    var id = entry.getKey();
                    var state = entry.getValue().get("state");
                    var on = state.get("on").asBoolean();
                    return new Light(id, LightState.fromBoolean(on));
                })
                .toList();
    }

    @PUT
    @Path("{token}/lights/{id}/state")
    JsonNode switchState(@PathParam("token") String token, @PathParam("id") String lightId, SwitchCommand switchCommand);

    default void switchOff(String lightId) {
        var token = ConfigProvider.getConfig().getValue("application.hue-api.token", String.class);

        var jsonResponse = this.switchState(token, lightId, SwitchCommand.off());

        if (jsonResponse.toString().contains("error")) {
            LOGGER.error("⚠️ - {}", jsonResponse);
            throw new RuntimeException(jsonResponse.toString());
        }
    }
}
