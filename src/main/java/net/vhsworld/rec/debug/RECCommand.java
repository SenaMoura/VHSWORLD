package net.vhsworld.rec.debug;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.vhsworld.rec.RECMod;

import java.util.ArrayList;
import java.util.List;

/**
 * /rec — A BANCADA, do lado do jogador.
 *
 * <h3>Por que este arquivo existe (Pedro, 2026-08-03)</h3>
 * "Coloca comandos que possibilitam eu ver os eventos e as entidades mais rapido." E a
 * regra passou a valer para tudo que entrar no mod daqui para a frente.
 *
 * ⚠️ A LISTA DE ENTIDADES E DESCOBERTA, NAO CADASTRADA. Ela sai do registro do Forge
 * filtrando o namespace `recmod`, entao <b>criatura nova aparece aqui sozinha</b>, sem
 * ninguem lembrar de nada. Uma lista escrita a mao seria uma segunda verdade que fura a
 * primeira em silencio — a mesma armadilha que o Staging evita ao ler o elenco do bioma em
 * vez de ter lista propria. Os EVENTOS ainda precisam de uma linha, mas de uma so, e ela
 * mora toda em {@link TestBench}.
 *
 * <ul>
 *   <li><b>/rec entidades</b> — todas as criaturas do mod e quantas estao perto de voce</li>
 *   <li><b>/rec entidade &lt;id&gt; [distancia]</b> — poe uma na sua frente</li>
 *   <li><b>/rec brilho [raio]</b> — acende as criaturas do mod atraves da parede</li>
 *   <li><b>/rec eventos</b> — tudo que da para disparar</li>
 *   <li><b>/rec evento &lt;id&gt;</b> — dispara agora</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = RECMod.MOD_ID)
public final class RECCommand {

    private RECCommand() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static final SuggestionProvider<CommandSourceStack> OUR_ENTITIES =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    ours().stream().map(t -> key(t).getPath()).toList(), builder);

    private static final SuggestionProvider<CommandSourceStack> OUR_TRIGGERS =
            (ctx, builder) -> SharedSuggestionProvider.suggest(
                    TestBench.all().stream().map(TestBench.Trigger::id).toList(), builder);

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("rec")
                .requires(source -> source.hasPermission(2))
                .executes(ctx -> help(ctx.getSource()))

                .then(Commands.literal("entidades").executes(ctx -> listEntities(ctx.getSource())))

                .then(Commands.literal("entidade")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(OUR_ENTITIES)
                                .executes(ctx -> spawn(ctx, 6.0D))
                                .then(Commands.argument("distancia", DoubleArgumentType.doubleArg(1.0D, 64.0D))
                                        .executes(ctx -> spawn(ctx,
                                                DoubleArgumentType.getDouble(ctx, "distancia"))))))

                .then(Commands.literal("brilho")
                        .executes(ctx -> glow(ctx.getSource(), 64.0D))
                        .then(Commands.argument("raio", DoubleArgumentType.doubleArg(1.0D, 256.0D))
                                .executes(ctx -> glow(ctx.getSource(),
                                        DoubleArgumentType.getDouble(ctx, "raio")))))

                .then(Commands.literal("eventos").executes(ctx -> listTriggers(ctx.getSource())))

                .then(Commands.literal("evento")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(OUR_TRIGGERS)
                                .executes(RECCommand::fire))));
    }

    // ------------------------------------------------------------------ entidades

    /** Todo EntityType registrado por nos. Descoberto, nunca cadastrado. */
    private static List<EntityType<?>> ours() {
        List<EntityType<?>> out = new ArrayList<>();
        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES) {
            ResourceLocation id = key(type);
            if (id != null && RECMod.MOD_ID.equals(id.getNamespace())) out.add(type);
        }
        return out;
    }

    private static ResourceLocation key(EntityType<?> type) {
        return ForgeRegistries.ENTITY_TYPES.getKey(type);
    }

    private static int listEntities(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        // Quantas de cada tipo estao perto AGORA — a metade que responde "ele nasceu?"
        // sem precisar procurar no escuro.
        AABB box = player.getBoundingBox().inflate(128.0D);
        List<Entity> near = player.level().getEntitiesOfClass(Entity.class, box,
                e -> key(e.getType()) != null
                        && RECMod.MOD_ID.equals(key(e.getType()).getNamespace()));

        source.sendSuccess(() -> Component.literal("[REC] criaturas do mod (128 blocos)")
                .withStyle(ChatFormatting.DARK_PURPLE), false);

        for (EntityType<?> type : ours()) {
            String path = key(type).getPath();

            int count = 0;
            double closest = Double.MAX_VALUE;
            for (Entity entity : near) {
                if (entity.getType() != type) continue;
                count++;
                closest = Math.min(closest, Math.sqrt(entity.distanceToSqr(player)));
            }

            final int n = count;
            final double d = closest;
            source.sendSuccess(() -> Component.literal(String.format("  %-22s ", path))
                    .withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(n == 0
                                    ? "nenhuma por perto"
                                    : n + " viva(s), a mais perto a " + Math.round(d) + " blocos")
                            .withStyle(n == 0 ? ChatFormatting.DARK_GRAY : ChatFormatting.GREEN)), false);
        }
        return 1;
    }

    /**
     * Poe a criatura na SUA FRENTE, e nao atras.
     *
     * ⚠️ E de proposito, e e a diferenca entre este comando e /diretor colocar: aquele
     * testa a COLOCACAO (arco de tras, racionamento, regra de spawn) e este testa a
     * CRIATURA. Para olhar o bicho — modelo, animacao, tamanho — ele tem que aparecer onde
     * se esta olhando. Misturar os dois faria cada teste responder metade da pergunta.
     */
    private static int spawn(CommandContext<CommandSourceStack> ctx, double distance)
            throws CommandSyntaxException {

        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        String id = StringArgumentType.getString(ctx, "id");

        EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(
                new ResourceLocation(RECMod.MOD_ID, id));

        if (type == null) {
            source.sendFailure(Component.literal("nao existe recmod:" + id));
            return 0;
        }

        ServerLevel level = player.serverLevel();

        Vec3 look = player.getLookAngle();
        double x = player.getX() + look.x * distance;
        double z = player.getZ() + look.z * distance;

        // O chao daquela coluna: soltar no ar faria a criatura cair enquanto se olha para
        // ela, e a primeira coisa que se veria seria a animacao de queda.
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                (int) Math.floor(x), (int) Math.floor(z));

        // Debaixo do teto (caverna, construcao) a altura do mapa nao serve: fica a sua.
        if (Math.abs(y - player.getY()) > 8.0D) y = player.getBlockY();

        Entity entity = type.create(level);
        if (entity == null) {
            source.sendFailure(Component.literal("recmod:" + id + " nao pode ser criada"));
            return 0;
        }

        float yaw = (float) (Math.atan2(player.getZ() - z, player.getX() - x) * (180.0D / Math.PI)) - 90.0F;
        entity.moveTo(x, y, z, yaw, 0.0F);

        // ⚠️ MobSpawnType.COMMAND: invocacao pedida SEMPRE passa, mesmo com o Diretor
        // negando tudo. Se testar dependesse do humor do compasso, nao daria para saber se
        // o bug e da criatura ou do Diretor — a mesma razao pela qual o SpawnGate deixa
        // ovo e comando fora.
        if (entity instanceof Mob mob) {
            mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(x, y, z)),
                    MobSpawnType.COMMAND, null, null);
        }

        if (!level.addFreshEntity(entity)) {
            source.sendFailure(Component.literal("o mundo recusou a entidade"));
            return 0;
        }

        final int fy = y;
        source.sendSuccess(() -> Component.literal("recmod:" + id + " a "
                        + Math.round(distance) + " blocos na sua frente (y=" + fy + ")")
                .withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    /**
     * ACENDE as criaturas do mod — atraves da parede.
     *
     * ⚠️ Resolve o problema que mais custa tempo de teste neste mod: as criaturas sao
     * pretas, aparecem no escuro e varias so existem quando NAO se olha para elas. Procurar
     * uma no breu para conferir se ela nasceu gasta mais tempo do que a mecanica que se
     * queria olhar. Alterna: se ja ha alguma acesa, apaga todas.
     */
    private static int glow(CommandSourceStack source, double radius) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();

        AABB box = player.getBoundingBox().inflate(radius);
        List<Entity> near = player.level().getEntitiesOfClass(Entity.class, box,
                e -> key(e.getType()) != null
                        && RECMod.MOD_ID.equals(key(e.getType()).getNamespace()));

        boolean anyLit = near.stream().anyMatch(Entity::hasGlowingTag);
        for (Entity entity : near) entity.setGlowingTag(!anyLit);

        int count = near.size();
        source.sendSuccess(() -> Component.literal(anyLit
                        ? "apagadas " + count + " criatura(s)"
                        : "acesas " + count + " criatura(s) em " + Math.round(radius) + " blocos")
                .withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    // ------------------------------------------------------------------ eventos

    private static int listTriggers(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("[REC] eventos disparaveis")
                .withStyle(ChatFormatting.DARK_PURPLE), false);

        for (TestBench.Trigger trigger : TestBench.all()) {
            source.sendSuccess(() -> Component.literal(String.format("  %-11s ", trigger.id()))
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(trigger.help()).withStyle(ChatFormatting.GRAY)), false);
        }
        return 1;
    }

    private static int fire(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack source = ctx.getSource();
        ServerPlayer player = source.getPlayerOrException();
        String id = StringArgumentType.getString(ctx, "id");

        TestBench.Trigger trigger = TestBench.get(id);
        if (trigger == null) {
            source.sendFailure(Component.literal("nao ha evento '" + id + "' (veja /rec eventos)"));
            return 0;
        }

        String answer = trigger.fire().apply(player);
        source.sendSuccess(() -> Component.literal(answer).withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static int help(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("[REC] bancada")
                .withStyle(ChatFormatting.DARK_PURPLE), false);
        line(source, "/rec entidades", "todas as criaturas do mod e quantas estao perto");
        line(source, "/rec entidade <id> [dist]", "poe uma na sua frente");
        line(source, "/rec brilho [raio]", "acende as criaturas atraves da parede");
        line(source, "/rec eventos", "tudo que da para disparar");
        line(source, "/rec evento <id>", "dispara agora");
        line(source, "/diretor", "o compasso: pressao, relogios e elenco");
        return 1;
    }

    private static void line(CommandSourceStack source, String cmd, String help) {
        source.sendSuccess(() -> Component.literal("  " + cmd + " ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("— " + help).withStyle(ChatFormatting.GRAY)), false);
    }
}
