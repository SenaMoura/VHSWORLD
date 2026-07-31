package net.vhsworld.rec.escape;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BaseRailBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.vhsworld.rec.RECMod;
import net.vhsworld.rec.init.ModBlocks;
import net.vhsworld.rec.item.ModSounds;
import net.vhsworld.rec.worldgen.dim.DimensionProfile;
import net.vhsworld.rec.worldgen.dim.TrainChunkGenerator;

/**
 * A PORTA NASCE NA LINHA, a frente de quem andou o bastante.
 *
 * ============================ POR QUE ELA NAO ESTA EM LUGAR NENHUM ====================
 *
 * A outra saida do mod — o Espelho — fica parada num ponto: ela ja esta no mapa e o
 * trabalho do jogador e ACHAR. Nas duas dimensoes desta porta isso nao serve, e o motivo
 * e o que elas SAO.
 *
 * A TRAIN e uma reta infinita entre dois paredoes que nao se alcanca; a PARKOURLAND e uma
 * torre fechada e finita de que se cai para fora. Nas duas nao ha desvio, nao ha o que
 * revistar, nao ha para onde sair do caminho: a unica coisa que se pode fazer e avancar.
 * Um Espelho parado num ponto delas seria um DESTINO numa dimensao cujo assunto e nao
 * haver destino nenhum — o jogador deixaria de percorrer o caminho e passaria a ir a um
 * lugar.
 *
 * Aqui o metodo cobra a unica coisa que as duas ja permitem. Avanca-se. E depois de
 * avancar o bastante, a saida aparece — nao porque foi encontrada, mas porque o caminho
 * foi feito.
 *
 * ============================ SO AVANCO CONTA ============================
 *
 * ⚠️ A CONTA E DE DISTANCIA E NAO DE TEMPO, e a diferenca e a mecanica inteira. Com um
 * relogio, o jogador otimo e o que fica parado esperando — e ficar parado num lugar seguro
 * e exatamente o oposto do que o metodo quer comprar. Com distancia, o unico jeito de
 * pagar e atravessar a dimensao, que e a experiencia que se queria vender.
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID)
public final class TrackDoor {

    private TrackDoor() {}

    private static final String TAG = "recmod:track_walk";

    /**
     * Quantos blocos de linha a porta custa.
     *
     * 200 sao uns 45 segundos andando e uns 35 correndo. E "um tempinho": longo o
     * bastante para a linha ter sido uma travessia e nao um corredor, e curto o bastante
     * para nao virar castigo em quem entrou sem saber que teria que andar.
     */
    private static final double WALK_COST = 200.0D;

    /**
     * A quantos blocos a frente ela nasce.
     *
     * ⚠️ TEM QUE CABER NA BRUMA DA TRAIN, que e fechada (fog 0.60). Se ela nascesse a 40
     * blocos, apareceria dentro da neblina e o jogador passaria por ela sem ver. 14 e
     * perto o bastante para ela aparecer JA VISIVEL, que e o que faz o momento ser um
     * acontecimento e nao uma descoberta.
     */
    private static final int AHEAD = 14;

    /**
     * O maior passo que ainda conta como andar.
     *
     * Sem isto, um teleporte de mil blocos — a fita, um comando, o castigo do espelho —
     * entraria na conta como se o jogador tivesse caminhado, e a porta nasceria de graca.
     */
    private static final double MAX_STEP = 3.0D;

    /** So a cada tantos tiques: a conta e barata, mas nao precisa de vinte por segundo. */
    private static final int EVERY = 4;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.getServer().getTickCount() % EVERY != 0) return;

        for (ServerLevel level : event.getServer().getAllLevels()) {
            DimensionProfile profile = DimensionProfile.of(level);
            // ⚠️ SO A TRAIN. A PARKOURLAND tambem e `ExitMethod.DOOR`, mas a porta dela ja
            // esta pronta no topo da torre — ver o comentario do `place`. Deixar o
            // contador rodando la faria nascer uma SEGUNDA porta no meio da subida, e o
            // jogador sairia sem nunca chegar ao topo.
            if (profile == null || !"train".equals(profile.id())) continue;
            for (ServerPlayer player : level.players()) {
                walk(level, player);
            }
        }
    }

    private static void walk(ServerLevel level, ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        CompoundTag mark = data.contains(TAG) ? data.getCompound(TAG) : new CompoundTag();

        double x = player.getX(), y = player.getY(), z = player.getZ();
        if (!mark.contains("lastX")) {
            mark.putDouble("lastX", x);
            mark.putDouble("lastY", y);
            mark.putDouble("lastZ", z);
            data.put(TAG, mark);
            return;
        }

        double sx = x - mark.getDouble("lastX");
        double sy = y - mark.getDouble("lastY");
        double sz = z - mark.getDouble("lastZ");
        mark.putDouble("lastX", x);
        mark.putDouble("lastY", y);
        mark.putDouble("lastZ", z);

        // ⚠️ O PASSO E MEDIDO NOS TRES EIXOS, e ja foi so em X. Enquanto a porta era so da
        // TRAIN aquilo bastava — la o caminho e uma reta e todo avanco e horizontal. A
        // PARKOURLAND quebrou isso: e uma TORRE, e quem sobe cem blocos de plataforma nao
        // anda um metro em X. Com a conta antiga, o jogador podia escalar a dimensao
        // inteira sem nunca pagar a porta, e a unica saida dele nunca apareceria.
        double step = Math.sqrt(sx * sx + sy * sy + sz * sz);

        if (step <= MAX_STEP) {
            mark.putDouble("walked", mark.getDouble("walked") + step);
            // A direcao guardada e a do ULTIMO movimento horizontal com alguma intencao.
            // Ela so e usada na TRAIN (de que lado da linha a porta nasce), e por isso nao
            // pode se mexer com o tremor de quem esta parado nem com quem so subiu.
            if (Math.abs(sx) > 0.02D) mark.putInt("dir", sx > 0 ? 1 : -1);
        }

        if (mark.getDouble("walked") >= WALK_COST) {
            int dir = mark.getInt("dir");
            if (dir == 0) dir = 1;
            if (place(level, player, dir)) {
                mark.putDouble("walked", 0.0D);
            }
            // Se nao deu para pôr (chunk ainda nao carregado la na frente), a conta fica
            // cheia e a proxima passagem tenta de novo — nada se perde.
        }

        data.put(TAG, mark);
    }

    /**
     * Poe a porta na linha, `AHEAD` blocos a frente, em cima do trilho.
     *
     * ⚠️ O TRILHO E PROCURADO NO MUNDO, e nao calculado. A via da TRAIN vem de uma peca
     * que o Pedro construiu e que o gerador carimba: as fileiras de trilho estao onde ele
     * as pos, e nao onde este arquivo acharia que estao. Escrever aqui um "z = 0 e z = 8"
     * funcionaria ate o dia em que ele mexesse na peca, e ai a porta passaria a nascer ao
     * lado dos trilhos, ou no vazio — sem erro nenhum, so uma porta no lugar errado.
     * Varrendo a largura do estrado atras de um bloco de trilho, ela acompanha a peca.
     */
    /**
     * Poe a porta na linha, `AHEAD` blocos a frente, em cima do trilho.
     *
     * ⚠️ SO A TRAIN CHEGA AQUI. A PARKOURLAND tambem usa `ExitMethod.DOOR`, mas la a porta
     * NAO nasce andando: ela ja esta pronta no topo da torre, ao lado do bau, desenhada
     * pelo proprio gerador. Foi decisao do Pedro e ela conserta um problema que esta
     * classe tinha: naquela dimensao o caminho e vertical e feito de plataformas soltas
     * sobre o vazio, entao "a frente" nao existe em linha reta e a porta acabava nascendo
     * em cima de um degrau qualquer no meio da subida — encurtando o parkour, que e a
     * dimensao inteira. No topo, ela e o PREMIO da subida, junto do bau, e nao um atalho.
     */
    private static boolean place(ServerLevel level, ServerPlayer player, int dir) {
        int tx = player.getBlockX() + dir * AHEAD;
        int y = TrainChunkGenerator.DECK_Y + 2;

        if (!level.isLoaded(new BlockPos(tx, y, 0))) return false;

        int rail = railNear(level, tx, player.getBlockZ());
        raise(level, tx, y, rail, Direction.WEST);

        level.playSound(null, tx, y, rail, ModSounds.TAPE_STATIC.get(),
                SoundSource.BLOCKS, 1.0F, 0.6F);
        player.displayClientMessage(Component.translatable("message.recmod.track_door"), true);
        return true;
    }

    /**
     * Levanta a porta: as duas metades e os batentes em volta.
     *
     * ⚠️ AS DUAS METADES PRECISAM CONCORDAR. Uma porta do Minecraft sao dois blocos que
     * guardam o MESMO `facing` e a MESMA dobradica, e o de cima ainda carrega
     * `half=upper`. Escrever so o de baixo da meia porta; escrever os dois com valores
     * diferentes da uma porta que abre pela metade, com a folha de cima virada para outro
     * lado. Nenhum dos dois casos da erro — os dois dao uma porta torta que so se ve
     * dentro do jogo.
     *
     * ⚠️ E O DE CIMA VAI PRIMEIRO. `DoorBlock.canSurvive` do bloco de baixo exige chao
     * firme embaixo, e o de cima exige que o de baixo seja porta; escrevendo de baixo para
     * cima, o de baixo e conferido num instante em que o de cima ainda nao existe e o jogo
     * o quebra sozinho. Publico como `raise` porque o gerador da PARKOURLAND levanta a
     * dele exatamente do mesmo jeito.
     */
    public static void raise(ServerLevel level, int x, int y, int z, Direction facing) {
        BlockState door = ModBlocks.EXIT_DOOR.get().defaultBlockState()
                .setValue(DoorBlock.FACING, facing)
                .setValue(DoorBlock.HINGE, DoorHingeSide.LEFT)
                .setValue(DoorBlock.OPEN, Boolean.FALSE)
                .setValue(DoorBlock.POWERED, Boolean.FALSE);

        BlockState post = Blocks.DEEPSLATE_TILES.defaultBlockState();

        // os batentes e a verga primeiro: sao eles que fazem o vao ser lido como porta
        for (int h = 0; h < 2; h++) {
            level.setBlock(new BlockPos(x, y + h, z - 1), post, 2);
            level.setBlock(new BlockPos(x, y + h, z + 1), post, 2);
        }
        for (int dz = -1; dz <= 1; dz++) {
            level.setBlock(new BlockPos(x, y + 2, z + dz), post, 2);
        }

        level.setBlock(new BlockPos(x, y + 1, z),
                door.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER), 2);
        level.setBlock(new BlockPos(x, y, z),
                door.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), 2);
    }

    /**
     * A fileira de trilho mais proxima deste Z, dentro da largura do estrado.
     *
     * ⚠️ NUNCA DEVOLVE "NAO ACHEI". Se a varredura nao encontrar trilho nenhum — peca
     * trocada, chunk meio carregado, um dia em que o Pedro refaca a via com outro bloco —
     * ela cai no meio do estrado em vez de desistir. E a diferenca entre "a porta nasceu
     * um pouco fora do trilho" e "a porta nunca nasce", e desde que a fita virou so ida a
     * segunda e um jogador preso para sempre numa reta infinita. Bem colocada e melhor;
     * colocada e obrigatorio.
     */
    private static int railNear(ServerLevel level, int x, int fromZ) {
        int y = TrainChunkGenerator.DECK_Y + 1;
        int best = TrainChunkGenerator.DECK_WIDE / 2;
        int bestDistance = Integer.MAX_VALUE;
        for (int z = -2; z < TrainChunkGenerator.DECK_WIDE + 2; z++) {
            if (!(level.getBlockState(new BlockPos(x, y, z)).getBlock() instanceof BaseRailBlock)) {
                continue;
            }
            int distance = Math.abs(z - fromZ);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = z;
            }
        }
        return best;
    }
}
