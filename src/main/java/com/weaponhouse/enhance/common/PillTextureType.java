package com.weaponhouse.enhance.common;

public enum PillTextureType {
    DEFAULT("enhance_pill"),
    ATTACK("enhance_pill_attack"),
    LIFE("enhance_pill_life"),
    DEFENSE("enhance_pill_defense"),
    SPEED("enhance_pill_speed"),
    HARMONY("enhance_pill_harmony");
    private final String textureName;
    PillTextureType(String textureName) {
        this.textureName = textureName;
    }
}