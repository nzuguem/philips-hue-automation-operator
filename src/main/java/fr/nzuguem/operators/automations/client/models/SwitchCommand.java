package fr.nzuguem.operators.automations.client.models;

public record SwitchCommand(
        boolean on
) {

    public static SwitchCommand off() {
        return new SwitchCommand(false);
    }

}
