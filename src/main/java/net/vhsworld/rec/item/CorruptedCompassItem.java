package net.vhsworld.rec.item;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A bussola corrompida.
 *
 * Ela nao gira: escreve. Apertar o botao direito diz, em texto, a distancia e o
 * rumo ate o ponto de spawn — que combina muito mais com um mod de camera e
 * leitura de tela do que uma agulha girando, e ainda evita depender do sistema
 * de modelo animado do vanilla.
 *
 * E tambem a peca base: a Ancora nasce dela, e o Localizador de Estruturas vai
 * nascer dela tambem.
 */
public class CorruptedCompassItem extends Item {

    private static final String[] RHUMBS = {
            "N", "NE", "E", "SE", "S", "SW", "W", "NW"
    };

    public CorruptedCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide && player instanceof ServerPlayer server) {
            BlockPos target = spawnOf(server);
            double dx = target.getX() - player.getX();
            double dz = target.getZ() - player.getZ();
            int distance = (int) Math.round(Math.sqrt(dx * dx + dz * dz));

            player.displayClientMessage(Component.translatable(
                    "recmod.compass.reading", rhumb(dx, dz), distance), true);

            level.playSound(null, player.blockPosition(), SoundEvents.ITEM_FRAME_ROTATE_ITEM,
                    SoundSource.PLAYERS, 0.7F, 0.6F);
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    private static BlockPos spawnOf(ServerPlayer player) {
        BlockPos bed = player.getRespawnPosition();
        if (bed != null) return bed;

        ServerLevel overworld = player.server.overworld();
        return overworld.getSharedSpawnPos();
    }

    /**
     * O rumo, em oito pontos.
     *
     * atan2 devolve zero apontando para +X (leste) e cresce em direcao a +Z (sul).
     * O deslocamento de meio setor antes de dividir e o que faz "quase norte" ler
     * como norte, em vez de pular para nordeste no primeiro grau.
     */
    private static String rhumb(double dx, double dz) {
        double degrees = Math.toDegrees(Math.atan2(dx, -dz));
        int index = (int) Math.floor(((degrees + 360.0D) % 360.0D) / 45.0D + 0.5D) % 8;
        return RHUMBS[index];
    }
}
