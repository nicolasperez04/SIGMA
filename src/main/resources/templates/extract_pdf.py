import fitz
import sys

try:
    doc = fitz.open("ejemplo visual.pdf")
    print("Paginas:", doc.page_count)
    print("Metadatos:", doc.metadata)
    for i in range(doc.page_count):
        p = doc.load_page(i)
        print(f"=== Pagina {i+1} ===")
        txt = p.get_text("text")
        print(txt[:3000])
        imgs = p.get_images(full=True)
        print(f"Imagenes: {len(imgs)}")
        # Guardar imagen del render para ver el diseño
        if i == 0:
            mat = fitz.Matrix(2, 2)
            pix = p.get_pixmap(matrix=mat)
            pix.save("pagina1_preview.png")
            print("Preview guardada como pagina1_preview.png")
except Exception as e:
    print("Error:", e)
    import traceback
    traceback.print_exc()

