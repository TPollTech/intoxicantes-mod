"""Gera a sprite 16x16 do Opio (seiva seca da papoula) e salva em textures/item/opio.png.

Uso: python tools/gen_opio.py  (a partir da raiz do projeto do mod)
"""
from PIL import Image

IMG = "src/main/resources/assets/intoxicantes/textures/item/opio.png"


def main():
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()

    # tons de seiva de papoula secada: dourado-sujo com degradê vertical
    CLARO = (198, 154, 60, 255)
    MEDIO = (168, 122, 44, 255)
    ESCURO = (128, 88, 30, 255)
    BRILHO = (224, 186, 96, 255)

    # blob orgânico: por linha, faixa horizontal com bordas irregulares
    linhas = {
        3:  (6, 9), 4: (5, 10), 5: (4, 11), 6: (4, 12), 7: (3, 12),
        8: (3, 13), 9: (3, 12), 10: (4, 12), 11: (4, 11), 12: (5, 10), 13: (6, 9),
    }
    for y, (a, b) in linhas.items():
        for x in range(a, b + 1):
            if y <= 5:
                cor = BRILHO if x <= a + 1 else CLARO
            elif y <= 9:
                cor = CLARO if x <= a + 1 else MEDIO
            else:
                cor = MEDIO if x <= a + 1 else ESCURO
            px[x, y] = cor

    # brilho especular no topo
    px[6, 4] = (240, 210, 130, 255)
    px[7, 4] = (240, 210, 130, 255)
    px[6, 5] = (232, 196, 112, 255)

    # pontinhos de impureza (seiva prensada)
    for (x, y) in [(8, 8), (9, 10), (7, 11), (10, 6)]:
        px[x, y] = ESCURO

    img.save(IMG)
    print(f"Sprite salva: {IMG}")


if __name__ == "__main__":
    main()
