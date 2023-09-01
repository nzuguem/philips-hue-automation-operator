package fr.nzuguem.operators.automations.client.models;

public enum LightState {
    ON, OFF;

    public static  LightState fromBoolean(boolean value) {
       return value ? ON : OFF;
    }
}
