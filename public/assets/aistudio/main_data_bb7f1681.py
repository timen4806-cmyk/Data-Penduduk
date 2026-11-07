import hashlib
import hmac
import os
import sqlite3
import tkinter as tk
from datetime import datetime
from tkinter import filedialog, messagebox, ttk

import customtkinter as ctk
from openpyxl import Workbook
from openpyxl.styles import Font, PatternFill
from reportlab.lib import colors
from reportlab.lib.pagesizes import A3, landscape
from reportlab.lib.styles import getSampleStyleSheet
from reportlab.platypus import (
    Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle
)


APP_DIR = os.path.dirname(os.path.abspath(__file__))
DB_PATH = os.path.join(APP_DIR, "penduduk.db")

FIELDS = [
    ("nik", "NIK"),
    ("nama", "Nama lengkap"),
    ("tempat_lahir", "Tempat lahir"),
    ("tanggal_lahir", "Tanggal lahir"),
    ("jenis_kelamin", "Jenis kelamin"),
    ("alamat", "Alamat"),
    ("rt", "RT"),
    ("rw", "RW"),
    ("kelurahan", "Kelurahan/Desa"),
    ("kecamatan", "Kecamatan"),
    ("agama", "Agama"),
    ("status_perkawinan", "Status perkawinan"),
    ("pekerjaan", "Pekerjaan"),
    ("kewarganegaraan", "Kewarganegaraan"),
]

FIELD_KEYS = [key for key, _ in FIELDS]

ADDRESS_KEYS = {"alamat", "rt", "rw", "kelurahan", "kecamatan"}

DISPLAY_FIELDS = [
    (key, "Alamat lengkap" if key == "alamat" else label)
    for key, label in FIELDS
    if key not in {"rt", "rw", "kelurahan", "kecamatan"}
]


def full_address(row):
    parts = [
        row["alamat"] or "",
        f"RT {row['rt']}" if row["rt"] else "",
        f"RW {row['rw']}" if row["rw"] else "",
        f"Kelurahan/Desa {row['kelurahan']}" if row["kelurahan"] else "",
        f"Kecamatan {row['kecamatan']}" if row["kecamatan"] else "",
    ]
    return ", ".join(part.strip() for part in parts if part.strip())

def hash_password(password, salt=None):
    salt = salt or os.urandom(16)
    hashed = hashlib.pbkdf2_hmac(
        "sha256", password.encode("utf-8"), salt, 310_000
    )
    return salt.hex(), hashed.hex()


def verify_password(password, salt_hex, hash_hex):
    salt = bytes.fromhex(salt_hex)
    _, candidate = hash_password(password, salt)
    return hmac.compare_digest(candidate, hash_hex)


class Database:
    def __init__(self):
        self.conn = sqlite3.connect(DB_PATH)
        self.conn.row_factory = sqlite3.Row
        self.conn.execute("PRAGMA foreign_keys = ON")
        self.create_tables()

    def create_tables(self):
        self.conn.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                salt TEXT NOT NULL,
                password_hash TEXT NOT NULL
            )
        """)
        self.conn.execute("""
            CREATE TABLE IF NOT EXISTS penduduk (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nik TEXT UNIQUE NOT NULL,
                nama TEXT NOT NULL,
                tempat_lahir TEXT DEFAULT '',
                tanggal_lahir TEXT DEFAULT '',
                jenis_kelamin TEXT DEFAULT '',
                alamat TEXT DEFAULT '',
                rt TEXT DEFAULT '',
                rw TEXT DEFAULT '',
                kelurahan TEXT DEFAULT '',
                kecamatan TEXT DEFAULT '',
                agama TEXT DEFAULT '',
                status_perkawinan TEXT DEFAULT '',
                pekerjaan TEXT DEFAULT '',
                kewarganegaraan TEXT DEFAULT 'WNI'
            )
        """)
        self.conn.commit()

    def user_count(self):
        return self.conn.execute("SELECT COUNT(*) FROM users").fetchone()[0]

    def create_user(self, username, password):
        salt, password_hash = hash_password(password)
        self.conn.execute(
            "INSERT INTO users (username, salt, password_hash) VALUES (?, ?, ?)",
            (username, salt, password_hash),
        )
        self.conn.commit()

    def authenticate(self, username, password):
        user = self.conn.execute(
            "SELECT salt, password_hash FROM users WHERE username = ?",
            (username,),
        ).fetchone()
        return bool(
            user and verify_password(password, user["salt"], user["password_hash"])
        )

    def all_residents(self, search=""):
        if search:
            term = f"%{search}%"
            return self.conn.execute("""
                SELECT * FROM penduduk
                WHERE nik LIKE ? OR nama LIKE ?
                ORDER BY nama COLLATE NOCASE
            """, (term, term)).fetchall()
        return self.conn.execute(
            "SELECT * FROM penduduk ORDER BY nama COLLATE NOCASE"
        ).fetchall()

    def get_resident(self, resident_id):
        return self.conn.execute(
            "SELECT * FROM penduduk WHERE id = ?", (resident_id,)
        ).fetchone()

    def save_resident(self, data, resident_id=None):
        if resident_id is None:
            columns = ", ".join(FIELD_KEYS)
            placeholders = ", ".join("?" for _ in FIELD_KEYS)
            self.conn.execute(
                f"INSERT INTO penduduk ({columns}) VALUES ({placeholders})",
                [data[key] for key in FIELD_KEYS],
            )
        else:
            assignments = ", ".join(f"{key} = ?" for key in FIELD_KEYS)
            self.conn.execute(
                f"UPDATE penduduk SET {assignments} WHERE id = ?",
                [data[key] for key in FIELD_KEYS] + [resident_id],
            )
        self.conn.commit()

    def delete_resident(self, resident_id):
        self.conn.execute("DELETE FROM penduduk WHERE id = ?", (resident_id,))
        self.conn.commit()

    def close(self):
        self.conn.close()


class ResidentDialog(ctk.CTkToplevel):
    def __init__(self, parent, db, resident=None, on_saved=None):
        super().__init__(parent)
        self.db = db
        self.resident = resident
        self.on_saved = on_saved
        self.entries = {}

        self.title("Edit data penduduk" if resident else "Tambah penduduk")
        self.geometry("780x700")
        self.minsize(700, 600)
        self.transient(parent)
        self.grab_set()

        self.grid_columnconfigure(0, weight=1)
        self.grid_rowconfigure(1, weight=1)

        ctk.CTkLabel(
            self, text=self.title(), font=ctk.CTkFont(size=22, weight="bold")
        ).grid(row=0, column=0, padx=24, pady=(20, 12), sticky="w")

        scroll = ctk.CTkScrollableFrame(self)
        scroll.grid(row=1, column=0, padx=20, pady=5, sticky="nsew")
        scroll.grid_columnconfigure(1, weight=1)

        for row, (key, label) in enumerate(DISPLAY_FIELDS):
            value = (
                full_address(resident)
                if resident and key == "alamat"
                else resident[key] if resident else ""
            )

            ctk.CTkLabel(scroll, text=label).grid(
                row=row, column=0, padx=(12, 18), pady=7, sticky="w"
            )

            if key == "jenis_kelamin":
                widget = ctk.CTkComboBox(
                    scroll,
                    values=["", "Laki-laki", "Perempuan"],
                )
                widget.set(value or "")
            elif key == "alamat":
                widget = ctk.CTkTextbox(scroll, height=100)
                widget.insert("1.0", value or "")
                widget.configure(wrap="word")
            else:
                widget = ctk.CTkEntry(scroll)
                widget.insert(0, value or "")
                if key == "nik":
                    widget.configure(placeholder_text="16 digit angka")

            widget.grid(row=row, column=1, padx=12, pady=7, sticky="ew")
            self.entries[key] = widget

        buttons = ctk.CTkFrame(self, fg_color="transparent")
        buttons.grid(row=2, column=0, padx=20, pady=18, sticky="e")
        ctk.CTkButton(
            buttons, text="Batal", fg_color="gray", command=self.destroy
        ).pack(side="left", padx=6)
        ctk.CTkButton(
            buttons, text="Simpan", command=self.save
        ).pack(side="left", padx=6)

    def read_value(self, key, widget):
        if key == "alamat":
            return widget.get("1.0", "end").strip()
        return widget.get().strip()

    def save(self):
        data = {
            key: self.read_value(key, widget)
            for key, widget in self.entries.items()
        }

        for key in ("rt", "rw", "kelurahan", "kecamatan"):
            data[key] = ""

        if len(data["nik"]) != 16 or not data["nik"].isdigit():
            messagebox.showwarning("Periksa NIK", "NIK harus terdiri dari 16 digit angka.", parent=self)
            return
        if not data["nama"]:
            messagebox.showwarning("Periksa data", "Nama lengkap wajib diisi.", parent=self)
            return

        if not data["kewarganegaraan"]:
            data["kewarganegaraan"] = "WNI"

        try:
            self.db.save_resident(
                data, self.resident["id"] if self.resident else None
            )
        except sqlite3.IntegrityError:
            messagebox.showwarning(
                "NIK sudah terdaftar",
                "NIK tersebut sudah digunakan oleh data penduduk lain.",
                parent=self,
            )
            return

        if self.on_saved:
            self.on_saved()
        self.destroy()


class DataWargaApp(ctk.CTk):
    def __init__(self):
        super().__init__()
        ctk.set_appearance_mode("light")
        ctk.set_default_color_theme("blue")

        self.db = Database()
        self.title("DataWarga — Administrasi Penduduk")
        self.geometry("1400x800")
        self.minsize(1000, 650)
        self.protocol("WM_DELETE_WINDOW", self.close_app)

        if self.db.user_count() == 0:
            if not self.first_run_setup():
                self.destroy()
                return

        if self.login():
            self.build_dashboard()
            self.refresh_table()
        else:
            self.destroy()

    def first_run_setup(self):
        dialog = ctk.CTkToplevel(self)
        dialog.title("Buat akun admin")
        dialog.geometry("420x340")
        dialog.resizable(False, False)
        dialog.grab_set()

        result = {"ok": False}
        ctk.CTkLabel(
            dialog, text="Buat akun admin",
            font=ctk.CTkFont(size=22, weight="bold")
        ).pack(pady=(24, 12))

        username = ctk.CTkEntry(dialog, placeholder_text="Username")
        username.pack(fill="x", padx=32, pady=8)
        password = ctk.CTkEntry(dialog, placeholder_text="Password", show="•")
        password.pack(fill="x", padx=32, pady=8)
        confirm = ctk.CTkEntry(dialog, placeholder_text="Ulangi password", show="•")
        confirm.pack(fill="x", padx=32, pady=8)

        def create():
            u, p, p2 = username.get().strip(), password.get(), confirm.get()
            if not u or len(p) < 8:
                messagebox.showwarning(
                    "Periksa data", "Isi username dan gunakan password minimal 8 karakter.",
                    parent=dialog,
                )
                return
            if p != p2:
                messagebox.showwarning("Password berbeda", "Password tidak cocok.", parent=dialog)
                return
            self.db.create_user(u, p)
            result["ok"] = True
            dialog.destroy()

        ctk.CTkButton(dialog, text="Buat akun", command=create).pack(pady=18)
        self.wait_window(dialog)
        return result["ok"]

    def login(self):
        dialog = ctk.CTkToplevel(self)
        dialog.title("Login — DataWarga")
        dialog.geometry("400x340")
        dialog.resizable(False, False)
        dialog.grab_set()
        result = {"ok": False}

        ctk.CTkLabel(
            dialog, text="◈ DataWarga",
            font=ctk.CTkFont(size=26, weight="bold")
        ).pack(pady=(32, 8))
        ctk.CTkLabel(dialog, text="Masuk ke panel administrasi").pack(pady=(0, 18))

        username = ctk.CTkEntry(dialog, placeholder_text="Username")
        username.pack(fill="x", padx=36, pady=8)
        password = ctk.CTkEntry(dialog, placeholder_text="Password", show="•")
        password.pack(fill="x", padx=36, pady=8)

        def submit(event=None):
            if self.db.authenticate(username.get().strip(), password.get()):
                result["ok"] = True
                dialog.destroy()
            else:
                messagebox.showerror(
                    "Login gagal", "Username atau password salah.", parent=dialog
                )

        ctk.CTkButton(dialog, text="Masuk", command=submit).pack(
            fill="x", padx=36, pady=16
        )
        dialog.bind("<Return>", submit)
        self.wait_window(dialog)
        return result["ok"]

    def build_dashboard(self):
        self.grid_columnconfigure(1, weight=1)
        self.grid_rowconfigure(0, weight=1)

        sidebar = ctk.CTkFrame(self, width=220, corner_radius=0)
        sidebar.grid(row=0, column=0, sticky="nsew")
        sidebar.grid_propagate(False)

        ctk.CTkLabel(
            sidebar, text="◈ DataWarga",
            font=ctk.CTkFont(size=22, weight="bold")
        ).pack(padx=20, pady=(28, 8), anchor="w")
        ctk.CTkLabel(
            sidebar, text="ADMINISTRASI PENDUDUK", text_color="gray"
        ).pack(padx=20, pady=(0, 28), anchor="w")

        ctk.CTkButton(
            sidebar, text="＋  Tambah data", command=self.add_resident
        ).pack(fill="x", padx=16, pady=6)
        ctk.CTkButton(
            sidebar, text="↓  Ekspor Excel", command=self.export_excel,
            fg_color="#23865b", hover_color="#176b47"
        ).pack(fill="x", padx=16, pady=6)
        ctk.CTkButton(
            sidebar, text="↓  Ekspor PDF", command=self.export_pdf,
            fg_color="#5755d9", hover_color="#4543b5"
        ).pack(fill="x", padx=16, pady=6)
        ctk.CTkButton(
            sidebar, text="Keluar", command=self.logout,
            fg_color="gray", hover_color="#555"
        ).pack(side="bottom", fill="x", padx=16, pady=20)

        main = ctk.CTkFrame(self, fg_color="transparent")
        main.grid(row=0, column=1, padx=24, pady=22, sticky="nsew")
        main.grid_columnconfigure(0, weight=1)
        main.grid_rowconfigure(2, weight=1)

        header = ctk.CTkFrame(main, fg_color="transparent")
        header.grid(row=0, column=0, sticky="ew", pady=(0, 16))
        ctk.CTkLabel(
            header, text="Data penduduk",
            font=ctk.CTkFont(size=27, weight="bold")
        ).pack(side="left")

        search_frame = ctk.CTkFrame(main)
        search_frame.grid(row=1, column=0, sticky="ew", pady=(0, 12))
        search_frame.grid_columnconfigure(0, weight=1)
        self.search_entry = ctk.CTkEntry(
            search_frame, placeholder_text="Cari nama atau NIK…"
        )
        self.search_entry.grid(row=0, column=0, padx=10, pady=10, sticky="ew")
        self.search_entry.bind("<Return>", lambda _event: self.refresh_table())
        ctk.CTkButton(
            search_frame, text="Cari", width=100, command=self.refresh_table
        ).grid(row=0, column=1, padx=(0, 10), pady=10)
        ctk.CTkButton(
            search_frame, text="Reset", width=80, fg_color="gray",
            command=self.reset_search
        ).grid(row=0, column=2, padx=(0, 10), pady=10)

        table_frame = ctk.CTkFrame(main)
        table_frame.grid(row=2, column=0, sticky="nsew")
        table_frame.grid_columnconfigure(0, weight=1)
        table_frame.grid_rowconfigure(0, weight=1)

        style = ttk.Style()
        style.theme_use("clam")
        style.configure("Treeview", rowheight=30, font=("Arial", 9))
        style.configure("Treeview.Heading", font=("Arial", 9, "bold"))

        self.tree = ttk.Treeview(
            table_frame,
            columns=FIELD_KEYS,
            show="headings",
            selectmode="browse",
        )

        display_keys = [key for key, _ in DISPLAY_FIELDS]

        self.tree = ttk.Treeview(
            table_frame,
            columns=display_keys,
            show="headings",
            selectmode="browse",
        )

        for key, label in DISPLAY_FIELDS:
            self.tree.heading(key, text=label)
            self.tree.column(key, width=125, minwidth=90, anchor="w")

        self.tree.column("nik", width=145, minwidth=145)
        self.tree.column("nama", width=180, minwidth=150)
        self.tree.column("alamat", width=430, minwidth=300)

        yscroll = ttk.Scrollbar(table_frame, orient="vertical", command=self.tree.yview)
        xscroll = ttk.Scrollbar(table_frame, orient="horizontal", command=self.tree.xview)
        self.tree.configure(yscrollcommand=yscroll.set, xscrollcommand=xscroll.set)
        self.tree.grid(row=0, column=0, sticky="nsew")
        yscroll.grid(row=0, column=1, sticky="ns")
        xscroll.grid(row=1, column=0, sticky="ew")
        self.tree.bind("<Double-1>", lambda _event: self.edit_resident())

        actions = ctk.CTkFrame(main, fg_color="transparent")
        actions.grid(row=3, column=0, sticky="e", pady=(12, 0))
        ctk.CTkButton(
            actions, text="Edit terpilih", command=self.edit_resident
        ).pack(side="left", padx=5)
        ctk.CTkButton(
            actions, text="Hapus terpilih", fg_color="#c0392b",
            hover_color="#922b21", command=self.delete_resident
        ).pack(side="left", padx=5)

        self.count_label = ctk.CTkLabel(main, text="")
        self.count_label.grid(row=4, column=0, sticky="w", pady=(8, 0))

    def refresh_table(self):
        for item in self.tree.get_children():
            self.tree.delete(item)

        rows = self.db.all_residents(self.search_entry.get().strip())

        for row in rows:
            values = [
                full_address(row) if key == "alamat" else (row[key] or "")
                for key, _ in DISPLAY_FIELDS
            ]
            self.tree.insert(
                "",
                "end",
                iid=str(row["id"]),
                values=values,
            )

        self.count_label.configure(text=f"{len(rows)} data ditampilkan")

    def reset_search(self):
        self.search_entry.delete(0, "end")
        self.refresh_table()

    def selected_id(self):
        selection = self.tree.selection()
        return int(selection[0]) if selection else None

    def add_resident(self):
        ResidentDialog(self, self.db, on_saved=self.refresh_table)

    def edit_resident(self):
        resident_id = self.selected_id()
        if resident_id is None:
            messagebox.showinfo("Pilih data", "Pilih data penduduk yang akan diedit.")
            return
        ResidentDialog(
            self, self.db, self.db.get_resident(resident_id),
            on_saved=self.refresh_table,
        )

    def delete_resident(self):
        resident_id = self.selected_id()
        if resident_id is None:
            messagebox.showinfo("Pilih data", "Pilih data penduduk yang akan dihapus.")
            return
        if messagebox.askyesno("Konfirmasi", "Hapus data penduduk terpilih?"):
            self.db.delete_resident(resident_id)
            self.refresh_table()

    def export_excel(self):
        path = filedialog.asksaveasfilename(
            title="Simpan Excel",
            defaultextension=".xlsx",
            filetypes=[("Excel workbook", "*.xlsx")],
            initialfile="data_penduduk.xlsx",
        )
        if not path:
            return

        wb = Workbook()
        ws = wb.active
        ws.title = "Data Penduduk"
        ws.append([label for _, label in DISPLAY_FIELDS])
        for row in self.db.all_residents():
            ws.append([
                full_address(row) if key == "alamat" else (row[key] or "")
                for key, _ in DISPLAY_FIELDS
            ])

        ws.freeze_panes = "A2"
        ws.auto_filter.ref = ws.dimensions
        for column in ws.columns:
            width = max(len(str(cell.value or "")) for cell in column) + 2
            ws.column_dimensions[column[0].column_letter].width = min(max(width, 12), 36)

        try:
            wb.save(path)
            messagebox.showinfo("Ekspor selesai", f"File Excel tersimpan:\n{path}")
        except OSError as exc:
            messagebox.showerror("Gagal menyimpan", str(exc))

    def export_pdf(self):
        path = filedialog.asksaveasfilename(
            title="Simpan PDF",
            defaultextension=".pdf",
            filetypes=[("PDF", "*.pdf")],
            initialfile="data_penduduk.pdf",
        )
        if not path:
            return

        styles = getSampleStyleSheet()

        from reportlab.lib.enums import TA_CENTER
        from reportlab.lib.styles import ParagraphStyle

        header_style = ParagraphStyle(
            "HeaderPutihTengah",
            parent=styles["BodyText"],
            textColor=colors.white,
            alignment=TA_CENTER,
            fontName="Helvetica-Bold",
        )

        headers = [
            Paragraph(label, header_style)
            for _, label in DISPLAY_FIELDS
        ]

        data = [headers]

        for row in self.db.all_residents():
            cells = []

            for key, _ in DISPLAY_FIELDS:
                value = (
                    full_address(row)
                    if key == "alamat"
                    else (row[key] or "")
                )
                safe_value = (
                    str(value)
                    .replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                )
                cells.append(
                    Paragraph(safe_value, styles["BodyText"])
                )

            data.append(cells)

        page_width = landscape(A3)[0]
        available_width = page_width - 40
        address_width = 230
        other_column_count = len(DISPLAY_FIELDS) - 1
        other_width = (
            (available_width - address_width) / other_column_count
            if other_column_count > 0
            else available_width
        )

        col_widths = [
            address_width if key == "alamat" else other_width
            for key, _ in DISPLAY_FIELDS
        ]

        table = Table(
            data,
            colWidths=col_widths,
            repeatRows=1,
        )
        table.setStyle(TableStyle([
            ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#198754")),
            ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
            ("TEXTCOLOR", (0, 1), (-1, -1), colors.HexColor("#222222")),
            ("ROWBACKGROUNDS", (0, 1), (-1, -1), [
                colors.white,
                colors.HexColor("#f1f8f4"),
            ]),
            ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
            ("FONTSIZE", (0, 0), (-1, -1), 6),
            ("LEADING", (0, 0), (-1, -1), 7),
            ("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#d8deea")),
            ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ]))

        doc = SimpleDocTemplate(
            path,
            pagesize=landscape(A3),
            leftMargin=20,
            rightMargin=20,
            topMargin=24,
            bottomMargin=24,
        )

        try:
            doc.build([
                Paragraph("Daftar Data Penduduk", styles["Title"]),
                Paragraph(
                    f"Dicetak: {datetime.now().strftime('%d-%m-%Y %H:%M')}",
                    styles["Normal"],
                ),
                Spacer(1, 12),
                table,
            ])
            messagebox.showinfo(
                "Ekspor selesai",
                f"File PDF tersimpan:\n{path}",
            )
        except OSError as exc:
            messagebox.showerror("Gagal menyimpan", str(exc))
        except Exception as exc:
            messagebox.showerror(
                "Gagal membuat PDF",
                f"Terjadi kesalahan saat membuat PDF:\n{exc}",
            )

    def logout(self):
        if messagebox.askyesno("Keluar", "Keluar dari akun dan aplikasi?"):
            self.destroy()

    def close_app(self):
        self.db.close()
        self.destroy()


if __name__ == "__main__":
    app = DataWargaApp()
    if app.winfo_exists():
        app.mainloop()
        try:
            app.db.close()
        except Exception:
            pass
