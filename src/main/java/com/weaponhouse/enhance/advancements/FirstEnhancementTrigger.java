package com.weaponhouse.enhance.advancements;

import com.google.gson.JsonObject;
import com.weaponhouse.enhance.Enhance;
import net.minecraft.advancements.criterion.AbstractCriterionTrigger;
import net.minecraft.advancements.criterion.CriterionInstance;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.loot.ConditionArrayParser;
import net.minecraft.loot.ConditionArraySerializer;
import net.minecraft.util.ResourceLocation;
public class FirstEnhancementTrigger extends AbstractCriterionTrigger<FirstEnhancementTrigger.Instance> {
    public static final ResourceLocation ID = new ResourceLocation(Enhance.MOD_ID, "first_enhancement");
    public FirstEnhancementTrigger() {}
    @Override
    public ResourceLocation getId() {
        return ID;
    }
    @Override
    public Instance deserializeTrigger(JsonObject json, EntityPredicate.AndPredicate entityPredicate, ConditionArrayParser conditionsParser) {
        return new Instance(entityPredicate);
    }
    public void trigger(ServerPlayerEntity player, String buffType) {
        if (!"enhance_level".equals(buffType)) {
            this.triggerListeners(player, instance -> true);
        }
    }
    public static class Instance extends CriterionInstance {
        public Instance(EntityPredicate.AndPredicate player) {
            super(FirstEnhancementTrigger.ID, player);
        }
        @Override
        public JsonObject serialize(ConditionArraySerializer conditions) {
            return super.serialize(conditions);
        }
    }
}