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
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
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

      resource.setStatus(new EnergySavingAutomationStatus(this.statusOfLights(lights), Boolean.TRUE, 0));

      var numberMinutesBeforeNextReconciliation =this.computeNumberMinutesBeforeNextReconciliation(resource);
      LOGGER.info("We are outside the configured interval 🌒- The next reconciliation will be in {} Minutes", numberMinutesBeforeNextReconciliation);
      return UpdateControl.updateStatus(resource)
              .rescheduleAfter(numberMinutesBeforeNextReconciliation, TimeUnit.MINUTES);
    }

    LOGGER.info("We are inside the configured interval 🌝");

    var allLightsOn = this.allLightsOn(lights);

    if (allLightsOn.isEmpty()) {

      this.processAllLightsOff(resource, lights);

    } else {

      this.processAtLeastOneLightsOn(resource, lights);
    }

    return UpdateControl.updateResourceAndStatus(resource)
            .rescheduleAfter(5, TimeUnit.MINUTES);
  }

  private long computeNumberMinutesBeforeNextReconciliation(EnergySavingAutomation resource) {
    var currentDateTime = LocalDateTime.now(Clock.systemDefaultZone());

    if (currentDateTime.getHour() < resource.getSpec().startTimeHours()) {
      return ChronoUnit.MINUTES.
              between(currentDateTime,
                      currentDateTime.with(LocalTime.of(resource.getSpec().startTimeHours(), 0, 0))
              );
    }

    var dateTimeOfStartTimeHoursNextDay = currentDateTime
            .with(LocalTime.MIDNIGHT)
            .plusDays(1)
            .plusHours(resource.getSpec().startTimeHours());

    return ChronoUnit.MINUTES.between(currentDateTime, dateTimeOfStartTimeHoursNextDay);
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

    resource.setStatus(new EnergySavingAutomationStatus(this.setAllLightsToStateOff(lights), Boolean.FALSE, 0));

  }

  private void processAtLeastOneLightsOn(EnergySavingAutomation resource, List<Light> lights) {

    var allLightsOn = this.allLightsOn(lights);

    LOGGER.info("We are inside the configured interval 🌝 - switchOff allLightsOn - {}", allLightsOn);

    this.switchOffAllLightsOn(allLightsOn);

    if (Objects.isNull(resource.getStatus())) {

      resource.setStatus(new EnergySavingAutomationStatus(this.setAllLightsToStateOff(lights), Boolean.FALSE, 1));

    } else {

      var status = resource.getStatus();
      resource.setStatus(new EnergySavingAutomationStatus(this.setAllLightsToStateOff(lights), Boolean.FALSE, status.getNumberOfInterventions() + 1));

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

