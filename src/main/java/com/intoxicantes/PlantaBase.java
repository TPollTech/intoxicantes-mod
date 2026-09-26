package com.intoxicantes;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/**
 * A PLANTA DE CONSTRUÇÃO (v1.2.60) — o formato que o PENDRIVE DE PLANTA
 * carrega e o PROJETOR DE HOLOGRAMA projeta. Cada bloco é a string
 * {@code "x,y,z:id"} (o MESMO formato do arquivo do Planejador 3D do
 * Freebuff: uma planta salva lá entra aqui sem conversão).
 *
 * As coordenadas são RELATIVAS ao centro da escaneada/origem da planta —
 * a maquete nasce onde o projetor aponta, não onde a construção original
 * estava. O limite de blocos protege o renderer (e o FPS de todo mundo).
 */
public record PlantaBase(String nome, List<String> blocos) {

    /** Teto de blocos por planta (protege o renderer de maquetes-monstro). */
    public static final int LIMITE = 400;

    /** Versão sem nada gravado (projetor recém-craftado). */
    public static final PlantaBase VAZIA = new PlantaBase("", List.of());

    public static final Codec<PlantaBase> CODEC = RecordCodecBuilder.create(instancia -> instancia
            .group(Codec.STRING.fieldOf("nome").forGetter(PlantaBase::nome),
                    Codec.STRING.listOf().fieldOf("blocos").forGetter(PlantaBase::blocos))
            .apply(instancia, PlantaBase::new));

    public static final StreamCodec<ByteBuf, PlantaBase> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, PlantaBase::nome,
            ByteBufCodecs.STRING_UTF8.listOf(), PlantaBase::blocos,
            PlantaBase::new);

    public int total() {
        return blocos.size();
    }

    public boolean vazia() {
        return blocos.isEmpty();
    }

    /** "x,y,z:id" → as coordenadas relativas do bloco. */
    public static int[] coords(String bloco) {
        String[] partes = bloco.split(":")[0].split(",");
        return new int[]{Integer.parseInt(partes[0]),
                Integer.parseInt(partes[1]),
                Integer.parseInt(partes[2])};
    }

    /** Altura do piso da planta (a maquete aterrissa por ele). */
    public int minY() {
        if (blocos.isEmpty()) return 0;
        int menor = Integer.MAX_VALUE;
        for (String b : blocos) menor = Math.min(menor, coords(b)[1]);
        return menor;
    }

    /** Corta a lista no LIMITE (a varredura grande é truncada, nunca estoura). */
    public static PlantaBase truncada(String nome, List<String> blocos) {
        return new PlantaBase(nome,
                blocos.size() > LIMITE ? blocos.subList(0, LIMITE) : blocos);
    }
}
