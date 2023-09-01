package fr.nzuguem.operators.automations.specification;

import fr.nzuguem.operators.automations.client.models.LightState;

import java.util.Map;

public final class EnergySavingAutomationStatus {
    private  Map<String, LightState> lightsState;
    private  Boolean outOfInterval;
    private  int numberOfInterventions;

    public EnergySavingAutomationStatus(
            Map<String, LightState> lightsState,
            Boolean outOfInterval,
            int numberOfInterventions
    ) {
        this.lightsState = lightsState;
        this.outOfInterval = outOfInterval;
        this.numberOfInterventions = numberOfInterventions;
    }

    public EnergySavingAutomationStatus() {}

    public Map<String, LightState> getLightsState() {
        return lightsState;
    }

    public Boolean getOutOfInterval() {
        return outOfInterval;
    }

    public int getNumberOfInterventions() {
        return numberOfInterventions;
    }

    public void setLightsState(Map<String, LightState> lightsState) {
        this.lightsState = lightsState;
    }

    public void setOutOfInterval(Boolean outOfInterval) {
        this.outOfInterval = outOfInterval;
    }

    public void setNumberOfInterventions(int numberOfInterventions) {
        this.numberOfInterventions = numberOfInterventions;
    }
}
