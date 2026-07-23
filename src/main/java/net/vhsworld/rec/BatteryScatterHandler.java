package net.vhsworld.rec;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.util.RandomSource;
import net.vhsworld.rec.init.ModItems;

// Espalha pilhas pelo chão perto dos jogadores de tempos em tempos (achar "no chão").
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BatteryScatterHandler {

    private static final int INTERVAL_TICKS = 600;   // tenta a cada ~30s
    private static final double SPAWN_CHANCE = 0.30; // 30% por jogador em cada tentativa
    private static final int MIN_RADIUS = 6;
    private static final int MAX_RADIUS = 20;
    private static final int MAX_NEARBY = 3;         // não empilhar pilhas soltas na área

    private static int timer = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (++timer < INTERVAL_TICKS) return;
        timer = 0;

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (ServerLevel level : server.getAllLevels()) {
            RandomSource rand = level.getRandom();
            for (ServerPlayer player : level.players()) {
                if (rand.nextDouble() > SPAWN_CHANCE) continue;
                trySpawnNear(level, player, rand);
            }
        }
    }

    private static void trySpawnNear(ServerLevel level, ServerPlayer player, RandomSource rand) {
        BlockPos origin = player.blockPosition();

        // posição aleatória em anel ao redor do jogador
        double angle = rand.nextDouble() * Math.PI * 2.0;
        int dist = MIN_RADIUS + rand.nextInt(MAX_RADIUS - MIN_RADIUS + 1);
        int x = origin.getX() + (int) Math.round(Math.cos(angle) * dist);
        int z = origin.getZ() + (int) Math.round(Math.sin(angle) * dist);

        if (!level.isLoaded(new BlockPos(x, origin.getY(), z))) return;

        // procura um chão sólido perto da altura do jogador (funciona na superfície e em cavernas)
        BlockPos floor = findFloor(level, x, origin.getY(), z);
        if (floor == null) return;

        // evita acumular pilhas soltas na mesma região
        AABB box = new AABB(floor).inflate(MAX_RADIUS);
        long nearby = level.getEntitiesOfClass(ItemEntity.class, box,
                e -> e.getItem().is(ModItems.BATTERY.get())).size();
        if (nearby >= MAX_NEARBY) return;

        ItemEntity drop = new ItemEntity(level,
                floor.getX() + 0.5, floor.getY() + 0.1, floor.getZ() + 0.5,
                new ItemStack(ModItems.BATTERY.get()));
        drop.setDeltaMovement(0, 0, 0);
        drop.setPickUpDelay(20);
        level.addFreshEntity(drop);
    }

    // Varre para baixo a partir de player.Y+3 procurando bloco sólido com ar em cima.
    private static BlockPos findFloor(ServerLevel level, int x, int startY, int z) {
        for (int y = startY + 3; y >= startY - 6; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState ground = level.getBlockState(pos);
            BlockPos above = pos.above();
            BlockState air = level.getBlockState(above);

            boolean solidTop = ground.isFaceSturdy(level, pos, net.minecraft.core.Direction.UP);
            boolean openAbove = air.isAir() || air.getCollisionShape(level, above).isEmpty();
            boolean noLiquid = ground.getFluidState().isEmpty() && air.getFluidState().isEmpty();

            if (solidTop && openAbove && noLiquid) {
                return above;
            }
        }
        return null;
    }
}
