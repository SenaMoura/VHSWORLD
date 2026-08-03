package net.vhsworld.rec.director;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * O RASTRO: as ultimas coisas que ESTE jogador colocou no mundo.
 *
 * ⚠️ ISTO E O QUE FAZ A AUSENCIA DOER. Sumir com uma tocha qualquer do mapa nao produz
 * nada: o jogador nunca soube que ela existia, entao nao ha o que estranhar. O arrepio
 * inteiro mora em ter sido VOCE que fincou aquela tocha ali, ha dez minutos, com a mao —
 * e ela nao estar mais. Sem este arquivo, a mecanica vira ruido de mundo; com ele, vira
 * uma discussao entre o jogador e a propria memoria.
 *
 * ⚠️ SO GUARDA O QUE E SEGURO MEXER. A tentacao de anotar tudo e real e e uma cilada:
 * um bau, um bloco de estrutura ou um pedaco de redstone mexido pelo mod nao e terror, e
 * perda de progresso — e perda de progresso ninguem perdoa, ainda que tenha assustado.
 * A lista de candidatos vive em {@link #isCandidate} e deve continuar curta e chata.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID)
public final class PlacementTrace {

    private PlacementTrace() {}

    /** Quantas colocacoes recentes cabem por jogador. */
    private static final int MEMORY = 64;

    /**
     * Uma coisa que o jogador colocou, onde, e QUANDO.
     *
     * ⚠️ O `placedAt` entrou depois do teste de 2026-08-02, e ele existe por um motivo que
     * o teste provou: tocha recem-fincada nao tem memoria associada. O jogador ainda esta
     * com o gesto na mao — se ela some, ou ele ve sumir, ou ele conclui na hora que o mod
     * fez. Marca precisa ENVELHECER para virar lembranca: e depois de andar, voltar e
     * passar por ela de novo que ela deixa de ser um bloco e vira "a tocha do corredor".
     */
    public record Mark(ResourceKey<Level> dimension, BlockPos pos, BlockState state, long placedAt) {}

    private static final Map<UUID, Deque<Mark>> TRACE = new HashMap<>();

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        BlockState state = event.getPlacedBlock();
        if (!isCandidate(state)) return;

        // ⚠️ Porta e dois blocos. Anotar so a METADE DE BAIXO evita a mesma porta entrar
        // duas vezes na lista e evita mexer na metade errada la na frente.
        BlockPos pos = event.getPos();
        if (state.getBlock() instanceof DoorBlock
                && state.getValue(DoorBlock.HALF) == net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER) {
            return;
        }

        Deque<Mark> marks = TRACE.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
        marks.addLast(new Mark(player.level().dimension(), pos.immutable(), state,
                player.level().getGameTime()));
        while (marks.size() > MEMORY) marks.removeFirst();
    }

    /**
     * O que a ausencia pode mexer.
     *
     * Tocha some; porta abre. Sao as duas unicas coisas do jogo que o jogador nota na
     * hora, lembra de ter feito, e cuja alteracao nao custa item nem quebra construcao —
     * dan para desfazer com um clique. Redstone de proposito fora: torch de redstone
     * parece tocha e derruba maquina.
     */
    private static boolean isCandidate(BlockState state) {
        return state.is(Blocks.TORCH)
                || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.SOUL_TORCH)
                || state.is(Blocks.SOUL_WALL_TORCH)
                || state.getBlock() instanceof DoorBlock;
    }

    /** O rastro deste jogador, do mais antigo para o mais novo. Nunca null. */
    static Deque<Mark> of(ServerPlayer player) {
        return TRACE.computeIfAbsent(player.getUUID(), id -> new ArrayDeque<>());
    }

    static void forget(ServerPlayer player, Mark mark) {
        Deque<Mark> marks = TRACE.get(player.getUUID());
        if (marks != null) marks.remove(mark);
    }

    public static void clear(UUID player) {
        TRACE.remove(player);
    }
}
