package net.vhsworld.rec.director;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.vhsworld.rec.config.RECConfig;
import net.vhsworld.rec.net.AbsencePacket;
import net.vhsworld.rec.net.RECNetwork;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A AUSENCIA: o mundo mexe numa coisa que o jogador poe, pelas costas dele.
 *
 * A tocha que ele fincou nao esta mais la. A porta que ele fechou esta aberta. Nao ha
 * criatura, nao ha vulto, nao ha nada para apontar — e e exatamente por isso que ela e a
 * coisa mais assustadora que este mod pode fazer por duzentas linhas. Terror psicologico
 * nao e medo do monstro: e o jogador duvidando da propria memoria, sozinho, sem nada que
 * prove que ele esta certo.
 *
 * ⚠️ AS TRES TRAVAS QUE FAZEM ISTO SER MEDO E NAO SACANAGEM:
 *
 *   E SEU. So mexe no que ESTE jogador colocou (ver PlacementTrace). Tocha anonima do
 *   mapa nao produz nada, porque ninguem sabia que ela existia.
 *
 *   E PELAS COSTAS. Nunca no campo de visao e nunca colado. O que se ve acontecer e um
 *   bug; o que se descobre ter acontecido e uma assombracao.
 *
 *   E BARATO DE DESFAZER. Tocha e porta, mais nada. No dia em que isto mexer num bau, o
 *   jogador para de ter medo e passa a ter raiva — e raiva desinstala mod.
 */
public final class Absence {

    private Absence() {}

    /** O que aconteceu. Vai no fio para o visor saber o que desenhar. */
    public enum Kind {
        /** A tocha sumiu. */
        TORCH_TAKEN,
        /** A porta mudou de estado sozinha. */
        DOOR_MOVED
    }

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    /** Freio do aviso de "nao achou alvo". Ver o comentario no fim do tryApply. */
    private static long lastMissLog = 0L;

    private static final double MIN_DISTANCE = 8.0D;
    private static final double MAX_DISTANCE = 64.0D;

    /**
     * Tenta fazer uma ausencia acontecer com este jogador. Devolve true se aconteceu.
     *
     * ⚠️ Quem decide QUANDO nao e este arquivo — e o Diretor. Aqui so se responde "deu
     * para fazer alguma coisa agora?".
     */
    static boolean tryApply(ServerPlayer player) {
        if (!RECConfig.COMMON.absence.get()) return false;
        if (!(player.level() instanceof ServerLevel level)) return false;

        // Embaralha para nao pegar sempre a colocacao mais antiga, que costuma ser a mais
        // longe e a que o jogador menos lembra.
        List<PlacementTrace.Mark> candidates = new ArrayList<>(PlacementTrace.of(player));
        Collections.shuffle(candidates);

        // ⚠️ Contadores por MOTIVO. A primeira versao registrava so "nao achou alvo
        // (rastro=42)", e no primeiro teste em que o rastro nao estava vazio esse aviso
        // foi inutil exatamente como o silencio que ele substituiu: 42 marcas, nenhuma
        // pista de por que as 42 falharam. Aviso de diagnostico que nao separa causas nao
        // e diagnostico, e um "nao sei" mais comprido.
        int outro = 0, longe = 0, perto = 0, visao = 0, mudou = 0;

        for (PlacementTrace.Mark mark : candidates) {
            if (!mark.dimension().equals(level.dimension())) { outro++; continue; }

            BlockPos pos = mark.pos();
            if (!level.isLoaded(pos)) { outro++; continue; }

            double distance = Math.sqrt(pos.distToCenterSqr(player.position()));
            if (distance < MIN_DISTANCE) { perto++; continue; }
            if (distance > MAX_DISTANCE) { longe++; continue; }

            if (inSight(player, pos)) { visao++; continue; }

            BlockState now = level.getBlockState(pos);

            // O jogador pode ter quebrado, trocado ou movido a coisa. Se o mundo nao bate
            // mais com a anotacao, a anotacao e lixo — some com ela e tenta a proxima.
            if (!matches(mark.state(), now)) {
                PlacementTrace.forget(player, mark);
                mudou++;
                continue;
            }

            if (apply(level, player, pos, now)) {
                PlacementTrace.forget(player, mark);
                LOG.info("[DIRETOR] AUSENCIA em {} ({}), a {} blocos de {}",
                        pos, now.getBlock().getName().getString(),
                        String.format("%.1f", distance), player.getGameProfile().getName());
                return true;
            }
        }

        // ⚠️ ESTE RAMO E O MAIS IMPORTANTE DO REGISTRO. Se a ausencia nunca acontecer no
        // jogo, e ele que separa as tres causas possiveis: o Diretor nem quis (nao sai
        // linha nenhuma), quis e o rastro estava vazio (rastro=0), ou quis e nada servia
        // — longe demais, perto demais, no campo de visao, ja quebrado (rastro>0). Sem
        // isto, "nao aconteceu nada" seria indistinguivel de "esta quebrado".
        //
        // ⚠️ COM FREIO. Quando nao ha alvo, a batida nunca e reportada, entao o relogio
        // dela nao zera e ela volta a querer a cada poucos segundos — sem o freio, este
        // aviso sozinho enche o log e esconde o que importa.
        long now = System.currentTimeMillis();
        if (now - lastMissLog > 60_000L) {
            lastMissLog = now;
            LOG.info("[DIRETOR] ausencia sem alvo | rastro={} perto(<{})={} longe(>{})={} visao={} mudou={} outro={}",
                    candidates.size(), (int) MIN_DISTANCE, perto, (int) MAX_DISTANCE, longe,
                    visao, mudou, outro);
        }
        return false;
    }

    /** Faz a coisa e avisa o cliente, para o visor poder mostrar o lugar depois. */
    private static boolean apply(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof DoorBlock door) {
            boolean open = state.getValue(DoorBlock.OPEN);
            // O rangido toca no lugar da porta, e isso e proposital: o jogador ouve algo
            // acontecer em algum lugar da base e nao sabe onde. E a unica ausencia que
            // se anuncia, e ela se anuncia mal.
            door.setOpen(null, level, state, pos, !open);
            RECNetwork.toPlayer(player, new AbsencePacket(pos, Kind.DOOR_MOVED));
            return true;
        }

        if (isTorch(state)) {
            // Sem drop e sem particula: ela simplesmente nao esta mais ali. Qualquer
            // efeito seria uma prova, e a mecanica inteira depende de nao haver prova.
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            RECNetwork.toPlayer(player, new AbsencePacket(pos, Kind.TORCH_TAKEN));
            return true;
        }

        return false;
    }

    /**
     * O jogador esta olhando para esse lado?
     *
     * Grosseiro de proposito — cone largo, sem raycast. Errar para o lado do "ele PODE
     * estar vendo" e barato (a ausencia so espera a proxima chance); errar para o outro
     * lado significa sumir com uma tocha na cara do jogador, que estraga o efeito de vez.
     */
    private static boolean inSight(ServerPlayer player, BlockPos pos) {
        Vec3 toBlock = Vec3.atCenterOf(pos).subtract(player.getEyePosition()).normalize();
        return player.getLookAngle().normalize().dot(toBlock) > 0.2D;
    }

    private static boolean matches(BlockState placed, BlockState now) {
        if (isTorch(placed)) return isTorch(now);
        return placed.getBlock() == now.getBlock();
    }

    private static boolean isTorch(BlockState state) {
        return state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH)
                || state.is(Blocks.SOUL_TORCH) || state.is(Blocks.SOUL_WALL_TORCH);
    }
}
