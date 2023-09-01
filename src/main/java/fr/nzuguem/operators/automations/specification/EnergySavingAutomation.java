package fr.nzuguem.operators.automations.specification;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Plural;
import io.fabric8.kubernetes.model.annotation.ShortNames;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1")
@Group("automations.nzuguem.fr")
@ShortNames("esa")
@Plural("esas")
public class EnergySavingAutomation extends CustomResource<EnergySavingAutomationSpec, EnergySavingAutomationStatus> implements Namespaced {}

