package com.hz.hatmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class HatMod implements ModInitializer {
    public static final String MOD_ID = "hat-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // 存储等待执行hat操作的玩家
    private static final Map<UUID, Boolean> playersToHat = new HashMap<>();

    @Override
    public void onInitialize() {
        LOGGER.info("Hat Mod initialized!");

        // 注册/hat命令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(net.minecraft.server.command.CommandManager.literal("hat")
                    .executes(context -> {
                        ServerPlayerEntity player = context.getSource().getPlayer();
                        if (player != null) {
                            playersToHat.put(player.getUuid(), true);
                            context.getSource().sendFeedback(() -> Text.literal("将手中的物品戴到头上!"), false);
                        }
                        return 1;
                    })
            );
        });

        // 在服务器每刻更新时处理hat操作
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (UUID playerId : playersToHat.keySet()) {
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
                if (player != null) {
                    performHatOperation(player);
                }
            }
            playersToHat.clear();
        });
    }

    private void performHatOperation(ServerPlayerEntity player) {
        ItemStack handStack = player.getMainHandStack();

        if (handStack.isEmpty()) {
            player.sendMessage(Text.literal("手中没有物品!"), false);
            return;
        }

        // 交换手中物品和头盔物品
        ItemStack helmetStack = player.getEquippedStack(EquipmentSlot.HEAD);
        player.equipStack(EquipmentSlot.HEAD, handStack.copy());
        player.getInventory().setStack(player.getInventory().selectedSlot, helmetStack);

        player.sendMessage(Text.literal("已成功将物品戴到头上!"), false);
    }
}