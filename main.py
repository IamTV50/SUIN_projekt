import csv
import random

def generate_data_csv(rows=1000, filename="suinGeneratedData.csv"):
    # atributes and their values
    attributes = {
        "Oddaljenost_uporabnika_od_paketnika_(km)": lambda: round(random.uniform(0.1, 50.0), 2),
        "Kapaciteta_paketnika": lambda: random.randint(1, 10),
        "Vrsta_paketnika": lambda: random.choice(["navadni", "hlajen", "suhi"]),
        "Dostopnost_paketnika": lambda: random.choice(["24/7", "delovni_cas"]),
        "Zasedenost_paketnika (%)": lambda: round(random.uniform(0, 100), 1),
        "Varnost_paketnika": lambda: random.choice(["da", "ne"]),
        "Dostopnost_za_invalide": lambda: random.choice(["da", "ne"]),
        "Ocena_uporabnikov": lambda: round(random.uniform(1, 5), 1),
        "Zascita_pred_vremenskimi_vplivi": lambda: random.choice(["pod_streho", "v_zaprtem_prostoru", "na_prostem"]),
        "Dostopnost_parkirisca": lambda: random.choice(["da", "ne"]),
        "Stanje_paketnika": lambda: random.choice(["deluje_pravilno", "v_okvari"]),
        "Geolokacija_lat": lambda: round(random.uniform(-90, 90), 6),
        "Geolokacija_lon": lambda: round(random.uniform(-180, 180), 6),
    }

    with open(filename, mode="w", newline="", encoding="utf-8") as file:
        writer = csv.writer(file)

        # header
        writer.writerow(attributes.keys())

        for _ in range(rows):
            writer.writerow([generator() for generator in attributes.values()])

    print(f"'{filename}' with {rows} rows generated.")


generate_data_csv(50_000)
