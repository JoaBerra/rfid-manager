#!/usr/bin/env python3
"""Kör Room-migreringen 1->2 mot en riktig SQLite-databas (Python sqlite3).

Bygger en v1-databas av `createSql` i schemas/.../1.json, lägger in rader (transmitted 0/1),
kör SQL-satserna ur app/.../data/local/Migrations.kt (MIGRATION_1_2_SQL) och jämför resultatet
med schemas/.../2.json (kolumner, NOT NULL, standardvärden, index) samt kontrollerar att
befintliga rader fick rätt status och att ingen data gick förlorad.

Användning (från RFIDManager/):  python3 tools/verify_migration_1_2.py
Komplement till Migration1To2SqlTest (som inte kör SQL). Ersätter inte ett instrumented
MigrationTestHelper-test på enhet/emulator.
"""
import json
import re
import sqlite3
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SCHEMAS = ROOT / "app/schemas/com.joakim.rfidmanager.data.local.AppDatabase"
MIGRATIONS = ROOT / "app/src/main/java/com/joakim/rfidmanager/data/local/Migrations.kt"


def load(version):
    return json.loads((SCHEMAS / f"{version}.json").read_text())["database"]["entities"][0]


def migration_sql():
    src = MIGRATIONS.read_text()
    block = src.split("val MIGRATION_1_2_SQL: List<String> = listOf(")[1].split("\n)\n")[0]
    return re.findall(r'^\s*"(.*)",?\s*$', block, flags=re.M)


def table_info(con):
    return {r[1]: (r[2], r[3], r[4], r[5]) for r in con.execute("PRAGMA table_info(persisted_readings)")}


def index_names(con):
    return {r[1] for r in con.execute("PRAGMA index_list(persisted_readings)")}


def main():
    v1, v2 = load(1), load(2)
    sql = migration_sql()
    assert len(sql) == 8, f"förväntade 8 satser, fick {len(sql)}"

    old = sqlite3.connect(":memory:")
    old.execute(v1["createSql"].replace("${TABLE_NAME}", "persisted_readings"))
    for idx in v1["indices"]:
        old.execute(idx["createSql"].replace("${TABLE_NAME}", "persisted_readings"))
    rows = [
        (1, "RFID", "A", 1000, 0, "persisted"),
        (2, "RFID", "B", 2000, 1, "transmitted"),
        (3, "EAN", "C", 3000, 0, "persisted"),
        (1_700_000_000_123, "RFID", "D", 4000, 1, "transmitted"),
    ]
    old.executemany(
        "INSERT INTO persisted_readings (id,type,uidOrCode,timestamp,transmitted,status) VALUES (?,?,?,?,?,?)", rows)
    old.commit()

    for stmt in sql:
        old.execute(stmt)
    old.commit()

    # Referens: färsk databas direkt från schema 2
    fresh = sqlite3.connect(":memory:")
    fresh.execute(v2["createSql"].replace("${TABLE_NAME}", "persisted_readings"))
    for idx in v2["indices"]:
        fresh.execute(idx["createSql"].replace("${TABLE_NAME}", "persisted_readings"))

    ok = True

    def check(cond, msg):
        nonlocal ok
        print(("OK   " if cond else "FEL  ") + msg)
        ok = ok and cond

    check(table_info(old) == table_info(fresh), "kolumner (typ, notnull, default, pk) identiska med färsk v2-databas")
    check(index_names(old) == index_names(fresh), f"index identiska: {sorted(index_names(old))}")

    got = {r[0]: r[1:] for r in old.execute(
        "SELECT id, outboxStatus, attempts, lastError, lastAttemptAt, sentAt, transmitted, status, uidOrCode, timestamp "
        "FROM persisted_readings")}
    check(len(got) == len(rows), f"alla {len(rows)} rader kvar")
    for (rid, _t, uid, ts, tr, status) in rows:
        g = got[rid]
        expected = "SENT" if tr == 1 else "PENDING"
        check(g[0] == expected, f"id {rid}: transmitted={tr} -> {g[0]}")
        check(g[1] == 0 and g[2] is None and g[3] is None and g[4] is None, f"id {rid}: attempts=0, lastError/lastAttemptAt/sentAt = NULL")
        check(g[5] == tr and g[6] == status and g[7] == uid and g[8] == ts, f"id {rid}: gamla kolumner orörda")

    old.execute("INSERT INTO persisted_readings (type,uidOrCode,timestamp,status,transmitted) VALUES ('RFID','E',5,'persisted',0)")
    new_id, st = old.execute("SELECT id, outboxStatus FROM persisted_readings WHERE uidOrCode='E'").fetchone()
    check(st == "PENDING" and new_id > 1_700_000_000_123, f"ny rad får DEFAULT PENDING och autoinkrement fortsätter ({new_id})")

    print("RESULTAT:", "GODKÄND" if ok else "UNDERKÄND")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
