import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.patches import Rectangle, FancyBboxPatch

W, HEADER, ROW = 4.2, 0.62, 0.42
TOP = 7.7
XS = [0.3, 5.85, 11.4, 16.95, 22.5]
CW, CH = 27, 9.3

# (marcador, nombre, tipo, obligatorio)
TABLAS = [
 ("USUARIO", "#E3E7EB", [
   ("PK","id","BIGINT",True), ("","nombre","VARCHAR(255)",True), ("UK","correo","VARCHAR(255)",True),
   ("","telefono","VARCHAR(255)",False), ("","direccion","VARCHAR(255)",False),
   ("","password","VARCHAR(255)",True), ("","rol","ENUM",True), ("","activo","BIT",True)]),
 ("BOLETA", "#B2F2BB", [
   ("PK","id","BIGINT",True), ("FK","usuario_id","BIGINT",True), ("","fecha","DATETIME(6)",True),
   ("","direccion","VARCHAR(255)",True), ("","estado","ENUM",True), ("","total","INT",True)]),
 ("DETALLE_BOLETA", "#B2F2BB", [
   ("PK","id","BIGINT",True), ("FK","boleta_id","BIGINT",True), ("FK","producto_id","BIGINT",True),
   ("","cantidad","INT",True), ("","precio_unitario","INT",True), ("","subtotal","INT",True)]),
 ("PRODUCTO", "#A5D8FF", [
   ("PK","id","BIGINT",True), ("","nombre","VARCHAR(255)",True), ("","descripcion","VARCHAR(255)",False),
   ("","precio","INT",True), ("","stock","INT",True), ("","stock_minimo","INT",True),
   ("","activo","BIT",True), ("FK","categoria_id","BIGINT",True)]),
 ("CATEGORIA", "#A5D8FF", [
   ("PK","id","BIGINT",True), ("UK","nombre","VARCHAR(255)",True), ("","descripcion","VARCHAR(255)",False)]),
]

fig, ax = plt.subplots(figsize=(CW, CH), dpi=100)
ax.set_xlim(0, CW); ax.set_ylim(0, CH); ax.axis("off")
fig.patch.set_facecolor("white")

pos = {}   # nombre -> dict(x, top, filas{col: y})
for (nombre, color, cols), x in zip(TABLAS, XS):
    h = HEADER + ROW * len(cols)
    # sombra suave + cuerpo
    ax.add_patch(Rectangle((x+0.06, TOP-h-0.06), W, h, fc="#00000018", ec="none", zorder=1))
    ax.add_patch(Rectangle((x, TOP-h), W, h, fc="white", ec="#1F2A44", lw=1.6, zorder=2))
    ax.add_patch(Rectangle((x, TOP-HEADER), W, HEADER, fc=color, ec="#1F2A44", lw=1.6, zorder=3))
    ax.text(x+W/2, TOP-HEADER/2, nombre, ha="center", va="center", fontsize=15, fontweight="bold",
            color="#10213F", zorder=4)
    filas = {}
    for i, (mk, nom, tipo, nn) in enumerate(cols):
        y = TOP - HEADER - (i + 0.5) * ROW
        filas[nom] = y
        if i > 0:
            ax.plot([x, x+W], [y+ROW/2, y+ROW/2], color="#D5DAE3", lw=0.8, zorder=3)
        if mk:
            c = {"PK": "#B45309", "FK": "#1D4ED8", "UK": "#7C3AED"}[mk]
            ax.text(x+0.14, y, mk, ha="left", va="center", fontsize=10.5, fontweight="bold", color=c,
                    family="DejaVu Sans Mono", zorder=4)
        ax.text(x+0.72, y, ("* " if nn else "  ") + nom, ha="left", va="center", fontsize=12,
                family="DejaVu Sans Mono", fontweight="bold" if mk == "PK" else "normal",
                color="#10213F", zorder=4)
        ax.text(x+W-0.14, y, tipo, ha="right", va="center", fontsize=10.5, color="#5B6B85",
                family="DejaVu Sans Mono", zorder=4)
    pos[nombre] = dict(x=x, filas=filas, h=h)

def crow(x_edge, y, d):
    """Pata de gallo (lado 'muchos'). d=+1 si la linea llega desde la izquierda, -1 desde la derecha."""
    ax_ = x_edge - d * 0.32
    for dy in (-0.15, 0, 0.15):
        ax.plot([ax_, x_edge], [y, y + dy], color="#1F2A44", lw=1.6, zorder=5)

def uno(x_edge, y, d):
    """Barra del lado 'uno'."""
    bx = x_edge + d * 0.22
    ax.plot([bx, bx], [y-0.14, y+0.14], color="#1F2A44", lw=1.8, zorder=5)

def relacion(padre, fila_p, hijo, fila_h, texto, punteada=False):
    P, H = pos[padre], pos[hijo]
    yp, yh = P["filas"][fila_p], H["filas"][fila_h]
    if P["x"] < H["x"]:      # padre a la izquierda
        x1, x2, d_p, d_h = P["x"]+W, H["x"], +1, +1
    else:                    # padre a la derecha
        x1, x2, d_p, d_h = P["x"], H["x"]+W, -1, -1
    xm = (x1 + x2) / 2
    ls = (0, (4, 3)) if punteada else "-"
    ax.plot([x1, xm, xm, x2], [yp, yp, yh, yh], color="#1F2A44", lw=1.6, ls=ls, zorder=4)
    uno(x1, yp, d_p)
    crow(x2, yh, d_h)
    ax.text(x1 + d_p*0.45, yp-0.30, "1", fontsize=10, color="#1F2A44", ha="center", fontweight="bold", zorder=6)
    ax.text(x2 - d_h*0.55, yh+0.30, "N", fontsize=10, color="#1F2A44", ha="center", fontweight="bold", zorder=6)
    ax.text(xm, max(yp, yh)+0.55, texto, fontsize=9.5, color="#334155", ha="center", style="italic",
            bbox=dict(fc="white", ec="none", pad=1.2), zorder=6)

relacion("USUARIO", "id", "BOLETA", "usuario_id", "realiza", punteada=True)
relacion("BOLETA", "id", "DETALLE_BOLETA", "boleta_id", "contiene")
relacion("PRODUCTO", "id", "DETALLE_BOLETA", "producto_id", "se vende en")
relacion("CATEGORIA", "id", "PRODUCTO", "categoria_id", "agrupa")

# Titulo
ax.text(0.3, 8.85, "Modelo de datos relacional · DisGas (Gas El Volcán)", fontsize=22, fontweight="bold",
        color="#10213F", va="center")
ax.text(0.3, 8.33, "MySQL 8 · 5 tablas · USUARIO → BOLETA → DETALLE_BOLETA ← PRODUCTO ← CATEGORIA   (Figura 1 de la pauta)",
        fontsize=12.5, color="#475569", va="center")

# Leyenda
ly = 2.75
ax.add_patch(FancyBboxPatch((0.3, 0.3), CW-0.6, ly-0.45, boxstyle="round,pad=0.02,rounding_size=0.12",
                            fc="#F8FAFC", ec="#CBD5E1", lw=1.2, zorder=1))
ax.text(0.7, ly-0.4, "Cómo leer el diagrama", fontsize=13, fontweight="bold", color="#10213F")
izq = [
 ("PK", "#B45309", "Clave primaria (identifica cada fila)"),
 ("FK", "#1D4ED8", "Clave foránea (apunta a la tabla padre)"),
 ("UK", "#7C3AED", "Valor único (no se puede repetir)"),
 ("*",  "#10213F", "Campo obligatorio (NOT NULL)"),
]
for i, (m, c, t) in enumerate(izq):
    y = ly-0.85-i*0.34
    ax.text(0.8, y, m, fontsize=11, fontweight="bold", color=c, family="DejaVu Sans Mono", va="center")
    ax.text(1.5, y, t, fontsize=11, color="#334155", va="center")
cx = 9.4
ax.plot([cx, cx+0.8], [ly-0.85, ly-0.85], color="#1F2A44", lw=1.6)
uno(cx, ly-0.85, +1); crow(cx+0.8, ly-0.85, +1)
ax.text(cx+1.1, ly-0.85, "Relación 1 a N: una fila de la tabla padre tiene muchas filas hijas", fontsize=11, color="#334155", va="center")
ax.plot([cx, cx+0.8], [ly-1.19, ly-1.19], color="#1F2A44", lw=1.6, ls=(0,(4,3)))
ax.text(cx+1.1, ly-1.19, "Línea punteada: relación no identificante (como en la Figura 1 de la pauta)", fontsize=11, color="#334155", va="center")
ax.text(cx, ly-1.65, "Valores de los campos ENUM:", fontsize=11, fontweight="bold", color="#10213F", va="center")
ax.text(cx, ly-1.97, "usuario.rol  =  ADMINISTRADOR · VENDEDOR · CLIENTE", fontsize=10.8, family="DejaVu Sans Mono", color="#334155", va="center")
ax.text(cx, ly-2.27, "boleta.estado  =  PENDIENTE · EN_RUTA · ENTREGADO · CANCELADO", fontsize=10.8, family="DejaVu Sans Mono", color="#334155", va="center")
gx = 20.3
ax.text(gx, ly-0.4, "Gestión en la aplicación", fontsize=13, fontweight="bold", color="#10213F")
notas = ["Hibernate crea estas tablas solo, a partir de las",
         "entidades Java (spring.jpa.hibernate.ddl-auto=update).",
         "DataInitializer carga el catálogo (2 categorías y",
         "8 productos) y los usuarios de demostración.",
         "Los datos viven en el volumen Docker  db_data."]
for i, t in enumerate(notas):
    ax.text(gx, ly-0.85-i*0.34, t, fontsize=10.8, color="#334155", va="center")

fig.savefig("diagrama_modelo_datos.png", dpi=130, bbox_inches="tight", facecolor="white")
print("ok")
