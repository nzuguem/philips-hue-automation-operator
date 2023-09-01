package fr.nzuguem.operators.automations.reconcilier;

import fr.nzuguem.operators.automations.client.HueClient;
import fr.nzuguem.operators.automations.client.models.Light;
import fr.nzuguem.operators.automations.client.models.LightState;
import fr.nzuguem.operators.automations.specification.EnergySavingAutomation;
import fr.nzuguem.operators.automations.specification.EnergySavingAutomationStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.*;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class EnergySavingAutomationReconciler implements Reconciler<EnergySavingAutomation>, Cleaner<EnergySavingAutomation> {

  private static final Logger LOGGER = LoggerFactory.getLogger(EnergySavingAutomationReconciler.class);

  private final KubernetesClient k8sClient;

  private HueClient hueClient;

  public EnergySavingAutomationReconciler(KubernetesClient k8sClient, @RestClient  HueClient hueClient) {
    this.k8sClient = k8sClient;
    this.hueClient = hueClient;
  }

  // Loop Reconciliation : Create -> Observes -> Take Action -> Analyses
  @Override
  public UpdateControl<EnergySavingAutomation> reconcile(EnergySavingAutomation resource, Context context) {

    var lights = this.hueClient.getLights();

    if (this.isOutOfInterval(resource)) {


      if (Objects.isNull(resource.getStatus())) {
        LOGGER.info("We are outside the configured interval 🌒");
        resource.setStatus(new EnergySavingAutomationStatus(this.statusOfLights(lights), Boolean.TRUE, 0));
        return UpdateControl.updateStatus(resource);
      }

      LOGGER.info("We are already outside the configured interval 🌒");
      return UpdateControl.noUpdate();
    }

    LOGGER.info("We are inside the configured interval 🌝");

    var allLightsOn = this.allLightsOn(lights);

    if (allLightsOn.isEmpty()) {

      this.processAllLightsOff(resource, lights);

    } else {

      this.processAtLeastOneLightsOn(resource, lights);
    }

    return UpdateControl.updateResourceAndStatus(resource)
            .rescheduleAfter(2, TimeUnit.MINUTES);
  }

  private boolean isOutOfInterval(EnergySavingAutomation resource) {
    var currentHour = LocalDateTime.now(Clock.systemDefaultZone()).getHour();
    return currentHour < resource.getSpec().startTimeHours() || currentHour >= resource.getSpec().endTimeHours();
  }

  private Map<String, LightState> statusOfLights(List<Light> lights) {

    return lights
            .stream()
            .collect(Collectors.toUnmodifiableMap(Light::id, Light::state));
  }

  private List<Light> allLightsOn(List<Light> lights) {

    return lights.stream()
            .filter(light -> Objects.equals(light.state(), LightState.ON))
            .toList();
  }

  private void processAllLightsOff(EnergySavingAutomation resource, List<Light> lights) {

    LOGGER.info("We are inside the configured interval 🌝 - no lights switched on");

    if (Objects.isNull(resource.getStatus())) {

      resource.setStatus(new EnergySavingAutomationStatus(this.setAllLightsToStateOff(lights), Boolean.FALSE, 0));

    } else {

      var status = resource.getStatus();
      status.setLightsState(this.setAllLightsToStateOff(lights));

    }
  }

  private void processAtLeastOneLightsOn(EnergySavingAutomation resource, List<Light> lights) {

    var allLightsOn = this.allLightsOn(lights);

    LOGGER.info("We are inside the configured interval 🌝 - switchOff allLightsOn - {}", allLightsOn);

    this.switchOffAllLightsOn(allLightsOn);

    if (Objects.isNull(resource.getStatus())) {

      resource.setStatus(new EnergySavingAutomationStatus(this.setAllLightsToStateOff(lights), Boolean.FALSE, 1));

    } else {

      var status = resource.getStatus();
      status.setLightsState(this.setAllLightsToStateOff(lights));
      status.setNumberOfInterventions(status.getNumberOfInterventions() + 1);

    }

    this.annotateWithLastIntervention(resource);
  }

  private Map<String, LightState> setAllLightsToStateOff(List<Light> lights) {
    return lights.stream()
            .collect(Collectors.toUnmodifiableMap(Light::id, light -> LightState.OFF));
  }

  private void switchOffAllLightsOn(List<Light> allLightsOn) {
    allLightsOn.forEach(light -> this.hueClient.switchOff(light.id()));
  }

  private void annotateWithLastIntervention(EnergySavingAutomation resource) {
    var currentDateTime = LocalDateTime.now(Clock.systemDefaultZone()).toString();
    resource.getMetadata().getAnnotations().put("automations.nzuguem.fr/last-intervention", currentDateTime);
  }


  // function called resource deletion. IT IS BLOCKING FOR KUBECTL
  @Override
  public DeleteControl cleanup(EnergySavingAutomation energySavingAutomation, Context<EnergySavingAutomation> context) {
    LOGGER.info("🔥 resource deletion - {}", energySavingAutomation.getMetadata().getName());
    return DeleteControl.defaultDelete();
  }
}

