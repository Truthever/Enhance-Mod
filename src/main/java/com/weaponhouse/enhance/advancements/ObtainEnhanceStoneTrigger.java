package com.weaponhouse.enhance.advancements;

import com.google.gson.JsonObject;
import net.minecraft.advancements.criterion.AbstractCriterionTrigger;
import net.minecraft.advancements.criterion.CriterionInstance;
import net.minecraft.advancements.criterion.EntityPredicate;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.loot.ConditionArrayParser;
import net.minecraft.loot.ConditionArraySerializer;
import net.minecraft.util.ResourceLocation;
public class ObtainEnhanceStoneTrigger extends AbstractCriterionTrigger<ObtainEnhanceStoneTrigger.Instance> {
    private static final ResourceLocation ID = new ResourceLocation("enhance", "obtain_enhance_stone");
    @Override
    public ResourceLocation getId() {
        return ID;
    }
    @Override
    public Instance deserializeTrigger(JsonObject json, EntityPredicate.AndPredicate entityPredicate, ConditionArrayParser conditionsParser) {
        return new Instance(entityPredicate);
    }
    public void trigger(ServerPlayerEntity player) {
        this.triggerListeners(player, Instance::test);
    }
    public static class Instance extends CriterionInstance {
        public Instance(EntityPredicate.AndPredicate player) {
            super(ObtainEnhanceStoneTrigger.ID, player);
        }
        @Override
        public JsonObject serialize(ConditionArraySerializer conditions) {
            return super.serialize(conditions);
        }
        public boolean test() {
            return true;
        }
    }
}