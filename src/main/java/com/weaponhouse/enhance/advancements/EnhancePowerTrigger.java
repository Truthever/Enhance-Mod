package com.weaponhouse.enhance.advancements;

import com.google.gson.JsonObject;
import net.minecraft.advancements.criterion.AbstractCriterionTrigger;
import net.minecraft.advancements.criterion.CriterionInstance;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.loot.ConditionArrayParser;
import net.minecraft.loot.ConditionArraySerializer;
import net.minecraft.util.ResourceLocation;
public class EnhancePowerTrigger extends AbstractCriterionTrigger<EnhancePowerTrigger.Instance> {
    private static final ResourceLocation ID = new ResourceLocation("enhance", "enhance_power");
    @Override
    public ResourceLocation getId() {
        return ID;
    }
    @Override
    public Instance deserializeTrigger(JsonObject json, EntityPredicate.AndPredicate entityPredicate, ConditionArrayParser conditionsParser) {
        return new Instance(entityPredicate);
    }
    public void trigger(ServerPlayerEntity player) {
        this.triggerListeners(player, instance -> true);
    }
    public static class Instance extends CriterionInstance {
        public Instance(EntityPredicate.AndPredicate player) {
            super(EnhancePowerTrigger.ID, player);
        }
        @Override
        public JsonObject serialize(ConditionArraySerializer conditions) {
            return super.serialize(conditions);
        }
    }
}