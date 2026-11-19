"""Create a dated SQL backup, or restore one to a new SQLite file."""

import argparse
from contextlib import closing
from datetime import datetime, timezone
from pathlib import Path
import re
import sqlite3
import subprocess
import tempfile


def validated_restore(dump, target):
    # 'xb' reserves the new name, including in concurrent restore attempts.
    with target.open("xb"):
        pass
    try:
        with closing(sqlite3.connect(target)) as db:
            db.executescript(dump.read_text(encoding="utf-8-sig"))
            if db.execute("PRAGMA integrity_check").fetchall() != [("ok",)]:
                raise ValueError("SQLite integrity check failed")
            if db.execute("PRAGMA foreign_key_check").fetchone() is not None:
                raise ValueError("Foreign key check failed")
            if not db.execute("SELECT 1 FROM WEB_MIGRATION WHERE Name='001-schema'").fetchone():
                raise ValueError("The SQL backup lacks the cinema web schema")
    except BaseException:
        target.unlink(missing_ok=True)
        raise


def backup(args):
    args.output_dir.mkdir(parents=True, exist_ok=True)
    stamp = datetime.now(timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
    target = args.output_dir / ("cinema-" + stamp + ".sql")
    with tempfile.TemporaryDirectory(dir=args.output_dir) as work:
        pending = Path(work) / "backup.sql"
        if args.turso:
            if not re.fullmatch(r"[a-z0-9][a-z0-9-]*", args.turso):
                raise ValueError("Invalid Turso database name")
            with pending.open("wb") as output:
                subprocess.run(["turso", "db", "shell", args.turso, ".dump"], stdout=output, check=True)
        else:
            uri = args.sqlite.resolve(strict=True).as_uri() + "?mode=ro"
            with closing(sqlite3.connect(uri, uri=True)) as source, closing(sqlite3.connect(":memory:")) as snapshot:
                source.backup(snapshot)
                with pending.open("w", encoding="utf-8", newline="\n") as output:
                    for line in snapshot.iterdump():
                        print(line, file=output)
        validated_restore(pending, Path(work) / "verify.sqlite")
        pending.replace(target)
    return target


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    export = commands.add_parser("backup", help="Write a verified, timestamped SQL dump")
    source = export.add_mutually_exclusive_group(required=True)
    source.add_argument("--sqlite", type=Path, help="Local cinema web database")
    source.add_argument("--turso", help="Database name in the authenticated Turso CLI")
    export.add_argument("--output-dir", type=Path, required=True)
    restore = commands.add_parser("restore", help="Create a new SQLite file from a trusted dump")
    restore.add_argument("dump", type=Path)
    restore.add_argument("--sqlite", type=Path, required=True)
    args = parser.parse_args()
    if args.command == "backup":
        print(backup(args))
    else:
        validated_restore(args.dump, args.sqlite)
        print(args.sqlite)


if __name__ == "__main__":
    main()
