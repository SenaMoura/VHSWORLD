package net.vhsworld.rec.client.codex;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * Uma ficha do registro: o que o item faz, como se consegue e a receita, se houver.
 *
 * Os textos vivem nos arquivos de idioma, nao aqui — assim a ficha nasce traduzida
 * e ninguem precisa mexer em Java para corrigir uma frase.
 */
public class CodexEntry {

    public final Item item;

    /** Chave base no lang: recmod.codex.<nome>.desc e .obtain */
    public final String key;

    /** Receita a mostrar na animacao, ou null se o item nao se craftar. */
    public final ResourceLocation recipe;

    /** Em qual card o item aparece. */
    public final CodexCategory category;

    public CodexEntry(Item item, String key, ResourceLocation recipe, CodexCategory category) {
        this.item = item;
        this.key = key;
        this.recipe = recipe;
        this.category = category;
    }

    public String descKey() {
        return "recmod.codex." + key + ".desc";
    }

    public String obtainKey() {
        return "recmod.codex." + key + ".obtain";
    }
}
