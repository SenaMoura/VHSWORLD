package net.vhsworld.rec.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.BlockHitResult;
import net.vhsworld.rec.escape.Escape;
import net.vhsworld.rec.escape.ExitMethod;
import net.vhsworld.rec.item.ModSounds;

/**
 * A PORTA DE SAIDA da TRAIN e da PARKOURLAND, no molde da do Dimensional Doors.
 *
 * ============================ O QUE VEIO DE LA, E O QUE NAO ============================
 *
 * O Pedro pediu "igual a do mod dimensional doors". O que faz aquela porta ser
 * reconhecivel sao duas coisas, e as duas sao de FORMA:
 *
 *   1. E uma PORTA DE VERDADE. Dois blocos de altura, fina, com dobradica, que abre e
 *      fecha — e nao um cubo com desenho de porta, que era o que esta classe era antes.
 *      Por isso ela herda de `DoorBlock`: a forma, o encaixe das duas metades, a
 *      dobradica e o giro sao os do proprio Minecraft.
 *   2. O painel nao e madeira, e um VAZIO que se mexe. A ideia e deles; o desenho e nosso
 *      (ver tools/build_exit_door.py, que explica o que foi copiado e o que nao).
 *
 * Nenhum pixel e nenhuma linha de codigo veio do mod deles.
 *
 * ============================ ABRIR NAO E SAIR ============================
 *
 * ⚠️ SAO DOIS ATOS SEPARADOS, e a separacao e o que faz a porta funcionar como porta.
 * Clicar so abre. Quem leva embora e ATRAVESSAR — e atravessar so e possivel depois de
 * abrir, porque fechada ela e solida.
 *
 * A versao anterior teleportava no clique E no toque, e era pior por um motivo concreto:
 * o jogador que encostava sem querer no batente ja tinha ido, sem ter escolhido ir. Com o
 * ato dividido, abrir e uma decisao reversivel (da para fechar de novo e continuar
 * andando) e so o passo para dentro e definitivo — que e exatamente como uma porta se
 * comporta, e o que o mod deles faz.
 */
public class ExitDoorBlock extends DoorBlock {

    /**
     * ⚠️ `BlockSetType.IRON` pelo SOM, e nao pelo comportamento. Ele traz o metal
     * (rangido, batida seca), que e o que uma porta destas tem que soar numa linha de trem
     * e numa gaiola de parkour. O que ele tambem traz — "nao abre na mao" — e desfeito
     * logo abaixo, no `use`: a porta PRECISA abrir na mao, porque nao ha redstone nenhuma
     * nas duas dimensoes que a usam e ela e a unica saida delas.
     */
    public ExitDoorBlock(Properties properties) {
        super(properties, BlockSetType.IRON);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player,
                                 InteractionHand hand, BlockHitResult hit) {
        // Abre e fecha, e mais nada. Ver o comentario da classe.
        state = state.cycle(OPEN);
        level.setBlock(pos, state, 10);
        level.playSound(player, pos, ModSounds.TAPE_STATIC.get(), SoundSource.BLOCKS,
                0.7F, state.getValue(OPEN) ? 0.65F : 0.9F);
        level.gameEvent(player, state.getValue(OPEN)
                ? net.minecraft.world.level.gameevent.GameEvent.BLOCK_OPEN
                : net.minecraft.world.level.gameevent.GameEvent.BLOCK_CLOSE, pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /**
     * Atravessar e sair.
     *
     * ⚠️ SO DISPARA COM A PORTA ABERTA, e isso e de graca: fechada, o bloco tem colisao e
     * o jogador nao entra no espaco dele. Nao ha condicao escrita aqui para isso — quem
     * garante e a forma da porta, e e por isso que ela precisava ser uma porta de verdade.
     *
     * ⚠️ O `Escape.leave` E CHAMADO DUAS VEZES POR PASSAGEM e isso esta previsto: a porta
     * tem duas metades, e o jogo chama `entityInside` uma vez por bloco tocado. A segunda
     * chamada sai fora sozinha porque o jogador ja nao esta mais numa dimensao nossa —
     * ver a guarda no proprio `Escape.leave`, que existe por causa deste caso.
     */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide) return;
        if (!state.getValue(OPEN)) return;
        if (entity instanceof ServerPlayer player) {
            Escape.leave(player, ExitMethod.DOOR);
        }
    }

    /** Qual lado a dobradica fica. So para quem planta a porta pelo codigo saber pedir. */
    public static DoorHingeSide hinge(boolean left) {
        return left ? DoorHingeSide.LEFT : DoorHingeSide.RIGHT;
    }
}
